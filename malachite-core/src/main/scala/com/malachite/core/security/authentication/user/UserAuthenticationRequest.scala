package com.malachite.core.security.authentication.user

import com.malachite.core.security.authentication.abstractions.AuthenticationRequest
import tools.jackson.databind.annotation.JsonDeserialize

case class UserAuthenticationRequest(version: String,
									 owner: String,
									 @JsonDeserialize(contentAs = classOf[java.lang.Long])
									 uniqueId: String,					// <-- this field uniquely identifies the connected web socket client. a client may connect from multiple devices...
									 contexts: Array[String],
									 permissions: Array[Permission],
									 additionalData: Option[Map[String, String]] = None,
									) extends AuthenticationRequest
