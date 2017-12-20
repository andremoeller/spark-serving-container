package com.amazonaws.spark.serving

import org.apache.spark.ml.Model
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType
import org.scalatra._

class SparkInferenceServlet(val model: Model[_], val schema : StructType, val spark: SparkSession) extends ScalatraServlet {

  post("/invocations") {
    require("application/json".equals(request.getContentType),
      "The Spark serving container expects requests with application/json content type.")

    val body = request.body.trim
    import spark.implicits._

    response.setContentType("application/json")

    val df = spark.read.schema(schema).json(body.split("\n").toList.toDS)

    // Only return the prediction columns.
    val predictions = model
      .transform(df)
      .drop(schema.fieldNames : _*)
      .drop("assembledFeatures")
      .toJSON
      .collect()

    predictions.mkString("\n")
  }

  get("/ping") {
    // Ping should return 200 when the model is loaded and ready for inference.
    model.explainParams +  "\nOk"
  }

}
