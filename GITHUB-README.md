# Stashspace Android App

A powerful, feature-rich bookmark manager for Android! 📱✨

## 📦 **What This App Does**

Stashspace helps you organize and manage your saved bookmarks from various platforms (Instagram, TikTok, YouTube, etc.) in one place. It's designed to work with the Stasht browser extension's JSON exports.

## 🚀 **Key Features**

### ✅ **Core Features**
- Import JSON bookmarks from Stasht browser extension
- Local SQLite database storage (no cloud)
- Search across captions, creators, platforms, URLs
- Filter by Videos, Photos, and custom collections
- Multiple sort options (newest, oldest, creator A-Z, most viewed)
- Grid and List view modes
- Dark theme with Material 3 design

### ✨ **NEW Features (v2.0)**
- ⭐ **Favorites System** - Mark items with a star
- 📦 **Multi-Select Mode** - Select multiple items for bulk actions
- 🧠 **Smart Collections** - Automatic categories (Recent, Untagged, etc.)
- #️⃣ **Hashtag Auto-Tagging** - Extract tags from hashtags in captions
- 👁️ **View Count Tracking** - See how many times you've viewed each item
- 📝 **Notes Field** - Add personal notes to bookmarks
- 📊 **Enhanced Export** - JSON with stats, CSV spreadsheet format
- 📥 **Media Download** - Download media to device for offline viewing
- 🔧 **Bug Fixes** - Fixed duplicate IDs, empty collections, video detection

## 📱 **Installation**

### Option 1: Automated Build (GitHub Actions)
1. Fork this repository
2. The workflow automatically builds the APK on every push!
3. Download the APK from the Actions artifacts

### Option 2: Local Build
1. Install [Android Studio](https://developer.android.com/studio)
2. Open the project: `File → Open → stashspace-android`
3. Wait for Gradle sync
4. Build: `Build → Build Bundle(s)/APK(s) → Build APK(s)`
5. Find APK: `app/build/outputs/apk/debug/app-debug.apk`

### Option 3: Use Pre-built APK
1. Go to **Releases** page
2. Download the latest APK
3. Install on your Android device

## 📥 **How to Import Your Bookmarks**

1. Use the Stasht browser extension to export your bookmarks as JSON
2. Open Stashspace on Android
3. Tap "Choose bookmark JSON"
4. Select your exported JSON file
5. Your bookmarks will be imported!

## 🧪 **Test Data**

Try the app with the included test data:
- File: `sample-stashspace-export.json`
- Contains 8 diverse bookmarks with videos, photos, tags, and collections

## 🛠️ **Build Requirements**

- **Android Studio** (recommended)
- **Android SDK 35** (API level 35)
- **Java JDK 17+**
- **Kotlin 2.0.21**
- **Gradle 8.9+**

## 📚 **Documentation**

- [IMPROVEMENTS.md](IMPROVEMENTS.md) - Complete list of improvements
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [BUILD-APK.md](BUILD-APK.md) - Build instructions

## 🤝 **Contributing**

Pull requests are welcome! Please read the contributing guidelines before submitting.

## 📄 **License**

This project is open source. Feel free to use, modify, and distribute.

## 🙏 **Acknowledgments**

Built with ❤️ for bookmark enthusiasts
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://developer.android.com/guide/themes/material)
- [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/mediasuite)
- [Coil](https://coil-kt.github.io/Coil/)

---

## 🎯 **What's New in v2.0**

### 🐛 Bug Fixes
- Fixed database ID collision issue
- Fixed empty collections display
- Improved video/photo detection

### ✨ New Features
- Favorites and multi-select
- Hashtag auto-tagging
- View count tracking
- Notes field
- Smart collections
- CSV export
- Enhanced JSON export with stats

See [CHANGELOG.md](CHANGELOG.md) for full details!

---

**Ready to organize your bookmarks? 🎉**<br>
[Download the latest APK]() ← Will be available after first build!

---

*Built for Android 8.0+ (API 26) | Target SDK: 35*