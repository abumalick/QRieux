# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## App Focus

QR/barcode scanner designed for elderly and non-tech users. Priorities:
- **Simplicity** - minimal UI, obvious actions, large touch targets
- **No ads** - clean experience, no distractions
- **Security** - no tracking, minimal permissions, offline-capable

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug            # Build debug APK
./gradlew :composeApp:bundleRelease            # Build release bundle (requires key.properties)

# iOS
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # Build iOS debug framework
./gradlew :composeApp:linkReleaseFrameworkIosArm64          # Build iOS release framework

# Tests
./gradlew :composeApp:testDebugUnitTest        # Run all unit tests

# Run single test class
./gradlew :composeApp:testDebugUnitTest --tests "net.hilson.qr_scanner.qr_scanner.ComposeAppAndroidUnitTest"
```

Version:
- Android: `composeApp/build.gradle.kts` — update both `versionCode` and `versionName`
- iOS: `iosApp/iosApp/Info.plist` — update `CFBundleShortVersionString` and `CFBundleVersion`

## Architecture

QR scanner app using Kotlin Multiplatform + Compose Multiplatform. Supports Android and iOS.

### Source Sets

- **commonMain** - Shared code (QrContentType, Platform expect declarations)
- **androidMain** - Android implementation (CameraX, ML Kit, Android-specific UI)
- **iosMain** - iOS implementation (AVFoundation, Vision framework, iOS-specific UI)

### Key Components

**Common:**
- **util/QrContentType.kt** - Sealed class that parses QR content into URL/Email/Phone/Text types
- **Platform.kt** - Expect declarations for platform functions (vibrate, openUrl, share, etc.)

**Android (androidMain):**
- **App.kt** - Root composable; manages camera permission state and scan results
- **scanner/CameraPreview.kt** - CameraX preview with flash toggle and scan overlay UI
- **scanner/QrAnalyzer.kt** - ML Kit barcode analyzer (ImageAnalysis.Analyzer impl)
- **ui/ScanResultSheet.kt** - Displays scanned content with context-aware actions
- **ui/PermissionScreen.kt** - Camera permission request UI

**iOS (iosMain):**
- **App.ios.kt** - iOS root composable with AVFoundation camera permission handling
- **scanner/CameraPreview.ios.kt** - AVCaptureSession preview with Vision barcode scanning
- **scanner/GalleryScanner.ios.kt** - Vision framework barcode scanning for images
- **PhotoPicker.ios.kt** - PHPickerViewController for photo selection

### Brand Color

Primary: `#4A90D9` (blue) — defined in `ui/theme/Theme.kt`, used in app icon and UI.

### Tech Stack

**Shared:**
- Compose Multiplatform for UI
- Compose Material3 + Material Icons Extended

**Android:**
- CameraX for camera capture
- ML Kit Barcode Scanning for QR detection
- Accompanist Permissions for runtime permission handling

**iOS:**
- AVFoundation for camera capture
- Vision framework for barcode scanning (built-in, no extra dependencies)
- PhotosUI for photo picker

### Localization

Use `stringResource(R.string.xxx)` for all user-facing strings. Add translations to:
- `composeApp/src/androidMain/res/values/strings.xml` (English, default)
- `composeApp/src/androidMain/res/values-ar/strings.xml` (Arabic)
- `composeApp/src/androidMain/res/values-fr/strings.xml` (French)

## Play Store Metadata (Fastlane)

```bash
bundle install                            # install fastlane
bundle exec fastlane validate_credentials # test Play Store connection
bundle exec fastlane upload_metadata      # push metadata + changelogs
bundle exec fastlane upload_screenshots   # push images
```

Metadata files in `fastlane/metadata/android/{locale}/`:
- `title.txt` (max 30 chars)
- `short_description.txt` (max 80 chars)
- `full_description.txt` (max 4000 chars)
- `changelogs/{versionCode}.txt`
- `images/` - icon, featureGraphic, phoneScreenshots/, etc.

Locales: `en-US`, `fr-FR`, `ar`

## Claude Commands

### /release
Full release process: analyzes changes since last tag, creates changelogs (en/fr/ar), bumps version, builds bundle, uploads to Play Store alpha track, and creates git tag.
