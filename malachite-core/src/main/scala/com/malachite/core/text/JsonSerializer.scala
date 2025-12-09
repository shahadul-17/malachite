package com.malachite.core.text

import com.malachite.core.utilities.DateTimeFormatter
import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.core.`type`.TypeReference
import tools.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import tools.jackson.module.scala.DefaultScalaModule
import org.apache.logging.log4j.LogManager
import tools.jackson.databind.json.JsonMapper

object JsonSerializer {

	private val logger = LogManager.getLogger(getClass)
	private val dateFormat = DateTimeFormatter.createDateFormat("dd-MMM-yyyy hh:mm:ss:SSS a z")
	// NOTE: INSTANCES OF THE OBJECT MAPPER CLASS ARE THREAD SAFE...
	private val primaryObjectMapper = createObjectMapper(true)
	// the secondary object mapper does not pretty-print the JSON...
	private val secondaryObjectMapper = createObjectMapper(false)

	private def createObjectMapper(prettyPrint: Boolean): ObjectMapper = {
		val objectMapperBuilder = JsonMapper.builder()
			// disables validation that no extra content follows a parsed value...
			.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
			// ignores unknown properties during deserialization...
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			// sets zero (0) if null is found for primitive types...
			.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
			// ignores empty objects during serialization...
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			// pretty-prints the JSON...
			.configure(SerializationFeature.INDENT_OUTPUT, prettyPrint)
			// registers the default Scala module...
			.addModule(DefaultScalaModule)
			// JSON mapper shall format dates in this format...
			.defaultDateFormat(dateFormat)

		// if object mapper shall not produce pretty-printed JSON...
		if (!prettyPrint) {
			objectMapperBuilder
				// we shall ignore null values during serialization...
				.changeDefaultPropertyInclusion(value => value.withValueInclusion(JsonInclude.Include.NON_NULL))
		}

		// builds the object mapper...
		objectMapperBuilder.build()
	}

	def serialize(value: Any, prettyPrint: Boolean = false): String = {
		// if pretty-print is enabled...
		val objectMapper = if (prettyPrint) { primaryObjectMapper }		// <-- we shall select the primary object mapper...
		else { secondaryObjectMapper }		// <-- otherwise, we shall select the secondary object mapper...

		objectMapper.writeValueAsString(value)
	}

	def deserialize[T](json: String, classOfType: Class[T]): T
		= primaryObjectMapper.readValue(json, classOfType)

	def deserialize[T](json: String, typeReference: TypeReference[T]): T
		= primaryObjectMapper.readValue(json, typeReference)
}
