package com.malachite.core.web.http.server

import com.malachite.core.SmartResponse
import com.malachite.core.web.http.HttpMethod
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

trait HttpRequestHandler {

	def customizeResponse(smartResponse: SmartResponse, httpServer: HttpServer): Unit = { }

	def onRequestHandled(smartResponse: SmartResponse, httpServer: HttpServer): Unit = { }

	def handle(requestId: Long,
			   method: HttpMethod,
			   request: HttpServletRequest,
			   response: HttpServletResponse,
			   httpServer: HttpServer,
			  ): SmartResponse
}
