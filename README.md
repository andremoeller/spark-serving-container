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
docker run -p 8080:8080 com.amazonaws/sagemaker-spark-serving
```

### Test

```sh
time curl -d "@data/post-data.txt" -H "Content-Type: application/json" -X POST http://localhost:8080/invocations
```

This shows latency generally between 200ms and 400ms for very small payloads (~2KB), and about 2.5 seconds for the first
inference since the servlet has to load the model.

The same on data/large-post-data.txt (~5MB) shows latency of about 10 seconds.

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

val payload = """{"label":0.0,"features":{"type":0,"size":692,"indices":[127,128,129,130,131,154,155,156,157,158,159,181,182,183,184,185,186,187,188,189,207,208,209,210,211,212,213,214,215,216,217,235,236,237,238,239,240,241,242,243,244,245,262,263,264,265,266,267,268,269,270,271,272,273,289,290,291,292,293,294,295,296,297,300,301,302,316,317,318,319,320,321,328,329,330,343,344,345,346,347,348,349,356,357,358,371,372,373,374,384,385,386,399,400,401,412,413,414,426,427,428,429,440,441,442,454,455,456,457,466,467,468,469,470,482,483,484,493,494,495,496,497,510,511,512,520,521,522,523,538,539,540,547,548,549,550,566,567,568,569,570,571,572,573,574,575,576,577,578,594,595,596,597,598,599,600,601,602,603,604,622,623,624,625,626,627,628,629,630,651,652,653,654,655,656,657],"values":[51.0,159.0,253.0,159.0,50.0,48.0,238.0,252.0,252.0,252.0,237.0,54.0,227.0,253.0,252.0,239.0,233.0,252.0,57.0,6.0,10.0,60.0,224.0,252.0,253.0,252.0,202.0,84.0,252.0,253.0,122.0,163.0,252.0,252.0,252.0,253.0,252.0,252.0,96.0,189.0,253.0,167.0,51.0,238.0,253.0,253.0,190.0,114.0,253.0,228.0,47.0,79.0,255.0,168.0,48.0,238.0,252.0,252.0,179.0,12.0,75.0,121.0,21.0,253.0,243.0,50.0,38.0,165.0,253.0,233.0,208.0,84.0,253.0,252.0,165.0,7.0,178.0,252.0,240.0,71.0,19.0,28.0,253.0,252.0,195.0,57.0,252.0,252.0,63.0,253.0,252.0,195.0,198.0,253.0,190.0,255.0,253.0,196.0,76.0,246.0,252.0,112.0,253.0,252.0,148.0,85.0,252.0,230.0,25.0,7.0,135.0,253.0,186.0,12.0,85.0,252.0,223.0,7.0,131.0,252.0,225.0,71.0,85.0,252.0,145.0,48.0,165.0,252.0,173.0,86.0,253.0,225.0,114.0,238.0,253.0,162.0,85.0,252.0,249.0,146.0,48.0,29.0,85.0,178.0,225.0,253.0,223.0,167.0,56.0,85.0,252.0,252.0,252.0,229.0,215.0,252.0,252.0,252.0,196.0,130.0,28.0,199.0,252.0,252.0,253.0,252.0,252.0,233.0,145.0,25.0,128.0,252.0,253.0,252.0,141.0,37.0]}}"""
val body = ByteBuffer.wrap(payload.getBytes)
val invokeEndpointRequest = new InvokeEndpointRequest().withEndpointName(endpointName).withContentType(contentType).withBody(body)
val invokeEndpointResponse = sagemakerRuntime.invokeEndpoint(invokeEndpointRequest)
val responseBody = new String(invokeEndpointResponse.getBody.array)

println(responseBody)
```

This gives a JSON-formatted response with predictions from the model container:
```
{"label":0.0,"features":{"type":0,"size":692,"indices":[127,128,129,130,131,154,155,156,157,158,159,181,182,183,184,185,186,187,188,189,207,208,209,210,211,212,213,214,215,216,217,235,236,237,238,239,240,241,242,243,244,245,262,263,264,265,266,267,268,269,270,271,272,273,289,290,291,292,293,294,295,296,297,300,301,302,316,317,318,319,320,321,328,329,330,343,344,345,346,347,348,349,356,357,358,371,372,373,374,384,385,386,399,400,401,412,413,414,426,427,428,429,440,441,442,454,455,456,457,466,467,468,469,470,482,483,484,493,494,495,496,497,510,511,512,520,521,522,523,538,539,540,547,548,549,550,566,567,568,569,570,571,572,573,574,575,576,577,578,594,595,596,597,598,599,600,601,602,603,604,622,623,624,625,626,627,628,629,630,651,652,653,654,655,656,657],"values":[51.0,159.0,253.0,159.0,50.0,48.0,238.0,252.0,252.0,252.0,237.0,54.0,227.0,253.0,252.0,239.0,233.0,252.0,57.0,6.0,10.0,60.0,224.0,252.0,253.0,252.0,202.0,84.0,252.0,253.0,122.0,163.0,252.0,252.0,252.0,253.0,252.0,252.0,96.0,189.0,253.0,167.0,51.0,238.0,253.0,253.0,190.0,114.0,253.0,228.0,47.0,79.0,255.0,168.0,48.0,238.0,252.0,252.0,179.0,12.0,75.0,121.0,21.0,253.0,243.0,50.0,38.0,165.0,253.0,233.0,208.0,84.0,253.0,252.0,165.0,7.0,178.0,252.0,240.0,71.0,19.0,28.0,253.0,252.0,195.0,57.0,252.0,252.0,63.0,253.0,252.0,195.0,198.0,253.0,190.0,255.0,253.0,196.0,76.0,246.0,252.0,112.0,253.0,252.0,148.0,85.0,252.0,230.0,25.0,7.0,135.0,253.0,186.0,12.0,85.0,252.0,223.0,7.0,131.0,252.0,225.0,71.0,85.0,252.0,145.0,48.0,165.0,252.0,173.0,86.0,253.0,225.0,114.0,238.0,253.0,162.0,85.0,252.0,249.0,146.0,48.0,29.0,85.0,178.0,225.0,253.0,223.0,167.0,56.0,85.0,252.0,252.0,252.0,229.0,215.0,252.0,252.0,252.0,196.0,130.0,28.0,199.0,252.0,252.0,253.0,252.0,252.0,233.0,145.0,25.0,128.0,252.0,253.0,252.0,141.0,37.0]},"assembledFeatures":{"type":0,"size":692,"indices":[127,128,129,130,131,154,155,156,157,158,159,181,182,183,184,185,186,187,188,189,207,208,209,210,211,212,213,214,215,216,217,235,236,237,238,239,240,241,242,243,244,245,262,263,264,265,266,267,268,269,270,271,272,273,289,290,291,292,293,294,295,296,297,300,301,302,316,317,318,319,320,321,328,329,330,343,344,345,346,347,348,349,356,357,358,371,372,373,374,384,385,386,399,400,401,412,413,414,426,427,428,429,440,441,442,454,455,456,457,466,467,468,469,470,482,483,484,493,494,495,496,497,510,511,512,520,521,522,523,538,539,540,547,548,549,550,566,567,568,569,570,571,572,573,574,575,576,577,578,594,595,596,597,598,599,600,601,602,603,604,622,623,624,625,626,627,628,629,630,651,652,653,654,655,656,657],"values":[51.0,159.0,253.0,159.0,50.0,48.0,238.0,252.0,252.0,252.0,237.0,54.0,227.0,253.0,252.0,239.0,233.0,252.0,57.0,6.0,10.0,60.0,224.0,252.0,253.0,252.0,202.0,84.0,252.0,253.0,122.0,163.0,252.0,252.0,252.0,253.0,252.0,252.0,96.0,189.0,253.0,167.0,51.0,238.0,253.0,253.0,190.0,114.0,253.0,228.0,47.0,79.0,255.0,168.0,48.0,238.0,252.0,252.0,179.0,12.0,75.0,121.0,21.0,253.0,243.0,50.0,38.0,165.0,253.0,233.0,208.0,84.0,253.0,252.0,165.0,7.0,178.0,252.0,240.0,71.0,19.0,28.0,253.0,252.0,195.0,57.0,252.0,252.0,63.0,253.0,252.0,195.0,198.0,253.0,190.0,255.0,253.0,196.0,76.0,246.0,252.0,112.0,253.0,252.0,148.0,85.0,252.0,230.0,25.0,7.0,135.0,253.0,186.0,12.0,85.0,252.0,223.0,7.0,131.0,252.0,225.0,71.0,85.0,252.0,145.0,48.0,165.0,252.0,173.0,86.0,253.0,225.0,114.0,238.0,253.0,162.0,85.0,252.0,249.0,146.0,48.0,29.0,85.0,178.0,225.0,253.0,223.0,167.0,56.0,85.0,252.0,252.0,252.0,229.0,215.0,252.0,252.0,252.0,196.0,130.0,28.0,199.0,252.0,252.0,253.0,252.0,252.0,233.0,145.0,25.0,128.0,252.0,253.0,252.0,141.0,37.0]},"rawPrediction":{"type":1,"values":[20.0,0.0]},"probability":{"type":1,"values":[1.0,0.0]},"prediction":0.0}
```

### Model Data

The PipelineModel needs to be a tar.gz file.

The example model data tarball was made from test-pipeline-model with:

```
tar -zcvf model.tar.gz *
```

Then uploaded to s3.