// *****************************************************************************
// Projects
// *****************************************************************************

import com.typesafe.sbt.packager.docker._

lazy val root = project.in(file("."))
  .aggregate(model, transformer)
  .settings(
    name := "$name;format="norm"$",
    skip in publish := true
  )

lazy val model =
  project
    .in(file("$deviceType;format="norm"$"))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.sprayJson,
        library.streambedCore,
        library.scalaCheck       % Test,
        library.streambedTestKit % Test,
        library.utest            % Test
      )
    )

lazy val transformer =
  project
    .in(file("$deviceType;format="norm"$-transformer"))
    .enablePlugins(AutomateHeaderPlugin, DockerPlugin, GitVersioning, GitBranchPrompt)
    .dependsOn(model)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.chronicleQueue,
        library.fs,
        library.loraControlPlane,
        library.loraStreams,
        library.ioxSss,
        library.jaegerTracing,
        library.sprayJson,
        library.streambedCore,
        library.scalaCheck       % Test,
        library.streambedTestKit % Test,
        library.utest            % Test
      ),
      // #
      Seq(
        mappings in Docker := assembly.value.pair(Path.flatRebase("/opt/docker/lib")),
        dockerCommands := Seq(
          Cmd("FROM", image.ioxLandlord),
          Cmd("LABEL", s"""cisco.info.name=\${name.value.replaceAll("-", "")}"""),
          Cmd("COPY", "opt/docker", "/opt/docker"),
          ExecCmd("CMD", "/opt/docker/bin/start", (mainClass in Compile).value.getOrElse(""))
        )
      )
    )

// *****************************************************************************
// Library and image dependencies
// *****************************************************************************

lazy val image =
  new {
    object Version {
      val ioxLandlord = library.Version.streambed
    }

    val ioxLandlord = s"farmco/iox-landlord:\${Version.ioxLandlord}"
  }

lazy val library =
  new {
    object Version {
      val loraSdk    = "0.10.1"
      val scalaCheck = "1.14.0"
      val sprayJson  = "1.3.4"
      val streambed  = "0.19.4"
      val utest      = "0.6.4"
    }
    val chronicleQueue   = "com.cisco.streambed"      %% "chronicle-queue"    % Version.streambed
    val fs               = "com.cisco.streambed"      %% "fs"                 % Version.streambed
    val loraControlPlane = "com.cisco.streambed.lora" %% "lora-control-plane" % Version.loraSdk
    val loraStreams      = "com.cisco.streambed.lora" %% "lora-streams"       % Version.loraSdk
    val ioxSss           = "com.cisco.streambed"      %% "iox-sss"            % Version.streambed
    val jaegerTracing    = "com.cisco.streambed"      %% "jaeger-tracing"     % Version.streambed
    val scalaCheck       = "org.scalacheck"           %% "scalacheck"         % Version.scalaCheck
    val sprayJson        = "io.spray"                 %% "spray-json"         % Version.sprayJson
    val streambedCore    = "com.cisco.streambed"      %% "streambed-core"     % Version.streambed
    val streambedTestKit = "com.cisco.streambed"      %% "streambed-testkit"  % Version.streambed
    val utest            = "com.lihaoyi"              %% "utest"              % Version.utest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  gitSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.6",
    organization := "$organization;format="package"$",
    organizationName := "$organizationName$",
    startYear := Some(2018),
    headerLicense := Some(HeaderLicense.Custom("Copyright (c) $organizationName$, 2018")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    publishArtifact in (Compile, packageDoc) := false,  // Remove if these libraries are OSS
    publishArtifact in (Compile, packageSrc) := false,  // Remove if these libraries are OSS
    testFrameworks += new TestFramework("utest.runner.Framework"),
    wartremoverWarnings in (Compile, compile) ++= Warts.unsafe,
    resolvers += "streambed-repositories" at "https://repositories.streambed.io/jars/"
)

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )
