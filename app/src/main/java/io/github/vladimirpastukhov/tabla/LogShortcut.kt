package io.github.vladimirpastukhov.tabla

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import java.util.UUID

/** What a pinned shortcut logs: the event title and the account calendar it goes to. */
data class LogShortcut(val title: String, val calendar: AccountCalendar) {

    fun toShortcutInfo(context: Context): ShortcutInfoCompat {
        val intent = Intent(context, LogActivity::class.java)
            .setAction(ACTION_LOG)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_CALENDAR_ID, calendar.calendarId)
            .putExtra(EXTRA_ACCOUNT_NAME, calendar.accountName)
        return ShortcutInfoCompat.Builder(context, UUID.randomUUID().toString())
            .setShortLabel(title)
            .setLongLabel(title)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
    }

    companion object {
        private const val ACTION_LOG = "io.github.vladimirpastukhov.tabla.LOG"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_CALENDAR_ID = "calendarId"
        private const val EXTRA_ACCOUNT_NAME = "accountName"

        /** Reads a shortcut back from the intent it launched, or null if the intent is not one. */
        fun fromIntent(intent: Intent): LogShortcut? {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: return null
            val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) ?: return null
            val calendarId = intent.getLongExtra(EXTRA_CALENDAR_ID, -1)
            return LogShortcut(title, AccountCalendar(calendarId, accountName))
        }
    }
}
