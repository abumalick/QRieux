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

async function scrollToShareButton() {
  // Share QR button is below the fold — scroll into view
  await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Share QR"))');
  // The Compose Button renders as a clickable View parent with a child TextView.
  // Check enabled on the parent View, not the child TextView.
  const btn = await $('android=new UiSelector().clickable(true).childSelector(new UiSelector().text("Share QR"))');
  await btn.waitForExist({ timeout: 5_000 });
  return btn;
}

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
  const title = await $('android=new UiSelector().text("Create QR Code")');
  await title.waitForExist({ timeout: 10_000 });
}

export async function selectQrType(typeName: string): Promise<void> {
  await scrollToTop();
  // Dropdown is the first EditText (ExposedDropdownMenuBox)
  const dropdown = await $('android=new UiSelector().className("android.widget.EditText").instance(0)');
  await dropdown.waitForExist({ timeout: 5_000 });
  await dropdown.click();
  await browser.pause(500);

  const option = await $(`android=new UiSelector().text("${typeName}")`);
  await option.waitForExist({ timeout: 3_000 });
  await option.click();
  await browser.pause(500);
}

export async function enterTextInField(label: string, value: string): Promise<void> {
  // Scroll to beginning first, then forward to the label (scrollIntoView only scrolls forward)
  await scrollToTop();
  await $(`android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("${label}").className("android.widget.TextView"))`);

  const labelEl = await $(`android=new UiSelector().text("${label}").className("android.widget.TextView")`);
  await labelEl.waitForExist({ timeout: 5_000 });
  await labelEl.click();
  await browser.pause(300);

  const editText = await $('android=new UiSelector().focused(true).className("android.widget.EditText")');
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
  await $(`android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("${label}").className("android.widget.TextView"))`);

  const labelEl = await $(`android=new UiSelector().text("${label}").className("android.widget.TextView")`);
  await labelEl.waitForExist({ timeout: 5_000 });
  await labelEl.click();
  await browser.pause(300);

  const editText = await $('android=new UiSelector().focused(true).className("android.widget.EditText")');
  await editText.waitForExist({ timeout: 3_000 });
  await editText.clearValue();
  await driver.pressKeyCode(4);
  await browser.pause(300);
}

export async function isShareQrButtonEnabled(): Promise<boolean> {
  await scrollToShareButton();
  // The Compose Button is a clickable View with enabled=false when disabled.
  // Check if a disabled parent with "Share QR" child exists.
  const disabledBtn = await $('android=new UiSelector().enabled(false).childSelector(new UiSelector().text("Share QR"))');
  return !(await disabledBtn.isExisting());
}

export async function isPreviewHintVisible(): Promise<boolean> {
  const hint = await $('android=new UiSelector().textContains("Fill in the form")');
  return hint.isExisting();
}

export async function isValidationErrorVisible(errorText: string): Promise<boolean> {
  const err = await $(`android=new UiSelector().text("${errorText}")`);
  return err.isExisting();
}

export async function tapBackToScan(): Promise<void> {
  await scrollToTop();
  const btn = await $('~Back to Scan');
  await btn.waitForExist({ timeout: 5_000 });
  await btn.click();
}

export async function getSelectedType(): Promise<string> {
  const dropdown = await $('android=new UiSelector().className("android.widget.EditText").instance(0)');
  await dropdown.waitForExist({ timeout: 5_000 });
  return dropdown.getText();
}

export async function waitForQrGenerated(): Promise<void> {
  await browser.waitUntil(
    async () => {
      await scrollToShareButton();
      const disabledBtn = await $('android=new UiSelector().enabled(false).childSelector(new UiSelector().text("Share QR"))');
      return !(await disabledBtn.isExisting());
    },
    { timeout: 10_000, timeoutMsg: 'QR code was not generated in time' }
  );
}
