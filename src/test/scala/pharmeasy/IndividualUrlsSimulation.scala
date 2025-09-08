package pharmeasy

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class IndividualUrlsSimulation extends Simulation {

  private val httpProtocol = http
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.9")

  private def buildScenario(name: String, url: String, envPrefix: String) = {
    val rampUsersCount = CommonConfig.rampUsersFromEnv(s"${envPrefix}_USERS", 10)
    val durationSecs   = CommonConfig.durationFromEnvSeconds(s"${envPrefix}_DURATION_SECS", 60)

    val scn = scenario(s"GET ${name}")
      .exec(
        http(s"GET ${name}")
          .get(url)
          .check(status.in(200, 301, 302))
      )

    scn.inject(
      rampUsers(rampUsersCount).during(durationSecs.seconds)
    )
  }

  private val home       = buildScenario("home", CommonConfig.urls("home"), "HOME")
  private val medicine   = buildScenario("online_medicine", CommonConfig.urls("online_medicine"), "MEDICINE")
  private val diagnostics= buildScenario("diagnostics", CommonConfig.urls("diagnostics"), "DIAG")
  private val blog       = buildScenario("blog", CommonConfig.urls("blog"), "BLOG")
  private val category   = buildScenario("healthcare_category", CommonConfig.urls("healthcare_category"), "HCAT")
  private val cart       = buildScenario("cart", CommonConfig.urls("cart"), "CART")
  private val diagCart   = buildScenario("diag_cart", CommonConfig.urls("diag_cart"), "DCART")

  setUp(
    home,
    medicine,
    diagnostics,
    blog,
    category,
    cart,
    diagCart
  ).protocols(httpProtocol)
}
