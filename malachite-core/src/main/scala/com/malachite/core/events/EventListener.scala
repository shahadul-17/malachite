package com.malachite.core.events

import com.malachite.core.events.EventArgs

trait EventListener[T <: EventArgs] extends java.lang.Comparable[EventListener[T]] {

	val eventId: Long = EventListener.counter.getAndIncrement

	def topic: String = ""

	def onConsume(message: T): Unit

	override def hashCode(): Int = eventId.hashCode

	override def equals(obj: scala.Any): Boolean = obj match {
		case other: EventListener[?] =>
			eventId == other.eventId
		case _ =>
			false
	}

	override def compareTo(other: EventListener[T]): Int
		= java.lang.Long.compare(eventId, other.eventId)

	override def toString: String = topic
}

private object EventListener {
	private val counter = new java.util.concurrent.atomic.AtomicLong(1L)
}
