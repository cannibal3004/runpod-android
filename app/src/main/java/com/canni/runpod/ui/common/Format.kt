package com.canni.runpod.ui.common

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val utcFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC)

fun String?.asInstantOrNull(): Instant? =
    takeIf { !isNullOrBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun formatUtc(iso: String?): String =
    iso.asInstantOrNull()?.let { utcFormatter.format(it) } ?: "—"

fun formatUptime(seconds: Long?): String {
    if (seconds == null || seconds < 0) return "—"
    val d = seconds / 86_400
    val h = (seconds % 86_400) / 3_600
    val m = (seconds % 3_600) / 60
    return when {
        d > 0 -> "${d}d ${h}h ${m}m"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}

fun formatCostPerHour(cost: Double?): String =
    cost?.let { "${"%.2f".format(it)} USD/hr" } ?: "—"

fun formatBucketLabel(startTime: String?, bucketSize: String): String {
    val instant = startTime.asInstantOrNull() ?: return "—"
    return if (bucketSize == "hour") {
        instant.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        instant.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MMM d"))
    }
}
