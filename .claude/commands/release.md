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

Create changelog files for the NEW versionCode in:
- `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
- `fastlane/metadata/android/fr-FR/changelogs/<versionCode>.txt`
- `fastlane/metadata/android/ar/changelogs/<versionCode>.txt`

Format: Bullet points starting with "•", max 500 chars per file.
Translate appropriately for each language (French for fr-FR, Arabic for ar).

## 4. Update Version in build.gradle.kts

Edit `composeApp/build.gradle.kts` to update:
- `versionCode = <new-code>`
- `versionName = "<new-name>"`

## 5. Build Release Bundle

Run: `just android-release`

Ensure build succeeds before proceeding.

## 6. Commit Changes

Stage and commit with message: `v<versionName> (versionCode <code>)`

Create a git tag: `git tag v<versionName>`

## 7. Upload to Production

Run: `just android-production`

## 8. Summary

Report to user:
- Version released
- Changelog summary
- Upload status
- Remind about git push if remote exists
