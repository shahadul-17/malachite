package com.malachite.core.web.http

enum HttpStatusCode(val value: Int, val aliasOf: Option[HttpStatusCode] = None) {

	// --- Fallback ---
	case NONE extends HttpStatusCode(0)

	// --- 1xx Informational ---
	case CONTINUE extends HttpStatusCode(100)
	case SWITCHING_PROTOCOLS extends HttpStatusCode(101)
	case PROCESSING extends HttpStatusCode(102)
	case EARLY_HINTS extends HttpStatusCode(103)

	// --- 2xx Success ---
	case OK extends HttpStatusCode(200)
	case CREATED extends HttpStatusCode(201)
	case ACCEPTED extends HttpStatusCode(202)
	case NON_AUTHORITATIVE_INFORMATION extends HttpStatusCode(203)
	case NO_CONTENT extends HttpStatusCode(204)
	case RESET_CONTENT extends HttpStatusCode(205)
	case PARTIAL_CONTENT extends HttpStatusCode(206)
	case MULTI_STATUS extends HttpStatusCode(207)
	case ALREADY_REPORTED extends HttpStatusCode(208)
	case IM_USED extends HttpStatusCode(226)

	// --- 3xx Redirection ---
	case AMBIGUOUS extends HttpStatusCode(300)
	case MULTIPLE_CHOICES extends HttpStatusCode(AMBIGUOUS.value, Some(AMBIGUOUS))
	case MOVED_PERMANENTLY extends HttpStatusCode(301)
	case MOVED extends HttpStatusCode(MOVED_PERMANENTLY.value, Some(MOVED_PERMANENTLY))
	case FOUND extends HttpStatusCode(302)
	case REDIRECT extends HttpStatusCode(FOUND.value, Some(FOUND))
	case SEE_OTHER extends HttpStatusCode(303)
	case REDIRECT_METHOD extends HttpStatusCode(SEE_OTHER.value, Some(SEE_OTHER))
	case NOT_MODIFIED extends HttpStatusCode(304)
	case USE_PROXY extends HttpStatusCode(305)
	case UNUSED extends HttpStatusCode(306)
	case TEMPORARY_REDIRECT extends HttpStatusCode(307)
	case REDIRECT_KEEP_VERB extends HttpStatusCode(TEMPORARY_REDIRECT.value, Some(TEMPORARY_REDIRECT))
	case PERMANENT_REDIRECT extends HttpStatusCode(308)

	// --- 4xx Client Error ---
	case BAD_REQUEST extends HttpStatusCode(400)
	case UNAUTHORIZED extends HttpStatusCode(401)
	case PAYMENT_REQUIRED extends HttpStatusCode(402)
	case FORBIDDEN extends HttpStatusCode(403)
	case NOT_FOUND extends HttpStatusCode(404)
	case METHOD_NOT_ALLOWED extends HttpStatusCode(405)
	case NOT_ACCEPTABLE extends HttpStatusCode(406)
	case PROXY_AUTHENTICATION_REQUIRED extends HttpStatusCode(407)
	case REQUEST_TIMEOUT extends HttpStatusCode(408)
	case CONFLICT extends HttpStatusCode(409)
	case GONE extends HttpStatusCode(410)
	case LENGTH_REQUIRED extends HttpStatusCode(411)
	case PRECONDITION_FAILED extends HttpStatusCode(412)
	case REQUEST_ENTITY_TOO_LARGE extends HttpStatusCode(413)
	case REQUEST_URI_TOO_LONG extends HttpStatusCode(414)
	case UNSUPPORTED_MEDIA_TYPE extends HttpStatusCode(415)
	case REQUESTED_RANGE_NOT_SATISFIABLE extends HttpStatusCode(416)
	case EXPECTATION_FAILED extends HttpStatusCode(417)
	case MISDIRECTED_REQUEST extends HttpStatusCode(421)
	case UNPROCESSABLE_ENTITY extends HttpStatusCode(422)
	case UNPROCESSABLE_CONTENT extends HttpStatusCode(UNPROCESSABLE_ENTITY.value, Some(UNPROCESSABLE_ENTITY))
	case LOCKED extends HttpStatusCode(423)
	case FAILED_DEPENDENCY extends HttpStatusCode(424)
	case UPGRADE_REQUIRED extends HttpStatusCode(426)
	case PRECONDITION_REQUIRED extends HttpStatusCode(428)
	case TOO_MANY_REQUESTS extends HttpStatusCode(429)
	case REQUEST_HEADER_FIELDS_TOO_LARGE extends HttpStatusCode(431)
	case UNAVAILABLE_FOR_LEGAL_REASONS extends HttpStatusCode(451)

	// --- 5xx Server Error ---
	case INTERNAL_SERVER_ERROR extends HttpStatusCode(500)
	case NOT_IMPLEMENTED extends HttpStatusCode(501)
	case BAD_GATEWAY extends HttpStatusCode(502)
	case SERVICE_UNAVAILABLE extends HttpStatusCode(503)
	case GATEWAY_TIMEOUT extends HttpStatusCode(504)
	case HTTP_VERSION_NOT_SUPPORTED extends HttpStatusCode(505)
	case VARIANT_ALSO_NEGOTIATES extends HttpStatusCode(506)
	case INSUFFICIENT_STORAGE extends HttpStatusCode(507)
	case LOOP_DETECTED extends HttpStatusCode(508)
	case NOT_EXTENDED extends HttpStatusCode(510)
	case NETWORK_AUTHENTICATION_REQUIRED extends HttpStatusCode(511)

	// --- Instance methods ---
	def isCanonical: Boolean = aliasOf.isEmpty

	def isAlias: Boolean = !isCanonical

	def canonical: HttpStatusCode = aliasOf.getOrElse(this)
}

object HttpStatusCode {

	private val all = values.toIndexedSeq

	private val nameMap: Map[String, HttpStatusCode] =
		all.map(v => v.toString -> v).toMap

	private val valueMap: Map[Int, HttpStatusCode] =
		all.filter(_.isCanonical).map(v => v.value -> v).toMap

	def fromName(name: String): HttpStatusCode = {
		val sanitized = Option(name)
			.filterNot(_.isBlank)
			.map(_.trim.toUpperCase)
			.getOrElse("")
		if (sanitized.isEmpty) then { NONE }
		else { nameMap.getOrElse(sanitized, NONE) }
	}

	def fromValue(value: Int): HttpStatusCode =
		valueMap.getOrElse(value, NONE)

	def getAll: Seq[HttpStatusCode] = all
}
