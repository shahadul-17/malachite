package com.malachite.core.common

import com.malachite.core.utilities.DateTimeFormatter

private[common] class StopwatchImpl extends Stopwatch {

	private var nanosecondPrecisionEnabled: Boolean = true
	private var _startTime: Long = 0L
	private var _endTime: Long = 0L
	private var _elapsedTime: Long = 0L

	private def currentTimestamp: Long = {
		if (nanosecondPrecisionEnabled) System.nanoTime()
		else System.currentTimeMillis()
	}

	override def started: Boolean = _startTime > 0L

	override def enableNanosecondPrecision: Stopwatch = {
		nanosecondPrecisionEnabled = true
		this
	}

	override def disableNanosecondPrecision: Stopwatch = {
		nanosecondPrecisionEnabled = false
		this
	}

	override def startTime: Long = _startTime

	override def endTime: Long = _endTime

	override def reset: Stopwatch = {
		_startTime = 0L
		_endTime = 0L
		_elapsedTime = 0L
		this
	}

	override def start: Stopwatch = {
		// if already started or already stopped...
		if (_startTime == 0L && _endTime == 0L) {
			_startTime = currentTimestamp
		}
		this
	}

	override def startNew: Stopwatch = {
		reset
		start
	}

	override def stop: Stopwatch = {
		// if not started, do nothing...
		if (_startTime != 0L) {
			_endTime = currentTimestamp
			_elapsedTime = _endTime - _startTime
		}
		this
	}

	override def elapsedTime: Long = _elapsedTime

	override def humanReadableElapsedTime: String = {
		val elapsed = elapsedTime

		if (nanosecondPrecisionEnabled) {
			DateTimeFormatter.formatNanosecondsTime(elapsed)
		} else {
			DateTimeFormatter.formatTime(elapsed)
		}
	}
}
