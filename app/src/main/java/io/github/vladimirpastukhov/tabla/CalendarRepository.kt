package io.github.vladimirpastukhov.tabla

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Colors
import android.provider.CalendarContract.Events
import java.time.ZonedDateTime

/** The main calendar of one Google account on this device. */
data class AccountCalendar(val calendarId: Long, val accountName: String, val color: Int = 0)

/** One of the event colors an account offers; [key] is what the event stores and Google syncs. */
data class EventColor(val key: String, val color: Int)

/** Reads and writes the device calendar provider; Google's sync adapter uploads the events. */
class CalendarRepository(private val resolver: ContentResolver) {

    /** One main calendar per Google account, sorted by account name. */
    fun mainCalendars(): List<AccountCalendar> {
        val projection = arrayOf(
            Calendars._ID,
            Calendars.ACCOUNT_NAME,
            Calendars.OWNER_ACCOUNT,
            Calendars.IS_PRIMARY,
            Calendars.CALENDAR_COLOR,
        )
        val rows = mutableListOf<Row>()
        resolver.query(
            Calendars.CONTENT_URI,
            projection,
            "${Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(GOOGLE_ACCOUNT_TYPE),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                rows += Row(
                    id = c.getLong(0),
                    accountName = c.getString(1),
                    ownerAccount = c.getString(2),
                    isPrimary = c.getInt(3) == 1,
                    color = c.getInt(4),
                )
            }
        }
        // IS_PRIMARY is the documented flag; for Google accounts the main calendar is also
        // the one owned by the account itself, which covers providers that leave it unset.
        return rows.groupBy { it.accountName }
            .mapNotNull { (account, calendars) ->
                val main = calendars.firstOrNull { it.isPrimary }
                    ?: calendars.firstOrNull { it.ownerAccount == account }
                main?.let { AccountCalendar(it.id, account, it.color) }
            }
            .sortedBy { it.accountName }
    }

    /** Event colors the account's sync adapter offers (Google syncs its fixed palette), in key order. */
    fun eventColors(accountName: String): List<EventColor> {
        val colors = mutableListOf<EventColor>()
        resolver.query(
            Colors.CONTENT_URI,
            arrayOf(Colors.COLOR_KEY, Colors.COLOR),
            "${Colors.ACCOUNT_NAME} = ? AND ${Colors.ACCOUNT_TYPE} = ? AND ${Colors.COLOR_TYPE} = ?",
            arrayOf(accountName, GOOGLE_ACCOUNT_TYPE, Colors.TYPE_EVENT.toString()),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                colors += EventColor(key = c.getString(0), color = c.getInt(1))
            }
        }
        // Google uses keys "1".."11"; sort numerically so the palette keeps its usual order.
        return colors.sortedWith(compareBy({ it.key.toIntOrNull() ?: Int.MAX_VALUE }, { it.key }))
    }

    /** The account's main calendar, preferring [calendarId] if it still belongs to the account. */
    fun findMainCalendar(calendarId: Long, accountName: String): AccountCalendar? {
        val calendars = mainCalendars().filter { it.accountName == accountName }
        return calendars.firstOrNull { it.calendarId == calendarId } ?: calendars.firstOrNull()
    }

    /**
     * Inserts a 15-minute event with no reminders; returns the new event id, or null on failure.
     * A null [colorKey] keeps the calendar's color; a key the account no longer has is dropped.
     */
    fun insertLogEvent(calendarId: Long, title: String, start: ZonedDateTime, colorKey: String?): Long? {
        if (colorKey != null) {
            try {
                return insertEvent(calendarId, title, start, colorKey)
            } catch (e: IllegalArgumentException) {
                // The provider rejects color keys missing from the account's palette.
            }
        }
        return insertEvent(calendarId, title, start, null)
    }

    private fun insertEvent(calendarId: Long, title: String, start: ZonedDateTime, colorKey: String?): Long? {
        val end = start.plus(EVENT_DURATION)
        val values = ContentValues().apply {
            put(Events.CALENDAR_ID, calendarId)
            put(Events.TITLE, title)
            put(Events.DTSTART, start.toInstant().toEpochMilli())
            put(Events.DTEND, end.toInstant().toEpochMilli())
            put(Events.EVENT_TIMEZONE, start.zone.id)
            put(Events.HAS_ALARM, 0)
            // A log entry should not mark the user as busy.
            put(Events.AVAILABILITY, Events.AVAILABILITY_FREE)
            if (colorKey != null) put(Events.EVENT_COLOR_KEY, colorKey)
        }
        val uri = resolver.insert(Events.CONTENT_URI, values) ?: return null
        return ContentUris.parseId(uri)
    }

    private data class Row(
        val id: Long,
        val accountName: String,
        val ownerAccount: String?,
        val isPrimary: Boolean,
        val color: Int,
    )

    private companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}
