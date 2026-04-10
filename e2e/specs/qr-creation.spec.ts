import {
  dismissOnboarding,
  waitForScanner,
  tapCreateButton,
  waitForGeneratorScreen,
  enterTextInField,
  clearField,
  selectQrType,
  isShareQrButtonEnabled,
  isGenerateButtonEnabled,
  tapGenerateButton,
  waitForQrResultScreen,
  tapEditButton,
  isValidationErrorVisible,
  tapBackToScan,
} from '../helpers/screens.js';

describe('QR Creation', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
    await tapCreateButton();
    await waitForGeneratorScreen();
  });

  it('shows default state with disabled Generate button', async () => {
    expect(await isGenerateButtonEnabled()).toBe(false);
  });

  it('creates a Text QR code', async () => {
    await enterTextInField('Text', 'Hello World E2E');
    expect(await isGenerateButtonEnabled()).toBe(true);

    await tapGenerateButton();
    await waitForQrResultScreen();
    expect(await isShareQrButtonEnabled()).toBe(true);
    await tapEditButton();
  });

  it('switches to Website and generates QR', async () => {
    await selectQrType('Website');
    expect(await isGenerateButtonEnabled()).toBe(false);

    await enterTextInField('Website address', 'https://example.com');
    expect(await isGenerateButtonEnabled()).toBe(true);

    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('creates an Email QR code', async () => {
    await selectQrType('Email');
    await enterTextInField('Email address', 'test@example.com');
    expect(await isGenerateButtonEnabled()).toBe(true);

    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('shows validation error for invalid email', async () => {
    await enterTextInField('Email address', 'invalid-email');
    await browser.pause(500);
    expect(await isValidationErrorVisible('Enter a valid email address')).toBe(true);
    expect(await isGenerateButtonEnabled()).toBe(false);
  });

  it('creates a Phone QR code', async () => {
    await selectQrType('Phone');
    await enterTextInField('Phone number', '+1234567890');
    expect(await isGenerateButtonEnabled()).toBe(true);

    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('creates a Wi-Fi QR code', async () => {
    await selectQrType('Wi-Fi');
    expect(await isGenerateButtonEnabled()).toBe(false);

    await enterTextInField('Wi-Fi name', 'TestNetwork');
    await enterTextInField('Password', 'secret123');
    expect(await isGenerateButtonEnabled()).toBe(true);

    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();
  });

  it('shows validation for missing Wi-Fi name', async () => {
    await clearField('Wi-Fi name');
    await browser.pause(500);
    expect(await isValidationErrorVisible('Enter the Wi-Fi name')).toBe(true);
    expect(await isGenerateButtonEnabled()).toBe(false);
  });

  it('navigates back to scanner', async () => {
    await tapBackToScan();
    await waitForScanner();
  });
});
