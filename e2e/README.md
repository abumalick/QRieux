# QRieux E2E Tests

Appium-based end-to-end tests for Android and iOS using WebdriverIO v9.

## Prerequisites

- **Node.js** >= 18
- **Android**: `ANDROID_HOME` set, emulator running or device connected, debug APK built (`just android-debug`)
- **iOS**: Xcode + command line tools, simulator running, simulator app built
- **Java**: JDK 11+ (required by Appium UiAutomator2 driver)

## Setup

```bash
just e2e-install
```

This runs `bun install` and installs Appium drivers (UiAutomator2 + XCUITest) automatically via postinstall.

## Running Tests

```bash
# Android (requires running emulator + built APK)
just e2e-android

# iOS (requires running simulator + built app)
just e2e-ios

# Build APK then run Android tests
just e2e-android-full
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `E2E_APP_PATH` | Override app/apk path | `/path/to/app.apk` or `/path/to/iosApp.app` |
| `E2E_DEVICE_NAME` | iOS simulator name | `iPhone 16 Pro` |
| `E2E_PLATFORM_VERSION` | iOS version | `18.5` |

## Failure Artifacts

When a test fails, the following are automatically captured:

| Artifact | Location | Description |
|----------|----------|-------------|
| Screenshot | `artifacts/<run>/<test>/screenshot.png` | Screen at moment of failure |
| Page source | `artifacts/<run>/<test>/page-source.xml` | Full UI hierarchy (XML) |
| Device logs | `artifacts/<run>/<test>/device.log` | Android logcat or iOS syslog |
| Video | `logs/` | Screen recording of failed test (via wdio-video-reporter) |
| Appium logs | `logs/appium.log` | Full Appium server log |
| wdio logs | `logs/` | WebdriverIO debug logs |

Run directories are timestamped: `artifacts/2026-03-22T10-30-00-android/test-name/`

## Limitations

- **Camera/QR scanning**: live camera can't be tested via Appium. Gallery scan uses pushed QR fixture images.
- **iOS gallery scan**: PHPicker + Vision on simulator doesn't reliably detect QR from picked images. Gallery scan tests run on Android only.
- **iOS real device**: requires signing config. Use simulator for now.
- **iOS app path**: auto-detected from `xcodebuild -showBuildSettings`. Override with `E2E_APP_PATH` if needed.
- **Video recording**: captures screenshots per command, can slightly slow tests. Only saved for failures.

## Project Structure

```
e2e/
├── wdio.shared.conf.ts      # Shared config (hooks, reporters, timeouts, appium service)
├── wdio.android.conf.ts     # Android capabilities
├── wdio.ios.conf.ts         # iOS capabilities (auto-detects app path)
├── helpers/
│   ├── screens.ts           # Platform dispatcher (routes to android/ios)
│   ├── screens.android.ts   # Android UiSelector helpers
│   ├── screens.ios.ts       # iOS XCUITest predicate helpers
│   ├── qr-fixtures.ts       # Push QR images to device/simulator
│   └── artifacts.ts         # Failure artifact capture
├── specs/
│   ├── app-launch.spec.ts        # App launch + onboarding (Android + iOS)
│   └── scan-from-gallery.spec.ts # Gallery scan flow (Android only)
├── fixtures/                # Pre-generated QR code PNGs
├── scripts/
│   └── generate-fixtures.ts # Regenerate QR fixture images
├── artifacts/               # Failure artifacts (gitignored)
└── logs/                    # Appium + wdio logs (gitignored)
```
