package com.malachite.core.kafka

import com.malachite.core.Environment
import com.malachite.core.utilities.{CloseableUtilities, ThreadUtilities}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.apache.kafka.common.errors.InterruptException
import org.apache.logging.log4j.{Level, LogManager}

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.{IteratorHasAsScala, SetHasAsScala}

private[kafka] class KafkaConnectionImpl(override val configuration: KafkaClientConfiguration) extends KafkaConnection {

	private val logger = LogManager.getLogger(getClass)

	private val _closed = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val _connecting = new java.util.concurrent.atomic.AtomicBoolean(false)
	@volatile
	private var _connected = false
	private val _topicsModified = new java.util.concurrent.atomic.AtomicBoolean(false)

	private val _topics: java.util.Set[String]
		= java.util.concurrent.CopyOnWriteArraySet[_root_.java.lang.String]()

	private var consumer: org.apache.kafka.clients.consumer.Consumer[String, String] = uninitialized
	private var producer: org.apache.kafka.clients.producer.Producer[String, String] = uninitialized

	override def closed: Boolean = _closed.get

	override def checkAndThrowExceptionIfClosed(): Unit = {
		if (closed) {
			throw new java.lang.IllegalStateException("Operation(s) cannot be performed on a disposed object.")
		}
	}

	override def connecting: Boolean = _connecting.get

	override def connected: Boolean = _connected

	override def topicsModified: Boolean = _topicsModified.get

	override def topics: Set[String] = {
		checkAndThrowExceptionIfClosed()

		_topics.asScala.toSet
	}

	override def connect: Boolean = {
		checkAndThrowExceptionIfClosed()

		if (_connected) {
			logger.log(Level.WARN, "Kafka client is already connected to the server.")

			return true
		}

		if (_connecting.getAndSet(true)) {
			logger.log(Level.WARN, "Kafka client is already trying to establish connection to the server.")

			return false
		}

		val connectionTimeout = java.time.Duration.ofMillis(configuration.connectionTimeoutInMilliseconds)

		var consumerOpt: Option[org.apache.kafka.clients.consumer.Consumer[String, String]] = None
		var producerOpt: Option[org.apache.kafka.clients.producer.Producer[String, String]] = None

		try {
			val consumerProperties = KafkaConnectionImpl.toConsumerProperties(configuration)
			val consumer: org.apache.kafka.clients.consumer.Consumer[String, String]
				= new org.apache.kafka.clients.consumer.KafkaConsumer(consumerProperties)
			consumer.listTopics(connectionTimeout)
			consumerOpt = Some(consumer)

			logger.log(Level.INFO, "Successfully connected to Kafka server as consumer.")

			val producerProperties = KafkaConnectionImpl.toProducerProperties(configuration)
			val producer: org.apache.kafka.clients.producer.Producer[String, String]
				= new org.apache.kafka.clients.producer.KafkaProducer(producerProperties)
			producer.clientInstanceId(connectionTimeout)
			producerOpt = Some(producer)

			logger.log(Level.INFO, "Successfully connected to Kafka server as producer.")

			this.consumer = consumer
			this.producer = producer

			_connected = true
			_connecting.set(false)

			logger.log(Level.INFO, "Successfully established a connection to the Kafka server.")

			true
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while connecting to Kafka server.", exception)

				consumerOpt.foreach(CloseableUtilities.tryClose)
				producerOpt.foreach(CloseableUtilities.tryClose)

				_connected = false
				_connecting.set(false)

				false
		}
	}

	override def subscribe(topic: String): Boolean = {
		checkAndThrowExceptionIfClosed()

		val added = _topics.add(topic)

		if (added) {
			_topicsModified.set(true)
		}

		added
	}

	override def unsubscribe(topic: String): Boolean = {
		checkAndThrowExceptionIfClosed()

		val removed = _topics.remove(topic)

		if (removed) {
			_topicsModified.set(true)
		}

		removed
	}

	override def synchronizeTopics(throwExceptionIfClosed: Boolean = true): Boolean = {
		if (throwExceptionIfClosed) {
			checkAndThrowExceptionIfClosed()
		}

		if (closed) {
			logger.log(Level.WARN, "Unable to synchronize topics because this connection has been disposed.")

			return false
		}

		if (!_connected) {
			logger.log(Level.WARN, "Unable to synchronize topics because no connection has been established to Kafka server.")

			return false
		}

		if (_topicsModified.getAndSet(false)) {
			logger.log(Level.INFO, "Synchronizing topics.")

			try {
				consumer.unsubscribe()
				consumer.subscribe(_topics)
			} catch {
				case exception: Exception =>
					logger.log(Level.ERROR, "An exception occurred while synchronizing topics.", exception)

					return false
			}

			logger.log(Level.INFO, "Topics synchronized successfully.")

			true
		} else {
			// logger.log(Level.DEBUG, "Topics have not been modified since last synchronization.")

			false
		}
	}

	override def poll(pollTimeout: java.time.Duration, throwExceptionIfClosed: Boolean = true): Array[KafkaRecord] = {
		if (throwExceptionIfClosed) {
			checkAndThrowExceptionIfClosed()
		}

		if (closed) {
			// logger.log(Level.WARN, "Unable to poll because this connection has been disposed.")

			Array.empty
		} else if (!_connected) {
			// logger.log(Level.WARN, "Unable to poll because no connection has been established to Kafka server.")

			Array.empty
		} else if (_topics.isEmpty) {
			// logger.log(Level.WARN, "Skipped polling because no topics have been subscribed to.")

			Array.empty
		} else {
			try {
				val consumerRecords = consumer.poll(pollTimeout)

				if (consumerRecords == null || consumerRecords.isEmpty) {
					// logger.log(Level.WARN, "No Kafka records found after polling.")

					Array.empty
				} else {
					val records = consumerRecords.iterator.asScala.map { record =>
						KafkaRecord(record.topic, record.value, Option(record.key), Some(record.partition), Some(record.offset))
					}.toArray

					// logger.log(Level.DEBUG, "Successfully retrieved {} records from Kafka.", kafkaRecords.length)

					records
				}
			} catch {
				case exception: IllegalStateException =>
					// logger.log(Level.ERROR, "An exception occurred while retrieving records from Kafka.", exception)

					Array.empty
				case exception: InterruptException =>
					// logger.log(Level.ERROR, "An exception (interruption) occurred while retrieving records from Kafka.", exception)

					Array.empty
				case exception: Exception =>
					logger.log(Level.ERROR, "An exception occurred while retrieving records from Kafka.", exception)

					Array.empty
			}
		}
	}

	override def commit(throwExceptionIfClosed: Boolean = true): Unit = {
		if (throwExceptionIfClosed) {
			checkAndThrowExceptionIfClosed()
		}

		if (closed) {
			logger.log(Level.WARN, "Unable to commit because this connection has been disposed.")

			return
		}

		if (!_connected) {
			logger.log(Level.WARN, "Unable to commit because no connection has been established to Kafka server.")

			return
		}

		consumer.commitAsync((offsets, exception) => {
			if (exception == null) {
				// logger.log(Level.DEBUG, "Commit operation completed successfully.")
			} else {
				logger.log(Level.ERROR, "An exception occurred during commit operation.", exception)
			}
		})
	}

	override def publish(record: KafkaRecord, throwExceptionIfClosed: Boolean = true): java.util.concurrent.Future[org.apache.kafka.clients.producer.RecordMetadata] = {
		if (throwExceptionIfClosed) {
			checkAndThrowExceptionIfClosed()
		}

		if (closed) {
			logger.log(Level.WARN, "Unable to publish because this connection has been disposed.")

			return java.util.concurrent.CompletableFuture.completedFuture(null)
		}

		if (!_connected) {
			logger.log(Level.WARN, "Unable to publish because no connection has been established to Kafka server.")

			return java.util.concurrent.CompletableFuture.completedFuture(null)
		}

		val producerRecord = new ProducerRecord(record.topic, record.key.orNull, record.value)

		producer.send(producerRecord, (metadata, exception) => {
			if (exception == null) {
				// logger.log(Level.DEBUG, "Successfully published the record to Kafka.")
			} else {
				logger.log(Level.ERROR, "An exception occurred while publishing the record to Kafka.", exception)
			}
		})
	}

	override def flush(throwExceptionIfClosed: Boolean = true): Boolean = {
		if (throwExceptionIfClosed) {
			checkAndThrowExceptionIfClosed()
		}

		if (closed) {
			logger.log(Level.WARN, "Unable to flush because this connection has been disposed.")

			return false
		}

		if (!_connected) {
			logger.log(Level.WARN, "Unable to flush because no connection has been established to Kafka server.")

			return false
		}

		try {
			producer.flush()

			true
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while flushing the buffered records.", exception)

				false
		}
	}

	override def close(): Unit = close(false)

	override def close(closed: Boolean): Unit = {
		if (closed) {
			_connected = false
			_connecting.set(false)

			CloseableUtilities.tryClose(consumer)
			CloseableUtilities.tryClose(producer)
			_topics.clear()

			consumer = null
			producer = null

			logger.log(Level.INFO, "Kafka connection closed gracefully.")
		} else {
			if (_closed.getAndSet(true)) {
				logger.log(Level.WARN, "Kafka connection has already been closed.")
			} else {
				_connected = false
				_connecting.set(false)

				logger.log(Level.DEBUG, "Raised flags to close Kafka connection.")
			}
		}
	}

	override def await(): Unit = {
		while (!closed) {
			ThreadUtilities.trySleep(configuration.threadSleepTimeoutInMilliseconds)
		}
	}
}

private object KafkaConnectionImpl {

	private def toConsumerProperties(configuration: KafkaClientConfiguration): java.util.Properties = {
		val properties = new java.util.Properties

		// Core
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.servers)
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, Environment.instanceId)
		properties.put(ConsumerConfig.CLIENT_ID_CONFIG, s"kafka-consumer-${Environment.instanceId}")
		properties.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "CONSUMER")

		// Serialization
		properties.put(
			ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
			"org.apache.kafka.common.serialization.StringDeserializer"
		)
		properties.put(
			ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
			"org.apache.kafka.common.serialization.StringDeserializer"
		)

		// Polling behavior
		properties.put(
			ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
			configuration.maximumPollRecords.toString
		)
		properties.put(
			ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
			configuration.maximumPollIntervalInMilliseconds.toString
		)

		// Liveness
		/*properties.put(
			ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,
			configuration.sessionTimeoutInMilliseconds.toString
		)
		properties.put(
			ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
			configuration.heartbeatIntervalInMilliseconds.toString
		)*/

		// Offset management
		properties.put(
			ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
			configuration.enableAutoCommit.toString
		)
		properties.put(
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
			configuration.autoOffsetReset
		)

		// Timeouts
		properties.put(
			ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG,
			configuration.connectionTimeoutInMilliseconds.toString
		)

		// Safety
		properties.put(
			ConsumerConfig.ISOLATION_LEVEL_CONFIG,
			"read_committed"
		)

		properties
	}

	private def toProducerProperties(configuration: KafkaClientConfiguration): java.util.Properties = {
		val properties = new java.util.Properties

		// Core
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.servers)
		properties.put(ProducerConfig.CLIENT_ID_CONFIG, s"kafka-producer-${Environment.instanceId}")

		// Serialization
		properties.put(
			ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
			"org.apache.kafka.common.serialization.StringSerializer"
		)
		properties.put(
			ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
			"org.apache.kafka.common.serialization.StringSerializer"
		)

		// Reliability
		properties.put(
			ProducerConfig.ACKS_CONFIG,
			configuration.acknowledgments
		)
		properties.put(
			ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
			configuration.enableIdempotence.toString
		)

		// Retry handling
		properties.put(
			ProducerConfig.RETRIES_CONFIG,
			configuration.retryCount.toString
		)
		properties.put(
			ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
			configuration.retryBackoffInMilliseconds.toString
		)

		// Throughput
		properties.put(
			ProducerConfig.LINGER_MS_CONFIG,
			configuration.lingerInMilliseconds.toString
		)
		properties.put(
			ProducerConfig.BATCH_SIZE_CONFIG,
			configuration.maximumProducerRecordBatchSize.toString
		)
		properties.put(
			ProducerConfig.BUFFER_MEMORY_CONFIG,
			configuration.bufferMemoryInBytes.toString
		)

		// Safety
		properties.put(
			ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
			"5" // required for idempotence
		)

		// Timeouts
		properties.put(
			ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
			configuration.connectionTimeoutInMilliseconds.toString
		)
		properties.put(
			ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
			(configuration.connectionTimeoutInMilliseconds * 2).toString
		)

		properties
	}
}
