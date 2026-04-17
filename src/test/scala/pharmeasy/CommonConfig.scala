package pharmeasy

import io.gatling.core.Predef._

import java.nio.file.Paths

object CommonConfig {
  val urls: Map[String, String] = Map(
    "home" -> "https://pharmeasy.in/",
    "online_medicine" -> "https://pharmeasy.in/online-medicine-order?src=homecard",
    "diagnostics" -> "https://pharmeasy.in/diagnostics",
    "blog" -> "https://pharmeasy.in/blog/",
    "healthcare_category" -> "https://pharmeasy.in/health-care/9066?src=homecard",
    "cart" -> "https://pharmeasy.in/cart?src=header",
    "diag_cart" -> "https://pharmeasy.in/diag-pwa/cart",
    "pdp" -> "https://pharmeasy.in/online-medicine-order/telma-40mg-strip-of-30-tablets-12024#otherProducts"
  )

  def rampUsersFromEnv(key: String, default: Int): Int =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toInt).toOption).getOrElse(default)

  def durationFromEnvSeconds(key: String, defaultSeconds: Int): Int =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toInt).toOption).getOrElse(defaultSeconds)

  private def truthyString(s: String): Boolean = {
    val z = s.trim.toLowerCase(java.util.Locale.ROOT)
    z == "true" || z == "1" || z == "yes"
  }

  /**
   * Log each outgoing GET (method, URL, optional Cookie) to logger {@code pharmeasy.request}.
   * Enable with any of: {@code -Dgatling.request.debug=true}, {@code GATLING_DEBUG=true},
   * {@code DEBUG=true}. Disable by unsetting / setting to false.
   */
  val requestDebugEnabled: Boolean =
    Option(System.getProperty("gatling.request.debug")).exists(truthyString) ||
      Option(System.getenv("GATLING_DEBUG")).exists(truthyString) ||
      Option(System.getenv("DEBUG")).exists(truthyString)

  private def disablePharmeasyCookie: Boolean =
    Option(System.getenv("DISABLE_PHARMEASY_COOKIE")).exists(truthyString)

  /** First non-comment line from {@code config/pharmeasy-default-cookie.txt} under the project root. */
  private def loadDefaultCookieFile(): Option[String] = {
    val f = Paths.get(System.getProperty("user.dir"), "config", "pharmeasy-default-cookie.txt").toFile
    if (!f.isFile) None
    else {
      val src = scala.io.Source.fromFile(f, "UTF-8")
      try src.getLines().map(_.trim).find(l => l.nonEmpty && !l.startsWith("#"))
      finally src.close()
    }
  }

  /**
   * Full value for the HTTP `Cookie` header (no `Cookie:` prefix).
   * Resolution order:
   *   0. If `DISABLE_PHARMEASY_COOKIE` is true — no cookie.
   *   1. `PHARMEASY_COOKIE` — full cookie string.
   *   2. `X_ACCESS_TOKEN` and optional `XDI` — `X-Access-Token=...; XdI=...` or token only.
   *   3. `ACCESS_TOKEN` only — legacy `accessToken=...`
   *   4. Else first non-comment line of `config/pharmeasy-default-cookie.txt` (repo default for all URLs).
   */
  val cookieHeader: Option[String] = {
    if (disablePharmeasyCookie) None
    else {
      val fromPharm = Option(System.getenv("PHARMEASY_COOKIE")).map(_.trim).filter(_.nonEmpty)
      if (fromPharm.nonEmpty) fromPharm
      else {
        val xt = Option(System.getenv("X_ACCESS_TOKEN")).map(_.trim).filter(_.nonEmpty)
        val xdi = Option(System.getenv("XDI")).map(_.trim).filter(_.nonEmpty)
        val fromParts: Option[String] = (xt, xdi) match {
          case (Some(t), Some(d)) => Some(s"X-Access-Token=$t; XdI=$d")
          case (Some(t), None)    => Some(s"X-Access-Token=$t")
          case _ =>
            Option(System.getenv("ACCESS_TOKEN")).map(_.trim).filter(_.nonEmpty).map(t => s"accessToken=$t")
        }
        fromParts.orElse(loadDefaultCookieFile())
      }
    }
  }

}
