package com.amazonaws.spark.serving

import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType
import org.scalatra._

class SparkInferenceServlet(val model: Model[_], val schema : StructType) extends ScalatraServlet {

  val spark = SparkSession.builder().master("local").getOrCreate

  post("/invocations") {
    require("application/json".equals(request.getContentType),
      "The Spark serving container expects requests with application/json content type.")

    val body = request.body
    println("BODY")
    println(body)

    import spark.implicits._

    // DataFrames serialized to JSON aren't correctly deserialized into a DataFrame as Vectors,
    // hence the schema.
    val df = spark.read.schema(schema).json(Seq(body).toDS)
    response.setContentType("application/json")
    val resp = model.transform(df).toJSON.collect().mkString
    resp
  }

  get("/ping") {
    "Ok"
  }

}
