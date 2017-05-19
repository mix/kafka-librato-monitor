package mix.kafka.monitor

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.librato.metrics.reporter.LibratoReporter
import com.quantifind.kafka.OffsetGetter
import com.quantifind.kafka.offsetapp.OffsetInfoReporter
import kafka.utils.Logging

class LibratoReporterPlugin(pluginArgs: String) extends OffsetInfoReporter with Logging {

  private lazy val metrics = new MetricRegistry

  private lazy val libratoConfig = LibratoConfig.parseArguments(pluginArgs)

  logger.info(s"starting LibratoReporterPlugin with prefix = ${libratoConfig.prefix}")

  private lazy val libratoReporter = LibratoReporter
    .builder(metrics, libratoConfig.email, libratoConfig.token)
    .setSource(libratoConfig.source)
    .setPrefix(libratoConfig.prefix)
    .setRateUnit(TimeUnit.SECONDS)
    .setDurationUnit(TimeUnit.MILLISECONDS)
    .build()

  private lazy val cacheProvider = new MetricsCacheProvider(metrics, libratoConfig.metricsCacheExpiration)
  private lazy val offsetReporter = new LibratoOffsetReporter(metrics, libratoReporter, cacheProvider,
    libratoConfig.source, libratoConfig.reportingInterval)

  override def report(offsets: IndexedSeq[OffsetGetter.OffsetInfo]): Unit = offsetReporter.report(offsets)
}
