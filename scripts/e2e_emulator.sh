#!/usr/bin/env bash
# Manages the fixed Android emulator the E2E suite runs against.
#
# The suite is only meaningful if every run sees the same device, so the AVD is
# defined here rather than assumed to exist on the machine, and every start
# wipes user data. Anything left behind by a previous run — indexed MediaStore
# rows, granted permissions, app state — would otherwise leak into the next one.
set -euo pipefail

AVD_NAME="qrieux-e2e"
SYSTEM_IMAGE="system-images;android-36;google_apis;x86_64"
DEVICE_PROFILE="pixel_6"
EMULATOR_PORT=5554
SERIAL="emulator-${EMULATOR_PORT}"
BOOT_TIMEOUT=300

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"

# avdmanager follows ANDROID_USER_HOME/XDG while the emulator only searches
# ANDROID_AVD_HOME, ANDROID_SDK_HOME/avd and ~/.android/avd. Left to the host
# those can disagree and the emulator cannot find the AVD that was just
# created, so pin one location for both.
export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
mkdir -p "$ANDROID_AVD_HOME"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
EMULATOR="$ANDROID_HOME/emulator/emulator"
ADB="$ANDROID_HOME/platform-tools/adb"
[ -x "$ADB" ] || ADB="$(command -v adb)"

die() { echo "e2e-emulator: $*" >&2; exit 1; }

require_tools() {
    [ -x "$SDKMANAGER" ] || die "sdkmanager not found at $SDKMANAGER — install the Android cmdline-tools"
    [ -x "$AVDMANAGER" ] || die "avdmanager not found at $AVDMANAGER — install the Android cmdline-tools"
    [ -x "$EMULATOR" ] || die "emulator not found at $EMULATOR — install the Android emulator package"
}

is_running() {
    "$ADB" devices | grep -q "^${SERIAL}[[:space:]]*device$"
}

cmd_ensure() {
    require_tools
    # The package name is already a path: system-images;android-36;... -> system-images/android-36/...
    local image_path="$ANDROID_HOME/${SYSTEM_IMAGE//;//}"
    if [ ! -d "$image_path" ]; then
        echo "e2e-emulator: installing $SYSTEM_IMAGE"
        # Feed a fixed number of licence acceptances: `yes |` dies of SIGPIPE
        # under pipefail once sdkmanager stops reading.
        printf 'y\n%.0s' $(seq 50) | "$SDKMANAGER" --install "$SYSTEM_IMAGE" >/dev/null
    fi

    if ! "$AVDMANAGER" list avd -c | grep -qx "$AVD_NAME"; then
        echo "e2e-emulator: creating AVD $AVD_NAME"
        echo "no" | "$AVDMANAGER" create avd \
            --name "$AVD_NAME" \
            --package "$SYSTEM_IMAGE" \
            --device "$DEVICE_PROFILE" \
            --force >/dev/null
    fi
    echo "e2e-emulator: $AVD_NAME ready ($SYSTEM_IMAGE)"
}

cmd_start() {
    cmd_ensure
    # Without KVM the emulator falls back to software emulation and a suite that
    # takes minutes takes hours, so fail loudly rather than appear to hang.
    if [ ! -r /dev/kvm ] || [ ! -w /dev/kvm ]; then
        die "no access to /dev/kvm — add yourself to the kvm group (sudo usermod -aG kvm \"\$USER\"), then log out and back in, or run this under 'sg kvm -c ...'"
    fi
    if is_running; then
        echo "e2e-emulator: replacing the running instance for a clean boot"
        cmd_stop
    fi

    # -wipe-data with no snapshot is what makes runs comparable; the rest keeps
    # the device from varying with the host (clock, audio, window manager).
    local flags=(
        -avd "$AVD_NAME"
        -port "$EMULATOR_PORT"
        -wipe-data
        -no-snapshot-save
        -no-snapshot-load
        -no-boot-anim
        -no-audio
        -timezone UTC
    )
    if [ "${E2E_EMULATOR_WINDOW:-0}" = "1" ]; then
        flags+=(-gpu auto)
    else
        flags+=(-no-window -gpu swiftshader_indirect)
    fi

    echo "e2e-emulator: booting $AVD_NAME on $SERIAL"
    nohup "$EMULATOR" "${flags[@]}" >/tmp/qrieux-emulator.log 2>&1 &

    "$ADB" -s "$SERIAL" wait-for-device
    local waited=0
    until [ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
        sleep 2
        waited=$((waited + 2))
        [ "$waited" -lt "$BOOT_TIMEOUT" ] || die "emulator did not finish booting within ${BOOT_TIMEOUT}s (see /tmp/qrieux-emulator.log)"
    done

    # Animations make Appium's waits race against the compositor.
    "$ADB" -s "$SERIAL" shell settings put global window_animation_scale 0
    "$ADB" -s "$SERIAL" shell settings put global transition_animation_scale 0
    "$ADB" -s "$SERIAL" shell settings put global animator_duration_scale 0

    local locale
    locale="$("$ADB" -s "$SERIAL" shell getprop persist.sys.locale 2>/dev/null | tr -d '\r')"
    locale="${locale:-$("$ADB" -s "$SERIAL" shell getprop ro.product.locale 2>/dev/null | tr -d '\r')}"
    case "$locale" in
        en-US|en_US|en-us) ;;
        *) die "emulator locale is '$locale' but the specs match English text — expected en-US" ;;
    esac

    echo "e2e-emulator: $SERIAL ready"
}

cmd_stop() {
    if is_running; then
        "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1 || true
        local waited=0
        while is_running && [ "$waited" -lt 30 ]; do
            sleep 1
            waited=$((waited + 1))
        done
    fi
    echo "e2e-emulator: $SERIAL stopped"
}

cmd_status() {
    if is_running; then
        echo "running ($SERIAL)"
    else
        echo "stopped"
    fi
}

cmd_serial() { echo "$SERIAL"; }

case "${1:-}" in
    ensure) cmd_ensure ;;
    start)  cmd_start ;;
    stop)   cmd_stop ;;
    status) cmd_status ;;
    serial) cmd_serial ;;
    *) die "usage: $0 {ensure|start|stop|status|serial}" ;;
esac
