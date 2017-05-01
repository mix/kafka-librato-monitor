name := "kafka-librato-monitor"

organization := "mix.kafka"

scalaVersion := "2.11.8"

// Dependencies
libraryDependencies ++= Seq(
  "com.google.guava" % "guava" % "18.0",
  "com.quantifind" % "kafkaoffsetmonitor_2.10" % "0.3.0-SNAPSHOT",
  "com.librato.metrics" % "metrics-librato" % "5.0.0"
)

// javac & scalac options
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

scalacOptions := Seq("-deprecation",
  "-unchecked",
  "-feature",
  "-target:jvm-1.8",
  "-Yresolve-term-conflict:package",
  "-language:postfixOps",
  "-language:implicitConversions"
)

// set Ivy logging to be at the highest level, if needed for debugging
ivyLoggingLevel := UpdateLogging.Quiet

logLevel := Level.Info

