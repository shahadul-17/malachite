package com.malachite.server

import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.security.authentication.apiclient.{ApiClientAuthenticationService, ApiClientAuthenticationServiceImpl}
import com.malachite.core.security.authentication.user.{UserAuthenticationService, UserAuthenticationServiceImpl}
import com.malachite.core.{Application, Environment}
import com.malachite.core.web.http.server.{HttpRequestHandler, HttpServer, TomcatHttpServer}
import org.apache.logging.log4j.{Level, LogManager}

import scala.compiletime.uninitialized

class ServerApplication private extends Application {

    private val logger = LogManager.getLogger(getClass)
    private var httpServer: HttpServer = uninitialized
    private var apiClientAuthenticationService: ApiClientAuthenticationService = uninitialized
    private var userAuthenticationService: UserAuthenticationService = uninitialized

    override def initialize(): Unit = {
        logger.log(Level.INFO, "Initializing Server application.")

        val configuration = ConfigurationProvider.getConfiguration
        val executorService = Environment.executorService

        apiClientAuthenticationService = new ApiClientAuthenticationServiceImpl
        userAuthenticationService = new UserAuthenticationServiceImpl

        val httpRequestHandler: HttpRequestHandler = new HttpRequestHandlerImpl(
            apiClientAuthenticationService,
            userAuthenticationService,
        )

        httpServer = TomcatHttpServer(
            options = configuration.httpServer,
            executorService = executorService,
            userAuthenticationService = userAuthenticationService,
            httpRequestHandler = httpRequestHandler,
        )

        logger.log(Level.INFO, "Server application initialization successful.")
    }

    override def execute(): Unit = {
        httpServer
            .start()
            .await()

        // val p = HashProvider.computeHash("visena".decode(Encoding.UTF_8), Environment.getSecretKey).encode(URL_SAFE_BASE_64)
        /*val apiClientAuthenticationService: ApiClientAuthenticationService = new ApiClientAuthenticationServiceImpl
        val userAuthenticationService: UserAuthenticationService = new UserAuthenticationServiceImpl(apiClientAuthenticationService)
        val apiClientAuthenticationResponse = apiClientAuthenticationService.authenticate(ApiClientAuthenticationRequest(
            username = "visena",
            password = "myvisena@2025",
        ))
        val userAuthenticationResponse = userAuthenticationService.authenticate(UserAuthenticationRequest(
            apiKey = apiClientAuthenticationResponse.apiKey,
            token = apiClientAuthenticationResponse.token,
            uniqueId = 1L,
            contexts = Array("HELLO", "WORLD"),
            permissions = Array(TOPIC, CONTEXT),
            additionalData = Some(Map(
                "name" -> "visena",
                "email" -> "visena@gmail.com",
            )),
        ))
        val validationResult = userAuthenticationService.validate(UserValidationRequest(userAuthenticationResponse.token))
        // val isValid = x.validateApiKey(apiKey)

        logger.log(Level.INFO, "USER TOKEN: {}", userAuthenticationResponse.token)
        // logger.log(Level.INFO, "API KEY: {}", apiKey)
        // logger.log(Level.INFO, "IS API KEY VALID 2: {}", isValid.isDefined)*/

        logger.log(Level.INFO, "Server application execution completed.")
    }
}
