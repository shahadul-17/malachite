package com.malachite.core.messaging

import com.malachite.core.events.{AsyncEventDispatcher, BlockingEventQueue, EventArgs, EventQueue}
import org.apache.logging.log4j.{LogManager, Logger}

trait MessageBroker[T <: Message] extends AsyncEventDispatcher[T] {

	override protected def initializeQueue: EventQueue[T]
		= BlockingEventQueue(eventQueueCapacity)
}
