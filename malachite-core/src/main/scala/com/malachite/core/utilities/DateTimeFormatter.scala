package com.malachite.core.utilities

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object DateTimeFormatter {

    private val EPSILON = 0.00001
    private val DEFAULT_DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"

    private def formatTime(time: Double, unit: String): String = {
        val rounded = Math.round((time + EPSILON) * 100) / 100.0
        s"$rounded $unit"
    }

    /** Formats the given time in milliseconds. */
    def formatTime(timeInMilliseconds: Double): String = {
        var time = timeInMilliseconds

        if (time < 1000) {
            return formatTime(time, "ms")
        }

        time /= 1000 // milliseconds -> seconds
        if (time < 60) {
            return formatTime(time, "seconds")
        }

        time /= 60 // seconds -> minutes
        if (time < 60) {
            return formatTime(time, "minutes")
        }

        time /= 60 // minutes -> hours
        if (time < 24) {
            return formatTime(time, "hours")
        }

        time /= 24 // hours -> days
        if (time < 30) {
            return formatTime(time, "days")
        }

        time /= 30 // days -> months
        if (time < 12) {
            return formatTime(time, "months")
        }

        time /= 12 // months -> years
        formatTime(time, "years")
    }

    /** Overload for `Long` milliseconds. */
    def formatTime(timeInMilliseconds: Long): String
        = formatTime(timeInMilliseconds.toDouble)

    /** Formats the given time in nanoseconds. */
    def formatNanosecondsTime(timeInNanoseconds: Double): String = {
        var time = timeInNanoseconds
        if (time < 1_000_000) {
            return formatTime(time, "ns")
        }

        time /= 1_000_000 // nanoseconds -> milliseconds
        formatTime(time)
    }

    /** Overload for `Long` nanoseconds. */
    def formatNanosecondsTime(timeInNanoseconds: Long): String
        = formatNanosecondsTime(timeInNanoseconds.toDouble)

    def createDateFormat(pattern: String = DEFAULT_DATE_TIME_FORMAT_PATTERN): DateFormat
        = new SimpleDateFormat(pattern)

    def parseDate(date: String, pattern: String = DEFAULT_DATE_TIME_FORMAT_PATTERN): Date
        = createDateFormat(pattern).parse(date)

    def formatDate(date: Date, pattern: String = DEFAULT_DATE_TIME_FORMAT_PATTERN): String
        = createDateFormat(pattern).format(date)
}
