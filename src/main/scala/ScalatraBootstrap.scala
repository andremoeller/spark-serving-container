import com.amazonaws.spark.serving._
import org.scalatra._
import javax.servlet.ServletContext

import org.apache.spark.SparkConf
import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}

class ScalatraBootstrap extends LifeCycle with SparkModelLoader {
  override def init(context: ServletContext) {

    val conf = new SparkConf().registerKryoClasses(Array(
      classOf[org.apache.spark.ml.linalg.Vector],
      classOf[org.apache.spark.ml.linalg.DenseVector],
      classOf[org.apache.spark.ml.linalg.SparseVector]))

    val spark = SparkSession.builder
      .config(conf)
      .config("spark.submit.deployMode", "client")
      .config("spark.logConf", true)
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .appName("SparkInferenceServlet")
      // Spark local mode with max 3 retries.
      .master("local[*, 3]")
      .getOrCreate
    context.mount(new SparkInferenceServlet(loadModel(),
      // Input DataFrame's schema to deserialize JSON.
      // TODO: don't hardcode this. This is just for demonstration.
      StructType(Array(
        StructField("label", DoubleType),
        StructField("features", VectorType))),
      spark), "/*")
  }
}
