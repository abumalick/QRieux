import {
  dismissOnboarding,
  waitForScanner,
  tapFlashButton,
  getFlashButtonLabel,
  tapHelpButton,
  isOnboardingVisible,
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

  it('help button shows onboarding', async () => {
    await tapHelpButton();
    await browser.pause(1000);
    expect(await isOnboardingVisible()).toBe(true);
  });

  it('can dismiss onboarding and return to scanner', async () => {
    await dismissOnboarding();
    await waitForScanner();
  });
});
