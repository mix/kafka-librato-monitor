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
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(9), Partition(2) -> Lag(8), Partition(3) -> Lag(7)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(6), Partition(2) -> Lag(5)),
      ConsumerGroupTopic("g2", "t1") -> Map(Partition(1) -> Lag(4), Partition(2) -> Lag(3), Partition(3) -> Lag(2))
    )

    LibratoOffsetReporter.offsetsToMetrics(offsets) shouldBe expectedMetrics
  }

  behavior of "mergeMetrics"

  it should "merge metrics by updating values for all existing groups, topics & partitions" in {
    val existingMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(9), Partition(2) -> Lag(8)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(3)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(7))
    )

    val newMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(3), Partition(2) -> Lag(12)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(8)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0))
    )

    val expectedMergedMetrics = newMetrics
    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics by updating values for existing groups & topics with new & missing partitions" in {
    val existingMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(9), Partition(2) -> Lag(8)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(3)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(7))
    )

    val newMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(10)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(2) -> Lag(3), Partition(3) -> Lag(5)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0), Partition(2) -> Lag(1))
    )

    val expectedMergedMetrics = Map (
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(10), Partition(2) -> Lag(8)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(3), Partition(2) -> Lag(3), Partition(3) -> Lag(5)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0), Partition(2) -> Lag(1))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics by updating values for existing groups with new & missing topics" in {
    val existingMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(9), Partition(2) -> Lag(8)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(3)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(7))
    )

    val newMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(3), Partition(2) -> Lag(12)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0)),
      ConsumerGroupTopic("g2", "t3") -> Map(Partition(1) -> Lag(10))
    )

    val expectedMergedMetrics = Map (
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(3), Partition(2) -> Lag(12)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0)),
      ConsumerGroupTopic("g2", "t3") -> Map(Partition(1) -> Lag(10))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics by updating values for new & missing groups" in {
    val existingMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(9), Partition(2) -> Lag(8)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(3)),
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(7))
    )

    val newMetrics = Map(
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0)),
      ConsumerGroupTopic("g3", "t1") -> Map(Partition(1) -> Lag(3), Partition(2) -> Lag(10)),
      ConsumerGroupTopic("g3", "t2") -> Map(Partition(1) -> Lag(8))
    )

    val expectedMergedMetrics = Map (
      ConsumerGroupTopic("g2", "t2") -> Map(Partition(1) -> Lag(0)),
      ConsumerGroupTopic("g3", "t1") -> Map(Partition(1) -> Lag(3), Partition(2) -> Lag(10)),
      ConsumerGroupTopic("g3", "t2") -> Map(Partition(1) -> Lag(8))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  it should "merge metrics in expected order" in {
    val existingMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(9), Partition(2) -> Lag(8), Partition(3) -> Lag(7)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(6), Partition(2) -> Lag(5)),
      ConsumerGroupTopic("g2", "t1") -> Map(Partition(1) -> Lag(4), Partition(2) -> Lag(3), Partition(3) -> Lag(2)))

    val newMetrics = Map(
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(0), Partition(2) -> Lag(1), Partition(3) -> Lag(2)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(2) -> Lag(3), Partition(3) -> Lag(1)),
      ConsumerGroupTopic("g3", "t3") -> Map(Partition(1) -> Lag(4))
    )

    val expectedMergedMetrics = Map (
      ConsumerGroupTopic("g1", "t1") -> Map(Partition(1) -> Lag(0), Partition(2) -> Lag(1), Partition(3) -> Lag(2)),
      ConsumerGroupTopic("g1", "t2") -> Map(Partition(1) -> Lag(6), Partition(2) -> Lag(3), Partition(3) -> Lag(1)),
      ConsumerGroupTopic("g3", "t3") -> Map(Partition(1) -> Lag(4))
    )

    LibratoOffsetReporter.mergeMetrics(existingMetrics, newMetrics) shouldBe expectedMergedMetrics
  }

  behavior of "getMetricName"

  it should "resolve metric name in expected order" in {
    val group = "dummy-group"
    val topic = "dummy-topic"
    val metricSuffix = "metric-suffix"
    val metricKey = ConsumerGroupTopic(group, topic)
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
    val metricKey = ConsumerGroupTopic(group, topic)

    val expectedMetricName = s"$resolvedGroup.$resolvedTopic.$resolvedMetricSuffix"
    LibratoOffsetReporter.getMetricName(metricKey, metricSuffix) shouldBe expectedMetricName
  }

  it should "convert words to lowercase in metric name" in {
    val group = "DUMMY-group"
    val topic = "dummy-TOPIC"
    val metricSuffix = "Metric-Suffix"
    val resolvedGroup = "dummy-group"
    val resolvedTopic = "dummy-topic"
    val resolvedMetricSuffix = "metric-suffix"
    val metricKey = ConsumerGroupTopic(group, topic)

    val expectedMetricName = s"$resolvedGroup.$resolvedTopic.$resolvedMetricSuffix"
    LibratoOffsetReporter.getMetricName(metricKey, metricSuffix) shouldBe expectedMetricName
  }
}
