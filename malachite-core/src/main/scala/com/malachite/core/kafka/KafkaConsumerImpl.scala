package com.malachite.core.kafka

import com.malachite.core.Environment
import com.malachite.core.threading.{AsyncTask, LimitedAsyncTaskExecutor}
import com.malachite.core.utilities.{CloseableUtilities, ThreadUtilities}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.InterruptException
import org.apache.logging.log4j.{Level, LogManager}

import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala, SetHasAsScala}
import scala.compiletime.uninitialized

private[kafka] class KafkaConsumerImpl(val configuration: KafkaClientConfiguration) extends KafkaConsumer with java.lang.Runnable {

	private val logger = LogManager.getLogger(getClass)
	private val _disposed = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val _connecting = new java.util.concurrent.atomic.AtomicBoolean(false)
	@volatile
	private var _connected = false
	private val topicsModified = new java.util.concurrent.atomic.AtomicBoolean(false)
	@volatile
	private var _topics: java.util.Set[String] = java.util.Collections.emptySet
	private var consumer: org.apache.kafka.clients.consumer.Consumer[String, String] = uninitialized
	private var thread: java.lang.Thread = uninitialized
	private val listenerListMapByTopic: java.util.Map[String, java.util.List[KafkaEventListener]]
		= new java.util.concurrent.ConcurrentHashMap

	private def checkDisposed(): Unit = {
		if (_disposed.get) {
			throw new java.lang.IllegalStateException("Operation(s) cannot be performed on a disposed object.")
		}
	}

	override def disposed: Boolean = _disposed.get

	override def connected: Boolean = {
		checkDisposed()

		_connected
	}

	override def topics: Set[String] = {
		checkDisposed()

		_topics.asScala.toSet
	}

	override def connect: Boolean = {
		checkDisposed()

		if (_connected) {
			logger.log(Level.WARN, "Kafka client is already connected to the server.")

			return true
		}

		if (_connecting.getAndSet(true)) {
			logger.log(Level.WARN, "Kafka client is already trying to establish connection to the server.")

			return false
		}

		try {
			val properties = KafkaConsumerImpl.toProperties(configuration)

			consumer = new org.apache.kafka.clients.consumer.KafkaConsumer(properties)

			logger.log(Level.INFO, "Successfully connected to Kafka server.")

			_connected = true
			_connecting.set(false)

			thread = new Thread(this)
			thread.start()

			true
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while connecting to Kafka server.", exception)

				_connected = false
				_connecting.set(false)

				false
		}
	}

	override def await(): Unit = {
		checkDisposed()
		ThreadUtilities.tryJoin(thread)
	}

	override def subscribe(listener: KafkaEventListener): Boolean = {
		checkDisposed()

		try {
			val listeners = listenerListMapByTopic.computeIfAbsent(listener.topic, topic =>
				new java.util.concurrent.CopyOnWriteArrayList[KafkaEventListener])
			val added = listeners.add(listener)

			if (added) {
				topicsModified.set(true)
			}

			added
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to subscribe you to the topic, '{}'.", listener.topic, exception)

				false
		}
	}

	override def unsubscribe(topic: String): Boolean = {
		checkDisposed()

		try {
			val listeners = listenerListMapByTopic.remove(topic)

			!(listeners == null || listeners.isEmpty)
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to unsubscribe you from the topic, '{}'.", topic, exception)

				false
		}
	}

	override def unsubscribe(listener: KafkaEventListener): Boolean = {
		checkDisposed()

		try {
			val listeners = listenerListMapByTopic.get(listener.topic)

			if (listeners == null || listeners.isEmpty) {
				false
			} else {
				val removed = listeners.remove(listener)

				if (removed) {
					topicsModified.set(true)
				}

				removed
			}
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to unsubscribe you from the topic, '{}'.", listener.topic, exception)

				false
		}
	}

	private def pollInternal(pollTimeout: java.time.Duration): Array[KafkaRecord] = {
		if (_disposed.get || topics.isEmpty) { Array.empty }
		else {
			try {
				val records = consumer.poll(pollTimeout)

				if (records.isEmpty) {
					// logger.log(Level.WARN, "No Kafka records found after polling.")

					Array.empty
				} else {
					val kafkaRecords = records.iterator.asScala.map { record =>
						KafkaRecord(record.topic, record.key, record.value, Some(record.partition), Some(record.offset))
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

	override def poll(pollTimeout: java.time.Duration): Array[KafkaRecord] = {
		checkDisposed()
		pollInternal(pollTimeout)
	}

	private def commitInternal(): Unit = consumer.commitAsync((offsets, exception) => {
		if (exception == null) {
			logger.log(Level.DEBUG, "Commit operation completed successfully.")
		} else {
			logger.log(Level.ERROR, "An exception occurred during commit operation.", exception)
		}
	})

	override def commit(): Unit = {
		checkDisposed()
		commitInternal()
	}

	override def run(): Unit = {
		val pollTimeout = java.time.Duration.ofMillis(configuration.pollTimeoutInMilliseconds)
		val limitedAsyncTaskExecutor = LimitedAsyncTaskExecutor(defaultLimit = configuration.maximumConcurrentTaskLimit)
		val asyncTasks: java.util.List[AsyncTask[Unit]] = new java.util.ArrayList

		while (_connected && !_disposed.get) {
			// checking if topics have been modified...
			if (topicsModified.getAndSet(false)) {
				logger.log(Level.INFO, "Subscribing to new topic(s).")

				// assigning the new topics to the local variable...
				_topics = listenerListMapByTopic.keySet

				consumer.unsubscribe()
				consumer.subscribe(_topics)

				logger.log(Level.INFO, "Successfully subscribed to new topic(s).")
			}

			val records = pollInternal(pollTimeout)

			if (records.isEmpty) {
				ThreadUtilities.trySleep(configuration.threadSleepTimeoutInMilliseconds)
			} else {
				records.foreach { record =>
					asyncTasks.add(limitedAsyncTaskExecutor.run(() => {
						if (_connected && !_disposed.get) {
							Option(listenerListMapByTopic.get(record.topic))
								.map(_.asScala)
								.getOrElse(Seq.empty)
								.foreach(_.onConsume(record))
						}
					}))
				}

				AsyncTask.awaitAllAsyncTasks(asyncTasks.asScala)
				asyncTasks.clear()
				commitInternal()
			}
		}

		CloseableUtilities.tryClose(consumer)
		listenerListMapByTopic.clear()

		consumer = null
		thread = null

		logger.log(Level.INFO, "Kafka consumer has stopped gracefully.")
	}

	override def close(): Unit = {
		if (_disposed.getAndSet(true)) {
			logger.log(Level.WARN, "Kafka consumer has already been disposed.")
		} else {
			_connected = false
			_connecting.set(false)
		}
	}
}

private object KafkaConsumerImpl {
	private def toProperties(configuration: KafkaClientConfiguration): java.util.Properties = {
		val properties = new java.util.Properties
		properties.put("bootstrap.servers", configuration.servers)
		properties.put("max.poll.records", configuration.maximumPollRecords)
		properties.put("max.poll.interval.ms", configuration.maximumPollIntervalInMilliseconds)
		properties.put("group.id", Environment.instanceId)
		properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
		properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
		properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
		properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
		properties.put("auto.offset.reset", "earliest")
		properties.put("group.protocol", "CONSUMER")
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
		properties
	}
}
