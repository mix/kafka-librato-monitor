package mix.kafka.monitor

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{Gauge, MetricRegistry, ScheduledReporter}
import com.quantifind.kafka.OffsetGetter
import kafka.utils.Logging

class LibratoOffsetReporter(metricRegistry: MetricRegistry,
                            reporter: ScheduledReporter,
                            source: String,
                            reportingInterval: Duration,
                            cacheExpiration: Duration) extends Logging {

  import LibratoOffsetReporter._

  logger.info(s"starting LibratoOffsetReporter with source = $source, interval = $reportingInterval")

  reporter.start(reportingInterval.getSeconds, TimeUnit.SECONDS)

  def report(offsets: IndexedSeq[OffsetGetter.OffsetInfo]): Unit = {

    val updatedMetrics: Metrics = offsetsToMetrics(offsets)
    val existingMetrics: Metrics = cacheProvider.cachedEntries(updatedMetrics.keys.toSeq)

    // merge metrics and update cache
    val mergedMetrics: Metrics = mergeMetrics(existingMetrics, updatedMetrics)
    cacheProvider.updateCache(mergedMetrics)

    // add new metrics to dropwizard registry
    val newMetricKeys: Set[ConsumerGroupTopic] = mergedMetrics.keySet -- existingMetrics.keys
    newMetricKeys foreach addMetricToRegistry
  }

  private def addMetricToRegistry(key: MetricKey): Unit = {
    import java.lang.{Integer => JInt, Long => JLong}
    logger.info(s"adding metrics for $key to registry")

    metricRegistry.register(getMetricName(key, "lag"), new Gauge[JLong] {
      override def getValue: JLong = {
        cacheProvider.cachedValues(key).getOrElse(Map.empty).values.map(_.value).sum
      }
    })

    metricRegistry.register(getMetricName(key, "partitions"), new Gauge[JInt] {
      override def getValue: JInt = {
        cacheProvider.cachedValues(key).getOrElse(Map.empty).keySet.size
      }
    })
  }

  private lazy val cacheProvider = new CacheProvider[MetricKey, MetricValue](cacheExpiration)(expirationListener)

  private def expirationListener(consumerGroupTopic: ConsumerGroupTopic): Unit = {
    logger.info(s"removing metrics for $consumerGroupTopic from registry")

    metricRegistry.remove(getMetricName(consumerGroupTopic, "lag"))
    metricRegistry.remove(getMetricName(consumerGroupTopic, "partitions"))
  }

}

object LibratoOffsetReporter {

  type MetricKey = ConsumerGroupTopic

  type MetricValue = Map[Partition, Lag]

  type Metrics = Map[MetricKey, MetricValue]

  def getMetricName(metricKey: ConsumerGroupTopic, metricSuffix: String): String = {
    val nameComponents = Seq(metricKey.group, metricKey.topic, metricSuffix) map resolveName
    nameComponents mkString "."
  }

  def offsetsToMetrics(rawOffsets: Seq[OffsetGetter.OffsetInfo]): Metrics = {
    val groupedOffsets = rawOffsets.groupBy(o => ConsumerGroupTopic(o.group, o.topic))
    groupedOffsets map { case (groupTopic, offsets) =>
      val partitionLagForGroupTopic = offsets map { offset =>
        Partition(offset.partition) -> Lag(offset.lag)
      } toMap

      groupTopic -> partitionLagForGroupTopic
    }
  }

  def mergeMetrics(existingMetrics: Metrics, newMetrics: Metrics): Metrics = {
    newMetrics map { case (groupTopic, newPartitions) =>
      val existingPartitions = existingMetrics.getOrElse(groupTopic, Map.empty)
      groupTopic -> mergePartitionsForMetric(existingPartitions, newPartitions)
    }
  }

  private def mergePartitionsForMetric(existingPartitions: Map[Partition, Lag],
                                       updatedPartitions: Map[Partition, Lag]): Map[Partition, Lag] = {

    val existingPartitionsWithUpdatedLag = existingPartitions map { case (partition, lag) =>
      partition -> updatedPartitions.getOrElse(partition, lag)
    }

    val newPartitions = updatedPartitions -- existingPartitions.keySet
    existingPartitionsWithUpdatedLag ++ newPartitions
  }

  private def resolveName(rawName: String): String = {
    rawName.replaceAll("\\.", "-").trim.toLowerCase
  }

}
