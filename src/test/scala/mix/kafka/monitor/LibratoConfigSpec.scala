package mix.kafka.monitor

import java.time.Duration

import org.scalatest.{FlatSpec, Matchers}

class LibratoConfigSpec extends FlatSpec with Matchers {

  import LibratoConfigConstants._

  behavior of "parseArguments"

  it should "build valid instance of config from string args" in {
    val email = "mail@example.com"
    val token = "12345678ABC"
    val prefix = "prefix"
    val source = "source"

    val args = s"$emailParam=$email,$tokenParam=$token,$prefixParam=$prefix,$sourceParam=$source," +
      s"$reportingIntervalParam=PT10S,$metricsCacheExpirationParam=PT1M"

    val expectedConfig = LibratoConfig(email, token, prefix, source, Duration.ofSeconds(10), Duration.ofMinutes(1))
    LibratoConfig.parseArguments(args) shouldBe expectedConfig
  }

  it should "build instance of config with defaults for missing params" in {
    val email = "mail@example.com"
    val token = "12345678ABC"

    val args = s"$emailParam=$email,$tokenParam=$token"
    val hostname = java.net.InetAddress.getLocalHost.getHostAddress

    val expectedConfig = LibratoConfig(email, token, defaultPrefix, hostname,
      Duration.ofSeconds(30), Duration.ofMinutes(10))
    LibratoConfig.parseArguments(args) shouldBe expectedConfig
  }

  it should "throw IllegalArgumentException if email param is missing" in {
    val token = "12345678ABC"

    val args = s"$tokenParam=$token"
    intercept[IllegalArgumentException](LibratoConfig.parseArguments(args))
  }

  it should "throw IllegalArgumentException if token param is missing" in {
    val email = "mail@example.com"

    val args = s"$emailParam=$email"
    intercept[IllegalArgumentException](LibratoConfig.parseArguments(args))
  }

}
