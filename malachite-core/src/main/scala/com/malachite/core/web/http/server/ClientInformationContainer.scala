package com.malachite.core.web.http.server

case class ClientInformationContainer(clientSetMapByUniqueId: java.util.Map[String, java.util.Set[WebSocketClient]],
									  clientSetMapByContext: java.util.Map[String, java.util.Set[WebSocketClient]],
									  clientSetMapByTopic: java.util.Map[String, java.util.Set[WebSocketClient]],
									 )

object ClientInformationContainer {

	private val CLIENT_MAP_BY_UNIQUE_ID_INITIAL_CAPACITY = 8192
	private val CLIENT_MAP_BY_CONTEXT_INITIAL_CAPACITY = 8192
	private val CLIENT_MAP_BY_TOPIC_INITIAL_CAPACITY = 8192

	def apply(): ClientInformationContainer = ClientInformationContainer(
		clientSetMapByUniqueId = new java.util.concurrent.ConcurrentHashMap(CLIENT_MAP_BY_UNIQUE_ID_INITIAL_CAPACITY),
		clientSetMapByContext = new java.util.concurrent.ConcurrentHashMap(CLIENT_MAP_BY_CONTEXT_INITIAL_CAPACITY),
		clientSetMapByTopic = new java.util.concurrent.ConcurrentHashMap(CLIENT_MAP_BY_TOPIC_INITIAL_CAPACITY),
	)
}
