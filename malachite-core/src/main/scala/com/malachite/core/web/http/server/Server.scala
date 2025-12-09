package com.malachite.core.web.http.server

import org.apache.logging.log4j.{Level, LogManager}

trait Server {

	private val logger = LogManager.getLogger(getClass)

	private val _state = State()

	protected def onStart(): Unit

	protected def onStop(): Unit

	protected def onAwait(): Unit

	protected def onDispose(): Unit

	def state: State = _state

	def executorService: java.util.concurrent.ExecutorService

	def start(): Server = {
		if (_state.setRunningIfNotRunning()) {
			onStart()
		} else {
			logger.log(Level.WARN, "Server is already running.")
		}

		this
	}

	def stop(): Server = {
		if (_state.setNotRunningIfRunning()) {
			onStop()
		} else {
			logger.log(Level.WARN, "Server is already stopped.")
		}

		this
	}

	def await(): Server = {
		try {
			onAwait()
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while waiting for the server to stop gracefully.", exception)
		}

		this
	}

	def dispose(): Unit = {
		if (_state.setDisposedIfNotDisposed()) {
			onDispose()
		} else {
			logger.log(Level.WARN, "Server has already been disposed.")
		}
	}
}
