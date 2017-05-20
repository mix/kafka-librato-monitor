package mix.kafka.monitor

import java.time.Duration
import java.util.concurrent.TimeUnit

import kafka.utils.Logging

import scala.collection.JavaConverters._

class CacheProvider[K <: AnyRef, V <: AnyRef](cacheExpiration: Duration)(expirationListener: K => Unit)
  extends Logging {

  logger.info(s"starting CacheProvider with cacheExpiration = $cacheExpiration")

  def cachedEntries(keys: Seq[K]): Map[K, V] = {
    cache.getAllPresent(keys.asJava).asScala.toMap
  }

  def cachedValues(key: K): Option[V] = {
    Option(cache.getIfPresent(key))
  }

  def updateCache(entries: Map[K, V]): Unit = {
    cache.putAll(entries.asJava)
  }

  import com.github.benmanes.caffeine.cache._

  private lazy val cache = Caffeine
    .newBuilder()
    .expireAfterAccess(cacheExpiration.getSeconds, TimeUnit.SECONDS)
    .removalListener(removalListener)
    .build[K, V]

  private lazy val removalListener = new RemovalListener[K, V] {
    override def onRemoval(notification: RemovalNotification[K, V]): Unit = {
      val key = notification.getKey
      notification.getCause match {
        case RemovalCause.REPLACED =>
          logger.debug(s"updating value for $key")

        case RemovalCause.EXPIRED =>
          logger.info(s"removing $key from registry after expiration")
          expirationListener.apply(key)

        case _ =>
          throw new IllegalStateException(s"unsupported notification_cause: ${notification.getCause} for cache")
      }
    }
  }
}
