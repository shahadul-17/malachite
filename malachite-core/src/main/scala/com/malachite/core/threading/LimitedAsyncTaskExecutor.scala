package com.malachite.core.threading

import com.malachite.core.utilities.SemaphoreUtilities
import org.apache.logging.log4j.{Level, LogManager, Logger}

import java.util.concurrent.{Callable, locks}
import scala.jdk.CollectionConverters.MapHasAsJava

class LimitedAsyncTaskExecutor private (defaultLimit: Int = 0,
										limitMapByContext: Map[String, Integer] = Map.empty,
									   ) {

	private val logger: Logger = LogManager.getLogger(classOf[LimitedAsyncTaskExecutor])
	private val readWriteLock: locks.ReadWriteLock = new locks.ReentrantReadWriteLock(false)
	private val readLock: locks.Lock = readWriteLock.readLock()
	private val writeLock: locks.Lock = readWriteLock.writeLock()
	private val semaphoreMapByContext: java.util.Map[String, EnhancedSemaphore]
		= new java.util.HashMap[String, EnhancedSemaphore](LimitedAsyncTaskExecutor.SEMAPHORE_MAP_BY_CONTEXT_INITIAL_CAPACITY)
	private val _limitMapByContext: java.util.Map[String, Integer] = limitMapByContext.asJava
	private val _defaultLimit: Int = LimitedAsyncTaskExecutor.sanitizeLimit(defaultLimit)

	if (!_limitMapByContext.isEmpty) {
		val entrySet = _limitMapByContext.entrySet()
		val iterator = entrySet.iterator()

		while (iterator.hasNext) {
			val entry = iterator.next()
			val context = entry.getKey
			val sanitizedLimit = LimitedAsyncTaskExecutor.sanitizeLimit(entry.getValue)

			if (sanitizedLimit != 0) {
				val semaphore = new EnhancedSemaphore(sanitizedLimit, false)

				semaphoreMapByContext.put(context, semaphore)
			}
		}
	}

	def limit(context: String): Int = {
		if (context == null || _limitMapByContext == null) {
			_defaultLimit
		} else {
			_limitMapByContext.getOrDefault(context, _defaultLimit)
		}
	}

	private def retrieveSemaphore(context: String): Option[EnhancedSemaphore] = {
		var semaphoreOpt: Option[EnhancedSemaphore] = None

		readLock.lock()

		try {
			semaphoreOpt = Option(semaphoreMapByContext.get(context))
		} finally {
			readLock.unlock()
		}

		if (semaphoreOpt.isDefined) { return semaphoreOpt }

		writeLock.lock()

		try {
			semaphoreOpt = Option(semaphoreMapByContext.get(context))

			if (_defaultLimit > 0 && semaphoreOpt.isEmpty) {
				logger.log(Level.INFO, "Creating a new semaphore because no semaphore found for the given context, '{}'.", context)

				val semaphore = new EnhancedSemaphore(_defaultLimit, false)

				semaphoreOpt = Some(semaphore)

				semaphoreMapByContext.put(context, semaphore)
			}
		} finally {
			writeLock.unlock()
		}

		semaphoreOpt
	}

	def run[T](task: Callable[T]): AsyncTask[T]
		= run(LimitedAsyncTaskExecutor.DEFAULT_CONTEXT, task)

	def run[T](context: String, task: Callable[T]): AsyncTask[T] = AsyncTask.run(() => {
		retrieveSemaphore(context).map { semaphore =>
			val acquired = SemaphoreUtilities.tryAcquireSemaphore(semaphore)

			if (acquired) {
				try {
					task.call
				} finally {
					SemaphoreUtilities.releaseSemaphore(semaphore)
				}
			} else {
				null.asInstanceOf[T]
			}
		}.getOrElse(task.call)
	})
}

object LimitedAsyncTaskExecutor {

	private val SEMAPHORE_MAP_BY_CONTEXT_INITIAL_CAPACITY = 16
	private val DEFAULT_CONTEXT = "4@42bdb8520c97f!f9b#f5b60V5705a5ad13d0W6"

	private def sanitizeLimit(limit: Int): Int = if (limit < 1) { 0 } else { limit }

	def apply(defaultLimit: Int = 0,
			  limitMapByContext: Map[String, Integer] = Map.empty,
			 ): LimitedAsyncTaskExecutor
		= new LimitedAsyncTaskExecutor(defaultLimit, limitMapByContext)
}
