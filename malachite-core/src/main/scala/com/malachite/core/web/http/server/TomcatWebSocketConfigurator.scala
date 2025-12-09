package com.malachite.core.web.http.server

import jakarta.websocket.server.ServerEndpointConfig

class TomcatWebSocketConfigurator(val clientOrchestrator: WebSocketClientOrchestrator) extends ServerEndpointConfig.Configurator {

	override def getEndpointInstance[T](endpointClass: Class[T]): T = {
		val requestHandler = new TomcatWebSocketRequestHandler(clientOrchestrator)

		endpointClass.cast(requestHandler)
	}
}

object TomcatWebSocketConfigurator {

	def apply(clientOrchestrator: WebSocketClientOrchestrator): TomcatWebSocketConfigurator
		= new TomcatWebSocketConfigurator(clientOrchestrator)
}
