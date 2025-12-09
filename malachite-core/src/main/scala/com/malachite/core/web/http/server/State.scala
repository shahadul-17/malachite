package com.malachite.core.web.http.server

trait State {

	private val _disposed = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val _running = new java.util.concurrent.atomic.AtomicBoolean(false)
	private val _connected = new java.util.concurrent.atomic.AtomicBoolean(false)

	def disposed: Boolean = _disposed.get

	def setDisposedIfNotDisposed(): Boolean = _disposed.compareAndSet(false, true)

	def setNotDisposedIfDisposed(): Boolean = _disposed.compareAndSet(true, false)

	def setDisposed(disposed: Boolean): State = {
		_disposed.set(disposed)

		this
	}

	def running: Boolean = _running.get

	def setRunningIfNotRunning(): Boolean = _running.compareAndSet(false, true)

	def setNotRunningIfRunning(): Boolean = _running.compareAndSet(true, false)

	def setRunning(running: Boolean): State = {
		_running.set(running)

		this
	}

	def connected: Boolean = _connected.get

	def setConnectedIfDisconnected(): Boolean = _connected.compareAndSet(false, true)

	def setDisconnectedIfConnected(): Boolean = _connected.compareAndSet(true, false)

	def setConnected(connected: Boolean): State = {
		_connected.set(connected)

		this
	}
}

object State {
	def apply(): State = new StateImpl
}

private class StateImpl extends State
