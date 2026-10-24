package io.github.vladimirpastukhov.tabla

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import java.time.Instant
import java.time.ZoneId
import java.util.Date

/** Launched by a pinned shortcut: adds the event, shows a toast and finishes without any UI. */
class LogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Theme.NoDisplay requires finishing before onResume.
        toast(logEvent())
        finish()
    }

    private fun logEvent(): String {
        val shortcut = LogShortcut.fromIntent(intent) ?: return getString(R.string.add_failed)
        if (!hasCalendarPermission()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return getString(R.string.permission_needed)
        }
        val repository = CalendarRepository(contentResolver)
        val calendar = repository.findMainCalendar(shortcut.calendar.calendarId, shortcut.calendar.accountName)
            ?: return getString(R.string.account_missing, shortcut.calendar.accountName)

        val start = quarterHourStart(Instant.now(), ZoneId.systemDefault())
        repository.insertLogEvent(calendar.calendarId, shortcut.title, start, shortcut.colorKey)
            ?: return getString(R.string.add_failed)

        val timeFormat = DateFormat.getTimeFormat(this)
        val end = start.plus(EVENT_DURATION)
        return getString(
            R.string.added,
            shortcut.title,
            timeFormat.format(Date.from(start.toInstant())),
            timeFormat.format(Date.from(end.toInstant())),
        )
    }

    private fun toast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
