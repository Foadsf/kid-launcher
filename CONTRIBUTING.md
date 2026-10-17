# Contributing

Thanks for helping. This launcher is for small children on old phones, and it stays useful by
staying small and safe:

- **Keep it simple.** Big pictures, few choices, nothing a child can wander off into. Features
  that add settings screens, widgets, web content or theming will probably be declined. Open an
  issue to discuss a feature first.
- **No permissions, ever.** No network, no storage, no analytics, no crash reporting, no ads.
  CI fails if the APK requests any permission.
- **API 19 stays supported.** No AndroidX, no Compose, no library dependencies. `lintDebug`
  fails on any call newer than API 19; keep it that way rather than suppressing it.
- **No proprietary dependencies.** Everything must be FLOSS and buildable from source, so the
  app can be packaged for F-Droid.
- **Pin versions exactly.** Bump the Gradle wrapper or AGP deliberately, one change per PR, and
  say why.
- **Touch targets stay ≥ 140 dp**, in portrait and landscape.
- **Artwork is drawn as code** (`tools/gen_icons.py`, `SkyView.java`). Do not add bitmap art
  unless it is CC0 or similar and recorded in `licenses/README.md`.

## Pull requests

1. Build with `./gradlew assembleDebug lintDebug`. CI runs the same on every PR.
2. Test on a real phone or an API 19 emulator if you can. `tools/verify_on_device.py` covers the
   tiles, the Games screen and the parent gate. Say which device and Android version you used,
   and check both orientations for layout changes.
3. Adding a game or tile? Add it to `KidApps.java` and its word to `values/strings.xml` *and*
   `values-nl/strings.xml`.

By contributing, you agree that your work is licensed under the GPL-3.0.
