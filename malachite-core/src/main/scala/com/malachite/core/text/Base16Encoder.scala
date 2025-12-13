package com.malachite.core.text

/**
 * A utility object for hexadecimal (Base16) encoding and decoding operations.
 */
object Base16Encoder {

	// Character arrays for hexadecimal symbols
	private val UpperCasedHexadecimalSymbols = Array(
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'A', 'B', 'C', 'D', 'E', 'F'
	)

	private val LowerCasedHexadecimalSymbols = Array(
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'a', 'b', 'c', 'd', 'e', 'f'
	)

	/**
	 * Converts an array of bytes to a hex/base16 string.
	 *
	 * @param bytes      Array of bytes to be converted
	 * @param upperCased If true, the output hexadecimal symbols will be upper-cased
	 * @return Hex/base16 string
	 */
	def encode(bytes: Array[Byte], upperCased: Boolean = false): String = {
		// Select the hexadecimal symbols based on the upperCased flag
		val hexadecimalSymbols =
			if upperCased then UpperCasedHexadecimalSymbols
			else LowerCasedHexadecimalSymbols

		// Create a character array to hold the base-16 representation
		// The length will be twice the number of bytes
		val bytesAsHexadecimalSymbols = new Array[Char](bytes.length * 2)

		// Convert each byte to two hexadecimal characters
		for i <- bytes.indices do
			val j = i * 2
			val byteValue = bytes(i)

			// Get the upper and lower 4 bits of the byte and convert to hex symbols
			val hexadecimalSymbolA = hexadecimalSymbols((byteValue & 0xF0) >> 4)
			val hexadecimalSymbolB = hexadecimalSymbols(byteValue & 0x0F)

			// Store the hex symbols in the character array
			bytesAsHexadecimalSymbols(j) = hexadecimalSymbolA
			bytesAsHexadecimalSymbols(j + 1) = hexadecimalSymbolB

		// Create a string from the character array
		new String(bytesAsHexadecimalSymbols)
	}

	/**
	 * Gets the numerical value (decimal equivalent) of a hex/base-16 symbol.
	 * For example: '1' = 1, 'A'/'a' = 10, 'F'/'f' = 15.
	 *
	 * Note: This method assumes valid hex input for performance. Non-hex
	 * symbols will return erroneous values.
	 *
	 * @param base16Symbol Hex/Base-16 symbol (e.g., '1', 'A', 'f')
	 * @return The decimal value of the hex symbol
	 */
	private def getValueOfBase16Symbol(base16Symbol: Char): Int =
		// Get the ASCII value of the character
		val asciiValue = base16Symbol.toInt

		// Handle digit characters ('0'-'9')
		if asciiValue < 58 then
			asciiValue - 48 // ASCII '0' = 48
		// Handle uppercase letters ('A'-'F')
		else if asciiValue < 71 then
			asciiValue - 55 // ASCII 'A' = 65, and 'A' = 10 in hex, so 65 - 55 = 10
		// Handle lowercase letters ('a'-'f')
		else
			asciiValue - 87 // ASCII 'a' = 97, and 'a' = 10 in hex, so 97 - 87 = 10

	/**
	 * Decodes a hex/base16 encoded string into bytes.
	 *
	 * @param encodedText Hex/base16 encoded string to be decoded
	 * @return Decoded bytes
	 */
	def decode(encodedText: String): Array[Byte] = {
		// If the input is empty, return an empty byte array
		if encodedText == null || encodedText.isEmpty then
			return new Array[Byte](0)

		// Convert the encoded text to a character array
		val encodedTextAsCharacters = encodedText.toCharArray
		val encodedTextLength = encodedTextAsCharacters.length

		// The decoded byte array will be half the length of the hex string
		val byteArrayLength = encodedTextLength / 2
		val bytes = new Array[Byte](byteArrayLength)

		// Process pairs of hex characters to produce bytes
		var i = 0
		var j = 0
		while i < encodedTextLength do
			val base16SymbolA = encodedTextAsCharacters(i)
			val base16SymbolB = encodedTextAsCharacters(i + 1)

			// Convert the hex characters to their decimal values
			val numericValueA = getValueOfBase16Symbol(base16SymbolA)
			val numericValueB = getValueOfBase16Symbol(base16SymbolB)

			// Combine the values to form a byte
			val byteValue = ((numericValueA << 4) + numericValueB).toByte

			// Store the byte
			bytes(j) = byteValue

			// Move to the next pair of hex characters and the next byte
			i += 2
			j += 1

		bytes
	}
}
