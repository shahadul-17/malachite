package com.malachite.core.kafka3

import com.malachite.core.events.EventQueue
import org.apache.logging.log4j.LogManager

import scala.compiletime.uninitialized

trait KafkaClient extends java.lang.AutoCloseable {

	private val logger = LogManager.getLogger(getClass)

	protected val properties: java.util.Properties

	private val producer = KafkaProducer(properties)
	private val consumer = KafkaConsumer(properties)

	def publish(record: KafkaRecord, timeoutInMillisecondsOpt: Option[Long] = None): Boolean
		= producer.tryPublish(record, timeoutInMillisecondsOpt)

	def subscribe(listener: KafkaEventListener): Boolean
		= consumer.subscribe(listener)

	def unsubscribe(listener: KafkaEventListener): Boolean
		= consumer.unsubscribe(listener)

	def start: KafkaClient = {
		producer.start
		consumer.start

		this
	}

	def stop(interrupt: Boolean = false): KafkaClient = {
		producer.stop(interrupt)
		consumer.stop(interrupt)

		this
	}

	def await(): Unit = {
		producer.await()
		consumer.await()
	}

	override def close(): Unit = {
		producer.close()
		consumer.close()
	}
}

object KafkaClient {
	def apply(properties: java.util.Properties): KafkaClient
		= new KafkaClientImpl(properties)
}

private final class KafkaClientImpl(override protected val properties: java.util.Properties) extends KafkaClient
