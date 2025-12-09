package com.malachite.core.web.http.server

import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.security.authentication.user.Permission
import com.malachite.core.text.Encoding
import org.apache.logging.log4j.{Level, LogManager}

trait WebSocketClient extends Comparable[WebSocketClient] {

	private val logger = LogManager.getLogger(getClass)

	val sessionId: Long

	val nodeId: String

	def validated: Boolean

	def uniqueId: String

	def sessionGuid: String

	def sessionGuid_=(sessionGuid: String): Unit

	def owner: String

	def contexts: Set[String]

	def permissions: Set[Permission]

	def topics: Map[String, Set[String]]

	def addTopics(context: String, topics: Set[String]): Boolean

	def removeTopics(context: String, topics: Set[String]): Boolean

	protected def convertMessageToBytes(message: String,
										encoding: Encoding,
									   ): Array[Byte]
		= message.decode(encoding)

	def sendMessage(bytes: Array[Byte]): Boolean

	def sendMessage(message: String, encoding: Encoding = Encoding.UTF_8): Boolean = {
		val messageAsBytes = convertMessageToBytes(message, encoding)
		val messageSent = sendMessage(messageAsBytes)

		if (messageSent) {
			logger.log(Level.DEBUG, "Successfully sent the message '{}' to client with session ID {}.", message, sessionId)
		} else {
			logger.log(Level.DEBUG, "Failed to send the message '{}' to client with session ID {}.", message, sessionId)
		}

		messageSent
	}

	override def hashCode(): Int = sessionId.hashCode

	override def equals(obj: Any): Boolean = obj match {
		case client: WebSocketClient => client.sessionId == sessionId
		case _ => false
	}

	override def compareTo(client: WebSocketClient): Int
		= sessionId.compareTo(client.sessionId)
}
