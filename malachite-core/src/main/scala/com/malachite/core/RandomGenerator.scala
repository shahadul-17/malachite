package com.malachite.core

import com.malachite.core.extensions.ByteArrayExtensions.*

object RandomGenerator {

	private var previouslyGeneratedRandomNumber: Long = System.nanoTime

	private val RANDOM_BYTE_COUNT: Int = 64 * 1024		// <-- NOTE: THIS MUST ALWAYS BE POWER OF TWO...!!!
	private val ONE_LESS_THAN_RANDOM_BYTE_COUNT: Int = RANDOM_BYTE_COUNT - 1
	private val ARBITRARY_NUMBER: Long = 7914285934107268631L
	private val MAGIC_NUMBER: Long = 0xff51afd7ed558ccdL
	private val RANDOM_BYTES: Array[Byte] = generateRandomBytes(RANDOM_BYTE_COUNT)
	private val WRITE_OFFSET = new java.util.concurrent.atomic.AtomicInteger(0)

	private def getReadOffset(bytesToReserve: Int): Int = WRITE_OFFSET.getAndUpdate { writeOffset =>
		val next = writeOffset + bytesToReserve

		next & ONE_LESS_THAN_RANDOM_BYTE_COUNT
	}

	private def generateRandomByte(seed: Long): Byte = {
		var randomNumber: Long = seed + previouslyGeneratedRandomNumber + ARBITRARY_NUMBER
		randomNumber = randomNumber ^ (randomNumber >>> 33)
		randomNumber = randomNumber * MAGIC_NUMBER
		previouslyGeneratedRandomNumber = randomNumber
		randomNumber = (randomNumber ^ (randomNumber >>> 24)) >>> 56

		randomNumber.toByte
	}

	private def generateRandomBytes(count: Int): Array[Byte] = {
		val randomBytes = new Array[Byte](count)

		for (i <- 0 until count) {
			randomBytes(i) = generateRandomByte(i)
		}

		randomBytes
	}

	def generateByte: Byte = {
		// atomically reserves a single byte and wraps the write-offset...
		val readOffset = getReadOffset(1)

		// returns the byte at the circular offset...
		RANDOM_BYTES(readOffset)
	}

	def generateBytes(count: Int): Array[Byte] = {
		// atomically reserves a contiguous range and wraps the write-offset...
		var readOffset = getReadOffset(count)
		val bytes = new Array[Byte](count)
		var destinationPosition = 0
		var remainingBytes = count

		while (remainingBytes > 0) {
			val chunkLength = java.lang.Math.min(remainingBytes, RANDOM_BYTE_COUNT - readOffset)

			System.arraycopy(RANDOM_BYTES, readOffset, bytes, destinationPosition, chunkLength)

			remainingBytes -= chunkLength
			destinationPosition += chunkLength
			readOffset = 0
		}

		bytes
	}

	def generateShort: Short = generateBytes(java.lang.Short.BYTES).toShort()

	def generateInteger: Int = generateBytes(java.lang.Integer.BYTES).toInteger()

	def generateLong: Long = generateBytes(java.lang.Long.BYTES).toLong()
}
