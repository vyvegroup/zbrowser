# ZBrowser 🌐

A modern, fast, and beautiful Android browser built with Chromium WebView and Material Design 3.

![Android](https://img.shields.io/badge/Android-8.0%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)
![Material 3](https://img.shields.io/badge/Material%20Design-3-purple)
![License](https://img.shields.io/badge/License-MIT-yellow)

## ✨ Features

- 🚀 **Chromium WebView** - Fast browsing powered by the Chromium engine
- 🎨 **Material Design 3** - Full Material You with dynamic colors and dark mode
- 📑 **Multi-Tab Browsing** - Open multiple tabs with easy switching
- 🔒 **Incognito Mode** - Browse privately without saving history
- 🔖 **Bookmarks** - Save and organize your favorite sites
- 📜 **History** - Browse and search your browsing history
- 🔍 **Find in Page** - Search for text within web pages
- 📱 **Desktop Mode** - Toggle desktop site view
- ⬇️ **Download Manager** - Built-in file download support
- 🛡️ **Ad Blocker** - Basic ad blocking built-in
- 🔐 **Privacy Controls** - Do Not Track, cookie management, clear data
- ⚙️ **Customizable** - Search engine, theme, and advanced settings
- 🔄 **Swipe Refresh** - Pull to refresh pages
- 🔗 **Quick Links** - Fast access to popular sites
- 📤 **Share** - Share URLs to other apps

## 📱 Requirements

- Android 8.0 (API 26) or higher
- ~10MB storage space
- Internet connection

## 🏗️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI Framework | Material Design 3 |
| Browser Engine | Chromium WebView |
| Database | Room |
| Architecture | MVVM |
| Navigation | Navigation Component |
| Async | Kotlin Coroutines + Flow |
| DI | Manual DI |
| Build | Gradle 8.5 + AGP 8.2 |
| CI/CD | GitHub Actions |

## 📦 Build & Run

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Kotlin 1.9.22

### Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/zbrowser.git
   cd zbrowser
   ```

2. Open in Android Studio

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

### Release Build

For signed release builds, create a `keystore.properties` file in the project root:

```properties
storeFile=release.keystore
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Then build:
```bash
./gradlew assembleRelease
```

## 🔑 Signing Setup

### Generate a Keystore

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias zbrowser \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12
```

### Convert to Base64 (for GitHub Actions)

```bash
base64 -w 0 release.keystore > keystore_base64.txt
```

### GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

| Secret Name | Description |
|-------------|-------------|
| `KEYSTORE_BASE64` | Base64 encoded keystore file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias name |
| `KEY_PASSWORD` | Key password |

## 🔄 CI/CD with GitHub Actions

The project includes a complete GitHub Actions workflow that:

1. **Builds** the APK on every tag push (`v*`) or manual trigger
2. **Signs** with v1 + v2 + v3 signature schemes for maximum compatibility
3. **Creates** a GitHub Release with the signed APK and SHA-256 checksum

### Trigger a Release

```bash
# Tag and push
git tag v1.0.0
git push origin v1.0.0
```

Or use the GitHub Actions UI to manually trigger a build.

### APK Signature Schemes

The workflow signs APKs with all three signature schemes for maximum device compatibility:

| Scheme | Version | Compatibility | Purpose |
|--------|---------|--------------|---------|
| v1 | JAR signing | Android 1.0+ | Legacy compatibility |
| v2 | APK Signature Scheme v2 | Android 7.0+ | Full APK verification |
| v3 | APK Signature Scheme v3 | Android 9.0+ | Key rotation support |

## 📁 Project Structure

```
zbrowser/
├── .github/
│   └── workflows/
│       └── build-release.yml      # CI/CD pipeline
├── app/
│   ├── build.gradle.kts           # App-level Gradle config
│   ├── proguard-rules.pro         # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/zbrowser/app/
│       │   ├── ZBrowserApp.kt     # Application class
│       │   ├── MainActivity.kt    # Main activity
│       │   ├── data/              # Room database entities & DAOs
│       │   │   ├── Bookmark.kt
│       │   │   ├── HistoryEntry.kt
│       │   │   ├── BookmarkDao.kt
│       │   │   ├── HistoryDao.kt
│       │   │   └── AppDatabase.kt
│       │   ├── ui/                # UI layer (Fragments, ViewModels)
│       │   │   ├── browser/       # Main browser screen
│       │   │   ├── tabs/          # Tab management
│       │   │   ├── bookmarks/     # Bookmarks screen
│       │   │   ├── history/       # History screen
│       │   │   └── settings/      # Settings screen
│       │   └── web/               # WebView management
│       │       ├── TabWebView.kt
│       │       └── WebViewManager.kt
│       └── res/                   # Resources (layouts, drawables, etc.)
├── build.gradle.kts               # Root Gradle config
├── settings.gradle.kts            # Gradle settings
├── gradle.properties              # Gradle properties
└── README.md
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -am 'Add my feature'`
4. Push the branch: `git push origin feature/my-feature`
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
