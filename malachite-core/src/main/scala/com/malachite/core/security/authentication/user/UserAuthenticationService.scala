package com.malachite.core.security.authentication.user

import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.security.authentication.abstractions.AuthenticationService
import com.malachite.core.security.cryptography.TokenGenerator
import com.malachite.core.text.{Encoding, JsonSerializer}
import org.apache.logging.log4j.{Level, LogManager}

trait UserAuthenticationService extends AuthenticationService[
	UserAuthenticationRequest,
	UserAuthenticationResponse,
	UserValidationRequest,
	UserValidationResponse,
	UserTokenContent,
] {

	private val logger = LogManager.getLogger(getClass)

	private def configuration = ConfigurationProvider.getConfiguration.userAuthenticationService

	// THIS METHOD SHALL BE CALLED BY ANOTHER BACKEND SERVICE...
	def authenticate(request: UserAuthenticationRequest): UserAuthenticationResponse = {
		val tokenContent = UserTokenContent(
			version = request.version,
			issuedAt = System.currentTimeMillis,
			expirationInMilliseconds = configuration.tokenExpirationInMilliseconds,
			additionalData = request.additionalData,
			uniqueId = request.uniqueId,
			issuedBy = request.owner,
			contexts = request.contexts,
			permissions = request.permissions,
		)
		val tokenContentAsJson = tokenContent.toJson()
		val token = TokenGenerator.generateToken(tokenContentAsJson.decode(Encoding.UTF_8))

		UserAuthenticationResponse(token)
	}

	override protected def deserializeJsonTokenContent(tokenContentAsJson: String): UserTokenContent
		= JsonSerializer.deserialize(tokenContentAsJson, classOf[UserTokenContent])

	override def performAdditionalValidation(request: UserValidationRequest,
											 tokenContent: UserTokenContent,
											): UserValidationResponse = UserValidationResponse(tokenContent)
}

class UserAuthenticationServiceImpl extends UserAuthenticationService
