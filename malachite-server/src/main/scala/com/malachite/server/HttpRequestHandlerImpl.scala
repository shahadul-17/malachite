package com.malachite.server

import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.{Environment, SmartException, SmartResponse}
import com.malachite.core.security.cryptography.{HybridEncryptionProvider, SecretKeyGenerator}
import com.malachite.core.text.{Encoding, JsonSerializer}
import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.security.authentication.apiclient.{ApiClientAuthenticationRequest, ApiClientAuthenticationService, ApiClientValidationRequestImpl, ApiClientValidationResponse}
import com.malachite.core.security.authentication.user.{UserAuthenticationRequest, UserAuthenticationService, UserValidationRequest, UserValidationResponse}
import com.malachite.core.utilities.{FileSystemUtilities, ReaderUtilities}
import com.malachite.core.web.http.{HttpHeader, HttpMethod, HttpStatusCode}
import com.malachite.core.web.http.server.{HttpRequestHandler, HttpServer, OwnerWebSocketClient, WebSocketClient, WebSocketMessageHandler}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

class HttpRequestHandlerImpl(private val apiClientAuthenticationService: ApiClientAuthenticationService,
							 private val userAuthenticationService: UserAuthenticationService,
							) extends HttpRequestHandler {

	private def authenticateApiClient(method: HttpMethod,
									  request: HttpServletRequest,
									 ): SmartResponse = {
		ReaderUtilities.readString(request.getReader)
			.map(JsonSerializer.deserialize(_, classOf[ApiClientAuthenticationRequest]))
			.map(apiClientAuthenticationService.authenticate)
			.map { authenticationResponse =>
				SmartResponse(HttpStatusCode.OK, "Successfully authenticated API client.")
					.addData("apiKey", authenticationResponse.apiKey)
					.addData("token", authenticationResponse.token)
			}
			.orNull
	}

	private def authenticateUser(method: HttpMethod,
								 request: HttpServletRequest,
								 validationResponse: ApiClientValidationResponse,
								): SmartResponse = {
		ReaderUtilities.readString(request.getReader)
			.map(JsonSerializer.deserialize(_, classOf[UserAuthenticationRequest]))
			.map(_.copy(
				version = validationResponse.tokenContent.version,
				owner = validationResponse.tokenContent.username,
			))
			.map(userAuthenticationService.authenticate)
			.map { authenticationResponse =>
				SmartResponse(HttpStatusCode.OK, "User authentication successful.")
					.addData("token", authenticationResponse.token)
			}
			.orNull
	}

	private def validateApiClient(request: HttpServletRequest): ApiClientValidationResponse = {
		val apiKeyOpt = Option(request.getHeader(HttpHeader.API_KEY.value))
		val tokenOpt = Option(request.getHeader(HttpHeader.TOKEN.value))

		(apiKeyOpt, tokenOpt) match {
			case (Some(apiKey), Some(token)) =>
				val validationRequest = ApiClientValidationRequestImpl(apiKey, token)

				apiClientAuthenticationService.validate(validationRequest)
			case _ =>
				throw SmartException(HttpStatusCode.UNAUTHORIZED, "You are not authorized to access the requested resource.")
		}
	}

	private def validateUser(request: HttpServletRequest): UserValidationResponse = {
		Option(request.getHeader(HttpHeader.TOKEN.value)) match {
			case Some(token) =>
				val validationRequest = UserValidationRequest(token)

				userAuthenticationService.validate(validationRequest)
			case _ =>
				throw SmartException(HttpStatusCode.UNAUTHORIZED, "You are not authorized to access the requested resource.")
		}
	}

	private def reloadConfiguration(method: HttpMethod,
									request: HttpServletRequest,
									validationResponse: ApiClientValidationResponse,
								   ): SmartResponse = {
		SmartResponse(HttpStatusCode.ACCEPTED, "Configuration is being reloaded.")
			.addData("reloadConfiguration", true)
	}

	private def stopApplication(method: HttpMethod,
								request: HttpServletRequest,
								validationResponse: ApiClientValidationResponse,
							   ): SmartResponse = {
		SmartResponse(HttpStatusCode.ACCEPTED, "Application stop request is being processed.")
			.addData("stopApplication", true)
	}

	private def generateKeys(method: HttpMethod,
							 request: HttpServletRequest,
							 validationResponse: ApiClientValidationResponse,
							): SmartResponse = {
		SmartResponse(HttpStatusCode.ACCEPTED, "Key generation request is being processed.")
			.addData("generateKeys", true)
	}

	private def invalidateCachedKeys(method: HttpMethod,
									 request: HttpServletRequest,
									 validationResponse: ApiClientValidationResponse,
									): SmartResponse = {
		SmartResponse(HttpStatusCode.ACCEPTED, "Cached key invalidation request is being processed.")
			.addData("invalidateCachedKeys", true)
	}

	private def invalidateCachedApiClientSystemCredentials(method: HttpMethod,
														   request: HttpServletRequest,
														   validationResponse: ApiClientValidationResponse,
														  ): SmartResponse = {
		SmartResponse(HttpStatusCode.ACCEPTED, "Cached API client system credentials invalidation request is being processed.")
			.addData("invalidateCachedApiClientSystemCredentials", true)
	}

	private def sendMessageToWebSocketClientsBySenderSessionGuid(method: HttpMethod,
																 request: HttpServletRequest,
																 messageHandler: WebSocketMessageHandler,
																): SmartResponse = {
		val validationResponse = validateUser(request)
		val senderSessionGuid = Option(request.getHeader(HttpHeader.SENDER_SESSION_GUID.value))
			.getOrElse(throw SmartException(HttpStatusCode.BAD_REQUEST, "Sender session GUID not provided."))
		val message = ReaderUtilities.readString(request.getReader)
			.getOrElse(throw SmartException(HttpStatusCode.BAD_REQUEST, "No request body provided."))

		messageHandler.handleWebSocketMessage(message, senderSessionGuid)
	}

	private def sendMessageToWebSocketClients(method: HttpMethod,
											  request: HttpServletRequest,
											  messageHandler: WebSocketMessageHandler,
											  validationResponse: ApiClientValidationResponse,
											 ): SmartResponse = {
		val message = ReaderUtilities.readString(request.getReader)
			.getOrElse(throw SmartException(HttpStatusCode.BAD_REQUEST, "No request body provided."))
		val sender: WebSocketClient = OwnerWebSocketClient(
			owner = validationResponse.tokenContent.username,
			nodeId = Environment.instanceId,
		)

		messageHandler.handleWebSocketMessage(message, sender)
	}

	override def onRequestHandled(smartResponse: SmartResponse, httpServer: HttpServer): Unit = {
		smartResponse.getData("reloadConfiguration") match {
			case Some(true) => ConfigurationProvider.tryLoadConfiguration()
			case _ =>
		}

		smartResponse.getData("stopApplication") match {
			case Some(true) => httpServer.stop()
			case _ =>
		}

		smartResponse.getData("generateKeys") match {
			case Some(true) =>
				val configuration = ConfigurationProvider.getConfiguration
				val encodedSecretKey = SecretKeyGenerator.generateSymmetricKey().encode(Encoding.URL_SAFE_BASE_64)
				val asymmetricKeyPair = HybridEncryptionProvider.generateAsymmetricKeyPair
				val encodedPublicKey = asymmetricKeyPair.publicKey.encode(Encoding.URL_SAFE_BASE_64)
				val encodedPrivateKey = asymmetricKeyPair.privateKey.encode(Encoding.URL_SAFE_BASE_64)

				FileSystemUtilities.writeStringToFile(encodedSecretKey, configuration.secretKeyFilePath, backupIfAlreadyExists = true)
				FileSystemUtilities.writeStringToFile(encodedPublicKey, configuration.publicKeyFilePath, backupIfAlreadyExists = true)
				FileSystemUtilities.writeStringToFile(encodedPrivateKey, configuration.privateKeyFilePath, backupIfAlreadyExists = true)
				Environment.invalidateCachedKeys()
			case _ =>
		}

		smartResponse.getData("invalidateCachedKeys") match {
			case Some(true) => Environment.invalidateCachedKeys()
			case _ =>
		}

		smartResponse.getData("invalidateCachedApiClientSystemCredentials") match {
			case Some(true) => Environment.invalidateCachedApiClientSystemCredentials()
			case _ =>
		}
	}

	override def handle(requestId: Long,
						method: HttpMethod,
						request: HttpServletRequest,
						response: HttpServletResponse,
						httpServer: HttpServer): SmartResponse = {
		if (request.getRequestURI == "/api/v1.0/authenticate/client") {
			authenticateApiClient(method, request)
		} else if (method == HttpMethod.POST && request.getRequestURI == "/api/v1.0/message/web-socket") {
			sendMessageToWebSocketClientsBySenderSessionGuid(method, request, httpServer.webSocketClientOrchestrator)
		} else {
			val validationResponse = validateApiClient(request)

			(method, request.getRequestURI) match {
				case (HttpMethod.POST, "/api/v1.0/configuration/reload") => reloadConfiguration(method, request, validationResponse)
				case (HttpMethod.POST, "/api/v1.0/application/stop") => stopApplication(method, request, validationResponse)
				case (HttpMethod.POST, "/api/v1.0/application/keys/generate") => generateKeys(method, request, validationResponse)
				case (HttpMethod.POST, "/api/v1.0/application/keys/cache/invalidate") => invalidateCachedKeys(method, request, validationResponse)
				case (HttpMethod.POST, "/api/v1.0/application/credentials/api-client/system/cache/invalidate") => invalidateCachedApiClientSystemCredentials(method, request, validationResponse)
				case (HttpMethod.POST, "/api/v1.0/authenticate/user") => authenticateUser(method, request, validationResponse)
				case (HttpMethod.POST, "/api/v1.0/message") => sendMessageToWebSocketClients(method, request, httpServer.webSocketClientOrchestrator, validationResponse)
				case (_, "/api/v1.0/test") =>
					/*for (i <- 1 until 2) {

					}*/
					Environment.backgroundTaskExecutor.addTask(new TestTask)
					SmartResponse(HttpStatusCode.ACCEPTED, "Test task is being processed.")
				case _ => null
			}
		}
	}
}
