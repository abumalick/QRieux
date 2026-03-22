import { mkdirSync, writeFileSync } from 'fs';
import path from 'path';

let artifactBaseDir = '';

export function setArtifactBaseDir(dir: string) {
  artifactBaseDir = dir;
}

export function getArtifactBaseDir(): string {
  return artifactBaseDir;
}

export async function captureFailureArtifacts(testName: string): Promise<void> {
  if (!artifactBaseDir) return;

  const sanitized = testName.replace(/[^a-zA-Z0-9_-]/g, '-').replace(/-+/g, '-').toLowerCase();
  const dir = path.join(artifactBaseDir, sanitized);
  mkdirSync(dir, { recursive: true });

  // Screenshot
  try {
    await browser.saveScreenshot(path.join(dir, 'screenshot.png'));
  } catch (e) {
    console.error('Failed to capture screenshot:', e);
  }

  // Page source (UI hierarchy)
  try {
    const source = await browser.getPageSource();
    writeFileSync(path.join(dir, 'page-source.xml'), source, 'utf-8');
  } catch (e) {
    console.error('Failed to capture page source:', e);
  }

  // Device logs (logcat for Android, syslog for iOS)
  try {
    const logType = driver.isAndroid ? 'logcat' : 'syslog';
    const logs = await driver.getLogs(logType);
    const logText = logs.map((entry: any) => {
      const ts = entry.timestamp ? new Date(entry.timestamp).toISOString() : '';
      return `${ts} ${entry.message}`;
    }).join('\n');
    writeFileSync(path.join(dir, 'device.log'), logText, 'utf-8');
  } catch (e) {
    console.error('Failed to capture device logs:', e);
  }

  console.log(`Failure artifacts saved to: ${dir}`);
}
