package com.malachite.core.events

import com.malachite.core.events.EventQueue
import org.apache.logging.log4j.{LogManager, Logger}

class BlockingEventQueue[T](val capacity: Int) extends EventQueue[T] {

	override protected val logger: Logger = LogManager.getLogger(getClass)

	private val queue: java.util.concurrent.BlockingQueue[T]
		= new java.util.concurrent.ArrayBlockingQueue[T](capacity)

	override def enqueue(event: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit = {
		timeoutInMillisecondsOpt match {
			case Some(timeoutInMilliseconds) if timeoutInMilliseconds > 0L =>
				queue.offer(event, timeoutInMilliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)
			case _ =>
				queue.put(event)
		}
	}

	override def dequeue: Option[T] = Option(queue.take)
}
