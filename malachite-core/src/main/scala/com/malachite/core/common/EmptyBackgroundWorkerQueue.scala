package com.malachite.core.common

import org.apache.logging.log4j.Logger

class EmptyBackgroundWorkerQueue[T] extends BackgroundWorkerQueue[T] {

	override protected val logger: Logger = null

	override def enqueue(task: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit = { }

	override def dequeue: Option[T] = None
}
