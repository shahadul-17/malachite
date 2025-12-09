package com.malachite.core.threading

trait BackgroundTask {

	private[threading] def isExecutable(taskExecutor: BackgroundTaskExecutor): Boolean = true

	private[threading] def doExecute(taskExecutor: BackgroundTaskExecutor): Unit = execute()

	protected def execute(): Unit
}
