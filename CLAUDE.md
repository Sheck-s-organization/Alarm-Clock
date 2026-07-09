# CLAUDE.md — TDD Alarm Clock (Android / Kotlin)

## What this app is

A personalised Android alarm clock built strictly test-first. Alarms can be:
1. **Work-schedule aware** (`skipOnDaysOff`): silent on non-working days, PTO, holidays.
2. **Location aware** (`locationRule`): only rings inside the geo-fence of a saved place
   (e.g. near Home for the Sunday church alarm), with a per-alarm fail-safe when location
   is unknown. Places live in their own table (`SavedLocationEntity`) and are created by
   street address (`AddressResolver`/`Geocoder`) or current location; alarms reference
   them by `placeId` and the repository resolves the fence when loading.

## Build & test environment

- The Android SDK is **not** available in Claude Code sessions — never run
  `./gradlew` at the repo root locally; CI builds the app module on every push.
- The `core/` module is a **standalone pure-JVM Gradle build** and its tests DO run
  locally: `cd core && /opt/gradle/bin/gradle test` (the wrapper's distribution
  download is blocked locally; use the system Gradle).
- CI (`.github/workflows/build.yml`) runs `./gradlew testDebugUnitTest` (which also
  runs `core` tests via a task dependency) and `./gradlew assembleDebug`.

## Development workflow — TDD, non-negotiable

Red → Green → Refactor for every change:
1. Write the failing test first (`core/src/test/` for domain logic — preferred;
   `app/src/test/` for mappers/ViewModels using the fakes in `app/src/test/.../Fakes.kt`).
2. Run it locally when it's a core test; confirm it fails for the right reason.
3. Write the minimal production code; re-run; commit test + code together with a
   message that records the red→green cycle.

**All decision logic lives in `core`** (no Android imports there). If a new feature
needs a "should it ring?" or "when is the next one?" answer, extend `AlarmGate` /
`NextTriggerCalculator` / `WorkCalendar` in core with tests, then wire it in `app`.

## Structure

```
core/src/main/kotlin/com/tddalarm/core/
  schedule/   WorkCalendar, WorkSchedule, DateRange, Holiday, DayOffReason
  geo/        GeoPoint, distanceMeters (haversine), GeoFence
  alarm/      Alarm, LocationRule, AlarmGate, FiringDecision, SkipReason,
              NextTriggerCalculator
app/src/main/kotlin/com/tddalarm/app/
  data/       entities, DAOs, AppDatabase, Mappers, repo/ (ports + Room impls)
  scheduling/ AndroidAlarmScheduler, HandleAlarmTrigger, AlarmReceiver, BootReceiver
  firing/     AlarmRingService, AlarmFiringActivity, Dismiss/SnoozeReceiver
  location/   LocationProvider + AddressResolver (ports), Fused/Geocoder impls
  ui/         MainActivity, ViewModels + factory, alarms/, schedule/, locations/
```

Ports (`AlarmStore`, `WorkCalendarStore`, `AlarmSchedulerPort`, `LocationProvider`)
keep ViewModels and `HandleAlarmTrigger` unit-testable with the fakes — no Mockito
needed so far; prefer extending the fakes.

## Conventions

- Kotlin only, 4-space indent, ~100-char lines.
- Room queries: `suspend` or `Flow` only.
- JUnit 4; pure-JVM tests only in `src/test/` (no Robolectric).
- Every new class/function ships with at least one test in the same commit.
