<h1 align="center">FewStep: AI-Powered Productivity Ecosystem</h1>

<p align="center">
  <em>Small Steps, Big Changes. A professional-grade Android application combining habit tracking, 24/7 background step counting, and AI-driven voice coaching into a single, seamless experience.</em>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-FFCA28?style=for-the-badge" />
</p>

---

## 🚀 Overview

**FewStep** is not just another habit tracker; it is a full-fledged productivity startup. Built with the latest industry standards, FewStep bridges the gap between physical health and mental discipline by utilizing hardware sensors and Artificial Intelligence. 

Whether you are trying to read 10 pages a day or walk 10,000 steps, FewStep acts as your personal coach in your pocket—even speaking to you out loud to keep you on track.

## ✨ Key Features

- **🚶 24/7 Background Step Tracking:** Uses the Android `SensorManager` API running on a robust `Foreground Service` to track steps continuously with minimal battery drain.
- **🎙️ AI Voice Reminders:** Integrates Android's Text-to-Speech (TTS) engine. The AI generates personalized, natural-sounding voice alerts for upcoming habits.
- **📊 Offline-First Architecture:** Utilizes **Room DB** as a local cache for offline use, syncing seamlessly with **Firebase Firestore** when the internet is restored.
- **🎮 Gamification & Dopamine Overlays:** Complete tasks to earn XP, level up from "Novice" to "Champion," and trigger visual "Streak Overlays" designed to boost user engagement.
- **🔒 Military-Grade Security:** User data is strictly isolated using Cloud Firestore Rules, and all sensitive API keys are hidden via `secrets.properties`.
- **🔄 Custom In-App Updates:** Bypasses standard app store limits with a proprietary GitHub-based update engine that downloads and installs APKs directly within the app.

## 🛠️ Technology Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin (100%) |
| **UI Framework** | Jetpack Compose (Material 3, Glassmorphism UI) |
| **Architecture** | MVVM (Model-View-ViewModel) with Repository Pattern |
| **Local Database** | Room Persistence Library |
| **Cloud & Auth** | Firebase Authentication, Cloud Firestore |
| **Sensors & Services** | `Sensor.TYPE_STEP_COUNTER`, AlarmManager, Foreground Services |
| **Monetization** | Google AdMob, Start.io Integration |

## 🏗️ Architecture Design

The app follows the **MVVM (Model-View-ViewModel)** architectural pattern to ensure a clean separation of concerns:
- **UI Layer:** Stateless Jetpack Compose functions that react to changes in `StateFlow`.
- **ViewModel Layer:** Manages UI state and handles business logic (e.g., `HomeViewModel`, `WalkViewModel`).
- **Data Layer:** The **Repository Pattern** acts as a single source of truth, mediating between the local `Room` database and the remote `Firebase` cloud.


## 🤝 Contact & Deployment
**Live App:** [Click here to download](https://21ambuj.github.io/FewStep-/)

**Developer:** Ambuj Kumar Maurya
- GitHub: [@21ambuj](https://github.com/21ambuj)
- LinkedIn: [@21ambuj](https://linkedin.com/in/21ambuj).

---
*Built with ❤️ for productivity.*
