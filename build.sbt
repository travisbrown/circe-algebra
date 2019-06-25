organization in ThisBuild := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import"
)

val catsVersion = "2.0.0-M4"
val circeVersion = "0.12.0-M3"

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

val sharedSettings = Seq(
  scalacOptions ++= {
    if (priorTo2_13(scalaVersion.value)) compilerOptions
    else
      compilerOptions.flatMap {
        case "-Ywarn-unused-import" => Seq("-Ywarn-unused:imports")
        case "-Xfuture"             => Nil
        case "-Yno-adapted-args"    => Nil
        case other                  => Seq(other)
      }
  },
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Xlint"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Xlint"))
  },
  coverageHighlighting := true,
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "io.circe" %% "circe-testing" % circeVersion % Test,
    "org.scalatest" %% "scalatest" % "3.1.0-SNAP13" % Test,
    "org.scalatestplus" %% "scalatestplus-scalacheck" % "1.0.0-SNAP8" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test,
    "org.typelevel" %% "discipline-scalatest" % "0.12.0-M3" % Test
  ),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
)

val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(libraryDependencies += "io.circe" %% "circe-literal" % circeVersion)
  .aggregate(algebra, benchmarks, free)
  .dependsOn(algebra)

lazy val algebra = project.in(file("algebra")).settings(moduleName := "circe-algebra").settings(sharedSettings)

lazy val free = project
  .in(file("free"))
  .settings(
    moduleName := "circe-algebra-free",
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion
  )
  .settings(sharedSettings ++ noPublishSettings)

lazy val benchmarks = project
  .in(file("benchmarks"))
  .settings(libraryDependencies += "io.circe" %% "circe-jawn" % circeVersion)
  .settings(sharedSettings ++ noPublishSettings)
  .settings(mainClass in assembly := Some("io.circe.algebra.benchmarks.DecodingApp"))
  .enablePlugins(JmhPlugin)
  .dependsOn(algebra, free)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/circe-algebra")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/travisbrown/circe-algebra"),
      "scm:git:git@github.com:travisbrown/circe-algebra.git"
    )
  ),
  developers := List(
    Developer(
      "travisbrown",
      "Travis Brown",
      "travisrobertbrown@gmail.com",
      url("https://twitter.com/travisbrown")
    )
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
