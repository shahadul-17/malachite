package com.malachite.core.kafka

trait KafkaProducer extends java.lang.AutoCloseable {

	val configuration: KafkaClientConfiguration

	def disposed: Boolean

	def connected: Boolean

	def connect: Boolean

	def await(): Unit

	def publish(record: KafkaRecord): Boolean
}

object KafkaProducer {
	def apply(configuration: KafkaClientConfiguration): KafkaProducer
		= new KafkaProducerImpl(configuration)
}
