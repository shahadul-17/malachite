package com.malachite.core.threading

import java.util.concurrent.{Callable, ExecutorService, Future}
import scala.jdk.CollectionConverters.IterableHasAsScala

/**
 * Represents an asynchronous task.
 * @tparam T T of the data/result produced by this async task.
 */
trait AsyncTask[T] {

	/**
	 * Gets the data associated with the async task.
	 *
	 * @return The data associated with the async task.
	 *         If no data is associated with the task, null is returned.
	 */
	val data: T

	/**
	 * Gets the future associated with the async task.
	 *
	 * @return The future associated with the async task.
	 *         If no future is associated with the task, null is returned.
	 */
	val future: Future[T]

	/**
	 * Gets the throwable associated with the async task.
	 *
	 * @return The throwable associated with the async task.
	 *         If no throwable is associated with the task, null is returned.
	 */
	val throwable: Throwable

	/** Awaits the async task to finish. May throw RuntimeException if the task failed. */
	def await: T

	/** Awaits the async task to finish. Returns null if an exception occurs. */
	def tryAwait: T
}

object AsyncTask {

	/** Returns the ExecutorService used by the async runtime. */
	def executorService: ExecutorService = AsyncTaskImpl.executorService

	/** Wraps the provided data within an AsyncTask. */
	def from[T](data: T): AsyncTask[T] = AsyncTaskImpl.from(data)

	/** Wraps the provided Future within an AsyncTask. */
	def from[T](future: Future[T]): AsyncTask[T] = AsyncTaskImpl.from(future)

	/** Wraps the provided Throwable within an AsyncTask. */
	def from[T](throwable: Throwable): AsyncTask[T] = AsyncTaskImpl.from(throwable)

	/** Returns an empty AsyncTask that resolves to null. */
	def empty: AsyncTask[?] = AsyncTaskImpl.empty

	/** Asynchronously executes a Callable task. */
	def run[T](task: Callable[T]): AsyncTask[T] = AsyncTaskImpl.run(task)

	def await[T](tasks: Iterable[Callable[T]]): Iterable[T] = AsyncTaskImpl.await(tasks)

	def awaitAsyncTasks[T](tasks: Iterable[AsyncTask[T]]): Iterable[T] = AsyncTaskImpl.awaitAsyncTasks(tasks)

	def awaitAll[T](tasks: Iterable[Callable[T]]): Iterable[Either[Throwable, T]] = AsyncTaskImpl.awaitAll(tasks)

	def awaitAllAsyncTasks[T](tasks: Iterable[AsyncTask[T]]): Iterable[Either[Throwable, T]] = AsyncTaskImpl.awaitAllAsyncTasks(tasks)

	def awaitAllAsyncTasks[T](tasks: java.util.Collection[AsyncTask[T]]): Iterable[Either[Throwable, T]] = AsyncTaskImpl.awaitAllAsyncTasks(tasks.asScala)
}
