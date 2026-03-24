import {
  dismissOnboarding,
  waitForScanner,
  waitForGeneratorScreen,
  waitForQrGenerated,
  isShareQrButtonEnabled,
  getSelectedType,
  tapBackToScan,
  shareTextToApp,
} from '../helpers/screens.js';

describe('Share Text to App', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
  });

  it('opens QR generator with plain text pre-filled as Text type', async () => {
    await shareTextToApp('Hello from E2E test');
    await waitForGeneratorScreen();

    expect(await getSelectedType()).toBe('Text');
    await waitForQrGenerated();
    expect(await isShareQrButtonEnabled()).toBe(true);
  });

  it('navigates back to scanner after text share', async () => {
    await tapBackToScan();
    await waitForScanner();
  });

  it('opens QR generator with URL pre-filled as Website type', async () => {
    await shareTextToApp('https://example.com/test');
    await waitForGeneratorScreen();

    expect(await getSelectedType()).toBe('Website');
    await waitForQrGenerated();
    expect(await isShareQrButtonEnabled()).toBe(true);
  });

  it('navigates back to scanner after URL share', async () => {
    await tapBackToScan();
    await waitForScanner();
  });
});
