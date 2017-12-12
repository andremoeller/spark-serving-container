import com.amazonaws.spark.serving._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with SparkModelLoader {
  override def init(context: ServletContext) {
    context.mount(new SparkInferenceServlet(loadModel()), "/*")
  }
}
