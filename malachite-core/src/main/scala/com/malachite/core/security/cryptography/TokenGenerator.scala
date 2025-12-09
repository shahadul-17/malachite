package com.malachite.core.security.cryptography

import com.malachite.core.Environment
import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.security.cryptography.{HashProvider, HybridEncryptionProvider}
import com.malachite.core.text.Encoding
import org.apache.logging.log4j.{Level, LogManager}

object TokenGenerator {

	private val logger = LogManager.getLogger(getClass)

	private val TOKEN_PARTS_SEPARATOR: String = "\\."
	private val TOKEN_ENCODING: Encoding = Encoding.URL_SAFE_BASE_64

	def generateToken(content: Array[Byte]): String = {
		val secretKey = Environment.secretKey
		val signature = HashProvider.computeHash(content, secretKey)
		val publicKey = Environment.publicKey
		val encryptedContent = HybridEncryptionProvider
			.encrypt(content, publicKey)
		val encodedSignature = signature.encode(TOKEN_ENCODING)
		val encodedEncryptedContent = encryptedContent.encode(TOKEN_ENCODING)

		s"$encodedEncryptedContent.$encodedSignature"
	}

	def validateToken(token: String): Option[Array[Byte]] = {
		if (token == null || token.isBlank) { None }
		else {
			val tokenParts = token.split(TOKEN_PARTS_SEPARATOR)

			if (tokenParts.length == 2) {
				try {
					val privateKey = Environment.privateKey
					val encryptedContent = tokenParts(0).decode(TOKEN_ENCODING)
					val content = HybridEncryptionProvider.decrypt(encryptedContent, privateKey)
					val secretKey = Environment.secretKey
					val signature = tokenParts(1).decode(TOKEN_ENCODING)

					if (HashProvider.isMatched(content, signature, secretKey)) {
						Some(content)
					} else {
						None
					}
				} catch {
					case exception: Exception =>
						logger.log(Level.WARN, "An exception occurred during token validation.", exception)

						None
				}
			} else {
				None
			}
		}
	}
}
