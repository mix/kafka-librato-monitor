package mix.kafka.monitor

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{Gauge, MetricRegistry, ScheduledReporter}
import com.quantifind.kafka.OffsetGetter
import com.quantifind.kafka.OffsetGetter.OffsetInfo

class OffsetReporter(metrics: MetricRegistry,
                     reporter: ScheduledReporter,
                     reportingInterval: Duration,
                     metricsCacheExpiration: Duration) {

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

  private def getMetricName(offsetInfo: OffsetInfo): String = {
    val nameComponents = Seq (
      offsetInfo.group,
      offsetInfo.topic,
      offsetInfo.partition.toString,
      "lag"
    )

    nameComponents.map(_.replaceAll(".", "-")).mkString(".")
  }

  import com.github.benmanes.caffeine.cache._

  private[this] lazy val cache = Caffeine
    .newBuilder()
    .expireAfterAccess(metricsCacheExpiration.getSeconds, TimeUnit.SECONDS)
    .removalListener(removalListener)
    .build[String, Lag](cacheLoader)

  private lazy val removalListener = new RemovalListener[String, Lag] {
    override def onRemoval(notification: RemovalNotification[String, Lag]): Unit = {
      metrics.remove(notification.getKey)
    }
  }

  private lazy val cacheLoader = new CacheLoader[String, Lag] {
    override def load(key: String): Lag = {
      val default = Lag(0L)
      val lag = new Gauge[Long] {
        override def getValue: Long = default.value
      }

      metrics.register(key, lag)
      default
    }
  }
}
