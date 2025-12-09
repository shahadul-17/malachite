package com.malachite.core.web.http.server

enum WebSocketMessageType {
	case NONE
	case ECHO
	case PING
	case CLIENT_VALIDATION
	case SUBSCRIBE_TO_TOPICS
	case UNSUBSCRIBE_FROM_TOPICS
	case SEND_TO_SELF_ONLY
	case SEND_TO_CLIENTS
	case SEND_TO_UNIQUE_IDS
	case SEND_TO_TOPICS
	case SEND_TO_CONTEXTS
	case BROADCAST
}
