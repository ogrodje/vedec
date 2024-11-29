import com.typesafe.sbt.packager.docker.Cmd

val scala3Version = "3.6.1"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .in(file("."))
  .settings(
    name         := "vedec",
    version      := "0.0.1",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-feature",
      "-language:implicitConversions",
      "-java-output-version",
      "21"
    ),
    libraryDependencies ++= Seq("org.scalameta" %% "munit" % "1.0.2" % Test) ++
      Dependencies.zio ++ Dependencies.refined ++ Dependencies.circe ++ Dependencies.logging
  )
  .settings(
    Compile / mainClass              := Some("Main"),
    assembly / mainClass             := Some("Main"),
    assembly / assemblyJarName       := "vedec.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")                        =>
        MergeStrategy.discard
      case PathList("META-INF", "jpms.args")                    =>
        MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") =>
        MergeStrategy.first
      case PathList("deriving.conf")                            =>
        MergeStrategy.last
      case PathList(ps @ _*) if ps.last endsWith ".class"       => MergeStrategy.last
      case x                                                    =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )
  .settings(
    Compile / mainClass             := Some("Main"),
    Compile / discoveredMainClasses := Seq(),
    dockerExposedPorts              := Seq(4441),
    dockerExposedUdpPorts           := Seq.empty[Int],
    dockerUsername                  := Some("ogrodje"),
    dockerUpdateLatest              := true,
    dockerRepository                := Some("ghcr.io"),
    dockerBaseImage                 := "azul/zulu-openjdk-alpine:21-latest",
    packageName                     := "vedec",
    dockerCommands                  := dockerCommands.value.flatMap {
      case add @ Cmd("RUN", args @ _*) if args.contains("id") =>
        List(
          Cmd("LABEL", "maintainer Oto Brglez <otobrglez@gmail.com>"),
          Cmd("LABEL", "org.opencontainers.image.url https://github.com/ogrodje/vedec"),
          Cmd("LABEL", "org.opencontainers.image.source https://github.com/ogrodje/vedec"),
          Cmd("RUN", "apk add --no-cache bash"),
          Cmd("ENV", "SBT_VERSION", sbtVersion.value),
          Cmd("ENV", "SCALA_VERSION", scalaVersion.value),
          Cmd("ENV", "VEDEC_VERSION", version.value),
          add
        )
      case other                                              => List(other)
    }
  )

resolvers ++= Dependencies.projectResolvers
