package com.malachite.core.utilities

object ThreadUtilities {

	def trySleep(timeoutInMilliseconds: Long): Unit = {
		try {
			Thread.sleep(timeoutInMilliseconds)
		} catch {
			case throwable: Throwable =>
		}
	}

	def tryInterrupt(thread: Thread): Boolean = {
		if (thread != null) {
			try {
				thread.interrupt()

				true
			} catch {
				case throwable: Throwable => false
			}
		} else { false }
	}

	def tryJoin(thread: Thread, waitTimeoutInMilliseconds: Long = 0L): Unit = {
		if (thread != null) {
			try {
				thread.join(waitTimeoutInMilliseconds)
			} catch {
				case throwable: Throwable =>
			}
		}
	}

	def tryInterruptCurrentThread: Boolean = tryInterrupt(Thread.currentThread)

	/**
	 * Blocks until all tasks have completed execution after a shutdown request,
	 * or the timeout occurs, or the current thread is interrupted, whichever happens first.
	 *
	 * @param timeoutInMilliseconds The maximum time to wait (in milliseconds).
	 * @param executorService       Executor service that shall await termination.
	 * @return true if the executor terminated or interrupted while waiting
	 *         and false if the timeout elapsed before termination.
	 */
	def awaitExecutorServiceTermination(timeoutInMilliseconds: Long, executorService: java.util.concurrent.ExecutorService): Boolean = {
		var terminated = false
		try {
			terminated = executorService.awaitTermination(timeoutInMilliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)
		}
		catch {
			case exception: Exception =>
				terminated = true
		}
		terminated
	}

	/**
	 * Retrieves the name of the platform thread on which the current thread
	 * (possibly a virtual thread) is mounted.
	 *
	 * If the thread is not virtual, its name is returned.
	 * If it is a virtual thread and the carrier/platform thread cannot be resolved,
	 * returns None.
	 *
	 * @return Some(platform thread name) or None if not available.
	 */
	def currentPlatformThreadName: Option[String] = {
		val currentThread = Thread.currentThread()

		// if the current thread is a virtual thread...
		if (currentThread.isVirtual) {
			// we shall parse the platform worker name from toString...
			val currentThreadInformation = currentThread.toString.toLowerCase
			val indexOfWorker = currentThreadInformation.indexOf("worker-")
			
			if (indexOfWorker < 0) { None }
			else {
				Some(currentThreadInformation.substring(indexOfWorker))
			}
		} else {
			// non-virtual threads always have a name...
			Some(currentThread.getName)
		}
	}
}
