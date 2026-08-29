import path from 'path';
import { config as sharedConfig } from './wdio.shared.conf.js';

export const config: WebdriverIO.Config = {
  ...sharedConfig,

  specs: ['./specs/**/*.spec.ts'],
  exclude: ['./specs/screenshots.spec.ts'],

  capabilities: [{
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:app': process.env.E2E_APP_PATH ||
      path.join(process.cwd(), '..', 'composeApp', 'build', 'outputs', 'apk', 'debug', 'composeApp-debug.apk'),
    // Pin the target so a phone plugged into the machine can never be picked up
    // instead of the emulator the suite is calibrated against.
    'appium:udid': process.env.E2E_DEVICE_SERIAL || 'emulator-5554',
    'appium:appPackage': 'net.hilson.qrieux.dev',
    'appium:appActivity': 'net.hilson.qrieux.MainActivity',
    'appium:autoGrantPermissions': true,
    'appium:clearDeviceLogsOnStart': true,
    'appium:newCommandTimeout': 240,
    'appium:noReset': false,
  }],
};
