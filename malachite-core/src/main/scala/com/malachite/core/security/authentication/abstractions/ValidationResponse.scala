package com.malachite.core.security.authentication.abstractions

trait ValidationResponse[TokenContentType <: TokenContent] {
	val tokenContent: TokenContentType
}
