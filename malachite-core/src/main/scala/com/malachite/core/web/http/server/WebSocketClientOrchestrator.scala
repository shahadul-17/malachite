package com.malachite.core.web.http.server

import com.malachite.core.security.authentication.user.{UserAuthenticationService, UserValidationResponse}

trait WebSocketClientOrchestrator extends WebSocketMessageSender with WebSocketMessageHandler {

	def clientCount: Int

	def uniqueClientCount(owner: String): Int

	def sessionIds: Set[Long]

	def sessionIds(sessionGuids: Set[String]): Set[Long]

	def owners(sessionIds: Set[Long]): Set[String]

	def clients: Set[WebSocketClient]

	def addEntryToSessionIdMapBySessionGuid(client: WebSocketClient): Unit

	def removeEntryFromSessionIdMapBySessionGuid(client: WebSocketClient): Option[Long]

	def getTemporaryClientBySessionId(sessionId: Long): Option[WebSocketClient]

	def addTemporaryClientToMap(client: WebSocketClient): Unit

	def removeTemporaryClientFromMap(sessionId: Long): Option[WebSocketClient]

	def getClientBySessionId(sessionId: Long): Option[WebSocketClient]

	def getClientsBySessionIds(sessionIds: Set[Long]): Set[WebSocketClient]
		= sessionIds.flatMap(getClientBySessionId)

	def getClientsByUniqueId(uniqueId: String, owner: String): Set[WebSocketClient]

	def getClientsByUniqueIds(uniqueIds: Set[String], owner: String): Set[WebSocketClient]
		= uniqueIds.flatMap(getClientsByUniqueId(_, owner))

	def getClientsByTopics(topics: Map[String, Set[String]], owner: String): Set[WebSocketClient]

	def getClientsByContext(context: String, owner: String): Set[WebSocketClient]
		= getClientsByContexts(Set(context), owner)

	def getClientsByContexts(contexts: Set[String], owner: String): Set[WebSocketClient]

	def addClientToTopicMap(client: WebSocketClient): Unit

	def removeClientFromTopicMap(client: WebSocketClient,
								 topicsToRemove: Map[String, Set[String]] = Map.empty): Unit

	def addClientToMap(client: WebSocketClient): Unit

	def removeClientFromMap(sessionId: Long): Option[WebSocketClient]

	/**
	 * Finds a WebSocket client associated with the given session ID.
	 * The method first looks for the client in the temporary client map.
	 * If no match is found, it checks the main client map.
	 *
	 * @param sessionId Unique identifier of the client session.
	 * @return An Option containing the WebSocket client if found,
	 *         or None if no client is associated with the given session ID.
	 */
	def findClientBySessionId(sessionId: Long): Option[WebSocketClient]
		= getTemporaryClientBySessionId(sessionId).orElse(getClientBySessionId(sessionId))

	/**
	 * Removes a client associated with the given session ID. The method removes
	 * the client from the temporary client map as well as the main client map.
	 * If a client is successfully removed from either map, it is returned.
	 * Otherwise, returns None.
	 *
	 * @param sessionId Unique identifier of the client session to be removed.
	 * @return An Option containing the removed client if found in either the temporary or main map,
	 *         or None if no client was associated with the given session ID.
	 */
	def removeClientBySessionId(sessionId: Long): Option[WebSocketClient] = {
		val temporaryClientOpt = removeTemporaryClientFromMap(sessionId)
		val clientOpt = removeClientFromMap(sessionId)
		val webSocketClientOpt = if (temporaryClientOpt.isDefined) {
			temporaryClientOpt
		} else if (clientOpt.isDefined) {
			clientOpt
		} else {
			None
		}

		webSocketClientOpt.foreach(removeEntryFromSessionIdMapBySessionGuid)

		webSocketClientOpt
	}

	def closeSession(sessionId: Long): Option[WebSocketClient]
		= removeClientBySessionId(sessionId).flatMap(closeSession)

	def closeSession(client: WebSocketClient): Option[WebSocketClient]

	def validateClient(requestAsJson: String, client: WebSocketClient): UserValidationResponse
}

object WebSocketClientOrchestrator {

	def apply(options: HttpServerOptions,
			  authenticationService: UserAuthenticationService,
			  webSocketRequestHandlerOpt: Option[WebSocketRequestHandler],
			 ): WebSocketClientOrchestrator
		= new WebSocketClientOrchestratorImpl(options, authenticationService, webSocketRequestHandlerOpt)
}
