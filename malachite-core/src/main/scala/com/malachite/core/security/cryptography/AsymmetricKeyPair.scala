package com.malachite.core.security.cryptography

import com.malachite.core.text.{Encoder, Encoding}

trait AsymmetricKeyPair {
	val publicKey: Array[Byte]
	val privateKey: Array[Byte]
}

object AsymmetricKeyPair {
	def apply(publicKey: Array[Byte],
			  privateKey: Array[Byte]): AsymmetricKeyPair =
		new AsymmetricKeyPairImpl(publicKey, privateKey)
}

private[cryptography] class AsymmetricKeyPairImpl(val publicKey: Array[Byte],
												  val privateKey: Array[Byte]
												 ) extends AsymmetricKeyPair
