organization in ThisBuild := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Xlint",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

val catsVersion = "1.0.0-RC1"
val circeVersion = "0.9.0-M2"

val sharedSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor >= 11 => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Xlint"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Xlint"))
  },
  coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  ),
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-testing" % circeVersion % Test,
    "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.typelevel" %% "discipline" % "0.8" % Test,
    "org.typelevel" %% "cats-laws" % catsVersion % Test
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")
)

val root = project.in(file("."))
  .settings(sharedSettings)
  .settings(libraryDependencies += "io.circe" %% "circe-literal" % circeVersion)
  .aggregate(algebra, benchmarks, free)
  .dependsOn(algebra)

lazy val algebra = project.in(file("algebra"))
  .settings(moduleName := "circe-algebra")
  .settings(sharedSettings)

lazy val free = project.in(file("free"))
  .settings(
    moduleName := "circe-algebra-free",
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion
  )
  .settings(sharedSettings ++ noPublishSettings)

lazy val benchmarks = project.in(file("benchmarks"))
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
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
