# Tabla — one-tap calendar log

**Tabla** is both the project name and the app name shown on the device.

## Purpose

Log a routine action (e.g. "took the pill") to Google Calendar with a single tap
from the home screen. The calendar entry is a record that the action happened,
not a reminder for the future.

## User flow

1. **Create.** The user adds a "Tabla" shortcut from the launcher's
   widgets/shortcuts picker, or opens the Tabla app from the app drawer.
   A setup screen opens and asks for:
   - **Event name** — free text, e.g. "Pill".
   - **Google account** — chosen from the Google accounts already on the device.
   - **Color** — the calendar's own color (default) or one of the account's
     Google event colors.
2. **Place.** The launcher pins the result as a shortcut icon. It can sit on the
   home screen or in the bottom dock.
3. **Tap.** Each tap creates a 15-minute event in that account's main calendar
   and shows a toast, e.g. `Added: Pill 14:00–14:15`.

The user can create several shortcuts, each with its own name and account. To
change one, delete it and create a new one.

## Event rules

| Property  | Value |
|-----------|-------|
| Title     | Name given at setup |
| Calendar  | Main (primary) calendar of the chosen account |
| Color     | Color chosen at setup; calendar color if none |
| Start     | Current time rounded down to the quarter hour (:00, :15, :30, :45) |
| Duration  | Always 15 minutes |
| Reminders | None — the event never notifies |

Example: tap at 14:07 → event 14:00–14:15.

There is no double-tap protection: every tap creates a new event.

## Look

- Standard app-icon size and shape (1x1 slot).
- Icon: a round pill with a diagonal line across it.
- Label under the icon: the event name.

## Technical decisions

- **Pinned shortcut, not an app widget.** Most launchers (Pixel, One UI) do not
  allow widgets in the dock; pinned shortcuts are allowed there. The setup screen
  is an `ACTION_CREATE_SHORTCUT` activity that returns the shortcut via
  `ShortcutManager.createShortcutResultIntent`, so the launcher lists it in its
  picker and opens setup on creation.
- **Write through `CalendarContract`, not the Google Calendar API.** The event is
  inserted into the device's calendar provider and Google's own sync uploads it.
  No OAuth, no Cloud project, works offline. Needs `READ_CALENDAR` and
  `WRITE_CALENDAR`.
- **Account picker** lists calendars with `ACCOUNT_TYPE = "com.google"` and
  `IS_PRIMARY = 1`, one per account. The shortcut stores the calendar ID (plus
  account name as a fallback to find the calendar again if the ID changes).
- **Colors** come from `CalendarContract.Colors` (`TYPE_EVENT`) for the chosen
  account; the event stores `EVENT_COLOR_KEY` so Google syncs the color. If the
  key disappears from the account, the event falls back to the calendar color.
- **Tap handling** goes to a transparent, no-UI activity that inserts the event,
  shows the toast and finishes immediately.
- **Stack:** Kotlin, Jetpack Compose for the setup screen, min SDK 26
  (Android 8.0, first version with pinned shortcuts).

## Out of scope

- Editing an existing shortcut.
- Custom durations, reminders, or choosing a non-primary calendar.
- Accounts not signed in on the device.
- Undo, history view, or statistics.

## Open edge cases

- Calendar permission revoked after setup → tapping should show a toast asking
  to re-grant, and open the permission request.
- Account removed from the device → toast that the account is gone; the user
  recreates the shortcut.
