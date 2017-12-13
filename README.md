# SageMaker Spark Serving

This model container deserializes a PipelineModel with a VectorAssembler and a RandomForestClassifier and uses the
PipelineModel to make predictions on JSON data.

This model container complies with the SageMaker Hosting container requirements.

## Build & Run

```sh
sbt clean assembly; java -jar target/scala-2.11/SageMaker\ Spark\ Serving-assembly-0.1.0-SNAPSHOT.jar
```
## Test

Running functional tests:
```sh
sbt test
```

Or, when the server is running:

```sh
curl -d "@data/post-data.txt" -H "Content-Type: application/json" -X POST http://localhost:8080/invocations
```

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
time curl -d "@data/post-data.txt" -H "Content-Type: application/json" -X POST http://localhost:8080/invocations
```

No effort was put into performance. This shows latency generally between 200ms and 400ms. The same on data/large-post-data.txt shows latency of about 10 seconds, which is horrendous.

### Deploy

Tag and push your image to AWS ECR.
```
`aws ecr get-login --no-include-email`
ACCOUNT=038453126632
REGION=us-east-1
ECR_IMAGE=$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/spark-serving
docker tag com.amazonaws/sagemaker-spark-serving $ECR_IMAGE
docker push ECR_IMAGE
```

Make an endpoint with the SageMaker Java SDK (using spark-shell just for convenience):

```sh
spark-shell --packages com.amazonaws:aws-java-sdk-sagemaker:1.11.248,com.amazonaws:aws-java-sdk-sagemakerruntime:1.11.248
```

```scala
import java.util.UUID.randomUUID
import com.amazonaws.SdkBaseException
import com.amazonaws.retry.RetryUtils
import com.amazonaws.services.sagemaker.model._
import com.amazonaws.services.sagemaker.{AmazonSageMaker, AmazonSageMakerClientBuilder}
import com.amazonaws.services.sagemakerruntime.{AmazonSageMakerRuntime, AmazonSageMakerRuntimeClientBuilder}

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

// Poll for endpoint completion

val describeEndpointRequest = new DescribeEndpointRequest().withEndpointName(endpointName)
val describeEndpointResponse = sagemaker.describeEndpoint(describeEndpointRequest)
```

The model data tarball was made from test-pipeline-model with:

```
tar -zcvf model.tar.gz *
```

Then uploaded to s3.