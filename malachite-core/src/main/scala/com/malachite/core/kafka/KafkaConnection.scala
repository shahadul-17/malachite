package com.malachite.core.kafka

trait KafkaConnection extends java.lang.AutoCloseable {

	val configuration: KafkaClientConfiguration

	def closed: Boolean

	def checkAndThrowExceptionIfClosed(): Unit

	def connecting: Boolean

	def connected: Boolean

	def topicsModified: Boolean

	def topics: Set[String]

	def connect: Boolean

	def subscribe(topic: String): Boolean

	def unsubscribe(topic: String): Boolean

	def synchronizeTopics(throwExceptionIfClosed: Boolean = true): Boolean

	def poll(pollTimeout: java.time.Duration, throwExceptionIfClosed: Boolean = true): Array[KafkaRecord]

	def commit(throwExceptionIfClosed: Boolean = true): Unit

	def publish(record: KafkaRecord, throwExceptionIfClosed: Boolean = true): java.util.concurrent.Future[org.apache.kafka.clients.producer.RecordMetadata]

	def flush(throwExceptionIfClosed: Boolean = true): Boolean

	def close(closed: Boolean): Unit

	def await(): Unit
}

object KafkaConnection {
	def apply(configuration: KafkaClientConfiguration): KafkaConnection
		= new KafkaConnectionImpl(configuration)
}
