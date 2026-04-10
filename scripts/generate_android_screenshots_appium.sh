#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
E2E_DIR="$PROJECT_DIR/e2e"
METADATA_DIR="$PROJECT_DIR/fastlane/metadata/android"
APK_PATH="$PROJECT_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk"

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
ANDROID_AVD_HOME="$HOME/.android/avd"
export JAVA_HOME ANDROID_HOME ANDROID_AVD_HOME

AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
EMULATOR="$ANDROID_HOME/emulator/emulator"
ADB="$ANDROID_HOME/platform-tools/adb"

SYSTEM_IMAGE="system-images;android-35;google_apis;arm64-v8a"
PHONE_DEVICE="pixel_7"
TABLET_DEVICE="pixel_tablet"

PHONE_AVD="QRieux-Screenshots-Phone"
TABLET_AVD="QRieux-Screenshots-Tablet"
BACKGROUNDS_DIR="$SCRIPT_DIR/screenshot_backgrounds"

SKIP_BUILD=false
ONLY_LOCALE=""
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --locale=*) ONLY_LOCALE="${arg#--locale=}" ;;
  esac
done

# --- Preflight ---
for tool in "$AVDMANAGER" "$EMULATOR" "$ADB"; do
  if [ ! -f "$tool" ]; then
    echo "ERROR: $tool not found"
    exit 1
  fi
done

# --- Build ---
if [ "$SKIP_BUILD" = false ]; then
  echo "==> Building debug APK..."
  cd "$PROJECT_DIR"
  ./gradlew :composeApp:assembleDebug -q
fi

if [ ! -f "$APK_PATH" ]; then
  echo "ERROR: APK not found at $APK_PATH"
  echo "Run without --skip-build first."
  exit 1
fi

# Locale configs: lang|locale|fastlane_dir
LOCALES=(
  "en|US|en-US"
  "zh|CN|zh-CN"
  "hi|IN|hi-IN"
  "es|ES|es-ES"
  "fr|FR|fr-FR"
  "ar|SA|ar"
  "bn|BD|bn-BD"
  "pt|BR|pt-BR"
  "ru|RU|ru-RU"
  "ja|JP|ja-JP"
  "in|ID|id"
  "de|DE|de-DE"
  "ur|PK|ur"
  "tr|TR|tr-TR"
  "ko|KR|ko-KR"
  "vi|VN|vi"
  "it|IT|it-IT"
  "th|TH|th"
  "ta|IN|ta-IN"
  "sw|KE|sw"
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

# Device configs: avd_name|device_profile|screenshot_subdir|bg_image
DEVICES=(
  "$PHONE_AVD|$PHONE_DEVICE|phoneScreenshots|phone_bg.png"
  "$TABLET_AVD|$TABLET_DEVICE|tenInchScreenshots|tablet_bg.png"
)

create_avd() {
  local avd_name="$1" device_profile="$2"
  if "$AVDMANAGER" list avd -c 2>/dev/null | grep -q "^${avd_name}$"; then
    echo "  AVD $avd_name already exists"
    return
  fi
  echo "  Creating AVD: $avd_name ($device_profile)"
  echo "no" | "$AVDMANAGER" create avd -n "$avd_name" -k "$SYSTEM_IMAGE" -d "$device_profile" --force
}

start_emulator() {
  local avd_name="$1"
  echo "  Starting emulator: $avd_name"
  "$EMULATOR" -avd "$avd_name" -no-audio -no-boot-anim -no-snapshot-save -gpu host &
  EMULATOR_PID=$!
  echo "  Waiting for boot..."
  "$ADB" wait-for-device
  # Wait until boot animation finishes
  for i in $(seq 1 90); do
    if [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      echo "  Emulator booted (${i}s)"
      return
    fi
    sleep 1
  done
  echo "ERROR: Emulator did not boot in 90s"
  exit 1
}

stop_emulator() {
  "$ADB" emu kill 2>/dev/null || true
  wait "$EMULATOR_PID" 2>/dev/null || true
}

cleanup() {
  echo "==> Cleaning up..."
  stop_emulator 2>/dev/null || true
  for device_entry in "${DEVICES[@]}"; do
    IFS='|' read -r avd_name _ _ _ <<< "$device_entry"
    "$AVDMANAGER" delete avd -n "$avd_name" 2>/dev/null || true
  done
}
trap cleanup EXIT

TOTAL_LOCALES=${#LOCALES[@]}
echo "==> Generating screenshots for $TOTAL_LOCALES locales × ${#DEVICES[@]} devices"

cd "$E2E_DIR"

for device_entry in "${DEVICES[@]}"; do
  IFS='|' read -r avd_name device_profile screenshot_subdir bg_file <<< "$device_entry"
  bg_device_path="/data/local/tmp/screenshot_bg.png"
  echo ""
  echo "=== Device: $device_profile ($avd_name) ==="

  create_avd "$avd_name" "$device_profile"
  start_emulator "$avd_name"

  # Push background image to device
  "$ADB" push "$BACKGROUNDS_DIR/$bg_file" "$bg_device_path" 2>/dev/null || true

  locale_idx=0
  for locale_entry in "${LOCALES[@]}"; do
    IFS='|' read -r lang locale dir <<< "$locale_entry"
    locale_idx=$((locale_idx + 1))
    echo "--- [$locale_idx/$TOTAL_LOCALES] $dir ($device_profile) ---"

    output_dir="$METADATA_DIR/$dir/images/$screenshot_subdir"
    mkdir -p "$output_dir"

    E2E_APP_PATH="$APK_PATH" \
    SCREENSHOT_OUTPUT_DIR="$output_dir" \
    SCREENSHOT_PREFIX="" \
    SCREENSHOT_LANG="$lang" \
    SCREENSHOT_LOCALE="$locale" \
    SCREENSHOT_BACKGROUND="$bg_device_path" \
      npx wdio run wdio.screenshots.android.conf.ts 2>&1 | tail -20 || {
        echo "WARNING: Failed for $dir on $device_profile"
      }
  done

  stop_emulator
done

# --- Optimize PNGs ---
if command -v pngquant &>/dev/null; then
  echo ""
  echo "==> Optimizing PNGs..."
  find "$METADATA_DIR" -path "*/images/*Screenshots/*.png" -exec pngquant --force --quality=65-80 --ext .png {} \;
  echo "Done."
else
  echo "pngquant not found — skipping optimization"
fi

echo ""
echo "==> Screenshots saved to $METADATA_DIR"
