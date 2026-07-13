# TDD Alarm Clock

An Android alarm clock, built test-first, whose alarms know when *not* to ring:

- **Work-schedule aware** — a "skip on days off" alarm stays silent on non-working days,
  during PTO periods, and on holidays (one-time or annual).
- **Location aware** — an alarm can be restricted to a saved place ("only ring within
  25 km of Home"), so the Sunday church alarm doesn't wake you when you're out of town.
  Places are managed on their own screen, entered by street address (geocoded on-device
  via `Geocoder`) or from the current location, and referenced by any number of alarms.
  If the device location is unknown at ring time, the alarm fail-safes to ringing
  (configurable per alarm).
- **Reliable** — exact scheduling via `AlarmManager.setAlarmClock`, re-registration after
  reboot, foreground ringing service, full-screen dismiss/snooze over the lock screen.

## Architecture

```
core/   Pure-JVM Kotlin module (standalone Gradle build) — ALL decision logic
        WorkCalendar (working days + PTO + holidays)
        GeoFence / haversine distance
        AlarmGate      — the single ring-or-skip decision, evaluated at ring time
        NextTriggerCalculator — next occurrence + next *expected* firing
app/    Android module (MVVM) — Room persistence, AlarmManager scheduling,
        receivers/foreground service/firing screen, Fragment UI
```

The split is deliberate: every rule that decides whether you wake up is plain Kotlin with
no Android dependencies, developed red→green→refactor and runnable on any JVM:

```bash
cd core && gradle test          # no Android SDK required
```

The full build (unit tests for both modules + APK) runs in CI on every push:

```bash
./gradlew testDebugUnitTest     # app tests + core tests (via dependency)
./gradlew assembleDebug
```

Ring-time decisions are pure functions of (alarm, date, work calendar, location), so the
skip logic is exhaustively unit-tested — including PTO/holiday precedence, month/year
boundaries, unknown-location fail-safes, and combined work+location rules.

## Key behaviours

| Scenario | Result |
|---|---|
| Work alarm, normal Monday | rings |
| Work alarm, weekend / PTO / holiday | silent, next occurrence chained |
| Church alarm on Sunday, at home | rings |
| Church alarm on Sunday, out of town | silent |
| Church alarm, location unknown | rings (default, per-alarm configurable) |
| Any repeating alarm after a skipped day | automatically scheduled for its next day |
| Device reboot | all enabled alarms re-registered |
| Editor closed by tapping outside / Back | draft is saved and scheduled (never silently lost) |
| Editor closed via Cancel button | edits discarded — the only discard path |

## Toolchain

Kotlin 1.9.22 · AGP 8.2.0 · Gradle 8.4 (wrapper) · Room 2.6.1 (KSP) · minSdk 26 · targetSdk 34
