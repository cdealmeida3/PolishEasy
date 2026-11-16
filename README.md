# PolishEasy

An Android app for learning Polish in short daily sessions.

## Download

The newest builds of the app are always at the same links:

- **[PolishEasy-debug.apk](https://github.com/cdealmeida3/PolishEasy/releases/download/latest/PolishEasy-debug.apk)**, the debug build. It installs as "PolishEasy Debug".
- **[PolishEasy-release.apk](https://github.com/cdealmeida3/PolishEasy/releases/download/latest/PolishEasy-release.apk)**, the signed, minified release build.

Every update to the default branch rebuilds them and replaces the `latest` release. The same release also holds `PolishEasy-release.aab`, the file to upload to Google Play, and `PolishEasy-release-mapping.txt` for reading crash stack traces. The release files only appear once the signing secrets are set; see [docs/release.md](docs/release.md).

All debug builds are signed with the same debug key, so a new debug APK installs over the previous one without uninstalling. The debug build has its own application id, `com.polisheasy.app.debug`, so it can sit next to the release build (`com.polisheasy.app`) on one phone.

## Build

```
./gradlew :engine:test :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

```
./gradlew :app:assembleRelease :app:bundleRelease
```

builds the minified release APK and App Bundle under `app/build/outputs/`. They are unsigned unless these four environment variables are set: `RELEASE_KEYSTORE_FILE` (a path to the keystore), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`.

See `GLOSSARY.md` for the project's vocabulary, `docs/adr/` for recorded decisions and [docs/audio.md](docs/audio.md) for how the spoken Polish is made and packaged.
