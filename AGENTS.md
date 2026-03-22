# AGENTS.md

This file provides guidance to AI Agents working on this project

**⚠️ Public repo** — never commit secrets, API keys, or credentials to GitHub.

## App Focus

QR/barcode scanner designed for elderly and non-tech users. Priorities:

- **Simplicity** - minimal UI, obvious actions, large touch targets
- **No ads** - clean experience, no distractions
- **Security** - no tracking, minimal permissions, offline-capable

## Build Commands

```bash
just android-debug              # Build debug APK
just android-release            # Build release bundle (requires key.properties)
just ios-framework-debug        # Build iOS debug framework for simulator
just ios-framework-release      # Build iOS release framework
just ios-build                  # Build iOS app (App Store)
just ios-run                    # Build + install on connected iPhone
just ios-beta                   # Build + upload to TestFlight
just gen-android-screenshots    # Generate Android screenshots (--skip-build, --locale=xx)
just gen-ios-screenshots        # Generate iOS screenshots (--skip-build, --locale=xx)
just test                       # Run all unit tests
just test-class <fully.qualified.ClassName>  # Run single test class
just e2e-install                # Install E2E dependencies (bun)
just e2e-android                # Run E2E tests on Android (pass wdio args)
just e2e-ios                    # Run E2E tests on iOS (pass wdio args)
just e2e-android-full           # Build APK + run E2E tests
# Example: just e2e-android --spec specs/qr-generator.spec.ts
```

**Version bump** (must update both platforms):

- Android: `composeApp/build.gradle.kts` — `versionCode` and `versionName`
- iOS: `iosApp/iosApp/Info.plist` — `CFBundleShortVersionString` and `CFBundleVersion`

**Debug vs Release:**

- Debug build has applicationId suffix `.dev` and app name "QRieux Dev" (can install alongside release)

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

Use `stringResource(R.string.xxx)` for all user-facing strings. 20 languages supported.

Android strings: `composeApp/src/androidMain/res/values-{code}/strings.xml`
Codes: (default=en), `ar`, `bn`, `de`, `es`, `fr`, `hi`, `in` (Indonesian), `it`, `ja`, `ko`, `pt-rBR`, `ru`, `sw`, `ta`, `th`, `tr`, `ur`, `vi`, `zh-rCN`

### Manual Testing

Open `examples/test-qr-codes.md` to test all supported QR types (URLs, emails, phones, text).

### E2E Tests

Appium + WebdriverIO v9 (TypeScript) in `e2e/`. Requires running emulator/simulator + built app.

- Config: `e2e/wdio.shared.conf.ts` (shared), `wdio.android.conf.ts`, `wdio.ios.conf.ts`
- Specs: `e2e/specs/`
- On failure: screenshot, page source XML, device logs auto-saved to `e2e/artifacts/<timestamp>-<platform>/<test>/`
- Appium/wdio logs: `e2e/logs/`
- Env overrides: `E2E_APP_PATH`, `E2E_DEVICE_NAME`, `E2E_PLATFORM_VERSION`

## Fastlane

All fastlane commands run from **project root** (Fastfile is at `fastlane/Fastfile`).

```bash
just bundle-install             # install fastlane

# Android (Play Store)
just android-validate           # test Play Store connection
just android-metadata           # push metadata + changelogs
just android-screenshots        # push images
just android-production          # upload AAB to production track

# iOS (App Store Connect)
just ios-beta                   # build + upload to TestFlight
just ios-metadata               # push metadata to ASC
```

**Android metadata** in `fastlane/metadata/android/{locale}/`:

- `title.txt` (max 30 chars), `short_description.txt` (max 80 chars), `full_description.txt` (max 4000 chars)
- `changelogs/{versionCode}.txt`, `images/`

**iOS metadata** in `fastlane/metadata/ios/{locale}/`:

- `release_notes.txt`

Android locales: `en-US`, `zh-CN`, `hi-IN`, `es-ES`, `fr-FR`, `ar`, `bn-BD`, `pt-BR`, `ru-RU`, `ja-JP`, `id`, `de-DE`, `ur`, `tr-TR`, `ko-KR`, `vi`, `it-IT`, `th`, `ta-IN`, `sw`
iOS locales: `en-US`, `zh-Hans`, `hi`, `es-ES`, `fr-FR`, `ar-SA`, `pt-BR`, `ru`, `ja`, `id`, `de-DE`, `tr`, `ko`, `vi`, `it`, `th`

## Claude Commands

### /release

Full release process: analyzes changes since last tag, creates changelogs (all 20 locales), bumps version, builds bundle, uploads to Play Store production track, and creates git tag.
