package com.malachite.core.security.cryptography

import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.extensions.IntegerExtensions.*

object HybridEncryptionProvider {

	private def generateSymmetricKey: Array[Byte] = SymmetricKeyEncryptionProvider.generateSecretKey

	def generateAsymmetricKeyPair: AsymmetricKeyPair = AsymmetricKeyEncryptionProvider.generateKeyPair

	def encrypt(bytes: Array[Byte], publicKey: Array[Byte]): Array[Byte] = {
		// generating a random symmetric key...
		val symmetricKey = generateSymmetricKey
		// performing encryption using the randomly generated key...
		val encryptedData = SymmetricKeyEncryptionProvider.encrypt(bytes, symmetricKey)
		// now encrypting the randomly generated symmetric key using our public key...
		val encryptedSymmetricKey = AsymmetricKeyEncryptionProvider.encrypt(symmetricKey, publicKey)
		// getting the length as a byte array...
		val encryptedSymmetricKeyLengthAsBytes = encryptedSymmetricKey.length.toByteArray
		// calculating total length...
		val resultLength = encryptedSymmetricKeyLengthAsBytes.length + encryptedSymmetricKey.length + encryptedData.length
		// initializing a byte array to hold the ciphertext...
		val ciphertext = new Array[Byte](resultLength)

		// copying the key length bytes...
		System.arraycopy(encryptedSymmetricKeyLengthAsBytes, 0, ciphertext, 0, encryptedSymmetricKeyLengthAsBytes.length)
		// copying the encrypted symmetric key...
		System.arraycopy(encryptedSymmetricKey, 0, ciphertext, encryptedSymmetricKeyLengthAsBytes.length, encryptedSymmetricKey.length)
		// copying the encrypted data...
		System.arraycopy(encryptedData, 0, ciphertext, encryptedSymmetricKeyLengthAsBytes.length + encryptedSymmetricKey.length, encryptedData.length)

		// finally returning the ciphertext...
		ciphertext
	}

	def decrypt(ciphertext: Array[Byte], privateKey: Array[Byte]): Array[Byte] = {
		// getting the length of the encrypted symmetric key...
		val encryptedSymmetricKeyLength = ciphertext.toInteger()
		// calculating the offset positions...
		val encryptedSymmetricKeyOffset = Integer.BYTES 			// size of int (4 bytes)
		val encryptedDataOffset = encryptedSymmetricKeyOffset + encryptedSymmetricKeyLength
		// extracting the encrypted symmetric key...
		val encryptedSymmetricKey = new Array[Byte](encryptedSymmetricKeyLength)

		System.arraycopy(ciphertext, encryptedSymmetricKeyOffset, encryptedSymmetricKey, 0, encryptedSymmetricKeyLength)

		// extracting the encrypted data...
		val encryptedDataLength = ciphertext.length - encryptedDataOffset
		val encryptedData = new Array[Byte](encryptedDataLength)

		System.arraycopy(ciphertext, encryptedDataOffset, encryptedData, 0, encryptedDataLength)

		// decrypting the symmetric key using the private key...
		val symmetricKey = AsymmetricKeyEncryptionProvider.decrypt(encryptedSymmetricKey, privateKey)

		// Decrypt the actual data using the recovered symmetric key
		SymmetricKeyEncryptionProvider.decrypt(encryptedData, symmetricKey)
	}
}
