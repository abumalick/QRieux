import { mkdirSync } from 'fs';
import path from 'path';
import { captureFailureArtifacts, setArtifactBaseDir } from './helpers/artifacts.js';

const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);

export const config: WebdriverIO.Config = {
  specs: [],
  capabilities: [],

  runner: 'local',
  maxInstances: 1,

  logLevel: 'debug',
  outputDir: './logs',
  bail: 0,

  waitforTimeout: 45_000,
  connectionRetryTimeout: 120_000,
  connectionRetryCount: 3,

  framework: 'mocha',
  mochaOpts: {
    ui: 'bdd',
    timeout: 3 * 60 * 1000, // 3 min — mobile tests are slow
  },

  reporters: [
    'spec',
    ['video', {
      saveAllVideos: false,
      videoSlowdownMultiplier: 3,
    }],
  ],

  services: [
    ['appium', {
      args: {
        relaxedSecurity: true,
        log: './logs/appium.log',
      },
    }],
  ],

  specFileRetries: 1,

  onPrepare() {
    mkdirSync('./logs', { recursive: true });
  },

  before(_capabilities: any, _specs: string[]) {
    const platformName = (driver.capabilities as any).platformName || 'unknown';
    const dir = path.resolve('./artifacts', `${timestamp}-${platformName.toLowerCase()}`);
    mkdirSync(dir, { recursive: true });
    setArtifactBaseDir(dir);
  },

  async afterTest(test: any, _context: any, { passed }: { error?: Error; result?: any; duration?: number; passed: boolean }) {
    if (!passed) {
      await captureFailureArtifacts(test.title);
    }
  },
};
