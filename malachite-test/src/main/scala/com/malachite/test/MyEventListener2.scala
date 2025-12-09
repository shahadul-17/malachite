package com.malachite.test

import com.malachite.core.kafka.{KafkaRecord, KafkaEventListener}
import org.apache.logging.log4j.{Level, LogManager}

case class MyEventListener2() extends KafkaEventListener {

	private val logger = LogManager.getLogger(getClass)

	override def topic: String = "my-topic-2"

	override def onConsume(record: KafkaRecord): Unit = {
		logger.log(Level.DEBUG, "Successfully consumed key '{}', value '{}' to topic '{}' (partition = '{}', offset = '{}')",
			record.key, record.value, record.topic, record.partition, record.offset)
	}
}
