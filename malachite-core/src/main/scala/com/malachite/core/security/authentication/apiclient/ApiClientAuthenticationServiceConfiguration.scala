package com.malachite.core.security.authentication.apiclient

case class ApiClientAuthenticationServiceConfiguration(apiKeyVersion: String,
													   whitelistedApiClientInformationMap: Map[String, WhitelistedApiClientInformation],
													  )

case class WhitelistedApiClientInformation(id: Long,
										   systemUser: Boolean,
										   version: String,
										   firstName: String,
										   lastName: Option[String],
										   username: String,
										   hashedPassword: String,
										   apiKey: String,
										   tokenExpirationInMilliseconds: Option[Long],
										  )
