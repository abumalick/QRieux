---
description: Prepare and upload a new release to Play Store production track
allowed-tools: Read, Edit, Write, Bash, Glob, Grep
---

# Release Process

Execute the full release process for QRieux app:

## 1. Gather Changes Since Last Release

First, identify what changed:
- Check git tags: `git describe --tags --abbrev=0 2>/dev/null`
- If no tags, use the first commit or ask user for reference point
- Get commits since last release: `git log <last-tag>..HEAD --oneline`
- Summarize the user-facing changes (ignore internal/refactoring commits)

## 2. Determine Version Numbers

Read current version from `composeApp/build.gradle.kts`:
- Find `versionCode` and `versionName`
- Increment `versionCode` by 1
- For `versionName`:
  - Patch release (bug fixes): increment last number (1.1.1 → 1.1.2)
  - Minor release (new features): increment middle number (1.1.1 → 1.2.0)
  - Ask user if unclear

## 3. Create Changelogs

Create changelog files for the NEW versionCode in all supported locales:
`en-US`, `zh-CN`, `hi-IN`, `es-ES`, `fr-FR`, `ar`, `bn-BD`, `pt-BR`, `ru-RU`, `ja-JP`, `id`, `de-DE`, `ur`, `tr-TR`, `ko-KR`, `vi`, `it-IT`, `th`, `ta-IN`, `sw`

Path: `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`

Format: Bullet points starting with "•", max 500 chars per file.
Translate appropriately for each language.

## 4. Update Version

Edit `composeApp/build.gradle.kts`:
- `versionCode = <new-code>`
- `versionName = "<new-name>"`

Edit `iosApp/iosApp/Info.plist` to keep the platforms in step:
- `CFBundleShortVersionString` = `<new-name>`
- `CFBundleVersion` = `<new-code>`

Edit `fastlane/fdroid/net.hilson.qrieux.yml` so the F-Droid recipe matches:
- `versionName`, `versionCode`, `CurrentVersion`, `CurrentVersionCode`
- `commit` = the full hash of the release commit (filled in after step 6)

## 5. Build Release Bundle

Run: `just android-release`

Ensure build succeeds before proceeding.

## 6. Commit and Tag

Stage and commit with message: `v<versionName> (versionCode <code>)`

Tag and push both, so the tag exists on GitHub before the release is created:

```bash
git tag -a v<versionName> -m "v<versionName> (versionCode <code>)"
git push && git push origin v<versionName>
```

## 7. Publish the APK for F-Droid

**Do not skip this.** F-Droid rebuilds the tagged commit and compares it against
the APK published here. Without a matching asset at the exact URL below,
F-Droid cannot publish the update at all — and the failure is silent from this
side. The URL is fixed by `Binaries` in `fastlane/fdroid/net.hilson.qrieux.yml`:

```
https://github.com/abumalick/QRieux/releases/download/v%v/QRieux-%v.apk
```

Build the APK **from the tag**, not from the working tree, so it matches what
F-Droid builds:

```bash
git stash -u                      # only if the tree is dirty
git checkout v<versionName>
just android-release-apk
just verify-fdroid-apk            # must pass; see below
```

`verify-fdroid-apk` fails if the APK carries a signing block F-Droid rejects.
The usual cause is AGP's dependency-metadata block; `dependenciesInfo` in
`composeApp/build.gradle.kts` disables it, so a failure here means that setting
was lost. Do not publish an APK that fails this check.

Confirm the signature matches the key F-Droid expects
(`AllowedAPKSigningKeys` in the recipe):

```bash
apksigner verify --print-certs composeApp/build/outputs/apk/release/composeApp-release.apk
```

Then publish, naming the asset exactly `QRieux-<versionName>.apk`:

```bash
cp composeApp/build/outputs/apk/release/composeApp-release.apk .tmp/QRieux-<versionName>.apk
gh release create v<versionName> --repo abumalick/QRieux \
  --title "QRieux <versionName>" \
  --notes-file fastlane/metadata/android/en-US/changelogs/<versionCode>.txt \
  .tmp/QRieux-<versionName>.apk
git checkout main
```

No merge request to fdroiddata is needed for an update: `UpdateCheckMode: Tags`
picks the release up from the tag.

## 8. Upload to Production

Run: `just android-production`

## 9. Summary

Report to user:
- Version released
- Changelog summary
- Play Store upload status
- GitHub release URL and confirmation the APK asset is attached
- Remind about git push if remote exists
