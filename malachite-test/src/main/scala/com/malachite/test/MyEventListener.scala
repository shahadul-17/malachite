package com.malachite.test

import com.malachite.core.kafka.{KafkaRecord, KafkaEventListener}
import org.apache.logging.log4j.{Level, LogManager}

case class MyEventListener() extends KafkaEventListener {

	private val logger = LogManager.getLogger(getClass)

	override def topic: String = "my-topic"

	override def onConsume(record: KafkaRecord): Unit = {
		logger.log(Level.DEBUG, "Successfully consumed id '{}', key '{}', value '{}' to topic '{}' (partition = '{}', offset = '{}')",
			record.recordId, record.key, record.value, record.topic, record.partition, record.offset)
	}
}
