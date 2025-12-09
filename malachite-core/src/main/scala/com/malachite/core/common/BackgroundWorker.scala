package com.malachite.core.common

import com.malachite.core.Environment
import com.malachite.core.threading.AsyncTask
import org.apache.logging.log4j.{Level, Logger}

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.IterableHasAsScala

trait BackgroundWorker[T] extends java.lang.Runnable with java.lang.AutoCloseable {

	protected val logger: Logger

	private val stopwatch = Stopwatch().disableNanosecondPrecision

	protected val _disposed = new java.util.concurrent.atomic.AtomicBoolean(false)
	protected val _running = new java.util.concurrent.atomic.AtomicBoolean(false)
	protected var future: java.util.concurrent.Future[?] = uninitialized

	private var _taskQueue: BackgroundWorkerQueue[T] = BackgroundWorkerQueue.empty

	protected def taskQueue: BackgroundWorkerQueue[T] = _taskQueue

	protected def executorService: java.util.concurrent.ExecutorService = Environment.executorService

	protected def name: String = getClass.getSimpleName

	protected def taskQueueCapacity: Int = BackgroundWorker.DEFAULT_TASK_QUEUE_CAPACITY

	protected def initializeTaskQueue: BackgroundWorkerQueue[T]
		= new BlockingBackgroundWorkerQueue[T](taskQueueCapacity)

	protected def asyncTaskListInitialCapacity: Int = BackgroundWorker.DEFAULT_ASYNC_TASK_LIST_INITIAL_CAPACITY

	protected def maximumConcurrentTaskCount: Int = BackgroundWorker.DEFAULT_MAXIMUM_CONCURRENT_TASK_COUNT

	protected def flushIntervalInMilliseconds: Long = BackgroundWorker.DEFAULT_FLUSH_INTERVAL_IN_MILLISECONDS

	protected def onInitialize(): Unit = { }

	protected def onIteration(): Unit = { }

	protected def onTaskRetrieve(task: T): Option[T] = Some(task)

	protected def performTask(task: T): Unit

	protected def onFlush(taskCount: Int): Unit = { }

	protected def onDispose(): Unit = { }

	def disposed: Boolean = _disposed.get

	def running: Boolean = _running.get

	def addTask(task: T, timeoutInMillisecondsOpt: Option[Long] = None): Boolean
		= _taskQueue.tryEnqueue(task, timeoutInMillisecondsOpt)

	def start: this.type = {
		if (_running.getAndSet(true)) {
			logger.log(Level.WARN, s"$name is already running.")
		}

		future = executorService.submit(this)

		this
	}

	def stop(interrupt: Boolean = false): this.type = {
		if (!_running.getAndSet(false)) {
			logger.log(Level.WARN, s"$name has already stopped.")
		}

		if (interrupt) {
			future.cancel(interrupt)
		}

		this
	}

	def await(): Unit = {
		if (future != null) {
			try {
				future.get()
			} catch {
				case exception: InterruptedException =>
					// logger.log(Level.WARN, s"$name has been interrupted.", exception)
				case exception: java.util.concurrent.CancellationException =>
					// logger.log(Level.WARN, s"$name has been stopped gracefully.", exception)
			}
		}
	}

	private def flush(taskCount: Int): Unit = {
		try {
			onFlush(taskCount)
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while flushing {} tasks.", taskCount, exception)
		}

		// resetting the stopwatch (even if exception occurred)...
		stopwatch.startNew
	}

	private def dispose(): Unit = {
		if (_disposed.getAndSet(true)) {
			logger.log(Level.WARN, s"$name is already disposed.")
		}

		try {
			onDispose()
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, s"An exception occurred while disposing $name.", exception)
		}
	}

	override def close(): Unit = stop()

	override def run(): Unit = {
		logger.log(Level.DEBUG, s"$name has started.")

		onInitialize()

		_taskQueue = initializeTaskQueue

		var i = 0
		val asyncTaskList: java.util.List[AsyncTask[Unit]]
			= new java.util.ArrayList(asyncTaskListInitialCapacity)

		stopwatch.startNew

		while (running) {
			// this callback method shall perform any task
			// that shall be done on each iteration...
			onIteration()

			_taskQueue
				.tryDequeue
				.flatMap { task => onTaskRetrieve(task) }
				.foreach { element =>
					// executing the task asynchronously...
					asyncTaskList.add(AsyncTask.run(() => {
						if (running && !disposed) {
							performTask(element)
						}
					}))

					i += 1

					if (i == maximumConcurrentTaskCount) {
						logger.log(Level.DEBUG, "{} has reached the maximum concurrent task limit, {}. Waiting for the completion of these tasks to be executed before proceeding further.", name, i)

						AsyncTask.awaitAllAsyncTasks(asyncTaskList.asScala)
						asyncTaskList.clear()
						flush(i)

						i = 0
					}
				}

			// checking if the flush interval has been reached...
			if (i > 0 && stopwatch.stop.elapsedTime >= flushIntervalInMilliseconds) {
				logger.log(Level.DEBUG, "{} has reached the flush interval, {} (flushing {} tasks). Waiting for the completion of these tasks to be executed before proceeding further.", name, stopwatch.humanReadableElapsedTime, i)

				AsyncTask.awaitAllAsyncTasks(asyncTaskList.asScala)
				asyncTaskList.clear()
				flush(i)

				i = 0
			}
		}

		AsyncTask.awaitAllAsyncTasks(asyncTaskList.asScala)
		asyncTaskList.clear()
		flush(i)
		dispose()

		logger.log(Level.DEBUG, s"$name has stopped gracefully.")

		// ThreadUtilities.tryInterruptCurrentThread
	}
}

private object BackgroundWorker {
	private val DEFAULT_ASYNC_TASK_LIST_INITIAL_CAPACITY = 8192
	private val DEFAULT_TASK_QUEUE_CAPACITY = 64 * 1024
	private val DEFAULT_MAXIMUM_CONCURRENT_TASK_COUNT = 1024
	private val DEFAULT_FLUSH_INTERVAL_IN_MILLISECONDS = 1_000
}
