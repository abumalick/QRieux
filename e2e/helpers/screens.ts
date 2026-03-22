// Thin screen interaction helpers for Android
// All selectors are Android UiSelector syntax — iOS will need separate helpers.

export async function dismissOnboarding(): Promise<void> {
  const skipBtn = await $('android=new UiSelector().text("Skip")');
  if (await skipBtn.isExisting()) {
    await skipBtn.click();
    // Wait for scanner to appear after dismissing
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
  // Android system photo picker runs in a separate process.
  // Wait for picker UI to load.
  await browser.pause(3000);

  // Try selectors for the photo picker grid items:
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

export async function assertScanResultContains(expected: string): Promise<void> {
  const el = await $(`android=new UiSelector().textContains("${expected}")`);
  await el.waitForExist({ timeout: 5_000 });
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
