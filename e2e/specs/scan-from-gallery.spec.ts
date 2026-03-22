import { pushAllFixtures, cleanupFixtures } from '../helpers/qr-fixtures.js';
import {
  dismissOnboarding,
  waitForScanner,
  tapGalleryButton,
  pickImageFromGallery,
  waitForScanResult,
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

    expect(await isActionButtonVisible('Scan Again')).toBe(true);

    const hasCopy = await isActionButtonVisible('Copy');
    const hasOpenBrowser = await isActionButtonVisible('Open in Browser');
    const hasSendEmail = await isActionButtonVisible('Send Email');
    const hasCall = await isActionButtonVisible('Call');
    const hasConnectWifi = await isActionButtonVisible('Connect to WiFi');
    expect(hasCopy || hasOpenBrowser || hasSendEmail || hasCall || hasConnectWifi).toBe(true);

    await tapScanAgain();
    await waitForScanner();
  });

  it('can scan again after dismissing result', async () => {
    await tapGalleryButton();
    await pickImageFromGallery();
    await waitForScanResult();

    expect(await isActionButtonVisible('Scan Again')).toBe(true);

    await tapScanAgain();
    await waitForScanner();
  });
});
