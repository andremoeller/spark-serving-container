# SageMaker Spark Serving #

## Build & Run ##

```sh
$ cd SageMaker_Spark_Serving
$ ./sbt
> jetty:start
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
