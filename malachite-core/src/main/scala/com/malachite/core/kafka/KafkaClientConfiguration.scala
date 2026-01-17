package com.malachite.core.kafka

import tools.jackson.databind.annotation.JsonDeserialize

case class KafkaClientConfiguration(
	servers: String,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	connectionTimeoutInMilliseconds: Long,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	pollTimeoutInMilliseconds: Long,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	threadSleepTimeoutInMilliseconds: Long,

	// Consumer limits
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumConcurrentTaskLimit: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumPollRecords: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumPollIntervalInMilliseconds: Int,

	// Producer limits
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumBufferedRecords: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	maximumProducerRecordBatchSize: Int,

	// Reliability
	enableAutoCommit: Boolean,
	autoOffsetReset: String,

	// Retry & backoff
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	retryCount: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Long])
	retryBackoffInMilliseconds: Long,

	// Consumer liveness
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	sessionTimeoutInMilliseconds: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	heartbeatIntervalInMilliseconds: Int,

	// Producer delivery guarantees
	enableIdempotence: Boolean,
	acknowledgments: String,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	lingerInMilliseconds: Int,
	@JsonDeserialize(contentAs = classOf[java.lang.Integer])
	bufferMemoryInBytes: Int,
)
