package com.malachite.core.kafka

trait KafkaConsumer extends java.lang.AutoCloseable {

	val configuration: KafkaClientConfiguration

	def disposed: Boolean

	def connected: Boolean

	def topics: Set[String]

	def connect: Boolean

	def await(): Unit
	
	def subscribe(listener: KafkaEventListener): Boolean

	def unsubscribe(topic: String): Boolean

	def unsubscribe(listener: KafkaEventListener): Boolean

	def poll(pollTimeout: java.time.Duration): Array[KafkaRecord]

	def commit(): Unit
}

object KafkaConsumer {
	def apply(configuration: KafkaClientConfiguration): KafkaConsumer
		= new KafkaConsumerImpl(configuration)
}
