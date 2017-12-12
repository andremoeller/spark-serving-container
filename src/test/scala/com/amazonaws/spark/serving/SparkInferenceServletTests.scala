package com.amazonaws.spark.serving

import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.SparkSession
import org.scalatra.test.scalatest._
import org.scalatest.FunSuiteLike

class SparkInferenceServletTests extends ScalatraSuite with FunSuiteLike with SparkModelLoader {

  val spark = SparkSession.builder
    .master("local")
    .appName("spark session")
    .getOrCreate()

  val dataset = spark.createDataFrame(
    Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))
  ).toDF("id", "hour", "mobile", "userFeatures", "clicked")

  addServlet(new SparkInferenceServlet(loadModel()), "/*")

  test("GET / on SparkInferenceServlet should return status 200") {
    get("/") {
      status should equal (200)
    }
  }

  test("GET /ping on SparkInferenceServlet should return status 200 and explain params") {
    get("/ping") {
      status should equal (200)
    }
  }

  test("POST /invocations on SparkInferenceServlet should return status 200") {
//    def post[A](uri: String, body: Array[Byte] = Array(), headers: Iterable[(String, String)] = Seq.empty)(f: => A): A =
    val dataset = spark.read.format("libsvm").load("data/sample_libsvm_data.txt")
    val payload2 = dataset.toJSON.head
    val payload = spark.createDataFrame(
      Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))
    ).toDF("id", "hour", "mobile", "userFeatures", "clicked").toJSON.head
    post("/invocations", body = payload2.getBytes()) {
      println(response.body)
      status should equal (200)
    }
  }

}
