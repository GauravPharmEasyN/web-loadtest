package pharmeasy

import io.gatling.core.Predef._

object CommonConfig {
  val urls: Map[String, String] = Map(
    "home" -> "https://pharmeasy.in/",
    "online_medicine" -> "https://pharmeasy.in/online-medicine-order?src=homecard",
    "diagnostics" -> "https://pharmeasy.in/diagnostics",
    "blog" -> "https://pharmeasy.in/blog/",
    "healthcare_category" -> "https://pharmeasy.in/health-care/9066?src=homecard",
    "cart" -> "https://pharmeasy.in/cart?src=header",
    "diag_cart" -> "https://pharmeasy.in/diag-pwa/cart"
  )

  def rampUsersFromEnv(key: String, default: Int): Int =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toInt).toOption).getOrElse(default)

  def durationFromEnvSeconds(key: String, defaultSeconds: Int): Int =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toInt).toOption).getOrElse(defaultSeconds)
}
