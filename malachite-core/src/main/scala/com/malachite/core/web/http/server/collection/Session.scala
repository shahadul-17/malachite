package com.malachite.core.web.http.server.collection

trait Session extends Comparable[Session] {

	def authenticated: Boolean
	
	def sessionId: Long

	def sessionGuid: String

	def owner: String

	def userId: String

	def contexts: Set[String]

	def topics: Map[String, Set[String]]

	override def hashCode(): Int = sessionId.hashCode

	override def equals(obj: Any): Boolean = obj match {
		case session: Session => session.sessionId == sessionId
		case _ => false
	}

	override def compareTo(session: Session): Int
		= sessionId.compareTo(session.sessionId)
}
