package com.malachite.core.web

enum MediaType(val value: String) {
	case APPLICATION_JSON extends MediaType("application/json")
	case TEXT_XML extends MediaType("text/xml")

	lazy val withUtf8: String = s"$value${MediaType.CHARACTER_SET_SUFFIX_UTF_8}"
}

object MediaType {
	private val CHARACTER_SET_SUFFIX_UTF_8 = "; charset=utf-8"
}
