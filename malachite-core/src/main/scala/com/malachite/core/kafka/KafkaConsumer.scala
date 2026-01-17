package com.malachite.core.kafka

import com.malachite.core.kafka.KafkaEventListener
import com.malachite.core.threading.{AsyncTask, LimitedAsyncTaskExecutor}
import org.apache.logging.log4j.{Level, LogManager}
import scala.jdk.CollectionConverters.ListHasAsScala

class KafkaConsumer(val connection: KafkaConnection) extends java.lang.Runnable {

	private val logger = LogManager.getLogger(getClass)
	private val _disposed = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val listenerListMapByTopic: java.util.Map[String, java.util.List[KafkaEventListener]]
		= new java.util.concurrent.ConcurrentHashMap

	def subscribe(listener: KafkaEventListener): Boolean = {
		connection.checkAndThrowExceptionIfClosed()

		try {
			val listeners = listenerListMapByTopic.computeIfAbsent(listener.topic, topic => {
				// if this topic doesn't exist yet, then we need to subscribe to it...
				connection.subscribe(topic)

				// and create a new list to hold the listeners for this topic...
				new java.util.concurrent.CopyOnWriteArrayList[KafkaEventListener]
			})

			listeners.add(listener)
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to subscribe you to the topic, '{}'.", listener.topic, exception)

				false
		}
	}

	def unsubscribe(topic: String): Boolean = {
		connection.checkAndThrowExceptionIfClosed()

		try {
			val listeners = listenerListMapByTopic.remove(topic)
			val removed = !(listeners == null || listeners.isEmpty)

			if (removed) {
				connection.unsubscribe(topic)
			}

			removed
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to unsubscribe you from the topic, '{}'.", topic, exception)

				false
		}
	}

	def unsubscribe(listener: KafkaEventListener): Boolean = {
		connection.checkAndThrowExceptionIfClosed()

		try {
			val listeners = listenerListMapByTopic.get(listener.topic)

			if (listeners == null || listeners.isEmpty) {
				false
			} else {
				val removed = listeners.remove(listener)

				if (removed && listeners.isEmpty) {
					connection.unsubscribe(listener.topic)
				}

				removed
			}
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to unsubscribe you from the topic, '{}'.", listener.topic, exception)

				false
		}
	}

	override def run(): Unit = {
		val pollTimeout = java.time.Duration.ofMillis(connection.configuration.pollTimeoutInMilliseconds)
		val limitedAsyncTaskExecutor = LimitedAsyncTaskExecutor(defaultLimit = connection.configuration.maximumConcurrentTaskLimit)
		val asyncTasks: java.util.List[AsyncTask[Unit]] = new java.util.ArrayList

		while (connection.connected && !connection.closed) {
			connection.synchronizeTopics(throwExceptionIfClosed = false)

			val records = connection.poll(pollTimeout, throwExceptionIfClosed = false)

			if (records.nonEmpty) {
				records.foreach { record =>
					asyncTasks.add(limitedAsyncTaskExecutor.run(() => {
						if (connection.connected && !connection.closed) {
							Option(listenerListMapByTopic.get(record.topic))
								.map(_.asScala)
								.getOrElse(Seq.empty)
								.foreach(_.onConsume(record))
						}
					}))
				}

				AsyncTask.awaitAllAsyncTasks(asyncTasks)
				asyncTasks.clear()
				connection.commit(throwExceptionIfClosed = false)
			}
		}

		listenerListMapByTopic.clear()

		logger.log(Level.INFO, "Kafka consumer has stopped gracefully.")
	}
}
