package com.malachite.core.security.authentication.user

import com.malachite.core.security.authentication.abstractions.ValidationRequest

case class UserValidationRequest(override val token: String) extends ValidationRequest
