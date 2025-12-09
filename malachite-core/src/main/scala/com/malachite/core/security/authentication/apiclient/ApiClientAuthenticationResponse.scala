package com.malachite.core.security.authentication.apiclient

import com.malachite.core.security.authentication.abstractions.AuthenticationResponse

case class ApiClientAuthenticationResponse(apiKey: String,
										   token: String,
										  ) extends AuthenticationResponse
