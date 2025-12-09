package com.malachite.core.extensions

object LongExtensions {
	extension (value: Long) {
		def toByteArray: Array[Byte] = Array[Byte](
			(value >>> 56).asInstanceOf[Byte],
			(value >>> 48).asInstanceOf[Byte],
			(value >>> 40).asInstanceOf[Byte],
			(value >>> 32).asInstanceOf[Byte],
			(value >>> 24).asInstanceOf[Byte],
			(value >>> 16).asInstanceOf[Byte],
			(value >>> 8).asInstanceOf[Byte],
			value.asInstanceOf[Byte]
		)
	}
}
