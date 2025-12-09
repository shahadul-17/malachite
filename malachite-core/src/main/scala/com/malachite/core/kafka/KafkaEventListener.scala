package com.malachite.core.kafka

import com.malachite.core.events.EventListener

trait KafkaEventListener extends EventListener[KafkaRecord]
