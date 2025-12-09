package com.malachite.core.web.http.server

import com.malachite.core.SmartResponse
import com.malachite.core.web.http.HttpMethod
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

private[server] class TomcatHttpRequestHandler(private val options: HttpServerOptions,
											   private val requestHandler: HttpRequestHandler,
											   private val httpServer: HttpServer,
											  ) extends EnhancedHttpServlet {

	private val counter = new java.util.concurrent.atomic.AtomicLong(0L)

	override def customizeResponse(smartResponse: SmartResponse): Unit
		= requestHandler.customizeResponse(smartResponse, httpServer)

	override def onRequestProcessed(smartResponse: SmartResponse): Unit
		= requestHandler.onRequestHandled(smartResponse, httpServer)

	override def processRequest(method: HttpMethod,
								request: HttpServletRequest,
								response: HttpServletResponse
							   ): SmartResponse = {
		val requestId = counter.incrementAndGet()

		requestHandler.handle(requestId, method, request, response, httpServer)
	}
}
