package mix.kafka.monitor

import com.quantifind.kafka.OffsetGetter.OffsetInfo
import com.twitter.util.Time
import org.scalatest.{FlatSpec, Matchers}

class LibratoOffsetReporterSpec extends FlatSpec with Matchers {

  behavior of "offsetsToMetrics"

  it should "convert offsets to metrics with multiple groups, multiple topics and multiple partitions" in {
    val offsets = Seq(
      OffsetInfo("g1", "t1", 1, 1L, 10L, None, Time.epoch, Time.epoch),
      OffsetInfo("g1", "t1", 2, 2L, 10L, None, Time.epoch, Time.epoch),
      OffsetInfo("g1", "t1", 3, 3L, 10L, None, Time.epoch, Time.epoch),

      OffsetInfo("g1", "t2", 1, 4L, 10L, None, Time.epoch, Time.epoch),
      OffsetInfo("g1", "t2", 2, 5L, 10L, None, Time.epoch, Time.epoch),

      OffsetInfo("g2", "t1", 1, 6L, 10L, None, Time.epoch, Time.epoch),
      OffsetInfo("g2", "t1", 2, 7L, 10L, None, Time.epoch, Time.epoch),
      OffsetInfo("g2", "t1", 3, 8L, 10L, None, Time.epoch, Time.epoch)
    )

    val expectedMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(9), toJInt(2) -> toJLong(8), toJInt(3) -> toJLong(7)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(6), toJInt(2) -> toJLong(5)),
      GroupTopic("g2", "t1") -> Map(toJInt(1) -> toJLong(4), toJInt(2) -> toJLong(3), toJInt(3) -> toJLong(2))
    )

    LibratoOffsetReporter.offsetsToMetrics(offsets) shouldBe expectedMetrics
  }

  behavior of "mergeMetrics"

  it should "merge metrics by updating values for all existing groups, topics & partitions" in {
    val existingMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(9), toJInt(2) -> toJLong(8)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(3)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(7))
    )

    val newMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(12)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(8)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0))
    )

    val expectedMergedMetrics = Map (
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(12)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(8)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics by updating values for existing groups & topics with new & missing partitions" in {
    val existingMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(9), toJInt(2) -> toJLong(8)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(3)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(7))
    )

    val newMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(10)),
      GroupTopic("g1", "t2") -> Map(toJInt(2) -> toJLong(3), toJInt(3) -> toJLong(5)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0), toJInt(2) -> toJLong(1))
    )

    val expectedMergedMetrics = Map (
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(10), toJInt(2) -> toJLong(8)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(3), toJInt(3) -> toJLong(5)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0), toJInt(2) -> toJLong(1))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics by updating values for existing groups with new & missing topics" in {
    val existingMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(9), toJInt(2) -> toJLong(8)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(3)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(7))
    )

    val newMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(12)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0)),
      GroupTopic("g2", "t3") -> Map(toJInt(1) -> toJLong(10))
    )

    val expectedMergedMetrics = Map (
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(12)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0)),
      GroupTopic("g2", "t3") -> Map(toJInt(1) -> toJLong(10))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics by updating values for new & missing groups" in {
    val existingMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(9), toJInt(2) -> toJLong(8)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(3)),
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(7))
    )

    val newMetrics = Map(
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0)),
      GroupTopic("g3", "t1") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(10)),
      GroupTopic("g3", "t2") -> Map(toJInt(1) -> toJLong(8))
    )

    val expectedMergedMetrics = Map (
      GroupTopic("g2", "t2") -> Map(toJInt(1) -> toJLong(0)),
      GroupTopic("g3", "t1") -> Map(toJInt(1) -> toJLong(3), toJInt(2) -> toJLong(10)),
      GroupTopic("g3", "t2") -> Map(toJInt(1) -> toJLong(8))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics in expected order" in {
    val existingMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(9), toJInt(2) -> toJLong(8), toJInt(3) -> toJLong(7)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(6), toJInt(2) -> toJLong(5)),
      GroupTopic("g2", "t1") -> Map(toJInt(1) -> toJLong(4), toJInt(2) -> toJLong(3), toJInt(3) -> toJLong(2))
    )

    val newMetrics = Map(
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(0), toJInt(2) -> toJLong(1), toJInt(3) -> toJLong(2)),
      GroupTopic("g1", "t2") -> Map(toJInt(2) -> toJLong(3), toJInt(3) -> toJLong(1)),
      GroupTopic("g3", "t3") -> Map(toJInt(1) -> toJLong(4))
    )

    val expectedMergedMetrics = Map (
      GroupTopic("g1", "t1") -> Map(toJInt(1) -> toJLong(0), toJInt(2) -> toJLong(1), toJInt(3) -> toJLong(2)),
      GroupTopic("g1", "t2") -> Map(toJInt(1) -> toJLong(6), toJInt(2) -> toJLong(3), toJInt(3) -> toJLong(1)),
      GroupTopic("g3", "t3") -> Map(toJInt(1) -> toJLong(4))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  behavior of "getMetricName"

  it should "resolve metric name in expected order" in {
    val group = "dummy-group"
    val topic = "dummy-topic"
    val metricSuffix = "metric-suffix"
    val metricKey = GroupTopic(group, topic)
    val expectedMetricName = s"$group.$topic.$metricSuffix"
    LibratoOffsetReporter.getMetricName(metricKey, metricSuffix) shouldBe expectedMetricName
  }

  it should "replace dots with dashes in metric name" in {
    val group = "dummy.group"
    val topic = "dummy.topic"
    val metricSuffix = "new.metric.suffix"
    val resolvedGroup = "dummy-group"
    val resolvedTopic = "dummy-topic"
    val resolvedMetricSuffix = "new-metric-suffix"
    val metricKey = GroupTopic(group, topic)

    val expectedMetricName = s"$resolvedGroup.$resolvedTopic.$resolvedMetricSuffix"
    LibratoOffsetReporter.getMetricName(metricKey, metricSuffix) shouldBe expectedMetricName
  }

  it should "convert words to lowercase metric name" in {
    val group = "DUMMY-group"
    val topic = "dummy-TOPIC"
    val metricSuffix = "Metric-Suffix"
    val resolvedGroup = "dummy-group"
    val resolvedTopic = "dummy-topic"
    val resolvedMetricSuffix = "metric-suffix"
    val metricKey = GroupTopic(group, topic)

    val expectedMetricName = s"$resolvedGroup.$resolvedTopic.$resolvedMetricSuffix"
    LibratoOffsetReporter.getMetricName(metricKey, metricSuffix) shouldBe expectedMetricName
  }

  import java.lang.{Integer => JInt, Long => JLong}

  private val toJInt: Int => JInt = _.asInstanceOf[JInt]

  private val toJLong: Long => JLong = _.asInstanceOf[JLong]
}
