import path from 'path';
import { config as sharedConfig } from './wdio.shared.conf.js';

export const config: WebdriverIO.Config = {
  ...sharedConfig,

  specs: ['./specs/**/*.spec.ts'],

  capabilities: [{
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:app': process.env.E2E_APP_PATH ||
      path.join(process.cwd(), '..', 'composeApp', 'build', 'outputs', 'apk', 'debug', 'composeApp-debug.apk'),
    'appium:appPackage': 'net.hilson.qrieux.dev',
    'appium:appActivity': 'net.hilson.qrieux.MainActivity',
    'appium:autoGrantPermissions': true,
    'appium:clearDeviceLogsOnStart': true,
    'appium:newCommandTimeout': 240,
    'appium:noReset': false,
  }],
};
