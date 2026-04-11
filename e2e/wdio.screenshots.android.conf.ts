import { execSync } from 'child_process';
import path from 'path';
import { config as sharedConfig } from './wdio.shared.conf.js';

export const config: WebdriverIO.Config = {
  ...sharedConfig,

  specs: ['./specs/screenshots.spec.ts'],

  capabilities: [{
    platformName: 'Android',
    'appium:automationName': 'UiAutomator2',
    'appium:app': process.env.E2E_APP_PATH ||
      path.join(process.cwd(), '..', 'composeApp', 'build', 'outputs', 'apk', 'debug', 'composeApp-debug.apk'),
    'appium:appPackage': 'net.hilson.qrieux.dev',
    'appium:appActivity': 'net.hilson.qrieux.MainActivity',
    'appium:udid': process.env.ANDROID_SERIAL || undefined,
    'appium:autoGrantPermissions': true,
    'appium:newCommandTimeout': 240,
    'appium:noReset': false,
    ...(process.env.DEVICE_PIN ? { 'appium:unlockType': 'pin', 'appium:unlockKey': process.env.DEVICE_PIN } : {}),
    'appium:language': process.env.SCREENSHOT_LANG || 'en',
    'appium:locale': process.env.SCREENSHOT_LOCALE || 'US',
    'appium:optionalIntentArguments':
      `--ez DEMO_CAMERA true${process.env.SCREENSHOT_BACKGROUND ? ` --es SCREENSHOT_BACKGROUND ${process.env.SCREENSHOT_BACKGROUND}` : ''}`,
  }],

  specFileRetries: 0,

  reporters: ['spec'],

  async before(_capabilities: any, _specs: string[]) {
    if (typeof sharedConfig.before === 'function') {
      await (sharedConfig.before as Function)(_capabilities, _specs);
    }
    // Enable Android demo mode for clean status bar
    const serial = process.env.ANDROID_SERIAL;
    const adb = serial ? `adb -s ${serial}` : 'adb';
    try {
      execSync(`${adb} shell settings put global sysui_demo_allowed 1`);
      execSync(`${adb} shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941`);
      execSync(`${adb} shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false`);
      execSync(`${adb} shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4`);
      execSync(`${adb} shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4`);
      execSync(`${adb} shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false`);
      // Disable animations
      execSync(`${adb} shell settings put global window_animation_scale 0`);
      execSync(`${adb} shell settings put global transition_animation_scale 0`);
      execSync(`${adb} shell settings put global animator_duration_scale 0`);
    } catch (e) {
      console.warn('[android] Failed to set demo mode:', e);
    }
  },

  async after() {
    // Restore animations and exit demo mode
    const serial = process.env.ANDROID_SERIAL;
    const adb = serial ? `adb -s ${serial}` : 'adb';
    try {
      execSync(`${adb} shell am broadcast -a com.android.systemui.demo -e command exit`);
      execSync(`${adb} shell settings put global window_animation_scale 1`);
      execSync(`${adb} shell settings put global transition_animation_scale 1`);
      execSync(`${adb} shell settings put global animator_duration_scale 1`);
    } catch (e) {
      console.warn('[android] Failed to restore settings:', e);
    }
  },
};
