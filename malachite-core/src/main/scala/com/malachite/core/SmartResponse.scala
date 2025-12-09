package com.malachite.core

import com.malachite.core.text.JsonSerializer
import com.malachite.core.utilities.ExceptionUtilities
import com.malachite.core.web.MediaType
import com.malachite.core.web.http.HttpStatusCode

import java.util.Date
import scala.collection.mutable

class SmartResponse(val statusCode: Int, val message: String) {

	private var requestId: Long = -1L
	var time: Date = Date()
	var contentType: String = MediaType.APPLICATION_JSON.withUtf8
	var customizedContent: String = ""
	private val data: mutable.Map[String, Any] = mutable.HashMap.empty

	def getRequestId: Long = requestId

	def setRequestId(requestId: Long): SmartResponse = {
		this.requestId = requestId

		this
	}

	def getData(key: String): Option[Any] = Option(data).flatMap(_.get(key))

	def addData(key: String, value: Any): SmartResponse = {
		data.update(key, value)

		this
	}

	def addAllData(values: Map[String, Any]): SmartResponse = {
		if (values.isEmpty) { this }
		else {
			data ++= values

			this
		}
	}

	def removeData(key: String): SmartResponse = {
		data.remove(key)

		this
	}

	def clearData: SmartResponse = {
		data.clear()

		this
	}

	protected def toMap: Map[String, Any] = Map(
		"time" -> time,
		"statusCode" -> statusCode,
		"message" -> message,
		"data" -> Option(data).filterNot(_.isEmpty)
	)

	def toJson(prettyPrint: Boolean = false): String
		= JsonSerializer.serialize(toMap, prettyPrint)

	override def toString: String = toJson(true)
}

object SmartResponse {

	def apply(statusCode: Int, message: String): SmartResponse =
		new SmartResponse(statusCode = statusCode, message = message)

	def apply(statusCode: HttpStatusCode, message: String): SmartResponse =
		apply(statusCode.value, message)

	def from(throwable: Throwable,
			 defaultStatusCode: Int,
			 includeStackTrace: Boolean
			): SmartResponse = {
		SmartException.find(throwable).map { smartException =>
			val smartResponse = SmartResponse(smartException.statusCode, smartException.getMessage)
				.addAllData(smartException.allData)

			if (includeStackTrace) {
				smartResponse.addData("stackTrace", smartException.stackTraceAsString)
			}

			smartResponse
		}.getOrElse {
			val smartResponse = SmartResponse(defaultStatusCode, throwable.getMessage)

			if (includeStackTrace) {
				smartResponse.addData("stackTrace", ExceptionUtilities.retrieveStackTrace(throwable))
			}

			smartResponse
		}
	}
}
