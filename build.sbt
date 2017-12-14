import java.nio.file.Paths

val ScalatraVersion = "2.6.2"

val SparkVersion = "2.2.0"

organization := "com.amazonaws"

name := "SageMaker-Spark-Serving"

assemblyJarName in assembly := "sagemaker-spark-serving-fat-jar.jar"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.15.v20160210" % "container;compile",
  "javax.servlet" % "javax.servlet-api" % "3.1.0",

  "org.apache.spark" %% "spark-core" % SparkVersion,
  "org.apache.spark" %% "spark-mllib" % SparkVersion,
  "org.apache.spark" %% "spark-sql" % SparkVersion,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact : File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"
  val modelPath = "/opt/ml/model"

  new Dockerfile {
    from("java")
    env("MODEL_PATH" -> modelPath)
    add(artifact, artifactTargetPath)
    //
    // SageMaker downloads the
    add(new File("test-pipeline-model"), modelPath)
    expose(8080)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.filterDistinctLines
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

enablePlugins(DockerPlugin)
enablePlugins(ScalatraPlugin)