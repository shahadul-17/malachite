package com.malachite.core.security.authentication.user

enum Permission {
	case NONE
	case ECHO
	case PING
	case SELF
	case CLIENT
	case UNIQUE_ID
	case TOPIC
	case CONTEXT
	case BROADCAST
	case WILDCARD
}
