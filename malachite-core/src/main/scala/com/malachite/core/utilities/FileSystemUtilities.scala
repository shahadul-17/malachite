package com.malachite.core.utilities

import com.malachite.core.Environment
import com.malachite.core.extensions.ByteArrayExtensions.*
import com.malachite.core.extensions.StringExtensions.*
import com.malachite.core.text.Encoding
import org.apache.logging.log4j.{Level, LogManager}

object FileSystemUtilities {

	private val DIRECTORY_SEPARATOR = "/"
	private val DIRECTORY_SEPARATOR_SANITIZATION_REGULAR_EXPRESSION = "/+"
	private val BACKUP_FILE_TIMESTAMP_FORMATTER = "yyyyMMdd_HHmmss_SSS"
	private val PLATFORM_DEPENDENT_DIRECTORY_SEPARATORS = Array(
		"\\"
	)

	def getDirectorySeparator: String = DIRECTORY_SEPARATOR

	def resolvePathPlaceholders(path: String): String = {
		val date = DateTimeFormatter.formatDate(new java.util.Date())
		val profile = Environment.profile
		val instanceId = Environment.instanceId
		val uniqueValue = Environment.uniqueValue
		val applicationName = Environment.applicationName
		val applicationDataDirectoryPath = Environment.applicationDataDirectoryPath
		val applicationSpecificDataDirectoryPath = Environment.applicationSpecificDataDirectoryPath
		val instanceSpecificDataDirectoryPath = Environment.instanceSpecificDataDirectoryPath
		val resolvedPath = sanitizePath(path
			.replace("{{date}}", date)
			.replace("{{profile}}", profile)
			.replace("{{applicationName}}", applicationName)
			.replace("{{instanceId}}", instanceId)
			.replace("{{uniqueValue}}", uniqueValue)
			.replace("{{applicationData}}", applicationDataDirectoryPath)
			.replace("{{applicationSpecificData}}", applicationSpecificDataDirectoryPath)
			.replace("{{instanceSpecificData}}", instanceSpecificDataDirectoryPath))

		resolvedPath
	}

	def sanitizePath(path: String, resolveCanonicalPath: Boolean = true): String = {
		var sanitizedPath = path
		val directorySeparator = getDirectorySeparator

		// replaces all the platform-dependent directory separators with the common directory separator...
		PLATFORM_DEPENDENT_DIRECTORY_SEPARATORS.foreach { platformDependentDirectorySeparator =>
			sanitizedPath = sanitizedPath
				.replace(platformDependentDirectorySeparator, directorySeparator)
		}

		// replaces multiple occurrences of directory separators
		// with a single common directory separator...
		sanitizedPath = sanitizedPath
			.replaceAll(DIRECTORY_SEPARATOR_SANITIZATION_REGULAR_EXPRESSION, directorySeparator)

		if (resolveCanonicalPath) {
			new java.io.File(sanitizedPath).getCanonicalPath
		} else {
			sanitizedPath
		}
	}

	def extractDirectoryPath(filePath: String): Option[String] = {
		val file = java.io.File(filePath)
		val directoryPath = Option(file.getParent)
			.map(_.trim)
			.filter(_.nonEmpty)

		directoryPath
	}

	def createDirectoryIfDoesNotExist(directoryPath: String): Boolean = {
		Option(directoryPath).map(_.trim).filter(_.nonEmpty).exists { sanitizedPath =>
			val directory = java.io.File(sanitizedPath)

			try {
				// mkdirs returns false if already exists, true if created
				directory.mkdirs()

				true
			} catch {
				case exception: Exception =>
					val logger = LogManager.getLogger(getClass)
					logger.log(Level.WARN, s"An exception occurred while creating directory '$sanitizedPath'.", exception)

					false
			}
		}
	}

	/**
	 * Safely backs up a file by renaming it with a timestamp and incremental suffix.
	 * Example: file.txt -> file_backup_20251002_183012_123.txt
	 * If the timestamp backup already exists: file_backup_20251002_183012_123_1.txt
	 *
	 * @param filePath Path to the file to back up (must exist and be a file)
	 * @return Some(backupFile) if successful, None otherwise
	 */
	def backupFileWithTimestamp(filePath: String): Option[java.io.File] = {
		val file = new java.io.File(filePath)

		if (file == null || !file.exists() || !file.isFile) { return None }

		val sourcePath = file.toPath
		val parent = sourcePath.getParent.toString
		val fileName = file.getName
		val indexOfPeriod = fileName.lastIndexOf('.')
		val baseName = if (indexOfPeriod == -1) { fileName } else { fileName.substring(0, indexOfPeriod) }
		val extension = if (indexOfPeriod == -1) { "" } else { fileName.substring(indexOfPeriod) } // includes "."
		val timestamp = DateTimeFormatter.formatDate(new java.util.Date, BACKUP_FILE_TIMESTAMP_FORMATTER)
		var suffix = 0
		var candidateName = s"${baseName}_backup_$timestamp" + (if (suffix > 0) { s"_$suffix" } else { "" }) + extension
		var backupPath: java.nio.file.Path = java.nio.file.Paths.get(parent, candidateName)

		while (java.nio.file.Files.exists(backupPath)) {
			candidateName = s"${baseName}_backup_$timestamp" + (if (suffix > 0) { s"_$suffix" } else { "" }) + extension
			backupPath = java.nio.file.Paths.get(parent, candidateName)
			suffix += 1
		}

		try {
			try {
				java.nio.file.Files.move(sourcePath, backupPath, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
			} catch {
				case _: java.nio.file.AtomicMoveNotSupportedException =>
					java.nio.file.Files.move(sourcePath, backupPath)
			}

			Some(backupPath.toFile)
		} catch {
			case exception: Exception =>
				exception.printStackTrace()

				None
		}
	}

	def readFile(filePath: String): Array[Byte]
		= java.nio.file.Files.readAllBytes(java.nio.file.Path.of(filePath))

	def readFileAsString(filePath: String, encoding: Encoding = Encoding.UTF_8): String
		= readFile(filePath).encode(encoding)

	def writeToFile(content: Array[Byte], filePath: String, backupIfAlreadyExists: Boolean = false): Unit = {
		extractDirectoryPath(filePath).map { directoryPath =>
			createDirectoryIfDoesNotExist(directoryPath)
		}

		if (backupIfAlreadyExists) {
			backupFileWithTimestamp(filePath).foreach { backupFile =>
				val logger = LogManager.getLogger(getClass)
				logger.log(Level.INFO, "Successfully backed up the existing file, '{}' to '{}'.", filePath, backupFile)
			}
		}

		java.nio.file.Files.write(java.nio.file.Path.of(filePath), content)
	}

	def writeStringToFile(content: String, filePath: String, encoding: Encoding = Encoding.UTF_8, backupIfAlreadyExists: Boolean = false): Unit
		= writeToFile(content.decode(encoding), filePath, backupIfAlreadyExists)
}
