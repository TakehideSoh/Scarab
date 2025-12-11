val scala213 = "2.13.16"
val scala212 = "2.12.20"

val buildSettings: Seq[Setting[_]] = inThisBuild(Seq(
  organization := "jp.kobe_u.scarab",
  scalaVersion := scala213,
  version := "1.9.7-SNAPSHOT",
  homepage := Some(url("http://tsoh.org/scarab/")),
  crossScalaVersions := Seq(scala212, scala213),
  description := "A prototyping tool for developing SAT-based CP systems",
  licenses := List("BSD New" -> url("https://github.com/TakehideSoh/Scarab/blob/master/LICENSE")),
  scmInfo := Some(ScmInfo(url("https://github.com/TakehideSoh/Scarab.git"), "https://github.com/TakehideSoh/Scarab.git")),
  developers := List(
    Developer("TakehideSoh", "Takehide Soh", "soh@lion.kobe-u.ac.jp", url("http://tsoh.org/"))  
  ),
))

lazy val root = (project in file(".")).
  settings(
    assembly / assemblyJarName := "scarab.jar",
    assembly / mainClass := Some("ScarabMain"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "module-info.class" => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    buildSettings,
    name := "Scarab",
    libraryDependencies += "net.java.dev.jna" % "jna" % "5.16.0",
    libraryDependencies += "org.ow2.sat4j" % "org.ow2.sat4j.core" % "2.3.6",
    libraryDependencies += "org.ow2.sat4j" % "org.ow2.sat4j.pb" % "2.3.6",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
  )
