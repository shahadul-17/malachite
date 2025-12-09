package com.malachite.core.kafka3

import com.malachite.core.events.EventListener

trait KafkaEventListener extends EventListener[KafkaRecord]
