package com.malachite.core.web.http.server

import com.malachite.core.Guid
import com.malachite.core.text.JsonSerializer

trait WebSocketMessage extends BasicWebSocketMessage {

	/**
	 * Message object creation timestamp.
	 */
	val createdAt: java.util.Date

	/**
	 * Unique (incremental) identifier of the message.
	 */
	val messageId: Long

	/**
	 * Unique identifier of the message.
	 */
	val messageGuid: String

	/**
	 * ID of the application node/instance from which the message was received.
	 */
	val nodeId: String

	/**
	 * Owner (commonly a company. e.g., Visena AS, Cefalo, etc.) of the sender.
	 */
	val owner: String

	/**
	 * Session GUID of the sender.
	 */
	val senderSessionGuid: String
}

object WebSocketMessage {

	private val COUNTER = new java.util.concurrent.atomic.AtomicLong(1L)

	def apply(nodeId: String,
			  owner: String,
			  senderSessionGuid: String,
			  basicWebSocketMessage: BasicWebSocketMessage): WebSocketMessage = WebSocketMessage(
		createdAt = new java.util.Date(),
		messageId = COUNTER.getAndIncrement(),
		messageGuid = Guid.newGuid().toString,
		nodeId = nodeId,
		owner = owner,
		senderSessionGuid = senderSessionGuid,
		basicWebSocketMessage = basicWebSocketMessage,
	)

	def apply(createdAt: java.util.Date,
			  messageId: Long,
			  messageGuid: String,
			  nodeId: String,
			  owner: String,
			  senderSessionGuid: String,
			  basicWebSocketMessage: BasicWebSocketMessage): WebSocketMessage = WebSocketMessage(
		createdAt = createdAt,
		messageType = basicWebSocketMessage.messageType,
		messageId = messageId,
		messageGuid = messageGuid,
		nodeId = nodeId,
		owner = owner,
		senderSessionGuid = senderSessionGuid,
		receiverSessionGuids = basicWebSocketMessage.receiverSessionGuids,
		uniqueIds = basicWebSocketMessage.uniqueIds,
		contexts = basicWebSocketMessage.contexts,
		topics = basicWebSocketMessage.topics,
		sendToSelf = basicWebSocketMessage.sendToSelf,
		// extracting the payload from the basic WebSocket message object...
		payload = Option(basicWebSocketMessage.payload).map(_.trim).filterNot(_.isBlank) match {
			case Some(payload) => payload
			case _ => Option(basicWebSocketMessage.payloadAsObject).filterNot(_.isEmpty).map(JsonSerializer.serialize(_)) match {
				case Some(payload) => payload
				case _ => ""
			}
		},
		payloadAsObject = Map.empty,		// <-- NOTE: IN CASE OF THIS CONSTRUCTOR, THE "payloadAsObject" SHALL ALWAYS BE EMPTY FOR EASE OF USE...!!!
	)

	def apply(createdAt: java.util.Date,
			  messageType: WebSocketMessageType,
			  messageId: Long,
			  messageGuid: String,
			  nodeId: String,
			  owner: String,
			  senderSessionGuid: String,
			  receiverSessionGuids: Set[String],
			  uniqueIds: Set[String],
			  contexts: Set[String],
			  topics: Map[String, Set[String]],
			  sendToSelf: Boolean,
			  payload: String,
			  payloadAsObject: Map[String, Any],
			 ): WebSocketMessage = WebSocketMessageImpl(
		createdAt = createdAt,
		messageType = messageType,
		messageId = messageId,
		messageGuid = messageGuid,
		nodeId = nodeId,
		owner = owner,
		senderSessionGuid = senderSessionGuid,
		receiverSessionGuids = receiverSessionGuids,
		uniqueIds = uniqueIds,
		contexts = contexts,
		topics = topics,
		sendToSelf = sendToSelf,
		payload = payload,
		payloadAsObject = payloadAsObject,
	)

	def apply(json: String): WebSocketMessage
		= JsonSerializer.deserialize(json, classOf[WebSocketMessageImpl])
}

private final case class WebSocketMessageImpl(override val createdAt: java.util.Date,
											  override val messageType: WebSocketMessageType,
											  override val messageId: Long,
											  override val messageGuid: String,
											  override val nodeId: String,
											  override val owner: String,
											  override val senderSessionGuid: String,
											  override val receiverSessionGuids: Set[String],
											  override val uniqueIds: Set[String],
											  override val contexts: Set[String],
											  override val topics: Map[String, Set[String]],
											  override val sendToSelf: Boolean,
											  override val payload: String,
											  override val payloadAsObject: Map[String, Any],
											 ) extends WebSocketMessage
