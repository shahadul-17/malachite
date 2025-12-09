package com.malachite.core.configurations

import com.malachite.core.common.ArgumentsParser
import com.malachite.core.text.JsonSerializer
import com.malachite.core.threading.VirtualThreadExecutorServiceOptions
import com.malachite.core.utilities.{DateTimeFormatter, FileSystemUtilities}
import com.malachite.core.web.http.server.HttpServerOptions
import org.apache.logging.log4j.{Level, LogManager}

import java.io.InputStream
import scala.io.Source

private class ConfigurationProvider

object ConfigurationProvider {

	private val logger = LogManager.getLogger(getClass)
	private val configurationReference = new java.util.concurrent.atomic.AtomicReference[Configuration]()

	private def resolvePathPlaceholders(path: String, configurationOpt: Option[Configuration] = None): String = {
		val date = DateTimeFormatter.formatDate(new java.util.Date())
		val profile = configurationOpt.map(_.profile).getOrElse(ArgumentsParser.profile)
		val instanceId = configurationOpt.map(_.instanceId).getOrElse(ArgumentsParser.getArgument("instanceId"))
		val uniqueValue = configurationOpt.map(_.uniqueValue).getOrElse(ArgumentsParser.getArgument("uniqueValue"))
		val applicationName = configurationOpt.map(_.applicationName).getOrElse(ArgumentsParser.getArgument("applicationName"))
		val applicationDataDirectoryPath = configurationOpt.map(_.applicationDataDirectoryPath).getOrElse(ArgumentsParser.getArgument("applicationDataDirectoryPath"))
		val applicationSpecificDataDirectoryPath = configurationOpt.map(_.applicationSpecificDataDirectoryPath).getOrElse(ArgumentsParser.getArgument("applicationSpecificDataDirectoryPath"))
		val instanceSpecificDataDirectoryPath = configurationOpt.map(_.instanceSpecificDataDirectoryPath).getOrElse(ArgumentsParser.getArgument("instanceSpecificDataDirectoryPath"))
		val resolvedPath = FileSystemUtilities.sanitizePath(path
			.replace("{{date}}", date)
			.replace("{{profile}}", profile)
			.replace("{{applicationName}}", applicationName)
			.replace("{{instanceId}}", instanceId)
			.replace("{{uniqueValue}}", uniqueValue)
			.replace("{{applicationData}}", applicationDataDirectoryPath)
			.replace("{{applicationSpecificData}}", configurationOpt.map(_.applicationSpecificDataDirectoryPath).getOrElse(applicationDataDirectoryPath))
			.replace("{{instanceSpecificData}}", configurationOpt.map(_.instanceSpecificDataDirectoryPath).getOrElse(instanceSpecificDataDirectoryPath)))

		resolvedPath
	}

	private def getConfigurationAsStream: Option[InputStream] = {
		val profile = ArgumentsParser.profile
		val externalConfigurationFilePath = ArgumentsParser.getArgument("externalConfigurationFilePath")

		if (externalConfigurationFilePath.isEmpty) {
			val configurationFileName = s"configuration.$profile.json"

			logger.log(Level.INFO, "Loading configuration, '{}'.", configurationFileName)

			Option(getClass.getClassLoader.getResourceAsStream(configurationFileName)).orElse {
				logger.log(Level.WARN, "Failed to load configuration from resource, '{}'.", configurationFileName)

				None
			}
		} else {
			val sanitizedExternalConfigurationFilePath = resolvePathPlaceholders(externalConfigurationFilePath)

			logger.log(Level.INFO, "Loading external configuration, '{}'.", sanitizedExternalConfigurationFilePath)

			try {
				val inputStream: InputStream = new java.io.FileInputStream(sanitizedExternalConfigurationFilePath)

				Option(inputStream).orElse {
					logger.log(Level.WARN, "Failed to load external configuration from '{}'.", sanitizedExternalConfigurationFilePath)

					None
				}
			} catch {
				case exception: Exception =>
					logger.log(Level.ERROR, "Failed to load external configuration from '{}'.", sanitizedExternalConfigurationFilePath, exception)

					None
			}
		}
	}

	private def overwriteConfigurationFromArguments(configuration: Configuration) = {
		val virtualThreadExecutorServiceOptions = Option(configuration.virtualThreadExecutorService).getOrElse(VirtualThreadExecutorServiceOptions.DEFAULT)
		val httpServerOptions = Option(configuration.httpServer).getOrElse(HttpServerOptions.DEFAULT)
		var updatedConfiguration = configuration.copy(
			version = ArgumentsParser.getArgument("version", configuration.version),
			instanceId = ArgumentsParser.getArgument("instanceId", configuration.instanceId),
			uniqueValue = ArgumentsParser.getArgument("uniqueValue", configuration.uniqueValue),
			applicationName = ArgumentsParser.getArgument("applicationName", configuration.applicationName),
			includeStackTrace = ArgumentsParser.getArgumentAsBoolean("includeStackTrace", configuration.includeStackTrace),
			virtualThreadExecutorService = virtualThreadExecutorServiceOptions.copy(
				availablePlatformThreadCount = ArgumentsParser.getArgumentAsInteger("virtualThreadExecutorServiceAvailablePlatformThreadCount", virtualThreadExecutorServiceOptions.availablePlatformThreadCount),
				maximumPlatformThreadPoolSize = ArgumentsParser.getArgumentAsInteger("virtualThreadExecutorServiceMaximumPlatformThreadPoolSize", virtualThreadExecutorServiceOptions.maximumPlatformThreadPoolSize),
				minimumUnblockedPlatformThreadCount = ArgumentsParser.getArgumentAsInteger("virtualThreadExecutorServiceMinimumUnblockedPlatformThreadCount", virtualThreadExecutorServiceOptions.minimumUnblockedPlatformThreadCount)
			),
			httpServer = httpServerOptions.copy(
				host = ArgumentsParser.getArgument("httpServerHost", httpServerOptions.host),
				port = ArgumentsParser.getArgumentAsInteger("httpServerPort", httpServerOptions.port),
				contextPath = ArgumentsParser.getArgument("httpServerContextPath", httpServerOptions.contextPath),
				documentBaseDirectoryPath = ArgumentsParser.getArgument("httpServerDocumentBaseDirectoryPath", httpServerOptions.documentBaseDirectoryPath),
				requestHandlerBasePath = ArgumentsParser.getArgument("httpServerRequestHandlerBasePath", httpServerOptions.requestHandlerBasePath),
			)
		)

		val applicationDataDirectoryPath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("applicationDataDirectoryPath", updatedConfiguration.applicationDataDirectoryPath),
			Some(updatedConfiguration))

		updatedConfiguration = updatedConfiguration.copy(applicationDataDirectoryPath = applicationDataDirectoryPath)

		val applicationSpecificDataDirectoryPath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("applicationSpecificDataDirectoryPath", updatedConfiguration.applicationSpecificDataDirectoryPath),
			Some(updatedConfiguration))

		updatedConfiguration = updatedConfiguration.copy(applicationSpecificDataDirectoryPath = applicationSpecificDataDirectoryPath)

		val instanceSpecificDataDirectoryPath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("instanceSpecificDataDirectoryPath", updatedConfiguration.instanceSpecificDataDirectoryPath),
			Some(updatedConfiguration))

		updatedConfiguration = updatedConfiguration.copy(instanceSpecificDataDirectoryPath = instanceSpecificDataDirectoryPath)

		val apiClientSystemCredentialsFilePath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("apiClientSystemCredentialsFilePath", updatedConfiguration.apiClientSystemCredentialsFilePath),
			Some(updatedConfiguration))
		val secretKeyFilePath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("secretKeyFilePath", updatedConfiguration.secretKeyFilePath),
			Some(updatedConfiguration))
		val publicKeyFilePath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("publicKeyFilePath", updatedConfiguration.publicKeyFilePath),
			Some(updatedConfiguration))
		val privateKeyFilePath = resolvePathPlaceholders(
			ArgumentsParser.getArgument("privateKeyFilePath", updatedConfiguration.privateKeyFilePath),
			Some(updatedConfiguration))

		updatedConfiguration.copy(
			apiClientSystemCredentialsFilePath = apiClientSystemCredentialsFilePath,
			secretKeyFilePath = secretKeyFilePath,
			publicKeyFilePath = publicKeyFilePath,
			privateKeyFilePath = privateKeyFilePath,
		)
	}

	private def setSystemProperties(configuration: Configuration): Configuration = {
		val properties = System.getProperties

		// this property makes sure that the embedded apache tomcat throws
		// exceptions rather than handling them silently or just showing logs...
		properties.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true")

		configuration
	}

	def loadConfiguration: Configuration = {
		logger.log(Level.INFO, "Loading configuration.")

		// retrieves configuration as a stream...
		getConfigurationAsStream.map { inputStream =>
			// reads JSON content from the input stream...
			val content = Source.fromInputStream(inputStream).mkString
			// parses the JSON content as configuration...
			var configuration = JsonSerializer.deserialize(content, classOf[Configuration])

			// if the configuration is null, we shall throw an exception...
			if (configuration == null) {
				throw new Exception("Configuration could not be loaded. Please check previous logs for more details.")
			}

			// this method shall overwrite the configuration values
			// from command-line arguments...
			configuration = overwriteConfigurationFromArguments(configuration)
			// this method shall set configuration values
			// to system properties...
			configuration = setSystemProperties(configuration)

			// if the configuration is loaded successfully,
			// we'll set that to our static variable...
			setConfiguration(configuration)

			logger.log(Level.INFO, "Successfully loaded configuration for '{}' profile.", configuration.profile)

			configuration
		}.getOrElse {
			throw new Exception("Configuration could not be loaded. Please check previous logs for more details.")
		}
	}

	def tryLoadConfiguration(): Option[Configuration] = {
		try {
			val configuration = loadConfiguration

			Option(configuration)
		} catch {
			case exception: Exception =>
				logger.log(Level.ERROR, "An exception occurred while loading configuration.", exception)

				None
		}
	}

	def getConfiguration: Configuration = configurationReference.get()

	private def setConfiguration(configuration: Configuration): Unit
		= configurationReference.set(configuration)
}
