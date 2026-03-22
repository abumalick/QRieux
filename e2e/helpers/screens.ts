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
