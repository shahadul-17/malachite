package com.malachite.core.security.cryptography

object SecretKeyGenerator {

	private val DEFAULT_SECURE_RANDOM_BYTES_LENGTH = 128
	private val SCOPED_SECURE_RANDOM = ScopedValue.newInstance[java.security.SecureRandom]()
	private val DEFAULT_KEY_GENERATION_ALGORITHM = "HmacSHA512"
	private val DEFAULT_KEY_GENERATION_KEY_SIZE = 512

	private def getSecureRandom: java.security.SecureRandom = SCOPED_SECURE_RANDOM.get()

	def generateSecureRandomBytes(length: Int = DEFAULT_SECURE_RANDOM_BYTES_LENGTH): Array[Byte] = {
		val buffer = new Array[Byte](length)
		val secureRandom = getSecureRandom
		secureRandom.nextBytes(buffer)
		buffer
	}

	def generateSymmetricKey(algorithm: String = DEFAULT_KEY_GENERATION_ALGORITHM,
							 keySize: Int = DEFAULT_KEY_GENERATION_KEY_SIZE
							): Array[Byte] = {
		val keyGenerator = javax.crypto.KeyGenerator.getInstance(algorithm)
		keyGenerator.init(keySize)
		keyGenerator.generateKey().getEncoded
	}

	def generateAsymmetricKeyPair(algorithm: String, keySize: Int): AsymmetricKeyPair = {
		val keyPairGenerator = java.security.KeyPairGenerator.getInstance(algorithm)
		keyPairGenerator.initialize(keySize)
		val keyPair = keyPairGenerator.generateKeyPair()
		val publicKey = keyPair.getPublic.getEncoded
		val privateKey = keyPair.getPrivate.getEncoded

		AsymmetricKeyPair(publicKey, privateKey)
	}

	def toSymmetricKeySpecification(secretKey: Array[Byte], algorithm: String): java.security.Key
		= new javax.crypto.spec.SecretKeySpec(secretKey, algorithm)

	def toPublicKeySpecification(publicKeyBytes: Array[Byte], algorithm: String): java.security.Key = {
		val keyFactory = java.security.KeyFactory.getInstance(algorithm)
		val keySpecification: java.security.spec.KeySpec = new java.security.spec.X509EncodedKeySpec(publicKeyBytes)

		keyFactory.generatePublic(keySpecification)
	}

	def toPrivateKeySpecification(privateKeyBytes: Array[Byte], algorithm: String): java.security.Key = {
		val keyFactory = java.security.KeyFactory.getInstance(algorithm)
		val keySpecification: java.security.spec.KeySpec = new java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes)

		keyFactory.generatePrivate(keySpecification)
	}
}
