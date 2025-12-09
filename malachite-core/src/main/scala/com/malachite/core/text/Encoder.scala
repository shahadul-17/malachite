package com.malachite.core.text

object Encoder {

	def toUtf8(bytes: Array[Byte], offset: Int = 0, length: Int = -1): String
		= new String(bytes, offset, if (length < 0) { bytes.length } else { length }, java.nio.charset.StandardCharsets.UTF_8)

	def fromUtf8(encodedText: String): Array[Byte]
		= encodedText.getBytes(java.nio.charset.StandardCharsets.UTF_8)

	def toBase16(bytes: Array[Byte], upperCased: Boolean = false): String
		= Base16Encoder.encode(bytes, upperCased)

	def fromBase16(encodedText: String): Array[Byte]
		= Base16Encoder.decode(encodedText)

	def toBase64(bytes: Array[Byte], urlSafe: Boolean = true, paddingEnabled: Boolean = false): String
		= Base64Encoder.encode(bytes, urlSafe, paddingEnabled)

	def fromBase64(encodedText: String, urlSafe: Boolean = true): Array[Byte]
		= Base64Encoder.decode(encodedText, urlSafe)

	def encode(bytes: Array[Byte], encoding: Encoding): String = encoding match {
		case null => ""
		case Encoding.UTF_8 => toUtf8(bytes)
		case Encoding.HEX | Encoding.BASE_16 => toBase16(bytes)
		case Encoding.BASE_64 => toBase64(bytes, urlSafe = false)
		case Encoding.URL_SAFE_BASE_64 => toBase64(bytes)
	}

	def decode(encodedText: String, encoding: Encoding): Array[Byte] = encoding match {
		case null => Array.emptyByteArray
		case Encoding.UTF_8 => fromUtf8(encodedText)
		case Encoding.HEX | Encoding.BASE_16 => fromBase16(encodedText)
		case Encoding.BASE_64 => fromBase64(encodedText, urlSafe = false)
		case Encoding.URL_SAFE_BASE_64 => fromBase64(encodedText)
	}
}
