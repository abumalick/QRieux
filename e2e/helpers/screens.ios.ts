// Screen interaction helpers for iOS (XCUITest selectors)
// Uses label predicates and accessibility identifiers

import { execSync } from 'child_process';
import fs from 'fs';
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

export async function shareTextToApp(text: string): Promise<void> {
  const encoded = encodeURIComponent(text);
  execSync(`xcrun simctl openurl booted 'qrieux://create?text=${encoded}'`);
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

export async function tapCreateButton(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Create"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForGeneratorScreen(): Promise<void> {
  const desc = await $('-ios predicate string:label CONTAINS "Choose what you want to share"');
  await desc.waitForExist({ timeout: 10_000 });
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

  // Dismiss picker by tapping description area
  const desc = await $('-ios predicate string:label CONTAINS "Choose what you want to share"');
  await desc.click();
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
  const btn = await $('-ios predicate string:label == "Share QR"');
  if (!(await btn.isExisting())) return false;
  return btn.isEnabled();
}

export async function tapGenerateButton(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Generate QR Code"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function isGenerateButtonEnabled(): Promise<boolean> {
  const btn = await $('-ios predicate string:label == "Generate QR Code"');
  if (!(await btn.isExisting())) return false;
  return btn.isEnabled();
}

export async function waitForQrResultScreen(): Promise<void> {
  const title = await $('-ios predicate string:label == "Your QR Code"');
  await title.waitForExist({ timeout: 10_000 });
}

export async function tapEditButton(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Edit"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function isValidationErrorVisible(errorText: string): Promise<boolean> {
  // Scroll up to see validation errors (they appear near the input fields)
  await iosScrollUp();
  const err = await $(`-ios predicate string:label == "${errorText}"`);
  return err.isExisting();
}

export async function tapScanTab(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Scan"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export const tapBackToScan = tapScanTab;

export async function tapHelpTab(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Help"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForHelpScreen(): Promise<void> {
  const title = await $('-ios predicate string:label == "How to Use QRieux"');
  await title.waitForExist({ timeout: 10_000 });
}

export async function getSelectedType(): Promise<string> {
  const field = await $('-ios predicate string:label == "selection_field"');
  await field.waitForExist({ timeout: 5_000 });
  return await field.getValue() as string;
}

// --- Toast / flash / help / share helpers ---

export async function waitForToast(text: string): Promise<void> {
  const toast = await $(`-ios predicate string:label CONTAINS "${text}"`);
  await toast.waitForExist({ timeout: 5_000 });
}

export async function tapFlashButton(): Promise<void> {
  const onBtn = await $('~Turn on flash');
  if (await onBtn.isExisting()) {
    await onBtn.click();
    return;
  }
  const offBtn = await $('~Turn off flash');
  await offBtn.waitForExist({ timeout: 5_000 });
  await offBtn.click();
}

export async function getFlashButtonLabel(): Promise<string> {
  const offBtn = await $('~Turn off flash');
  if (await offBtn.isExisting()) return 'Turn off flash';
  const onBtn = await $('~Turn on flash');
  if (await onBtn.isExisting()) return 'Turn on flash';
  throw new Error('Flash button not found');
}

export async function tapHelpButton(): Promise<void> {
  const btn = await $('~Help');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function isOnboardingVisible(): Promise<boolean> {
  const el = await $('-ios predicate string:label == "What are QR Codes?"');
  return el.isExisting();
}

export async function tapShareQrButton(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Share QR"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function dismissShareSheet(): Promise<void> {
  // iOS share sheet: swipe down to dismiss (tap on dimmed area is unreliable on iOS 26+)
  const { width, height } = await browser.getWindowSize();
  const centerX = Math.floor(width / 2);
  const sheetTop = Math.floor(height * 0.4);
  const sheetBottom = Math.floor(height * 0.9);

  await driver.performActions([{
    type: 'pointer', id: 'dismiss', parameters: { pointerType: 'touch' },
    actions: [
      { type: 'pointerMove', duration: 0, x: centerX, y: sheetTop },
      { type: 'pointerDown', button: 0 },
      { type: 'pointerMove', duration: 300, x: centerX, y: sheetBottom },
      { type: 'pointerUp', button: 0 },
    ],
  }]);
  await driver.releaseActions();
  await browser.pause(1000);
}

export async function selectWifiSecurity(securityName: string): Promise<void> {
  await iosDismissKeyboard();
  await iosScrollToElement('Security');

  // Tap the security selection_field (last one visible — type chooser may be scrolled away)
  const fields = await $$('-ios predicate string:label == "selection_field"');
  const count = fields.length;
  if (count < 1) {
    throw new Error('Could not find Wi-Fi security selection field');
  }
  await fields[count - 1].click();
  await browser.pause(500);

  const picker = await $('-ios class chain:**/XCUIElementTypePickerWheel');
  await picker.waitForExist({ timeout: 5_000 });

  let found = false;
  for (let i = 0; i < 5; i++) {
    const current = await picker.getValue();
    if (current === securityName) { found = true; break; }
    await driver.execute('mobile: selectPickerWheelValue', {
      element: picker.elementId,
      order: 'next',
      offset: 0.15,
    });
    await browser.pause(300);
  }
  if (!found) {
    const final = await picker.getValue();
    if (final !== securityName) {
      throw new Error(`Could not select Wi-Fi security "${securityName}" — picker stuck on "${final}"`);
    }
  }

  // Dismiss picker
  const title = await $('-ios predicate string:label == "Create QR Code"');
  await title.click();
  await browser.pause(500);
}

export async function toggleWifiHidden(): Promise<void> {
  await iosDismissKeyboard();
  await iosScrollToElement('Hidden network');
  const toggle = await $('-ios class chain:**/XCUIElementTypeSwitch');
  await toggle.waitForExist({ timeout: 5_000 });
  await toggle.click();
  await browser.pause(300);
}

export async function shareVCardToApp(vcfContent: string): Promise<void> {
  const containerPath = execSync(
    'xcrun simctl get_app_container booted net.hilson.qrieux group.net.hilson.qrieux',
  ).toString().trim();

  // Strip vCard to QR-friendly fields (simulates what ShareExtension does)
  const stripped = stripVCardForTest(vcfContent);
  const destPath = path.join(containerPath, 'shared-text.txt');
  fs.writeFileSync(destPath, stripped, 'utf-8');

  execSync('xcrun simctl openurl booted qrieux://shared-text');
  await browser.pause(2000);
}

function stripVCardForTest(vcard: string): string {
  const allowedPrefixes = [
    'BEGIN:', 'END:', 'VERSION:', 'N:', 'N;', 'FN:', 'FN;',
    'TEL:', 'TEL;', 'EMAIL:', 'EMAIL;', 'ORG:', 'ORG;',
    'TITLE:', 'TITLE;', 'ADR:', 'ADR;', 'URL:', 'URL;',
    'BDAY:', 'NOTE:',
  ];
  const lines = vcard.split(/\r?\n/);
  const result: string[] = [];
  let skipContinuation = false;
  for (const line of lines) {
    if (!line.trim()) continue;
    if (line.startsWith(' ') || line.startsWith('\t')) {
      if (!skipContinuation) result.push(line);
      continue;
    }
    const upper = line.trim().toUpperCase();
    const allowed = allowedPrefixes.some(p => upper.startsWith(p));
    skipContinuation = !allowed;
    if (allowed) result.push(line.trim());
  }
  return result.join('\r\n');
}

export async function reactivateApp(): Promise<void> {
  await driver.activateApp('net.hilson.qrieux');
  await browser.pause(1000);
}

// --- History helpers ---

export async function tapHistoryTab(): Promise<void> {
  const btn = await $('-ios predicate string:label == "History"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForHistoryScreen(): Promise<void> {
  // Wait for the History title text
  const title = await $('-ios predicate string:label == "History"');
  await title.waitForExist({ timeout: 10_000 });
}

export async function isHistoryEmpty(): Promise<boolean> {
  const empty = await $('-ios predicate string:label == "No history yet"');
  return empty.isExisting();
}

export async function getHistoryEntryCount(): Promise<number> {
  const scanned = await $$('-ios predicate string:label == "Scanned"');
  const created = await $$('-ios predicate string:label == "Created"');
  return scanned.length + created.length;
}

export async function tapHistoryEntry(index: number): Promise<void> {
  const entries = await $$('-ios predicate string:label == "Scanned" OR label == "Created"');
  if (index >= entries.length) {
    throw new Error(`History entry ${index} not found (total: ${entries.length})`);
  }
  await entries[index].click();
}

export async function swipeDeleteHistoryEntry(index: number): Promise<void> {
  const entries = await $$('-ios predicate string:label == "Scanned" OR label == "Created"');
  if (index >= entries.length) {
    throw new Error(`History entry ${index} not found for swipe delete`);
  }
  const el = entries[index];
  const { width } = await browser.getWindowSize();
  const location = await el.getLocation();
  const size = await el.getSize();
  const y = location.y + Math.floor(size.height / 2);

  // Swipe far enough to trigger dismiss (full screen width)
  await driver.performActions([{
    type: 'pointer', id: 'swipe', parameters: { pointerType: 'touch' },
    actions: [
      { type: 'pointerMove', duration: 0, x: width - 30, y },
      { type: 'pointerDown', button: 0 },
      { type: 'pointerMove', duration: 400, x: 10, y },
      { type: 'pointerUp', button: 0 },
    ],
  }]);
  await driver.releaseActions();
  await browser.pause(1000);
}

export async function tapClearHistory(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Clear All"');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
  await browser.pause(300);
  // Confirm dialog
  const confirm = await $('-ios predicate string:label == "Clear"');
  await confirm.waitForExist({ timeout: 3_000 });
  await confirm.click();
  await browser.pause(300);
}

export async function tapBackToHistory(): Promise<void> {
  const btn = await $('-ios predicate string:label == "Back to History"');
  if (await btn.isExisting()) {
    await btn.click();
    return;
  }
  const back = await $('~Navigate back');
  await back.waitForExist({ timeout: 5_000 });
  await back.click();
}