package com.malachite.core.kafka3

import com.malachite.core.events.EventQueue
import com.malachite.core.utilities.ThreadUtilities
import org.apache.kafka.common.errors.InterruptException
import org.apache.logging.log4j.{Level, LogManager, Logger}
import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala, SetHasAsJava, SetHasAsScala}

class KafkaConsumerEventQueue(val pollTimeoutInMilliseconds: Int,
							  val sleepTimeoutInMilliseconds: Int,
							  private val consumer: KafkaConsumer,
							 ) extends EventQueue[KafkaRecord] {

	override protected val logger: Logger = LogManager.getLogger(getClass)

	private var currentIndex: Int = 0
	private var kafkaRecords: Array[KafkaRecord] = Array.empty
	private val pollTimeout = java.time.Duration.ofMillis(pollTimeoutInMilliseconds)

	override def enqueue(event: KafkaRecord, timeoutInMillisecondsOpt: Option[Long]): Unit = {
		throw new UnsupportedOperationException("KafkaConsumerEventQueue does not support enqueue operations.")
	}

	override def dequeue: Option[KafkaRecord] = {
		// if the current index exceeds the array of kafka records
		// or if the array is empty...
		if (currentIndex >= kafkaRecords.length || kafkaRecords.isEmpty) {
			// we shall reset the current index...
			currentIndex = 0
			// and also retrieve the kafka records...
			kafkaRecords = consumer.poll(pollTimeout)
		}

		// if the array of kafka records is still empty...
		if (kafkaRecords.isEmpty) {
			// we shall put the thread to sleep for a while...
			ThreadUtilities.trySleep(sleepTimeoutInMilliseconds)

			// we shall return None...
			None
		} else {
			val kafkaRecord = kafkaRecords(currentIndex)

			currentIndex += 1

			Some(kafkaRecord)
		}
	}
}
