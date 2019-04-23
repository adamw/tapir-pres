import sbt._
import Keys._

name := "tapir-pres"
organization := "com.softwaremill"
scalaVersion := "2.12.8"

val tapirVersion = "0.6"

libraryDependencies ++= Seq(
  "com.softwaremill.tapir" %% "tapir-core" % tapirVersion,
  "com.softwaremill.tapir" %% "tapir-akka-http-server" % tapirVersion,
  "com.softwaremill.tapir" %% "tapir-json-circe" % tapirVersion,
  "com.softwaremill.tapir" %% "tapir-openapi-docs" % tapirVersion,
  "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
  "com.softwaremill.tapir" %% "tapir-sttp-client" % tapirVersion,
  "org.webjars" % "swagger-ui" % "3.22.0"
)
