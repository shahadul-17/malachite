package com.malachite.core.text

trait JsonSerializable {

	def toJson(prettyPrint: Boolean = false): String
		= JsonSerializer.serialize(this, prettyPrint)

	override def toString: String = toJson(true)
}
