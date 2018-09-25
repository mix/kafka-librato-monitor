name := "kafka-librato-monitor"

organization := "mix.kafka"

scalaVersion := "2.11.11"

// Dependencies
libraryDependencies ++= Seq(
  "com.github.ben-manes.caffeine" % "caffeine" % "1.0.0",
  "com.quantifind" %% "kafkaoffsetmonitor" % "0.4.6-SNAPSHOT",
  "com.librato.metrics" % "metrics-librato" % "5.0.5",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

// To remove multiple exclusions
excludeDependencies ++= Seq(
  ExclusionRule("com.google.code.findbugs", "annotations")
)

// use mix log4j.properties
assemblyMergeStrategy in assembly := {
  case "log4j.properties" => MergeStrategy.first
  case x => MergeStrategy.defaultMergeStrategy.apply(x)
}

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
