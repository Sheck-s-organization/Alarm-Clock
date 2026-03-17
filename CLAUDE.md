# CLAUDE.md — Smart Alarm Clock (Android / Kotlin)

> **Default branch**: `main`
> **Current date**: 2026-03-17

## Project Overview

A Kotlin-first Android alarm clock app (minSdk 26, targetSdk 34) built with:
- **Architecture**: MVVM + Repository pattern
- **UI**: Fragment-based navigation (Navigation Component), ViewBinding
- **Storage**: Room (KSP), WorkManager
- **DI / async**: Kotlin Coroutines + Flow
- **Location**: Google Play Services Fused Location

---

## Session Start Checklist

At the beginning of **every** session run these steps in order:

```bash
# 1. Make sure main exists locally; create it from master if absent
git fetch origin
git branch --list main | grep -q main \
  || git checkout -b main origin/master 2>/dev/null \
  || git checkout -b main master

# 2. Pull latest changes into main
git checkout main
git pull origin main   # or: git pull origin master if main tracks master

# 3. Create / switch to your feature branch off main
git checkout -b claude/<short-description>-<sessionId>   # new branch
# or: git checkout <existing-feature-branch>

# 4. Confirm starting state
git status
./gradlew testDebugUnitTest   # all tests must be green before any work begins
```

If `main` does not exist on the remote, create it from the current default branch:

```bash
git checkout -b main
git push -u origin main
```

---

## Development Workflow — TDD

Follow the **Red → Green → Refactor** cycle for every code change.

### 1. Red — write a failing test first

- Add a unit test under `app/src/test/` that describes the desired behaviour.
- Run it and confirm it **fails** for the right reason:

```bash
./gradlew testDebugUnitTest
```

### 2. Green — write the minimal production code to pass

- Implement only what is needed to make the failing test(s) pass.
- Re-run the tests and confirm they are **all green**.

### 3. Refactor — clean up

- Remove duplication, improve naming, simplify logic.
- Re-run tests after every change — they must stay green.

### Test locations

| Test type | Source set | Runner |
|---|---|---|
| Pure unit tests (no Android framework) | `app/src/test/` | JUnit 4 on the JVM |
| Instrumented / Room / UI tests | `app/src/androidTest/` | `AndroidJUnitRunner` on device/emulator |

Run unit tests only:
```bash
./gradlew testDebugUnitTest
```

Run instrumented tests (requires connected device or emulator):
```bash
./gradlew connectedDebugAndroidTest
```

---

## Pull Request Requirements

A PR **must not** be created until:

1. **All unit tests pass** — `./gradlew testDebugUnitTest` exits with code 0.
2. **New code is covered** — every new class / function introduced has at least one test.
3. **No regressions** — the full test suite is green, not just the new tests.
4. **Branch is up-to-date** — rebase or merge `main` into your feature branch before opening the PR.

```bash
# Pre-PR checklist
git fetch origin
git rebase origin/main          # or: git merge origin/main
./gradlew testDebugUnitTest     # must be: BUILD SUCCESSFUL
```

Only after all of the above should you open a PR targeting `main`.

---

## Build Commands

```bash
# Compile only (no tests)
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Full check (compile + lint + unit tests)
./gradlew check

# Clean build
./gradlew clean assembleDebug
```

---

## Project Structure

```
app/
  src/
    main/
      java/com/smartalarm/app/
        data/
          dao/          # Room DAOs
          db/           # Database + converters
          entities/     # Room entities (Alarm, WorkSchedule, …)
          repository/   # Repository layer
        manager/        # SmartAlarmManager — core scheduling logic
        receiver/       # BroadcastReceivers (alarm, boot, snooze, …)
        service/        # AlarmService (foreground)
        ui/             # Fragments + Adapters, organised by feature
        util/           # Constants, helpers
        viewmodel/      # ViewModels
    test/               # JVM unit tests (TDD target)
    androidTest/        # Instrumented tests
```

---

## Implementation Plan — Personalized Alarm Clock (Ground-Up)

All previous source code has been removed. We build one small, tested step at a time.
Each phase must have **all tests green** before the next phase begins.

The three personalisation pillars to reach by the end:
1. **Reliable alarm firing** — rings at the exact scheduled time, every time
2. **Work schedule integration** — skip alarms on days off / holidays
3. **City-aware alarms** — different alarm time depending on which city you're in
4. **Last-workday alarms** — fire on the last workday of the week, month, or both

---

### Phase 0 — Minimal Skeleton (app launches)

> Goal: the app compiles, launches, and shows a single empty screen. No features yet.

- [ ] **0.1** Create `MainActivity` — single `Activity` with an empty layout (`TextView` "Alarms").
- [ ] **0.2** Create `AlarmApplication` — bare `Application` subclass registered in manifest.
- [ ] **0.3** Verify `./gradlew assembleDebug` succeeds with zero errors.
- [ ] **0.4** Write a smoke unit test (e.g. `1 + 1 == 2`) to confirm the test runner works.

---

### Phase 1 — Alarm Data Model + Room

> Goal: a single `Alarm` can be saved to and read from the local database.

- [ ] **1.1** Define `Alarm` entity: `id`, `label`, `hour`, `minute`, `enabled`, `createdAt`.
- [ ] **1.2** Create `AlarmDao` with `insert`, `delete`, `getAll` (returns `Flow<List<Alarm>>`), `getById`.
- [ ] **1.3** Create `AppDatabase` (Room, version 1) with `AlarmDao`.
- [ ] **1.4** Create `AlarmRepository` wrapping the DAO.
- [ ] **1.5** Write unit tests for the repository using an in-memory Room database.

---

### Phase 2 — Alarm List UI

> Goal: user sees a list of alarms and can add / delete one.

- [ ] **2.1** Create `AlarmViewModel` exposing `allAlarms: StateFlow<List<Alarm>>`, `addAlarm()`, `deleteAlarm()`.
- [ ] **2.2** Build `AlarmsFragment` with a `RecyclerView` list and a FAB that opens a time-picker dialog.
- [ ] **2.3** Wire `MainActivity` → `AlarmsFragment` via Navigation Component.
- [ ] **2.4** Write unit tests for `AlarmViewModel` (add, delete, list).

---

### Phase 3 — Scheduling & Firing (core reliability)

> Goal: an enabled alarm fires at exactly the right time and rings the device.

- [ ] **3.1** Create `AlarmScheduler` — schedules/cancels alarms via `AlarmManager.setAlarmClock()`.
- [ ] **3.2** Create `AlarmReceiver` (`BroadcastReceiver`) — receives the trigger intent.
- [ ] **3.3** Create `AlarmService` (foreground) — plays the default ringtone + vibrates.
- [ ] **3.4** Create `AlarmFiringActivity` — full-screen dismiss screen shown over lock screen.
- [ ] **3.5** Create `BootReceiver` — reschedules all enabled alarms after device reboot.
- [ ] **3.6** Wire `AlarmRepository.setEnabled()` to call `AlarmScheduler.schedule/cancel`.
- [ ] **3.7** Write unit tests for `AlarmScheduler` next-trigger-time calculation.
- [ ] **3.8** Write unit tests for `AlarmReceiver` decision logic (mock `AlarmService` start).

---

### Phase 4 — Snooze & Dismiss

> Goal: user can snooze or dismiss a firing alarm; snooze re-fires after N minutes.

- [ ] **4.1** Add `snoozeDurationMinutes` and `snoozeMaxCount` fields to `Alarm` (Room migration v2).
- [ ] **4.2** Create `SnoozeReceiver` — reschedules a one-shot re-trigger.
- [ ] **4.3** Create `DismissReceiver` — stops `AlarmService`, cancels notification.
- [ ] **4.4** Update `AlarmFiringActivity` — show Snooze / Dismiss buttons; disable Snooze when limit reached.
- [ ] **4.5** Write unit tests for snooze rescheduling logic and max-snooze enforcement.

---

### Phase 5 — Work Schedule Integration

> Goal: alarms skip automatically on days off and holidays.

- [ ] **5.1** Define `WorkSchedule` entity: fixed weekdays (e.g. Mon–Fri), stored as `Set<Int>`.
- [ ] **5.2** Create `WorkScheduleDao`, `WorkScheduleRepository`, `WorkScheduleViewModel`.
- [ ] **5.3** Add a "Work schedule" screen so the user can pick their working days.
- [ ] **5.4** Add `workScheduleId` to `Alarm` (nullable; Room migration v3).
- [ ] **5.5** Update `AlarmReceiver` to skip firing if today is not a work day.
- [ ] **5.6** Define `Holiday` entity (one-time and annual types); `HolidayDao`; simple import of public holidays.
- [ ] **5.7** Update skip logic to also check holidays.
- [ ] **5.8** Write unit tests for all skip-vs-fire decision paths.

---

### Phase 6 — City-Aware Alarms

> Goal: user can set a different alarm time when they are in a specific city.

- [ ] **6.1** Define `SavedLocation` entity: `name`, `latitude`, `longitude`, `radiusMeters`.
- [ ] **6.2** Create `CityDetector` — resolves device lat/lng to a city name via `Geocoder` (mockable interface).
- [ ] **6.3** Add a "Locations" screen to save named places (GPS or manual entry).
- [ ] **6.4** Add optional `locationOverrides: List<LocationTimeOverride>` to `Alarm` (JSON column).
- [ ] **6.5** Update `AlarmReceiver` / `AlarmScheduler` to apply city-based time override when matched.
- [ ] **6.6** Write unit tests for `CityDetector` and the override evaluation logic.

---

### Phase 7 — Last-Workday Alarms

> Goal: an alarm can be set to fire on the last workday of the week, month, or both.

- [ ] **7.1** Add `RepeatMode` enum to `Alarm`: `DAILY`, `WEEKDAYS`, `LAST_WORKDAY_OF_WEEK`,
      `LAST_WORKDAY_OF_MONTH`, `LAST_WORKDAY_OF_WEEK_AND_MONTH`.
- [ ] **7.2** Implement last-workday calculation helpers (pure functions, no Android deps).
- [ ] **7.3** Expose repeat mode selector in the alarm edit UI.
- [ ] **7.4** Update `AlarmScheduler` to compute the correct next trigger for each repeat mode.
- [ ] **7.5** Write unit tests across month/year boundaries and with holidays blocking the last Friday.

---

### Phase 8 — Polish & Settings

> Goal: app is stable and user-configurable.

- [ ] **8.1** Add `SettingsFragment`: default snooze duration, vibration on/off, dismiss timeout.
- [ ] **8.2** Add alarm history log (`AlarmLog` entity) — view past fire/skip/dismiss events.
- [ ] **8.3** Handle GPS failures gracefully (fallback message, no crash).
- [ ] **8.4** Final pass: `./gradlew check` clean, all tests green.

---

### Definition of Done (per phase)

- Every new class / function has at least one unit test.
- `./gradlew testDebugUnitTest` exits 0 before any PR is opened.
- Each phase ships as its own PR targeting `main`.

---

## Coding Conventions

- **Language**: Kotlin only — no Java new files.
- **Formatting**: Follow standard Kotlin style (4-space indent, 100-char line limit).
- **Null safety**: Prefer non-nullable types; use `?.`, `?:`, and `requireNotNull` over `!!`.
- **Coroutines**: `viewModelScope` in ViewModels, `CoroutineScope(Dispatchers.IO)` in repositories.
- **Room queries**: Always `suspend` or return `Flow`; never block the main thread.
- **Tests**: Use JUnit 4 (`@Test`, `@Before`, `@After`). Mock Android framework dependencies with Mockito (`mockito-core`). Prefer pure-JVM tests in `src/test/` over instrumented tests where possible.

---

## Dependencies (key)

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 1.9.22 | Language |
| AGP | 8.2.0 | Android Gradle Plugin |
| Room | 2.6.1 | Local database |
| Navigation | 2.7.7 | Fragment navigation |
| WorkManager | 2.9.0 | Background scheduling |
| Coroutines | 1.7.3 | Async / concurrency |
| JUnit 4 | 4.13.2 | Unit testing |
| Mockito | 5.8.0 | Mocking in unit tests |
| Coroutines Test | 1.7.3 | `runTest`, `TestCoroutineDispatcher` |
