package com.malachite.core.kafka

import com.malachite.core.text.JsonSerializable
import tools.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(as = classOf[KafkaRecordImpl])
trait KafkaRecord extends JsonSerializable {
	val topic: String
	val key: Option[String]
	val value: String
	val partition: Option[Int]
	val offset: Option[Long]
}

object KafkaRecord {
	def apply(topic: String,
			  value: String,
			  key: Option[String] = None,
			  partition: Option[Int] = None,
			  offset: Option[Long] = None,
			 ): KafkaRecord = KafkaRecordImpl(
		topic = topic,
		key = key,
		value = value,
		partition = partition,
		offset = offset,
	)
}

private case class KafkaRecordImpl(
	override val topic: String,
	override val key: Option[String],
	override val value: String,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	override val partition: Option[Int] = None,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	override val offset: Option[Long] = None,
) extends KafkaRecord
