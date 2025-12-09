package com.malachite.core.kafka

import com.malachite.core.Environment
import com.malachite.core.threading.AsyncTask
import com.malachite.core.utilities.{CloseableUtilities, ThreadUtilities}
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.logging.log4j.{Level, LogManager}

import scala.compiletime.uninitialized

private[kafka] class KafkaProducerImpl(val configuration: KafkaClientConfiguration) extends KafkaProducer with java.lang.Runnable {

	private val logger = LogManager.getLogger(getClass)
	private val _disposed = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val _connecting = new java.util.concurrent.atomic.AtomicBoolean(false)
	@volatile
	private var _connected = false
	private var producer: org.apache.kafka.clients.producer.Producer[String, String] = uninitialized
	private var thread: java.lang.Thread = uninitialized
	private val producerRecords: java.util.concurrent.BlockingQueue[ProducerRecord[String, String]]
		= new java.util.concurrent.ArrayBlockingQueue(configuration.maximumBufferedRecords)

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
			val properties = KafkaProducerImpl.toProperties(configuration)

			producer = new org.apache.kafka.clients.producer.KafkaProducer(properties)

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

	override def publish(record: KafkaRecord): Boolean = {
		checkDisposed()

		val producerRecord = new ProducerRecord(record.topic, record.key, record.value)

		try {
			producerRecords.add(producerRecord)
		} catch {
			case exception: InterruptedException =>
				// logger.log(Level.ERROR, "An exception occurred while publishing the record.", exception)

				false
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while publishing the record.", exception)

				false
		}
	}

	private def sendInternal(record: ProducerRecord[String, String]): java.util.concurrent.Future[RecordMetadata] = producer.send(record, (metadata, exception) => {
		if (exception == null) {
			logger.log(Level.DEBUG, "Successfully sent the record to Kafka.")
		} else {
			logger.log(Level.ERROR, "An exception occurred while sending the record to Kafka.", exception)
		}
	})

	private def sendInternal(batch: java.util.List[ProducerRecord[String, String]],
							 asyncTasks: java.util.List[AsyncTask[RecordMetadata]],
							): Iterable[Either[Throwable, RecordMetadata]] = {
		val batchIterator = batch.iterator

		while (batchIterator.hasNext) {
			val record = batchIterator.next
			val future = sendInternal(record)
			val asyncTask = AsyncTask.from(future)

			asyncTasks.add(asyncTask)
		}

		val results = AsyncTask.awaitAllAsyncTasks(asyncTasks)

		producer.flush()

		results
	}

	override def run(): Unit = {
		val batch: java.util.List[ProducerRecord[String, String]]
			= new java.util.ArrayList(configuration.maximumProducerRecordBatchSize)
		val asyncTasks: java.util.List[AsyncTask[RecordMetadata]]
			= new java.util.ArrayList(configuration.maximumProducerRecordBatchSize)
		val recordsToDrain = configuration.maximumProducerRecordBatchSize - 1

		while (_connected && !_disposed.get) {
			val producerRecord = producerRecords.take

			batch.add(producerRecord)

			producerRecords.drainTo(batch, recordsToDrain)

			sendInternal(batch, asyncTasks)

			batch.clear()
			asyncTasks.clear()
		}

		CloseableUtilities.tryClose(producer)

		producer = null
		thread = null

		logger.log(Level.INFO, "Kafka producer has stopped gracefully.")
	}

	override def close(): Unit = {
		if (_disposed.getAndSet(true)) {
			logger.log(Level.WARN, "Kafka producer has already been disposed.")
		} else {
			_connected = false
			_connecting.set(false)
		}
	}
}

private object KafkaProducerImpl {
	private def toProperties(configuration: KafkaClientConfiguration): java.util.Properties = {
		val properties = new java.util.Properties
		properties.put("bootstrap.servers", configuration.servers)
		// properties.put("group.id", Environment.instanceId)
		properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
		properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
		properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
		properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
		properties.put("auto.offset.reset", "earliest")
		properties
	}
}
