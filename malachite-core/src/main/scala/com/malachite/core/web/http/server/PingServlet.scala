package com.malachite.core.web.http.server

import com.malachite.core.SmartResponse
import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.utilities.{HttpServletUtilities, ThreadUtilities}
import com.malachite.core.web.http.{HttpHeader, HttpMethod, HttpStatusCode}
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class PingServlet extends EnhancedHttpServlet {

    @Override
    override def processRequest(method: HttpMethod,
                                request: HttpServletRequest,
                                response: HttpServletResponse,
                               ): SmartResponse = {
        // for any method other than 'GET', we shall return null...
        if (method != HttpMethod.GET) { return null }

        // retrieving configuration...
        val configuration = ConfigurationProvider.getConfiguration
        // assigning the status code...
        val statusCode = HttpStatusCode.OK
        // assigning the message...
        val message = "Ping request processed successfully."
        // retrieving current profile...
        val profile = configuration.profile
        // retrieving the application name...
        val applicationName = configuration.applicationName
        // retrieving the instance ID...
        val instanceId = configuration.instanceId
        // retrieving the unique value of this application instance from configuration...
        // NOTE: THIS UNIQUE VALUE ENSURES THAT NO TWO APPLICATION INSTANCES CAN GENERATE THE SAME UNIQUE ID...
        val uniqueValue = configuration.uniqueValue
        // assigning the virtual thread name...
        val virtualThreadName = Thread.currentThread().getName
        // assigning the platform thread name...
        val platformThreadName = ThreadUtilities.currentPlatformThreadName
        // retrieving the client's IP address...
        val remoteIpAddress = HttpServletUtilities.retrieveRemoteIpAddress(request)
        // retrieving the client's user agent...
        val userAgent = request.getHeader(HttpHeader.USER_AGENT.value)
        // instantiating and preparing the smart response...
        val smartResponse = SmartResponse(statusCode, message)
                .addData("profile", profile)
                .addData("applicationName", applicationName)
                .addData("instanceId", instanceId)
                .addData("uniqueValue", uniqueValue)
                .addData("virtualThread", virtualThreadName)
                .addData("platformThread", platformThreadName)
                .addData("remoteIpAddress", remoteIpAddress)
                .addData("userAgent", userAgent)

        // returning the smart response...
        smartResponse
    }
}
