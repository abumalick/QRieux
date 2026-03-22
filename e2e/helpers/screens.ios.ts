// Thin screen interaction helpers for iOS (XCUITest selectors)

export async function dismissOnboarding(): Promise<void> {
  // Dismiss onboarding if showing
  const skipBtn = await $('-ios predicate string:label == "Skip"');
  if (await skipBtn.isExisting()) {
    await skipBtn.click();
    await browser.pause(1000);
  }

  // Handle app's own permission screen (shows before system alert)
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
  // iOS PHPickerViewController: wait for picker to load
  await browser.pause(3000);

  // Dismiss the "Private Access to Photos" banner if showing
  const dismissBanner = await $('-ios predicate string:name == "PXGSingleViewContainerView_AX"');
  if (await dismissBanner.isExisting()) {
    // Tap the X button on the banner
    const closeBanner = await $('-ios predicate string:label == "Close"');
    if (await closeBanner.isExisting()) {
      await closeBanner.click();
      await browser.pause(500);
    }
  }

  // Tap the first photo in the grid. PHPicker renders photos as XCUIElementTypeOther
  // without individual accessibility labels, so we tap by coordinate.
  // The grid starts below the nav bar + banner area.
  const { width } = await browser.getWindowSize();
  const gridX = Math.floor(width / 6); // center of first column (3-column grid)
  const gridY = 450; // below nav bar + "Private Access" banner

  await driver.performActions([{
    type: 'pointer',
    id: 'finger1',
    parameters: { pointerType: 'touch' },
    actions: [
      { type: 'pointerMove', duration: 0, x: gridX, y: gridY },
      { type: 'pointerDown', button: 0 },
      { type: 'pause', duration: 100 },
      { type: 'pointerUp', button: 0 },
    ],
  }]);
  await driver.releaseActions();
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
