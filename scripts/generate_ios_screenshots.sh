#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCREENSHOTS_DIR="$PROJECT_DIR/fastlane/screenshots/ios"
DERIVED_DATA="$PROJECT_DIR/build/ios-screenshots"
APP_BUNDLE="$DERIVED_DATA/Build/Products/Debug-iphonesimulator/iosApp.app"

IPHONE_TYPE="com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro-Max"
IPAD_TYPE="com.apple.CoreSimulator.SimDeviceType.iPad-Pro-13-inch-M5-16GB"
RUNTIME="com.apple.CoreSimulator.SimRuntime.iOS-26-2"
BUNDLE_ID="net.hilson.qrieux"
BACKGROUNDS_DIR="$SCRIPT_DIR/screenshot_backgrounds"

SKIP_BUILD=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
  esac
done

# --- Build ---
if [ "$SKIP_BUILD" = false ]; then
  echo "==> Building Kotlin framework for simulator..."
  cd "$PROJECT_DIR"
  ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

  echo "==> Building iOS app for simulator..."
  xcodebuild \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination "generic/platform=iOS Simulator" \
    -derivedDataPath "$DERIVED_DATA" \
    -allowProvisioningUpdates \
    -quiet
fi

if [ ! -d "$APP_BUNDLE" ]; then
  echo "ERROR: App bundle not found at $APP_BUNDLE"
  echo "Run without --skip-build first."
  exit 1
fi

# --- Screenshot content per locale ---
# Format: type|filename|en_content|fr_content|ar_content
SCREENSHOTS=(
  "scanner|1_scanner|__SCANNER__|__SCANNER__|__SCANNER__"
  "url|2_url_result|https://www.wikipedia.org|https://www.wikipedia.org|https://www.wikipedia.org"
  "wifi|3_wifi_result|WIFI:T:WPA;S:CoffeeShop;P:welcome2024;;|WIFI:T:WPA;S:Café Libre;P:bienvenue2024;;|WIFI:T:WPA;S:مقهى الضيافة;P:أهلا2024;;"
  "phone|4_phone_result|+1 (555) 123-4567|+33 6 12 34 56 78|+966 50 123 4567"
)

# Locale configs: lang|locale|dir
LOCALES=(
  "en|en_US|en-US"
  "fr|fr_FR|fr-FR"
  "ar|ar_SA|ar-SA"
)

get_content() {
  local entry="$1" locale_idx="$2"
  echo "$entry" | cut -d'|' -f$((3 + locale_idx))
}

warmup_simulator() {
  local udid="$1"
  xcrun simctl boot "$udid"
  xcrun simctl bootstatus "$udid" -b
  sleep 8
  xcrun simctl shutdown "$udid"
}

capture_screenshots_for_device() {
  local udid="$1" device_label="$2" prefix="$3" bg_file="$4"

  for locale_idx in 0 1 2; do
    IFS='|' read -r lang locale dir <<< "${LOCALES[$locale_idx]}"
    echo "[$device_label] Locale: $dir"

    mkdir -p "$SCREENSHOTS_DIR/$dir"

    # Set locale (requires shutdown+boot)
    xcrun simctl shutdown "$udid" 2>/dev/null || true
    xcrun simctl boot "$udid"
    xcrun simctl bootstatus "$udid" -b
    xcrun simctl spawn "$udid" defaults write "Apple Global Domain" AppleLanguages -array "$lang"
    xcrun simctl spawn "$udid" defaults write "Apple Global Domain" AppleLocale -string "$locale"
    xcrun simctl shutdown "$udid"
    xcrun simctl boot "$udid"
    xcrun simctl bootstatus "$udid" -b

    # Clean status bar
    xcrun simctl status_bar "$udid" override \
      --time "9:41" \
      --batteryState charged \
      --batteryLevel 100 \
      --cellularMode active \
      --cellularBars 4 \
      --wifiBars 3 \
      --operatorName "" 2>/dev/null || true

    xcrun simctl install "$udid" "$APP_BUNDLE"

    for shot_idx in "${!SCREENSHOTS[@]}"; do
      local entry="${SCREENSHOTS[$shot_idx]}"
      local filename
      filename="$(echo "$entry" | cut -d'|' -f2)"
      local content
      content="$(get_content "$entry" "$locale_idx")"

      echo "[$device_label]   -> ${prefix}${filename}.png"

      if [ -n "$bg_file" ] && [ -f "$bg_file" ]; then
        env "SIMCTL_CHILD_SCREENSHOT_CONTENT=$content" \
            "SIMCTL_CHILD_SCREENSHOT_BACKGROUND=$bg_file" \
            xcrun simctl launch "$udid" "$BUNDLE_ID"
      else
        env "SIMCTL_CHILD_SCREENSHOT_CONTENT=$content" \
            xcrun simctl launch "$udid" "$BUNDLE_ID"
      fi

      sleep 4

      xcrun simctl io "$udid" screenshot \
        "$SCREENSHOTS_DIR/$dir/${prefix}${filename}.png"

      xcrun simctl terminate "$udid" "$BUNDLE_ID" 2>/dev/null || true
      sleep 1
    done
  done
}

# --- Run ---
IPHONE_UDID=""
IPAD_UDID=""
cleanup() {
  echo "==> Cleaning up simulators..."
  [ -n "$IPHONE_UDID" ] && { xcrun simctl shutdown "$IPHONE_UDID" 2>/dev/null || true; xcrun simctl delete "$IPHONE_UDID" 2>/dev/null || true; }
  [ -n "$IPAD_UDID" ] && { xcrun simctl shutdown "$IPAD_UDID" 2>/dev/null || true; xcrun simctl delete "$IPAD_UDID" 2>/dev/null || true; }
}
trap cleanup EXIT

echo "==> Creating simulators..."
IPHONE_UDID=$(xcrun simctl create "QRieux-iPhone" "$IPHONE_TYPE" "$RUNTIME")
IPAD_UDID=$(xcrun simctl create "QRieux-iPad" "$IPAD_TYPE" "$RUNTIME")

IPHONE_BG="$BACKGROUNDS_DIR/phone_bg.png"
IPAD_BG="$BACKGROUNDS_DIR/tablet_bg.png"
[ -f "$IPHONE_BG" ] && echo "==> iPhone background: $IPHONE_BG" || { echo "==> No iPhone background (using black)"; IPHONE_BG=""; }
[ -f "$IPAD_BG" ] && echo "==> iPad background: $IPAD_BG" || { echo "==> No iPad background (using black)"; IPAD_BG=""; }

# Warmup both simulators to dismiss system notifications (Apple Intelligence etc.)
echo "==> Warming up simulators (dismissing system notifications)..."
warmup_simulator "$IPHONE_UDID" &
warmup_simulator "$IPAD_UDID" &
wait

# Run both devices concurrently
echo "==> Capturing screenshots (iPhone + iPad in parallel)..."
capture_screenshots_for_device "$IPHONE_UDID" "iPhone" "" "$IPHONE_BG" &
IPHONE_PID=$!
capture_screenshots_for_device "$IPAD_UDID" "iPad" "ipad_" "$IPAD_BG" &
IPAD_PID=$!

# Wait for both and propagate failures
FAILED=false
wait "$IPHONE_PID" || FAILED=true
wait "$IPAD_PID" || FAILED=true

if [ "$FAILED" = true ]; then
  echo "ERROR: One or more screenshot captures failed"
  exit 1
fi

echo ""
echo "==> Optimizing PNGs with pngquant..."
if command -v pngquant &>/dev/null; then
  find "$SCREENSHOTS_DIR" -name "*.png" -newer "$SCRIPT_DIR/generate_ios_screenshots.sh" \
    -exec pngquant --quality=65-80 --force --ext .png --skip-if-larger {} +
else
  echo "WARNING: pngquant not found, skipping optimization (brew install pngquant)"
fi

echo ""
echo "==> Done! Screenshots saved to: $SCREENSHOTS_DIR"
echo ""
find "$SCREENSHOTS_DIR" -name "*.png" -newer "$SCRIPT_DIR/generate_ios_screenshots.sh" | sort
