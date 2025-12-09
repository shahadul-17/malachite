package com.malachite.core.utilities

import org.apache.logging.log4j.{Level, LogManager}

object CloseableUtilities {

	private val logger = LogManager.getLogger(getClass)

	def tryClose(closeable: AutoCloseable): Unit = {
		if (closeable != null) {
			val className = closeable.getClass.getName

			try {
				closeable.close()

				logger.log(Level.DEBUG, "Successfully closed an instance of '{}'.", className)
			} catch {
				case exception: Exception =>
					logger.log(Level.ERROR, "An exception occurred while closing an instance of '{}'.", className)
			}
		}
	}
}
