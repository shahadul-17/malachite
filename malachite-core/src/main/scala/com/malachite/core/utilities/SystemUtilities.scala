package com.malachite.core.utilities

object SystemUtilities {

	private var _debugModeEnabled: Boolean = false

	def checkDebugModeEnabled: Boolean = {
		val jvmInputArguments = java.lang.management.ManagementFactory
			.getRuntimeMXBean
			.getInputArguments

		_debugModeEnabled = false

		jvmInputArguments.forEach { argument =>
			if (argument.toLowerCase.contains("-agentlib:jdwp")) {
				_debugModeEnabled = true
			}
		}

		_debugModeEnabled
	}

	def debugModeEnabled: Boolean = _debugModeEnabled
}
