package com.malachite.core.utilities

import java.lang.reflect.Modifier
import scala.collection.mutable

object ObjectUtilities {

	def toMap(value: Any, includePrivateFields: Boolean = false): Map[String, Any] = {
		val map = mutable.HashMap.empty[String, Any]
		val clazz = value.getClass

		clazz.getDeclaredFields.map { field =>
			if (includePrivateFields) { field.setAccessible(true) }
			if (includePrivateFields || !Modifier.isPrivate(field.getModifiers)) {
				val fieldName = field.getName
				val fieldValue = field.get(value)

				map += (fieldName -> fieldValue)
			}
		}

		Map.from(map)
	}
}
