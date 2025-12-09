package com.malachite.core.web.http.server

import com.malachite.core.{SmartException, SmartResponse}
import com.malachite.core.common.Stopwatch
import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.utilities.HttpServletUtilities
import com.malachite.core.web.http.{HttpMethod, HttpStatusCode}
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

abstract class EnhancedHttpServlet extends HttpServlet {

	private val logger = LogManager.getLogger(getClass)

	protected def shallIncludeStackTrace: Boolean
		= ConfigurationProvider.getConfiguration.includeStackTrace

	protected def customizeResponse(smartResponse: SmartResponse): Unit = { }

	protected def onRequestProcessed(smartResponse: SmartResponse): Unit = { }

	protected def processRequest(method: HttpMethod, request: HttpServletRequest, response: HttpServletResponse): SmartResponse

	private def onRequestReceived(method: HttpMethod, request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// starting a new stopwatch...
		val stopwatch = Stopwatch().start
		// holds the client's IP address...
		var remoteIpAddress = ""
		// holds the smart response...
		var smartResponse: SmartResponse = null

		try {
			// retrieving client's IP address...
			remoteIpAddress = HttpServletUtilities.retrieveRemoteIpAddress(request)

			logger.log(Level.INFO, "Received '{}' request for '{}' from {}:{}", method, request.getRequestURI, remoteIpAddress, request.getRemotePort)

			// calling the processRequest() method to process the received request...
			smartResponse = processRequest(method, request, response)

			// if smart response is null...
			if (smartResponse == null) {
				// we shall throw a smart exception...
				throw SmartException(HttpStatusCode.BAD_REQUEST, "Invalid request provided.")
			}
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while processing '{}' request for '{}' from {}:{}",
					method, request.getRequestURI, remoteIpAddress, request.getRemotePort, exception)

				// preparing a smart response from the exception...
				smartResponse = SmartResponse.from(exception, HttpStatusCode.INTERNAL_SERVER_ERROR.value, shallIncludeStackTrace)
		}

		// after we've received the response, we shall call this method
		// to make sure that the response can be customized if needed...
		customizeResponse(smartResponse)

		try {
			// sending the smart response...
			HttpServletUtilities.sendSmartResponse(smartResponse, response)
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while sending response of '{}' request for '{}' from {}:{}",
					method, request.getRequestURI, remoteIpAddress, request.getRemotePort, exception)
		}

		// stopping the stopwatch...
		stopwatch.stop

		logger.log(Level.INFO, "Finished processing '{}' request for '{}' from {}:{} in {}.",
			method, request.getRequestURI, remoteIpAddress, request.getRemotePort, stopwatch.humanReadableElapsedTime)

		// this method shall perform any operation that
		// needs to be done after the response is sent...
		onRequestProcessed(smartResponse)
	}

	override def doHead(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.HEAD, request, response)

	override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.GET, request, response)

	override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.POST, request, response)

	override def doPut(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.PUT, request, response)

	override def doPatch(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.PATCH, request, response)

	override def doDelete(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.DELETE, request, response)

	override def doOptions(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.OPTIONS, request, response)

	override def doTrace(request: HttpServletRequest, response: HttpServletResponse): Unit
		= onRequestReceived(HttpMethod.TRACE, request, response)
}
