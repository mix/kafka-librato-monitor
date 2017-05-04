package mix.kafka.monitor

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.librato.metrics.reporter.LibratoReporter
import com.quantifind.kafka.OffsetGetter
import com.quantifind.kafka.offsetapp.OffsetInfoReporter

class LibratoReporterPlugin(pluginArgs: String) extends OffsetInfoReporter {

  private lazy val metrics = new MetricRegistry

  private lazy val libratoConfig = LibratoConfig.parseArguments(pluginArgs)

  private lazy val libratoReporter = LibratoReporter
    .builder(metrics, libratoConfig.email, libratoConfig.token)
    .setSource(libratoConfig.source)
    .setPrefix(libratoConfig.prefix)
    .setRateUnit(TimeUnit.SECONDS)
    .setDurationUnit(TimeUnit.MILLISECONDS)
    .build()

  private lazy val offsetReporter = new OffsetReporter(metrics, libratoReporter,
    libratoConfig.reportingInterval, libratoConfig.metricsCacheExpiration)

  override def report(info: IndexedSeq[OffsetGetter.OffsetInfo]): Unit = offsetReporter.report(info)
}
