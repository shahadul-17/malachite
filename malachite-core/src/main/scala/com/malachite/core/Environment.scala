package com.malachite.core

import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.extensions.AtomicReferenceExtensions.*
import com.malachite.core.security.authentication.abstractions.Credentials
import com.malachite.core.security.cryptography.HybridEncryptionProvider
import com.malachite.core.text.{Encoding, JsonSerializer}
import com.malachite.core.threading.BackgroundTaskExecutor
import com.malachite.core.utilities.FileSystemUtilities
import org.apache.logging.log4j.{Level, LogManager}

import java.util.concurrent.ExecutorService
import scala.compiletime.uninitialized

object Environment {

	private val logger = LogManager.getLogger(getClass)
	private val lastLoadedApiClientSystemCredentialsFilePath = new java.util.concurrent.atomic.AtomicReference[String]
	private val lastLoadedSecretKeyFilePath = new java.util.concurrent.atomic.AtomicReference[String]
	private val lastLoadedPublicKeyFilePath = new java.util.concurrent.atomic.AtomicReference[String]
	private val lastLoadedPrivateKeyFilePath = new java.util.concurrent.atomic.AtomicReference[String]
	private var _apiClientSystemCredentials: Credentials = uninitialized
	private var _secretKey: Array[Byte] = uninitialized
	private var _publicKey: Array[Byte] = uninitialized
	private var _privateKey: Array[Byte] = uninitialized
	private var _executorService: ExecutorService = uninitialized
	private var _backgroundTaskExecutor: BackgroundTaskExecutor = uninitialized

	def profile: String = ConfigurationProvider.getConfiguration.profile

	def instanceId: String = ConfigurationProvider.getConfiguration.instanceId

	def uniqueValue: String = ConfigurationProvider.getConfiguration.uniqueValue

	def applicationName: String = ConfigurationProvider.getConfiguration.applicationName

	def applicationDataDirectoryPath: String = ConfigurationProvider.getConfiguration.applicationDataDirectoryPath

	def applicationSpecificDataDirectoryPath: String = ConfigurationProvider.getConfiguration.applicationSpecificDataDirectoryPath

	def instanceSpecificDataDirectoryPath: String = ConfigurationProvider.getConfiguration.instanceSpecificDataDirectoryPath

	def executorService: ExecutorService = _executorService

	def apiClientSystemCredentials: Credentials = {
		val configuration = ConfigurationProvider.getConfiguration
		val currentApiClientSystemCredentialsFilePath = configuration.apiClientSystemCredentialsFilePath

		if (!lastLoadedApiClientSystemCredentialsFilePath.inverseCompareAndSet(currentApiClientSystemCredentialsFilePath)) {
			logger.log(Level.DEBUG, "Returning cached API client system credentials from file '{}'.", currentApiClientSystemCredentialsFilePath)

			return _apiClientSystemCredentials
		}

		logger.log(Level.DEBUG, "Loading new API client system credentials from file '{}'.", currentApiClientSystemCredentialsFilePath)

		val encryptedApiClientSystemCredentials = FileSystemUtilities.readFileAsString(currentApiClientSystemCredentialsFilePath)
			.decode(Encoding.URL_SAFE_BASE_64)
		val apiClientSystemCredentialsAsJson = HybridEncryptionProvider.decrypt(encryptedApiClientSystemCredentials, privateKey)
			.encode(Encoding.UTF_8)
		val apiClientSystemCredentials = JsonSerializer.deserialize(apiClientSystemCredentialsAsJson, classOf[Credentials])

		_apiClientSystemCredentials = apiClientSystemCredentials

		logger.log(Level.DEBUG, "Successfully loaded new API client system credentials from file '{}'.", currentApiClientSystemCredentialsFilePath)

		_apiClientSystemCredentials
	}

	def secretKey: Array[Byte] = {
		val configuration = ConfigurationProvider.getConfiguration
		val currentSecretKeyFilePath = configuration.secretKeyFilePath

		if (!lastLoadedSecretKeyFilePath.inverseCompareAndSet(currentSecretKeyFilePath)) {
			logger.log(Level.DEBUG, "Returning cached secret key from file '{}'.", currentSecretKeyFilePath)

			return _secretKey
		}

		logger.log(Level.DEBUG, "Loading new secret key from file '{}'.", currentSecretKeyFilePath)

		val newSecretKey = FileSystemUtilities.readFileAsString(currentSecretKeyFilePath).decode(Encoding.URL_SAFE_BASE_64)
		_secretKey = newSecretKey

		logger.log(Level.DEBUG, "Successfully loaded new secret key from file '{}'.", currentSecretKeyFilePath)

		_secretKey
	}

	def publicKey: Array[Byte] = {
		val configuration = ConfigurationProvider.getConfiguration
		val currentPublicKeyFilePath = configuration.publicKeyFilePath

		if (!lastLoadedPublicKeyFilePath.inverseCompareAndSet(currentPublicKeyFilePath)) {
			logger.log(Level.DEBUG, "Returning cached public key from file '{}'.", currentPublicKeyFilePath)

			return _publicKey
		}

		logger.log(Level.DEBUG, "Loading new public key from file '{}'.", currentPublicKeyFilePath)

		val newPublicKey = FileSystemUtilities.readFileAsString(currentPublicKeyFilePath).decode(Encoding.URL_SAFE_BASE_64)
		_publicKey = newPublicKey

		logger.log(Level.DEBUG, "Successfully loaded new public key from file '{}'.", currentPublicKeyFilePath)

		_publicKey
	}

	def privateKey: Array[Byte] = {
		val configuration = ConfigurationProvider.getConfiguration
		val currentPrivateKeyFilePath = configuration.privateKeyFilePath

		if (!lastLoadedPrivateKeyFilePath.inverseCompareAndSet(currentPrivateKeyFilePath)) {
			logger.log(Level.DEBUG, "Returning cached private key from file '{}'.", currentPrivateKeyFilePath)

			return _privateKey
		}

		logger.log(Level.DEBUG, "Loading new private key from file '{}'.", currentPrivateKeyFilePath)

		val newPrivateKey = FileSystemUtilities.readFileAsString(currentPrivateKeyFilePath).decode(Encoding.URL_SAFE_BASE_64)
		_privateKey = newPrivateKey

		logger.log(Level.DEBUG, "Successfully loaded new private key from file '{}'.", currentPrivateKeyFilePath)

		_privateKey
	}

	def invalidateCachedApiClientSystemCredentials(): Unit = {
		logger.log(Level.DEBUG, "Invalidating cached API client system credentials.")

		lastLoadedApiClientSystemCredentialsFilePath.set("")

		logger.log(Level.DEBUG, "Cached API client system credentials invalidation successful.")
	}

	def invalidateCachedKeys(): Unit = {
		logger.log(Level.DEBUG, "Invalidating cached keys.")

		lastLoadedSecretKeyFilePath.set("")
		lastLoadedPublicKeyFilePath.set("")
		lastLoadedPrivateKeyFilePath.set("")

		logger.log(Level.DEBUG, "Cached key invalidation successful.")
	}

	def backgroundTaskExecutor: BackgroundTaskExecutor = Option(_backgroundTaskExecutor)
		.getOrElse(throw new IllegalStateException("Background task executor isn't initialized."))

	private[core] def setExecutorService(executorService: ExecutorService): Unit = {
		Environment._executorService = executorService
	}

	private[core] def setBackgroundTaskExecutor(backgroundTaskExecutor: BackgroundTaskExecutor): Unit = {
		Environment._backgroundTaskExecutor = backgroundTaskExecutor
	}
}
