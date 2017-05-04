name := "kafka-librato-monitor"

organization := "mix.kafka"

scalaVersion := "2.10.3"

// Dependencies
libraryDependencies ++= Seq(
  "com.github.ben-manes.caffeine" % "caffeine" % "1.0.0",
  "com.quantifind" % "kafkaoffsetmonitor_2.10" % "0.3.0-SNAPSHOT",
  "com.librato.metrics" % "metrics-librato" % "5.0.5",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

// javac & scalac options
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

scalacOptions := Seq("-deprecation",
  "-unchecked",
  "-feature",
  "-Yresolve-term-conflict:package",
  "-language:postfixOps",
  "-language:implicitConversions"
)

// set Ivy logging to be at the highest level, if needed for debugging
ivyLoggingLevel := UpdateLogging.Quiet

logLevel := Level.Info
