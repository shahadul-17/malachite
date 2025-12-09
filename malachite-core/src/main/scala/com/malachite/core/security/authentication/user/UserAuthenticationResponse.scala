package com.malachite.core.security.authentication.user

import com.malachite.core.security.authentication.abstractions.AuthenticationResponse

case class UserAuthenticationResponse(token: String) extends AuthenticationResponse
