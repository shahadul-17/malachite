package com.malachite.core.web.http.server

import com.malachite.core.security.authentication.user.UserAuthenticationService
import org.apache.catalina.startup.Tomcat
import org.apache.tomcat.websocket.server.WsSci
import jakarta.websocket.server.{ServerContainer, ServerEndpointConfig}
import org.apache.catalina.Lifecycle
import org.apache.logging.log4j.{Level, LogManager}

class TomcatHttpServer private (val options: HttpServerOptions,
								val executorService: java.util.concurrent.ExecutorService,
								val authenticationService: UserAuthenticationService,
								val httpRequestHandler: HttpRequestHandler,
								val webSocketRequestHandlerOpt: Option[WebSocketRequestHandler],
							   ) extends HttpServer {
	private val tomcat = new Tomcat
	private val clientOrchestrator = WebSocketClientOrchestrator(
		options,
		authenticationService,
		webSocketRequestHandlerOpt,
	)

	override def onStart(): Unit = tomcat.start()

	override def onStop(): Unit = tomcat.stop()

	override def onAwait(): Unit = {
		if (state.running) {
			tomcat.getServer.await()
		}
	}

	override def onDispose(): Unit = {
		stop()
		tomcat.destroy()
	}

	override def webSocketClientOrchestrator: WebSocketClientOrchestrator = clientOrchestrator
}

object TomcatHttpServer {

	private val logger = LogManager.getLogger(getClass)

	def apply(options: HttpServerOptions,
			  executorService: java.util.concurrent.ExecutorService,
			  userAuthenticationService: UserAuthenticationService,
			  httpRequestHandler: HttpRequestHandler,
			  webSocketRequestHandlerOpt: Option[WebSocketRequestHandler] = None,
			 ): TomcatHttpServer = {
		val documentBaseDirectoryAbsolutePath = new java.io.File(options.documentBaseDirectoryPath)
			.getAbsolutePath
		val tomcatHttpServer = new TomcatHttpServer(
			options, executorService, userAuthenticationService,
			httpRequestHandler, webSocketRequestHandlerOpt)
		val tomcat = tomcatHttpServer.tomcat
		// setting host name from the options...
		tomcat.setHostname(options.host)
		// setting port from the options on which the tomcat server shall run...
		tomcat.setPort(options.port)
		// setting tomcat base directory...
		tomcat.setBaseDir(documentBaseDirectoryAbsolutePath)
		// getting the tomcat connector...
		val connector = tomcat.getConnector
		// getting the tomcat connector protocol handler...
		val protocolHandler = connector.getProtocolHandler
		// setting the executor...
		protocolHandler.setExecutor(tomcatHttpServer.executorService)
		// adding context to tomcat...
		val context = tomcat.addContext(options.contextPath, documentBaseDirectoryAbsolutePath)
		// instantiating ping servlet...
		val pingServlet: jakarta.servlet.Servlet = new PingServlet
		// ping servlet must have a name to be uniquely identified...
		val pingServletClassName = pingServlet.getClass.getSimpleName
		// adding ping servlet to the Tomcat server...
		tomcat.addServlet(options.contextPath, pingServletClassName, pingServlet)
		// mapping the servlet...
		context.addServletMappingDecoded(options.pingPath, pingServletClassName)
		// instantiating tomcat internal request handler...
		val tomcatHttpRequestHandler: jakarta.servlet.Servlet = new TomcatHttpRequestHandler(
			options, tomcatHttpServer.httpRequestHandler, tomcatHttpServer)
		// request handler shall have a name to be uniquely identified...
		val requestHandlerClassName = tomcatHttpRequestHandler.getClass.getSimpleName
		// adding the request handler to the Tomcat server...
		tomcat.addServlet(options.contextPath, requestHandlerClassName, tomcatHttpRequestHandler)
		// mapping the servlet...
		context.addServletMappingDecoded(options.requestHandlerBasePath, requestHandlerClassName)
		// adding the websocket support to the tomcat server...
		context.addServletContainerInitializer(new WsSci(), null)
		// adding a lifecycle listener to the tomcat server...
		context.addLifecycleListener(event => {
			if (event.getType == Lifecycle.AFTER_START_EVENT) {
				logger.log(Level.INFO, "Executing tomcat server lifecycle event '{}'.", event.getType)

				// registering the web socket endpoint programmatically...
				val serverEndpointConfiguration = ServerEndpointConfig.Builder
					.create(classOf[TomcatWebSocketRequestHandler], options.webSocketRequestHandlerBasePath)
					.configurator(TomcatWebSocketConfigurator(tomcatHttpServer.clientOrchestrator))
					.build()
				// adding the websocket endpoint to the server container...
				context.getServletContext.getAttribute(classOf[ServerContainer].getName) match {
					case serverContainer: ServerContainer =>
						serverContainer.addEndpoint(serverEndpointConfiguration)

						logger.log(Level.INFO, "Successfully registered the web socket endpoint '{}'.", options.webSocketRequestHandlerBasePath)
					case _ =>
						logger.log(Level.WARN, "Failed to register the web socket endpoint '{}'.", options.webSocketRequestHandlerBasePath)
				}
			}
		})
		// returning the tomcat HTTP server...
		tomcatHttpServer
	}
}
