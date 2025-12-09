package com.malachite.core.kafka

trait KafkaClient extends java.lang.AutoCloseable {

	val configuration: KafkaClientConfiguration

	def disposed: Boolean

	def connected: Boolean

	def topics: Set[String]

	def connect: Boolean

	def await(): Unit
	
	def publish(record: KafkaRecord): Boolean

	def subscribe(listener: KafkaEventListener): Boolean

	def unsubscribe(topic: String): Boolean

	def unsubscribe(listener: KafkaEventListener): Boolean
}

object KafkaClient {
	def apply(configuration: KafkaClientConfiguration): KafkaClient
		= new KafkaClientImpl(configuration)
}
