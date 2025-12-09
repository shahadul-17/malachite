package com.malachite.core.web.http.server

trait HttpServer extends Server {
	def options: HttpServerOptions
	def webSocketClientOrchestrator: WebSocketClientOrchestrator
}
