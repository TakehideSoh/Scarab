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
  ),
  bintrayReleaseOnPublish := false,
  bintrayOrganization := None,
  bintrayRepository := "maven",
  bintrayPackage := "Scarab"
))

val commonSettings: Seq[Setting[_]] = Seq(
  bintrayPackage := (bintrayPackage in ThisBuild).value,
  bintrayRepository := (bintrayRepository in ThisBuild).value
)

lazy val root = (project in file(".")).
  dependsOn(sat4jCore).
  dependsOn(sat4jPB).
  settings(
    buildSettings,
    commonSettings,
    name := "Scarab"
  )

// sat4j 2.3.6-R2404
lazy val sat4jCore = (project in file("org.sat4j.core")).
  settings(
    commonSettings,
    name := "org.sat4j.core",
    version := "2.3.6-R2404",
    autoScalaLibrary := false,
    crossPaths := false,
    licenses := List("EPL-1.0" -> url("https://www.eclipse.org/legal/epl-v10.html"))
  )

// sat4j 2.3.6-R2404
lazy val sat4jPB = (project in file("org.sat4j.pb")).
  dependsOn(sat4jCore).
  settings(
    commonSettings,
    name := "org.sat4j.pb",
    version := "2.3.6-R2404",
    autoScalaLibrary := false,
    crossPaths := false,
    licenses := List("EPL-1.0" -> url("https://www.eclipse.org/legal/epl-v10.html"))
  )
