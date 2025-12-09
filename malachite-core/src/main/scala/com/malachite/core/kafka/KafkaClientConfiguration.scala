package com.malachite.core.kafka

import tools.jackson.databind.annotation.JsonDeserialize

case class KafkaClientConfiguration(
	servers: String,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	pollTimeoutInMilliseconds: Long,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	threadSleepTimeoutInMilliseconds: Long,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumConcurrentTaskLimit: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumPollRecords: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumPollIntervalInMilliseconds: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumBufferedRecords: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumProducerRecordBatchSize: Int,
)
