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

## Implementation Plan — Personalized Alarm Clock

The goal is a personalized alarm clock with three core pillars:
1. **Work schedule integration** — skip alarms on days off / holidays
2. **City-aware alarms** — different alarm behaviour depending on which city you're in
3. **Last-workday alarms** — fire on the last workday of the week, month, or both

Most of the backend logic already exists in `SmartAlarmManager`. The plan below focuses on
wiring everything together, exposing it in the UI, and ensuring solid test coverage at each step.

---

### Phase 1 — Test Foundation (Red → Green baseline)

> Goal: every existing code path has at least one passing test before new work begins.

- [ ] **1.1** Run `./gradlew testDebugUnitTest` and fix any currently failing tests.
- [ ] **1.2** Add unit tests for `SmartAlarmManager` covering:
  - `isWorkDay()` for both FIXED_DAYS and ROTATING schedules
  - `isLastWorkdayOfWeek()` and `isLastWorkdayOfMonth()` across month boundaries
  - `evaluateAlarmAtTrigger()` — all `AlarmDecision` outcomes (Fire, FireWithOverride, Skip)
  - Holiday suppression logic (ONE_TIME, ANNUAL, RANGE types)
- [ ] **1.3** Add unit tests for `HolidayRepository.isTodayHoliday()` date-matching logic.
- [ ] **1.4** Add unit tests for `LocationRepository.isInsideLocation()` distance calculation.

---

### Phase 2 — Work Schedule Integration (end-to-end)

> Goal: a user can define their work schedule and alarms automatically skip on days off.

- [ ] **2.1** Verify `EditScheduleFragment` correctly saves FIXED_DAYS schedules (e.g. Mon–Fri).
- [ ] **2.2** Verify `AlarmViewModel.createWorkScheduleAlarm()` links an alarm to the active schedule.
- [ ] **2.3** Ensure `AlarmReceiver` calls `evaluateAlarmAtTrigger()` and skips + logs on non-work days.
- [ ] **2.4** Add a simple "Days off" indicator to `item_alarm.xml` so users can see which days are skipped.
- [ ] **2.5** Write integration-style unit tests for the full skip path (mock repositories + manager).

---

### Phase 3 — City-Aware Alarms

> Goal: user can save named cities and set a different alarm time when they are in that city.

- [ ] **3.1** Add reverse-geocoding helper (`CityDetector`) using `Geocoder` API to resolve
      current lat/lng to a city name — pure-JVM testable with a mockable `Geocoder` interface.
- [ ] **3.2** Extend `SavedLocation` with an optional `cityName: String?` field (Room migration v3).
- [ ] **3.3** Update `LocationsFragment` / `EditLocationFragment` to display and save the resolved city name.
- [ ] **3.4** Update `SmartAlarmManager.evaluateAlarmAtTrigger()` to apply city-based time overrides
      (reuse the existing `AlarmLocationRule` time-override mechanism).
- [ ] **3.5** Update `EditAlarmFragment` to let users add a "When in city X, wake me at Y instead" rule.
- [ ] **3.6** Write unit tests for `CityDetector` and the city-override evaluation path.

---

### Phase 4 — Last-Workday Alarms

> Goal: user can create an alarm that fires only on the last workday of the week, month, or both.

- [ ] **4.1** Confirm `isLastWorkdayOfWeek()` and `isLastWorkdayOfMonth()` handle edge cases:
      month-end, public holidays falling on Friday, rotating-shift schedules.
- [ ] **4.2** Expose the `LAST_WORKDAY_OF_WEEK`, `LAST_WORKDAY_OF_MONTH`, and
      `LAST_WORKDAY_OF_WEEK_AND_MONTH` repeat options in `EditAlarmFragment`
      (currently defined in the model but not selectable in the UI).
- [ ] **4.3** Add a human-readable label to `item_alarm.xml` (e.g. "Last workday of month").
- [ ] **4.4** Write unit tests covering last-workday computation for all three modes across
      at least 3 consecutive months.

---

### Phase 5 — Settings Screen

> Goal: user can configure global preferences without editing code.

- [ ] **5.1** Create `SettingsFragment` backed by `SharedPreferences` (using AndroidX Preference library).
- [ ] **5.2** Expose: default snooze duration, max snooze count, vibration on/off,
      gradual volume ramp-up on/off, alarm dismiss timeout.
- [ ] **5.3** Wire `Constants` default values to read from `SharedPreferences` at runtime.
- [ ] **5.4** Add Settings entry to the bottom nav or action bar overflow menu.

---

### Phase 6 — Polish & Stabilisation

> Goal: app is stable, tested, and ready for daily use.

- [ ] **6.1** Add alarm history screen (read from `AlarmLog`) so users can see past events.
- [ ] **6.2** Handle GPS/location failures gracefully in `LocationRepository` (fallback + user message).
- [ ] **6.3** Fix `fallbackToDestructiveMigration()` — implement proper Room migrations for v2→v3.
- [ ] **6.4** Prune old `AlarmLog` entries automatically (call `pruneOldLogs()` on DB open or via WorkManager).
- [ ] **6.5** Final test pass: all unit tests green, no lint errors (`./gradlew check`).

---

### Definition of Done (per phase)

- All new classes/functions have at least one unit test.
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
