package com.malachite.core.utilities

object UrlUtilities {

	private val QUERY_STRING_SEPARATOR = '?'
	private val PATH_SEPARATOR = '/'
	private val PORT_SEPARATOR = ':'
	private val PROTOCOL_SEPARATOR = "://"

	/**
	 * Extracts host and port from the URL.
	 * e.g. For URL 'https://www.abc.com:8080/hello?ignoreStatus=true',
	 * this method shall return 'www.abc.com:8080'.
	 * @param url URL from which the host and port shall be extracted.
	 * @return The host and port of the URL separated by colon.
	 */
	def extractHostAndPort(url: String): String = {
		// finds the first index of the protocol separator...
		val indexOfProtocolSeparator = url.indexOf(PROTOCOL_SEPARATOR)

		// if a protocol separator is not found, we shall return the URL...
		if (indexOfProtocolSeparator == -1) { return url }

		// removes the protocol part from the URL...
		var hostAndPort = url.substring(indexOfProtocolSeparator + PROTOCOL_SEPARATOR.length())

		// finds the first index of path separator...
		val indexOfPathSeparator = hostAndPort.indexOf(PATH_SEPARATOR)

		// if a path separator is found, it means the URL contains a path...
		if (indexOfPathSeparator != -1) {
			// so, we shall remove the path from the URL to extract the host and port...
			hostAndPort = hostAndPort.substring(0, indexOfPathSeparator)
		}

		// finds the last index of query string separator...
		val lastIndexOfQueryStringSeparator = hostAndPort.lastIndexOf(QUERY_STRING_SEPARATOR)

		// if a query string separator is found, it means the URL contains a query string...
		if (lastIndexOfQueryStringSeparator != -1) {
			// so, we shall remove the query string from the URL to extract the host and port...
			hostAndPort = hostAndPort.substring(0, lastIndexOfQueryStringSeparator)
		}

		// returns the host and port...
		hostAndPort
	}

	/**
	 * Extracts host from the URL.
	 * e.g. For URL 'https://www.abc.com:8080/hello?ignoreStatus=true',
	 * this method shall return 'www.abc.com'.
	 * @param url URL from which the host shall be extracted.
	 * @return The host part of the URL.
	 */
	def extractHost(url: String): String = {
		// extracts host and port part of the url...
		val hostAndPort = extractHostAndPort(url)
		// finds the last index of port separator...
		val indexOfPortSeparator = hostAndPort.lastIndexOf(PORT_SEPARATOR)

		// if a port separator is not found, it means the host and port
		// do not contain port. so we shall return the host...
		if (indexOfPortSeparator == -1) { return hostAndPort }

		// but if host and port contains port, we shall remove the port
		// from the URL to extract the host...
		val host = hostAndPort.substring(0, indexOfPortSeparator)

		// returns the host...
		host
	}
}
