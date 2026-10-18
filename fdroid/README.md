# F-Droid submission draft

[`io.github.foadsf.kidlauncher.yml`](io.github.foadsf.kidlauncher.yml) is the metadata file for
[fdroiddata](https://gitlab.com/fdroid/fdroiddata). It lives here only as a draft: the copy F-Droid
uses is the one merged into fdroiddata.

The build pins the full commit hash of tag `v0.3.0`. The `prebuild` installs platform
`android-35` and build-tools `37.0.0`, the same pair CI installs. Gradle 9.8.1 is in F-Droid's
[gradle-transparency-log](https://gitlab.com/fdroid/gradle-transparency-log), so `gradlew-fdroid`
accepts it. `AutoName` is set by hand because the app label comes from a Gradle `resValue`
(see `kidName` below), which F-Droid cannot read from the sources.

Store text and screenshots come from [`fastlane/`](../fastlane/metadata/android/en-US) in this
repo, not from fdroiddata.

## How it was checked (2026-10-17, fdroidserver 2.4.5)

- **`fdroid lint`** passes in a current fdroiddata checkout, and `fdroid rewritemeta` leaves the
  file byte-for-byte unchanged. As a control, a copy with `Categories: Kids` and
  `License: GPL-3.0` was rejected on both lines, so lint really checks those fields.
- **`fdroid build -l io.github.foadsf.kidlauncher`** built `0.3.0` from `24fe6e3` and produced a
  103 KB unsigned release APK. The source scanner found nothing; it only removed
  `gradle-wrapper.jar`, as it always does.
  The **first** local attempt failed in `gradle clean` with
  `Could not create service of type OutputFilesRepository … For input string: ""`. The rerun,
  with no change to the metadata or the source, built cleanly. The cause was not pinned down.
  One possibility: on that first run the build directory did not exist yet, so fdroidserver took
  `SOURCE_DATE_EPOCH` from the git log of the metadata file, which was still uncommitted. Running
  `gradle clean` with an empty `SOURCE_DATE_EPOCH` by hand did not reproduce the error, though.
- **The built APK**, read with `aapt2 dump`: package `io.github.foadsf.kidlauncher`, versionCode
  3, minSdk 19, label `Kid Launcher`, greeting `Hi there!`, **no permissions**. The release build
  is R8-minified while CI builds debug only, so the dex was checked too: every class named in
  the manifest and layouts (`HomeActivity`, `GamesActivity`, `SkyView`, `TileGrid`,
  `ParentGateView`) is still present.
  **Not done:** running this release APK on a device or emulator.

## Inclusion criteria

- **Licence:** GPL-3.0 (`LICENSE`). No file says "or later", so the SPDX tag is `GPL-3.0-only`.
- **No dependencies:** there are no library dependencies at all (`gradle/libs.versions.toml` has
  only the Android Gradle Plugin). `dependenciesInfo` is off, so the APK has no Google-encrypted
  dependency block.
- **No network, no tracking:** the manifest has no permissions, and there are no network calls
  in the source.
- **Bundled asset:** `app/src/main/assets/fonts/fredoka_semibold.ttf` is Fredoka, SIL OFL 1.1,
  recorded in [`licenses/`](../licenses/README.md). `tools/make_font.sh` rebuilds it from
  upstream google/fonts and checks the upstream file against a pinned SHA-256.
- **Fastlane:** title, short description, full description, `changelogs/3.txt`, and two
  540×960 phone screenshots. The short description was 83 characters at `v0.3.0`. F-Droid's
  summary limit is 80 (`char_limits` in fdroidserver), and `fdroid update` cuts anything longer
  without warning, so it would have shown as "…No netw". It is now 78. The fix is not in the
  `v0.3.0` tag, so the store will probably keep the truncated text until a release that includes it.
- **Anonymous build:** F-Droid does not pass `-PkidName`, so its build greets *Hi there!* and is
  labelled *Kid Launcher*.

## What a reviewer may ask about

- **Samsung tiles.** Camera, Photos, Music and Videos first look for Samsung's stock apps
  (`com.sec.android.app.camera`, …). Camera, Photos and Music fall back to any app that does the
  job; Videos has no fallback, so on a non-Samsung phone that tile is not shown. The app works
  without any of them, so this should not need the `NonFreeDep` anti-feature, but a reviewer
  may raise it.
- **Android 11+ package visibility.** The app targets SDK 35 and its manifest has no `<queries>`
  element. On Android 11 and later, `queryIntentActivities` only sees apps that are visible to
  this one, so tiles for other apps (the games in particular) may not appear. It was tested only
  on API 19. This is a functional issue, not an inclusion rule, but F-Droid will offer the app
  to every phone from Android 4.4 up.
- **Launcher limits.** Without root or device-owner rights it cannot block the notification
  shade or recents. The README says so; the store text could too.
- **Debug signing hook.** `app/build.gradle.kts` looks for an optional `debug.keystore`. The
  F-Droid build ignores it: fdroidserver strips `signingConfigs` and signs the release APK
  itself.

## Submitting

1. Fork https://gitlab.com/fdroid/fdroiddata and clone the fork.
2. Copy this file to `metadata/io.github.foadsf.kidlauncher.yml` on a new branch.
3. In the fdroiddata checkout, run `fdroid lint io.github.foadsf.kidlauncher` and
   `fdroid rewritemeta io.github.foadsf.kidlauncher`. Both should leave the file as it is.
4. Push the branch and open a merge request using the *App inclusion* template.

## Releasing a new version

Bump `versionCode` and `versionName` in `app/build.gradle.kts`, add
`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, commit, and push a `vX.Y.Z` tag.
With `UpdateCheckMode: Tags` and `AutoUpdateMode: Version`, F-Droid picks the tag up by itself.
