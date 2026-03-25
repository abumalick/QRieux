import { pushAllFixtures, cleanupFixtures } from '../helpers/qr-fixtures.js';
import {
  dismissOnboarding,
  waitForScanner,
  waitForScanResult,
  tapActionButton,
  tapScanAgain,
  shareImageToApp,
  waitForToast,
  dismissShareSheet,
  reactivateApp,
} from '../helpers/screens.js';

describe('Action Buttons', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await pushAllFixtures();
  });

  after(async () => {
    await cleanupFixtures();
  });

  describe('Copy action', () => {
    it('copies URL and shows toast', async () => {
      await shareImageToApp('url-https');
      await waitForScanResult();
      await tapActionButton('Copy');
      await waitForToast('Copied to clipboard');
    });

    it('copies email and shows toast', async () => {
      await tapScanAgain();
      await waitForScanner();
      await shareImageToApp('email-mailto');
      await waitForScanResult();
      await tapActionButton('Copy');
      await waitForToast('Copied to clipboard');
    });

    it('copies phone and shows toast', async () => {
      await tapScanAgain();
      await waitForScanner();
      await shareImageToApp('phone-tel');
      await waitForScanResult();
      await tapActionButton('Copy');
      await waitForToast('Copied to clipboard');
    });

    it('copies plain text and shows toast', async () => {
      await tapScanAgain();
      await waitForScanner();
      await shareImageToApp('text-hello');
      await waitForScanResult();
      await tapActionButton('Copy');
      await waitForToast('Copied to clipboard');
    });
  });

  describe('Share action', () => {
    it('opens share sheet for URL', async () => {
      await tapScanAgain();
      await waitForScanner();
      await shareImageToApp('url-https');
      await waitForScanResult();
      await tapActionButton('Share');
      await browser.pause(1000);
      await dismissShareSheet();
    });
  });

  describe('External app actions', () => {
    it('Open in Browser — tap and return', async () => {
      await tapScanAgain();
      await waitForScanner();
      await shareImageToApp('url-https');
      await waitForScanResult();
      await tapActionButton('Open in Browser');
      await browser.pause(2000);
      await reactivateApp();
    });

    it('Send Email — tap and return', async () => {
      // Reset state: reactivateApp may leave scan result or scanner visible
      await shareImageToApp('email-mailto');
      await waitForScanResult();
      await tapActionButton('Send Email');
      await browser.pause(2000);
      await reactivateApp();
    });

    it('Call — tap and return', async () => {
      await shareImageToApp('phone-tel');
      await waitForScanResult();
      await tapActionButton('Call');
      await browser.pause(2000);
      await reactivateApp();
    });
  });

  describe('WiFi actions', () => {
    it('Connect to WiFi — tap without crashing', async () => {
      await shareImageToApp('wifi');
      await waitForScanResult();
      await tapActionButton('Connect to WiFi');
      // Android opens WiFi settings activity; iOS shows connecting toast
      await browser.pause(2000);
      await reactivateApp();
    });

    it('Copy Password — tap without crashing', async () => {
      // Pause to let any previous snackbar (WiFi connect result) dismiss
      await browser.pause(3000);
      await shareImageToApp('wifi');
      await waitForScanResult();
      await tapActionButton('Copy Password');
      await waitForToast('Copied to clipboard');
    });
  });
});
