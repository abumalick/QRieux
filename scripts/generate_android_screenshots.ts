#!/usr/bin/env bun
/**
 * Generate Android screenshots via Appium + WebdriverIO.
 *
 * Creates AVDs, boots emulators (headless), pushes background images,
 * runs wdio per locale, optimizes PNGs.
 *
 * Usage: bun scripts/generate_android_screenshots.ts [--skip-build] [--locale=en-US]
 */

import { mkdir, readFile, writeFile, rm } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { homedir } from 'node:os';

// --- Paths & config ---
const SCRIPT_DIR = import.meta.dir;
const PROJECT_DIR = join(SCRIPT_DIR, '..');
const E2E_DIR = join(PROJECT_DIR, 'e2e');
const METADATA_DIR = join(PROJECT_DIR, 'fastlane/metadata/android');
const APK_PATH = join(PROJECT_DIR, 'composeApp/build/outputs/apk/debug/composeApp-debug.apk');

const ANDROID_HOME = process.env.ANDROID_HOME ?? join(homedir(), 'Library/Android/sdk');
const JAVA_HOME = process.env.JAVA_HOME ?? '/Applications/Android Studio.app/Contents/jbr/Contents/Home';
const ANDROID_AVD_HOME = process.env.ANDROID_AVD_HOME ?? join(homedir(), '.config/.android/avd');

const AVDMANAGER = join(ANDROID_HOME, 'cmdline-tools/latest/bin/avdmanager');
const EMULATOR = join(ANDROID_HOME, 'emulator/emulator');
const ADB = join(ANDROID_HOME, 'platform-tools/adb');

const SYSTEM_IMAGE = 'system-images;android-35;google_apis;arm64-v8a';
const BACKGROUNDS_DIR = join(SCRIPT_DIR, 'screenshot_backgrounds');
const LOG_DIR = join(PROJECT_DIR, '.tmp/screenshot-runs');
const BG_DEVICE_PATH = '/data/local/tmp/screenshot_bg.png';

type Locale = { lang: string; locale: string; dir: string };
type Device = { avdName: string; deviceProfile: string; screenshotSubdir: string; bgFile: string };

const LOCALES: Locale[] = [
  { lang: 'en', locale: 'US', dir: 'en-US' },
  { lang: 'zh', locale: 'CN', dir: 'zh-CN' },
  { lang: 'hi', locale: 'IN', dir: 'hi-IN' },
  { lang: 'es', locale: 'ES', dir: 'es-ES' },
  { lang: 'fr', locale: 'FR', dir: 'fr-FR' },
  { lang: 'ar', locale: 'SA', dir: 'ar' },
  { lang: 'bn', locale: 'BD', dir: 'bn-BD' },
  { lang: 'pt', locale: 'BR', dir: 'pt-BR' },
  { lang: 'ru', locale: 'RU', dir: 'ru-RU' },
  { lang: 'ja', locale: 'JP', dir: 'ja-JP' },
  { lang: 'id', locale: 'ID', dir: 'id' },
  { lang: 'de', locale: 'DE', dir: 'de-DE' },
  { lang: 'ur', locale: 'PK', dir: 'ur' },
  { lang: 'tr', locale: 'TR', dir: 'tr-TR' },
  { lang: 'ko', locale: 'KR', dir: 'ko-KR' },
  { lang: 'vi', locale: 'VN', dir: 'vi' },
  { lang: 'it', locale: 'IT', dir: 'it-IT' },
  { lang: 'th', locale: 'TH', dir: 'th' },
  { lang: 'ta', locale: 'IN', dir: 'ta-IN' },
  { lang: 'sw', locale: 'KE', dir: 'sw' },
];

const DEVICES: Device[] = [
  { avdName: 'QRieux-Screenshots-Phone', deviceProfile: 'pixel_7', screenshotSubdir: 'phoneScreenshots', bgFile: 'phone_bg.png' },
  { avdName: 'QRieux-Screenshots-Tablet', deviceProfile: 'pixel_tablet', screenshotSubdir: 'tenInchScreenshots', bgFile: 'tablet_bg.png' },
];

// --- Parse args ---
const args = process.argv.slice(2);
const skipBuild = args.includes('--skip-build');
// --locale supports comma-separated list and can be passed multiple times
const localeArgs = args
  .filter(a => a.startsWith('--locale='))
  .flatMap(a => a.split('=')[1].split(','))
  .filter(Boolean);
const deviceArg = args.find(a => a.startsWith('--device='))?.split('=')[1];

let locales = LOCALES;
if (localeArgs.length > 0) {
  const requested = new Set(localeArgs);
  locales = LOCALES.filter(l => requested.has(l.dir));
  const found = new Set(locales.map(l => l.dir));
  const missing = localeArgs.filter(l => !found.has(l));
  if (missing.length > 0) {
    console.error(`ERROR: Locale(s) not found: ${missing.join(', ')}. Available:`);
    for (const l of LOCALES) console.error(`  ${l.dir}`);
    process.exit(1);
  }
}

let devices = DEVICES;
if (deviceArg) {
  devices = DEVICES.filter(d => d.deviceProfile === deviceArg || d.avdName.toLowerCase().includes(deviceArg.toLowerCase()));
  if (devices.length === 0) {
    console.error(`ERROR: Device '${deviceArg}' not found. Available:`);
    for (const d of DEVICES) console.error(`  ${d.deviceProfile}`);
    process.exit(1);
  }
}

// --- Logging ---
const stamp = () => new Date().toISOString().slice(11, 19);
const log = (msg: string) => console.log(`[${stamp()}] ${msg}`);
const section = (msg: string) => console.log(`\n[${stamp()}] ==> ${msg}`);

// --- Helpers ---
const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));
const decode = (b: Uint8Array | null | undefined) => (b ? new TextDecoder().decode(b) : '');

function baseEnv(extra: Record<string, string> = {}): Record<string, string> {
  // Filter out undefined so Bun.spawn is happy
  const env: Record<string, string> = {};
  for (const [k, v] of Object.entries(process.env)) {
    if (v !== undefined) env[k] = v;
  }
  env.JAVA_HOME = JAVA_HOME;
  env.ANDROID_HOME = ANDROID_HOME;
  env.ANDROID_AVD_HOME = ANDROID_AVD_HOME;
  Object.assign(env, extra);
  return env;
}

function runSync(cmd: string[]): { stdout: string; stderr: string; exitCode: number } {
  const r = Bun.spawnSync({ cmd, env: baseEnv() });
  return { stdout: decode(r.stdout), stderr: decode(r.stderr), exitCode: r.exitCode };
}

// --- Preflight ---
for (const tool of [AVDMANAGER, EMULATOR, ADB]) {
  if (!existsSync(tool)) {
    console.error(`ERROR: ${tool} not found`);
    process.exit(1);
  }
}

// --- Global state for cleanup ---
let emulatorProc: ReturnType<typeof Bun.spawn> | null = null;
let emulatorSerial: string | null = null;
let cleanupRan = false;

// --- AVD lifecycle ---
async function createAvd(device: Device): Promise<void> {
  const list = runSync([AVDMANAGER, 'list', 'avd', '-c']);
  if (list.stdout.split('\n').some(l => l.trim() === device.avdName)) {
    log(`  AVD ${device.avdName} already exists`);
    return;
  }
  log(`  Creating AVD: ${device.avdName} (${device.deviceProfile})`);
  const proc = Bun.spawn({
    cmd: [AVDMANAGER, 'create', 'avd', '-n', device.avdName, '-k', SYSTEM_IMAGE, '-d', device.deviceProfile, '--force'],
    stdin: new Blob(['no\n']),
    stdout: 'inherit',
    stderr: 'inherit',
    env: baseEnv(),
  });
  await proc.exited;
  if (proc.exitCode !== 0) throw new Error(`Failed to create AVD ${device.avdName}`);

  // Enable GPU — avdmanager defaults to hw.gpu.enabled=no which crashes the emulator
  const configPath = join(ANDROID_AVD_HOME, `${device.avdName}.avd/config.ini`);
  if (existsSync(configPath)) {
    const content = await readFile(configPath, 'utf-8');
    await writeFile(configPath, content.replace(/hw\.gpu\.enabled=no/, 'hw.gpu.enabled=yes'));
  }
}

async function startEmulator(device: Device): Promise<void> {
  log(`  Starting emulator: ${device.avdName}`);
  emulatorProc = Bun.spawn({
    cmd: [
      EMULATOR, '-avd', device.avdName,
      '-no-audio', '-no-boot-anim', '-no-snapshot', '-no-window',
      '-gpu', 'host', '-memory', '4096',
      '-dns-server', '8.8.8.8,8.8.4.4',
    ],
    env: baseEnv(),
    stdout: 'ignore',
    stderr: 'ignore',
  });

  log('  Waiting for emulator serial...');
  emulatorSerial = null;
  for (let i = 0; i < 30; i++) {
    const { stdout } = runSync([ADB, 'devices']);
    const match = stdout.match(/^(emulator-\d+)\s/m);
    if (match) { emulatorSerial = match[1]; break; }
    await sleep(1000);
  }
  if (!emulatorSerial) throw new Error('Could not detect emulator serial');
  process.env.ANDROID_SERIAL = emulatorSerial;
  log(`  Emulator serial: ${emulatorSerial}`);

  log('  Waiting for device...');
  runSync([ADB, '-s', emulatorSerial, 'wait-for-device']);

  log('  Device online, waiting for boot_completed...');
  for (let i = 1; i <= 120; i++) {
    const { stdout } = runSync([ADB, '-s', emulatorSerial, 'shell', 'getprop', 'sys.boot_completed']);
    if (stdout.trim() === '1') {
      log(`  Emulator booted (${i}s), settling...`);
      // Android system services (package manager, launcher) aren't ready immediately
      // after sys.boot_completed=1. Appium's io.appium.settings install can silently
      // fail or race. Wait until the package manager is responsive.
      for (let j = 0; j < 15; j++) {
        const check = runSync([ADB, '-s', emulatorSerial, 'shell', 'pm', 'list', 'packages', '-f', 'android']);
        if (check.stdout.includes('package:')) { log(`  System settled (${j}s)`); return; }
        await sleep(1000);
      }
      log('  System settle timeout — continuing anyway');
      return;
    }
    await sleep(1000);
  }
  throw new Error('Emulator did not boot in 120s');
}

async function stopEmulator(): Promise<void> {
  if (emulatorSerial) {
    try { runSync([ADB, '-s', emulatorSerial, 'emu', 'kill']); } catch {}
  }
  if (emulatorProc) {
    // Give 5s for graceful exit
    for (let i = 0; i < 5; i++) {
      if (emulatorProc.exitCode !== null) break;
      await sleep(1000);
    }
    if (emulatorProc.exitCode === null) {
      try { emulatorProc.kill('SIGKILL'); } catch {}
      try { await emulatorProc.exited; } catch {}
    }
  }
  emulatorProc = null;
  emulatorSerial = null;
  delete process.env.ANDROID_SERIAL;
}

async function cleanup(): Promise<void> {
  if (cleanupRan) return;
  cleanupRan = true;
  section('Cleaning up...');
  try { await stopEmulator(); } catch (e) { console.error('stopEmulator error:', e); }
  for (const device of DEVICES) {
    try { runSync([AVDMANAGER, 'delete', 'avd', '-n', device.avdName]); } catch {}
  }
}

// --- Wdio runner ---
async function runLocale(
  device: Device,
  locale: Locale,
  idx: number,
  total: number,
): Promise<boolean> {
  log(`--- [${idx}/${total}] ${locale.dir} (${device.deviceProfile}) ---`);

  const outputDir = join(METADATA_DIR, locale.dir, 'images', device.screenshotSubdir);
  await mkdir(outputDir, { recursive: true });

  // Remove stale screenshots from previous runs
  for await (const file of new Bun.Glob('*.png').scan({ cwd: outputDir })) {
    await rm(join(outputDir, file));
  }

  const logPath = join(LOG_DIR, `${device.avdName}-${locale.dir}.log`);

  const proc = Bun.spawn({
    cmd: ['npx', 'wdio', 'run', 'wdio.screenshots.android.conf.ts'],
    cwd: E2E_DIR,
    env: baseEnv({
      E2E_APP_PATH: APK_PATH,
      SCREENSHOT_OUTPUT_DIR: outputDir,
      SCREENSHOT_PREFIX: '',
      SCREENSHOT_LANG: locale.lang,
      SCREENSHOT_LOCALE: locale.locale,
      SCREENSHOT_BACKGROUND: BG_DEVICE_PATH,
    }),
    stdout: 'pipe',
    stderr: 'pipe',
  });

  const [out, err] = await Promise.all([
    new Response(proc.stdout).text(),
    new Response(proc.stderr).text(),
  ]);
  await proc.exited;
  const full = out + err;
  await writeFile(logPath, full);

  if (proc.exitCode !== 0) {
    console.error(`  FAILED: exit ${proc.exitCode}`);
    console.error(full.split('\n').slice(-30).join('\n'));
    console.error(`  Full log: ${logPath}`);
    return false;
  }
  log(`  OK (log: ${logPath})`);
  return true;
}

// --- Main ---
async function main(): Promise<void> {
  try {
    if (localeArgs.length > 0) log(`Running for locale(s): ${localeArgs.join(', ')}`);

    if (!skipBuild) {
      section('Building debug APK (clean, no-cache)...');
      // Must match `just android-debug`: Gradle's build cache survives clean and
      // serves stale dex files after Kotlin changes (see AGENTS.md Build cache gotcha).
      const proc = Bun.spawn({
        cmd: ['./gradlew', 'clean', ':composeApp:assembleDebug', '--no-build-cache'],
        cwd: PROJECT_DIR,
        env: baseEnv(),
        stdout: 'inherit',
        stderr: 'inherit',
      });
      await proc.exited;
      if (proc.exitCode !== 0) throw new Error('Gradle build failed');
    }

    if (!existsSync(APK_PATH)) {
      throw new Error(`APK not found at ${APK_PATH}. Run without --skip-build first.`);
    }

    await mkdir(LOG_DIR, { recursive: true });

    const total = locales.length;
    section(`Generating screenshots for ${total} locales × ${devices.length} devices`);

    const failures: Array<{ device: string; locale: string }> = [];

    for (const device of devices) {
      console.log('');
      section(`Device: ${device.deviceProfile} (${device.avdName})`);

      await createAvd(device);
      await startEmulator(device);

      // Push background image
      if (emulatorSerial) {
        runSync([ADB, '-s', emulatorSerial, 'push', join(BACKGROUNDS_DIR, device.bgFile), BG_DEVICE_PATH]);
      }

      let idx = 0;
      for (const locale of locales) {
        idx++;
        const ok = await runLocale(device, locale, idx, total);
        if (!ok) failures.push({ device: device.deviceProfile, locale: locale.dir });
      }

      await stopEmulator();
    }

    // Optimize PNGs (skipped if SKIP_PNGQUANT=1 for raw debugging)
    const hasPngquant = !process.env.SKIP_PNGQUANT && runSync(['which', 'pngquant']).exitCode === 0;
    if (hasPngquant) {
      console.log('');
      section('Optimizing PNGs...');
      let count = 0;
      for await (const file of new Bun.Glob('*/images/*Screenshots/*.png').scan({ cwd: METADATA_DIR })) {
        const path = join(METADATA_DIR, file);
        runSync(['pngquant', '--force', '--quality=65-80', '--ext', '.png', path]);
        count++;
      }
      log(`Optimized ${count} PNG(s).`);
    } else {
      log('pngquant not found — skipping optimization');
    }

    console.log('');
    section(`Screenshots saved to ${METADATA_DIR}`);

    if (failures.length > 0) {
      console.error('\nFailures:');
      for (const f of failures) console.error(`  ${f.device} / ${f.locale}`);
      throw new Error(`${failures.length} locale(s) failed`);
    }
  } finally {
    await cleanup();
  }
}

// --- Signal handlers (backstop; main's finally is the primary cleanup path) ---
let sigintCount = 0;
process.on('SIGINT', async () => {
  sigintCount++;
  if (sigintCount > 1) {
    console.error('\n[force exit]');
    process.exit(130);
  }
  console.error('\n[SIGINT] cleaning up...');
  await cleanup();
  process.exit(130);
});
process.on('SIGTERM', async () => {
  await cleanup();
  process.exit(143);
});

main()
  .then(() => process.exit(0))
  .catch(err => {
    console.error('\nERROR:', err instanceof Error ? err.message : err);
    process.exit(1);
  });
