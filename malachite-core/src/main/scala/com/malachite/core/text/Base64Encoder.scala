package com.malachite.core.text

import java.util.Base64
import java.nio.charset.StandardCharsets
import scala.util.Using

/**
 * A utility class for Base64 encoding and decoding operations.
 */
object Base64Encoder {

	// Thread-safe instances of Base64 encoders and decoders
	private val paddingEnabledBase64Encoder = Base64.getEncoder
	private val paddingEnabledUrlSafeBase64Encoder = Base64.getUrlEncoder
	private val paddingDisabledBase64Encoder = paddingEnabledBase64Encoder.withoutPadding()
	private val paddingDisabledUrlSafeBase64Encoder = paddingEnabledUrlSafeBase64Encoder.withoutPadding()
	private val base64Decoder = Base64.getDecoder
	private val urlSafeBase64Decoder = Base64.getUrlDecoder

	/**
	 * Gets the appropriate Base64 encoder based on provided parameters.
	 *
	 * @param urlSafe        Whether the encoder should be URL-safe
	 * @param paddingEnabled Whether the encoder should include padding characters
	 * @return The appropriate Base64.Encoder instance
	 */
	private def getBase64Encoder(urlSafe: Boolean, paddingEnabled: Boolean): Base64.Encoder =
		(urlSafe, paddingEnabled) match
			case (true, true) => paddingEnabledUrlSafeBase64Encoder
			case (true, false) => paddingDisabledUrlSafeBase64Encoder
			case (false, true) => paddingEnabledBase64Encoder
			case (false, false) => paddingDisabledBase64Encoder

	/**
	 * Gets the appropriate Base64 decoder based on the URL-safe parameter.
	 *
	 * @param urlSafe Whether the decoder should be URL-safe
	 * @return The appropriate Base64.Decoder instance
	 */
	private def getBase64Decoder(urlSafe: Boolean): Base64.Decoder
		= if (urlSafe) { urlSafeBase64Decoder } else { base64Decoder }

	/**
	 * Converts an array of bytes to a Base64 string with customizable parameters.
	 *
	 * @param bytes          Array of bytes to be converted
	 * @param urlSafe        If true, the output will use URL-safe Base64 encoding
	 * @param paddingEnabled If true, the output will include padding characters
	 * @return Base64 encoded string
	 */
	def encode(bytes: Array[Byte], urlSafe: Boolean = true, paddingEnabled: Boolean = false): String
		= getBase64Encoder(urlSafe, paddingEnabled).encodeToString(bytes)

	/**
	 * Decodes a Base64 encoded string to bytes.
	 *
	 * @param encodedText Base64 encoded text to be decoded
	 * @param urlSafe     If true, the input is considered URL-safe Base64 encoded
	 * @return Decoded bytes
	 */
	def decode(encodedText: String, urlSafe: Boolean = true): Array[Byte]
		= getBase64Decoder(urlSafe).decode(encodedText)
}
