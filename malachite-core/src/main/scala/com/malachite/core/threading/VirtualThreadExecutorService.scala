package com.malachite.core.threading

import com.malachite.core.utilities.ThreadUtilities
import org.apache.logging.log4j.{Level, LogManager}

class VirtualThreadExecutorService(val options: VirtualThreadExecutorServiceOptions = VirtualThreadExecutorServiceOptions.DEFAULT
								  ) extends java.util.concurrent.ExecutorService {
	// if the virtual thread scheduler configuration is provided...
	private val _options = Option(options).getOrElse(VirtualThreadExecutorServiceOptions.DEFAULT)

	// we shall set virtual thread scheduler properties...
	// NOTE: IF SETTING THESE PROPERTIES PROGRAMMATICALLY DOESN'T WORK,
	// YOU MUST PASS THE FOLLOWING VM ARGUMENTS WHILE RUNNING THIS APPLICATION-
	// -Djdk.virtualThreadScheduler.parallelism=16
	// -Djdk.virtualThreadScheduler.maxPoolSize=192
	// -Djdk.virtualThreadScheduler.minRunnable=4
	private val properties = System.getProperties
	properties.setProperty("jdk.virtualThreadScheduler.parallelism", Integer.toString(_options.availablePlatformThreadCount))
	properties.setProperty("jdk.virtualThreadScheduler.maxPoolSize", Integer.toString(_options.maximumPlatformThreadPoolSize))
	properties.setProperty("jdk.virtualThreadScheduler.minRunnable", Integer.toString(_options.minimumUnblockedPlatformThreadCount))

	private val logger = LogManager.getLogger(classOf[VirtualThreadExecutorService])
	private val virtualThreadFactory = java.lang.Thread.ofVirtual()
		.name("virtual-", 1L)
		.factory()
	private val executorService = java.util.concurrent.Executors.newThreadPerTaskExecutor(virtualThreadFactory)

	override def shutdown(): Unit = executorService.shutdown()

	override def shutdownNow(): java.util.List[Runnable] = executorService.shutdownNow()

	override def close(): Unit = executorService.close()

	override def isShutdown: Boolean = executorService.isShutdown

	override def isTerminated: Boolean = executorService.isTerminated

	override def awaitTermination(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean = executorService.awaitTermination(timeout, unit)

	override def submit[T](task: java.util.concurrent.Callable[T]): java.util.concurrent.Future[T] = executorService.submit(task)

	override def submit[T](task: Runnable, result: T): java.util.concurrent.Future[T] = executorService.submit(task, result)

	override def submit(task: Runnable): java.util.concurrent.Future[?] = executorService.submit(task)

	override def invokeAll[T](tasks: java.util.Collection[? <: java.util.concurrent.Callable[T]]): java.util.List[java.util.concurrent.Future[T]] = executorService.invokeAll(tasks)

	override def invokeAll[T](tasks: java.util.Collection[? <: java.util.concurrent.Callable[T]], timeout: Long, unit: java.util.concurrent.TimeUnit): java.util.List[java.util.concurrent.Future[T]] = executorService.invokeAll(tasks, timeout, unit)

	override def invokeAny[T](tasks: java.util.Collection[? <: java.util.concurrent.Callable[T]]): T = executorService.invokeAny(tasks)

	override def invokeAny[T](tasks: java.util.Collection[? <: java.util.concurrent.Callable[T]], timeout: Long, unit: java.util.concurrent.TimeUnit): T = executorService.invokeAny(tasks, timeout, unit)

	override def execute(command: Runnable): Unit = executorService.execute(command)

	def dispose(): Unit = {
		logger.log(Level.INFO, "Releasing all the resources associated with the virtual thread executor service.")

		try {
			shutdownNow

			logger.log(Level.INFO, "Virtual thread executor service shutdown successful.")
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while shutting down the underlying executor service.", exception)
		}

		// waits for the executor service termination...
		while (!ThreadUtilities.awaitExecutorServiceTermination(VirtualThreadExecutorService.EXECUTOR_SERVICE_TERMINATION_WAIT_TIMEOUT_IN_MILLISECONDS, executorService)) {
			logger.log(Level.INFO, "Waiting for the executor service termination.")
		}

		logger.log(Level.INFO, "Successfully terminated the virtual thread executor service.")

		try {
			close()

			logger.log(Level.INFO, "Successfully closed the virtual thread executor service.")
		} catch {
			case exception: Exception =>
				logger.log(Level.WARN, "An exception occurred while closing the underlying executor service.", exception)
		}
	}
}

private object VirtualThreadExecutorService {
	private val EXECUTOR_SERVICE_TERMINATION_WAIT_TIMEOUT_IN_MILLISECONDS = 20
}
