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

  val defaultPrefix = "kafka-monitor"
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
      nonEmptyOptionalString(argsMap.get(prefixParam), defaultPrefix),
      nonEmptyOptionalString(argsMap.get(sourceParam), java.net.InetAddress.getLocalHost.getHostAddress),
      Duration.parse(nonEmptyOptionalString(argsMap.get(reportingIntervalParam), "PT30S")),
      Duration.parse(nonEmptyOptionalString(argsMap.get(metricsCacheExpirationParam), "PT10M"))
    )
  }

  private def nonEmptyOptionalString(optionalString: Option[String], defaultValue: String): String = {
    optionalString.map(_.trim).filter(_.nonEmpty).getOrElse(defaultValue)
  }
}
