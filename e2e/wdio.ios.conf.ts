import path from 'path';
import { config as sharedConfig } from './wdio.shared.conf.js';

export const config: WebdriverIO.Config = {
  ...sharedConfig,

  // iOS specs not yet implemented — helpers use Android UiSelector syntax.
  // When iOS tests are added, create specs/ios/ with iOS-specific selectors.
  specs: [],

  capabilities: [{
    platformName: 'iOS',
    'appium:automationName': 'XCUITest',
    'appium:app': process.env.E2E_APP_PATH ||
      path.join(process.cwd(), '..', 'iosApp', 'build', 'Build', 'Products',
        'Debug-iphonesimulator', 'iosApp.app'),
    'appium:bundleId': 'net.hilson.qrieux',
    'appium:deviceName': process.env.E2E_DEVICE_NAME || 'iPhone 16 Pro',
    'appium:platformVersion': process.env.E2E_PLATFORM_VERSION || '18.5',
    'appium:autoDismissAlerts': true,
    'appium:newCommandTimeout': 240,
    'appium:noReset': false,
  }],
};
