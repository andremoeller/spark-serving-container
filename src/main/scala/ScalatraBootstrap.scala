import com.amazonaws.spark.serving._
import org.scalatra._
import javax.servlet.ServletContext

import org.apache.spark.ml.linalg.SQLDataTypes.VectorType
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

class ScalatraBootstrap extends LifeCycle with SparkModelLoader {

  /*
    VectorAssembler tries to evaluate the head of the DataFrame if it doesn't find metadata for a Vector field, which
    creates another Spark job. Adding the Metadata to the Vector field is a hack to coerce the VectorAssembler not to
    evaluate the first record. In the context of a large Spark job, the latency added by this additional Spark job
    is negligible, but the additional 0.2-1.5 seconds (depending on the size of the record)
    this adds is important for a serving container like this.

    These strings are from org.apache.spark.ml.attribute.AttributeKeys and used in org.apache.spark.ml.attribute.AttributeGroup

    See org.apache.spark.ml.feature.VectorAssembler#transformSchema
   */
  private val AttributeKey = "attrs"
  private val NumAttributesKey = "num_attrs"
  private val MlAttributeKey = "ml_attr"

  private val attributes : Metadata = new MetadataBuilder().putMetadata(AttributeKey, Metadata.empty)
    .putLong(NumAttributesKey, 1).build()
  private val metadata : Metadata = new MetadataBuilder().putMetadata(MlAttributeKey, attributes).build()

  private val spark = SparkSession.builder
    .config("spark.submit.deployMode", "client")
    .config("spark.logConf", true)
    .appName("SparkInferenceServlet")

    // Spark local mode with max 3 retries.
    .master("local[*, 3]")
    .getOrCreate

  // Input DataFrame's schema to deserialize JSON.
  // TODO: don't hardcode this. This is just for demonstration.
  val schema = StructType(Array(
    StructField("label", DoubleType),
    StructField("features", VectorType, metadata = metadata)))

  val InferenceServlet = new SparkInferenceServlet(loadModel(), schema, spark)

  override def init(context: ServletContext) {
    context.mount(InferenceServlet, "/*")
  }
}
