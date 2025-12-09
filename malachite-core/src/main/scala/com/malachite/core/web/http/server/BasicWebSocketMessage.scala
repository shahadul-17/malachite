package com.malachite.core.web.http.server

import com.malachite.core.text.{JsonSerializable, JsonSerializer}

trait BasicWebSocketMessage extends JsonSerializable {

	/**
	 * Type of the message.
	 */
	val messageType: WebSocketMessageType

	/**
	 * Session GUIDs of the receivers.
	 */
	val receiverSessionGuids: Set[String]

	/**
	 * Unique identifiers of the receivers.
	 */
	val uniqueIds: Set[String]

	/**
	 * Message shall be sent to all the members of the contexts.
	 */
	val contexts: Set[String]

	/**
	 * Message shall be sent to all the members who have subscribed to the topics.
	 * @note Topics is a map containing a set of topics (as value) for each context (as a key).
	 */
	val topics: Map[String, Set[String]]

	/**
	 * This flag indicates whether the message shall be sent back to the sender.
	 */
	val sendToSelf: Boolean

	/**
	 * Payload of the message.
	 */
	val payload: String

	/**
	 * Payload of the message as an object that can be
	 * serialized/deserialized as JSON, XML, etc.
	 */
	val payloadAsObject: Map[String, Any]
}

object BasicWebSocketMessage {

	def apply(messageType: WebSocketMessageType,
			  receiverSessionGuids: Set[String],
			  uniqueIds: Set[String],
			  contexts: Set[String],
			  topics: Map[String, Set[String]],
			  sendToSelf: Boolean,
			  payload: String,
			  payloadAsObject: Map[String, Any],
			 ): BasicWebSocketMessage = BasicWebSocketMessageImpl(
		messageType = messageType,
		receiverSessionGuids = receiverSessionGuids,
		uniqueIds = uniqueIds,
		contexts = contexts,
		topics = topics,
		sendToSelf = sendToSelf,
		payload = payload,
		payloadAsObject = payloadAsObject,
	)

	def apply(json: String): BasicWebSocketMessage
		= JsonSerializer.deserialize(json, classOf[BasicWebSocketMessageImpl])
}

private final case class BasicWebSocketMessageImpl(override val messageType: WebSocketMessageType,
												   override val receiverSessionGuids: Set[String],
												   override val uniqueIds: Set[String],
												   override val contexts: Set[String],
												   override val topics: Map[String, Set[String]],
												   override val sendToSelf: Boolean,
												   override val payload: String,
												   override val payloadAsObject: Map[String, Any],
												  ) extends BasicWebSocketMessage
