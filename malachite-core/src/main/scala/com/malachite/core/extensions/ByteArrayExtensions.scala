package com.malachite.core.extensions

import com.malachite.core.text.{Encoder, Encoding}

object ByteArrayExtensions {
	extension (bytes: Array[Byte]) {
		def encode(encoding: Encoding): String
			= Encoder.encode(bytes, encoding)

		def toShort(offset: Int = 0): Short = (((bytes(offset) & 0xFF) << 8) |
			((bytes(offset + 1) & 0xFF) << 0)).toShort

		def toInteger(offset: Int = 0): Int = ((bytes(offset) & 0xFF) << 24) |
			((bytes(offset + 1) & 0xFF) << 16) |
			((bytes(offset + 2) & 0xFF) << 8) |
			((bytes(offset + 3) & 0xFF) << 0)

		def toLong(offset: Int = 0): Long = ((bytes(offset) & 0xFF).toLong << 56) |
			((bytes(offset + 1) & 0xFF).toLong << 48) |
			((bytes(offset + 2) & 0xFF).toLong << 40) |
			((bytes(offset + 3) & 0xFF).toLong << 32) |
			((bytes(offset + 4) & 0xFF).toLong << 24) |
			((bytes(offset + 5) & 0xFF).toLong << 16) |
			((bytes(offset + 6) & 0xFF).toLong << 8) |
			(bytes(offset + 7).toLong & 0xFF)
	}
}
