package com.malachite.core.common

trait Stopwatch {

	def started: Boolean

	/** Enables nanosecond-level precision. */
	def enableNanosecondPrecision: Stopwatch

	/** Disables nanosecond-level precision. */
	def disableNanosecondPrecision: Stopwatch

	/** Resets the stopwatch. */
	def reset: Stopwatch

	/** Starts the stopwatch. */
	def start: Stopwatch

	/** Resets the current state of the stopwatch and starts again. */
	def startNew: Stopwatch

	/** Stops the stopwatch and updates elapsed time. */
	def stop: Stopwatch

	/** Gets the time when the stopwatch was started. */
	def startTime: Long

	/** Gets the time when the stopwatch was stopped. */
	def endTime: Long

	/** Gets the elapsed time. */
	def elapsedTime: Long

	/** Gets the elapsed time in a human-readable format. */
	def humanReadableElapsedTime: String
}

object Stopwatch {
	/** Creates a new instance of Stopwatch. */
	def apply(): Stopwatch = new StopwatchImpl()
}
