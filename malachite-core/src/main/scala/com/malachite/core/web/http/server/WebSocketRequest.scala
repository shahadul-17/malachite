package com.malachite.core.web.http.server

import com.malachite.core.security.authentication.user.Permission

trait WebSocketRequest {
	val webSocketMessage: WebSocketMessage
	val clientOrchestrator: WebSocketClientOrchestrator
	val client: WebSocketClient
}
