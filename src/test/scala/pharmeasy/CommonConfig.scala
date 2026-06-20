package pharmeasy

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import java.nio.file.Paths
import scala.collection.immutable.ListMap

object CommonConfig {
  val urls: ListMap[String, String] = ListMap(
    "home"                -> "https://pharmeasy.in/",
    "online_medicine"     -> "https://pharmeasy.in/online-medicine-order?src=homecard",
    "diagnostics"         -> "https://pharmeasy.in/diagnostics",
    "blog"                -> "https://pharmeasy.in/blog/",
    "healthcare_category" -> "https://pharmeasy.in/health-care/9066?src=homecard",
    "cart"                -> "https://pharmeasy.in/cart?src=header",
    "diag_cart"           -> "https://pharmeasy.in/diag-pwa/cart",
    "pdp"                 -> "https://pharmeasy.in/online-medicine-order/telma-40mg-strip-of-30-tablets-12024#otherProducts"
  )

  def rampUsersFromEnv(key: String, default: Int): Int =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toInt).toOption).getOrElse(default)

  def durationFromEnvSeconds(key: String, defaultSeconds: Int): Int =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toInt).toOption).getOrElse(defaultSeconds)

  def doubleFromEnv(key: String, default: Double): Double =
    Option(System.getenv(key)).flatMap(s => util.Try(s.toDouble).toOption).getOrElse(default)

  def stringFromEnv(key: String, default: String): String =
    Option(System.getenv(key)).map(_.trim).filter(_.nonEmpty).getOrElse(default)

  private def truthyString(s: String): Boolean = {
    val z = s.trim.toLowerCase(java.util.Locale.ROOT)
    z == "true" || z == "1" || z == "yes"
  }

  def envEnabled(key: String): Boolean =
    Option(System.getenv(key)).exists(s => truthyString(s))

  def envDisabled(key: String): Boolean = {
    val z = Option(System.getenv(key)).map(_.trim.toLowerCase(java.util.Locale.ROOT)).getOrElse("")
    z == "0" || z == "false" || z == "no" || z == "off"
  }

  val requestDebugEnabled: Boolean =
    Option(System.getProperty("gatling.request.debug")).exists(truthyString) ||
      envEnabled("GATLING_DEBUG") ||
      envEnabled("DEBUG")

  private def disablePharmeasyCookie: Boolean = envEnabled("DISABLE_PHARMEASY_COOKIE")

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
        val xt  = Option(System.getenv("X_ACCESS_TOKEN")).map(_.trim).filter(_.nonEmpty)
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

  /** Shared HTTP protocol for browser-style page simulations (Combined + Individual). */
  def browserHttpProtocol: HttpProtocolBuilder = {
    val base = HeavyLoadHttpProtocol
      .tune(http)
      .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36")
      .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
      .acceptEncodingHeader("gzip, deflate, br")
      .acceptLanguageHeader("en-US,en;q=0.9")
    cookieHeader.fold(base)(c => base.header("Cookie", c))
  }
}
