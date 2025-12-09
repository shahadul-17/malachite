package com.malachite.core.security.cryptography

import com.malachite.core.text.{Encoder, Encoding}

object HashProvider {

	private val DEFAULT_HASH_ALGORITHM_NAME = "SHA-512"
	private val DEFAULT_KEYED_HASH_ALGORITHM_NAME = "HmacSHA512"

	private def computeHash(messageAsBytes: Array[Byte], algorithmOpt: Option[String]): Array[Byte] = {
		val algorithm = algorithmOpt.getOrElse(DEFAULT_HASH_ALGORITHM_NAME)
		val messageDigest = java.security.MessageDigest.getInstance(algorithm)
		messageDigest.update(messageAsBytes)
		messageDigest.digest
	}

	private def computeKeyedHash(messageAsBytes: Array[Byte], secretKey: Array[Byte], algorithmOpt: Option[String]): Array[Byte] = {
		val algorithm = algorithmOpt.getOrElse(DEFAULT_KEYED_HASH_ALGORITHM_NAME)
		val messageAuthenticationCode = javax.crypto.Mac.getInstance(algorithm)
		val key = SecretKeyGenerator.toSymmetricKeySpecification(secretKey, algorithm)
		messageAuthenticationCode.init(key)
		messageAuthenticationCode.update(messageAsBytes)
		messageAuthenticationCode.doFinal()
	}

	def computeHash(messageAsBytes: Array[Byte],
					secretKey: Array[Byte] = Array.emptyByteArray,
					algorithm: Option[String] = None,
				   ): Array[Byte] = if (secretKey.isEmpty) {
		computeHash(messageAsBytes, algorithm)
	} else {
		computeKeyedHash(messageAsBytes, secretKey, algorithm)
	}
	
	def isMatched(messageAsBytes: Array[Byte],
				  preComputedHash: Array[Byte],
				  secretKey: Array[Byte] = Array.emptyByteArray,
				  algorithm: Option[String] = None,
				 ): Boolean = {
		val computedHash = computeHash(messageAsBytes, secretKey, algorithm)

		java.util.Arrays.equals(computedHash, preComputedHash)
	}
}
