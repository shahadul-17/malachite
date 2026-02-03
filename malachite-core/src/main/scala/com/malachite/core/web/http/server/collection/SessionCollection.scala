package com.malachite.core.web.http.server.collection

trait SessionCollection[T <: Session] {

	def get(sessionId: Long, flag: Flag): Option[T]

	def get(sessionIds: Set[Long], flag: Flag): Set[T]

	def add(session: T): Unit

	def remove(sessionId: Long, flag: Flag): Option[T]

	def getSessionIds(flag: Flag): Set[Long]

	def getSessionIdBySessionGuid(sessionGuid: String): Option[Long]
		= getSessionIdsBySessionGuids(Set(sessionGuid)).headOption

	def getSessionIdsBySessionGuids(sessionGuids: Set[String]): Set[Long]

	def getSessionIdByOwner(owner: String): Option[Long]
		= getSessionIdsByOwners(Set(owner)).headOption

	def getSessionIdsByOwners(owners: Set[String]): Set[Long]

	def getSessionIdByUserId(userId: String, owner: String): Option[Long]
		= getSessionIdsByUserIds(Set(userId), owner).headOption

	def getSessionIdsByUserIds(userIds: Set[String], owner: String): Set[Long]

	def getSessionIdByContext(context: String, owner: String): Option[Long]
		= getSessionIdsByContexts(Set(context), owner).headOption

	def getSessionIdsByContexts(contexts: Set[String], owner: String): Set[Long]

	def getSessionIdByTopic(topic: String, context: String, owner: String): Option[Long]
		= getSessionIdsByTopicContextPair(Map(topic -> Set(context)), owner).headOption

	def getSessionIdsByTopicContextPair(topicContextPair: Map[String, Set[String]], owner: String): Set[Long]

	def getOwners(sessionIds: Set[Long], flag: Flag): Set[String]
}
