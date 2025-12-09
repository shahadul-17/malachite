package com.malachite.core.web.http.server

import com.malachite.core.SmartResponse

trait WebSocketRequestHandler {
	def handle(request: WebSocketRequest): SmartResponse
}
