package com.malachite.core.security.authentication.apiclient

import com.malachite.core.security.authentication.abstractions.TokenContent
import tools.jackson.databind.annotation.JsonDeserialize

case class ApiClientTokenContent(version: String,
								 systemUser: Boolean,
								 issuedAt: Long,
								 @JsonDeserialize(contentAs = classOf[java.lang.Long])
								 expirationInMilliseconds: Option[Long],
								 additionalData: Option[Map[String, String]],
								 username: String,
								) extends TokenContent
