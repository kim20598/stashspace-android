# Stashspace for Android

A native Android companion for your Stasht saved-bookmark exports. This is a complete Android Studio project written in Kotlin and Jetpack Compose; it is not a WebView wrapper.

## Features

- Import a Stasht JSON export using Android's system file picker (no broad storage permission required).
- Local persistent library stored in an on-device SQLite database.
- Recognizes Stasht's `bookmarks` export and common alternate array names.
- Handles Instagram numeric `mediaType` values, including `2` for video and `8` for carousel posts, and media URL arrays.
- Search captions, creators, platforms, URLs, and collection names.
- Filter all saves, videos, photos, and named collections.
- Sort newest, oldest, or creator A–Z; switch between grid and list layouts.
- Open images and video playback in-app; open the original post in the installed browser/app.
- Export the normalized library back to JSON, and clear the local library.
- Dark, mobile-first interface with adaptive grid sizing.

## Open and build

1. Install **Android Studio** (current stable) and an Android SDK Platform 35.
2. Extract this ZIP.
3. In Android Studio choose **Open** and select the extracted `stashspace-android` folder (the folder containing `settings.gradle.kts`).
4. Allow Gradle sync to download the Android/Compose/Media3 dependencies.
5. Connect an Android phone with USB debugging enabled, or create an emulator (Android 8.0 / API 26 or newer).
6. Press **Run**. The application ID is `com.stashspace.app`.

You can also build from a terminal with Gradle 8.9+ and JDK 17:

```bash
gradle assembleDebug
```

The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Import your bookmarks

1. In the Stasht browser extension, use its bookmark export/download action to save the JSON file.
2. Open Stashspace on Android.
3. Tap **Choose bookmark JSON** and select that file in the Android file picker.
4. The imported library is stored locally and remains available after closing the app.

## Media playback notes

The exporter stores media URLs, not necessarily copies of the original media files. Instagram/TikTok CDN links can expire, require cookies or referrer context, be private, or block playback outside their apps. Stashspace attempts direct playback when a usable URL is present and always provides an **Open original** action. This is a source-platform restriction, not a guarantee that every saved clip can be streamed from a third-party app.

For dependable offline playback, the exporter must be extended to download the media files during export and include them in the export package. This Android build currently imports JSON URLs; it does not silently download or bypass platform access controls.

## Project details

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Jetpack Compose + Material 3
- Media3 ExoPlayer
- Coil image loading
- SQLite local persistence
- Minimum Android version: 8.0 (API 26)
- Target SDK: 35

No login, analytics SDK, ads, or cloud upload is included. Network access is used only to load remote bookmark media and original post links.
