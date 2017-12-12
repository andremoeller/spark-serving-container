package com.amazonaws.spark.serving

import org.apache.spark.ml.{Model, PipelineModel}
import org.apache.spark.sql.SparkSession

trait SparkModelLoader {
  // TODO: Read this path from environment variable in container.
  def loadModel(modelPath: String = "test-pipeline-model") : Model[_] = {
    val spark = SparkSession.builder().master("local").getOrCreate
    PipelineModel.load(modelPath)
  }
}