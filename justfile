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

# Build iOS app
ios-build:
    bundle exec fastlane ios build

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

# Upload AAB to alpha track
android-alpha:
    bundle exec fastlane android alpha

# Fastlane - iOS

# Push iOS metadata to App Store Connect
ios-metadata:
    bundle exec fastlane ios upload_metadata

# Dependencies

# Install fastlane and ruby dependencies
bundle-install:
    bundle install
