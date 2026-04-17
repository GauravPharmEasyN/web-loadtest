ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file(".")).enablePlugins(GatlingPlugin).settings(
  name := "pharmeasy-web-loadtest",
  version := "0.1.0",
  Test / fork := true,
  // Forked Gatling JVM only; whitespace-separated tokens (avoid spaces inside one flag).
  Test / javaOptions ++= sys.env.get("GATLING_JAVA_OPTS").toSeq.flatMap(_.trim.split("\\s+").filter(_.nonEmpty)),
  libraryDependencies ++= Seq(
    "io.gatling" % "gatling-test-framework" % "3.10.5" % Test,
    "io.gatling" % "gatling-http" % "3.10.5" % Test,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.10.5" % Test
  )
)
