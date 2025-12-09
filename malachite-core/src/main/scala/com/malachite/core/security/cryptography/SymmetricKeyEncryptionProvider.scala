package com.malachite.core.security.cryptography

import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides encryption and decryption capabilities using AES-256 GCM.
 * AES-GCM provides both confidentiality and authentication, making it suitable for secure communications.
 */
object SymmetricKeyEncryptionProvider {

	private val ALGORITHM = "AES"
	private val TRANSFORMATION = "AES/GCM/NoPadding"
	private val TAG_LENGTH_BIT = 128 					// GCM authentication tag length - 128 bits (16 bytes)
	private val INITIALIZATION_VECTOR_LENGTH = 12  		// IV (Initialization Vector) length for GCM - 96 bits (12 bytes)
	private val AES_KEY_BIT = 256    					// AES key size - 256 bits
	private val AES_KEY_LENGTH = AES_KEY_BIT / 8  		// 32 bytes

	private def createCipher(secretKey: Array[Byte], operationMode: Int): Cipher = {
		val cipher = Cipher.getInstance(TRANSFORMATION)
		val hashedSecret = HashProvider.computeHash(secretKey)
		val key = hashedSecret.slice(0, AES_KEY_LENGTH)
		val initializationVector = hashedSecret.slice(AES_KEY_LENGTH, AES_KEY_LENGTH + INITIALIZATION_VECTOR_LENGTH)
		val keySpecification = SecretKeyGenerator.toSymmetricKeySpecification(key, ALGORITHM)
		val algorithmParameterSpecification: AlgorithmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, initializationVector)

		// initializing cipher based on the operation mode...
		cipher.init(operationMode, keySpecification, algorithmParameterSpecification)
		cipher
	}

	def generateSecretKey: Array[Byte] = SecretKeyGenerator.generateSymmetricKey()

	def encrypt(bytes: Array[Byte], secretKey: Array[Byte]): Array[Byte]
		= createCipher(secretKey, Cipher.ENCRYPT_MODE).doFinal(bytes)

	def decrypt(ciphertext: Array[Byte], secretKey: Array[Byte]): Array[Byte]
		= createCipher(secretKey, Cipher.DECRYPT_MODE).doFinal(ciphertext)
}
