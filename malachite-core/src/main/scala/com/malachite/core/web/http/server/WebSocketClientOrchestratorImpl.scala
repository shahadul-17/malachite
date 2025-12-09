package com.malachite.core.web.http.server

import com.malachite.core.{Guid, SmartException, SmartResponse}
import com.malachite.core.configurations.{Configuration, ConfigurationProvider}
import com.malachite.core.security.authentication.user.{Permission, UserAuthenticationService, UserValidationRequest, UserValidationResponse}
import com.malachite.core.text.{Encoding, JsonSerializer}
import com.malachite.core.utilities.CloseableUtilities
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.threading.{AsyncTask, LimitedAsyncTaskExecutor}
import com.malachite.core.web.http.HttpStatusCode
import org.apache.logging.log4j.{Level, LogManager}

import scala.jdk.CollectionConverters.{CollectionHasAsScala, SetHasAsScala}

class WebSocketClientOrchestratorImpl(private val options: HttpServerOptions,
									  private val authenticationService: UserAuthenticationService,
									  private val webSocketRequestHandlerOpt: Option[WebSocketRequestHandler],
									 ) extends WebSocketClientOrchestrator {

	private val logger = LogManager.getLogger(getClass)

	private val sessionIdMapBySessionGuid: java.util.Map[String, Long]
		= new java.util.concurrent.ConcurrentHashMap(WebSocketClientOrchestratorImpl.SESSION_ID_MAP_BY_SESSION_GUID_INITIAL_CAPACITY)
	private val temporaryClientMapBySessionId: java.util.Map[Long, WebSocketClient]
		= new java.util.concurrent.ConcurrentHashMap(WebSocketClientOrchestratorImpl.TEMPORARY_CLIENT_MAP_BY_SESSION_ID_INITIAL_CAPACITY)
	private val clientMapBySessionId: java.util.Map[Long, WebSocketClient]
		= new java.util.concurrent.ConcurrentHashMap(WebSocketClientOrchestratorImpl.CLIENT_MAP_BY_SESSION_ID_INITIAL_CAPACITY)
	private val clientInformationContainerMapByOwner: java.util.Map[String, ClientInformationContainer]
		= new java.util.concurrent.ConcurrentHashMap(WebSocketClientOrchestratorImpl.CLIENT_INFORMATION_CONTAINER_MAP_BY_OWNER_INITIAL_CAPACITY)
	private val limitedAsyncTaskExecutor = LimitedAsyncTaskExecutor(options.webSocketMaximumConcurrentMessageSends)

	private def sanitizeOwner(owner: String): Option[String] = sanitizeOwner(Option(owner))

	private def sanitizeOwner(ownerOpt: Option[String]): Option[String]
		= ownerOpt.map(_.trim).filterNot(_.isBlank)

	private def convertMessageToBytes(message: String,
									  encoding: Encoding,
									 ): Array[Byte]
		= message.decode(encoding)

	private def sendMessageToClients(message: String,
									 sessionIds: Set[Long],
									 senderSessionIdToBeRemovedOpt: Option[Long],
									): Map[Long, Boolean] = {
		// decoding the message to an array of bytes...
		val messageAsBytes = convertMessageToBytes(message, Encoding.UTF_8)

		sendMessageToClients(messageAsBytes, sessionIds, senderSessionIdToBeRemovedOpt)
	}

	private def sendMessageToClients(messageAsBytes: Array[Byte],
									 sessionIds: Set[Long],
									 senderSessionIdToBeRemovedOpt: Option[Long],
									): Map[Long, Boolean] = {
		// if sender session ID to be removed is provided, we shall remove
		// that session ID from the list of session IDs...
		val filteredSessionIds = sessionIds -- senderSessionIdToBeRemovedOpt

		AsyncTask.awaitAsyncTasks(sessionIds.map { sessionId =>
			// NOTE: sendMessage() METHOD WILL NEVER THROW EXCEPTION...!!!
			limitedAsyncTaskExecutor.run(() => sessionId -> sendMessage(sessionId, messageAsBytes))
		}).toMap
	}

	protected def configuration: Configuration = ConfigurationProvider.getConfiguration

	protected def shallIncludeStackTrace: Boolean = configuration.includeStackTrace

	private def clientInformationContainer(owner: String, createIfAbsent: Boolean = false): Option[ClientInformationContainer] = {
		sanitizeOwner(owner).flatMap { sanitizedOwner =>
			if (createIfAbsent) {
				val clientInformationContainer = clientInformationContainerMapByOwner
					.computeIfAbsent(sanitizedOwner, key => ClientInformationContainer())

				Some(clientInformationContainer)
			} else {
				Option(clientInformationContainerMapByOwner.get(sanitizedOwner))
			}
		}
	}

	private def clientSetMapByUniqueId(owner: String, createIfAbsent: Boolean = false): java.util.Map[String, java.util.Set[WebSocketClient]]
		= clientInformationContainer(owner, createIfAbsent).map(_.clientSetMapByUniqueId).getOrElse(java.util.Collections.emptyMap)

	private def clientSetMapByContext(owner: String, createIfAbsent: Boolean = false): java.util.Map[String, java.util.Set[WebSocketClient]]
		= clientInformationContainer(owner, createIfAbsent).map(_.clientSetMapByContext).getOrElse(java.util.Collections.emptyMap)

	private def clientSetMapByTopic(owner: String, createIfAbsent: Boolean = false): java.util.Map[String, java.util.Set[WebSocketClient]]
		= clientInformationContainer(owner, createIfAbsent).map(_.clientSetMapByTopic).getOrElse(java.util.Collections.emptyMap)

	override def clientCount: Int = clientMapBySessionId.size

	override def uniqueClientCount(owner: String): Int = clientSetMapByUniqueId(owner).size

	override def sessionIds: Set[Long] = clientMapBySessionId.keySet.asScala.toSet

	def sessionIds(sessionGuids: Set[String]): Set[Long]
		= sessionGuids.flatMap { sessionGuid => Option(sessionIdMapBySessionGuid.get(sessionGuid)) }

	override def owners(sessionIds: Set[Long]): Set[String] = getClientsBySessionIds(sessionIds).map(_.owner)

	override def clients: Set[WebSocketClient] = clientMapBySessionId.values.asScala.toSet

	private def prepareSessionIdMapBySessionGuidKey(client: WebSocketClient): String
		= s"${client.sessionGuid}:${client.owner}@${client.nodeId}"

	override def addEntryToSessionIdMapBySessionGuid(client: WebSocketClient): Unit = {
		// placing the session GUID - session ID mapping...
		sessionIdMapBySessionGuid.put(client.sessionGuid, client.sessionId)
	}

	override def removeEntryFromSessionIdMapBySessionGuid(client: WebSocketClient): Option[Long] = {
		// removing the session GUID - session ID mapping...
		Option(sessionIdMapBySessionGuid.remove(client.sessionGuid))
	}

	override def getTemporaryClientBySessionId(sessionId: Long): Option[WebSocketClient]
		= Option(temporaryClientMapBySessionId.get(sessionId))

	override def addTemporaryClientToMap(client: WebSocketClient): Unit = {
		// placing the temporary client in the map by session ID...
		temporaryClientMapBySessionId.put(client.sessionId, client)
	}

	override def removeTemporaryClientFromMap(sessionId: Long): Option[WebSocketClient] = {
		// removing the temporary client from the map by session ID...
		Option(temporaryClientMapBySessionId.remove(sessionId))
	}

	override def getClientBySessionId(sessionId: Long): Option[WebSocketClient]
		= Option(clientMapBySessionId.get(sessionId))

	override def getClientsByUniqueId(uniqueId: String, owner: String): Set[WebSocketClient] = {
		val clientSetMapByUniqueId = this.clientSetMapByUniqueId(owner)

		Option(clientSetMapByUniqueId.get(uniqueId))
			.map(_.asScala.toSet)
			.getOrElse(Set.empty)
	}

	override def getClientsByTopics(topics: Map[String, Set[String]], owner: String): Set[WebSocketClient] = {
		val clientSetMapByTopic = this.clientSetMapByTopic(owner)

		// flatten all clients from all requested topics and contexts...
		WebSocketClientOrchestratorImpl
			.populateKeyWithTopicsAndContexts(topics)
			.flatMap(key => Option(clientSetMapByTopic.get(key)))
			.flatMap(_.asScala)
	}

	override def getClientsByContexts(contexts: Set[String], owner: String): Set[WebSocketClient] = {
		val clientSetMapByContext = this.clientSetMapByContext(owner)

		// flatten all clients from all requested contexts...
		contexts
			.flatMap(context => Option(clientSetMapByContext.get(context)))
			.flatMap(_.asScala)
	}

	private def addClientToTopicMap(client: WebSocketClient,
									clientSetMapByTopic: java.util.Map[String, java.util.Set[WebSocketClient]]): Unit = {
		// placing the client in the map by the topics corresponding to their contexts...
		WebSocketClientOrchestratorImpl.populateKeyWithTopicsAndContexts(client.topics).foreach { key =>
			val clientSet = clientSetMapByTopic.computeIfAbsent(key,
				key => new java.util.concurrent.CopyOnWriteArraySet[WebSocketClient]())

			clientSet.add(client)
		}
	}

	override def addClientToTopicMap(client: WebSocketClient): Unit = {
		val clientSetMapByTopic = this.clientSetMapByTopic(client.owner, createIfAbsent = true)

		addClientToTopicMap(client, clientSetMapByTopic)
	}

	private def removeClientFromTopicMap(client: WebSocketClient,
										 clientSetMapByTopic: java.util.Map[String, java.util.Set[WebSocketClient]],
										 topicsToRemove: Map[String, Set[String]],
										): Unit = {
		val _topicsToRemove = if (topicsToRemove.isEmpty) { client.topics } else { topicsToRemove }

		WebSocketClientOrchestratorImpl.populateKeyWithTopicsAndContexts(_topicsToRemove).foreach { key =>
			Option(clientSetMapByTopic.get(key))
				.map { clientSet => clientSet.remove(client) }
		}
	}

	override def removeClientFromTopicMap(client: WebSocketClient,
										  topicsToRemove: Map[String, Set[String]] = Map.empty,
										 ): Unit = {
		val clientSetMapByTopic = this.clientSetMapByTopic(client.owner)

		removeClientFromTopicMap(client, clientSetMapByTopic, topicsToRemove)
	}

	override def addClientToMap(client: WebSocketClient): Unit = {
		val clientInformationContainer = this.clientInformationContainer(client.owner, createIfAbsent = true)
			.getOrElse(throw new IllegalStateException("Client information container not found."))

		// placing the client in the map by session ID...
		clientMapBySessionId.put(client.sessionId, client)

		// placing the client in the map by unique ID...
		val clientSet = clientInformationContainer.clientSetMapByUniqueId.computeIfAbsent(client.uniqueId,
			key => new java.util.concurrent.CopyOnWriteArraySet[WebSocketClient]())

		clientSet.add(client)

		// placing the client in the map by context...
		client.contexts.foreach { context =>
			val clientSet = clientInformationContainer.clientSetMapByContext.computeIfAbsent(context,
				key => new java.util.concurrent.CopyOnWriteArraySet[WebSocketClient]())

			clientSet.add(client)
		}

		addClientToTopicMap(client, clientInformationContainer.clientSetMapByTopic)
	}

	override def removeClientFromMap(sessionId: Long): Option[WebSocketClient] = {
		Option(clientMapBySessionId.remove(sessionId)).flatMap { client =>
			clientInformationContainer(client.owner).map { clientInformationContainer =>
				Option(clientInformationContainer.clientSetMapByUniqueId.get(client.uniqueId))
					.map { clientSet => clientSet.remove(client) }

				client.contexts.foreach { context =>
					Option(clientInformationContainer.clientSetMapByContext.get(context))
						.map { clientSet => clientSet.remove(client) }
				}

				removeClientFromTopicMap(client,
					clientInformationContainer.clientSetMapByTopic, Map.empty)

				client
			}
		}
	}

	override def closeSession(client: WebSocketClient): Option[WebSocketClient] = client match {
		case tomcatWebSocketClient: TomcatWebSocketClient =>
			CloseableUtilities.tryClose(tomcatWebSocketClient.session)
			Some(client)
		case _ => None
	}

	override def validateClient(requestAsJson: String,
								client: WebSocketClient,
							   ): UserValidationResponse = {
		// NOTE: ALREADY VALIDATED CLIENTS SHALL NOT ENTER THIS METHOD...!!!
		logger.log(Level.DEBUG, "Performing WebSocket client validation for session ID '{}'.", client.sessionId)

		// deserializing JSON into UserValidationRequest object...
		val request = JsonSerializer.deserialize(requestAsJson, classOf[UserValidationRequest])
		// checking if the client has a valid token...
		val validationResponse = authenticationService.validate(request)

		/*val permissions = clientValidationResponse.tokenContent.permissions
			.filterNot(_ == Permission.NONE)

		if (permissions.isEmpty) {
			throw SmartException(HttpStatusCode.UNAUTHORIZED, "You do not have any permission.")
		}*/

		client match {
			case tomcatWebSocketClient: TomcatWebSocketClient =>
				// setting the validation response of the client...
				tomcatWebSocketClient.validationResponse = validationResponse

				// preparing session GUID...
				val guid = Guid.newGuid().toString
				val owner = tomcatWebSocketClient.owner
				val nodeId = tomcatWebSocketClient.nodeId
				val sessionGuid = s"$guid:$owner@$nodeId"

				// adding session GUID...
				tomcatWebSocketClient.sessionGuid = sessionGuid
			case _ =>
				// NOTE: THIS IS HIGHLY UNLIKELY...!!!
				throw new IllegalStateException("WebSocket client is not of type TomcatWebSocketClient.")
		}

		// adding session GUID v/s session ID mapping...
		addEntryToSessionIdMapBySessionGuid(client)
		// if the token is valid, we shall remove the temporary client from the map...
		removeTemporaryClientFromMap(client.sessionId)
		// and add it to the map of clients...
		addClientToMap(client)

		logger.log(Level.DEBUG, "WebSocket client validation successful for session ID '{}'.", client.sessionId)

		validationResponse
	}

	override def sendMessage(sessionId: Long, bytes: Array[Byte]): Boolean
		= findClientBySessionId(sessionId).exists(_.sendMessage(bytes))

	override def sendMessage(sessionId: Long,
							 message: String,
							 encoding: Encoding = Encoding.UTF_8,
							): Boolean
		= findClientBySessionId(sessionId).exists(_.sendMessage(message, encoding))

	override def sendMessageToClientsBySessionIds(message: String,
												  sessionIds: Set[Long],
												  senderSessionIdOpt: Option[Long] = None,
												 ): Map[Long, Boolean]
		= sendMessageToClients(message, sessionIds, senderSessionIdOpt)

	override def sendMessageToClientsByUniqueIds(message: String,
												 uniqueIds: Set[String],
												 owner: String,
												 senderSessionIdOpt: Option[Long],
												 sendToSelf: Boolean = false,
												): Map[Long, Boolean] = {
		// WE MUST ADD THE SENDER SESSION ID TO THE LIST OF SESSION IDS AS IT MIGHT BE MISSING...!!!
		val sessionIds = getClientsByUniqueIds(uniqueIds, owner).map(_.sessionId) ++ senderSessionIdOpt

		// if the 'sendToSelf' flag is true, we shall not pass the sender session ID...!!!
		sendMessageToClients(message, sessionIds, if (sendToSelf) { None } else { senderSessionIdOpt })
	}

	override def sendMessageToClientsByTopics(message: String,
											  topics: Map[String, Set[String]],
											  owner: String,
											  senderSessionIdOpt: Option[Long] = None,
											 ): Map[Long, Boolean] = {
		val sessionIds = getClientsByTopics(topics, owner).map(_.sessionId)

		sendMessageToClients(message, sessionIds, senderSessionIdOpt)
	}

	override def sendMessageToClientsByContexts(message: String,
												contexts: Set[String],
												owner: String,
												senderSessionIdOpt: Option[Long] = None,
											   ): Map[Long, Boolean] = {
		val sessionIds = getClientsByContexts(contexts, owner).map(_.sessionId)

		sendMessageToClients(message, sessionIds, senderSessionIdOpt)
	}

	override def sendMessageToAllClients(message: String,
										 senderSessionIdOpt: Option[Long] = None,
										): Map[Long, Boolean]
		= sendMessageToClients(message, sessionIds, senderSessionIdOpt)

	private def sendSmartResponse(message: WebSocketMessage,
								  response: SmartResponse,
								  sender: WebSocketClient, // <-- NOTE: CLIENT THAT HAS SENT THE MESSAGE...!!!
								 ): Unit = {
		// if this ID is None, we shall send the message to the sender as well...
		val senderSessionIdOpt = if (message.sendToSelf) { None } else { Some(sender.sessionId) }
		val jsonResponse = response.toJson()

		message.messageType match {
			// if the message shall be sent only to the sender...
			case WebSocketMessageType.SEND_TO_SELF_ONLY =>
				// the response shall be sent back to the sender...
				sender.sendMessage(jsonResponse)
			// if the message shall be sent to the specified clients...
			case WebSocketMessageType.SEND_TO_CLIENTS =>
				// retrieving receiver session IDs from receiver session GUIDs...
				val receiverSessionIds = sessionIds(message.receiverSessionGuids)
				// we shall send the message to the client/session IDs...
				val result = sendMessageToClientsBySessionIds(jsonResponse, receiverSessionIds, senderSessionIdOpt)
				val successCount = result.count(_._2)
				val failureCount = result.size - successCount

				logger.log(Level.DEBUG, s"Successfully sent the message to {} out of {} (failed {}) clients according to the specified receiver session GUIDs.",
					successCount, result.size, failureCount)
			// if the message shall be sent to the clients that match the provided unique IDs...
			case WebSocketMessageType.SEND_TO_UNIQUE_IDS =>
				// NOTE: WE SHALL PASS 'sessionIdOpt' INSTEAD OF 'senderSessionIdOpt'...!!!
				val result = sendMessageToClientsByUniqueIds(jsonResponse, message.uniqueIds,
					message.owner, Some(sender.sessionId), message.sendToSelf)
				val successCount = result.count(_._2)
				val failureCount = result.size - successCount

				logger.log(Level.DEBUG, s"Successfully sent the message to {} out of {} (failed {}) clients according to the specified unique IDs.",
					successCount, result.size, failureCount)
			// if topics are provided...
			case WebSocketMessageType.SEND_TO_TOPICS =>
				// we shall send the message to the clients that match the provided topics...
				val result = sendMessageToClientsByTopics(jsonResponse, message.topics, message.owner, senderSessionIdOpt)
				val successCount = result.count(_._2)
				val failureCount = result.size - successCount

				logger.log(Level.DEBUG, s"Successfully sent the message to {} out of {} (failed {}) clients according to the specified topics.",
					successCount, result.size, failureCount)
			// if contexts are provided...
			case WebSocketMessageType.SEND_TO_CONTEXTS =>
				// we shall send the message to all clients that match the provided contexts
				// and optionally the provided topics...
				val result = sendMessageToClientsByContexts(jsonResponse, message.contexts, message.owner, senderSessionIdOpt)
				val successCount = result.count(_._2)
				val failureCount = result.size - successCount

				logger.log(Level.DEBUG, s"Successfully sent the message to {} out of {} (failed {}) clients according to the specified contexts.",
					successCount, result.size, failureCount)
			// if the message shall be broadcasted to all the connected clients...
			case WebSocketMessageType.BROADCAST =>
				// we shall send the message to all clients...
				val result = sendMessageToAllClients(jsonResponse, senderSessionIdOpt)
				val successCount = result.count(_._2)
				val failureCount = result.size - successCount

				logger.log(Level.DEBUG, s"Successfully broadcasted the message to {} out of {} (failed {}) clients.",
					successCount, result.size, failureCount)
			case _ =>
				logger.log(Level.WARN, "Not sure what to do with the response: {}", jsonResponse)

				throw SmartException(HttpStatusCode.BAD_REQUEST, "Unknown/unsupported WebSocket message type.")
		}
	}

	private def validatePermissions(webSocketMessage: WebSocketMessage, sender: WebSocketClient): Unit = {
		val permissions = sender.permissions

		// if the client has 'WILDCARD' permissions, we shall not proceed any further...
		if (permissions.contains(Permission.WILDCARD)) { return }
		// if the message type is 'ECHO' and the client has the 'ECHO' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == WebSocketMessageType.ECHO && permissions.contains(Permission.ECHO)) { return }
		// if the message type is 'PING' and the client has the 'PING' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == WebSocketMessageType.PING && permissions.contains(Permission.PING)) { return }
		// if the 'sendToSelf' or 'sendToSelfOnly' flag is true, but the client does not have the 'SELF' permission...
		if ((webSocketMessage.messageType == WebSocketMessageType.SEND_TO_SELF_ONLY || webSocketMessage.sendToSelf) && !permissions.contains(Permission.SELF)) {
			// we shall throw an exception...
			throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to send this message.")
		}
		// if the message shall be sent to the specified clients and the client has the 'CLIENT' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == WebSocketMessageType.SEND_TO_CLIENTS && permissions.contains(Permission.CLIENT) && owners(sessionIds(webSocketMessage.receiverSessionGuids)).forall(_ == sender.owner)) { return }
		// if the message shall be sent to the clients having the specified unique IDs and the client has the 'UNIQUE_ID' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == WebSocketMessageType.SEND_TO_UNIQUE_IDS && permissions.contains(Permission.UNIQUE_ID)) { return }
		// if the message type is 'SUBSCRIBE_TO_TOPICS' or 'UNSUBSCRIBE_FROM_TOPICS'...
		if (webSocketMessage.messageType == WebSocketMessageType.SUBSCRIBE_TO_TOPICS
			|| webSocketMessage.messageType == WebSocketMessageType.UNSUBSCRIBE_FROM_TOPICS) {
			// if the contexts belong to the client's allowed contexts, wes shall not proceed any further...
			if (webSocketMessage.topics.keySet.subsetOf(sender.contexts)) { return }
			// otherwise, we shall throw an exception...
			else {
				throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to subscribe/unsubscribe to the topic(s) of the requested context(s).")
			}
		}
		// if the message shall be sent to the peers that match the provided topics, and the client has the 'TOPIC' permission...
		else if (webSocketMessage.messageType == WebSocketMessageType.SEND_TO_TOPICS && permissions.contains(Permission.TOPIC)) {
			val allowedTopics = sender.topics

			webSocketMessage.topics.foreach { (context, topicSet) =>
				val allowedTopicSet = allowedTopics.getOrElse(context, Set.empty)

				if (!topicSet.subsetOf(allowedTopicSet)) {
					throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to send this message.")
				}
			}

			return
		}
		// if the message shall be sent to all clients that match the provided contexts,
		// and the client has the 'CONTEXT' permission and the provided contexts are
		// a subset of the client's contexts, we shall not proceed any further...
		if (webSocketMessage.messageType == WebSocketMessageType.SEND_TO_CONTEXTS && permissions.contains(Permission.CONTEXT) && webSocketMessage.contexts.subsetOf(sender.contexts)) { return }
		// if the message shall be broadcasted/sent to all clients
		// and the client has the 'BROADCAST' permission, we shall not proceed any further...
		if (webSocketMessage.messageType == WebSocketMessageType.BROADCAST && permissions.contains(Permission.BROADCAST)) { return }

		// if none of the above conditions are met, we shall throw an exception...
		throw SmartException(HttpStatusCode.FORBIDDEN, "You do not have sufficient permission(s) to send this message.")
	}

	private def prepareDefaultResponse(message: WebSocketMessage): SmartResponse
		= SmartResponse(HttpStatusCode.OK, "A new message has been received.")
			.addData("createdAt", message.createdAt)
			.addData("messageType", message.messageType)
			.addData("messageId", message.messageId)
			.addData("messageGuid", message.messageGuid)
			.addData("nodeId", message.nodeId)
			.addData("owner", message.owner)
			.addData("senderSessionGuid", message.senderSessionGuid)
			.addData("receiverSessionGuids", message.receiverSessionGuids)
			.addData("uniqueIds", message.uniqueIds)
			.addData("contexts", message.contexts)
			.addData("topics", message.topics)
			.addData("sendToSelf", message.sendToSelf)
			.addData("payload", message.payload)

	override def handleWebSocketMessage(message: String, senderSessionGuid: String): SmartResponse = {
		val senderSessionId = sessionIds(Set(senderSessionGuid))
			.headOption.getOrElse(throw SmartException(HttpStatusCode.NOT_FOUND, "No web socket session associated with the provided GUID."))
		val client = getClientBySessionId(senderSessionId)
			.getOrElse(throw SmartException(HttpStatusCode.NOT_FOUND, "No web socket session associated with the provided GUID."))

		handleWebSocketMessage(message, client)
	}

	override def handleWebSocketMessage(message: String, sender: WebSocketClient): SmartResponse = {
		logger.log(Level.DEBUG, "Received message (from WebSocket): {}", message)

		try {
			// sanitizing the received message...
			val sanitizedMessage = message.trim
			// deserializing the sanitized message into a basic WebSocket message object...
			val basicWebSocketMessage = BasicWebSocketMessage(sanitizedMessage)
			// preparing the WebSocket message object from the basic WebSocket message...
			val webSocketMessage = WebSocketMessage(
				nodeId = sender.nodeId,
				owner = sender.owner,		// <-- NOTE: IF THE CLIENT HAS NOT BEEN VALIDATED, IT SHALL BE AN EMPTY STRING...!!!
				senderSessionGuid = sender.sessionGuid,
				basicWebSocketMessage = basicWebSocketMessage,
			)

			logger.log(Level.DEBUG, "Extracted WebSocket message payload: {}", webSocketMessage.payload)

			// if the message type is 'CLIENT_VALIDATION' and the client is not yet validated...
			if (webSocketMessage.messageType == WebSocketMessageType.CLIENT_VALIDATION && !sender.validated) {
				val validationResponse = validateClient(webSocketMessage.payload, sender)
				val response = SmartResponse(HttpStatusCode.OK, "WebSocket client validation successful.")
					.addData("sessionGuid", sender.sessionGuid)
					.addData("nodeId", sender.nodeId)
					.addData("uniqueId", sender.uniqueId)
					.addData("owner", sender.owner)
					.addData("contexts", sender.contexts)
					.addData("permissions", sender.permissions)
					.addData("tokenVersion", validationResponse.tokenContent.version)
					.addData("tokenIssuedAt", validationResponse.tokenContent.issuedAt)
					.addData("tokenExpirationInMilliseconds", validationResponse.tokenContent.expirationInMilliseconds)
					.addData("additionalData", validationResponse.tokenContent.additionalData)

				sender.sendMessage(response.toJson())

				logger.log(Level.DEBUG, "New client connected with session ID {}.", sender.sessionId)

				response
			} else {
				// if the client has not been validated but tries to send a message...
				if (!sender.validated) {
					// we'll throw an exception...
					throw SmartException(HttpStatusCode.UNAUTHORIZED, "You are not authorized to access this resource.")
				}

				// after that, we shall validate the permissions of the client...
				validatePermissions(webSocketMessage, sender)

				webSocketMessage.messageType match {
					// if the message type is 'ECHO'...
					case WebSocketMessageType.ECHO =>
						// the message shall be echoed back to the sender...
						sender.sendMessage(webSocketMessage.payload)

						// we shall prepare a response...
						SmartResponse(HttpStatusCode.OK, "Echo request processed successfully.")
							.addData("payload", webSocketMessage.payload)
					// if the message type is 'PING'...
					case WebSocketMessageType.PING =>
						// we shall prepare a response...
						val response = SmartResponse(HttpStatusCode.OK, "Ping request processed successfully.")

						// and send the response...
						sender.sendMessage(response.toJson())

						response
					case WebSocketMessageType.SUBSCRIBE_TO_TOPICS =>
						webSocketMessage.topics.foreach { (context, topics) =>
							sender.addTopics(context, topics)
						}

						// this method shall synchronize the topics with the topic map of the parent...
						addClientToTopicMap(sender)

						// we shall prepare a response...
						val response = SmartResponse(HttpStatusCode.OK, "Successfully subscribed to the requested topics.")
							.addData("topicsAdded", webSocketMessage.topics)
							.addData("topics", sender.topics)

						// and send the response...
						sender.sendMessage(response.toJson())

						response
					case WebSocketMessageType.UNSUBSCRIBE_FROM_TOPICS =>
						webSocketMessage.topics.foreach { (context, topics) =>
							sender.removeTopics(context, topics)
						}

						// this method shall synchronize the topics with the topic map of the parent...
						// NOTE: THIS METHOD SHALL ONLY REMOVE THE CLIENT FROM THE SPECIFIED TOPICS...!!!
						removeClientFromTopicMap(sender, webSocketMessage.topics)

						// we shall prepare a response...
						val response = SmartResponse(HttpStatusCode.OK, "Successfully unsubscribed from the requested topics.")
							.addData("topicsRemoved", webSocketMessage.topics)
							.addData("topics", sender.topics)

						// and send the response...
						sender.sendMessage(response.toJson())

						response
					case _ =>
						val response = webSocketRequestHandlerOpt.flatMap { webSocketRequestHandler =>
							Option(webSocketRequestHandler.handle(TomcatWebSocketRequest(
								webSocketMessage = webSocketMessage,
								clientOrchestrator = this,
								client = sender,
							)))
						}.getOrElse(prepareDefaultResponse(webSocketMessage))

						sendSmartResponse(webSocketMessage, response, sender)

						response
				}
			}
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred by client with session ID {} while processing the received message '{}'.", sender.sessionId, message, exception)

				// preparing a smart response from the exception...
				val response = SmartResponse.from(exception, HttpStatusCode.INTERNAL_SERVER_ERROR.value, shallIncludeStackTrace)
				// sending the smart response...
				sender.sendMessage(response.toJson())

				// if the status code is 'UNAUTHORIZED' or the 'closeSession' flag is true...
				if (response.statusCode == HttpStatusCode.UNAUTHORIZED.value
					|| response.getData("closeSession").contains(true)) {
					// we shall close the session...
					closeSession(sender)
				}

				response
		}
	}
}

private object WebSocketClientOrchestratorImpl {

	private val SESSION_ID_MAP_BY_SESSION_GUID_INITIAL_CAPACITY = 8192
	private val TEMPORARY_CLIENT_MAP_BY_SESSION_ID_INITIAL_CAPACITY = 8192
	private val CLIENT_MAP_BY_SESSION_ID_INITIAL_CAPACITY = 8192
	private val CLIENT_INFORMATION_CONTAINER_MAP_BY_OWNER_INITIAL_CAPACITY = 8192

	private def populateKeyWithTopicAndContext(topic: String, context: String): String = s"$topic@$context"

	private def populateKeyWithTopicsAndContexts(topics: Map[String, Set[String]]): Set[String] = topics
		.flatMap((context, topicSet) => topicSet.map(populateKeyWithTopicAndContext(_, context)))
		.toSet
}
