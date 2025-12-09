package com.malachite.core.common

object ArgumentsParser {

	private var _rawArguments: Array[String] = Array.empty
	private var argumentsMap: Map[String, String] = Map.empty

	private val ARGUMENT_NAME_PREFIX = "--"
	private val DEFAULT_PROFILE = "development"
	private val INITIALIZED = java.util.concurrent.atomic.AtomicBoolean(false)

	def populateArguments(arguments: Array[String]): Unit = {
		if (!INITIALIZED.compareAndSet(false, true)) { return }

		_rawArguments = arguments
		argumentsMap =
			arguments
				.sliding(2, 2) // take pairs of (name, value)
				.collect {
					case Array(name, value) if name.startsWith(ARGUMENT_NAME_PREFIX) =>
						name.stripPrefix(ARGUMENT_NAME_PREFIX) -> value
				}
				.toMap
	}

	lazy val profile: String = getArgument("profile", DEFAULT_PROFILE).toLowerCase

	def rawArguments: Array[String] = _rawArguments

	def getArgument(argumentName: String, defaultValue: String = ""): String =
		argumentsMap
			.get(argumentName)
			.map(_.trim)
			.filter(_.nonEmpty)
			.getOrElse(defaultValue)

	def getArgumentAsCharacter(argumentName: String): Char =
		getArgument(argumentName).headOption.getOrElse('\u0000')

	def getArgumentAsBoolean(argumentName: String): Boolean =
		getArgument(argumentName).equalsIgnoreCase("true")

	def getArgumentAsBoolean(argumentName: String, defaultValue: Boolean): Boolean =
		getArgument(argumentName, String.valueOf(defaultValue)).equalsIgnoreCase("true")

	def getArgumentAsByte(argumentName: String): Byte =
		getArgument(argumentName).toByte

	def getArgumentAsShort(argumentName: String): Short =
		getArgument(argumentName).toShort

	def getArgumentAsInteger(argumentName: String, defaultValue: Int): Int =
		getArgument(argumentName).toIntOption.getOrElse(defaultValue)

	def getArgumentAsLong(argumentName: String, defaultValue: Long): Long =
		getArgument(argumentName).toLongOption.getOrElse(defaultValue)

	def getArgumentAsFloat(argumentName: String): Float =
		getArgument(argumentName).toFloat

	def getArgumentAsDouble(argumentName: String, defaultValue: Double): Double =
		getArgument(argumentName).toDoubleOption.getOrElse(defaultValue)
}
