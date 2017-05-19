package mix.kafka.monitor

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import kafka.utils.Logging
import mix.kafka.monitor.LibratoOffsetReporter.getMetricName

case class GroupTopic(group: String, topic: String)

class MetricsCacheProvider(metricRegistry: MetricRegistry, cacheExpiration: Duration) extends Logging {

  import java.lang.{Integer => JInt, Long => JLong}

  logger.info(s"starting MetricsCacheProvider with cacheExpiration = $cacheExpiration")

  def existingCacheEntries(keys: Seq[GroupTopic]): Map[GroupTopic, Map[JInt, JLong]] = {
    import scala.collection.JavaConverters._
    cache.getAllPresent(keys.asJava).asScala.toMap
  }

  def partitionLagsFromCache(key: GroupTopic): Map[JInt, JLong] = {
    Option(cache.getIfPresent(key)).getOrElse(Map.empty)
  }

  def updateCache(entries: Map[GroupTopic, Map[JInt, JLong]]): Unit = {
    import scala.collection.JavaConverters._
    cache.putAll(entries.asJava)
  }

  import com.github.benmanes.caffeine.cache._

  private[this] lazy val cache = Caffeine
    .newBuilder()
    .expireAfterAccess(cacheExpiration.getSeconds, TimeUnit.SECONDS)
    .removalListener(removalListener)
    .build[GroupTopic, Map[JInt, JLong]]

  private lazy val removalListener = new RemovalListener[GroupTopic, Map[JInt, JLong]] {
    override def onRemoval(notification: RemovalNotification[GroupTopic, Map[JInt, JLong]]): Unit = {
      val metric = notification.getKey
      notification.getCause match {
        case RemovalCause.REPLACED =>
          logger.debug(s"updating value for $metric")

        case RemovalCause.EXPIRED =>
          logger.info(s"removing $metric from registry after expiration")
          metricRegistry.remove(getMetricName(metric, "lag"))
          metricRegistry.remove(getMetricName(metric, "partitions"))
        case _ =>
          throw new IllegalStateException(s"unsupported notification_cause: ${notification.getCause} for metrics cache")
      }
    }
  }

}
