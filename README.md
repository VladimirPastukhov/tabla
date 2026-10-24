# Tabla

A personal Android app I use to track my pill routine: once a day, after taking
the pill, I tap a button on my home screen and Tabla logs it to Google Calendar.

Tabla is not published on Google Play. It is built and installed from source.

## What it does

- Each Tabla button is a home-screen shortcut (it also fits in the dock) with a
  pill icon.
- When creating a button you choose the event name, the Google account and the
  event color.
- Each tap adds a 15-minute event to that account's main calendar, starting at
  the current quarter hour (a tap at 14:07 logs 14:00–14:15), with no reminders.
- A toast confirms what was added.

Events are written to the calendar on the device and Google syncs them, so no
Google sign-in inside the app is needed.

See [docs/idea.md](docs/idea.md) for the full design.

## Install

Requirements: Android 8.0 or newer, a Google account on the phone, Android SDK
and JDK 17 on the computer.

With USB debugging enabled on the phone:

```bash
./gradlew installDebug
```

Then open Tabla from the app drawer (or add a Tabla shortcut from the launcher's
widgets list), enter a name, pick an account and color, and tap Create.
