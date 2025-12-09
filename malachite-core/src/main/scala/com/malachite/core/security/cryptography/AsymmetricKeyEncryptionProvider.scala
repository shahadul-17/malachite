package com.malachite.core.security.cryptography

import com.malachite.core.security.cryptography.SymmetricKeyEncryptionProvider.ALGORITHM

import java.security.{KeyFactory, KeyPair, KeyPairGenerator, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import javax.crypto.Cipher

/**
 * Provides encryption and decryption capabilities using RSA-4096.
 * RSA provides asymmetric encryption, allowing data to be encrypted with a public key
 * and decrypted only with the corresponding private key.
 */
object AsymmetricKeyEncryptionProvider {

	private val ALGORITHM = "RSA"
	private val TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING"
	private val KEY_SIZE = 4096  										// RSA key size - 4096 bits for strong security
	private val MAX_ENCRYPT_BLOCK_SIZE = (KEY_SIZE / 8) - 66  			// Maximum data length for encryption (RSA-OAEP with SHA-256)
	private val MAX_DECRYPT_BLOCK_SIZE = KEY_SIZE / 8  					// Maximum data length for decryption

	def generateKeyPair: AsymmetricKeyPair
		= SecretKeyGenerator.generateAsymmetricKeyPair(ALGORITHM, KEY_SIZE)

	private def processInChunks(data: Array[Byte], blockSize: Int, cipher: Cipher): Array[Byte] = {
		val dataLength = data.length
		val outputStream = new java.io.ByteArrayOutputStream(dataLength + 1024)
		var offset = 0

		while (offset < dataLength) {
			val currentBlockSize = math.min(blockSize, dataLength - offset)
			val chunk = cipher.doFinal(data, offset, currentBlockSize)

			outputStream.write(chunk)
			offset += currentBlockSize
		}

		outputStream.toByteArray
	}

	private def createCipher(secretKey: Array[Byte], operationMode: Int): Cipher = {
		val cipher = Cipher.getInstance(TRANSFORMATION)
		val keySpecification = operationMode match {
			case Cipher.ENCRYPT_MODE => SecretKeyGenerator.toPublicKeySpecification(secretKey, ALGORITHM)
			case Cipher.DECRYPT_MODE => SecretKeyGenerator.toPrivateKeySpecification(secretKey, ALGORITHM)
			case _ => throw new IllegalArgumentException("Invalid operation mode.")
		}

		cipher.init(operationMode, keySpecification)
		cipher
	}

	def encrypt(bytes: Array[Byte], publicKey: Array[Byte]): Array[Byte] = {
		val cipher = createCipher(publicKey, Cipher.ENCRYPT_MODE)

		processInChunks(bytes, MAX_ENCRYPT_BLOCK_SIZE, cipher)
	}

	def decrypt(ciphertext: Array[Byte], privateKey: Array[Byte]): Array[Byte] = {
		val cipher = createCipher(privateKey, Cipher.DECRYPT_MODE)

		processInChunks(ciphertext, MAX_DECRYPT_BLOCK_SIZE, cipher)
	}
}
