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

  // Gallery scan tests don't work on iOS simulator yet (PHPicker + Vision limitations)
  specs: ['./specs/app-launch.spec.ts'],

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
};
