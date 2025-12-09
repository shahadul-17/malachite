package com.malachite.core.security.authentication.user

import com.malachite.core.security.authentication.abstractions.TokenContent
import tools.jackson.databind.annotation.JsonDeserialize

case class UserTokenContent(version: String,
							issuedAt: Long,
							@JsonDeserialize(contentAs = classOf[java.lang.Long])
							expirationInMilliseconds: Option[Long],
							additionalData: Option[Map[String, String]],
							uniqueId: String,
							issuedBy: String,		// <-- this field uniquely identifies the owner of the token...
							contexts: Array[String],
							permissions: Array[Permission],
						   ) extends TokenContent
