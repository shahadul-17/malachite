package com.malachite.core.utilities

import org.apache.logging.log4j.{Level, LogManager, Logger}
import java.util.concurrent.Semaphore

object SemaphoreUtilities {

	private val logger: Logger = LogManager.getLogger(getClass)

	def tryAcquireSemaphore(semaphore: Semaphore): Boolean = {
		Option(semaphore).map { semaphore =>
			try {
				semaphore.acquire()

				true
			} catch {
				case exception: Exception =>
					logger.log(Level.WARN, "An exception occurred while acquiring the semaphore.", exception)

					false
			}
		}.getOrElse {
			logger.log(Level.WARN, "Could not acquire the semaphore because it is null.")

			false
		}
	}

	def releaseSemaphore(semaphore: Semaphore): Unit = {
		Option(semaphore).map(_.release).getOrElse {
			logger.log(Level.WARN, "Could not release the semaphore because it is null.")
		}
	}
}
