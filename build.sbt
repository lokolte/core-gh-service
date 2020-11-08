name := "core-gh-service"
organization := "core.gh"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin)

scalaVersion := "2.13.3"

// This will set the name of the zip file under /target/universal
packageName in Universal := name.value

coverageExcludedPackages := "router.*;<empty>;Reverse.*"
coverageMinimum := 90.01
coverageFailOnMinimum := true

// This will create the "public/version.json" file.
ProjectVersionProperties.buildVersionSettings

libraryDependencies ++= Seq(
  guice,
  ws,
  caffeine,
  "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.0.0" % Test,
  "org.webjars"             % "swagger-ui"               % "3.25.0",
  "org.mockito"             % "mockito-core"             % "3.3.3",
  "net.logstash.logback"    % "logstash-logback-encoder" % "6.3",
  "ch.qos.logback"          % "logback-classic"          % "1.2.3" % Test,
  "com.typesafe.akka"       %% "akka-testkit"            % "2.6.1" % Test,
  "de.leanovate.play-mockws" %% "play-mockws" % "2.8.0" % Test,
)

scalafmtOnCompile := true

swaggerDomainNameSpaces := Seq("core.gh.models")
swaggerV3 := true
swaggerTarget := baseDirectory.value / "public"
swaggerAPIVersion := version.value

parallelExecution in Test := false
