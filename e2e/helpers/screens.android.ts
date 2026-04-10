// Screen interaction helpers for Android (UiSelector syntax)
// Compose Multiplatform doesn't support testTagsAsResourceId — uses text/description selectors

import { execSync } from 'child_process';

export async function shareImageToApp(fixtureName: string): Promise<void> {
  // Query MediaStore for the content URI by display name (with retry for media scanner lag)
  let mediaId: string | null = null;
  for (let attempt = 0; attempt < 3; attempt++) {
    const output = execSync(
      `adb shell content query --uri content://media/external/images/media --projection _id --where "\\"_display_name='${fixtureName}.png'\\""`,
    ).toString().trim();
    const match = output.match(/_id=(\d+)/);
    if (match) {
      mediaId = match[1];
      break;
    }
    await browser.pause(1000);
  }
  if (!mediaId) {
    throw new Error(`Could not find media URI for ${fixtureName}.png — ensure fixtures are pushed`);
  }

  const contentUri = `content://media/external/images/media/${mediaId}`;
  // Flags: NEW_TASK | SINGLE_TOP | GRANT_READ_URI_PERMISSION = 0x30000001
  // -d sets the data URI so FLAG_GRANT_READ_URI_PERMISSION creates a URI permission grant;
  // --eu sets EXTRA_STREAM which the app reads — same URI, so the grant covers it
  execSync(
    `adb shell am start -a android.intent.action.SEND -t image/png -f 0x30000001 ` +
    `-d '${contentUri}' --eu android.intent.extra.STREAM '${contentUri}' ` +
    `-n net.hilson.qrieux.dev/net.hilson.qrieux.MainActivity`,
  );
  await browser.pause(2000);
}

export async function shareTextToApp(text: string): Promise<void> {
  // Escape for inner single-quotes (device shell), then outer double-quotes (host shell)
  const innerEscaped = text.replace(/'/g, "'\\''");
  const escaped = innerEscaped.replace(/[\\$`"]/g, '\\$&');
  execSync(
    `adb shell "am start -a android.intent.action.SEND -t text/plain ` +
    `-f 0x30000000 --es android.intent.extra.TEXT '${escaped}' ` +
    `-n net.hilson.qrieux.dev/net.hilson.qrieux.MainActivity"`,
  );
  await browser.pause(2000);
}

export async function dismissOnboarding(): Promise<void> {
  const skipBtn = await $('android=new UiSelector().text("Skip")');
  if (await skipBtn.isExisting()) {
    await skipBtn.click();
    const instruction = await $('android=new UiSelector().text("Place QR code inside the frame")');
    await instruction.waitForExist({ timeout: 10_000 });
  }
}

export async function waitForScanner(): Promise<void> {
  const instruction = await $('android=new UiSelector().text("Place QR code inside the frame")');
  await instruction.waitForExist({ timeout: 10_000 });
}

export async function tapGalleryButton(): Promise<void> {
  const btn = await $('~Pick photo from gallery');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function pickImageFromGallery(): Promise<void> {
  await browser.pause(3000);

  const selectors = [
    'android=new UiSelector().resourceIdMatches(".*icon_thumbnail.*").instance(0)',
    'android=new UiSelector().checkable(true).instance(0)',
    'android=new UiSelector().packageName("com.google.android.providers.media.module").className("android.widget.ImageView").instance(0)',
  ];

  for (const sel of selectors) {
    const image = await $(sel);
    if (await image.isExisting()) {
      await image.click();
      await browser.pause(1000);
      return;
    }
  }

  throw new Error(
    'Could not find any image in the photo picker. ' +
    'Ensure QR fixtures were pushed to the device before running this test.'
  );
}

export async function waitForScanResult(): Promise<void> {
  const title = await $('android=new UiSelector().text("Scanned Result")');
  await title.waitForExist({ timeout: 15_000 });
}

export async function getScanResultText(): Promise<string> {
  // testTag doesn't map to contentDescription in Compose Multiplatform;
  // the result text renders as a ScrollView with text content
  const el = await $('android=new UiSelector().className("android.widget.ScrollView").textMatches(".+")');
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
  const btn = await $(`android=new UiSelector().text("${label}")`);
  return btn.isExisting();
}

export async function tapActionButton(label: string): Promise<void> {
  const btn = await $(`android=new UiSelector().text("${label}")`);
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function tapScanAgain(): Promise<void> {
  await tapActionButton('Scan Again');
}

// --- QR Creation helpers ---

async function scrollToTop(): Promise<void> {
  await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(3)');
  await browser.pause(300);
}

export async function tapCreateButton(): Promise<void> {
  const btn = await $('android=new UiSelector().text("Create")');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForGeneratorScreen(): Promise<void> {
  const desc = await $('android=new UiSelector().textContains("Choose what you want to share")');
  await desc.waitForExist({ timeout: 10_000 });
}

export async function selectQrType(typeName: string): Promise<void> {
  await scrollToTop();
  // The type chooser is an ExposedDropdownMenuBox with a Box anchor (not EditText).
  // Find the currently selected type text and click it to open the dropdown,
  // then click the desired option.
  const typeLabels = ['Text', 'Website', 'Email', 'Phone', 'Wi-Fi'];
  for (const label of typeLabels) {
    const trigger = await $(`android=new UiSelector().text("${label}")`);
    if (await trigger.isExisting()) {
      await trigger.click();
      await browser.pause(500);
      break;
    }
  }
  const option = await $(`android=new UiSelector().text("${typeName}")`);
  await option.waitForExist({ timeout: 3_000 });
  await option.click();
  await browser.pause(500);
}

export async function enterTextInField(label: string, value: string): Promise<void> {
  await scrollToTop();
  // Click directly on the EditText that has the matching label hint.
  // Use scrollIntoView to find it, then click it to focus.
  await $(`android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().className("android.widget.EditText").textContains("${label}"))`);
  let editText = await $(`android=new UiSelector().className("android.widget.EditText").textContains("${label}")`);
  if (!(await editText.isExisting())) {
    // Fallback: find the label TextView, click it, then find the focused EditText
    const labelEl = await $(`android=new UiSelector().text("${label}").className("android.widget.TextView")`);
    await labelEl.waitForExist({ timeout: 5_000 });
    await labelEl.click();
    await browser.pause(300);
    editText = await $('android=new UiSelector().focused(true).className("android.widget.EditText")');
  } else {
    await editText.click();
    await browser.pause(300);
  }
  await editText.waitForExist({ timeout: 3_000 });
  await editText.clearValue();
  if (value) {
    await editText.setValue(value);
  }
  await driver.pressKeyCode(4);
  await browser.pause(300);
}

export async function clearField(label: string): Promise<void> {
  await scrollToTop();
  await $(`android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().className("android.widget.EditText").textContains("${label}"))`);
  let editText = await $(`android=new UiSelector().className("android.widget.EditText").textContains("${label}")`);
  if (!(await editText.isExisting())) {
    const labelEl = await $(`android=new UiSelector().text("${label}").className("android.widget.TextView")`);
    await labelEl.waitForExist({ timeout: 5_000 });
    await labelEl.click();
    await browser.pause(300);
    editText = await $('android=new UiSelector().focused(true).className("android.widget.EditText")');
  } else {
    await editText.click();
    await browser.pause(300);
  }
  await editText.waitForExist({ timeout: 3_000 });
  await editText.clearValue();
  await driver.pressKeyCode(4);
  await browser.pause(300);
}

export async function isShareQrButtonEnabled(): Promise<boolean> {
  const disabledBtn = await $('android=new UiSelector().enabled(false).childSelector(new UiSelector().text("Share QR"))');
  return !(await disabledBtn.isExisting());
}

export async function tapGenerateButton(): Promise<void> {
  await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Generate QR Code"))');
  const btn = await $('android=new UiSelector().clickable(true).childSelector(new UiSelector().text("Generate QR Code"))');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function isGenerateButtonEnabled(): Promise<boolean> {
  await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Generate QR Code"))');
  const disabledBtn = await $('android=new UiSelector().enabled(false).childSelector(new UiSelector().text("Generate QR Code"))');
  return !(await disabledBtn.isExisting());
}

export async function waitForQrResultScreen(): Promise<void> {
  const title = await $('android=new UiSelector().text("Your QR Code")');
  await title.waitForExist({ timeout: 10_000 });
}

export async function tapEditButton(): Promise<void> {
  const btn = await $('android=new UiSelector().clickable(true).childSelector(new UiSelector().text("Edit"))');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function isValidationErrorVisible(errorText: string): Promise<boolean> {
  const err = await $(`android=new UiSelector().text("${errorText}")`);
  return err.isExisting();
}

export async function tapScanTab(): Promise<void> {
  const btn = await $('android=new UiSelector().text("Scan")');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export const tapBackToScan = tapScanTab;

export async function tapHelpTab(): Promise<void> {
  const btn = await $('android=new UiSelector().text("Help")');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function waitForHelpScreen(): Promise<void> {
  const title = await $('android=new UiSelector().text("How to Use QRieux")');
  await title.waitForExist({ timeout: 10_000 });
}

export async function getSelectedType(): Promise<string> {
  // The dropdown trigger is a clickable View (not EditText) showing the selected type name.
  // Find it by checking which type label exists outside the dropdown menu.
  const typeLabels = ['Text', 'Website', 'Email', 'Phone', 'Wi-Fi'];
  for (const label of typeLabels) {
    const el = await $(`android=new UiSelector().text("${label}")`);
    if (await el.isExisting()) return label;
  }
  throw new Error('Could not determine selected QR type');
}

export async function waitForQrGenerated(): Promise<void> {
  await tapGenerateButton();
  await waitForQrResultScreen();
}

// --- Toast / flash / help / share helpers ---

export async function waitForToast(_text: string): Promise<void> {
  // Native Android toasts are invisible to UiAutomator2 on API 30+.
  // Just pause to let the toast appear/dismiss — the tap itself is the real assertion.
  console.warn(`[waitForToast] Android: skipped (UiAutomator2 limitation). Expected: "${_text}"`);
  await browser.pause(1500);
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
  const el = await $('android=new UiSelector().text("What are QR Codes?")');
  return el.isExisting();
}

export async function tapShareQrButton(): Promise<void> {
  const btn = await $('android=new UiSelector().clickable(true).childSelector(new UiSelector().text("Share QR"))');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function dismissShareSheet(): Promise<void> {
  await driver.pressKeyCode(4);
  await browser.pause(1000);
}

export async function selectWifiSecurity(securityName: string): Promise<void> {
  await scrollToTop();
  await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Security"))');
  await browser.pause(300);

  // Find the security dropdown by its current value text (Box-based, not EditText)
  const securityValues = ['WPA/WPA2', 'WEP', 'None'];
  for (const val of securityValues) {
    const dropdown = await $(`android=new UiSelector().text("${val}")`);
    if (await dropdown.isExisting()) {
      await dropdown.click();
      await browser.pause(500);
      const option = await $(`android=new UiSelector().text("${securityName}")`);
      await option.waitForExist({ timeout: 3_000 });
      await option.click();
      await browser.pause(500);
      return;
    }
  }
  throw new Error('Could not find Wi-Fi security dropdown');
}

export async function toggleWifiHidden(): Promise<void> {
  await scrollToTop();
  await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Hidden network"))');
  await browser.pause(300);
  // Compose Switch renders as a clickable View, not android.widget.Switch.
  // The "Hidden network" Row has the Switch as the first clickable element before the label.
  // Find by scrolling to "Hidden network" text, then click the clickable View just before it.
  const hiddenLabel = await $('android=new UiSelector().text("Hidden network")');
  await hiddenLabel.waitForExist({ timeout: 5_000 });
  // Get bounds of label to find the switch (to its left)
  const location = await hiddenLabel.getLocation();
  const size = await hiddenLabel.getSize();
  // The Switch is to the left of the label text, at roughly the same Y
  const switchX = Math.max(location.x - 50, 30);
  const switchY = location.y + Math.floor(size.height / 2);
  await driver.action('pointer', { parameters: { pointerType: 'touch' } })
    .move({ x: switchX, y: switchY })
    .down()
    .up()
    .perform();
  await browser.pause(300);
}

export async function shareVCardToApp(vcfContent: string): Promise<void> {
  const filePath = '/sdcard/Download/test-contact.vcf';
  // Clean up file and stale MediaStore entry from previous runs
  execSync(`adb shell rm -f ${filePath}`);
  execSync(
    `adb shell content delete --uri content://media/external/file --where "\\"_display_name='test-contact.vcf'\\""`,
  );
  const b64 = Buffer.from(vcfContent).toString('base64');
  execSync(`adb shell "echo '${b64}' | base64 -d > ${filePath}"`);
  // Trigger media scan so MediaStore indexes the file
  execSync(`adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://${filePath}"`);
  await browser.pause(2000);
  // Query MediaStore for content URI
  let mediaId: string | null = null;
  for (let attempt = 0; attempt < 3; attempt++) {
    const output = execSync(
      `adb shell content query --uri content://media/external/file --projection _id --where "\\"_display_name='test-contact.vcf'\\""`,
    ).toString().trim();
    const match = output.match(/_id=(\d+)/);
    if (match) {
      mediaId = match[1];
      break;
    }
    await browser.pause(1000);
  }
  if (!mediaId) {
    throw new Error('Could not find media URI for test-contact.vcf');
  }
  const contentUri = `content://media/external/file/${mediaId}`;
  execSync(
    `adb shell am start -a android.intent.action.SEND -t text/x-vcard -f 0x30000001 ` +
    `-d '${contentUri}' --eu android.intent.extra.STREAM '${contentUri}' ` +
    `-n net.hilson.qrieux.dev/net.hilson.qrieux.MainActivity`,
  );
  await browser.pause(2000);
}

export async function reactivateApp(): Promise<void> {
  await driver.activateApp('net.hilson.qrieux.dev');
  await browser.pause(1000);
}
