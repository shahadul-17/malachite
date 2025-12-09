package com.malachite.core.common

import org.apache.logging.log4j.{Level, Logger}

trait BackgroundWorkerQueue[T] {

	protected val logger: Logger

	private def toTimeoutInMillisecondsOpt(timeoutInMilliseconds: Long): Option[Long]
		= Option(timeoutInMilliseconds).filterNot(_ < 1)

	def enqueue(task: T, timeoutInMilliseconds: Long): Unit
		= enqueue(task, toTimeoutInMillisecondsOpt(timeoutInMilliseconds))

	def tryEnqueue(task: T, timeoutInMilliseconds: Long): Boolean = {
		try {
			enqueue(task, timeoutInMilliseconds)

			true
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while placing the task in queue.", exception)

				false
		}
	}

	def enqueue(task: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit

	def tryEnqueue(task: T, timeoutInMillisecondsOpt: Option[Long] = None): Boolean = {
		try {
			enqueue(task, timeoutInMillisecondsOpt)

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

object BackgroundWorkerQueue {

	private val EMPTY_BACKGROUND_WORKER_QUEUE: BackgroundWorkerQueue[Nothing]
		= new EmptyBackgroundWorkerQueue[Nothing]

	def empty[T]: BackgroundWorkerQueue[T]
		= EMPTY_BACKGROUND_WORKER_QUEUE.asInstanceOf[BackgroundWorkerQueue[T]]
}
