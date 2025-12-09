package com.malachite.core.threading

import com.malachite.core.common.BackgroundWorker
import org.apache.logging.log4j.{LogManager, Logger}

trait BackgroundTaskExecutor extends BackgroundWorker[BackgroundTask] {

	override protected val logger: Logger = LogManager.getLogger(getClass)

	override protected def onTaskRetrieve(task: BackgroundTask): Option[BackgroundTask]
		= Some(task).filter(_.isExecutable(this))

	override protected def performTask(task: BackgroundTask): Unit
		= task.doExecute(this)
}

object BackgroundTaskExecutor {

	private val ASYNC_TASK_LIST_INITIAL_CAPACITY = 8192
	private val TASK_QUEUE_CAPACITY = 64 * 1024
	private val MAXIMUM_CONCURRENT_TASK_COUNT = 1024

	def apply(): BackgroundTaskExecutor
		= new BackgroundTaskExecutorImpl
}

private class BackgroundTaskExecutorImpl extends BackgroundTaskExecutor
