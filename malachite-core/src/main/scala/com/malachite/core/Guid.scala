package com.malachite.core

import com.malachite.core.extensions.ByteArrayExtensions.*

trait Guid extends java.lang.Comparable[Guid] {

	val version: Int
	protected val value: String

	override def hashCode: Int = value.hashCode

	override def equals(obj: Any): Boolean = obj match {
		case guidAsString: String => guidAsString.equals(value)
		case guid: Guid => guid.value.equals(value)
		case _ => false
	}

	override def compareTo(guid: Guid): Int
		= value.compareTo(guid.value)

	override def toString: String = value
}

object Guid {

	private val DEFAULT_VERSION = 7

	def apply(version: Int, guidAsString: String): Guid
		= new GuidImpl(version, guidAsString)

	def apply(version: Int, uuid: java.util.UUID): Guid
		= Guid(version, uuid.toString)

	def newGuid(version: Int = DEFAULT_VERSION): Guid = version match {
		case 4 => createVersion4
		case _ => createVersion7
	}

	def createVersion4: Guid = {
		val version = 4
		val uuid = java.util.UUID.randomUUID

		Guid(version, uuid)
	}

	def createVersion7: Guid = createVersion7(java.time.Instant.now)

	def createVersion7(timestamp: java.time.Instant): Guid = {
		val unixMs = timestamp.toEpochMilli

		require(unixMs >= 0L, "timestamp must be >= Unix epoch")

		val bytes = new Array[Byte](16)
		val random: java.util.Random = java.util.concurrent.ThreadLocalRandom.current
		random.nextBytes(bytes)

		// Insert 48-bit timestamp (big-endian)
		bytes(0) = ((unixMs >>> 40) & 0xFF).toByte
		bytes(1) = ((unixMs >>> 32) & 0xFF).toByte
		bytes(2) = ((unixMs >>> 24) & 0xFF).toByte
		bytes(3) = ((unixMs >>> 16) & 0xFF).toByte
		bytes(4) = ((unixMs >>> 8) & 0xFF).toByte
		bytes(5) = (unixMs & 0xFF).toByte
		// Version: set high nibble of byte 6 → 0111xxxx (0x70)
		bytes(6) = ((bytes(6) & 0x0F) | 0x70).toByte
		// Variant: RFC4122 → 10xxxxxx (0x80) on byte 8
		bytes(8) = ((bytes(8) & 0x3F) | 0x80).toByte

		val version = 7
		val mostSignificantBits = bytes.toLong(0)
		val leastSignificantBits = bytes.toLong(8)
		val uuid = new java.util.UUID(mostSignificantBits, leastSignificantBits)

		Guid(version, uuid)
	}

	def createVersion7(timestamp: java.util.Date): Guid
		= createVersion7(java.time.Instant.ofEpochMilli(timestamp.getTime))
}

private final class GuidImpl(override val version: Int,
							 override val value: String,
							) extends Guid
