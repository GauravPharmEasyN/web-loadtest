package pharmeasy

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Load test for selected JSON APIs on `pharmeasy.in`.
 *
 * **Default (throughput):** aggregate [[API_RPS]] (default 200), split evenly across four parallel scenarios
 * (`constantUsersPerSec` per scenario).
 *
 * **Dry run / smoke:** set `API_USERS` (positive int). One scenario hits all four APIs in order with
 * `rampUsers(API_USERS).during(API_DURATION_SECS)` (e.g. `API_USERS=2` `API_DURATION_SECS=5`).
 *
 * Only `GET /api/cart/getCartCount` sends `Cookie` when any of `CART_COOKIE`, `PHARMEASY_COOKIE`, or
 * `X_ACCESS_TOKEN` is set (full resolution chain via [[CommonConfig.cookieHeader]]).
 *
 * Env: `API_RPS`, `API_USERS`, `API_DURATION_SECS`, `API_PINCODE`, `API_OTC_ID`, plus all cookie vars
 * from [[CommonConfig]] with `CART_COOKIE` taking highest priority for the cart endpoint.
 */
class PharmeasyApiSimulation extends Simulation {

  private val durationSecs: Int   = CommonConfig.durationFromEnvSeconds("API_DURATION_SECS", 600)
  private val totalRps: Double    = CommonConfig.doubleFromEnv("API_RPS", 200.0)
  private val pincodeValue: String = CommonConfig.stringFromEnv("API_PINCODE", "400602")
  private val otcIdValue: String   = CommonConfig.stringFromEnv("API_OTC_ID", "3491142")

  /** When set to a positive int, use ramp-user dry run instead of constant RPS. */
  private val apiUsersDryRun: Option[Int] =
    Option(System.getenv("API_USERS")).flatMap(s => util.Try(s.toInt).toOption).filter(_ > 0)

  // CART_COOKIE takes priority; falls through to the full CommonConfig chain (PHARMEASY_COOKIE,
  // X_ACCESS_TOKEN+XDI, ACCESS_TOKEN, cookie file) so env var behaviour is consistent everywhere.
  private val cartCookie: Option[String] =
    Option(System.getenv("CART_COOKIE")).map(_.trim).filter(_.nonEmpty)
      .orElse(CommonConfig.cookieHeader)

  private val httpProtocol = HeavyLoadHttpProtocol.tune(
    http
      .baseUrl("https://pharmeasy.in")
      .acceptHeader("application/json, text/plain, */*")
      .userAgentHeader("Mozilla/5.0 (compatible; PharmeasyApiSimulation/1.0)")
  )

  private val rpsEach: Double = totalRps / 4.0

  private val reqCategories =
    http("fetchCategories")
      .get("/api/home/fetchCategories")
      .check(status.in(200, 204))
      .check(jsonPath("$").exists)

  private val reqPincode =
    http("fetchPincodeDetails")
      .get(s"/api/app/fetchPincodeDetails?pincode=$pincodeValue")
      .check(status.in(200, 204))
      .check(jsonPath("$").exists)

  private val reqOtc =
    http("fetchOtcEdd")
      .get(s"/api/otc/fetchOtcEdd/$otcIdValue")
      .check(status.in(200, 204, 404))
      .check(jsonPath("$").exists)

  private val reqCart = {
    val builder = http("getCartCount").get("/api/cart/getCartCount")
    cartCookie.fold(builder)(c => builder.header("Cookie", c))
      .check(status.in(200, 401))
      .check(jsonPath("$").exists)
  }

  private val scnCategories = scenario("fetchCategories").exec(reqCategories)
  private val scnPincode    = scenario("fetchPincodeDetails").exec(reqPincode)
  private val scnOtc        = scenario("fetchOtcEdd").exec(reqOtc)
  private val scnCart       = scenario("getCartCount").exec(reqCart)

  /** All four APIs once per virtual user, in order (for dry run). */
  private val scnSmokeAll =
    scenario("api_smoke_all").exec(reqCategories, reqPincode, reqOtc, reqCart)

  private val injectionThroughput =
    constantUsersPerSec(rpsEach).during(durationSecs.seconds)

  apiUsersDryRun match {
    case Some(n) =>
      setUp(
        scnSmokeAll.inject(rampUsers(n).during(durationSecs.seconds))
      ).protocols(httpProtocol)

    case None =>
      setUp(
        scnCategories.inject(injectionThroughput),
        scnPincode.inject(injectionThroughput),
        scnOtc.inject(injectionThroughput),
        scnCart.inject(injectionThroughput)
      ).protocols(httpProtocol)
  }
}
