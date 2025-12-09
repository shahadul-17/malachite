package com.malachite.core.utilities

import com.malachite.core.SmartResponse
import com.malachite.core.web.http.HttpHeader
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}

import java.nio.charset.StandardCharsets

object HttpServletUtilities {

	/**
	 * Retrieves the IP address of the client.
	 *
	 * @param request HTTP servlet request object.
	 * @return The IP address of the client.
	 */
	def retrieveRemoteIpAddress(request: HttpServletRequest): String = {
		// getting the forwarded remote IP address...
		val forwardedRemoteIpAddress = Option(request.getHeader(HttpHeader.FORWARDED_FOR.value)).map(_.trim).filterNot(_.isBlank).getOrElse("")
		// getting the remote IP address from the request...
		var remoteIpAddress = request.getRemoteAddr
		// if the forwarded remote IP address is not provided...
		if (forwardedRemoteIpAddress.isEmpty) {
			// we shall return the remote IP address...
			return remoteIpAddress
		}
		// we shall split the forwarded remote IP address by comma (',')...
		// NOTE: THE ARRAY RECEIVED AFTER SPLIT OPERATION HAS A MINIMUM LENGTH OF 1...
		val forwardedRemoteIpAddresses = forwardedRemoteIpAddress.split(",")
		// if there is only 1 forwarded remote IP address...
		if (forwardedRemoteIpAddresses.length == 1) {
			// we shall return the original forwarded remote IP address...
			// NOTE: WE COULD'VE RETURNED THE FIRST INDEX OF THE ARRAY BUT
			// WE'LL NEED TO TRIM THAT VALUE...
			return forwardedRemoteIpAddress
		}
		// otherwise, we shall take the first forwarded IP address
		// found in the array and sanitize the value...
		// NOTE: PLEASE CHECK THIS FOR FURTHER DEVELOPMENT
		remoteIpAddress = Option(forwardedRemoteIpAddresses(0)).map(_.trim).filterNot(_.isBlank).getOrElse(remoteIpAddress)
		// we shall return the remote IP address...
		remoteIpAddress
	}

	def sendSmartResponse(smartResponse: SmartResponse, response: HttpServletResponse): Unit = {
		// if a customized response is not available...
		val contentAsString = Option(smartResponse.customizedContent).filterNot(_.isEmpty).getOrElse(smartResponse.toJson())
		// decodes JSON content as byte array...
		val content = contentAsString.getBytes(StandardCharsets.UTF_8)
		// setting response status...
		response.setStatus(smartResponse.statusCode)
		// setting the content type of the response data...
		response.setContentType(smartResponse.contentType)
		// setting the content length...
		response.setContentLength(content.length)
		// getting the output stream to write response...
		val outputStream = response.getOutputStream
		outputStream.write(content, 0, content.length)
		outputStream.flush()
		outputStream.close()
	}
}
