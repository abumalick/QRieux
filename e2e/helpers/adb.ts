// Every adb call must name its target: the suite runs against a fixed emulator,
// and a phone plugged into the same machine would otherwise be picked up (or
// make adb refuse outright with "more than one device").
export const DEVICE_SERIAL = process.env.E2E_DEVICE_SERIAL || 'emulator-5554';

export const ADB = `adb -s ${DEVICE_SERIAL}`;
