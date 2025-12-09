package com.malachite.core.utilities

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.io.Reader

object ReaderUtilities {

    private val logger = LogManager.getLogger(getClass)

    private val BUFFER_LENGTH = 8192
    private val STRING_BUILDER_INITIAL_CAPACITY = 8192

    /**
     * Reads characters into a portion of an array.
     * This method will block until some input is available,
     * an I/O error occurs, or the end of the stream is reached.
     * @param buffer Destination buffer.
     * @param offset Offset at which to start storing characters.
     * @param length Maximum number of characters to read.
     * @param reader Reader from which to start reading.
     * @return The number of characters read. Returns -1
     * if end of stream is reached. Returns -2 in case of exception.
     */
    def read(buffer: Array[Char], offset: Int, length: Int, reader: Reader): Int = {
        try {
            reader.read(buffer, offset, length)
        } catch {
            case exception: Exception =>
                logger.log(Level.WARN, "An exception occurred while reading from the reader.", exception)

                -2
        }
    }

    /**
     * Reads data from the reader as string.
     * @param reader Reader to read from.
     * @param closeAutomatically Setting this flag to true shall
     *                           close the reader after reading or exception.
     * @return The string data read from the reader.
     */
    def readString(reader: Reader, closeAutomatically: Boolean = true): Option[String] = {
        // if reader is null, we shall return empty string...
        if (reader == null) { return None }

        val buffer = new Array[Char](BUFFER_LENGTH)
        var bytesRead: Int = 0
        val contentBuilder = new java.lang.StringBuilder(STRING_BUILDER_INITIAL_CAPACITY)

        // NOTE: IF BYTES READ IS EQUAL TO -2, IT MEANS EXCEPTION HAS OCCURRED.
        // THUS, THIS LOOP SHALL BE BROKEN...
        bytesRead = read(buffer, 0, buffer.length, reader)

        while (bytesRead > 0) {
            contentBuilder.append(buffer, 0, bytesRead)

            bytesRead = read(buffer, 0, buffer.length, reader)
        }

        // if 'closeAutomatically' flag is true,
        // we shall try to close the reader...
        if (closeAutomatically) { CloseableUtilities.tryClose(reader) }
        // returns None in case of exception...
        if (bytesRead == -2) { None }
        else {
            Some(contentBuilder.toString.trim)
        }
    }
}
