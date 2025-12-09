package com.malachite.core

import com.malachite.core.text.JsonSerializable
import com.malachite.core.utilities.{ExceptionUtilities, ObjectUtilities}
import com.malachite.core.web.http.HttpStatusCode

import java.net.http.HttpConnectTimeoutException
import java.util.Date
import scala.collection.mutable

class SmartException private (val smartExceptionData: SmartException.SmartExceptionData) extends Exception(smartExceptionData.message) {

	def this(statusCode: Int, message: String) = {
		this(SmartException.SmartExceptionData(
			statusCode = statusCode,
			message = message,
			stackTrace = "",
			thrownAt = new Date()
		))
		smartExceptionData.stackTrace = SmartException.retrieveStackTraceAsString(this)
	}

	def this(statusCode: HttpStatusCode, message: String) =
		this(statusCode.value, message)

	def this(statusCode: Int, message: String, data: Map[String, Any]) = {
		this(statusCode, message)

		smartExceptionData.data ++= data
	}

	def this(statusCode: HttpStatusCode, message: String, data: Map[String, Any]) =
		this(statusCode.value, message, data)

	def this(statusCode: Int, message: String, data: Any) =
		this(statusCode, message, ObjectUtilities.toMap(data))

	def this(statusCode: HttpStatusCode, message: String, data: Any) =
		this(statusCode.value, message, data)

	def statusCode: Int = smartExceptionData.statusCode

	def stackTraceAsString: String = smartExceptionData.stackTrace

	def getData[T](key: String): Option[T] =
		smartExceptionData.data.get(key).asInstanceOf[Option[T]]

	def getData[T](key: String, clazz: Class[T]): Option[T] = smartExceptionData.data.get(key) match {
		case Some(value) if clazz.isInstance(value) => Some(value.asInstanceOf[T])
		case _ => None
	}

	def allData: Map[String, Any] = smartExceptionData.data.toMap

	def addData(key: String, value: Any): SmartException = {
		smartExceptionData.data += (key -> value)

		this
	}

	def removeData(key: String): SmartException = {
		smartExceptionData.data -= key

		this
	}

	def clearData(): SmartException = {
		smartExceptionData.data.clear()

		this
	}

	override def toString: String = smartExceptionData.toString
}

object SmartException {

	private def retrieveStackTraceAsString(throwable: Throwable): String = {
		val stackTrace = ExceptionUtilities.retrieveStackTrace(throwable)
		val lastIndex = stackTrace.lastIndexOf('}')

		if (lastIndex == -1) {
			stackTrace
		}
		else {
			stackTrace.substring(lastIndex + 1).trim
		}
	}

	def find(throwable: Throwable): Option[SmartException] = {
		var cause = throwable

		while (cause != null) {
			cause match {
				case smartException: SmartException => return Some(smartException)
				case _: HttpConnectTimeoutException =>
					val smartException = new SmartException(
						HttpStatusCode.GATEWAY_TIMEOUT,
						"Could not connect to the requested server within the configured time period."
					)
					return Some(smartException)
				case _ =>
					cause = cause.getCause
			}
		}

		None
	}

	protected final case class SmartExceptionData(statusCode: Int,
												  message: String,
												  var stackTrace: String,
												  thrownAt: Date,
												  data: mutable.Map[String, Any] = mutable.HashMap.empty
												 ) extends JsonSerializable
}
