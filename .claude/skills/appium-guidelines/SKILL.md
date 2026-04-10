---
name: appium-guidelines
description: Best practices for writing stable, maintainable Appium + WebdriverIO e2e tests. Use proactively whenever writing, modifying, debugging, or reviewing e2e test code in the `e2e/` directory — including specs, screen helpers, selectors, waits, scrolling, or test structure. Also use when adding UI features that will need e2e coverage, or when investigating flaky/failing e2e tests.
---

# Appium + WebdriverIO Best Practices

When writing or modifying e2e tests in `e2e/`, follow these guidelines for stability and maintainability.

---

## Architecture

### Platform Dispatcher Pattern
All specs import from `e2e/helpers/screens.ts` which routes to platform-specific helpers. Never use platform-specific selectors directly in spec files.

- **Specs** (`e2e/specs/`) — platform-agnostic, import only from `screens.ts`
- **Screen helpers** (`e2e/helpers/screens.android.ts`, `screens.ios.ts`) — platform-specific selectors and interactions
- **Dispatcher** (`e2e/helpers/screens.ts`) — routes calls based on `driver.isAndroid`

When adding a new interaction:
1. Add the function to both `screens.android.ts` and `screens.ios.ts`
2. Add the dispatcher in `screens.ts`
3. Use it from specs via the dispatcher

### Compose Multiplatform Constraint
`testTagsAsResourceId` does NOT work in Compose Multiplatform. Use text/label/accessibility-based selectors instead.

---

## Selectors (in order of preference)

### 1. Accessibility ID (best)
```typescript
// Cross-platform, stable, fast
const btn = await $('~Pick photo from gallery');
```
Use `~` prefix. Maps to `contentDescription` (Android) / `accessibilityIdentifier` (iOS). Fastest and most stable.

### 2. Platform predicate / UiSelector
```typescript
// Android — UiSelector
const el = await $('android=new UiSelector().text("Scan Again")');

// iOS — predicate string
const el = await $('-ios predicate string:label == "Scan Again"');
```
Use text/label matching. Stable as long as the text doesn't change (and it's localized via string resources so changes are intentional).

### 3. iOS class chain (when needed)
```typescript
const nav = await $('-ios class chain:**/XCUIElementTypeNavigationBar');
```
Use for system UI elements that lack labels.

### 4. XPath (AVOID)
Never use XPath. It's slow, fragile, and breaks across OS versions. If you can't find an element without XPath, add an accessibility label to the app code instead.

---

## Waits

### Use `waitForExist` with explicit timeouts
```typescript
await element.waitForExist({ timeout: 10_000 });
```
Always pass an explicit timeout. Default timeouts vary and can cause unexpected behavior.

### Use `waitUntil` for complex conditions
```typescript
await browser.waitUntil(
  async () => {
    const btn = await $('...');
    return btn.isEnabled();
  },
  { timeout: 10_000, timeoutMsg: 'Button never became enabled' }
);
```
Always include `timeoutMsg` — it's the only clue when debugging CI failures.

### `browser.pause()` — use sparingly and document WHY
```typescript
// Wait for Android photo picker animation to settle
await browser.pause(3000);
```
Hard pauses are sometimes unavoidable (animations, system UIs, media scanner). When used:
- Add a comment explaining what you're waiting for
- Keep durations as short as possible
- Prefer `waitForExist`/`waitUntil` when an element can be checked

### Never combine redundant waits
```typescript
// BAD — redundant
await btn.waitForExist({ timeout: 5_000 });
await expect(btn).toBeDisplayed();

// GOOD — one or the other
await btn.waitForExist({ timeout: 5_000 });
await btn.click();

// OR for assertion
await expect(btn).toBeDisplayed();
```

---

## Test Structure

### Setup/teardown in `before`/`after`
```typescript
before(async () => {
  await browser.pause(3000); // app launch animation
  await dismissOnboarding();
  await waitForScanner();
});
```
Dismiss onboarding and wait for the app to be ready before any test.

### Each test should leave app in a known state
If a test navigates away from scanner, navigate back at the end:
```typescript
await tapScanAgain();
await waitForScanner();
```
This prevents test ordering dependencies.

### Keep tests focused
One behavior per `it()` block. Don't chain multiple unrelated assertions.

---

## Scrolling

### Android — UiScrollable
```typescript
// Scroll element into view
await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("Share QR"))');

// Scroll to top
await $('android=new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(3)');
```

### iOS — manual swipe via pointer actions
```typescript
const { width, height } = await browser.getWindowSize();
const centerX = Math.floor(width / 2);
await driver.performActions([{
  type: 'pointer', id: 'finger1', parameters: { pointerType: 'touch' },
  actions: [
    { type: 'pointerMove', duration: 0, x: centerX, y: Math.floor(height * 0.6) },
    { type: 'pointerDown', button: 0 },
    { type: 'pointerMove', duration: 300, x: centerX, y: Math.floor(height * 0.2) },
    { type: 'pointerUp', button: 0 },
  ],
}]);
await driver.releaseActions();
```
Always call `releaseActions()` after `performActions()`.

---

## Common Pitfalls

### 1. Stale element references
Re-query elements after navigation or scroll. Don't store a reference and reuse it after the screen changed.

### 2. System UI differences
Photo picker, share sheet, and permission dialogs vary by OS version. Use fallback selector strategies:
```typescript
const selectors = ['sel1', 'sel2', 'sel3'];
for (const sel of selectors) {
  const el = await $(sel);
  if (await el.isExisting()) { await el.click(); return; }
}
throw new Error('Could not find element');
```

### 3. Keyboard dismissal
On iOS, the Compose keyboard can block elements. Dismiss it before scrolling or tapping outside the keyboard area:
```typescript
await driver.execute('mobile: tap', { x: 200, y: safeY });
await browser.pause(500);
```

### 4. Share sheet dismissal
- **Android**: `driver.pressKeyCode(4)` (back button)
- **iOS**: swipe down gesture (tap-to-dismiss is unreliable on iOS 26+)

### 5. Toast detection
Android toasts are invisible to UiAutomator2 on API 30+. Don't assert on toast text on Android — use a pause and trust the action.

### 6. Media scanner lag on Android
After pushing files via `adb`, the MediaStore may not index them immediately. Retry with short pauses:
```typescript
for (let attempt = 0; attempt < 3; attempt++) {
  const output = execSync('adb shell content query ...').toString();
  if (output.match(/_id=(\d+)/)) break;
  await browser.pause(1000);
}
```

---

## Debugging Failures

Tests auto-capture on failure (configured in `wdio.shared.conf.ts`):
- Screenshot
- Page source XML
- Device logs

Saved to `e2e/artifacts/<timestamp>-<platform>/<test>/`. Check the XML source first — it shows the full element tree with all attributes.

### Useful commands during development
```bash
# Run a single spec
just e2e-android --spec specs/scan-from-gallery.spec.ts

# Clean artifacts
just e2e-clean
```
