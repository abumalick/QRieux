import {
  dismissOnboarding,
  tapHistoryTab,
  waitForHistoryScreen,
  isHistoryEmpty,
  getHistoryEntryCount,
  tapHistoryEntry,
  swipeDeleteHistoryEntry,
  tapClearHistory,
  tapBackToHistory,
  tapCreateButton,
  waitForGeneratorScreen,
  enterTextInField,
  tapGenerateButton,
  waitForQrResultScreen,
  tapEditButton,
  isShareQrButtonEnabled,
  selectQrType,
} from '../helpers/screens.js';

describe('History', () => {
  before(async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await browser.pause(2000);
  });

  it('shows empty state initially', async () => {
    await tapHistoryTab();
    await waitForHistoryScreen();
    expect(await isHistoryEmpty()).toBe(true);
    expect(await getHistoryEntryCount()).toBe(0);
  });

  it('records a generated QR in history', async () => {
    await tapCreateButton();
    await waitForGeneratorScreen();
    await browser.pause(1000);
    await selectQrType('Website');
    await enterTextInField('Website address', 'https://example.com');
    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();

    await tapHistoryTab();
    await waitForHistoryScreen();
    expect(await isHistoryEmpty()).toBe(false);
    expect(await getHistoryEntryCount()).toBe(1);
  });

  it('opens QR result from history and can go back', async () => {
    await tapHistoryTab();
    await waitForHistoryScreen();
    await tapHistoryEntry(0);
    await browser.pause(3000);
    await waitForQrResultScreen();
    expect(await isShareQrButtonEnabled()).toBe(true);

    await tapBackToHistory();
    await waitForHistoryScreen();
  });

  it('edit from history opens Create tab', async () => {
    await tapHistoryTab();
    await waitForHistoryScreen();
    await tapHistoryEntry(0);
    await browser.pause(3000);
    await waitForQrResultScreen();

    // Edit switches to Create tab with form pre-filled
    await tapEditButton();
    await waitForGeneratorScreen();
    // Back from Create tab goes to history (Android) or stays on Create (iOS)
  });

  it('deletes entry by swiping', async () => {
    await tapHistoryTab();
    await waitForHistoryScreen();
    const before = await getHistoryEntryCount();
    expect(before).toBeGreaterThan(0);

    await swipeDeleteHistoryEntry(0);
    await browser.pause(500);
    expect(await getHistoryEntryCount()).toBe(before - 1);
  });

  it('clears all history', async () => {
    // Create an entry to clear
    await tapCreateButton();
    await waitForGeneratorScreen();
    await browser.pause(1000);
    await selectQrType('Email');
    await enterTextInField('Email address', 'test@example.com');
    await tapGenerateButton();
    await waitForQrResultScreen();
    await tapEditButton();

    await tapHistoryTab();
    await waitForHistoryScreen();
    expect(await getHistoryEntryCount()).toBeGreaterThanOrEqual(1);

    await tapClearHistory();
    await browser.pause(500);
    expect(await isHistoryEmpty()).toBe(true);
    expect(await getHistoryEntryCount()).toBe(0);
  });
});
