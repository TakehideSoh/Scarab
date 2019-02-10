val scala212 = "2.12.4"
val scala211 = "2.11.8"

val buildSettings: Seq[Setting[_]] = inThisBuild(Seq(
  organization := "jp.kobe_u.scarab",
  scalaVersion := scala212, 
  version := "1.9.6-SNAPSHOT",  
  homepage := Some(url("http://tsoh.org/scarab/")),  
  crossScalaVersions := Seq(scala211, scala212),
  description := "A prototyping tool for developing SAT-based CP systems",
  licenses := List("BSD New" -> url("https://github.com/TakehideSoh/Scarab/blob/master/LICENSE")),
  scmInfo := Some(ScmInfo(url("https://github.com/TakehideSoh/Scarab.git"), "https://github.com/TakehideSoh/Scarab.git")),
  developers := List(
    Developer("TakehideSoh", "Takehide Soh", "soh@lion.kobe-u.ac.jp", url("http://tsoh.org/"))  
  ),
))

lazy val sat4jCore = (project in file("org.sat4j.core")).
   settings(
     name := "org.sat4j.core",
     version := "2.3.6-SNAPSHOT",
     autoScalaLibrary := false,
     crossPaths := false,
     licenses := List("EPL-1.0" -> url("https://www.eclipse.org/legal/epl-v10.html"))
   )
// sat4j 2.3.6-R2404
lazy val sat4jPB = (project in file("org.sat4j.pb")).
  dependsOn(sat4jCore).
  settings(
    name := "org.sat4j.pb",
    version := "2.3.6-SNAPSHOT",
    autoScalaLibrary := false,
    crossPaths := false,
    licenses := List("EPL-1.0" -> url("https://www.eclipse.org/legal/epl-v10.html"))
)

lazy val root = (project in file(".")).
  dependsOn(sat4jCore).  
  dependsOn(sat4jPB).  
  settings(
    assemblyJarName in assembly := "scarab.jar",
    buildSettings,
    name := "Scarab",
//    autoScalaLibrary := true
  )

mergeStrategy in assembly := {
case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
case _ => MergeStrategy.first
}
