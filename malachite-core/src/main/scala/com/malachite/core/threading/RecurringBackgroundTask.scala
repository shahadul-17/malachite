package com.malachite.core.threading

import com.malachite.core.common.Stopwatch

trait RecurringBackgroundTask extends BackgroundTask {

	private val stopwatch = Stopwatch().disableNanosecondPrecision.start

	private var executedOnce: Boolean = false
	private var executable: Boolean = false

	/**
	 * Specifies the initial delay in milliseconds before the recurring background task is executed
	 * for the first time after being scheduled. Subsequent executions will follow the defined
	 * recurrence interval.
	 */
	protected def initialExecutionDelay: Long = 0L

	/**
	 * Defines the interval in milliseconds at which the recurring background
	 * task will execute. The task will execute only if the specified time interval
	 * has elapsed since the last execution.
	 */
	protected def recurrenceInterval: Long

	protected def hasTimeElapsed: Boolean = {
		val interval = if (executedOnce) { recurrenceInterval } else { initialExecutionDelay }

		stopwatch.stop.elapsedTime >= interval
	}

	private[threading] override def isExecutable(taskExecutor: BackgroundTaskExecutor): Boolean = {
		// if stopwatch has not been started yet...
		if (!stopwatch.started) {
			// we must start it...
			stopwatch.start
		}

		executable = hasTimeElapsed

		if (!executable) {
			taskExecutor.addTask(this)
		}

		executable
	}

	private[threading] override def doExecute(taskExecutor: BackgroundTaskExecutor): Unit = {
		// we shall execute the task...
		execute()

		// mark the task as executed once...
		executedOnce = true
		// and restart the stopwatch...
		stopwatch.startNew

		// add the task to the executor queue...
		taskExecutor.addTask(this)
	}

	protected def execute(): Unit
}
