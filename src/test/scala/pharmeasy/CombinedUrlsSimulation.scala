package pharmeasy

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CombinedUrlsSimulation extends Simulation {

  private val httpProtocol = http
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.9")

  private val urlsSeq = CommonConfig.urls.toSeq
  private val feeder = Iterator.continually(Map("_urlPair" -> urlsSeq(scala.util.Random.nextInt(urlsSeq.length))))
  private val totalUsers = CommonConfig.rampUsersFromEnv("COMBINED_USERS", 1)
  private val durationSecs = CommonConfig.durationFromEnvSeconds("COMBINED_DURATION_SECS", 2)

  private val scn = scenario("Combined URLs Random GET")
    .feed(feeder)
    .exec { session =>
      val (name, url) = session("_urlPair").as[(String, String)]
      session.set("urlName", name).set("url", url)
    }
    .exec(
      http("GET - ${urlName}")
        .get("${url}")
        .check(status.in(200, 301, 302))
    )

  setUp(
    scn.inject(rampUsers(totalUsers).during(durationSecs.seconds))
  ).protocols(httpProtocol)
}
