package com.malachite.core.kafka3

import com.malachite.core.common.BackgroundWorker
import com.malachite.core.events.{AsyncEventDispatcher, BlockingEventQueue, EventQueue}
import com.malachite.core.utilities.CloseableUtilities
import org.apache.logging.log4j.{Level, LogManager, Logger}

import scala.compiletime.uninitialized

trait KafkaProducer extends AsyncEventDispatcher[KafkaRecord] {

	override protected val logger: Logger = LogManager.getLogger(getClass)

	protected val properties: java.util.Properties

	private var producer: org.apache.kafka.clients.producer.KafkaProducer[String, String] = uninitialized

	override def initialize(): Unit = {
		producer = new org.apache.kafka.clients.producer.KafkaProducer(properties)
	}

	override protected def initializeQueue: EventQueue[KafkaRecord]
		= BlockingEventQueue(eventQueueCapacity)

	override protected def performAction(iterationCount: Int, event: KafkaRecord): Unit = {
		val record = new org.apache.kafka.clients.producer.ProducerRecord(event.topic, event.key, event.value)
		val recordMetadata = producer.send(record).get

		logger.log(Level.DEBUG, "Successfully sent key '{}', value '{}' to topic '{}' (partition = '{}', offset = '{}')",
			event.key, event.value, recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset())
	}

	override def onCommit(iterationCount: Int, taskCount: Int): Unit = {
		producer.flush()

		logger.log(Level.DEBUG, "Successfully flushed {} tasks.", taskCount)
	}

	override protected def onDispose(): Unit = CloseableUtilities.tryClose(producer)
}

object KafkaProducer {
	def apply(properties: java.util.Properties): KafkaProducer
		= new KafkaProducerImpl(properties)
}

private final class KafkaProducerImpl(override protected val properties: java.util.Properties) extends KafkaProducer
