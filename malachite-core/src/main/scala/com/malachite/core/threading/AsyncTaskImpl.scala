package com.malachite.core.threading

import com.malachite.core.Environment
import org.apache.logging.log4j.{Level, LogManager}

import java.util.concurrent.{Callable, ExecutorService, Future}
import scala.jdk.CollectionConverters.*

private[threading] class AsyncTaskImpl[T] private(private var _data: T = null.asInstanceOf[T],
												  private var _future: Future[T] = null,
												  private var _throwable: Throwable = null,
												 ) extends AsyncTask[T] {

	private val logger = LogManager.getLogger(getClass)

	val data: T = _data

	val future: Future[T] = _future

	val throwable: Throwable = _throwable

	def await: T = {
		Option(_throwable) match {
			case Some(exception: RuntimeException) =>
				logger.log(Level.ERROR, "A runtime exception occurred while awaiting the task.", exception)

				throw exception
			case Some(throwable) =>
				logger.log(Level.ERROR, "An exception occurred while awaiting the task.", throwable)

				throw RuntimeException(throwable.getMessage, throwable)
			case _ =>
				// DON'T NEED TO DO ANYTHING...!!!
		}

		Option(_future) match {
			case Some(future) =>
				try {
					_data = _future.get()
				} catch {
					case exception: RuntimeException =>
						_throwable = exception

						logger.log(Level.ERROR, "A runtime exception occurred while awaiting the task.", exception)

						throw exception
					case throwable: Throwable =>
						_throwable = throwable

						logger.log(Level.ERROR, "An exception occurred while awaiting the task.", throwable)

						throw RuntimeException(throwable.getMessage, throwable)
				}
			case _ =>
				// DON'T NEED TO DO ANYTHING...!!!
		}

		_data
	}

	def tryAwait: T = {
		try { await } catch {
			case _: Throwable => null.asInstanceOf[T]
		}
	}
}

object AsyncTaskImpl {

	private val logger = LogManager.getLogger(getClass)

	private val EMPTY_ASYNC_TASK: AsyncTaskImpl[?] = new AsyncTaskImpl[Null]()

	/** Returns the ExecutorService used by the async runtime. */
	def executorService: ExecutorService = Environment.executorService

	def execute[T](tasks: Iterable[Callable[T]]): Iterable[Future[T]]
		= executorService.invokeAll(tasks.toList.asJava).asScala

	def from[T](data: T): AsyncTask[T]
		= new AsyncTaskImpl(_data = data)

	def from[T](future: Future[T]): AsyncTask[T]
		= new AsyncTaskImpl(_future = future)

	def from[T](throwable: Throwable): AsyncTask[T]
		= new AsyncTaskImpl(_throwable = throwable)

	def empty: AsyncTask[?] = EMPTY_ASYNC_TASK

	def run[T](task: Callable[T]): AsyncTask[T] = {
		try {
			val future = executorService.submit(task)

			from(future)
		} catch {
			case throwable: Throwable =>
				logger.log(Level.ERROR, "An exception occurred while running the async task.", throwable)

				from(throwable)
		}
	}

	def await[T](tasks: Iterable[Callable[T]]): Iterable[T]
		= execute(tasks).map(_.get)

	def awaitAsyncTasks[T](tasks: Iterable[AsyncTask[T]]): Iterable[T] = tasks.map(_.await)

	def awaitAll[T](tasks: Iterable[Callable[T]]): Iterable[Either[Throwable, T]] = execute(tasks).map { future =>
		try {
			Right(future.get)
		} catch {
			case throwable: Throwable => Left(throwable)
		}
	}

	def awaitAllAsyncTasks[T](tasks: Iterable[AsyncTask[T]]): Iterable[Either[Throwable, T]] = tasks.map { asyncTask =>
		try {
			Right(asyncTask.await)
		} catch {
			case throwable: Throwable => Left(throwable)
		}
	}
}
