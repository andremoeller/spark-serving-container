# SageMaker Spark Serving #

This model container deserializes a PipelineModel with a VectorAssembler and a RandomForestClassifier and uses the
PipelineModel to make predictions on JSON data.

This model container complies with the SageMaker Hosting container requirements.

## Build & Run ##

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
