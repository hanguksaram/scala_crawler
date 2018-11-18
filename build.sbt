name := "simlplecrawler"

version := "0.1"

scalaVersion := "2.12.7"
val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.10.0",
  "io.circe" %% "circe-parser" % "0.10.0",
  "org.dispatchhttp" %% "dispatch-core"   % "1.0.0"
)