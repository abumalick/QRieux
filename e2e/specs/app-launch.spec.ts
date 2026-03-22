import path from 'path';
import { getArtifactBaseDir } from '../helpers/artifacts.js';
import { dismissOnboarding, waitForScanner } from '../helpers/screens.js';

describe('QRieux App', () => {
  it('launches and shows scanner after onboarding', async () => {
    await browser.pause(3000);
    await dismissOnboarding();
    await waitForScanner();

    // Verify scanner UI elements are present (not just the instruction text)
    const hasGallery = await $('~Pick photo from gallery').isExisting();
    expect(hasGallery).toBe(true);

    const dir = getArtifactBaseDir();
    if (dir) {
      await browser.saveScreenshot(path.join(dir, 'launch-screenshot.png'));
    }
  });
});
