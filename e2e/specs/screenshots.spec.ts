// App Store screenshot generation spec — locale-agnostic
// DEMO_CAMERA env var skips onboarding + camera permission in the app.
// Uses assertions before each screenshot to verify the correct screen is shown.
// Env vars: SCREENSHOT_OUTPUT_DIR, SCREENSHOT_PREFIX

import path from 'path';
import { mkdirSync } from 'fs';
import { execSync } from 'child_process';
import { pushAllFixtures } from '../helpers/qr-fixtures.js';

const outputDir = process.env.SCREENSHOT_OUTPUT_DIR || path.join(process.cwd(), 'screenshots');
const prefix = process.env.SCREENSHOT_PREFIX || '';

function screenshotPath(name: string): string {
  return path.join(outputDir, `${prefix}${name}.png`);
}

const IOS_TABS = ['scan', 'create', 'history', 'help'];

async function tapTabByIndex(index: number): Promise<void> {
  if (driver.isAndroid) {
    const { width, height } = await browser.getWindowSize();
    // Material3 NavigationBar sits above the system gesture bar (~130px from bottom)
    const y = height - 130;
    const sectionWidth = width / 4;
    const x = Math.floor(sectionWidth * index + sectionWidth / 2);
    await driver.execute('mobile: clickGesture', { x, y });
  } else {
    execSync(`xcrun simctl openurl booted 'qrieux://tab/${IOS_TABS[index]}'`);
  }
  await browser.pause(1000);
}

// Wait for the scanner screen (scan overlay text or flash button visible)
async function waitForScanner(): Promise<void> {
  await browser.waitUntil(async () => {
    // Check for flash button (accessibility label) or scan instruction text
    if (driver.isAndroid) {
      const el = await $('android=new UiSelector().descriptionContains("flash")');
      return el.isExisting();
    } else {
      // The flash button always exists on scanner; check by class + position
      const buttons = await $$('-ios class chain:**/XCUIElementTypeButton');
      return buttons.length >= 3; // flash + gallery + tab bar buttons
    }
  }, { timeout: 15_000, timeoutMsg: 'Scanner screen did not load' });
}

// Wait for scan result overlay
async function waitForScanResult(): Promise<void> {
  if (driver.isAndroid) {
    await browser.waitUntil(async () => {
      const el = await $('android=new UiSelector().textContains("http")');
      return el.isExisting();
    }, { timeout: 15_000, timeoutMsg: 'Scan result did not appear' });
  } else {
    const el = await $('~scan_result_content');
    await el.waitForExist({ timeout: 15_000 });
  }
}

// Wait for QR generator screen (text field visible)
async function waitForQrGenerator(): Promise<void> {
  await browser.waitUntil(async () => {
    if (driver.isAndroid) {
      const el = await $('android=new UiSelector().className("android.widget.EditText")');
      return el.isExisting();
    } else {
      const fields = await $$('-ios class chain:**/XCUIElementTypeTextField');
      return fields.length > 0;
    }
  }, { timeout: 15_000, timeoutMsg: 'QR generator screen did not load' });
}

// Wait for QR result overlay
async function waitForQrResult(): Promise<void> {
  if (driver.isAndroid) {
    const el = await $('android=new UiSelector().text("Your QR Code")');
    await el.waitForExist({ timeout: 10_000 });
  } else {
    const el = await $('~qr_result_title');
    await el.waitForExist({ timeout: 10_000 });
  }
}

// Tap the Generate QR Code button
async function tapGenerate(): Promise<void> {
  if (driver.isAndroid) {
    await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Generate QR Code"))');
    const btn = await $('android=new UiSelector().clickable(true).childSelector(new UiSelector().text("Generate QR Code"))');
    await btn.waitForExist({ timeout: 5_000 });
    await btn.click();
  } else {
    const btn = await $('~generate_button');
    await btn.waitForExist({ timeout: 5_000 });
    await btn.click();
  }
}

describe('App Store Screenshots', () => {
  before(async () => {
    mkdirSync(outputDir, { recursive: true });
    if (driver.isAndroid) {
      await pushAllFixtures();
    }
    await waitForScanner();
  });

  it('1_scanner', async () => {
    await waitForScanner();
    await browser.saveScreenshot(screenshotPath('1_scanner'));
  });

  it('2_url_result', async () => {
    if (driver.isAndroid) {
      const { shareImageToApp } = await import('../helpers/screens.android.js');
      await shareImageToApp('url-https');
    } else {
      const containerPath = execSync(
        'xcrun simctl get_app_container booted net.hilson.qrieux group.net.hilson.qrieux',
      ).toString().trim();
      const fixturePath = path.join(__dirname, '..', 'fixtures', 'url-https.png');
      execSync(`cp '${fixturePath}' '${containerPath}/shared-image.jpg'`);
      execSync('xcrun simctl openurl booted qrieux://shared-image');
    }
    await waitForScanResult();
    await browser.saveScreenshot(screenshotPath('2_url_result'));
    // Go back to scanner
    await tapTabByIndex(0);
    await waitForScanner();
  });

  it('3_create_qr', async () => {
    if (driver.isAndroid) {
      const { shareTextToApp } = await import('../helpers/screens.android.js');
      await shareTextToApp('https://www.wikipedia.org');
    } else {
      const encoded = encodeURIComponent('https://www.wikipedia.org');
      execSync(`xcrun simctl openurl booted 'qrieux://create?text=${encoded}'`);
    }
    await waitForQrGenerator();
    await browser.pause(1000);
    await browser.saveScreenshot(screenshotPath('3_create_qr'));
  });

  it('4_create_qr_result', async () => {
    await tapGenerate();
    await waitForQrResult();
    await browser.pause(2000);
    await browser.saveScreenshot(screenshotPath('4_create_qr_result'));
    // Dismiss QR result overlay so nav bar becomes tappable
    if (driver.isAndroid) {
      const backBtn = await $('~Navigate back');
      await backBtn.waitForExist({ timeout: 5_000 });
      await backBtn.click();
    }
    await browser.pause(500);
  });

  it('5_history', async () => {
    await tapTabByIndex(2);
    await browser.pause(1500);
    await browser.saveScreenshot(screenshotPath('5_history'));
  });

  it('6_help', async () => {
    await tapTabByIndex(3);
    await browser.pause(1500);
    await browser.saveScreenshot(screenshotPath('6_help'));
  });
});
