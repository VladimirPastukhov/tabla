package io.github.vladimirpastukhov.tabla

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

val EVENT_DURATION: Duration = Duration.ofMinutes(15)

/** Start of the quarter hour (:00, :15, :30, :45) that contains [now] in [zone]. */
fun quarterHourStart(now: Instant, zone: ZoneId): ZonedDateTime {
    val local = now.atZone(zone).truncatedTo(ChronoUnit.MINUTES)
    return local.withMinute(local.minute / 15 * 15)
}
