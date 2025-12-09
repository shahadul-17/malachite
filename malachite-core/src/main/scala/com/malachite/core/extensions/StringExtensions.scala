package com.malachite.core.extensions

import com.malachite.core.text.{Encoder, Encoding}

object StringExtensions {
	extension (encodedText: String) {
		def decode(encoding: Encoding): Array[Byte]
			= Encoder.decode(encodedText, encoding)
	}
}
