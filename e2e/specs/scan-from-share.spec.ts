import { pushAllFixtures, cleanupFixtures } from '../helpers/qr-fixtures.js';
import {
  dismissOnboarding,
  waitForScanner,
  waitForScanResult,
  assertScanResultContains,
  isActionButtonVisible,
  tapScanAgain,
  shareImageToApp,
} from '../helpers/screens.js';

describe('Scan from Shared Image', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await pushAllFixtures();
  });

  after(async () => {
    await cleanupFixtures();
  });

  it('scans a URL QR code shared from another app', async () => {
    await shareImageToApp('url-https');
    await waitForScanResult();
    await assertScanResultContains('https://example.com');
    expect(await isActionButtonVisible('Open in Browser')).toBe(true);
  });

  it('scans an email QR code shared from another app', async () => {
    await tapScanAgain();
    await waitForScanner();
    await shareImageToApp('email-mailto');
    await waitForScanResult();
    await assertScanResultContains('test@example.com');
    expect(await isActionButtonVisible('Send Email')).toBe(true);
  });

  it('returns to scanner after dismissing shared scan result', async () => {
    await tapScanAgain();
    await waitForScanner();
  });
});
