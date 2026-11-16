# PolishEasy

An Android app for learning Polish in short daily sessions.

## Download

The newest builds of the app are always at the same links:

- **[PolishEasy-debug.apk](https://github.com/cdealmeida3/PolishEasy/releases/download/latest/PolishEasy-debug.apk)**, the debug build. It installs as "PolishEasy Debug".
- **[PolishEasy-release.apk](https://github.com/cdealmeida3/PolishEasy/releases/download/latest/PolishEasy-release.apk)**, the signed, minified release build.

Every update to the default branch rebuilds them and replaces the `latest` release. The same release also holds `PolishEasy-release.aab`, the file to upload to Google Play, and `PolishEasy-release-mapping.txt` for reading crash stack traces. See [docs/release.md](docs/release.md).

All debug builds are signed with the same debug key, so a new debug APK installs over the previous one without uninstalling. The debug build has its own application id, `com.polisheasy.app.debug`, so it can sit next to the release build (`com.polisheasy.app`) on one phone.

## Build

```
./gradlew :engine:test :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Every build of the app first checks the Authored Content and fails if anything in it is broken; `./gradlew :engine:checkContent` runs the check alone. See [docs/content-checks.md](docs/content-checks.md). `./gradlew :polishcheck:checkPolish` checks the Polish itself against a dictionary, reference conjugations, grammar rules and a recorded review of every sentence, and CI runs it on every build. See [docs/polish-check.md](docs/polish-check.md).

```
./gradlew :app:assembleRelease :app:bundleRelease
```

builds the minified release APK and App Bundle under `app/build/outputs/`. They are unsigned unless `RELEASE_KEYSTORE_FILE` (a path to the upload keystore) and `RELEASE_KEYSTORE_PASSWORD` are set. `RELEASE_KEY_ALIAS` defaults to `upload` and `RELEASE_KEY_PASSWORD` to the keystore password. The upload key is not in the repository: see [docs/release.md](docs/release.md).

## Screen tests

```
./gradlew :app:testDebugUnitTest
```

plays the real screens on the computer, with no phone or emulator, and CI runs it on every build. The tests start the app on Home and tap and type their way through a Daily Set to its Summary: one Exercise of each type, a wrong answer, an answer missing its accents, the listening controls, "can't listen now", the close button, Sound turned off in Settings, leaving and coming back, the Path and a Replay. The first run downloads the Android version they run on (about 150 MB).

They play a small fixture course in `app/src/test/resources/fixture-course`, written in the same files as `content/`, so an edit to the real course cannot break them. One test opens the real packaged course and checks only that its first step shows. The clock and the Clip player are replaced too, so a test can move to the next day and can say which Clip a screen asked for.

They check what a Learner sees, not the rules, which the engine's tests cover. What they cannot check stays with a run-through on a phone: the sound itself, the Reminder's notification, the time picker, the email draft for a mistake report, the real keyboard and anything that moves.

The same command writes a picture of every screen, in the light and the dark theme and at the largest font size, to `app/build/screenshots/`, and CI keeps them as the `screenshots` artifact of each run. [docs/design.md](docs/design.md) says how the app should look. Colours and font values live only in the app's `ui/theme` folder, and every build fails on one anywhere else (`./gradlew :app:checkDesignTokens`); [CODING_STANDARDS.md](CODING_STANDARDS.md) has the rest of the rules for writing a screen.

## More

See `GLOSSARY.md` for the project's vocabulary, `docs/adr/` for recorded decisions and [docs/audio.md](docs/audio.md) for how the spoken Polish is made and packaged. [docs/authoring.md](docs/authoring.md) has the rules for writing Daily Sets, and [docs/a1-path.md](docs/a1-path.md) outlines the Units of the A1 Path.
