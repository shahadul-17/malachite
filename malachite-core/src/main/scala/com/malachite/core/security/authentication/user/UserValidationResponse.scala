package com.malachite.core.security.authentication.user

import com.malachite.core.security.authentication.abstractions.ValidationResponse

case class UserValidationResponse(override val tokenContent: UserTokenContent,
								 ) extends ValidationResponse[UserTokenContent]
