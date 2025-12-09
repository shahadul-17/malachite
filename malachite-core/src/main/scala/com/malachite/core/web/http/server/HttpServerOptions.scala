package com.malachite.core.web.http.server

case class HttpServerOptions(host: String,
							 port: Int,
							 contextPath: String,
							 documentBaseDirectoryPath: String,
							 pingPath: String,
							 requestHandlerBasePath: String,
							 webSocketRequestHandlerBasePath: String,
							 webSocketMaximumConcurrentMessageSends: Int,
							)

object HttpServerOptions {
	val DEFAULT: HttpServerOptions = HttpServerOptions(
		host = "127.0.0.1",
		port = 8080,
		contextPath = "",
		documentBaseDirectoryPath = "",
		pingPath = "/ping",
		requestHandlerBasePath = "/",
		webSocketRequestHandlerBasePath = "/websocket",
		webSocketMaximumConcurrentMessageSends = 0,
	)
}
