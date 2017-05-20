package mix.kafka.monitor

case class Partition(id: Int)

case class Lag(value: Long)

case class ConsumerGroupTopic(group: String, topic: String)
