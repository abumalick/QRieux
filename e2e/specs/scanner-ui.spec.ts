import {
  dismissOnboarding,
  waitForScanner,
  tapFlashButton,
  getFlashButtonLabel,
  tapHelpTab,
  waitForHelpScreen,
  tapScanTab,
} from '../helpers/screens.js';

describe('Scanner UI', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
  });

  it('flash toggle changes label', async () => {
    await tapFlashButton();
    expect(await getFlashButtonLabel()).toBe('Turn off flash');
    await tapFlashButton();
    expect(await getFlashButtonLabel()).toBe('Turn on flash');
  });

  it('help tab shows help screen', async () => {
    await tapHelpTab();
    await waitForHelpScreen();
  });

  it('can return to scanner from help tab', async () => {
    await tapScanTab();
    await waitForScanner();
  });
});
