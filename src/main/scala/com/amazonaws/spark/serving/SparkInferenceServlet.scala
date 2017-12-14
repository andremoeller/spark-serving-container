package com.amazonaws.spark.serving

import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType
import org.scalatra._

class SparkInferenceServlet(val model: Model[_], val schema : StructType) extends ScalatraServlet {

  val spark = SparkSession.builder().master("local[*]").getOrCreate

  post("/invocations") {
    require("application/json".equals(request.getContentType),
      "The Spark serving container expects requests with application/json content type.")
    val body = request.body
    import spark.implicits._
    // DataFrames serialized to JSON aren't correctly deserialized into a DataFrame as Vectors, so the schema is needed.
    val df = spark.read.schema(schema).json(Seq(body).toDS)
    response.setContentType("application/json")
    model.transform(df).toJSON.collect().mkString
  }

  get("/ping") {
    // Ping should return 200 when the model is loaded and ready for inference.
    model.explainParams
    "Ok"
  }

}
