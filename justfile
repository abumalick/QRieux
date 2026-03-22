# Android

# Build debug APK
android-debug:
    ./gradlew :composeApp:assembleDebug

# Build release bundle (requires key.properties)
android-release:
    ./gradlew :composeApp:bundleRelease

# iOS

# Build iOS debug framework for simulator
ios-framework-debug:
    ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Build iOS release framework
ios-framework-release:
    ./gradlew :composeApp:linkReleaseFrameworkIosArm64

# Build iOS app (App Store)
ios-build:
    bundle exec fastlane ios build

# Build + install on connected iPhone
ios-run:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "Building for iOS device..."
    xcodebuild -scheme iosApp -project iosApp/iosApp.xcodeproj \
        -destination 'generic/platform=iOS' -allowProvisioningUpdates \
        -configuration Debug build 2>&1 | tail -3
    APP_PATH=$(xcodebuild -scheme iosApp -project iosApp/iosApp.xcodeproj \
        -destination 'generic/platform=iOS' -configuration Debug \
        -showBuildSettings 2>/dev/null \
        | grep -m1 "BUILT_PRODUCTS_DIR" | awk '{print $3}')
    DCTL_ID=$(xcrun devicectl list devices 2>&1 | grep iPhone | awk '{print $3}')
    if [ -z "$DCTL_ID" ]; then echo "No iPhone found"; exit 1; fi
    echo "Installing on iPhone..."
    xcrun devicectl device install app --device "$DCTL_ID" "$APP_PATH/iosApp.app"

# Build + upload to TestFlight
ios-beta:
    bundle exec fastlane ios beta

# Tests

# Run all unit tests
test:
    ./gradlew :composeApp:testDebugUnitTest

# Run a single test class
test-class class:
    ./gradlew :composeApp:testDebugUnitTest --tests "{{class}}"

# Fastlane - Android

# Test Play Store connection
android-validate:
    bundle exec fastlane android validate_credentials

# Push Android metadata + changelogs
android-metadata:
    bundle exec fastlane android upload_metadata

# Push Android screenshots
android-screenshots:
    bundle exec fastlane android upload_screenshots

# Upload AAB to production track
android-production:
    bundle exec fastlane android production

# Fastlane - iOS

# Push iOS metadata to App Store Connect
ios-metadata:
    bundle exec fastlane ios upload_metadata

# Screenshot generation

# Generate Android screenshots for all locales
gen-android-screenshots *args:
    ./scripts/generate_android_screenshots.sh {{args}}

# Generate iOS screenshots for all locales
gen-ios-screenshots *args:
    ./scripts/generate_ios_screenshots.sh {{args}}

# E2E tests

# Install E2E test dependencies
e2e-install:
    cd e2e && bun install

# Run E2E tests on Android (requires running emulator + debug APK)
e2e-android *args:
    cd e2e && bunx wdio run wdio.android.conf.ts {{args}}

# Run E2E tests on iOS (requires running simulator + built app)
e2e-ios *args:
    cd e2e && bunx wdio run wdio.ios.conf.ts {{args}}

# Build debug APK then run Android E2E tests
e2e-android-full *args: android-debug
    cd e2e && bunx wdio run wdio.android.conf.ts {{args}}

# Clean E2E artifacts and logs
e2e-clean:
    rm -rf e2e/artifacts e2e/logs

# Dependencies

# Install fastlane and ruby dependencies
bundle-install:
    bundle install
