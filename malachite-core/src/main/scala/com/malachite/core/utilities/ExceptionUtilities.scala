package com.malachite.core.utilities

import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

object ExceptionUtilities {

    private val STRING_WRITER_INITIAL_CAPACITY = 8192

    /**
     * Retrieves the stack trace as string from the throwable/exception.
     * @param throwable Throwable/Exception from which the stack trace shall
     *                  be retrieved.
     * @return The stack trace.
     */
    def retrieveStackTrace(throwable: Throwable): String = {
        // initializing a string writer...
        val writer = new StringWriter(STRING_WRITER_INITIAL_CAPACITY)
        // preparing a print writer...
        val printWriter = new PrintWriter(writer)
        // exception prints the stack trace to the print writer...
        throwable.printStackTrace(printWriter)
        // we'll flush the print writer...
        printWriter.flush()
        // retrieves the print writer from the string writer...
        val stackTrace = writer.toString
        // finally, we must close the print writer...
        printWriter.close()

        // and return the stack trace...
        stackTrace
    }
}
