package com.malachite.core.security.authentication.apiclient

import com.malachite.core.security.authentication.abstractions.ValidationResponse

case class ApiClientValidationResponse(apiKey: String,
									   tokenContent: ApiClientTokenContent,
									  ) extends ValidationResponse[ApiClientTokenContent]
