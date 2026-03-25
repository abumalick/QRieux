import { pushAllFixtures, cleanupFixtures } from '../helpers/qr-fixtures.js';
import {
  dismissOnboarding,
  waitForScanner,
  shareImageToApp,
  waitForToast,
} from '../helpers/screens.js';

describe('Error States', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await pushAllFixtures();
  });

  after(async () => {
    await cleanupFixtures();
  });

  it('shows error for image with no QR code', async () => {
    await shareImageToApp('no-qr');
    await waitForToast('No QR code found in image');
    await waitForScanner();
  });
});
