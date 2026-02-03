package com.malachite.core.web.http.server.collection

import com.malachite.core.utilities.CollectionUtilities

private[collection] case class SessionInformationContainer(
	sessionIdSetMapByUserId: java.util.Map[String, java.util.Set[Long]],
	sessionIdSetMapByContext: java.util.Map[String, java.util.Set[Long]],
	sessionIdSetMapByTopic: java.util.Map[String, java.util.Set[Long]],
)

private[collection] object SessionInformationContainer {
	def apply(initialCapacity: Int): SessionInformationContainer = SessionInformationContainer(
		sessionIdSetMapByUserId = CollectionUtilities.createConcurrentHashMap(initialCapacity),
		sessionIdSetMapByContext = CollectionUtilities.createConcurrentHashMap(initialCapacity),
		sessionIdSetMapByTopic = CollectionUtilities.createConcurrentHashMap(initialCapacity),
	)
}
