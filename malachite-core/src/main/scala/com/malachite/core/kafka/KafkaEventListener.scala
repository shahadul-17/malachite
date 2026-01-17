package com.malachite.core.kafka

trait KafkaEventListener {

	def topic: String

	def onConsume(record: KafkaRecord): Unit
}
