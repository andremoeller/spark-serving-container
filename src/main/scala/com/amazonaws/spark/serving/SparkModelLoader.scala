package com.amazonaws.spark.serving

import org.apache.spark.ml.{Model, PipelineModel}
import org.apache.spark.sql.SparkSession

trait SparkModelLoader {
  def loadModel(modelPath: String = System.getenv("MODEL_PATH")) : Model[_] = {
    val spark = SparkSession.builder.getOrCreate
    PipelineModel.load(modelPath)
  }
}