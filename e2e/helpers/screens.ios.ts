// Screen interaction helpers for iOS (XCUITest selectors)
// Uses label predicates and accessibility identifiers

import { execSync } from 'child_process';
import path from 'path';

const FIXTURES_DIR = path.join(__dirname, '..', 'fixtures');

export async function shareImageToApp(fixtureName: string): Promise<void> {
  const containerPath = execSync(
    'xcrun simctl get_app_container booted net.hilson.qrieux group.net.hilson.qrieux',
  ).toString().trim();

  const fixturePath = path.join(FIXTURES_DIR, `${fixtureName}.png`);
  const destPath = path.join(containerPath, 'shared-image.jpg');
  execSync(`cp '${fixturePath}' '${destPath}'`);

  execSync('xcrun simctl openurl booted qrieux://shared-image');
  await browser.pause(2000);
}

export async function dismissOnboarding(): Promise<void> {
  const skipBtn = await $('-ios predicate string:label == "Skip"');
  if (await skipBtn.isExisting()) {
    await skipBtn.click();
    await browser.pause(1000);
  }

  // Handle app's own permission screen
  const continueBtn = await $('-ios predicate string:label == "Continue"');
  if (await continueBtn.isExisting()) {
    await continueBtn.click();
    // autoAcceptAlerts handles the system permission dialog
    await browser.pause(2000);
  }

  const instruction = await $('-ios predicate string:label == "Place QR code inside the frame"');
  await instruction.waitForExist({ timeout: 15_000 });
}

export async function waitForScanner(): Promise<void> {
  const instruction = await $('-ios predicate string:label == "Place QR code inside the frame"');
  await instruction.waitForExist({ timeout: 10_000 });
}

export async function tapGalleryButton(): Promise<void> {
  const btn = await $('~Pick photo from gallery');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function pickImageFromGallery(): Promise<void> {
  // Wait for UIImagePickerController navigation bar to confirm picker is open
  const nav = await $('-ios class chain:**/XCUIElementTypeNavigationBar');
  await nav.waitForExist({ timeout: 10_000 });
  await browser.pause(1000);

  // Photo grid images report visible=false and are intermittently unfindable
  // via predicate queries. Use mobile: tap at a coordinate in the grid area.
  // Grid starts at y~324 on standard screen; first cell center is ~(66, 390).
  const { width } = await browser.getWindowSize();
  await driver.execute('mobile: tap', {
    x: Math.floor(width / 6),
    y: 400,
  });
  await browser.pause(2000);
}

export async function waitForScanResult(): Promise<void> {
  const title = await $('-ios predicate string:label == "Scanned Result"');
  await title.waitForExist({ timeout: 15_000 });
}

export async function getScanResultText(): Promise<string> {
  const el = await $('~scan_result_content');
  await el.waitForExist({ timeout: 10_000 });
  return el.getText();
}

export async function assertScanResultContains(expected: string): Promise<void> {
  const text = await getScanResultText();
  if (!text.includes(expected)) {
    throw new Error(`Scan result "${text}" does not contain "${expected}"`);
  }
}

export async function isActionButtonVisible(label: string): Promise<boolean> {
  const btn = await $(`-ios predicate string:label == "${label}"`);
  return btn.isExisting();
}

export async function tapActionButton(label: string): Promise<void> {
  const btn = await $(`-ios predicate string:label == "${label}"`);
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function tapScanAgain(): Promise<void> {
  await tapActionButton('Scan Again');
}

// --- QR Creation helpers ---

async function iosSwipe(direction: 'up' | 'down'): Promise<void> {
  const { width, height } = await browser.getWindowSize();
  const centerX = Math.floor(width / 2);
  const startY = direction === 'up' ? Math.floor(height * 0.6) : Math.floor(height * 0.3);
  const endY = direction === 'up' ? Math.floor(height * 0.2) : Math.floor(height * 0.7);

  await driver.performActions([{
    type: 'pointer', id: 'finger1', parameters: { pointerType: 'touch' },
    actions: [
      { type: 'pointerMove', duration: 0, x: centerX, y: startY },
      { type: 'pointerDown', button: 0 },
      { type: 'pointerMove', duration: 300, x: centerX, y: endY },
      { type: 'pointerUp', button: 0 },
    ],
  }]);
  await driver.releaseActions();
  await browser.pause(300);
}

async function iosScrollDown(): Promise<void> {
  await iosSwipe('up');
}

async function iosScrollUp(): Promise<void> {
  await iosSwipe('down');
}

async function iosScrollToShareButton(): Promise<void> {
  // Scroll down until "Share QR" is visible
  for (let i = 0; i < 3; i++) {
    const btn = await $('-ios predicate string:label == "Share QR"');
    if (await btn.isExisting()) return;
    await iosScrollDown();
  }
}

export async function tapCreateButton(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Create"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForGeneratorScreen(): Promise<void> {
  const title = await $('-ios predicate string:label == "Create QR Code"');
  await title.waitForExist({ timeout: 10_000 });
}

export async function selectQrType(typeName: string): Promise<void> {
  await iosDismissKeyboard();
  // Scroll to top aggressively to reveal the dropdown
  await iosScrollUp();
  await iosScrollUp();
  await browser.pause(300);

  // Tap the UITextField via accessibility label.
  // This also dismisses any Compose keyboard (UITextField becomes new first responder).
  const typeField = await $('-ios predicate string:label == "selection_field"');
  await typeField.waitForExist({ timeout: 5_000 });
  await typeField.click();
  await browser.pause(500);

  const picker = await $('-ios class chain:**/XCUIElementTypePickerWheel');
  await picker.waitForExist({ timeout: 5_000 });

  // Scroll picker until desired value (max 5 steps)
  for (let i = 0; i < 5; i++) {
    const current = await picker.getValue();
    if (current === typeName) break;
    await driver.execute('mobile: selectPickerWheelValue', {
      element: picker.elementId,
      order: 'next',
      offset: 0.15,
    });
    await browser.pause(300);
  }

  // Dismiss picker by tapping title area
  const title = await $('-ios predicate string:label == "Create QR Code"');
  await title.click();
  await browser.pause(500);
}

async function iosDismissKeyboard(): Promise<void> {
  // Tap on the QR preview area to trigger Compose's dismissKeyboardOnBackgroundTap.
  // Uses mobile: tap which dispatches through UIKit touch system.
  const { height } = await browser.getWindowSize();
  await driver.execute('mobile: tap', { x: 200, y: Math.floor(height * 0.4) });
  await browser.pause(500);
}

async function iosScrollToElement(label: string): Promise<void> {
  // Try scrolling up first, then down to find the element
  for (let dir = 0; dir < 2; dir++) {
    for (let i = 0; i < 3; i++) {
      const el = await $(`-ios predicate string:label == "${label}"`);
      if (await el.isExisting()) return;
      if (dir === 0) await iosScrollUp();
      else await iosScrollDown();
    }
  }
}

export async function enterTextInField(label: string, value: string): Promise<void> {
  await iosDismissKeyboard();
  await iosScrollToElement(label);

  const field = await $(`-ios predicate string:label == "${label}"`);
  await field.waitForExist({ timeout: 5_000 });
  await field.click();
  await browser.pause(300);
  await field.clearValue();
  if (value) {
    await field.setValue(value);
  }
  await iosDismissKeyboard();
}

export async function clearField(label: string): Promise<void> {
  await iosDismissKeyboard();
  await iosScrollToElement(label);

  const field = await $(`-ios predicate string:label == "${label}"`);
  await field.waitForExist({ timeout: 5_000 });
  await field.click();
  await browser.pause(300);
  await field.clearValue();
  await iosDismissKeyboard();
}

export async function isShareQrButtonEnabled(): Promise<boolean> {
  await iosScrollToShareButton();
  const btn = await $('-ios predicate string:label == "Share QR"');
  if (!(await btn.isExisting())) return false;
  return btn.isEnabled();
}

export async function isPreviewHintVisible(): Promise<boolean> {
  const hint = await $('-ios predicate string:label CONTAINS "Fill in the form"');
  return hint.isExisting();
}

export async function isValidationErrorVisible(errorText: string): Promise<boolean> {
  // Scroll up to see validation errors (they appear near the input fields)
  await iosScrollUp();
  const err = await $(`-ios predicate string:label == "${errorText}"`);
  return err.isExisting();
}

export async function tapBackToScan(): Promise<void> {
  await iosScrollUp();
  const btn = await $('~Back to Scan');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForQrGenerated(): Promise<void> {
  await browser.waitUntil(
    async () => {
      await iosScrollToShareButton();
      const btn = await $('-ios predicate string:label == "Share QR"');
      if (!(await btn.isExisting())) return false;
      return btn.isEnabled();
    },
    { timeout: 10_000, timeoutMsg: 'QR code was not generated in time' }
  );
}
