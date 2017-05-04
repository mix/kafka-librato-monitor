package mix.kafka.monitor

import java.time.Duration

case class Lag(value: Long)

case class LibratoConfig(email: String,
                         token: String,
                         prefix: String,
                         source: String,
                         reportingInterval: Duration,
                         metricsCacheExpiration: Duration)

object LibratoConfigConstants {
  val emailParam = "libratoEmail"
  val tokenParam = "libratoToken"
  val prefixParam = "libratoPrefix"
  val sourceParam = "libratoSource"
  val reportingIntervalParam = "libratoReportingInterval"
  val metricsCacheExpirationParam = "metricsCacheExpiration"

  val defaultPrefix = "kafka-lag"
}

object LibratoConfig {
  def parseArguments(args: String): LibratoConfig = {
    val argsMap = args
      .split(",")
      .map(_.split("=", 2))
      .filter(_.length > 1)
      .map(arg => arg(0) -> arg(1))
      .toMap

    import LibratoConfigConstants._

    LibratoConfig(
      argsMap.getOrElse(emailParam, throw new IllegalArgumentException(s"$emailParam is required")),
      argsMap.getOrElse(tokenParam, throw new IllegalArgumentException(s"$tokenParam is required")),
      argsMap.getOrElse(prefixParam, defaultPrefix),
      argsMap.getOrElse(sourceParam, java.net.InetAddress.getLocalHost.getHostAddress),
      Duration.parse(argsMap.getOrElse(reportingIntervalParam, "PT30S")),
      Duration.parse(argsMap.getOrElse(metricsCacheExpirationParam, "PT10M"))
    )
  }
}
