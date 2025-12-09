package com.malachite.core.events

import org.apache.logging.log4j.{Level, Logger}

trait EventQueue[T] {

	protected val logger: Logger

	private def toTimeoutInMillisecondsOpt(timeoutInMilliseconds: Long): Option[Long]
		= Option(timeoutInMilliseconds).filterNot(_ < 1)

	def enqueue(event: T, timeoutInMilliseconds: Long): Unit
		= enqueue(event, toTimeoutInMillisecondsOpt(timeoutInMilliseconds))

	def tryEnqueue(event: T, timeoutInMilliseconds: Long): Boolean = {
		try {
			enqueue(event, timeoutInMilliseconds)

			true
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while placing the task in queue.", exception)

				false
		}
	}

	def enqueue(event: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit

	def tryEnqueue(event: T, timeoutInMillisecondsOpt: Option[Long] = None): Boolean = {
		try {
			enqueue(event, timeoutInMillisecondsOpt)

			true
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while placing the task in queue.", exception)

				false
		}
	}

	def dequeue: Option[T]

	def tryDequeue: Option[T] = {
		try {
			dequeue
		} catch {
			case exception: InterruptedException =>
				// logger.log(Level.WARN, "An exception (interruption) occurred while retrieving a task from queue.", exception)

				None
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while retrieving a task from queue.", exception)

				None
		}
	}
}

object EventQueue {

	private val EMPTY_EVENT_QUEUE: EventQueue[Nothing]
		= new EmptyEventQueue[Nothing]

	def empty[T]: EventQueue[T]
		= EMPTY_EVENT_QUEUE.asInstanceOf[EventQueue[T]]
}
