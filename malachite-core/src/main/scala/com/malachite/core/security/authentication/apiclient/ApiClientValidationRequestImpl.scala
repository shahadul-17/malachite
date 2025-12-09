package com.malachite.core.security.authentication.apiclient

import com.malachite.core.security.authentication.abstractions.ValidationRequest

trait ApiClientValidationRequest extends ValidationRequest {
	val apiKey: String
	val token: String
}

case class ApiClientValidationRequestImpl(apiKey: String,
										  token: String,
										 ) extends ApiClientValidationRequest
