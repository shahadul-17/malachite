package com.malachite.core.web.http.server

import com.malachite.core.{Environment, Guid}
import jakarta.websocket.{CloseReason, Endpoint, EndpointConfig, Session}
import org.apache.logging.log4j.{Level, LogManager}

private[server] class TomcatWebSocketRequestHandler(val clientOrchestrator: WebSocketClientOrchestrator) extends Endpoint {

	private val logger = LogManager.getLogger(getClass)

	override def onOpen(session: Session, endpointConfiguration: EndpointConfig): Unit = {
		logger.log(Level.INFO, "Received new connection request with session ID {}.", session.getId)

		val guid = Guid.newGuid().toString
		val nodeId = Environment.instanceId
		val client = new TomcatWebSocketClient(
			sessionId = session.getId.toLong,
			nodeId = nodeId,
			session = session,
			endpointConfiguration = endpointConfiguration,
			clientOrchestrator = clientOrchestrator,
		)

		// adding the client to the temporary client map as this client is not yet authenticated...
		clientOrchestrator.addTemporaryClientToMap(client)
		// also adding a message handler to the client...
		session.addMessageHandler(client)
	}

	override def onClose(session: Session, closeReason: CloseReason): Unit = {
		val sessionId = session.getId.toLong

		clientOrchestrator.closeSession(sessionId)

		logger.log(Level.ERROR, "A client with session ID {} has disconnected.", sessionId)
	}

	override def onError(session: Session, throwable: Throwable): Unit = {
		val sessionId = session.getId.toLong

		clientOrchestrator.closeSession(sessionId)

		logger.log(Level.ERROR, "An exception occurred by client with session ID {}.", sessionId, throwable)
	}
}
