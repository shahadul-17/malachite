package com.malachite.core.web.http.server

import com.malachite.core.security.authentication.user.Permission

class OwnerWebSocketClient(override val owner: String,
						   override val nodeId: String,
						  ) extends WebSocketClient {

	override val sessionId: Long = -1L

	override def validated: Boolean = true

	override def uniqueId: String = ""

	override def sessionGuid: String = ""

	override def sessionGuid_=(sessionGuid: String): Unit = { }

	override def contexts: Set[String] = Set.empty

	override def permissions: Set[Permission] = Set(Permission.WILDCARD)

	override def topics: Map[String, Set[String]] = Map.empty

	override def addTopics(context: String, topics: Set[String]): Boolean = false

	override def removeTopics(context: String, topics: Set[String]): Boolean = false

	override def sendMessage(bytes: Array[Byte]): Boolean = false
}

object OwnerWebSocketClient {
	def apply(owner: String, nodeId: String): OwnerWebSocketClient
		= new OwnerWebSocketClient(owner, nodeId)
}
