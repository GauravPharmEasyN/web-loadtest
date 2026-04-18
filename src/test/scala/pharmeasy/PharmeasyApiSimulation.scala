package pharmeasy

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Load test for selected JSON APIs on `pharmeasy.in`.
 *
 * Aggregate target rate: [[API_RPS]] (default 200), split evenly across four endpoints.
 * Only `GET /api/cart/getCartCount` sends a `Cookie` header when any of
 * `CART_COOKIE`, `PHARMEASY_COOKIE`, or `X_ACCESS_TOKEN` is set; the other three calls are unauthenticated.
 *
 * Env: `API_RPS`, `API_DURATION_SECS`, `API_PINCODE`, `API_OTC_ID`, plus cookie vars above.
 */
class PharmeasyApiSimulation extends Simulation {

  private val durationSecs: Int =
    Option(System.getenv("API_DURATION_SECS")).flatMap(s => util.Try(s.toInt).toOption).getOrElse(600)

  private val totalRps: Double =
    Option(System.getenv("API_RPS")).flatMap(s => util.Try(s.toDouble).toOption).getOrElse(200.0)

  private val pincodeValue: String =
    Option(System.getenv("API_PINCODE")).map(_.trim).filter(_.nonEmpty).getOrElse("400602")

  private val otcIdValue: String =
    Option(System.getenv("API_OTC_ID")).map(_.trim).filter(_.nonEmpty).getOrElse("3491142")

  private val cartCookie: Option[String] =
    Option(System.getenv("CART_COOKIE")).map(_.trim).filter(_.nonEmpty)
      .orElse(Option(System.getenv("PHARMEASY_COOKIE")).map(_.trim).filter(_.nonEmpty))
      .orElse(
        Option(System.getenv("X_ACCESS_TOKEN")).map(_.trim).filter(_.nonEmpty).map(t => s"X-Access-Token=$t")
      )

  private val httpProtocol = HeavyLoadHttpProtocol.tune(
    http
      .baseUrl("https://pharmeasy.in")
      .acceptHeader("application/json, text/plain, */*")
      .userAgentHeader("Mozilla/5.0 (compatible; PharmeasyApiSimulation/1.0)")
  )

  private val rpsEach: Double = totalRps / 4.0

  private val scnCategories = scenario("fetchCategories").exec(
    http("fetchCategories")
      .get("/api/home/fetchCategories")
      .check(status.in(200, 204, 401, 403, 404))
  )

  private val scnPincode = scenario("fetchPincodeDetails").exec(
    http("fetchPincodeDetails")
      .get(s"/api/app/fetchPincodeDetails?pincode=$pincodeValue")
      .check(status.in(200, 204, 401, 403, 404))
  )

  private val scnOtc = scenario("fetchOtcEdd").exec(
    http("fetchOtcEdd")
      .get(s"/api/otc/fetchOtcEdd/$otcIdValue")
      .check(status.in(200, 204, 401, 403, 404))
  )

  private val scnCart = scenario("getCartCount").exec(
    cartCookie match {
      case Some(cookie) =>
        http("getCartCount")
          .get("/api/cart/getCartCount")
          .header("Cookie", cookie)
          .check(status.in(200, 204, 401, 403, 404))
      case None =>
        http("getCartCount")
          .get("/api/cart/getCartCount")
          .check(status.in(200, 204, 401, 403, 404))
    }
  )

  private val injection = constantUsersPerSec(rpsEach).during(durationSecs.seconds)

  setUp(
    scnCategories.inject(injection),
    scnPincode.inject(injection),
    scnOtc.inject(injection),
    scnCart.inject(injection)
  ).protocols(httpProtocol)
}
