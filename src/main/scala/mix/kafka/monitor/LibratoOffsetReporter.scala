package mix.kafka.monitor

import java.lang.{Integer => JInt, Long => JLong}
import java.time.Duration
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{Gauge, MetricRegistry, ScheduledReporter}
import com.quantifind.kafka.OffsetGetter
import kafka.utils.Logging

class LibratoOffsetReporter(metricRegistry: MetricRegistry,
                            reporter: ScheduledReporter,
                            cacheProvider: MetricsCacheProvider,
                            source: String,
                            reportingInterval: Duration) extends Logging {

  import LibratoOffsetReporter._

  logger.info(s"starting LibratoOffsetReporter with source = $source, interval = $reportingInterval")

  reporter.start(reportingInterval.getSeconds, TimeUnit.SECONDS)

  def report(offsets: IndexedSeq[OffsetGetter.OffsetInfo]): Unit = {
    val metricsWithUpdatedValues = offsetsToMetrics(offsets)
    val existingMetrics = cacheProvider.existingCacheEntries(metricsWithUpdatedValues.keys.toSeq)
    val newMetrics = metricsWithUpdatedValues -- existingMetrics.keys
    val mergedMetrics = mergeMetrics(existingMetrics, metricsWithUpdatedValues)
    cacheProvider.updateCache(mergedMetrics)
    newMetrics.keys foreach addMetricToRegistry
  }

  private def addMetricToRegistry(key: GroupTopic): Unit = {
    metricRegistry.register(getMetricName(key, "lag"), new Gauge[JLong] {
      override def getValue: JLong = {
        cacheProvider.partitionLagsFromCache(key).values.map(_.toLong).sum
      }
    })

    metricRegistry.register(getMetricName(key, "partitions"), new Gauge[JInt] {
      override def getValue: JInt = {
        cacheProvider.partitionLagsFromCache(key).keySet.size
      }
    })
  }

}

object LibratoOffsetReporter {

  def getMetricName(metricKey: GroupTopic, metricSuffix: String): String = {
    val nameComponents = Seq(metricKey.group, metricKey.topic, metricSuffix) map resolveName
    nameComponents mkString "."
  }

  def offsetsToMetrics(rawOffsets: Seq[OffsetGetter.OffsetInfo]): Map[GroupTopic, Map[JInt, JLong]] = {
    val groupedOffsets = rawOffsets.groupBy(o => GroupTopic(o.group, o.topic))
    groupedOffsets map { case (groupTopic, offsets) =>
      val partitionLagForGroupTopic = offsets map { offset =>
        offset.partition.asInstanceOf[JInt] -> offset.lag.asInstanceOf[JLong]
      } toMap

      groupTopic -> partitionLagForGroupTopic
    }
  }

  def mergeMetrics(existingMetrics: Map[GroupTopic, Map[JInt, JLong]],
                   newMetrics: Map[GroupTopic, Map[JInt, JLong]]): Map[GroupTopic, Map[JInt, JLong]] = {
    newMetrics map { case (groupTopic, newPartitions) =>
      val existingPartitions = existingMetrics.getOrElse(groupTopic, Map.empty)
      groupTopic -> mergePartitionsForMetric(groupTopic, existingPartitions, newPartitions)
    }
  }

  private def mergePartitionsForMetric(groupTopic: GroupTopic,
                                       existingPartitions: Map[JInt, JLong],
                                       updatedPartitions: Map[JInt, JLong]): Map[JInt, JLong] = {

    val existingPartitionsWithUpdatedLag = existingPartitions map { case (partition, lag) =>
      partition -> updatedPartitions.getOrElse(partition, lag)
    }

    val newPartitions = updatedPartitions.keySet -- existingPartitions.keySet
    val newPartitionsWithLag = newPartitions map { partition =>
      partition -> updatedPartitions.getOrElse(partition,
        throw new IllegalStateException(s"unable to get lag for $partition in $groupTopic"))
    }

    existingPartitionsWithUpdatedLag ++ newPartitionsWithLag
  }

  private def resolveName(rawName: String): String = {
    rawName.replaceAll("\\.", "-").trim.toLowerCase
  }

}
