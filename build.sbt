val sharedSettings = Seq(
  scalaVersion := "2.12.2",
  scalacOptions ++= Seq(
    "-Xlint",
    "-Ypartial-unification",
    "-feature",
    "-language:higherKinds"
  ),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(_ == "-Xlint")
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(_ == "-Xlint")
  },
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core" % "0.8.0",
    "io.circe" %% "circe-jawn" % "0.8.0",
    "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
    "org.scalatest" %% "scalatest" % "3.0.3" % Test
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
)

val root = project.in(file("."))
  .settings(sharedSettings)
  .aggregate(core, benchmarks)
  .dependsOn(core)

lazy val core = project.in(file("core"))
  .settings(sharedSettings)

lazy val benchmarks = project.in(file("benchmarks"))
  .settings(sharedSettings)
  .settings(mainClass in assembly := Some("io.circe.algebra.benchmarks.DecodingApp"))
  .enablePlugins(JmhPlugin)
  .dependsOn(core)
