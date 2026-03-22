import { execSync } from 'child_process';
import path from 'path';

const DEVICE_DIR = '/sdcard/DCIM/QRieux-E2E';
const FIXTURES_DIR = path.join(__dirname, '..', 'fixtures');

const pushedFiles: string[] = [];

export async function pushFixtureToDevice(fixtureName: string): Promise<string> {
  const localPath = path.join(FIXTURES_DIR, fixtureName);
  const devicePath = `${DEVICE_DIR}/${fixtureName}`;

  execSync(`adb shell mkdir -p '${DEVICE_DIR}'`);
  execSync(`adb push '${localPath}' '${devicePath}'`);

  execSync(
    `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://${devicePath}'`
  );
  pushedFiles.push(devicePath);
  return devicePath;
}

export async function pushAllFixtures(): Promise<void> {
  const fixtures = ['url-https.png', 'email-mailto.png', 'phone-tel.png', 'text-hello.png', 'wifi.png'];
  for (const f of fixtures) {
    await pushFixtureToDevice(f);
  }
  // Give media scanner time to index
  await browser.pause(2000);
}

export async function cleanupFixtures(): Promise<void> {
  try {
    execSync(`adb shell rm -rf '${DEVICE_DIR}'`);
    execSync(
      `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file:///sdcard/DCIM/'`
    );
  } catch {
    // Best-effort cleanup
  }
  pushedFiles.length = 0;
}
