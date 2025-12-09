package com.malachite.core.common

import org.apache.logging.log4j.{LogManager, Logger}

class BlockingBackgroundWorkerQueue[T](val capacity: Int) extends BackgroundWorkerQueue[T] {

	override protected val logger: Logger = LogManager.getLogger(getClass)

	private val queue: java.util.concurrent.BlockingQueue[T]
		= new java.util.concurrent.ArrayBlockingQueue[T](capacity)

	override def enqueue(task: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit = {
		timeoutInMillisecondsOpt match {
			case Some(timeoutInMilliseconds) if timeoutInMilliseconds > 0L =>
				queue.offer(task, timeoutInMilliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)
			case _ =>
				queue.put(task)
		}
	}

	override def dequeue: Option[T] = Option(queue.take)
}
