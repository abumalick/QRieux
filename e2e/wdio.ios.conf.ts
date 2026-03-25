import { execSync } from 'child_process';
import { config as sharedConfig } from './wdio.shared.conf.js';

function findIosAppPath(): string {
  if (process.env.E2E_APP_PATH) return process.env.E2E_APP_PATH;
  const deviceName = process.env.E2E_DEVICE_NAME || 'iPhone 16 Pro';
  const buildDir = execSync(
    `xcodebuild -scheme iosApp -project ../iosApp/iosApp.xcodeproj ` +
    `-destination 'platform=iOS Simulator,name=${deviceName}' ` +
    `-configuration Debug -showBuildSettings 2>/dev/null | ` +
    `grep -m1 "BUILT_PRODUCTS_DIR" | awk '{print $3}'`
  ).toString().trim();

  if (!buildDir) {
    throw new Error(
      'Could not determine iOS app path. Build the app first or set E2E_APP_PATH.'
    );
  }
  return `${buildDir}/iosApp.app`;
}

export const config: WebdriverIO.Config = {
  ...sharedConfig,

  specs: [
    './specs/app-launch.spec.ts',
    './specs/qr-creation.spec.ts',
    './specs/scan-from-gallery.spec.ts',
    './specs/scan-from-share.spec.ts',
    './specs/share-text-to-app.spec.ts',
    './specs/scan-vcard.spec.ts',
    './specs/action-buttons.spec.ts',
    './specs/scanner-ui.spec.ts',
    './specs/qr-creation-advanced.spec.ts',
    './specs/error-states.spec.ts',
  ],

  capabilities: [{
    platformName: 'iOS',
    'appium:automationName': 'XCUITest',
    'appium:app': findIosAppPath(),
    'appium:bundleId': 'net.hilson.qrieux',
    'appium:deviceName': process.env.E2E_DEVICE_NAME || 'iPhone 16 Pro',
    'appium:platformVersion': process.env.E2E_PLATFORM_VERSION || '26.2',
    'appium:autoAcceptAlerts': true,
    'appium:newCommandTimeout': 240,
    'appium:noReset': false,
  }],

  onPrepare() {
    if (typeof sharedConfig.onPrepare === 'function') sharedConfig.onPrepare({} as any, [] as any);
    // Grant full photo library access before tests start so
    // UIImagePickerController shows all photos without the limited-access banner
    // Also grant for .dev suffix (debug builds)
    for (const bundleId of ['net.hilson.qrieux', 'net.hilson.qrieux.dev']) {
      try { execSync(`xcrun simctl privacy booted grant photos ${bundleId}`); }
      catch (e) { console.warn(`[ios] Failed to grant photo access for ${bundleId}: ${e}`); }
    }
  },
};
