#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKGROUNDS_DIR="$SCRIPT_DIR/screenshot_backgrounds"

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
# Ensure avdmanager and emulator use the same AVD directory
ANDROID_AVD_HOME="$HOME/.android/avd"
export JAVA_HOME ANDROID_HOME ANDROID_AVD_HOME

AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
EMULATOR="$ANDROID_HOME/emulator/emulator"
ADB="$ANDROID_HOME/platform-tools/adb"

BUNDLE_ID="net.hilson.qrieux.dev"
APK_PATH="$PROJECT_DIR/composeApp/build/outputs/apk/debug/composeApp-debug.apk"

SYSTEM_IMAGE="system-images;android-35;google_apis;arm64-v8a"
PHONE_DEVICE="pixel_7"
TABLET_DEVICE="pixel_tablet"

PHONE_AVD="QRieux-Phone"
TABLET_AVD="QRieux-Tablet"
PHONE_PORT=5554
TABLET_PORT=5556

SKIP_BUILD=false
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
  esac
done

# --- Preflight checks ---
for tool in "$AVDMANAGER" "$EMULATOR" "$ADB"; do
  if [ ! -f "$tool" ]; then
    echo "ERROR: $tool not found"
    echo "Install Android SDK cmdline-tools: Android Studio > SDK Manager > SDK Tools > Android SDK Command-line Tools"
    exit 1
  fi
done

if ! "$SDKMANAGER" --list_installed 2>/dev/null | grep -q "$(echo "$SYSTEM_IMAGE" | tr ';' '|')" && \
   [ ! -d "$ANDROID_HOME/system-images/android-35/google_apis/arm64-v8a" ]; then
  echo "ERROR: System image $SYSTEM_IMAGE not installed"
  echo "Install via: $SDKMANAGER \"$SYSTEM_IMAGE\""
  exit 1
fi

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

# --- Screenshot content per locale ---
# Format: type|filename|en_content|fr_content|ar_content
SCREENSHOTS=(
  "scanner|1_scanner|__SCANNER__|__SCANNER__|__SCANNER__"
  "url|2_url_result|https://www.wikipedia.org|https://www.wikipedia.org|https://www.wikipedia.org"
  "wifi|3_wifi_result|WIFI:T:WPA;S:CoffeeShop;P:welcome2024;;|WIFI:T:WPA;S:Café Libre;P:bienvenue2024;;|WIFI:T:WPA;S:مقهى الضيافة;P:أهلا2024;;"
  "phone|4_phone_result|+1 (555) 123-4567|+33 6 12 34 56 78|+966 50 123 4567"
)

# Locale configs: lang_tag|fastlane_dir
LOCALES=(
  "en-US|en-US"
  "fr-FR|fr-FR"
  "ar-SA|ar"
)

get_content() {
  local entry="$1" locale_idx="$2"
  echo "$entry" | cut -d'|' -f$((3 + locale_idx))
}

wait_for_boot() {
  local serial="$1"
  echo "    Waiting for $serial to boot..."
  "$ADB" -s "$serial" wait-for-device
  local attempts=0
  while [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
    sleep 2
    attempts=$((attempts + 1))
    if [ "$attempts" -gt 90 ]; then
      echo "ERROR: Emulator $serial failed to boot after 3 minutes"
      exit 1
    fi
  done
  sleep 3
}

enable_demo_mode() {
  local serial="$1"
  "$ADB" -s "$serial" shell settings put global sysui_demo_allowed 1
  "$ADB" -s "$serial" shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
  "$ADB" -s "$serial" shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
  "$ADB" -s "$serial" shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
  "$ADB" -s "$serial" shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
  "$ADB" -s "$serial" shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 -e datatype none >/dev/null
  "$ADB" -s "$serial" shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
}

set_locale() {
  local serial="$1" locale="$2"
  "$ADB" -s "$serial" root >/dev/null 2>&1
  sleep 2
  "$ADB" -s "$serial" shell "setprop persist.sys.locale $locale; setprop ctl.restart zygote"
  sleep 10
  wait_for_boot "$serial"
}

capture_screenshots_for_device() {
  local avd_name="$1" serial="$2" device_label="$3" screenshot_subdir="$4" bg_file="$5"

  for locale_idx in 0 1 2; do
    IFS='|' read -r lang_tag fastlane_dir <<< "${LOCALES[$locale_idx]}"
    echo "[$device_label] Locale: $fastlane_dir"

    local output_dir="$PROJECT_DIR/fastlane/metadata/android/$fastlane_dir/images/$screenshot_subdir"
    mkdir -p "$output_dir"

    # Set locale and wait for restart
    set_locale "$serial" "$lang_tag"

    # Push background image to a world-readable path (scoped storage blocks /sdcard/)
    local bg_device_path=""
    if [ -n "$bg_file" ] && [ -f "$bg_file" ]; then
      bg_device_path="/data/local/tmp/screenshot_bg.png"
      "$ADB" -s "$serial" push "$bg_file" "$bg_device_path" >/dev/null
      "$ADB" -s "$serial" shell chmod 644 "$bg_device_path"
    fi

    # Install APK (reinstall to pick up any changes)
    "$ADB" -s "$serial" install -r -g "$APK_PATH" >/dev/null 2>&1

    # Disable animations for instant rendering
    "$ADB" -s "$serial" shell settings put global window_animation_scale 0
    "$ADB" -s "$serial" shell settings put global transition_animation_scale 0
    "$ADB" -s "$serial" shell settings put global animator_duration_scale 0

    enable_demo_mode "$serial"

    for entry in "${SCREENSHOTS[@]}"; do
      local filename
      filename="$(echo "$entry" | cut -d'|' -f2)"
      local content
      content="$(get_content "$entry" "$locale_idx")"
      local output_path="$output_dir/${filename}.png"

      echo "[$device_label]   -> $screenshot_subdir/${filename}.png"

      # Build am start command with extras
      local am_cmd="am start -W -n '$BUNDLE_ID/net.hilson.qrieux.MainActivity' --es SCREENSHOT_CONTENT '$content'"
      if [ -n "$bg_device_path" ]; then
        am_cmd="$am_cmd --es SCREENSHOT_BACKGROUND '$bg_device_path'"
      fi

      "$ADB" -s "$serial" shell "$am_cmd" >/dev/null

      sleep 6

      "$ADB" -s "$serial" exec-out screencap -p > "$output_path"

      "$ADB" -s "$serial" shell am force-stop "$BUNDLE_ID" >/dev/null
      sleep 1
    done
  done
}

# --- Cleanup ---
PHONE_PID=""
TABLET_PID=""
cleanup() {
  echo "==> Cleaning up..."
  [ -n "$PHONE_PID" ] && kill "$PHONE_PID" 2>/dev/null && wait "$PHONE_PID" 2>/dev/null || true
  [ -n "$TABLET_PID" ] && kill "$TABLET_PID" 2>/dev/null && wait "$TABLET_PID" 2>/dev/null || true
  "$AVDMANAGER" delete avd -n "$PHONE_AVD" 2>/dev/null || true
  "$AVDMANAGER" delete avd -n "$TABLET_AVD" 2>/dev/null || true
}
trap cleanup EXIT

# --- Create AVDs ---
echo "==> Creating emulators..."
echo no | "$AVDMANAGER" create avd --name "$PHONE_AVD" --package "$SYSTEM_IMAGE" --device "$PHONE_DEVICE" --force >/dev/null 2>&1
echo no | "$AVDMANAGER" create avd --name "$TABLET_AVD" --package "$SYSTEM_IMAGE" --device "$TABLET_DEVICE" --force >/dev/null 2>&1

# --- Background images ---
PHONE_BG="$BACKGROUNDS_DIR/phone_bg.png"
TABLET_BG="$BACKGROUNDS_DIR/tablet_bg.png"
[ -f "$PHONE_BG" ] && echo "==> Phone background: $PHONE_BG" || { echo "==> No phone background (using black)"; PHONE_BG=""; }
[ -f "$TABLET_BG" ] && echo "==> Tablet background: $TABLET_BG" || { echo "==> No tablet background (using black)"; TABLET_BG=""; }

# --- Start emulators ---
echo "==> Starting emulators..."
"$EMULATOR" -avd "$PHONE_AVD" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -port "$PHONE_PORT" -no-snapshot-load &
PHONE_PID=$!
"$EMULATOR" -avd "$TABLET_AVD" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -port "$TABLET_PORT" -no-snapshot-load &
TABLET_PID=$!

PHONE_SERIAL="emulator-$PHONE_PORT"
TABLET_SERIAL="emulator-$TABLET_PORT"

wait_for_boot "$PHONE_SERIAL"
wait_for_boot "$TABLET_SERIAL"

# --- Capture ---
echo "==> Capturing screenshots (phone + tablet in parallel)..."
capture_screenshots_for_device "$PHONE_AVD" "$PHONE_SERIAL" "Phone" "phoneScreenshots" "$PHONE_BG" &
CAPTURE_PHONE_PID=$!
capture_screenshots_for_device "$TABLET_AVD" "$TABLET_SERIAL" "Tablet" "tenInchScreenshots" "$TABLET_BG" &
CAPTURE_TABLET_PID=$!

FAILED=false
wait "$CAPTURE_PHONE_PID" || FAILED=true
wait "$CAPTURE_TABLET_PID" || FAILED=true

if [ "$FAILED" = true ]; then
  echo "ERROR: One or more screenshot captures failed"
  exit 1
fi

echo ""
echo "==> Optimizing PNGs with pngquant..."
if command -v pngquant &>/dev/null; then
  find "$PROJECT_DIR/fastlane/metadata/android" -name "*.png" -path "*/images/*" -newer "$SCRIPT_DIR/generate_android_screenshots.sh" \
    -exec pngquant --quality=65-80 --force --ext .png --skip-if-larger {} +
else
  echo "WARNING: pngquant not found, skipping optimization (brew install pngquant)"
fi

echo ""
echo "==> Done! Screenshots saved to:"
echo "    fastlane/metadata/android/*/images/"
echo ""
find "$PROJECT_DIR/fastlane/metadata/android" -name "*.png" -path "*/images/*" -newer "$SCRIPT_DIR/generate_android_screenshots.sh" | sort
