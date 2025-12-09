package com.malachite.core.events

import com.malachite.core.Environment
import com.malachite.core.common.Stopwatch
import com.malachite.core.events.{EventArgs, EventQueue}
import com.malachite.core.threading.{AsyncTask, LimitedAsyncTaskExecutor}
import org.apache.logging.log4j.{Level, Logger}

import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.{ListHasAsScala, SetHasAsScala}

trait AsyncEventDispatcher[T <: EventArgs] extends java.lang.Runnable with java.lang.AutoCloseable {

	private val _disposed = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val _running = new java.util.concurrent.atomic.AtomicBoolean(false)
	private var _eventQueue: EventQueue[T] = uninitialized
	private val stopwatch = Stopwatch().disableNanosecondPrecision
	private var future: java.util.concurrent.Future[?] = uninitialized
	private val topicsModified = new java.util.concurrent.atomic.AtomicBoolean(false)
	@volatile
	private var _topics: Set[String] = Set.empty
	private val listenerListMapByTopic: java.util.Map[String, java.util.List[EventListener[T]]]
		= new java.util.concurrent.ConcurrentHashMap
	private val limitedAsyncTaskExecutor = LimitedAsyncTaskExecutor(defaultLimit = asyncTaskExecutionLimit)

	protected val logger: Logger

	private def doCommit(iterationCount: Int,
						 actionCount: Int,
						 asyncTaskList: java.util.List[AsyncTask[Unit]],
						): Unit = {
		AsyncTask.awaitAllAsyncTasks(asyncTaskList.asScala)
		asyncTaskList.clear()
		commit(iterationCount, actionCount)
	}

	private def commit(iterationCount: Int, actionCount: Int): Unit = {
		try {
			onCommit(iterationCount, actionCount)
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while committing {} actions.", actionCount, exception)
		}

		// resetting the stopwatch (even if exception occurred)...
		stopwatch.startNew
	}

	private def dispose(): Unit = {
		if (_disposed.getAndSet(true)) {
			logger.log(Level.WARN, "Async event dispatcher thread has already been stopped.")
		}

		try {
			onDispose()
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while disposing the async event dispatcher.", exception)
		}
	}

	protected def asyncTaskListInitialCapacity: Int = AsyncEventDispatcher.DEFAULT_ASYNC_TASK_LIST_INITIAL_CAPACITY

	protected def asyncTaskExecutionLimit: Int = AsyncEventDispatcher.DEFAULT_ASYNC_TASK_EXECUTION_LIMIT

	protected def eventQueueCapacity: Int = AsyncEventDispatcher.DEFAULT_EVENT_QUEUE_CAPACITY

	protected def maximumConcurrentActionCount: Int = AsyncEventDispatcher.DEFAULT_MAXIMUM_CONCURRENT_ACTION_COUNT

	protected def commitIntervalInMilliseconds: Long = AsyncEventDispatcher.DEFAULT_COMMIT_INTERVAL_IN_MILLISECONDS

	protected def executorService: java.util.concurrent.ExecutorService = Environment.executorService

	protected def eventQueue: EventQueue[T] = _eventQueue

	protected def initialize(): Unit = { }

	protected def initializeQueue: EventQueue[T] = EventQueue.empty

	protected def onTopicsModified(iterationCount: Int, topics: Set[String]): Unit = { }

	protected def onIteration(iterationCount: Int): Unit = { }

	protected def filterEvent(iterationCount: Int, event: T): Option[T] = Some(event)

	protected def performAction(iterationCount: Int, event: T): Unit = {
		Option(listenerListMapByTopic.get(event.topic))
			.map(_.asScala)
			.getOrElse(Seq.empty)
			.foreach(_.onConsume(event))
	}

	protected def onCommit(iterationCount: Int, actionCount: Int): Unit = { }

	protected def onDispose(): Unit = { }

	def disposed: Boolean = _disposed.get

	def running: Boolean = _running.get

	def topics: Set[String] = _topics

	def publish(message: T, timeoutInMillisecondsOpt: Option[Long] = None): Unit
		= eventQueue.enqueue(message, timeoutInMillisecondsOpt)

	def tryPublish(message: T, timeoutInMillisecondsOpt: Option[Long] = None): Boolean
		= eventQueue.tryEnqueue(message, timeoutInMillisecondsOpt)

	def subscribe(listener: EventListener[T]): Boolean = {
		try {
			val listeners = listenerListMapByTopic.computeIfAbsent(listener.topic, topic =>
				new java.util.concurrent.CopyOnWriteArrayList[EventListener[T]])
			val added = listeners.add(listener)

			if (added) {
				topicsModified.set(true)
			}

			added
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to subscribe you to the event.", exception)

				false
		}
	}

	def unsubscribe(listener: EventListener[T]): Boolean = {
		try {
			val listeners = listenerListMapByTopic.get(listener.topic)

			if (listeners == null || listeners.isEmpty) {
				false
			} else {
				val removed = listeners.remove(listener)

				if (removed) {
					topicsModified.set(true)
				}

				removed
			}
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "We encountered an issue while trying to unsubscribe you from the event.", exception)

				false
		}
	}

	def start: this.type = {
		if (_running.getAndSet(true)) {
			logger.log(Level.WARN, "Async event dispatcher thread is already running.")
		}

		future = executorService.submit(this)

		this
	}

	def stop(interrupt: Boolean = false): this.type = {
		if (!_running.getAndSet(false)) {
			logger.log(Level.WARN, "Async event dispatcher thread has already stopped.")
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

	override def close(): Unit = stop()

	override def run(): Unit = {
		logger.log(Level.DEBUG, "Async event dispatcher thread has started.")

		initialize()

		_eventQueue = initializeQueue

		var i = 0
		var iterationCount = 0
		val asyncTaskList: java.util.List[AsyncTask[Unit]]
			= new java.util.ArrayList(asyncTaskListInitialCapacity)

		while (running) {
			// checking if topics have been modified...
			if (topicsModified.getAndSet(false)) {
				// assigning the new topics to the local variable...
				_topics = listenerListMapByTopic.keySet.asScala.toSet

				// this callback method shall perform any task
				// that shall be done when topics have been modified...
				onTopicsModified(iterationCount, topics)
			}

			// this callback method shall perform any task
			// that shall be done on each iteration...
			onIteration(iterationCount)

			eventQueue
				.tryDequeue
				.flatMap { event => filterEvent(iterationCount, event) }
				.foreach { event =>
					val currentIterationCount = iterationCount

					// executing the task asynchronously...
					asyncTaskList.add(limitedAsyncTaskExecutor.run(() => {
						if (running && !disposed) {
							performAction(currentIterationCount, event)
						}
					}))

					i += 1
					iterationCount += 1

					if (i == maximumConcurrentActionCount) {
						logger.log(Level.DEBUG, "Async event dispatcher thread has reached the maximum concurrent action limit, {}. Waiting for the completion of these tasks to be executed before proceeding further.", maximumConcurrentActionCount)

						doCommit(iterationCount, i, asyncTaskList)

						i = 0
					}
				}

			// checking if the flush interval has been reached...
			if (i > 0 && stopwatch.stop.elapsedTime >= commitIntervalInMilliseconds) {
				logger.log(Level.DEBUG, "Async event dispatcher thread has reached the commit interval, {} (committing {} actions). Waiting for the completion of these actions to be executed before proceeding further.", stopwatch.humanReadableElapsedTime, i)

				doCommit(iterationCount, i, asyncTaskList)

				i = 0
			}
		}

		doCommit(iterationCount, i, asyncTaskList)
		dispose()

		logger.log(Level.DEBUG, "Async event dispatcher thread has stopped gracefully.")
	}
}

private object AsyncEventDispatcher {
	private val DEFAULT_ASYNC_TASK_LIST_INITIAL_CAPACITY = 8192
	private val DEFAULT_ASYNC_TASK_EXECUTION_LIMIT = 64
	private val DEFAULT_EVENT_QUEUE_CAPACITY = 64 * 1024
	private val DEFAULT_MAXIMUM_CONCURRENT_ACTION_COUNT = 1024
	private val DEFAULT_COMMIT_INTERVAL_IN_MILLISECONDS = 1_000
}
