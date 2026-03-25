import { execSync } from 'child_process';
import path from 'path';

const FIXTURES_DIR = path.join(__dirname, '..', 'fixtures');
const FIXTURES = ['url-https.png', 'email-mailto.png', 'phone-tel.png', 'text-hello.png', 'wifi.png', 'vcard.png', 'no-qr.png'];

// Android
const DEVICE_DIR = '/sdcard/DCIM/QRieux-E2E';

function pushFixtureAndroid(fixtureName: string): void {
  const localPath = path.join(FIXTURES_DIR, fixtureName);
  const devicePath = `${DEVICE_DIR}/${fixtureName}`;

  execSync(`adb shell mkdir -p '${DEVICE_DIR}'`);
  execSync(`adb push '${localPath}' '${devicePath}'`);
  execSync(
    `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://${devicePath}'`
  );
}

function pushFixtureIos(fixtureName: string): void {
  const localPath = path.join(FIXTURES_DIR, fixtureName);
  execSync(`xcrun simctl addmedia booted '${localPath}'`);
}

export async function pushAllFixtures(): Promise<void> {
  const pushFn = driver.isAndroid ? pushFixtureAndroid : pushFixtureIos;
  for (const f of FIXTURES) {
    pushFn(f);
  }
  // Give media scanner/Photos time to index
  await browser.pause(2000);
}

export async function cleanupFixtures(): Promise<void> {
  if (!driver.isAndroid) return; // iOS: noReset handles cleanup

  try {
    execSync(`adb shell rm -rf '${DEVICE_DIR}'`);
    execSync(
      `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file:///sdcard/DCIM/'`
    );
  } catch (e) {
    console.warn('Fixture cleanup failed (best-effort):', (e as Error).message);
  }
}
