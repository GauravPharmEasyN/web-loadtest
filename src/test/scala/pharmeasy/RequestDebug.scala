package pharmeasy

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.slf4j.LoggerFactory

/** One-line request logs when {@link CommonConfig#requestDebugEnabled} is true. */
object RequestDebug {
  private val log = LoggerFactory.getLogger("pharmeasy.request")

  def logOutgoingCombined(): ChainBuilder =
    exec { session =>
      if (CommonConfig.requestDebugEnabled) {
        val url = session("url").as[String]
        val urlName = session("urlName").as[String]
        val cookie = CommonConfig.cookieHeader.map(c => s" | Cookie: $c").getOrElse("")
        log.info(s"REQUEST GET $url | mode=combined | page=$urlName$cookie")
      }
      session
    }

  def logOutgoingIndividual(pageKey: String, url: String): ChainBuilder =
    exec { session =>
      if (CommonConfig.requestDebugEnabled) {
        val cookie = CommonConfig.cookieHeader.map(c => s" | Cookie: $c").getOrElse("")
        log.info(s"REQUEST GET $url | mode=individual | page=$pageKey$cookie")
      }
      session
    }
}
