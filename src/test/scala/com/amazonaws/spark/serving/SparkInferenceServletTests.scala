package com.amazonaws.spark.serving

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatra.test.scalatest._
import org.scalatest.FunSuiteLike

class SparkInferenceServletTests extends ScalatraSuite with FunSuiteLike with SparkModelLoader {

  val spark : SparkSession = SparkSession.builder
    .master("local[*]")
    .getOrCreate()

  val dataset : DataFrame = spark.read.format("libsvm").load("data/sample_libsvm_data.txt")

  val pipelineModelPath = "test-pipeline-model"

  addServlet(new SparkInferenceServlet(loadModel(pipelineModelPath), dataset.schema), "/*")

  test("GET /ping on SparkInferenceServlet should return status 200 and explain params") {
    get("/ping") {
      status should equal (200)
    }
  }

  test("POST /invocations on SparkInferenceServlet should make an inference") {
    val payload = dataset.toJSON.head
    post("/invocations", body = payload.getBytes(), headers = Map("Content-Type"->"application/json")) {
      response.body should include ("prediction")
      response.getContentType should include ("application/json")
      status should equal (200)
    }
  }

}
