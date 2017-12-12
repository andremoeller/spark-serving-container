import com.amazonaws.spark.serving._
import org.scalatra._
import javax.servlet.ServletContext

import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.sql.types.{DoubleType, StructField, StructType}

class ScalatraBootstrap extends LifeCycle with SparkModelLoader {
  override def init(context: ServletContext) {
    context.mount(new SparkInferenceServlet(loadModel(),
      // Input DataFrame's schema to deserialize JSON.
      StructType(Array(
      StructField("label", DoubleType),
      StructField("features", VectorType)))), "/*")
  }
}
