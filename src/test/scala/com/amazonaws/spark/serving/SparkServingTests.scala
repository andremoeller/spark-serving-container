package com.amazonaws.spark.serving

package com.amazonaws.spark.serving

import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.SparkSession
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class SparkServingTests extends FlatSpec with MockitoSugar with BeforeAndAfter {

  val modelPath = "test-pipeline-model"

  val spark = SparkSession.builder
    .master("local")
    .appName("spark session")
    .getOrCreate()

  val dataset = spark.createDataFrame(
    Seq((0, 18, 1.0, Vectors.dense(0.0, 10.0, 0.5), 1.0))
  ).toDF("id", "hour", "mobile", "userFeatures", "clicked")

  ignore should "use vector assembler, random forest" in {
    dataset.show()

    val assembler = new VectorAssembler()
      .setInputCols(Array("hour", "mobile", "userFeatures"))
      .setOutputCol("features")

    val randomForest = new RandomForestClassifier()
      .setLabelCol("clicked")
      .setFeaturesCol("features")

    val pipeline = new Pipeline().setStages(Array(assembler, randomForest))
    val model = pipeline.fit(dataset)
    val out = model.transform(dataset)
    out.show()

    model.write.overwrite().save(modelPath)

    val loadedPipelineModel = PipelineModel.load(modelPath)

    val loadedOut = loadedPipelineModel.transform(dataset)
    loadedOut.show()
  }

  it should "use vector assembler, random forest with libsvm dataset" in {
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

  // TODO: for tests only.
  //  def savePipelineModelToTarGZ(pipelineModelPath: String) = {
  //    val gzipCompressorOutputStream = new GzipCompressorOutputStream()
  //    val tarArchiveInputStream = new TarArchiveOutputStream()
  //  }
}