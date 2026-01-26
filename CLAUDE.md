# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Run tests
./gradlew :composeApp:testDebugUnitTest

# Clean and rebuild
./gradlew clean :composeApp:assembleDebug
```

## Architecture

Android QR scanner app using Kotlin Multiplatform + Compose Multiplatform. Currently Android-only.

### Key Components

- **App.kt** - Root composable; manages camera permission state and scan results
- **scanner/CameraPreview.kt** - CameraX preview with flash toggle and scan overlay UI
- **scanner/QrAnalyzer.kt** - ML Kit barcode analyzer (ImageAnalysis.Analyzer impl)
- **ui/ScanResultSheet.kt** - Displays scanned content with context-aware actions (open URL, call phone, send email, copy, share)
- **ui/PermissionScreen.kt** - Camera permission request UI
- **util/QrContentType.kt** - Sealed class that parses QR content into URL/Email/Phone/Text types
- **ui/theme/Theme.kt** - Material3 color scheme with brand primary color

### Brand Colors

- Primary: `#4A90D9` (blue) - used in app icon and UI theme
- Theme defined in `ui/theme/Theme.kt`

### Tech Stack

- CameraX for camera capture
- ML Kit Barcode Scanning for QR detection
- Accompanist Permissions for runtime permission handling
- Compose Material3 for UI

### Localization

Use `stringResource(R.string.xxx)` for all user-facing strings. Add translations to:
- `composeApp/src/androidMain/res/values/strings.xml` (English, default)
- `composeApp/src/androidMain/res/values-ar/strings.xml` (Arabic)
- `composeApp/src/androidMain/res/values-fr/strings.xml` (French)
