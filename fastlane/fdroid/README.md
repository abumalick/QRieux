# F-Droid submission

F-Droid builds apps from source on its own infrastructure. The recipe lives in
F-Droid's `fdroiddata` repository, not here — `net.hilson.qrieux.yml` in this
directory is the copy to submit, kept alongside the app so it stays in sync.

## Why the scanner uses ZXing

F-Droid's inclusion policy rejects proprietary dependencies. ML Kit ships as a
closed-source binary, so barcode decoding uses ZXing, which was already a
dependency for QR generation. Keep it that way — reintroducing ML Kit would
remove the app from F-Droid.

## Store listing

F-Droid reads `fastlane/metadata/android/<locale>/` from this repository
directly, so titles, descriptions, changelogs and screenshots need no
duplication here. Changes to them are picked up automatically on the next
build.

`fdroid lint` reads those directory names as BCP-47 language tags, which are
not always the codes the Play Store uses. If it rejects a locale, alias the
directory rather than renaming it — Play Store uploads depend on the current
names.

## Submitting

The build recipe pins a git tag, so the tag must already contain the release
being submitted.

```bash
git clone git@gitlab.com:<your-fork>/fdroiddata.git
cd fdroiddata
git checkout -b net.hilson.qrieux
cp <this-repo>/fastlane/fdroid/net.hilson.qrieux.yml metadata/
git commit -am "New App: net.hilson.qrieux"
git push origin net.hilson.qrieux
```

Then open a merge request against `fdroiddata`. GitLab CI runs `fdroid lint`
and `fdroid build` on the branch; both must pass before review. Expect the app
to appear in the repository roughly 24-48h after the merge.

## Releasing a new version afterwards

`UpdateCheckMode: Tags` means F-Droid picks up new releases on its own from
signed git tags — no further merge requests are needed for routine updates.

## Signing

F-Droid signs with its own key, so the Play Store and F-Droid builds are not
interchangeable: users cannot upgrade from one to the other without
reinstalling. The release build only wires up the Play Store signing config
when `key.properties` is present, which keeps F-Droid's unsigned build working.
