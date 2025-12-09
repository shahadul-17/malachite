package com.malachite.core.kafka

import com.malachite.core.kafka.KafkaConsumer

private[kafka] class KafkaClientImpl(val configuration: KafkaClientConfiguration) extends KafkaClient {

	private val producer = KafkaProducer(configuration)
	private val consumer = KafkaConsumer(configuration)

	override def disposed: Boolean = producer.disposed || consumer.disposed

	override def connected: Boolean = producer.connected || consumer.connected

	override def topics: Set[String] = consumer.topics

	override def connect: Boolean = producer.connect && consumer.connect

	override def await(): Unit = {
		producer.await()
		consumer.await()
	}

	override def publish(record: KafkaRecord): Boolean
		= producer.publish(record)

	override def subscribe(listener: KafkaEventListener): Boolean = consumer.subscribe(listener)

	override def unsubscribe(topic: String): Boolean = consumer.unsubscribe(topic)

	override def unsubscribe(listener: KafkaEventListener): Boolean = consumer.unsubscribe(listener)

	override def close(): Unit = {
		producer.close()
		consumer.close()
	}
}
