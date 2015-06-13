val scala210 = "2.10.5"
val scala211 = "2.11.6"

val buildSettings: Seq[Setting[_]] = inThisBuild(Seq(
  organization := "jp.kobe_u.scarab",
  scalaVersion := scala211,
  version := "1.6.9-SNAPSHOT",
  homepage := Some(url("http://kix.istc.kobe-u.ac.jp/~soh/scarab/")),
  crossScalaVersions := Seq(scala210, scala211),
  description := "A prototyping tool for developing SAT-based CP systems",
  licenses := List("BSD New" -> url("https://github.com/TakehideSoh/Scarab/blob/master/LICENSE")),
  scmInfo := Some(ScmInfo(url("https://github.com/TakehideSoh/Scarab.git"), "https://github.com/TakehideSoh/Scarab.git")),
  developers := List(
    Developer("TakehideSoh", "Takehide Soh", "soh@lion.kobe-u.ac.jp", url("http://kix.istc.kobe-u.ac.jp/~soh/"))
  )
))

lazy val root = (project in file(".")).
  dependsOn(sat4jCore).
  settings(
    buildSettings,
    name := "Scarab"
  )

// sat4j 2.3.6-R2404
lazy val sat4jCore = (project in file("org.sat4j.core")).
  settings(
    name := "org.sat4j.core",
    version := "2.3.6-R2404",
    autoScalaLibrary := false,
    crossPaths := false
  )
