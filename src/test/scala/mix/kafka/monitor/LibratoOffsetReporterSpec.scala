package mix.kafka.monitor

import com.quantifind.kafka.OffsetGetter.OffsetInfo
import com.twitter.util.Time
import org.scalatest.{FlatSpec, Matchers}

class LibratoOffsetReporterSpec extends FlatSpec with Matchers {

  behavior of "getMetricName"

  it should "resolve metric name in expected order" in {
    val group = "dummy-group"
    val topic = "dummy-topic"
    val partition = 1
    val offsetInfo = OffsetInfo(group, topic, partition, 0L, 0L, None, Time.epoch, Time.epoch)

    val expectedMetricName = s"$group.$topic.$partition.lag"
    LibratoOffsetReporter.getMetricName(offsetInfo) shouldBe expectedMetricName
  }

  it should "replace dots with dashes in metric name" in {
    val group = "dummy.group"
    val topic = "dummy.topic"
    val resolvedGroup = "dummy-group"
    val resolvedTopic = "dummy-topic"
    val partition = 1
    val offsetInfo = OffsetInfo(group, topic, partition, 0L, 0L, None, Time.epoch, Time.epoch)

    val expectedMetricName = s"$resolvedGroup.$resolvedTopic.$partition.lag"
    LibratoOffsetReporter.getMetricName(offsetInfo) shouldBe expectedMetricName
  }

  it should "convert words to lowercase metric name" in {
    val group = "DUMMY-group"
    val topic = "dummy-TOPIC"
    val resolvedGroup = "dummy-group"
    val resolvedTopic = "dummy-topic"
    val partition = 1
    val offsetInfo = OffsetInfo(group, topic, partition, 0L, 0L, None, Time.epoch, Time.epoch)

    val expectedMetricName = s"$resolvedGroup.$resolvedTopic.$partition.lag"
    LibratoOffsetReporter.getMetricName(offsetInfo) shouldBe expectedMetricName
  }

}
