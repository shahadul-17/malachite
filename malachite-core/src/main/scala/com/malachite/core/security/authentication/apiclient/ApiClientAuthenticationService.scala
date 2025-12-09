package com.malachite.core.security.authentication.apiclient

import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.extensions.AtomicReferenceExtensions.*
import com.malachite.core.security.authentication.abstractions.{AuthenticationService, Credentials}
import com.malachite.core.security.cryptography.{HashProvider, TokenGenerator}
import com.malachite.core.text.{Encoding, JsonSerializer}
import com.malachite.core.web.http.HttpStatusCode
import com.malachite.core.{Environment, SmartException}
import org.apache.logging.log4j.{Level, LogManager}

import scala.compiletime.uninitialized

trait ApiClientAuthenticationService extends AuthenticationService[
	ApiClientAuthenticationRequest,
	ApiClientAuthenticationResponse,
	ApiClientValidationRequestImpl,
	ApiClientValidationResponse,
	ApiClientTokenContent,
] {

	private val logger = LogManager.getLogger(getClass)

	private val lastLoadedApiClientSystemCredentials = new java.util.concurrent.atomic.AtomicReference[Credentials]
	private var _apiClientSystemAuthenticationResponse: ApiClientAuthenticationResponse = uninitialized

	private def configuration = ConfigurationProvider.getConfiguration.apiClientAuthenticationService

	private def authenticate(request: ApiClientAuthenticationRequest, allowSystemUser: Boolean): ApiClientAuthenticationResponse = {
		val apiClientInformation = configuration.whitelistedApiClientInformationMap
			.getOrElse(request.username, throw SmartException(HttpStatusCode.UNAUTHORIZED, "You are not authorized to access the requested resource."))

		if (apiClientInformation.systemUser && !allowSystemUser) {
			logger.log(Level.WARN, "Someone has attempted to authenticate as system user ({}).", request.username)

			throw SmartException(HttpStatusCode.UNAUTHORIZED, "You are not authorized to access the requested resource.")
		}

		val passwordMatched = HashProvider.isMatched(
			request.password.decode(Encoding.UTF_8),
			apiClientInformation.hashedPassword.decode(Encoding.URL_SAFE_BASE_64),
			Environment.secretKey,
		)

		if (!passwordMatched) {
			throw SmartException(HttpStatusCode.UNAUTHORIZED, "Incorrect password provided.")
		}

		val tokenContent = ApiClientTokenContent(
			version = apiClientInformation.version,
			systemUser = apiClientInformation.systemUser,
			issuedAt = System.currentTimeMillis,
			expirationInMilliseconds = apiClientInformation.tokenExpirationInMilliseconds,
			additionalData = request.additionalData,
			username = apiClientInformation.username,
		)
		val tokenContentAsJson = tokenContent.toJson()
		val token = TokenGenerator.generateToken(tokenContentAsJson.decode(Encoding.UTF_8))

		ApiClientAuthenticationResponse(
			apiClientInformation.apiKey,
			token,
		)
	}

	// THIS METHOD SHALL BE CALLED BY ANOTHER BACKEND SERVICE...
	override def authenticate(request: ApiClientAuthenticationRequest): ApiClientAuthenticationResponse
		= authenticate(request, allowSystemUser = false)

	def authenticateSystem: ApiClientAuthenticationResponse = {
		val currentSystemCredentials = Environment.apiClientSystemCredentials

		if (!lastLoadedApiClientSystemCredentials.inverseCompareAndSet(currentSystemCredentials)) {
			logger.log(Level.DEBUG, "Returning cached API client system authentication response.")

			return _apiClientSystemAuthenticationResponse
		}

		logger.log(Level.DEBUG, "Performing API client system authentication.")

		val authenticationResponse = authenticate(ApiClientAuthenticationRequest(
			username = currentSystemCredentials.username,
			password = currentSystemCredentials.password,
		), allowSystemUser = true)

		_apiClientSystemAuthenticationResponse = authenticationResponse

		logger.log(Level.DEBUG, "Successfully performed API client system authentication.")

		_apiClientSystemAuthenticationResponse
	}

	override protected def deserializeJsonTokenContent(tokenContentAsJson: String): ApiClientTokenContent
		= JsonSerializer.deserialize(tokenContentAsJson, classOf[ApiClientTokenContent])

	override def performAdditionalValidation(request: ApiClientValidationRequestImpl,
											 tokenContent: ApiClientTokenContent,
											): ApiClientValidationResponse = {
		val apiClientInformation = configuration.whitelistedApiClientInformationMap.getOrElse(
			tokenContent.username, throw SmartException(HttpStatusCode.UNAUTHORIZED, "Failed to verify the API client."))

		if (tokenContent.version != apiClientInformation.version) {
			throw SmartException(HttpStatusCode.UNAUTHORIZED, "Unrecognized token version.")
		}
		if (request.apiKey != apiClientInformation.apiKey) {
			throw SmartException(HttpStatusCode.UNAUTHORIZED, "Invalid API key provided.")
		}

		ApiClientValidationResponse(
			apiClientInformation.apiKey,
			tokenContent,
		)
	}
}

class ApiClientAuthenticationServiceImpl extends ApiClientAuthenticationService
