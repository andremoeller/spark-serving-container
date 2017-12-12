package com.amazonaws.spark.serving

import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession
import org.scalatra._

class SparkInferenceServlet(val model: Model[_]) extends ScalatraServlet {

  val spark = SparkSession.builder().master("local").getOrCreate

  post("/invocations") {
    val body = request.body
    println(body)

  }

  get("/ping") {
    model.toString() + model.explainParams()
  }

  get("/") {
    views.html.hello()
  }

}
