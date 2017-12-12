val ScalatraVersion = "2.6.2"

val SparkVersion = "2.2.0"

organization := "com.amazonaws"

name := "SageMaker Spark Serving"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.5" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.15.v20160210" % "container;compile",
  "javax.servlet" % "javax.servlet-api" % "3.1.0",
  "com.sun.jersey" % "jersey-server" % "1.2",

  "ml.combust.mleap" %% "mleap-runtime" % "0.8.1",
  "org.apache.commons" % "commons-compress" % "1.15",
  "org.apache.spark" %% "spark-core" % SparkVersion,
  "org.apache.spark" %% "spark-mllib" % SparkVersion,
  "org.apache.spark" %% "spark-sql" % SparkVersion,
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
