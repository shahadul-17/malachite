package com.malachite.core.web.http.server.collection

import com.malachite.core.utilities.CollectionUtilities
import org.apache.logging.log4j.{Level, LogManager}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class SessionCollectionImpl[T <: Session](initialCapacity: Int) extends SessionCollection[T] {

	private val _initialCapacity: Int = Option(initialCapacity)
		.filter(_ > 0)
		.getOrElse(SessionCollectionImpl.DEFAULT_INITIAL_CAPACITY)

	private val logger = LogManager.getLogger(getClass)
	private val unauthenticatedSessionMapById = CollectionUtilities.createConcurrentHashMap[Long, T](_initialCapacity)
	private val sessionMapById = CollectionUtilities.createConcurrentHashMap[Long, T](_initialCapacity)
	private val sessionIdMapByGuid = CollectionUtilities.createConcurrentHashMap[String, Long](_initialCapacity)
	private val sessionInformationContainerMapByOwner = CollectionUtilities.createConcurrentHashMap[String, SessionInformationContainer](_initialCapacity)

	override def get(sessionId: Long, flag: Flag): Option[T] = flag match {
		case Flag.AUTHENTICATED => Option(sessionMapById.get(sessionId))
		case Flag.UNAUTHENTICATED => Option(unauthenticatedSessionMapById.get(sessionId))
		case Flag.ANY => Option(sessionMapById.get(sessionId)).orElse(Option(unauthenticatedSessionMapById.get(sessionId)))
		case _ =>
			logger.log(Level.WARN, "Unknown flag '{}'.", flag)

			None
	}

	override def get(sessionIds: Set[Long], flag: Flag): Set[T]
		= sessionIds.flatMap(get(_, flag))

	private def getSessionInformationContainer(owner: String, createIfAbsent: Boolean = false): Option[SessionInformationContainer] = {
		SessionCollectionImpl.sanitizeOwner(owner).flatMap { sanitizedOwner =>
			if (createIfAbsent) {
				val clientInformationContainer = sessionInformationContainerMapByOwner
					.computeIfAbsent(sanitizedOwner, key => SessionInformationContainer(_initialCapacity))

				Some(clientInformationContainer)
			} else {
				Option(sessionInformationContainerMapByOwner.get(sanitizedOwner))
			}
		}
	}

	private def getSessionIdSetMapByUserId(owner: String, createIfAbsent: Boolean = false): java.util.Map[String, java.util.Set[Long]]
		= getSessionInformationContainer(owner, createIfAbsent).map(_.sessionIdSetMapByUserId).getOrElse(java.util.Collections.emptyMap)

	private def getSessionIdSetMapByContext(owner: String, createIfAbsent: Boolean = false): java.util.Map[String, java.util.Set[Long]]
		= getSessionInformationContainer(owner, createIfAbsent).map(_.sessionIdSetMapByContext).getOrElse(java.util.Collections.emptyMap)

	private def getSessionIdSetMapByTopic(owner: String, createIfAbsent: Boolean = false): java.util.Map[String, java.util.Set[Long]]
		= getSessionInformationContainer(owner, createIfAbsent).map(_.sessionIdSetMapByTopic).getOrElse(java.util.Collections.emptyMap)

	private def addSessionToTopicMap(session: Session, sessionIdSetMapByTopic: java.util.Map[String, java.util.Set[Long]]): Unit = {
		// placing the session ID in the map by the topics corresponding to their contexts...
		SessionCollectionImpl.populateKeyWithTopicsAndContexts(session.topics).foreach { key =>
			val sessionIdSet = sessionIdSetMapByTopic.computeIfAbsent(key,
				key => CollectionUtilities.createConcurrentSet)

			sessionIdSet.add(session.sessionId)
		}
	}

	private def addAuthenticated(session: T): Unit = {
		// this must never throw exception unless there is some serious issue...
		val informationContainer = getSessionInformationContainer(session.owner, createIfAbsent = true)
			.getOrElse(throw new IllegalStateException("Session information container not found."))

		// placing the session ID in the map by user ID...
		val sessionIdSet = informationContainer.sessionIdSetMapByUserId.computeIfAbsent(session.userId,
			key => CollectionUtilities.createConcurrentSet[Long])

		sessionIdSet.add(session.sessionId)

		// placing the session ID in the map by context...
		session.contexts.foreach { context =>
			val sessionIdSet = informationContainer.sessionIdSetMapByContext.computeIfAbsent(context,
				key => CollectionUtilities.createConcurrentSet[Long])

			sessionIdSet.add(session.sessionId)
		}

		// placing the session ID in the map by the topics corresponding to their contexts...
		addSessionToTopicMap(session, informationContainer.sessionIdSetMapByTopic)

		// we shall place the session ID in the map by session GUID...
		sessionIdMapByGuid.put(session.sessionGuid, session.sessionId)
		// we shall place the session in the map by session ID...
		sessionMapById.put(session.sessionId, session)
	}

	private def addUnauthenticated(session: T): Unit
		= unauthenticatedSessionMapById.put(session.sessionId, session)

	override def add(session: T): Unit = {
		// if the session is authenticated...
		if (session.authenticated) {
			addAuthenticated(session)
		} else {
			addUnauthenticated(session)
		}
	}

	private def removeSessionFromTopicMap(session: Session, sessionIdSetMapByTopic: java.util.Map[String, java.util.Set[Long]]): Unit = {
		// removing the session ID from the map by the topics corresponding to their contexts...
		SessionCollectionImpl.populateKeyWithTopicsAndContexts(session.topics).foreach { key =>
			CollectionUtilities.removeElementAndCleanupIfEmpty(
				session.sessionId, key, sessionIdSetMapByTopic)
		}
	}

	private def removeAuthenticated(sessionId: Long): Option[T] = {
		// 1. we shall remove the session from the map by session ID...
		Option(sessionMapById.remove(sessionId)) match {
			case Some(session) =>
				getSessionInformationContainer(session.owner, createIfAbsent = false) match {
					case Some(informationContainer) =>
						// 2. we shall remove the session ID from the map by session GUID...
						sessionIdMapByGuid.remove(session.sessionGuid, session.sessionId)

						// 3. removing the session ID from the map by topic...
						removeSessionFromTopicMap(session, informationContainer.sessionIdSetMapByTopic)

						// 4. removing the session ID from the map by context...
						session.contexts.foreach { context =>
							CollectionUtilities.removeElementAndCleanupIfEmpty(
								session.sessionId, context, informationContainer.sessionIdSetMapByContext)
						}

						// 5. removing the session ID from the map by user ID...
						CollectionUtilities.removeElementAndCleanupIfEmpty(
							session.sessionId, session.userId, informationContainer.sessionIdSetMapByUserId)
					case _ =>
						logger.log(Level.DEBUG, "No session information container found for owner '{}'.", session.owner)
				}

				Some(session)
			case _ =>
				logger.log(Level.DEBUG, "No session found for the ID '{}'.", sessionId)

				None
		}
	}

	private def removeUnauthenticated(sessionId: Long): Option[T]
		= Option(unauthenticatedSessionMapById.remove(sessionId))

	override def remove(sessionId: Long, flag: Flag): Option[T] = {
		flag match {
			case Flag.AUTHENTICATED => removeAuthenticated(sessionId)
			case Flag.UNAUTHENTICATED => removeUnauthenticated(sessionId)
			case Flag.ANY => removeAuthenticated(sessionId).orElse(removeUnauthenticated(sessionId))
			case Flag.ALL =>
				val sessionOpt = removeAuthenticated(sessionId)
				val unauthenticatedSessionOpt = removeUnauthenticated(sessionId)

				sessionOpt.orElse(unauthenticatedSessionOpt)
			case _ =>
				logger.log(Level.WARN, "Unknown flag '{}'.", flag)

				None
		}
	}

	override def getSessionIds(flag: Flag): Set[Long] = {
		flag match {
			case Flag.AUTHENTICATED => CollectionUtilities.toScalaSet(sessionMapById.keySet)
			case Flag.UNAUTHENTICATED => CollectionUtilities.toScalaSet(unauthenticatedSessionMapById.keySet)
			case Flag.ALL =>
				val authenticatedSessionIds = sessionMapById.keySet
				val unauthenticatedSessionIds = unauthenticatedSessionMapById.keySet
				val totalCount = authenticatedSessionIds.size + unauthenticatedSessionIds.size
				val sessionIds = CollectionUtilities.createHashSet[Long](totalCount)
				sessionIds.addAll(authenticatedSessionIds)
				sessionIds.addAll(unauthenticatedSessionIds)

				CollectionUtilities.toScalaSet(sessionIds)
			case _ =>
				logger.log(Level.WARN, "Unknown flag '{}'.", flag)

				Set.empty
		}
	}

	override def getSessionIdsBySessionGuids(sessionGuids: Set[String]): Set[Long]
		= sessionGuids.flatMap { sessionGuid => Option(sessionIdMapByGuid.get(sessionGuid)) }

	override def getSessionIdsByOwners(owners: Set[String]): Set[Long] = owners
		.flatMap(getSessionIdSetMapByUserId(_, createIfAbsent = false).values.asScala.flatMap(CollectionUtilities.toScalaSet))

	override def getSessionIdsByUserIds(userIds: Set[String], owner: String): Set[Long] = {
		val sessionIdSetMapByUserId = getSessionIdSetMapByUserId(owner, createIfAbsent = false)

		userIds.flatMap(userId => CollectionUtilities.toScalaSet(sessionIdSetMapByUserId.get(userId)))
	}

	override def getSessionIdsByContexts(contexts: Set[String], owner: String): Set[Long] = {
		val sessionIdSetMapByContext = getSessionIdSetMapByContext(owner, createIfAbsent = false)

		contexts.flatMap(context => CollectionUtilities.toScalaSet(sessionIdSetMapByContext.get(context)))
	}

	override def getSessionIdsByTopicContextPair(topicContextPair: Map[String, Set[String]], owner: String): Set[Long] = {
		val sessionIdSetMapByTopic = getSessionIdSetMapByTopic(owner, createIfAbsent = false)

		SessionCollectionImpl.populateKeyWithTopicsAndContexts(topicContextPair)
			.flatMap(key => CollectionUtilities.toScalaSet(sessionIdSetMapByTopic.get(key)))
	}

	override def getOwners(sessionIds: Set[Long], flag: Flag): Set[String]
		= get(sessionIds, flag).map(_.owner)
}

private object SessionCollectionImpl {

	private val DEFAULT_INITIAL_CAPACITY = 8192

	private def sanitizeOwner(owner: String): Option[String] = sanitizeOwner(Option(owner))

	private def sanitizeOwner(ownerOpt: Option[String]): Option[String]
		= ownerOpt.map(_.trim).filterNot(_.isBlank)

	private def populateKeyWithTopicAndContext(topic: String, context: String): String = s"$topic@$context"

	private def populateKeyWithTopicsAndContexts(topics: Map[String, Set[String]]): Set[String] = topics
		.flatMap((context, topicSet) => topicSet.map(populateKeyWithTopicAndContext(_, context)))
		.toSet
}
