package com.malachite.server

import com.malachite.core.threading.{BackgroundTask, RecurringBackgroundTask}
import org.apache.logging.log4j.LogManager

import java.util.concurrent.atomic.AtomicInteger

class TestTask extends RecurringBackgroundTask {

	private val logger = LogManager.getLogger(getClass)

	override protected def initialExecutionDelay: Long = 5_000L

	override def recurrenceInterval: Long = 1_00L

	override def execute(): Unit = {
		val x = TestTask.count.getAndIncrement()
		logger.info("TEST TASK IS BEING EXECUTED {}", x)
// 		Thread.sleep(1000)
		// logger.info("TEST TASK HAS EXECUTED {}", x)
	}
}

object TestTask {
	private val count = new AtomicInteger(1)
}
