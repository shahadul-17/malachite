package com.malachite.core.security.authentication.abstractions

import com.malachite.core.SmartException
import com.malachite.core.security.cryptography.TokenGenerator
import com.malachite.core.text.Encoding
import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.utilities.DateTimeFormatter
import com.malachite.core.web.http.HttpStatusCode

trait AuthenticationService[
	AuthenticationRequestType <: AuthenticationRequest,
	AuthenticationResponseType <: AuthenticationResponse,
	ValidationRequestType <: ValidationRequest,
	ValidationResponseType <: ValidationResponse[TokenContentType],
	TokenContentType <: TokenContent,
] {

	def authenticate(request: AuthenticationRequestType): AuthenticationResponseType

	protected def deserializeJsonTokenContent(tokenContentAsJson: String): TokenContentType

	protected def performAdditionalValidation(request: ValidationRequestType,
											  tokenContent: TokenContentType
											 ): ValidationResponseType

	def validate(request: ValidationRequestType): ValidationResponseType = {
		TokenGenerator.validateToken(request.token).map { tokenContentAsBytes =>
			val tokenContentAsJson = tokenContentAsBytes.encode(Encoding.UTF_8)
			val tokenContent = deserializeJsonTokenContent(tokenContentAsJson)
			val response = performAdditionalValidation(request, tokenContent)

			tokenContent.expirationInMilliseconds.foreach { expirationInMilliseconds =>
				val currentTimestamp = System.currentTimeMillis
				val tokenExpiresAt = tokenContent.issuedAt + expirationInMilliseconds
				val delta = currentTimestamp - tokenExpiresAt

				// if the token has expired...
				if (delta > 0) {
					// we shall throw an exception...
					throw SmartException(HttpStatusCode.UNAUTHORIZED, s"The token has expired ${DateTimeFormatter.formatTime(delta)} ago.")
				}
			}

			response
		}.getOrElse(throw SmartException(HttpStatusCode.UNAUTHORIZED, "Invalid token provided."))
	}
}
