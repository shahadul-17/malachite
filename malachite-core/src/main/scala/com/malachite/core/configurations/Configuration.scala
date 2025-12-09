package com.malachite.core.configurations

import com.malachite.core.kafka.KafkaClientConfiguration
import com.malachite.core.security.authentication.apiclient.ApiClientAuthenticationServiceConfiguration
import com.malachite.core.security.authentication.user.UserAuthenticationServiceConfiguration
import com.malachite.core.threading.VirtualThreadExecutorServiceOptions
import com.malachite.core.web.http.server.HttpServerOptions

case class Configuration(version: String,
						 profile: String,
						 instanceId: String,
						 uniqueValue: String,
						 applicationName: String,
						 includeStackTrace: Boolean,
						 applicationDataDirectoryPath: String,
						 applicationSpecificDataDirectoryPath: String,
						 instanceSpecificDataDirectoryPath: String,
						 apiClientSystemCredentialsFilePath: String,
						 secretKeyFilePath: String,
						 publicKeyFilePath: String,
						 privateKeyFilePath: String,
						 httpClientHostnameVerificationDisabled: Boolean,
						 httpClientBasicAuthenticationRetryLimit: Int,
						 virtualThreadExecutorService: VirtualThreadExecutorServiceOptions,
						 httpServer: HttpServerOptions,
						 apiClientAuthenticationService: ApiClientAuthenticationServiceConfiguration,
						 userAuthenticationService: UserAuthenticationServiceConfiguration,
						 kafkaClient: KafkaClientConfiguration,
						)
