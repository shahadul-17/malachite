package com.malachite.core.security.authentication.apiclient

import com.malachite.core.security.authentication.abstractions.AuthenticationRequest

case class ApiClientAuthenticationRequest(username: String,
										  password: String,
										  additionalData: Option[Map[String, String]] = None,
										 ) extends AuthenticationRequest
