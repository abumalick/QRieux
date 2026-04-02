import {
  dismissOnboarding,
  waitForScanner,
  tapHelpTab,
  waitForHelpScreen,
  tapScanTab,
} from '../helpers/screens.js';

describe('Help Screen', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await tapHelpTab();
    await waitForHelpScreen();
  });

  it('shows all help items in the list', async () => {
    const titles = [
      'What are QR Codes?',
      'How to Scan',
      'Scan from Photos',
      'Create & Share QR Codes',
      'Supported Formats',
    ];
    for (const title of titles) {
      const el = browser.isAndroid
        ? await $(`android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("${title}"))`)
        : await $(`-ios predicate string:label == "${title}"`);
      expect(await el.isExisting()).toBe(true);
    }
  });

  it('returns to scanner from help', async () => {
    await tapScanTab();
    await waitForScanner();
  });
});
