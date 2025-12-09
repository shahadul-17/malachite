package com.malachite.core.extensions

object IntegerExtensions {
	extension (value: Int) {
		def toByteArray: Array[Byte] = Array[Byte](
			(value >>> 24).asInstanceOf[Byte],
			(value >>> 16).asInstanceOf[Byte],
			(value >>> 8).asInstanceOf[Byte],
			value.asInstanceOf[Byte]
		)
	}
}
