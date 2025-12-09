package com.malachite.core.kafka

trait KafkaEventListener extends java.lang.Comparable[KafkaEventListener] {

	val eventListenerId: Long = KafkaEventListener.generateEventListenerId

	def topic: String

	def onConsume(record: KafkaRecord): Unit

	override def hashCode: Int = eventListenerId.hashCode

	override def equals(obj: scala.Any): Boolean = obj match {
		case other: KafkaEventListener =>
			eventListenerId == other.eventListenerId
		case _ =>
			false
	}

	override def compareTo(other: KafkaEventListener): Int
		= java.lang.Long.compare(eventListenerId, other.eventListenerId)
}

private object KafkaEventListener {

	private val counter = new java.util.concurrent.atomic.AtomicLong(1L)

	private def generateEventListenerId: Long = counter.getAndIncrement
}
