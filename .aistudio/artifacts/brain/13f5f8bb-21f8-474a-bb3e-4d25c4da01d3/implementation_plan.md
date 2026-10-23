# RepRise – Push-up Streak Tracker

RepRise is a native, offline-first Android habit tracker that builds progressive upper-body strength by starting at 10 push-ups per day and adding +1 push-up for every consecutive day completed. It provides deterministic local streak tracking, midnight countdowns, customizable local reminders, an interactive completion history calendar, and local CSV backup without requiring any user accounts or internet connectivity.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and product choices were confirmed during Phase 1 clarification and govern this implementation plan:

- **Confirmed Decision 1 — Purely Offline On-Device Storage**: All daily completion history and user preferences will be stored 100% locally on the device using Room SQLite and Jetpack DataStore Preferences. No cloud databases, accounts, or network permissions will be required.
- **Confirmed Decision 2 — Focused Habit Tracking Without AI**: The app will remain strictly focused on daily push-up streak tracking, progressive targets, local reminders, and historical statistics without AI coaching or external web search calls.
- **Confirmed Decision 3 — Streak Break Target Reset**: When a full daily challenge period is missed, the active streak resets to 0 and the next daily target defaults to restarting at the base target (10 push-ups), while also retaining the user-configurable option in Settings to resume from the previous completed target if desired.

---

### 1. Overview & Core Concept

- **What It Does**: Tracks a daily self-reported push-up challenge tied to the device's local calendar date (`00:00:00` to `23:59:59`). Completing today's target locks in the day's record, increments the active streak by one, and increases tomorrow's target by +1 (e.g., 10, 11, 12, 13...). Missing a full day resets the active streak while preserving all past workout records and the all-time longest streak.
- **Target Audience / Persona**: Fitness-minded individuals and habit builders looking for a zero-friction, privacy-respecting daily push-up progression tracker that works reliably offline.
- **Key Value**: Eliminates guesswork and decision fatigue with a single daily number, a clear countdown to midnight, non-intrusive local reminders that only fire if today's workout is still incomplete, and tamper-resistant single-record-per-day persistence.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home Dashboard & Daily Check-In**: Users open the app to immediately see today's date, their active streak badge with a flame icon, a large circular progress ring framing today's push-up target, and a live countdown timer until midnight.
  2. **Completion & Confirmation Workflow**: Tapping **"Mark Today's Push-ups Done"** opens a confirmation modal where the user verifies self-reported completion and can optionally log actual reps performed (defaulting to today's target). Confirming updates the streak, totals, and progress ring in one atomic transaction, disables the completion button for the rest of the calendar day, and reveals a temporary 15-second **"Undo Today's Completion"** button to recover from accidental taps.
  3. **History & Calendar Inspection**: Switching to the **History** tab presents a 2x2 lifetime metrics summary (Active Streak, Longest Streak, Completed Days, Total Push-ups), an interactive month-by-month calendar grid color-coding Completed, Missed, and Pending Today dates, a day-detail inspector card when tapping any calendar cell, and a reverse-chronological log of completed workouts.
  4. **Settings, Reminders & Local Backup**: Switching to the **Settings** tab allows toggling the primary daily reminder (default 8:00 PM local time) and an optional second reminder before midnight, configuring progression parameters (Start Target, Daily Increment, Optional Target Cap, and Missed Streak Behavior), switching themes (Dark, Light, System), toggling completion haptics, exporting all history to a local CSV file via the system file picker, or clearing history behind a confirmation dialog.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Premium, high-contrast athletic utility with a dark-first posture (`AppThemeMode.DARK` by default, with full Light and System mode support).
  - *Color Palette & Mood*:
    - `--background`: Deep Charcoal (`#0F1216`) in Dark Mode / Crisp Alabaster (`#F7F9FC`) in Light Mode.
    - `--surface`: Elevated Slate (`#181C22`) / Pure White (`#FFFFFF`) with subtle surface variants (`#232931` / `#EDF1F7`) for layered metric cards.
    - `--primary`: Energetic Ember Orange (`#FF6B35` / `#E8551E`) for active streak flames, primary CTA buttons, and active progress rings.
    - `--secondary` / Status Accents: Emerald Green (`#2ECC71`) for completed states, Amber (`#F39C12`) for pending midnight countdowns, and Crimson (`#E74C3C`) for missed calendar days.
  - *Typography & Hierarchy*: Bundled Google Fonts (`Outfit` or `Space Grotesk` for bold athletic numeric displays paired with `Plus Jakarta Sans` or `Inter` for clean UI labels) scaled in `sp` so the daily target number commands immediate visual focus.
  - *Component Styling & Layout*: Material 3 `Scaffold` with edge-to-edge insets, rounded 20dp–24dp hero cards, 2x2 metric grids, and a 3-item bottom `NavigationBar` (`Home`, `History`, `Settings`) with 48dp+ accessible touch targets and `testTag` identifiers on all interactive elements.

- **Interactive Feedback & Motion**:
  - Smooth animated progress ring sweep (`animateFloatAsState`) upon marking today's challenge complete.
  - Tactile hardware haptic confirmation (`HapticFeedbackConstants.CONFIRM`) when logging reps (configurable in Settings).
  - Clear permission status banners when Android 13+ notification permissions are denied, with a one-tap shortcut to system notification settings.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Deterministic Domain Calculation Over Mutable Stored Counters**
  - *Chosen Approach*: Active streak, longest streak, and today's target are computed deterministically from the persisted list of daily records and current local date (`LocalDate`) inside pure domain calculators (`TargetCalculator`, `StreakCalculator`, `ChallengeManager`).
  - *Why*: If a user closes the app for several days or crosses midnight without opening the app, reopening it immediately recalculates the exact streak and target without relying on fragile background midnight jobs.
  - *Alternatives Considered*: Incrementing/resetting a stored integer counter in preferences at midnight, which fails if the phone is powered off or in deep sleep at `00:00`.

- **Decision 2: Database-Enforced Unique Local Date Constraint**
  - *Chosen Approach*: Store each daily challenge record keyed by an ISO-8601 `local_date` (`YYYY-MM-DD`) with a unique SQLite index and a `@Transaction` check in the DAO before marking complete.
  - *Why*: Guarantees idempotency so rapid repeated taps, timezone shifts, or UI race conditions can never insert duplicate completions for the same calendar day or inflate the streak.
  - *Alternatives Considered*: Filtering duplicates only in the UI layer, which risks database corruption on rapid taps.

- **Decision 3: Standard Android `AlarmManager` Inexact Window / Broadcast Scheduling for Reminders**
  - *Chosen Approach*: Schedule daily primary and optional secondary local reminders using native `AlarmManager` (`setWindow` / `setAndAllowWhileIdle` fallback without requiring restricted `SCHEDULE_EXACT_ALARM` permissions) paired with `NotificationReceiver` and `BootReceiver` (`BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`).
  - *Why*: Respects Google Play policy and Android battery restrictions by avoiding special exact-alarm permissions while ensuring reminders survive app restarts and device reboots, never duplicate when rescheduled, and only fire if the database confirms today's challenge is still incomplete.

- **Decision 4: Clean-Room Synthesis of Reference Repository Patterns**
  - *Chosen Approach*: Incorporate verified architectural lessons—transactional Room persistence, `RECEIVE_BOOT_COMPLETED` + `POST_NOTIFICATIONS` reminder recovery, and SAF CSV export (observed in `wbpxre150/android-shoppinglist`, MIT License), alongside reactive `StateFlow` + Material 3 Compose dashboard patterns—via clean-room native Kotlin code tailored specifically to RepRise.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```text
┌───────────────────────────────────────────────────────────────────────────┐
│                         Jetpack Compose UI Layer                          │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │     HomeScreen      │  │    HistoryScreen    │  │  SettingsScreen   │  │
│  │ • Target Ring & CTA │  │ • Month Calendar    │  │ • Reminder Pickers│  │
│  │ • Midnight Countdown│  │ • Day Detail Card   │  │ • Progression Form│  │
│  │ • Confirm & Undo UI │  │ • Completed Log     │  │ • CSV & Clear Data│  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
              ▼                        ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    RepRiseViewModel (StateFlow<UiState>)                  │
│          • Combines History Flow + Settings Flow + 1s Time Ticker         │
│          • Coordinates Completion, 15s Undo Window, & CSV Export          │
└──────────────────────┬───────────────────────────────────────┬────────────┘
                       │                                       │
                       ▼                                       ▼
┌─────────────────────────────────────────────┐ ┌───────────────────────────┐
│         Pure Domain Calculators             │ │   Platform Notifications  │
│  ┌───────────────────────────────────────┐  │ │  ┌─────────────────────┐  │
│  │          ChallengeManager             │  │ │  │  ReminderScheduler  │  │
│  │ • Midnight countdown & day statuses   │  │ │  │ • Cancels & replaces│  │
│  ├───────────────────┬───────────────────┤  │ │  │   pending alarms    │  │
│  │ TargetCalculator  │ StreakCalculator  │  │ │  ├─────────────────────┤  │
│  │ • Start target 10 │ • Active streak   │  │ │  │ NotificationReceiver│  │
│  │ • +Increment & Cap│ • Longest streak  │  │ │  │ & BootReceiver      │  │
│  │ • Missed behavior │ • Total reps/days │  │ │  │ • Checks DB before  │  │
│  └───────────────────┴───────────────────┘  │ │  │   posting alert     │  │
└──────────────────────┬──────────────────────┘ └──────────────┬────────────┘
                       │                                       │
                       ▼                                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      PushUpRepository (Single Source)                     │
├──────────────────────────────────────┬────────────────────────────────────┤
│       Room Database (SQLite)         │     Jetpack DataStore Preferences  │
│  • PushUpDayEntity (unique date key) │  • UserSettingsStore               │
│  • PushUpDayDao (@Transaction upsert)│  • Target rules, times, theme, etc.│
└──────────────────────────────────────┴────────────────────────────────────┘
```

- **Data Model & State**:
  - **`PushUpDayEntity`**:
    - `id: Long` (auto-generated primary key)
    - `localDate: String` (ISO-8601 `YYYY-MM-DD`, enforced with `unique = true` index)
    - `targetReps: Int` (locked-in target for that date so future setting edits never rewrite past targets)
    - `isCompleted: Boolean`
    - `completedAtEpochMillis: Long?` (nullable until completed)
    - `actualReps: Int?` (optional user-entered rep count; defaults to `targetReps` when computing `effectiveLoggedReps`)
  - **`UserSettings` (DataStore)**:
    - `startTarget: Int = 10`, `dailyIncrement: Int = 1`, `targetCapEnabled: Boolean = false`, `targetCap: Int = 100`
    - `missedStreakBehavior: MissedStreakBehavior = RESTART_AT_START_TARGET` (or `RESUME_FROM_PREVIOUS_TARGET`)
    - `reminderEnabled: Boolean = true`, `reminderHour: Int = 20`, `reminderMinute: Int = 0`
    - `secondReminderEnabled: Boolean = false`, `secondReminderHour: Int = 22`, `secondReminderMinute: Int = 30`
    - `themeMode: AppThemeMode = DARK`, `hapticFeedbackEnabled: Boolean = true`

- **Interactive Component & State Mapping**:
  - **Mark Today's Push-ups Done (`HomeScreen` -> `CompleteChallengeDialog` -> `RepRiseViewModel.markTodayComplete`)**: Validates optional numeric input (`>= todayTarget`, `<= 10,000`), executes `dao.markDayCompletedTransactional(...)`, cancels any active reminder notification for today, enables the 15-second Undo button (`canUndoTodayCompletion = true`), and immediately emits the updated `DailyChallengeSnapshot`.
  - **Undo Completion (`HomeScreen` -> `RepRiseViewModel.undoTodayCompletion`)**: Deletes only today's completion record if tapped within the active undo window, restores the pending state, and reschedules today's reminder if applicable.
  - **Calendar Month Navigation & Day Inspector (`HistoryScreen`)**: Allows stepping backward/forward across `YearMonth` values, jumping back to `"Today"`, and tapping any date cell to inspect its target, logged reps, completion timestamp, or missed status.
  - **Settings Validation & Rescheduling (`SettingsScreen` -> `RepRiseViewModel`)**: Validates numeric bounds (`startTarget` 1..1000, `dailyIncrement` 1..100, `targetCap >= startTarget`), persists to DataStore, and triggers `ReminderScheduler.syncReminders(...)` using `PendingIntent.FLAG_UPDATE_CURRENT` so changing reminder times overwrites existing schedules without duplicate alerts.

- **Verification & Unit Test Coverage (Cases A–L)**:
  - Automated unit and Robolectric tests (`gradle :app:testDebugUnitTest`) will verify:
    - **A–C**: Initial target of 10, Day 2 target of 11, Day 3 target of 12.
    - **D–E**: Missed full day breaks active streak to 0 while preserving longest streak; next target resets to 10 by default.
    - **F**: Repeated completion taps on the same date are rejected transactionally and never duplicate records or inflate streaks.
    - **G–H**: App restart preserves today's completion and history; reopening after multiple missed days recalculates streak and target accurately.
    - **I**: Crossing midnight rolls over the active date and does not carry an incomplete day into the new date.
    - **J–K**: Changing reminder time replaces scheduled alarms without duplicates; denied notification permission degrades gracefully without crashing.
    - **L**: Custom start targets, increments, target caps, and resume-from-previous-target settings compute accurately.
