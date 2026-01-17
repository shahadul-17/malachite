package com.malachite.core.kafka

import com.malachite.core.utilities.{CloseableUtilities, ThreadUtilities}

trait KafkaClient extends java.lang.AutoCloseable {

	val configuration: KafkaClientConfiguration

	private val connection = KafkaConnection(configuration)
	private val consumer = KafkaConsumer(connection)
	private val consumerThread = new Thread(consumer)
	private val producer = KafkaProducer(connection)
	private val producerThread = new Thread(producer)

	def connect: Boolean = {
		connection.connect

		consumerThread.start()
		producerThread.start()

		connection.connected
	}

	def publish(record: KafkaRecord): Boolean = producer.publish(record)

	def subscribe(listener: KafkaEventListener): Boolean = consumer.subscribe(listener)

	def unsubscribe(topic: String): Boolean = consumer.unsubscribe(topic)

	def unsubscribe(listener: KafkaEventListener): Boolean = consumer.unsubscribe(listener)

	def await(): Unit = {
		ThreadUtilities.tryJoin(consumerThread) 		// waiting for consumer thread to finish...
		ThreadUtilities.tryJoin(producerThread) 		// waiting for producer thread to finish...

		connection.await() 								// waiting for connection to close...
	}

	override def close(): Unit = {
		CloseableUtilities.tryClose(connection)		// this basically raises the flag to close the connection...

		await()										// awaiting the threads to finish...

		connection.close(closed = true)				// closing the underlying connection...
	}
}

object KafkaClient {
	def apply(configuration: KafkaClientConfiguration): KafkaClient
		= new KafkaClientImpl(configuration)
}

private class KafkaClientImpl(val configuration: KafkaClientConfiguration) extends KafkaClient
