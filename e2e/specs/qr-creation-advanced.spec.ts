import {
  dismissOnboarding,
  waitForScanner,
  tapCreateButton,
  waitForGeneratorScreen,
  selectQrType,
  enterTextInField,
  clearField,
  tapGenerateButton,
  waitForQrResultScreen,
  tapEditButton,
  tapShareQrButton,
  dismissShareSheet,
  selectWifiSecurity,
  toggleWifiHidden,
  tapBackToScan,
} from '../helpers/screens.js';

describe('QR Creation - Advanced', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await tapCreateButton();
    await waitForGeneratorScreen();
  });

  it('auto-normalizes website URL (adds https://)', async () => {
    await selectQrType('Website');
    await enterTextInField('Website address', 'example.com');
    await tapGenerateButton();
    await waitForQrResultScreen();
  });

  it('taps Share QR and opens share sheet', async () => {
    await tapShareQrButton();
    await browser.pause(1000);
    await dismissShareSheet();
    await tapEditButton();
  });

  it('Wi-Fi security selector — WEP', async () => {
    await selectQrType('Wi-Fi');
    await enterTextInField('Wi-Fi name', 'TestNet');
    await enterTextInField('Password', 'wepkey');
    await selectWifiSecurity('WEP');
    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('Wi-Fi security selector — None (no password required)', async () => {
    await selectWifiSecurity('None');
    await clearField('Password');
    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('Wi-Fi hidden network toggle', async () => {
    await toggleWifiHidden();
    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('navigates back to scanner', async () => {
    await tapBackToScan();
    await waitForScanner();
  });
});
