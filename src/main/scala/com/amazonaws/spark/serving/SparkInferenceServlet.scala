package com.amazonaws.spark.serving

import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession
import org.scalatra._

class SparkInferenceServlet(val model: Model[_]) extends ScalatraServlet {

  val spark = SparkSession.builder().master("local").getOrCreate

  post("/invocations") {
    println("invocations")
    val body = request.body
    val df = spark.read.json(body)
    println(body)
    df.show()
    val sb = new StringBuilder()
    val responseBody = model.transform(df).toJSON.collect().mkString
    println("responseBody")
    println(responseBody)
    responseBody
  }

  get("/ping") {
    model.toString() + model.explainParams()
  }

  get("/") {
    views.html.hello()
  }

}
