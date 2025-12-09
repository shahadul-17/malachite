package com.malachite.core.events

import com.malachite.core.events.EventQueue
import org.apache.logging.log4j.Logger

final class EmptyEventQueue[T] extends EventQueue[T] {

	override protected val logger: Logger = null

	override def enqueue(event: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit = { }

	override def dequeue: Option[T] = None
}
