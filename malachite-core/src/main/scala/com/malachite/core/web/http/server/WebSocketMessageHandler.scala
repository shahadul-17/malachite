package com.malachite.core.web.http.server

import com.malachite.core.SmartResponse

trait WebSocketMessageHandler {

	def handleWebSocketMessage(message: String, senderSessionGuid: String): SmartResponse

	def handleWebSocketMessage(message: String, sender: WebSocketClient): SmartResponse
}
