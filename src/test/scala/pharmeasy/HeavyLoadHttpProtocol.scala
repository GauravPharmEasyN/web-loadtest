package pharmeasy

import java.net.InetSocketAddress

import io.gatling.http.protocol.HttpProtocolBuilder

/**
 * Tuning for very high concurrency against a small set of HTTPS hosts (e.g. pharmeasy.in on CloudFront):
 *  - [[shareConnections]] — one global pool per JVM → far fewer sockets / FDs (mitigates "Too many open files").
 *  - Optional async DNS — fewer [[java.net.UnknownHostException]] storms vs the JDK resolver under load.
 *    Disable if your network blocks outbound DNS (e.g. `GATLING_ASYNC_DNS=false`).
 */
object HeavyLoadHttpProtocol {

  private val publicDns: Array[InetSocketAddress] = Array(
    new InetSocketAddress("8.8.8.8", 53),
    new InetSocketAddress("8.8.4.4", 53),
    new InetSocketAddress("1.1.1.1", 53)
  )

  private def asyncDnsEnabled: Boolean =
    !Option(System.getenv("GATLING_ASYNC_DNS")).map(_.trim.toLowerCase).exists(s => s == "0" || s == "false" || s == "no")

  def tune(builder: HttpProtocolBuilder): HttpProtocolBuilder = {
    val shared = builder.shareConnections
    if (asyncDnsEnabled) shared.asyncNameResolution(publicDns)
    else shared
  }
}
