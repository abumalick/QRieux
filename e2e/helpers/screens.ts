// Platform dispatcher — routes to Android or iOS helpers based on driver

import * as android from './screens.android.js';
import * as ios from './screens.ios.js';

const useAndroid = () => driver.isAndroid;

export const dismissOnboarding = () =>
  useAndroid() ? android.dismissOnboarding() : ios.dismissOnboarding();

export const waitForScanner = () =>
  useAndroid() ? android.waitForScanner() : ios.waitForScanner();

export const tapGalleryButton = () =>
  useAndroid() ? android.tapGalleryButton() : ios.tapGalleryButton();

export const pickImageFromGallery = () =>
  useAndroid() ? android.pickImageFromGallery() : ios.pickImageFromGallery();

export const waitForScanResult = () =>
  useAndroid() ? android.waitForScanResult() : ios.waitForScanResult();

export const getScanResultText = () =>
  useAndroid() ? android.getScanResultText() : ios.getScanResultText();

export const assertScanResultContains = (expected: string) =>
  useAndroid() ? android.assertScanResultContains(expected) : ios.assertScanResultContains(expected);

export const isActionButtonVisible = (label: string) =>
  useAndroid() ? android.isActionButtonVisible(label) : ios.isActionButtonVisible(label);

export const tapActionButton = (label: string) =>
  useAndroid() ? android.tapActionButton(label) : ios.tapActionButton(label);

export const tapScanAgain = () =>
  useAndroid() ? android.tapScanAgain() : ios.tapScanAgain();

// --- QR Creation ---

export const tapCreateButton = () =>
  useAndroid() ? android.tapCreateButton() : ios.tapCreateButton();

export const waitForGeneratorScreen = () =>
  useAndroid() ? android.waitForGeneratorScreen() : ios.waitForGeneratorScreen();

export const selectQrType = (typeName: string) =>
  useAndroid() ? android.selectQrType(typeName) : ios.selectQrType(typeName);

export const enterTextInField = (label: string, value: string) =>
  useAndroid() ? android.enterTextInField(label, value) : ios.enterTextInField(label, value);

export const clearField = (label: string) =>
  useAndroid() ? android.clearField(label) : ios.clearField(label);

export const isShareQrButtonEnabled = () =>
  useAndroid() ? android.isShareQrButtonEnabled() : ios.isShareQrButtonEnabled();

export const tapGenerateButton = () =>
  useAndroid() ? android.tapGenerateButton() : ios.tapGenerateButton();

export const isGenerateButtonEnabled = () =>
  useAndroid() ? android.isGenerateButtonEnabled() : ios.isGenerateButtonEnabled();

export const waitForQrResultScreen = () =>
  useAndroid() ? android.waitForQrResultScreen() : ios.waitForQrResultScreen();

export const tapEditButton = () =>
  useAndroid() ? android.tapEditButton() : ios.tapEditButton();

export const isValidationErrorVisible = (errorText: string) =>
  useAndroid() ? android.isValidationErrorVisible(errorText) : ios.isValidationErrorVisible(errorText);

export const tapBackToScan = () =>
  useAndroid() ? android.tapBackToScan() : ios.tapBackToScan();

export const getSelectedType = () =>
  useAndroid() ? android.getSelectedType() : ios.getSelectedType();

// --- Share from external app ---

export const shareImageToApp = (fixtureName: string) =>
  useAndroid() ? android.shareImageToApp(fixtureName) : ios.shareImageToApp(fixtureName);

export const shareTextToApp = (text: string) =>
  useAndroid() ? android.shareTextToApp(text) : ios.shareTextToApp(text);

export const shareVCardToApp = (vcfContent: string) =>
  useAndroid() ? android.shareVCardToApp(vcfContent) : ios.shareVCardToApp(vcfContent);

// --- Toast / flash / help / share ---

export const waitForToast = (text: string) =>
  useAndroid() ? android.waitForToast(text) : ios.waitForToast(text);

export const tapFlashButton = () =>
  useAndroid() ? android.tapFlashButton() : ios.tapFlashButton();

export const getFlashButtonLabel = () =>
  useAndroid() ? android.getFlashButtonLabel() : ios.getFlashButtonLabel();

export const tapHelpButton = () =>
  useAndroid() ? android.tapHelpButton() : ios.tapHelpButton();

export const isOnboardingVisible = () =>
  useAndroid() ? android.isOnboardingVisible() : ios.isOnboardingVisible();

// --- Tab navigation ---

export const tapScanTab = () =>
  useAndroid() ? android.tapScanTab() : ios.tapScanTab();

export const tapHelpTab = () =>
  useAndroid() ? android.tapHelpTab() : ios.tapHelpTab();

export const waitForHelpScreen = () =>
  useAndroid() ? android.waitForHelpScreen() : ios.waitForHelpScreen();

export const tapShareQrButton = () =>
  useAndroid() ? android.tapShareQrButton() : ios.tapShareQrButton();

export const dismissShareSheet = () =>
  useAndroid() ? android.dismissShareSheet() : ios.dismissShareSheet();

export const selectWifiSecurity = (securityName: string) =>
  useAndroid() ? android.selectWifiSecurity(securityName) : ios.selectWifiSecurity(securityName);

export const toggleWifiHidden = () =>
  useAndroid() ? android.toggleWifiHidden() : ios.toggleWifiHidden();

export const reactivateApp = () =>
  useAndroid() ? android.reactivateApp() : ios.reactivateApp();

