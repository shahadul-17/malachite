package com.malachite.core.kafka

import com.malachite.core.threading.AsyncTask
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.logging.log4j.{Level, LogManager}

class KafkaProducer(val connection: KafkaConnection) extends java.lang.Runnable {

	private val logger = LogManager.getLogger(getClass)
	private val records: java.util.concurrent.BlockingQueue[KafkaRecord]
		= new java.util.concurrent.ArrayBlockingQueue(connection.configuration.maximumBufferedRecords)

	def publish(record: KafkaRecord): Boolean = {
		connection.checkAndThrowExceptionIfClosed()

		try {
			records.offer(record)
		} catch {
			case exception: InterruptedException =>
				// logger.log(Level.ERROR, "An exception occurred while publishing the record.", exception)

				false
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while publishing the record.", exception)

				false
		}
	}

	private def poll: Option[KafkaRecord] = try {
		val producerRecord = records.poll(
			connection.configuration.pollTimeoutInMilliseconds,
			java.util.concurrent.TimeUnit.MILLISECONDS,
		)

		Option(producerRecord)
	} catch {
		case exception: Exception =>
			logger.log(Level.ERROR, "An exception occurred while polling the records.", exception)

			None
	}

	override def run(): Unit = {
		val batch: java.util.List[KafkaRecord]
			= new java.util.ArrayList(connection.configuration.maximumProducerRecordBatchSize)
		val asyncTasks: java.util.List[AsyncTask[RecordMetadata]]
			= new java.util.ArrayList(connection.configuration.maximumProducerRecordBatchSize)
		val recordsToDrain = connection.configuration.maximumProducerRecordBatchSize - 1

		while (connection.connected && !connection.closed) {
			poll match {
				case Some(record) =>
					batch.add(record)

					records.drainTo(batch, recordsToDrain)

					val batchIterator = batch.iterator

					while (batchIterator.hasNext) {
						val record = batchIterator.next
						val future = connection.publish(record, throwExceptionIfClosed = false)
						val asyncTask = AsyncTask.from(future)

						asyncTasks.add(asyncTask)
					}

					val results = AsyncTask.awaitAllAsyncTasks(asyncTasks)

					connection.flush(throwExceptionIfClosed = false)

					batch.clear()
					asyncTasks.clear()
				case _ =>
					// logger.log(Level.DEBUG, "No records to publish.")
			}
		}
	}
}
