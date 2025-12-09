package com.malachite.core.web.http.server

import com.malachite.core.security.authentication.user.{Permission, UserValidationResponse}
import com.malachite.core.utilities.CloseableUtilities
import jakarta.websocket.{EndpointConfig, MessageHandler, Session}
import org.apache.logging.log4j.{Level, LogManager}

import scala.jdk.CollectionConverters.{MapHasAsScala, SetHasAsJava, SetHasAsScala}

private[server] class TomcatWebSocketClient(override val sessionId: Long,
											override val nodeId: String,
											val session: Session,
											val endpointConfiguration: EndpointConfig,
											val clientOrchestrator: WebSocketClientOrchestrator,
										   ) extends WebSocketClient with MessageHandler.Whole[String] {

	private val logger = LogManager.getLogger(getClass)
	private val _sessionGuid = java.util.concurrent.atomic.AtomicReference[String]
	private val _validationResponse = java.util.concurrent.atomic.AtomicReference[UserValidationResponse]
	private val allowedTopicSetMapByContext: java.util.Map[String, java.util.Set[String]]
		= new java.util.concurrent.ConcurrentHashMap[String, java.util.Set[String]]()

	def validationResponse: Option[UserValidationResponse] = Option(_validationResponse.get)

	def validationResponse_=(validationResponse: UserValidationResponse): Unit
		= _validationResponse.set(validationResponse)

	override def validated: Boolean = validationResponse.isDefined

	override def sessionGuid: String = Option(_sessionGuid.get).getOrElse("")

	override def sessionGuid_=(sessionGuid: String): Unit = Option(sessionGuid).foreach(_sessionGuid.set)

	override def uniqueId: String = validationResponse.map(_.tokenContent.uniqueId).getOrElse("")

	override def owner: String = validationResponse.map(_.tokenContent.issuedBy).getOrElse("")

	override def contexts: Set[String] = validationResponse.map(_.tokenContent.contexts.toSet).getOrElse(Set.empty)

	override def permissions: Set[Permission] = validationResponse.map(_.tokenContent.permissions.toSet).getOrElse(Set.empty)

	override def topics: Map[String, Set[String]] = allowedTopicSetMapByContext.asScala
		.map { (context, topicSet) => (context, topicSet.asScala.toSet) }
		.toMap

	override def addTopics(context: String, topics: Set[String]): Boolean = {
		// find the appropriate set of topics for the context...
		val allowedTopicSet = allowedTopicSetMapByContext.computeIfAbsent(context,
			key => new java.util.concurrent.CopyOnWriteArraySet[String]())

		// adding the topics to the set of topics...
		allowedTopicSet.addAll(topics.asJava)
	}

	override def removeTopics(context: String, topics: Set[String]): Boolean = {
		// find the appropriate set of topics for the context
		// and removing the topics...
		Option(allowedTopicSetMapByContext.get(context))
			.exists(_.removeAll(topics.asJava))
	}

	/*private def sendSmartResponse(webSocketMessage: TomcatWebSocketMessage,
								  response: SmartResponse
								 ): Unit = {
		val responseOpt = Option(response)
		// if this ID is None, we shall send the message to the sender as well...
		val senderSessionIdOpt = if (webSocketMessage.sendToSelf) { None } else { Some(sessionId) }

		if (responseOpt.isDefined) {
			responseOpt.foreach { response =>
				val jsonResponse = response.toJson()

				// if the message shall be sent only to the sender...
				if (webSocketMessage.sendToSelfOnly && webSocketMessage.sendToSelf) {
					// the response shall be sent back to the sender...
					clientOrchestrator.sendMessage(sessionId, jsonResponse)
				}
				// if peer session IDs are provided...
				else if (webSocketMessage.peerSessionIds.nonEmpty) {
					// we shall send the message to the peers...
					clientOrchestrator.sendMessageToClientsBySessionIds(jsonResponse, webSocketMessage.peerSessionIds, senderSessionIdOpt)
				}
				// if topics are provided...
				else if (webSocketMessage.topics.nonEmpty) {
					// we shall send the message to the peers that match the provided topics...
					clientOrchestrator.sendMessageToClientsByTopics(jsonResponse, webSocketMessage.topics, senderSessionIdOpt)
				}
				// if no context is provided...
				else if (webSocketMessage.contexts.isEmpty) {
					// we shall send the message to all clients...
					clientOrchestrator.sendMessageToAllClients(jsonResponse, senderSessionIdOpt)
				}
				// if at least one context is provided...
				else if (webSocketMessage.contexts.nonEmpty) {
					// we shall send the message to all clients that match the provided contexts
					// and optionally the provided topics...
					clientOrchestrator.sendMessageToClientsByContexts(jsonResponse, webSocketMessage.contexts, senderSessionIdOpt)
				}
				// invalid case...
				else {
					logger.log(Level.WARN, "Not sure what to do with the response, '{}'.", jsonResponse)
				}
			}
		} else {
			logger.log(Level.WARN, "WebSocket request handler returned null response.")
		}
	}*/

	/*private def validateWebSocketClient(payload: String): SmartResponse = {
		logger.log(Level.DEBUG, "Performing WebSocket client validation.")

		// checking if the client has a valid token...
		val clientValidationResponse = clientOrchestrator.validateClient(payload)
		/*val permissions = clientValidationResponse.tokenContent.permissions
			.filterNot(_ == Permission.NONE)

		if (permissions.isEmpty) {
			throw SmartException(HttpStatusCode.UNAUTHORIZED, "You do not have any permission.")
		}*/

		// if the client has already been validated...
		if (this._validationResponse != null) {
			logger.log(Level.DEBUG, "Removing existing client validation for session ID '{}' and unique ID '{}'.", sessionId, uniqueId)

			// we shall first remove the client from the map of clients...
			clientOrchestrator.removeClientFromMap(sessionId)
			// then add the client to the temporary client map...
			clientOrchestrator.addTemporaryClientToMap(this)

			logger.log(Level.DEBUG, "Successfully removed existing client validation for session ID '{}' and unique ID '{}'.", sessionId, uniqueId)
		}

		this._validationResponse = clientValidationResponse

		// if the token is valid, we shall remove the temporary client from the map...
		clientOrchestrator.removeTemporaryClientFromMap(sessionId)
		// and add it to the map of clients...
		clientOrchestrator.addClientToMap(this)

		logger.log(Level.DEBUG, "WebSocket client validation successful.")

		SmartResponse(HttpStatusCode.OK, "WebSocket client validation successful.")
	}*/

	/*private def validatePermissions(webSocketMessage: TomcatWebSocketMessage): Unit = {
		val permissions = this.permissions

		// if the client has 'WILDCARD' permissions, we shall not proceed any further...
		if (permissions.contains(Permission.WILDCARD)) { return }
		// if the message type is 'ECHO' and the client has the 'ECHO' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == TomcatWebSocketMessageType.ECHO && permissions.contains(Permission.ECHO)) { return }
		// if the message type is 'PING' and the client has the 'PING' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == TomcatWebSocketMessageType.PING && permissions.contains(Permission.PING)) { return }
		// if the 'sendToSelf' or 'sendToSelfOnly' flag is true, but the client does not have the 'SELF' permission...
		if ((webSocketMessage.sendToSelf || webSocketMessage.sendToSelfOnly) && !permissions.contains(Permission.SELF)) {
			// we shall throw an exception...
			throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to send this message.")
		}
		// if the message shall be sent to the peers and the client has the 'PEER' permission, we shall not proceed any further...
		if (webSocketMessage.peerSessionIds.nonEmpty && permissions.contains(Permission.PEER)) { return }
		// if the message type is 'SUBSCRIBE_TO_TOPICS' or 'UNSUBSCRIBE_FROM_TOPICS'...
		if (webSocketMessage.messageType == TomcatWebSocketMessageType.SUBSCRIBE_TO_TOPICS
			|| webSocketMessage.messageType == TomcatWebSocketMessageType.UNSUBSCRIBE_FROM_TOPICS) {
			// if the contexts belong to the client's allowed contexts, wes shall not proceed any further...
			if (webSocketMessage.topics.keySet.subsetOf(contexts)) { return }
			// otherwise, we shall throw an exception...
			else {
				throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to subscribe/unsubscribe to the topic(s) of the requested context(s).")
			}
		}
		// if the message shall be sent to the peers that match the provided topics, and the client has the 'TOPIC' permission...
		else if (webSocketMessage.topics.nonEmpty && permissions.contains(Permission.TOPIC)) {
			webSocketMessage.topics.foreach { (context, topicSet) =>
				val allowedTopicSet = Option(allowedTopicSetMapByContext.get(context))
					.map(_.asScala.toSet)
					.getOrElse(Set.empty[String])

				if (!topicSet.subsetOf(allowedTopicSet)) {
					throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to send this message.")
				}
			}

			return
		}
		// if the message shall be sent to all clients and the client has the 'ALL' permission, we shall not proceed any further...
		if (webSocketMessage.contexts.isEmpty && permissions.contains(Permission.ALL)) { return }
		// if the message shall be sent to all clients that match the provided contexts,
		// and the client has the 'CONTEXT' permission and the provided contexts are
		// a subset of the client's contexts, we shall not proceed any further...
		if (webSocketMessage.contexts.nonEmpty && permissions.contains(Permission.CONTEXT) && webSocketMessage.contexts.subsetOf(contexts)) { return }

		// if none of the above conditions are met, we shall throw an exception...
		throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to send this message.")
	}*/

	/*private def handleMessage(message: String): Unit = {
		val webSocketMessage = JsonSerializer.deserialize[TomcatWebSocketMessage](message, classOf[TomcatWebSocketMessage])
		val payload = Option(webSocketMessage.payload).filterNot(_.isEmpty) match {
			case Some(payload) => payload
			case _ =>
				Option(webSocketMessage.payloadAsObject)
					.filterNot(_.isEmpty)
					.map(JsonSerializer.serialize(_)) match {
					case Some(payload) => payload
					case _ => ""
				}
		}

		if (webSocketMessage.messageType == TomcatWebSocketMessageType.CLIENT_VALIDATION) {
			val response = validateWebSocketClient(payload)

			clientOrchestrator.sendMessage(sessionId, response.toJson())

			logger.log(Level.DEBUG, "New client connected with session ID {}.", session.getId)

			return
		}

		logger.log(Level.DEBUG, "Received a message on WebSocket: {}", payload)

		// if the client has not been validated but tries to send a message...
		if (_validationResponse == null) {
			// we'll throw an exception...
			throw SmartException(HttpStatusCode.UNAUTHORIZED, "You are not authorized to access this resource.")
		}

		// after that, we shall validate the permissions of the client...
		validatePermissions(webSocketMessage)

		webSocketMessage.messageType match {
			// if the message type is 'ECHO'...
			case TomcatWebSocketMessageType.ECHO =>
				// the message shall be echoed back to the sender...
				clientOrchestrator.sendMessage(sessionId, payload)
			// if the message type is 'PING'...
			case TomcatWebSocketMessageType.PING =>
				// we shall prepare a response...
				val response = SmartResponse(HttpStatusCode.OK, "Ping request processed successfully.")

				// and send the response...
				clientOrchestrator.sendMessage(sessionId, response.toJson())
			case TomcatWebSocketMessageType.SUBSCRIBE_TO_TOPICS =>
				webSocketMessage.topics.foreach { (context, topicSet) =>
					// find the appropriate set of topics for the context...
					val allowedTopicSet = allowedTopicSetMapByContext.computeIfAbsent(context,
						key => new java.util.concurrent.CopyOnWriteArraySet[String]())

					// adding the topics to the set of topics...
					allowedTopicSet.addAll(topicSet.asJava)
				}

				// this method shall synchronize the topics with the topic map of the parent...
				clientOrchestrator.addClientToTopicMap(this)

				// we shall prepare a response...
				val response = SmartResponse(HttpStatusCode.OK, "Successfully subscribed to the requested topics.")
					.addData("topicsAdded", webSocketMessage.topics)
					.addData("topics", topics)

				// and send the response...
				clientOrchestrator.sendMessage(sessionId, response.toJson())
			case TomcatWebSocketMessageType.UNSUBSCRIBE_FROM_TOPICS =>
				webSocketMessage.topics.foreach { (context, topicSet) =>
					// find the appropriate set of topics for the context
					// and removing the topics...
					Option(allowedTopicSetMapByContext.get(context))
						.map(_.removeAll(topicSet.asJava))
				}

				// this method shall synchronize the topics with the topic map of the parent...
				// NOTE: THIS METHOD SHALL ONLY REMOVE THE CLIENT FROM THE SPECIFIED TOPICS...!!!
				clientOrchestrator.removeClientFromTopicMap(this, webSocketMessage.topics)

				// we shall prepare a response...
				val response = SmartResponse(HttpStatusCode.OK, "Successfully unsubscribed from the requested topics.")
					.addData("topicsRemoved", webSocketMessage.topics)
					.addData("topics", topics)

				// and send the response...
				clientOrchestrator.sendMessage(sessionId, response.toJson())
			case TomcatWebSocketMessageType.GENERIC =>
				val response = webSocketRequestHandler.handle(TomcatWebSocketRequest(
					sessionId = sessionId,
					message = payload,
					messageSender = clientOrchestrator,
				))

				sendSmartResponse(webSocketMessage, response)
			case _ =>
				logger.log(Level.WARN, "Unknown WebSocket message type.")
		}
	}*/

	override def sendMessage(bytes: Array[Byte]): Boolean = {
		if (session.isOpen) {
			logger.log(Level.DEBUG, "Connection is open to send message to client with session ID {}.", sessionId)

			Option(session.getBasicRemote) match {
				case Some(remoteEndpoint) =>
					logger.log(Level.DEBUG, "Remote endpoint available to send message to client with session ID {}.", sessionId)

					var outputStream: java.io.OutputStream = null

					try {
						outputStream = remoteEndpoint.getSendStream
						outputStream.write(bytes, 0, bytes.length)
						outputStream.flush()

						logger.log(Level.DEBUG, "Successfully sent {} bytes to client with session ID {}.", bytes.length, sessionId)
						// logger.log(Level.DEBUG, "Successfully sent the message '{}' to client with session ID {}.", message, sessionId)

						true
					} catch {
						case exception: Exception =>
							logger.log(Level.ERROR, "An exception occurred while sending {} bytes to the client with session ID {}.",
								bytes.length, sessionId, exception)
							/*logger.log(Level.ERROR, "An exception occurred while sending the message '{}' to the client with session ID {}.",
								message, sessionId, exception)*/

							false
					} finally {
						CloseableUtilities.tryClose(outputStream)
					}
				case _ =>
					logger.log(Level.WARN, "Remote endpoint not available to send {} bytes to client with session ID {}.", bytes.length, sessionId)
					// logger.log(Level.WARN, "Remote endpoint not available to send message '{}' to client with session ID {}.", message, sessionId)

					false
			}
		} else {
			logger.log(Level.WARN, "Failed to send {} bytes to client with session ID {} because connection is not open.", bytes.length, sessionId)
			// logger.log(Level.WARN, "Failed to send message '{}' to client with session ID {}.", message, sessionId)

			false
		}
	}

	// = clientOrchestrator.handleWebSocketMessage2XYZ(message, sessionId)
	override def onMessage(message: String): Unit = clientOrchestrator.handleWebSocketMessage(
		sender = this,
		message = message,
	)
}
