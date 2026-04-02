import {
  dismissOnboarding,
  waitForScanner,
  tapCreateButton,
  waitForGeneratorScreen,
  tapScanTab,
  tapHelpTab,
  waitForHelpScreen,
} from '../helpers/screens.js';

describe('Bottom Navigation', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
  });

  it('navigates to Create tab', async () => {
    await tapCreateButton();
    await waitForGeneratorScreen();
  });

  it('navigates back to Scan tab', async () => {
    await tapScanTab();
    await waitForScanner();
  });

  it('navigates to Help tab', async () => {
    await tapHelpTab();
    await waitForHelpScreen();
  });

  it('navigates from Help back to Scan', async () => {
    await tapScanTab();
    await waitForScanner();
  });
});
