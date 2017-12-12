package com.amazonaws.spark.serving

import org.scalatest.FlatSpec

class SparkModelLoaderTests extends FlatSpec with SparkModelLoader {

  it should "load a spark model" in {
    println(loadModel("test-pipeline-model").explainParams)
  }
}
