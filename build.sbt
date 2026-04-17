ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file(".")).enablePlugins(GatlingPlugin).settings(
  name := "pharmeasy-web-loadtest",
  version := "0.1.0",
  Test / fork := true,
  // Gatling runs in the Gatling sbt configuration (not plain Test); that fork defaults to -Xmx1G.
  // Append after those defaults so e.g. -Xmx8g overrides -Xmx1G. Putting -Xms on Test/javaOptions
  // alone could leave -Xms2g with a later -Xmx1G → "Initial heap size larger than maximum heap size".
  Gatling / javaOptions ++= sys.env.get("GATLING_JAVA_OPTS").toSeq.flatMap(_.trim.split("\\s+").filter(_.nonEmpty)),
  libraryDependencies ++= Seq(
    "io.gatling" % "gatling-test-framework" % "3.10.5" % Test,
    "io.gatling" % "gatling-http" % "3.10.5" % Test,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.10.5" % Test
  )
)
