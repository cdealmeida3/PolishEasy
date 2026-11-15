# PolishEasy

An Android app for learning Polish in short daily sessions.

## Download

The newest build of the app is always at the same link:

**[PolishEasy-debug.apk](https://github.com/cdealmeida3/PolishEasy/releases/download/latest/PolishEasy-debug.apk)**

Every update to the default branch rebuilds the APK and replaces the `latest` release. All builds are signed with the same debug key, so a new APK installs over the previous one without uninstalling.

## Build

```
./gradlew :engine:test :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

See `GLOSSARY.md` for the project's vocabulary and `docs/adr/` for recorded decisions.
