package com.itheamc.aiassistant.core.utils

import androidx.navigation.NavHostController
import com.itheamc.aiassistant.ui.navigation.AiAssistantRoute
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * String extension function to computes the human-readable difference between
 * the current time and the target ISO-8601 datetime.
 *
 * @return A string representing the difference in a human-readable format:
 *         - "5 minutes"
 *         - "1 hour 10 minutes"
 *         - "55 seconds"
 *         - "1 day 10 hours"
 *         - Returns "0 seconds" if the target time is in the past.
 *         - Returns "Invalid date" if the input string cannot be parsed.
 */
@OptIn(ExperimentalTime::class)
fun String.timeUntil(): String {
    return try {
        Instant.parse(this).timeUntil()
    } catch (_: Exception) {
        ""
    }
}

/**
 * Instant extension function to computes the human-readable difference between
 * the current time and the target ISO-8601 datetime.
 *
 * @return A string representing the difference in a human-readable format:
 *         - "5 minutes"
 *         - "1 hour 10 minutes"
 *         - "55 seconds"
 *         - "1 day 10 hours"
 *         - Returns "0 seconds" if the target time is in the past.
 *         - Returns "Invalid date" if the input string cannot be parsed.
 */
@OptIn(ExperimentalTime::class)
fun Instant.timeUntil(): String {
    return try {
        val target = this
        val now = Clock.System.now()
        val diff = target - now

        if (diff.inWholeSeconds <= 0) return ""

        val days = diff.inWholeHours / 24
        val hours = diff.inWholeHours % 24
        val minutes = (diff.inWholeMinutes % 60)
        val seconds = (diff.inWholeSeconds % 60)

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days day${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hour${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes minute${if (minutes > 1) "s" else ""}")
        if (seconds > 0 && parts.isEmpty()) parts.add("$seconds second${if (seconds > 1) "s" else ""}")

        parts.joinToString(" ")

    } catch (_: Exception) {
        ""
    }
}

/**
 * Calculates a human-readable "time ago" string relative to the current time.
 *
 * This extension function computes the duration between the `Instant` and the current system time,
 * then formats it into a user-friendly string. It breaks down the duration into days, hours,
 * and minutes.
 *
 * Example:
 * - If the `Instant` was 5 minutes ago, it returns `"5 minutes ago"`.
 * - If it was 1 day and 2 hours ago, it returns `"1 day 2 hours ago"`.
 * - If only seconds have passed, it returns `"X seconds ago"`.
 * - If the `Instant` is in the future, it returns an empty string.
 *
 * @receiver Instant The point in time to compare against the present.
 * @return A formatted string like "1 day 2 hours ago", "5 minutes ago", or an empty string if the time is in the future or an error occurs.
 */
@OptIn(ExperimentalTime::class)
fun String.timeAgo(): String {
    return try {
        Instant.parse(this).timeAgo()
    } catch (_: Exception) {
        ""
    }
}

@OptIn(ExperimentalTime::class)
fun Instant.timeAgo(): String {
    return try {
        val target = this
        val now = Clock.System.now()
        val diff = now - target

        if (diff.inWholeSeconds <= 0) return ""

        val days = diff.inWholeHours / 24

        // 👉 NEW: If more than 30 days, return formatted date
        if (days > 30) {
            val dt = target.toLocalDateTime(TimeZone.currentSystemDefault())

            val hour12 = when {
                dt.hour == 0 -> 12
                dt.hour > 12 -> dt.hour - 12
                else -> dt.hour
            }

            val amPm = if (dt.hour < 12) "AM" else "PM"

            fun Int.pad2() = toString().padStart(2, '0')

            return buildString {
                append(dt.year)
                append("-")
                append(dt.month.number.pad2())
                append("-")
                append(dt.day.pad2())
                append(" ")
                append(hour12.pad2())
                append(":")
                append(dt.minute.pad2())
                append(" ")
                append(amPm)
            }
        }


        val hours = diff.inWholeHours % 24
        val minutes = (diff.inWholeMinutes % 60)
        val seconds = (diff.inWholeSeconds % 60)

        val parts = mutableListOf<String>()
        if (days > 0) parts.add("$days day${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hour${if (hours > 1) "s" else ""}")
        if (minutes > 0) parts.add("$minutes minute${if (minutes > 1) "s" else ""}")
        if (seconds > 0 && parts.isEmpty()) parts.add("$seconds second${if (seconds > 1) "s" else ""}")

        parts.joinToString(" ") + " ago"

    } catch (_: Exception) {
        ""
    }
}

/**
 * Converts an ISO 8601 datetime string to a 12-hour time format with date
 * like "Dec 04, 2:29 PM" without using any external date-time library.
 *
 * Example:
 * "2025-12-04T14:29:31+05:45".formattedTimeWithDate()
 * -> "Dec 04, 2:29 PM"
 *
 * Returns an empty string if the input is invalid.
 */
@OptIn(ExperimentalTime::class)
fun String.formattedTimeWithDate(): String {
    return try {
        // Split date and time
        val parts = split("T")
        if (parts.size < 2) return ""

        val datePart = parts[0]
        val timePartRaw = parts[1]

        // Date parsing
        val dateComponents = datePart.split("-")
        if (dateComponents.size != 3) return ""

        val year = dateComponents[0].toInt() // not used but validated
        val month = dateComponents[1].toInt()
        val day = dateComponents[2].toInt()

        val monthName = when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> return ""
        }

        // Time parsing
        // Remove timezone offset or 'Z'
        val timePart = timePartRaw.takeWhile {
            it != '+' && it != '-' && it != 'Z'
        }

        val timeComponents = timePart.split(":")
        if (timeComponents.size < 2) return ""

        val hour24 = timeComponents[0].toInt()
        val minute = timeComponents[1].toInt()

        // 12-hour conversion
        val amPm = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

        "$monthName ${day.toString().padStart(2, '0')}, " +
                "$hour12:${minute.toString().padStart(2, '0')} $amPm"
    } catch (_: Exception) {
        ""
    }
}


/**
 * Extension function for [AiAssistantRoute] to handle navigation using a [NavHostController].
 * It constructs a navigation path with optional query parameters and provides standard
 * navigation options like popping up to a specific route.
 *
 * @param controller The [NavHostController] used to perform the navigation.
 * @param popUpTo The route to pop up to before navigating. If null, no backstack popping occurs.
 * @param inclusive Whether the [popUpTo] destination itself should be popped from the backstack.
 * @param launchSingleTop Whether to launch the destination as a single top instance.
 * @param queryParameters A map of key-value pairs to be appended to the route as query parameters.
 */
fun AiAssistantRoute.navigate(
    controller: NavHostController,
    popUpTo: String? = null,
    inclusive: Boolean = true,
    launchSingleTop: Boolean = true,
    queryParameters: Map<String, Any> = emptyMap(),
) {
    try {

        val path =
            if (queryParameters.isEmpty()) name else "$name?" + queryParameters.mapNotNull { "${it.key}=${it.value}" }
                .joinToString(separator = "&")

        controller.navigate(path) {
            popUpTo?.let {
                this.popUpTo(it) {
                    this.inclusive = inclusive
                }
            }

            this.launchSingleTop = launchSingleTop
        }
    } catch (_: Exception) {
        // Do nothing
    }
}