package com.malachite.core.kafka3

import com.malachite.core.events.EventArgs
import tools.jackson.databind.annotation.JsonDeserialize

case class KafkaRecord(override val topic: String,
					   key: String,
					   value: String,
					   partition: Option[String] = None,
					   @JsonDeserialize(contentAs = classOf[java.lang.Long])
					   offset: Option[Long] = None,
					  ) extends EventArgs
