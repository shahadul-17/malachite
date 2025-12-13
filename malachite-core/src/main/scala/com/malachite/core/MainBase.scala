package com.malachite.core

import com.malachite.core.common.ArgumentsParser
import com.malachite.core.configurations.ConfigurationProvider
import com.malachite.core.threading.{BackgroundTaskExecutor, VirtualThreadExecutorService}
import com.malachite.core.utilities.{FileSystemUtilities, ReflectionUtilities, SystemUtilities}
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.slf4j.bridge.SLF4JBridgeHandler

trait MainBase {

    protected def getDefaultLogsDirectoryPath: String = MainBase.DEFAULT_LOGS_DIRECTORY_PATH

    protected def getLogsDirectoryPath: String = {
        // getting the logs directory path from command-line arguments...
        var logsDirectoryPath = ArgumentsParser.getArgument("logsDirectoryPath")

        // if the logs directory path is not found in the command-line arguments...
        if (logsDirectoryPath.isEmpty) {
            // we shall assign the default logs directory path...
            logsDirectoryPath = getDefaultLogsDirectoryPath
        }

        // we'll retrieve the instance ID from command-line arguments...
        val instanceId = ArgumentsParser.getArgument("instanceId")
        // we'll also get the application name from command-line arguments...
        val applicationName = ArgumentsParser.getArgument("applicationName")

        // replacing all the placeholders...
        logsDirectoryPath = logsDirectoryPath
                .replace("{{applicationName}}", applicationName)
                .replace("{{instanceId}}", instanceId)
        // sanitizing the path because it may contain "//"....
        logsDirectoryPath = FileSystemUtilities.sanitizePath(logsDirectoryPath)

        // finally, we shall return the logs directory path...
        logsDirectoryPath
    }

    def run[Type <: Application](arguments: Array[String], applicationClass: Class[Type]): Unit = {
        // checks if the application is currently running in DEBUG mode...
        SystemUtilities.checkDebugModeEnabled
        // populates the arguments...
        ArgumentsParser.populateArguments(arguments)

        // NOTE: REMOVE COMMENTS FROM THE LINES BELOW TO SEE ALL APACHE TOMCAT LOGS...!!!
        // java.util.logging.LogManager.getLogManager.reset()
        // java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.ALL)
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()

        // getting the logs directory path...
        val logsDirectoryPath = getLogsDirectoryPath

        // setting system property to use the logs directory path by the logger...
        // NOTE 1: BEFORE SETTING THIS SYSTEM PROPERTY, LOGGER SHALL NOT BE INITIALIZED...
        // NOTE 2: WE TRIED TO PLACE LOGS DIRECTORY PATH IN CONFIGURATION. BUT IT WOULD
        // BE TOO MESSY. SO, WE FOLLOWED THIS APPROACH. WE WOULD HIGHLY APPRECIATE IF
        // YOU CAN PLACE THE LOGS DIRECTORY PATH IN CONFIGURATION (ALONG WITH THE
        // COMMAND-LINE ARGUMENTS SUPPORT).
        System.setProperty("log4j.saveDirectory", logsDirectoryPath)
        // enables log4j2 thread context map inheritance so that the child threads
        // inherit the thread context map...
        System.setProperty("log4j2.isThreadContextMapInheritable", "true")

        // console logging is disabled if the command line argument
        // "consoleLoggingEnabled" is false or this application is
        // currently running on "production" mode/profile...
        val consoleLoggingDisabled = !ArgumentsParser.getArgumentAsBoolean("consoleLoggingEnabled", true)
            || ArgumentsParser.profile.equals("production")
        // placing "consoleLoggingDisabled" key to Log4j's thread context so that
        // console logging gets disabled (e.g. in production)...
        // NOTE: THIS PIECE OF CODE MUST APPEAR BEFORE INITIALIZING THE LOGGER...
        org.apache.logging.log4j.ThreadContext.put("consoleLoggingDisabled", String.valueOf(consoleLoggingDisabled))

        // file logging is disabled if the command line argument
        // "fileLoggingEnabled" is false...
        val fileLoggingDisabled = !ArgumentsParser.getArgumentAsBoolean("fileLoggingEnabled", true)
        // placing "fileLoggingDisabled" key to Log4j's thread context so that
        // file logging gets disabled...
        // NOTE: THIS PIECE OF CODE MUST APPEAR BEFORE INITIALIZING THE LOGGER...
        org.apache.logging.log4j.ThreadContext.put("fileLoggingDisabled", String.valueOf(fileLoggingDisabled))

        val logger = LogManager.getLogger(classOf[MainBase])

        logger.log(Level.INFO, "Application has started up.")
        logger.log(Level.INFO, "Log files are placed in '{}' directory.", logsDirectoryPath)

        var applicationOpt: Option[Application] = None
        var executorServiceOpt: Option[VirtualThreadExecutorService] = None
        var backgroundTaskExecutorOpt: Option[BackgroundTaskExecutor] = None

        try {
            // loads profile-specific configuration from resource (JSON file)...
            val configuration = ConfigurationProvider.loadConfiguration
            // instantiating a new executor service...
            val executorService = new VirtualThreadExecutorService(configuration.virtualThreadExecutorService)
            // assigning the virtual thread executor service to the executorServiceOpt...
            executorServiceOpt = Some(executorService)
            // also assigning the executor service to the environment...
            Environment.setExecutorService(executorService)

            // instantiating and starting a new background task executor...
            val backgroundTaskExecutor = BackgroundTaskExecutor().start
            // assigning it to the backgroundTaskExecutorOpt...
            backgroundTaskExecutorOpt = Some(backgroundTaskExecutor)
            // and assigning it to the environment...
            Environment.setBackgroundTaskExecutor(backgroundTaskExecutor)

            // instantiating the application...
            val application = ReflectionUtilities.createInstance(applicationClass)
            // assigning the application to the applicationOpt...
            applicationOpt = Some(application)
            // initializes the application...
            application.initialize()
            // resets the application...
            application.reset()
            // executes the application...
            application.execute()
        } catch {
            case exception: Exception =>
                logger.log(Level.ERROR, "An unexpected exception occurred.", exception)
        } finally {
            // stopping the background task executor and waiting for it to finish...
            backgroundTaskExecutorOpt.foreach(_.close())
            // releasing all the resources associated with the application...
            applicationOpt.foreach(_.dispose())

            // removing instances from the environment...
            Environment.setExecutorService(null)

            // releasing all the resources associated with the virtual thread executor service...
            executorServiceOpt.foreach(_.dispose())
            // releases all the resources associated with the logger...
            // NOTE: THIS METHOD IS CALLED TO ENSURE THAT THE BUFFERED
            // CONTENT GETS FLUSHED TO DISK...
            LogManager.shutdown()
        }
    }
}

private object MainBase {
	private val DEFAULT_LOGS_DIRECTORY_PATH = "application-data/{{applicationName}}/{{instanceId}}/logs"
}
