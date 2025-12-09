package com.malachite.core.utilities

object ReflectionUtilities {

	def createInstance[Type](classOfType: Class[Type]): Type = {
		val constructor = classOfType.getDeclaredConstructor()
		constructor.setAccessible(true)
		constructor.newInstance()
	}
}
