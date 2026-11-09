# Quran Compose Multiplatform (Quran CMP)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.3-orange.svg?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS%20%7C%20Desktop-green.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Quran CMP** is a modern, cross-platform Quran reading and listening application built with **Compose Multiplatform** and **Kotlin Multiplatform (KMP)** for **Android**, **iOS**, and **Desktop** (macOS, Windows, Linux).

It is a faithful Compose Multiplatform reimplementation inspired by the acclaimed [quran_android](https://github.com/quran/quran_android) project, featuring 100% shared UI, business logic, offline-first SQLite databases, and native platform audio drivers.

---

## ✨ Features

### 📖 Multiple Mushaf Editions
- **Madani (1405H)**: Classic 604-page King Fahd Complex Mushaf edition.
- **Madani Tajweed**: Color-coded Tajweed rule annotations over the 604-page Madani layout.
- **Warsh (Nafiʻ)**: Riwayat Warsh ʻan Nafiʻ Mushaf edition.
- **Qaloon (Nafiʻ)**: Riwayat Qaloon ʻan Nafiʻ Mushaf edition.
- **IndoPak / Naskh**: 15-line Naskh script Mushaf (610 pages).
- Dynamic in-app mushaf package downloader with integrity checking and offline storage.

### 🎨 Reader Experience & Customization
- **High-Resolution Scanned Pages**: Crisp scanned Mushaf pages with natural right-to-left paging.
- **Interactive Ayah Highlighting**: Vector glyph and line bounding-box overlays mapped from `ayahinfo.db` with touch-to-select and spatial tolerance fallbacks.
- **Dual-Page Side-by-Side Mode**: Automatically optimizes reading on desktop screens, tablets, and wide landscape viewports.
- **Page Appearance & Themes**:
  - Classic Cream
  - Pure White
  - Warm Sepia
  - Soft Mint
  - Night Mode (high-contrast dark inversion)
- **Reader View Modes**: Full Mushaf page view, Translation list view, or Split Mushaf + Translation view.
- **Ayah Action Bar**: Tap any verse to:
  - Play recitation from that verse
  - Bookmark verse or assign custom tags
  - Copy Arabic text and active translations to clipboard
  - Share verse as formatted text or rendered image cards
  - View translation overlay side-by-side

### 🎧 Audio Recitation & Playback
- **Multiple Renowned Reciters**: Support for Mishary Al-Afasy, Abdul Basit (Murattal & Mujawwad), Mahmud Khalil Al-Husary, Abdur-Rahman As-Sudais, Sa'ud Ash-Shuraym, Muhammad Siddiq Al-Minshawi, Sa'ad Al-Ghamdi, Maher Al-Muaiqly, Abu Bakr Ash-Shatri, and more.
- **Playback Capabilities**:
  - Ayah-by-ayah streaming and automatic background caching
  - Gapless playback with sura timing synchronization databases
  - Configurable playback speed (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x)
  - Verse repeat and range looping for memorization (Hifz)
  - Audio playback bar with forward/backward navigation and quick reciter switcher

### 📚 Translations & Tafsir Manager
- In-app **Translation Manager** to browse, download, and manage translations from Quran.com / Quran Android databases.
- Multi-language coverage (English, Indonesian, Urdu, Turkish, French, German, Russian, Bengali, Spanish, Bosnian, Persian, etc.).
- Active translation switcher to view multiple translations concurrently alongside the Arabic text.

### 🔍 Search Engine
- **Arabic Text Search**: Diacritic-agnostic search engine with full Arabic normalization (tashkeel, hamza, and alef variations).
- **Translation Search**: Search across active translation databases with instant verse navigation.

### 🔖 Bookmarks & Custom Tags
- Save page bookmarks and specific ayah bookmarks.
- Organize bookmarks with customizable tags (e.g. *Memorize*, *Daily Wird*, *Reflections*).
- Quick resume from "Recent Pages" and persistent last-read position.

### 🌍 Internationalization
- Fully localized UI strings with support for over 50 languages and RTL interface layouts.

---

## 🛠️ Architecture & Tech Stack

| Category | Technology |
|---|---|
| **Language & Framework** | [Kotlin 2.1.0](https://kotlinlang.org/) Multiplatform (KMP), [Compose Multiplatform 1.7.3](https://www.jetbrains.com/lp/compose-multiplatform/) |
| **Project System** | [Kotlin Toolchain](https://github.com/JetBrains/amper) (v0.13.0, `project.yaml`, `module.yaml`) |
| **UI Components** | Jetpack / Compose Material 3 & Material Icons Extended |
| **Dependency Injection** | [Koin](https://insert-koin.io/) (`koin-core`, `koin-compose-viewmodel`) |
| **Navigation** | [Navigation Compose](https://developer.android.com/guide/navigation) (Type-safe routes) |
| **Database** | [androidx.sqlite:sqlite-bundled](https://developer.android.com/jetpack/androidx/releases/sqlite) (SQLite for ayah coordinates, translations, text, and bookmarks) |
| **Networking** | [Ktor Client 3.x](https://ktor.io/) (OkHttp engine on Android/JVM, Darwin engine on iOS) |
| **Image Loading & IO** | [Coil 3](https://coil-kt.github.io/coil/) & [Okio](https://square.github.io/okio/) |
| **Audio Playback** | JVM: [JLayer MP3](https://github.com/umjammer/jlayer) • Android: `MediaPlayer` • iOS: `AVPlayer` |

---

## 📂 Project Structure

```text
quran_cmp/
├── project.yaml                 # Kotlin Toolchain project configuration
├── libs.versions.toml           # Version catalog for dependencies
├── kotlin                       # Kotlin Toolchain CLI launcher (Unix)
├── kotlin.bat                   # Kotlin Toolchain CLI launcher (Windows)
├── shared/                      # Kotlin Multiplatform shared module
│   ├── module.yaml              # Shared module configuration (JVM, Android, iOS)
│   ├── composeResources/       # Multiplatform resources & localized strings (50+ languages)
│   ├── src/                     # Shared UI, ViewModels, Repositories, Database Handlers
│   │   ├── data/                # Data models, mushaf providers, page registries
│   │   ├── database/            # SQLite handlers (AyahInfo, Bookmarks, QuranText)
│   │   ├── repository/          # Audio playback controller, Quran repository, settings
│   │   ├── ui/                  # Compose UI screens (Home, Reader, Audio, Search, Settings)
│   │   └── platform/            # Multiplatform expect declarations
│   ├── src@android/             # Android-specific actual implementations
│   ├── src@jvm/                 # Desktop/JVM-specific actual implementations
│   ├── src@ios/                 # iOS-specific actual implementations
│   └── test/                    # Unit tests for core data structures & handlers
├── desktopApp/                  # Compose Desktop (JVM) runner
├── androidApp/                  # Android application runner (MainActivity & Manifest)
└── iosApp/                      # iOS application wrapper (SwiftUI & Xcode project)
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17** or newer installed and configured in your environment (`JAVA_HOME`).
- **Android SDK** (if running the Android target).
- **macOS with Xcode 15+** (if running the iOS target).

### Running the Applications

The project uses the bundled `./kotlin` ([Kotlin Toolchain](https://github.com/JetBrains/amper)) wrapper.

#### 🖥️ Desktop (Linux, macOS, Windows)
```bash
./kotlin run -m desktopApp
```
To enable hot reload during desktop development:
```bash
./kotlin run -m desktopApp --compose-hot-reload-mode
```

#### 📱 Android
Connect an Android device with USB debugging or start an Android Virtual Device (AVD), then execute:
```bash
./kotlin run -m androidApp
```

#### 🍏 iOS
Run on the iOS simulator using the command line:
```bash
./kotlin run -m iosApp -p iosSimulatorArm64
```
Alternatively, open `iosApp/module.xcodeproj` in **Xcode** and select your desired simulator or connected device.

#### 🧪 Running Tests
Execute the unit test suite across shared multiplatform targets:
```bash
./kotlin test
```

---

## 🙏 Acknowledgments & Credits

- [quran_android](https://github.com/quran/quran_android): The foundational open-source Quran project whose database schemas, ayah coordinate algorithms, and mushaf data make this project possible.
- [Quran.com](https://quran.com): For providing translations, recitations, and APIs that enrich Quranic software worldwide.
- [King Fahd Complex for the Printing of the Holy Quran](https://qurancomplex.gov.sa): For the holy mushaf scans and typographic resources.

---

## 📄 License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
