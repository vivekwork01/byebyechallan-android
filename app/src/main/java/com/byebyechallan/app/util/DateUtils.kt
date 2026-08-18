package com.byebyechallan.app.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Backend's date-time fields (expiryDate, etc.) are typed as
 * "date-time" in the OpenAPI spec but we don't know if they'll come back with a
 * timezone offset (e.g. "2026-12-31T00:00:00+05:30") or without one
 * (e.g. "2026-12-31T00:00:00"). This utility tries both so the app doesn't crash
 * either way. Confirm exact format with backend once it's live and simplify if needed.
 */
object DateUtils {

    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    /** Parses a backend date-time string into epoch millis, or null if unparsable. */
    fun parseToEpochMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            try {
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (e2: DateTimeParseException) {
                try {
                    LocalDate.parse(value.substring(0, 10)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                } catch (e3: Exception) {
                    null
                }
            }
        }
    }

    /** Formats a backend date-time string as "dd MMM yyyy" for display, or returns a fallback. */
    fun formatForDisplay(value: String?, fallback: String = "—"): String {
        val millis = parseToEpochMillis(value) ?: return fallback
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .format(displayFormatter)
    }

    fun isExpired(value: String?): Boolean {
        val millis = parseToEpochMillis(value) ?: return false
        return millis < System.currentTimeMillis()
    }

    /** Returns true if the date is within the next N days (and not already expired). */
    fun isExpiringSoon(value: String?, withinDays: Long = 30): Boolean {
        val millis = parseToEpochMillis(value) ?: return false
        val now = System.currentTimeMillis()
        val thresholdMillis = withinDays * 24 * 60 * 60 * 1000
        return millis >= now && (millis - now) <= thresholdMillis
    }

    /** Converts a LocalDate picked in a date-picker UI into the ISO string format the backend expects. */
    fun toIsoDateTimeString(localDate: LocalDate): String {
        return localDate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
