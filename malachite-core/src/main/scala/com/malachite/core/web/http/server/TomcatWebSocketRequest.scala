package com.malachite.core.web.http.server

import com.malachite.core.security.authentication.user.Permission

private[server] case class TomcatWebSocketRequest(webSocketMessage: WebSocketMessage,
												  clientOrchestrator: WebSocketClientOrchestrator,
												  client: WebSocketClient,
												 ) extends WebSocketRequest
