#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
E2E_DIR="$PROJECT_DIR/e2e"
SCREENSHOTS_DIR="$PROJECT_DIR/fastlane/screenshots/ios"
DERIVED_DATA="$PROJECT_DIR/build/ios-screenshots"
APP_BUNDLE="$DERIVED_DATA/Build/Products/Debug-iphonesimulator/iosApp.app"
BACKGROUNDS_DIR="$SCRIPT_DIR/screenshot_backgrounds"

SKIP_BUILD=false
ONLY_LOCALE=""
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --locale=*) ONLY_LOCALE="${arg#--locale=}" ;;
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

# Locale configs: lang|locale|fastlane_dir
LOCALES=(
  "en|en_US|en-US"
  "zh-Hans|zh_CN|zh-Hans"
  "hi|hi_IN|hi"
  "es|es_ES|es-ES"
  "fr|fr_FR|fr-FR"
  "ar|ar_SA|ar-SA"
  "pt-BR|pt_BR|pt-BR"
  "ru|ru_RU|ru"
  "ja|ja_JP|ja"
  "id|id_ID|id"
  "de|de_DE|de-DE"
  "tr|tr_TR|tr"
  "ko|ko_KR|ko"
  "vi|vi_VN|vi"
  "it|it_IT|it"
  "th|th_TH|th"
)

if [ -n "$ONLY_LOCALE" ]; then
  FILTERED=()
  for entry in "${LOCALES[@]}"; do
    dir="$(echo "$entry" | cut -d'|' -f3)"
    if [ "$dir" = "$ONLY_LOCALE" ]; then
      FILTERED+=("$entry")
    fi
  done
  if [ ${#FILTERED[@]} -eq 0 ]; then
    echo "ERROR: Locale '$ONLY_LOCALE' not found. Available:"
    for entry in "${LOCALES[@]}"; do echo "  $(echo "$entry" | cut -d'|' -f3)"; done
    exit 1
  fi
  LOCALES=("${FILTERED[@]}")
  echo "==> Running for locale: $ONLY_LOCALE only"
fi

# Device configs: device_name|prefix|bg_image
DEVICES=(
  "iPhone 16 Pro Max||phone_bg.png"
  "iPad Pro 13-inch (M5)|ipad_|tablet_bg.png"
)

TOTAL_LOCALES=${#LOCALES[@]}
TOTAL_DEVICES=${#DEVICES[@]}
echo "==> Generating screenshots for $TOTAL_LOCALES locales × $TOTAL_DEVICES devices"

# Clean old screenshots
echo "==> Cleaning old screenshots..."
for locale_entry in "${LOCALES[@]}"; do
  dir="$(echo "$locale_entry" | cut -d'|' -f3)"
  rm -f "$SCREENSHOTS_DIR/$dir"/*.png
done

cd "$E2E_DIR"

for device_entry in "${DEVICES[@]}"; do
  IFS='|' read -r device_name prefix bg_file <<< "$device_entry"
  bg_path="$BACKGROUNDS_DIR/$bg_file"
  sim_label="${prefix:-phone}"
  sim_label="${sim_label%_}" # strip trailing underscore
  echo ""
  echo "=== Device: $device_name ==="

  # Pre-create simulator to reuse across all locales (avoids create+boot per run)
  SIM_UDID=$(xcrun simctl create "appiumScreenshots-${sim_label}" "$device_name" 2>/dev/null | tail -1) || {
    echo "ERROR: Failed to create simulator for $device_name"; continue
  }
  xcrun simctl boot "$SIM_UDID" 2>/dev/null
  echo "Simulator: $SIM_UDID"

  locale_idx=0
  for locale_entry in "${LOCALES[@]}"; do
    IFS='|' read -r lang locale dir <<< "$locale_entry"
    locale_idx=$((locale_idx + 1))
    echo "--- [$locale_idx/$TOTAL_LOCALES] $dir ($device_name) ---"

    output_dir="$SCREENSHOTS_DIR/$dir"
    mkdir -p "$output_dir"

    E2E_APP_PATH="$APP_BUNDLE" \
    SCREENSHOT_OUTPUT_DIR="$output_dir" \
    SCREENSHOT_PREFIX="$prefix" \
    SCREENSHOT_LANG="$lang" \
    SCREENSHOT_LOCALE="$locale" \
    SCREENSHOT_DEVICE="$device_name" \
    SCREENSHOT_BACKGROUND="$bg_path" \
    SCREENSHOT_UDID="$SIM_UDID" \
      npx wdio run wdio.screenshots.ios.conf.ts 2>&1 | tail -20 || {
        echo "WARNING: Failed for $dir on $device_name"
      }
  done

  # Cleanup simulator
  xcrun simctl shutdown "$SIM_UDID" 2>/dev/null
  xcrun simctl delete "$SIM_UDID" 2>/dev/null
  echo "Cleaned up simulator: $sim_label"
done

# --- Optimize PNGs ---
if command -v pngquant &>/dev/null; then
  echo ""
  echo "==> Optimizing PNGs..."
  find "$SCREENSHOTS_DIR" -name '*.png' -exec pngquant --force --quality=65-80 --ext .png {} \;
  echo "Done."
else
  echo "pngquant not found — skipping optimization"
fi

echo ""
echo "==> Screenshots saved to $SCREENSHOTS_DIR"
