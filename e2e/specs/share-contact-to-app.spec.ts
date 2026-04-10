import {
  dismissOnboarding,
  waitForScanner,
  waitForGeneratorScreen,
  isGenerateButtonEnabled,
  getSelectedType,
  tapBackToScan,
  shareVCardToApp,
} from '../helpers/screens.js';

const TEST_VCARD = [
  'BEGIN:VCARD',
  'VERSION:3.0',
  'FN:John Doe',
  'N:Doe;John;;;',
  'TEL;TYPE=CELL:+1234567890',
  'EMAIL:john@example.com',
  'ORG:Acme Corp',
  'PHOTO;ENCODING=b;TYPE=JPEG:AAAA',
  'END:VCARD',
].join('\r\n');

describe('Share Contact to App', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();
  });

  it('opens QR generator with vCard pre-filled as Text type', async () => {
    await shareVCardToApp(TEST_VCARD);
    await waitForGeneratorScreen();

    expect(await getSelectedType()).toBe('Text');
    expect(await isGenerateButtonEnabled()).toBe(true);
  });

  it('navigates back to scanner after contact share', async () => {
    await tapBackToScan();
    await waitForScanner();
  });
});
