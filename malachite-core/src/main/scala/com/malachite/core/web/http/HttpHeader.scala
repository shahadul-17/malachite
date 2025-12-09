package com.malachite.core.web.http

import scala.collection.mutable

sealed abstract class HttpHeader(val value: String)

object HttpHeader {

	// --- NONE ---
	case object NONE extends HttpHeader("")

	// --- Standard / Common Headers (Request + Response) ---
	case object ACCEPT extends HttpHeader("Accept")
	case object ACCEPT_CHARSET extends HttpHeader("Accept-Charset")
	case object ACCEPT_ENCODING extends HttpHeader("Accept-Encoding")
	case object ACCEPT_LANGUAGE extends HttpHeader("Accept-Language")
	case object AUTHORIZATION extends HttpHeader("Authorization")
	case object CACHE_CONTROL extends HttpHeader("Cache-Control")
	case object CONNECTION extends HttpHeader("Connection")
	case object COOKIE extends HttpHeader("Cookie")
	case object CONTENT_LENGTH extends HttpHeader("Content-Length")
	case object CONTENT_TYPE extends HttpHeader("Content-Type")
	case object EXPECT extends HttpHeader("Expect")
	case object FROM extends HttpHeader("From")
	case object HOST extends HttpHeader("Host")
	case object IF_MATCH extends HttpHeader("If-Match")
	case object IF_MODIFIED_SINCE extends HttpHeader("If-Modified-Since")
	case object IF_NONE_MATCH extends HttpHeader("If-None-Match")
	case object IF_RANGE extends HttpHeader("If-Range")
	case object IF_UNMODIFIED_SINCE extends HttpHeader("If-Unmodified-Since")
	case object MAX_FORWARDS extends HttpHeader("Max-Forwards")
	case object ORIGIN extends HttpHeader("Origin")
	case object PRAGMA extends HttpHeader("Pragma")
	case object PROXY_AUTHORIZATION extends HttpHeader("Proxy-Authorization")
	case object RANGE extends HttpHeader("Range")
	case object REFERER extends HttpHeader("Referer")
	case object TE extends HttpHeader("TE")
	case object USER_AGENT extends HttpHeader("User-Agent")
	case object UPGRADE extends HttpHeader("Upgrade")
	case object VIA extends HttpHeader("Via")
	case object FORWARDED_FOR extends HttpHeader("X-Forwarded-For")
	case object REQUESTED_WITH extends HttpHeader("X-Requested-With")
	case object API_KEY extends HttpHeader("X-API-Key")
	case object TOKEN extends HttpHeader("X-TOKEN")
	case object ACCESS_TOKEN extends HttpHeader("X-ACCESS-TOKEN")
	case object REFRESH_TOKEN extends HttpHeader("X-REFRESH-TOKEN")
	case object SENDER_SESSION_GUID extends HttpHeader("X-Sender-Session-Guid")

	// --- Response-specific headers ---
	case object DATE extends HttpHeader("Date")
	case object TRAILER extends HttpHeader("Trailer")
	case object WARNING extends HttpHeader("Warning")
	case object WWW_AUTHENTICATE extends HttpHeader("WWW-Authenticate")
	case object PROXY_AUTHENTICATE extends HttpHeader("Proxy-Authenticate")
	case object ETAG extends HttpHeader("ETag")
	case object EXPIRES extends HttpHeader("Expires")
	case object LAST_MODIFIED extends HttpHeader("Last-Modified")
	case object VARY extends HttpHeader("Vary")
	case object LOCATION extends HttpHeader("Location")
	case object REFRESH extends HttpHeader("Refresh")
	case object SERVER extends HttpHeader("Server")
	case object SET_COOKIE extends HttpHeader("Set-Cookie")
	case object STRICT_TRANSPORT_SECURITY extends HttpHeader("Strict-Transport-Security")
	case object CONTENT_ENCODING extends HttpHeader("Content-Encoding")
	case object CONTENT_LANGUAGE extends HttpHeader("Content-Language")
	case object CONTENT_LOCATION extends HttpHeader("Content-Location")
	case object CONTENT_MD5 extends HttpHeader("Content-MD5")
	case object CONTENT_RANGE extends HttpHeader("Content-Range")
	case object ACCEPT_RANGES extends HttpHeader("Accept-Ranges")
	case object POWERED_BY extends HttpHeader("X-Powered-By")

	val values: Array[HttpHeader] = Array(
		NONE, ACCEPT, ACCEPT_CHARSET, ACCEPT_ENCODING, ACCEPT_LANGUAGE, AUTHORIZATION,
		CACHE_CONTROL, CONNECTION, COOKIE, CONTENT_LENGTH, CONTENT_TYPE, EXPECT, FROM,
		HOST, IF_MATCH, IF_MODIFIED_SINCE, IF_NONE_MATCH, IF_RANGE, IF_UNMODIFIED_SINCE,
		MAX_FORWARDS, ORIGIN, PRAGMA, PROXY_AUTHORIZATION, RANGE, REFERER, TE, USER_AGENT,
		UPGRADE, VIA, FORWARDED_FOR, REQUESTED_WITH, API_KEY, TOKEN, ACCESS_TOKEN,
		REFRESH_TOKEN, SENDER_SESSION_GUID, DATE, TRAILER, WARNING, WWW_AUTHENTICATE,
		PROXY_AUTHENTICATE, ETAG, EXPIRES, LAST_MODIFIED, VARY, LOCATION, REFRESH, SERVER,
		SET_COOKIE, STRICT_TRANSPORT_SECURITY, CONTENT_ENCODING, CONTENT_LANGUAGE, CONTENT_LOCATION,
		CONTENT_MD5, CONTENT_RANGE, ACCEPT_RANGES, POWERED_BY
	)

	private val nameMap: Map[String, HttpHeader] = {
		val map = mutable.Map[String, HttpHeader]()
		values.foreach(header => map.getOrElseUpdate(header.getClass.getSimpleName.stripSuffix("$").toUpperCase, header))
		map.toMap
	}

	private val valueMap: Map[String, HttpHeader] = {
		val map = mutable.Map[String, HttpHeader]()
		values.foreach(header => map.getOrElseUpdate(header.value.toUpperCase, header))
		map.toMap
	}

	def fromName(name: String): HttpHeader = {
		val sanitized = Option(name).map(_.trim).filterNot(_.isBlank).getOrElse("").toUpperCase
		if (sanitized.isEmpty) { NONE }
		else { nameMap.getOrElse(sanitized, NONE) }
	}

	def fromValue(value: String): HttpHeader = {
		val sanitized = Option(value).map(_.trim).filterNot(_.isBlank).getOrElse("").toUpperCase
		if (sanitized.isEmpty) { NONE }
		else { valueMap.getOrElse(sanitized, NONE) }
	}

	def getAll: Array[HttpHeader] = values
}
