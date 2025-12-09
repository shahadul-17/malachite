package com.malachite.core.security.authentication.abstractions

trait AuthenticationRequest {
	val additionalData: Option[Map[String, String]]
}
