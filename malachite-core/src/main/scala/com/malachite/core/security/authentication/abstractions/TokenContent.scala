package com.malachite.core.security.authentication.abstractions

import com.malachite.core.text.JsonSerializable

trait TokenContent extends JsonSerializable {
	val version: String
	val issuedAt: Long
	val expirationInMilliseconds: Option[Long]
	val additionalData: Option[Map[String, String]]
}
