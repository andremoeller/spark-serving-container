package com.amazonaws.spark.serving

import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.SparkSession
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class SparkPipelineModelTests extends FlatSpec with MockitoSugar with BeforeAndAfter {

  val modelPath = "test-pipeline-model"

  val spark = SparkSession.builder.master("local").getOrCreate()

  // This just generated the test-pipeline-model. It doesn't test anything in the model container.
  ignore should "use vector assembler, random forest with libsvm dataset" in {
    val dataset = spark.read.format("libsvm").load("data/sample_libsvm_data.txt")
    dataset.show()

    val assembler = new VectorAssembler()
      .setInputCols(Array("features"))
      .setOutputCol("assembledFeatures")

    val randomForest = new RandomForestClassifier()
      .setLabelCol("label")
      .setFeaturesCol("assembledFeatures")

    val pipeline = new Pipeline().setStages(Array(assembler, randomForest))
    val model = pipeline.fit(dataset)
    val out = model.transform(dataset)
    out.show()

    model.write.overwrite().save(modelPath)

    val loadedPipelineModel = PipelineModel.load(modelPath)

    val loadedOut = loadedPipelineModel.transform(dataset)
    loadedOut.show()
  }
}