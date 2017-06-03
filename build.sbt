val circeVersion = "0.9.0-M2"

val sharedSettings = Seq(
  scalaVersion := "2.12.4",
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
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-jawn" % circeVersion,
    "io.circe" %% "circe-testing" % circeVersion % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.typelevel" %% "discipline" % "0.8" % Test,
    "org.typelevel" %% "cats-free" % "1.0.0-RC1",
    "org.typelevel" %% "cats-laws" % "1.0.0-RC1" % Test
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
)

val root = project.in(file("."))
  .settings(sharedSettings)
  .settings(libraryDependencies += "io.circe" %% "circe-literal" % circeVersion)
  .aggregate(core, benchmarks)
  .dependsOn(core)

lazy val core = project.in(file("core"))
  .settings(sharedSettings)

lazy val benchmarks = project.in(file("benchmarks"))
  .settings(sharedSettings)
  .settings(mainClass in assembly := Some("io.circe.algebra.benchmarks.DecodingApp"))
  .enablePlugins(JmhPlugin)
  .dependsOn(core)
