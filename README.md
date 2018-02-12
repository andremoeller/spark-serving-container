# SageMaker Spark Serving

This model container deserializes a PipelineModel with a VectorAssembler and a RandomForestClassifier and uses the
PipelineModel to make predictions on JSON data.

This model container complies with the SageMaker Hosting container requirements.

## Caveats

* This example makes a restrictive assumption about the schema of the inference JSON data in
com.amazonaws.spark.serving.ScalatraBootstrap.
* This hasn't been optimized for latency at all.

## Docker Build and Run

### Build

```
sbt docker
```

Yields an image tagged com.amazonaws/sagemaker-spark-serving.

### Run

```
docker run -p 8080:8080 -p 4040:4040 com.amazonaws/sagemaker-spark-serving
```

### Test

```sh
time curl --data-binary "@data/post-data.txt" -H "Content-Type: application/json" -X POST http://localhost:8080/invocations
```

On my development machine, this shows latency generally about 200ms for small payloads (~2KB, data/post-data.txt),
about 1s for large payloads with many records (~5MB, data/many-records-post-data.txt) and about 1s on large payloads with one 
record (~5MB, data/one-large-record-post-data.txt), with the first invocation taking about an additional second.

### Deploy

Tag and push your image to AWS ECR.
```
`aws ecr get-login --no-include-email`
# Replace this with your own account and region.
ACCOUNT=038453126632
REGION=us-east-1
ECR_IMAGE=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/spark-serving
docker tag com.amazonaws/sagemaker-spark-serving $ECR_IMAGE
docker push ECR_IMAGE
```

###
Make an endpoint with the SageMaker Java SDK and make inferences against that endpoint with the SageMaker Runtime Java SDK.

(using spark-shell just for convenience):

```sh
spark-shell --packages com.amazonaws:aws-java-sdk-sagemaker:1.11.248,com.amazonaws:aws-java-sdk-sagemakerruntime:1.11.248
```

```scala
import java.util.UUID.randomUUID
import java.nio.ByteBuffer
import com.amazonaws.SdkBaseException
import com.amazonaws.retry.RetryUtils
import com.amazonaws.services.sagemaker.model._
import com.amazonaws.services.sagemaker.{AmazonSageMaker, AmazonSageMakerClientBuilder}
import com.amazonaws.services.sagemakerruntime.{AmazonSageMakerRuntime, AmazonSageMakerRuntimeClientBuilder}
import com.amazonaws.services.sagemakerruntime.model.InvokeEndpointRequest

val sagemaker = AmazonSageMakerClientBuilder.defaultClient()

val uid = randomUUID().toString.split("-")(0)
val modelName = s"spark-serving-model-$uid"

// Create Model
// Replace this with your own image URI.
// Be sure to give sagemaker.amazonaws.com pull permissions on your ECR repository.
val imageURI = "038453126632.dkr.ecr.us-east-1.amazonaws.com/spark-serving"

// Replace this with your own model data.
val modelData = "s3://am-model-data/model.tar.gz"
val containerDefinition = new ContainerDefinition().withImage(imageURI).withModelDataUrl(modelData)

// Replace this with your own SageMaker-compatible IAM role.
val role = "arn:aws:iam::038453126632:role/service-role/AmazonSageMaker-ExecutionRole-20171129T125754"
val createModelRequest = new CreateModelRequest().withModelName(modelName).withPrimaryContainer(containerDefinition).withExecutionRoleArn(role)
val createModelResponse = sagemaker.createModel(createModelRequest)

// Create Endpoint Config
val instanceCount = 1
val instanceType = "ml.m4.xlarge"
val productionVariant = new ProductionVariant().withInitialInstanceCount(instanceCount).withInstanceType(instanceType).withModelName(modelName).withVariantName("spark-serving-variant")
val endpointConfigName = s"spark-service-endpoint-config-$uid"
val createEndpointConfigRequest = new CreateEndpointConfigRequest().withEndpointConfigName(endpointConfigName).withProductionVariants(productionVariant)
val createEndpointConfigResponse = sagemaker.createEndpointConfig(createEndpointConfigRequest)

// Create Endpoint
val endpointName = s"spark-serving-endpoint-$uid"
val createEndpointRequest = new CreateEndpointRequest().withEndpointConfigName(endpointConfigName).withEndpointName(endpointName)
val createEndpointResponse = sagemaker.createEndpoint(createEndpointRequest)

// Poll for endpoint completion. This takes about ten minutes.

val describeEndpointRequest = new DescribeEndpointRequest().withEndpointName(endpointName)
val describeEndpointResponse = sagemaker.describeEndpoint(describeEndpointRequest)

var endpointInService = false

while (!endpointInService) {
  try {
    val describeEndpointResponse = sagemaker.describeEndpoint(describeEndpointRequest)
    val currentStatus = EndpointStatus.fromValue(describeEndpointResponse.getEndpointStatus)
    println(s"Endpoint creation status: $currentStatus")
    endpointInService = currentStatus match {
      case EndpointStatus.InService => true
      case EndpointStatus.Failed =>
      val message = s"Endpoint '$endpointName' failed for reason:" +
      s" '${describeEndpointResponse.getFailureReason}'"
      throw new RuntimeException(message)
      case _ => false // for any other statuses, continue polling
    }
  } catch {
      case e : SdkBaseException =>
      if (!RetryUtils.isRetryableServiceException(e)) {
        throw e
      }
      case t : Throwable => throw t
    }
  Thread.sleep(10000)
}

// Invoke the endpoint.

val sagemakerRuntime = AmazonSageMakerRuntimeClientBuilder.defaultClient

val contentType = "application/json"

// This payload is taken from a DataFrame with a "label" column with a Double and a "features" column with a SparseVector using DataFrame.toJSON
// The model container deserializes the PipelineModel when the SparkInferenceServlet is initialized and uses it to serve these predictions.

val data = "data/post-data.txt"
val payload = scala.io.Source.fromFile(data).mkString
val body = ByteBuffer.wrap(payload.getBytes)
val invokeEndpointRequest = new InvokeEndpointRequest().withEndpointName(endpointName).withContentType(contentType).withBody(body)
val invokeEndpointResponse = sagemakerRuntime.invokeEndpoint(invokeEndpointRequest)
val responseBody = new String(invokeEndpointResponse.getBody.array)

println(responseBody)
```

This gives a JSON-formatted response with predictions from the model container:
```
{"rawPrediction":{"type":1,"values":[13.0,7.0]},"probability":{"type":1,"values":[0.65,0.35]},"prediction":0.0}
```

### Model Data

The PipelineModel needs to be a tar.gz file.

The example model data tarball was made from test-pipeline-model with:

```
tar -zcvf model.tar.gz *
```

Then uploaded to s3.
