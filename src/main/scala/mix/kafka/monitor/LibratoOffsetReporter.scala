package mix.kafka.monitor

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{Gauge, MetricRegistry, ScheduledReporter}
import com.quantifind.kafka.OffsetGetter
import com.quantifind.kafka.OffsetGetter.OffsetInfo
import kafka.utils.Logging

class LibratoOffsetReporter(metrics: MetricRegistry,
                            reporter: ScheduledReporter,
                            reportingInterval: Duration,
                            metricsCacheExpiration: Duration) extends Logging {

  import LibratoOffsetReporter._

  logger.info(s"starting LibratoOffsetReporter with interval = ${reportingInterval.getSeconds} secs, " +
    s"cacheTTL = ${metricsCacheExpiration.getSeconds} secs")

  reporter.start(reportingInterval.getSeconds, TimeUnit.SECONDS)

  def report(offsets: IndexedSeq[OffsetGetter.OffsetInfo]): Unit = {
    val cacheEntries = offsets map { offset =>
      getMetricName(offset) -> Lag(offset.lag)
    } toMap

    import scala.collection.JavaConverters._

    /*
     * First fetching all keys from cache before updating them since that would trigger registration of new metrics
     * (via cacheLoader) to dropwizard-metrics registry without errors since dropwizard throw errors while trying to
     * register existing gauges - so basically relying on cache synchronised behavior to register metrics only once.
     */
    cache.getAll(cacheEntries.keys.asJava)
    cache.putAll(cacheEntries.asJava)
  }

  import com.github.benmanes.caffeine.cache._

  private[this] lazy val cache = Caffeine
    .newBuilder()
    .expireAfterAccess(metricsCacheExpiration.getSeconds, TimeUnit.SECONDS)
    .removalListener(removalListener)
    .build[String, Lag](cacheLoader)

  private lazy val removalListener = new RemovalListener[String, Lag] {
    override def onRemoval(notification: RemovalNotification[String, Lag]): Unit = {
      notification.getCause match {
        case RemovalCause.REPLACED =>
          logger.debug(s"updating value for ${notification.getKey}")
        case RemovalCause.EXPIRED =>
          val metricName = notification.getKey
          logger.info(s"removing $metricName from registry after expiration")
          metrics.remove(metricName)
        case _ =>
          throw new IllegalStateException(s"unsupported ${notification.getCause} for metrics cache")
      }
    }
  }

  private lazy val cacheLoader = new CacheLoader[String, Lag] {
    override def load(key: String): Lag = {
      val default = Lag(0L)
      val lag = new Gauge[Long] {
        override def getValue: Long = default.value
      }

      logger.info(s"adding $key to registry")
      metrics.register(key, lag)
      default
    }
  }
}

object LibratoOffsetReporter {
  def getMetricName(offsetInfo: OffsetInfo): String = {
    val nameComponents = Seq(
      offsetInfo.group,
      offsetInfo.topic,
      offsetInfo.partition.toString,
      "lag"
    )

    nameComponents.map(_.replaceAll("\\.", "-")).map(_.toLowerCase).mkString(".")
  }
}
