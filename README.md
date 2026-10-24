💪 RepRise — Daily Push-Up Streak Tracker

<p align="center">
  <strong>One push-up more. One day stronger.</strong>
</p><p align="center">
  A native Android fitness habit tracker that helps you build consistency through a simple daily push-up challenge.
</p><p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange" />
  <img src="https://img.shields.io/badge/Storage-Room%20%7C%20DataStore-lightgrey" />
</p>---

🚀 About RepRise

RepRise is a daily push-up challenge application designed to help you develop a consistent exercise habit.

The concept is simple:

- Start with 10 push-ups.
- Complete your daily challenge.
- Increase your target by one push-up for every consecutive completed day.
- Track your progress, streaks, milestones, and workout history.

Instead of complicated workout plans, RepRise focuses on one small daily action that gradually builds consistency.

🎯 The Challenge

Day| Push-up Target
Day 1| 10
Day 2| 11
Day 3| 12
Day 4| 13
Day 5| 14
Day 6| 15
Day 7| 16

The goal: Build a sustainable daily habit, one repetition at a time.

---

✨ Features

🏠 Home Dashboard

A clean dashboard that displays:

- Current active streak.
- Longest streak.
- Total push-ups logged.
- Today's challenge.
- Daily progress indicator.
- Challenge completion status.
- Next day's target.
- Daily deadline countdown.

🔥 Daily Streak System

Track consecutive completed days and build your streak over time.

- Automatically calculate your current streak.
- Track your longest-ever streak.
- Increase the daily target after consecutive completed days.
- Recalculate progress when reopening the application.
- Preserve historical workout records.

📈 Progressive Push-Up Challenge

Start with a configurable initial target and gradually increase the difficulty.

Default progression:

"10 → 11 → 12 → 13 → 14 → ..."

Customize:

- Initial push-up target.
- Daily increment.
- Optional maximum target.
- Behaviour after a broken streak.

🔄 Broken Streak Recovery

If a full calendar day is missed:

- Your active streak resets.
- The default next target returns to 10 push-ups.
- Previous completed workouts remain saved.
- Your longest streak remains preserved.

An alternative setting allows resuming from the previous completed target.

📅 Workout History

Review your progress using:

- Monthly calendar.
- Completed-day indicators.
- Missed-day indicators.
- Pending challenge indicators.
- Selected-date workout details.
- Actual repetitions logged.
- Completion timestamps.
- Historical workout records.

📊 30-Day Performance

Visualize your recent workout activity through a daily performance chart.

Track:

- Actual push-ups performed.
- Daily challenge targets.
- Individual workout details.
- Recent training consistency.

🏆 Milestones

Celebrate progress through streak achievements.

Milestone examples:

- 7-day streak.
- 30-day streak.
- 100-day streak.

Achievements are based on your longest completed streak.

🔔 Daily Reminders

Stay consistent with local Android reminders.

- Configure your daily reminder time.
- Enable or disable reminders.
- Optionally configure a second reminder.
- Receive reminders when today's challenge remains incomplete.
- Handle notification permissions appropriately.

⚙️ Customization

Configure your challenge according to your preferences.

- Initial target.
- Daily increment.
- Optional target cap.
- Streak recovery behaviour.
- Reminder settings.
- Dark theme.
- Light theme.
- System theme.
- Haptic feedback.

🔒 Offline-First

RepRise is designed to work without requiring an online account.

- Local workout history.
- Local settings.
- Offline challenge calculations.
- Local notification scheduling.
- No required cloud backend.

Your workout history and preferences are stored locally on your device.

---

🛠️ Tech Stack

Technology| Purpose
Kotlin| Android application development
Jetpack Compose| Modern declarative UI
Material 3| UI components and theming
MVVM| Application architecture
ViewModel| UI state management
StateFlow| Reactive state updates
Room| Local workout database
DataStore| Persistent user preferences
AlarmManager| Local reminder scheduling
Android Notifications| Daily workout reminders
JUnit| Unit testing
Robolectric| Android-related testing

---

🏗️ Architecture

RepRise follows a separation-of-concerns architecture, keeping the UI, business logic, local storage, and Android notification functionality separate.

RepRise
│
├── UI
│   ├── Home
│   ├── History
│   ├── Settings
│   ├── Components
│   └── ViewModel
│       └── UI State Management
│
├── Domain
│   ├── Target Calculator
│   ├── Streak Calculator
│   └── Challenge Manager
│
├── Data
│   ├── Repository
│   ├── Room Database
│   └── DataStore
│
└── Notifications
    ├── Reminder Scheduler
    ├── Notification Receiver
    └── Boot Receiver

This architecture separates challenge calculations, local persistence, UI state management, and Android notification functionality.

📱 Screens

Home

Your daily challenge, progress, statistics, and streak information.

History

Your monthly workout calendar, historical records, and performance chart.

Settings

Your challenge configuration, reminders, appearance, and data management.

---

🧪 Testing

The project includes automated tests for important challenge behaviour.

Test areas include:

- Initial challenge target.
- Consecutive daily progression.
- Broken streak recovery.
- Longest streak preservation.
- Duplicate completion prevention.
- Actual repetition validation.
- Historical record persistence.
- Midnight rollover.
- Reminder scheduling.
- Settings persistence.
- Performance chart calculations.

The project has previously reported successful unit and Robolectric test runs during development.

Test results should be checked against the latest project build before considering a release.

---

📦 Installation

Option 1: Download an APK

If a release APK is available:

1. Download the APK from the repository's Releases section.
2. Transfer it to your Android phone.
3. Open the APK.
4. Allow installation from the relevant source if Android asks.
5. Install RepRise.

Option 2: Build from Source

Requirements:

- Android Studio.
- Android SDK.
- JDK 17 or the version required by the project.
- Gradle configuration compatible with the project.

Steps:

1. Clone the repository.

git clone https://github.com/codewithutkarsh12/DAILY-ONE-PUSHUP-INCREASE-.git

2. Open the project in Android Studio.

3. Allow Gradle to synchronize.

4. Connect an Android device or start an emulator.

5. Build and run the application.

Build Debug APK

From the project root:

./gradlew assembleDebug

On Windows:

gradlew.bat assembleDebug

The APK is normally generated at:

app/build/outputs/apk/debug/app-debug.apk

The exact build instructions may vary depending on the current project configuration.

---

🎨 Design Philosophy

RepRise focuses on a clean and motivating fitness experience.

Design principles:

- Minimal and distraction-free interface.
- Dark-first visual design.
- Clear daily challenge.
- Simple progress indicators.
- Consistent navigation.
- Smooth interactions.
- Accessible controls.
- Focus on daily consistency.

The objective is to make completing and tracking a daily push-up challenge simple.

---

🔮 Future Improvements

Potential future enhancements:

- Additional workout types.
- More achievement milestones.
- Improved workout analytics.
- Custom challenge schedules.
- Additional progress visualizations.
- Exportable workout reports.
- Optional backup and restore functionality.

Future features should preserve the application's simplicity and offline-first approach.

---

🤝 Contributing

Contributions, suggestions, and feedback are welcome.

If you want to improve RepRise:

1. Fork the repository.
2. Create a feature branch.
3. Implement your changes.
4. Test the implementation.
5. Submit a pull request.

Please keep changes focused, maintainable, and consistent with the existing architecture.

---

📌 Project Status

RepRise is an Android fitness habit-tracking project focused on daily push-up progression and streak tracking.

The application has undergone implementation and testing during development.

Build status, available APKs, and the latest features should be verified against the current repository version.

---

👨‍💻 Developer

Utkarsh

GitHub: "@codewithutkarsh12" (https://github.com/codewithutkarsh12)

Project: "DAILY-ONE-PUSHUP-INCREASE" (https://github.com/codewithutkarsh12/DAILY-ONE-PUSHUP-INCREASE-)

---

⭐ Support

If you find RepRise interesting, consider giving the repository a star.

Your feedback and suggestions can help improve the project.

One push-up more. One day stronger. 💪
