import { pushAllFixtures, cleanupFixtures } from '../helpers/qr-fixtures.js';
import {
  dismissOnboarding,
  waitForScanner,
  waitForScanResult,
  assertScanResultContains,
  isActionButtonVisible,
  tapActionButton,
  tapScanAgain,
  shareImageToApp,
  reactivateApp,
} from '../helpers/screens.js';

describe('vCard Scanning', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await pushAllFixtures();
  });

  after(async () => {
    await cleanupFixtures();
  });

  it('shows contact fields in scan result', async () => {
    await shareImageToApp('vcard');
    await waitForScanResult();
    await assertScanResultContains('John Doe');
    await assertScanResultContains('+1234567890');
    await assertScanResultContains('john@example.com');
  });

  it('shows Add to Contacts and conditional action buttons', async () => {
    expect(await isActionButtonVisible('Add to Contacts')).toBe(true);
    expect(await isActionButtonVisible('Call')).toBe(true);
    expect(await isActionButtonVisible('Send Email')).toBe(true);
    expect(await isActionButtonVisible('Share')).toBe(true);
  });

  it('taps Add to Contacts without crashing', async () => {
    await tapActionButton('Add to Contacts');
    await browser.pause(2000);
    await reactivateApp();
  });

  it('returns to scanner via Scan Again', async () => {
    await shareImageToApp('vcard');
    await waitForScanResult();
    await tapScanAgain();
    await waitForScanner();
  });
});
