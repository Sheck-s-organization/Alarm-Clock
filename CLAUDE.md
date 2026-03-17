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
