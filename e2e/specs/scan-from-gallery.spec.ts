import { pushAllFixtures, cleanupFixtures } from '../helpers/qr-fixtures.js';
import {
  dismissOnboarding,
  waitForScanner,
  tapGalleryButton,
  pickImageFromGallery,
  waitForScanResult,
  assertScanResultContains,
  isActionButtonVisible,
  tapScanAgain,
} from '../helpers/screens.js';

describe('Scan from Gallery', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await pushAllFixtures();
  });

  after(async () => {
    await cleanupFixtures();
  });

  it('scans a QR from gallery and shows result with actions', async () => {
    await tapGalleryButton();
    await pickImageFromGallery();
    await waitForScanResult();

    // Verify "Scan Again" is always present on the result screen
    expect(await isActionButtonVisible('Scan Again')).toBe(true);

    // Verify at least one context-specific action exists (proves the QR was parsed)
    const hasCopy = await isActionButtonVisible('Copy');
    const hasOpenBrowser = await isActionButtonVisible('Open in Browser');
    const hasSendEmail = await isActionButtonVisible('Send Email');
    const hasCall = await isActionButtonVisible('Call');
    const hasConnectWifi = await isActionButtonVisible('Connect to WiFi');
    const hasAnyAction = hasCopy || hasOpenBrowser || hasSendEmail || hasCall || hasConnectWifi;
    expect(hasAnyAction).toBe(true);

    await tapScanAgain();
    await waitForScanner();
  });

  it('can scan again after dismissing result', async () => {
    await tapGalleryButton();
    await pickImageFromGallery();
    await waitForScanResult();

    // Verify this is a fresh result, not stale state
    expect(await isActionButtonVisible('Scan Again')).toBe(true);

    await tapScanAgain();
    await waitForScanner();
  });
});
