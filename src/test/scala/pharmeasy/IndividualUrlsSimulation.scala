package pharmeasy

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class IndividualUrlsSimulation extends Simulation {

  private val httpProtocol = CommonConfig.browserHttpProtocol

  private def buildScenario(name: String, url: String, envPrefix: String) = {
    val rampUsersCount = CommonConfig.rampUsersFromEnv(s"${envPrefix}_USERS", 10)
    val durationSecs   = CommonConfig.durationFromEnvSeconds(s"${envPrefix}_DURATION_SECS", 60)

    val scn = scenario(s"GET ${name}")
      .exec(RequestDebug.logOutgoingIndividual(name, url))
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
  private val pdp        = buildScenario("pdp", CommonConfig.urls("pdp"), "PDP")

  setUp(
    home,
    medicine,
    diagnostics,
    blog,
    category,
    cart,
    diagCart,
    pdp
  ).protocols(httpProtocol)
}
