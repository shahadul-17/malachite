package com.malachite.core.kafka3

import com.malachite.core.Environment
import com.malachite.core.common.{BackgroundWorker, BackgroundWorkerQueue}
import com.malachite.core.events.{AsyncEventDispatcher, EventQueue}
import com.malachite.core.utilities.CloseableUtilities
import org.apache.kafka.common.errors.InterruptException
import org.apache.logging.log4j.{Level, LogManager, Logger}

import java.util.concurrent.ExecutorService
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala, SetHasAsJava, SetHasAsScala}

trait KafkaConsumer extends AsyncEventDispatcher[KafkaRecord] {

	override protected val logger: Logger = LogManager.getLogger(getClass)

	protected val properties: java.util.Properties

	private var consumer: org.apache.kafka.clients.consumer.Consumer[String, String] = uninitialized

	private[kafka3] def poll(pollTimeout: java.time.Duration): Array[KafkaRecord] = {
		if (topics.isEmpty) {
			Array.empty
		} else {
			try {
				val records = consumer.poll(pollTimeout)

				if (records.isEmpty) {
					// logger.log(Level.WARN, "No Kafka records found after polling.")

					Array.empty
				} else {
					val kafkaRecords = records.iterator.asScala.map { record =>
						KafkaRecord(record.topic, record.key, record.value)
					}.toArray

					// logger.log(Level.DEBUG, "Successfully retrieved {} records from Kafka.", kafkaRecords.length)

					kafkaRecords
				}
			} catch {
				case exception: InterruptException =>
					// logger.log(Level.ERROR, "An exception (interruption) occurred while retrieving records from Kafka.", exception)

					Array.empty
				case exception: Exception =>
					logger.log(Level.ERROR, "An exception occurred while retrieving records from Kafka.", exception)

					Array.empty
			}
		}
	}

	override def initialize(): Unit = {
		consumer = new org.apache.kafka.clients.consumer.KafkaConsumer(properties)

		logger.log(Level.INFO, "Successfully connected to Kafka server as consumer.")
	}

	override def initializeQueue: EventQueue[KafkaRecord] = KafkaConsumerEventQueue(
		pollTimeoutInMilliseconds = 100,
		sleepTimeoutInMilliseconds = 1_000,
		consumer = this,
	)

	override protected def onTopicsModified(iterationCount: Int, topics: Set[String]): Unit = {
		logger.log(Level.INFO, "Subscribing to new topic(s).")

		val topics: java.util.Collection[String] = this.topics.asJava

		consumer.unsubscribe()
		consumer.subscribe(topics)

		logger.log(Level.INFO, "Successfully subscribed to new topic(s).")
	}

	override def onCommit(iterationCount: Int, actionCount: Int): Unit = consumer.commitAsync((offsets, exception) => {
		if (exception == null) {
			logger.log(Level.DEBUG, "Successfully committed {} tasks.", actionCount)
		} else {
			logger.log(Level.ERROR, "Failed to commit {} tasks.", actionCount, exception)
		}
	})

	override protected def onDispose(): Unit = CloseableUtilities.tryClose(consumer)
}

object KafkaConsumer {
	def apply(properties: java.util.Properties): KafkaConsumer
		= new KafkaConsumerImpl(properties)
}

private final class KafkaConsumerImpl(override protected val properties: java.util.Properties,
									 ) extends KafkaConsumer
