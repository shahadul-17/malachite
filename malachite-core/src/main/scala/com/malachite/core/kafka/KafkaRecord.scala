package com.malachite.core.kafka

import com.malachite.core.text.JsonSerializable
import tools.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(as = classOf[KafkaRecordImpl])
trait KafkaRecord extends java.lang.Comparable[KafkaRecord] with JsonSerializable {

	val recordId: Long
	val topic: String
	val key: String
	val value: String
	val partition: Option[Int]
	val offset: Option[Long]

	override def hashCode: Int = recordId.hashCode

	override def equals(obj: scala.Any): Boolean = obj match {
		case other: KafkaRecord =>
			recordId == other.recordId
		case _ =>
			false
	}

	override def compareTo(other: KafkaRecord): Int
		= java.lang.Long.compare(recordId, other.recordId)
}

object KafkaRecord {

	private val counter = new java.util.concurrent.atomic.AtomicLong(1L)

	private def generateRecordId: Long = counter.getAndIncrement

	def apply(topic: String,
			  key: String,
			  value: String,
			  partition: Option[Int] = None,
			  offset: Option[Long] = None,
			 ): KafkaRecord = KafkaRecordImpl(
		recordId = generateRecordId,
		topic = topic,
		key = key,
		value = value,
		partition = partition,
		offset = offset,
	)
}

private case class KafkaRecordImpl(
	override val recordId: Long,
	override val topic: String,
	override val key: String,
	override val value: String,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	override val partition: Option[Int] = None,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	override val offset: Option[Long] = None,
) extends KafkaRecord
