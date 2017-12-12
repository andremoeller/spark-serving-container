package com.amazonaws.spark.serving

import org.apache.hadoop.conf.Configuration
import org.apache.spark.ml.{Model, PipelineModel}
import org.apache.spark.sql.SparkSession

trait SparkModelLoader {
  // TODO: Read this path from environment variable in container.
  def loadModel(modelPath: String = "test-pipeline-model") : Model[_] = {
    val spark = SparkSession.builder().master("local").getOrCreate
    val hadoopConfig: Configuration = spark.sparkContext.hadoopConfiguration
    //
    hadoopConfig.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)
    PipelineModel.load(modelPath)
  }
}