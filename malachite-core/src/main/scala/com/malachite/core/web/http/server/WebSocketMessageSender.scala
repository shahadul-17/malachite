package com.malachite.core.web.http.server

import com.malachite.core.text.Encoding

trait WebSocketMessageSender {

	def sendMessage(sessionId: Long, bytes: Array[Byte]): Boolean

	def sendMessage(sessionId: Long, message: String, encoding: Encoding = Encoding.UTF_8): Boolean

	def sendMessageToClientsBySessionIds(message: String,
										 sessionIds: Set[Long],
										 senderSessionIdOpt: Option[Long] = None,
										): Map[Long, Boolean]

	def sendMessageToClientsByUniqueId(message: String,
									   uniqueId: String,
									   owner: String,
									   senderSessionIdOpt: Option[Long],
									   sendToSelf: Boolean = false,
									  ): Map[Long, Boolean]
		= sendMessageToClientsByUniqueIds(message, Set(uniqueId), owner, senderSessionIdOpt, sendToSelf)

	def sendMessageToClientsByUniqueIds(message: String,
										uniqueIds: Set[String],
										owner: String,
										senderSessionIdOpt: Option[Long],
										sendToSelf: Boolean = false,
									   ): Map[Long, Boolean]

	def sendMessageToClientsByTopics(message: String,
									 topics: Map[String, Set[String]],
									 owner: String,
									 senderSessionIdOpt: Option[Long] = None,
									): Map[Long, Boolean]

	def sendMessageToClientsByContext(message: String,
									  context: String,
									  owner: String,
									  senderSessionIdOpt: Option[Long] = None,
									 ): Map[Long, Boolean]
		= sendMessageToClientsByContexts(message, Set(context), owner, senderSessionIdOpt)

	def sendMessageToClientsByContexts(message: String,
									   contexts: Set[String],
									   owner: String,
									   senderSessionIdOpt: Option[Long] = None,
									  ): Map[Long, Boolean]

	def sendMessageToAllClients(message: String,
								senderSessionIdOpt: Option[Long] = None,
							   ): Map[Long, Boolean]
}
