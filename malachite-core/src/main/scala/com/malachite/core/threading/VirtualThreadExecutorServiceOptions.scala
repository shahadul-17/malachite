package com.malachite.core.threading

case class VirtualThreadExecutorServiceOptions(availablePlatformThreadCount: Int,
											   maximumPlatformThreadPoolSize: Int,
											   minimumUnblockedPlatformThreadCount: Int
											  )

object VirtualThreadExecutorServiceOptions {
	val DEFAULT: VirtualThreadExecutorServiceOptions = VirtualThreadExecutorServiceOptions(
		availablePlatformThreadCount = 16,
		maximumPlatformThreadPoolSize = 192,
		minimumUnblockedPlatformThreadCount = 4,
	)
}
