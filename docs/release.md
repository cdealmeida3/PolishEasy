# Releasing to Google Play

Every push to the default branch builds a signed App Bundle and release APK and attaches them to the rolling [`latest` release](https://github.com/cdealmeida3/PolishEasy/releases/tag/latest), next to the debug APK:

- `PolishEasy-release.aab`: upload this to Google Play.
- `PolishEasy-release.apk`: the same build, for installing directly on a phone.
- `PolishEasy-release-mapping.txt`: the R8 mapping file for reading crash stack traces.

Uploading to Play is a manual step.

## How signing works

- The upload key is a PKCS12 keystore (alias `upload`) kept only in two repository Actions secrets: `UPLOAD_KEYSTORE_BASE64` (the keystore, base64-encoded) and `UPLOAD_KEYSTORE_PASSWORD` (the password for both the keystore and the key). `build.yml` writes the keystore to the runner's temp folder and passes it to Gradle through `RELEASE_KEYSTORE_FILE` and `RELEASE_KEYSTORE_PASSWORD`. Nothing secret is in the repository, so it can be made public.
- The current upload key was created on 2026-11-16 and is valid until 2056. Its certificate SHA-256 fingerprint is `45:75:7F:E1:32:5E:ED:58:82:B3:E1:41:AC:20:54:DD:BF:8F:F6:D4:23:A1:84:43:4D:91:41:9B:05:FD:9B:B7`. It replaced an upload key that was committed to the repository for a few hours on the same day; that old key was never registered with Google Play and must never be used.
- The keystore and its password are backed up outside GitHub, on Carlos's Mac in `~/Documents/PolishEasy upload key/`. Keep a second copy somewhere safe, such as a password manager. Secrets cannot be read back out of GitHub. With Play App Signing a lost upload key can be reset through Play support, but it takes days.
- To replace the key, update both secrets: `base64 -i upload.p12 | gh secret set UPLOAD_KEYSTORE_BASE64`, then `gh secret set UPLOAD_KEYSTORE_PASSWORD`. If the app is already on Play, ask Play support to reset the upload key first.
- `versionCode` is the run number of `.github/workflows/build.yml`. Play needs a higher code for every upload, so do not rename that file or move the release build to another workflow: the counter would restart at 1.

## Steps to do by hand

### 1. Try the release APK on a phone

Download `PolishEasy-release.apk` from the `latest` release, install it, and play one Daily Set from start to summary. Cloud builds have no emulator, so this is the only check of the minified app on a real device. If it crashes on launch, the fix is keep rules for `com.polisheasy.engine.**` in `app/proguard-rules.pro`.

### 2. Create the Play developer account (once)

1. Go to https://play.google.com/console/signup and sign in with the Google account that should own the app.
2. Choose Personal (or Organisation if there is a registered company; that needs a D-U-N-S number).
3. Pay the one-time 25 USD fee and complete identity verification with a government ID. Personal accounts are also asked to verify an Android device through the Play Console mobile app. Verification can take a few days.

### 3. Create the app in Play Console

1. All apps, Create app. Name PolishEasy, default language, App, Free.
2. The package name is taken from the first uploaded bundle: `com.polisheasy.app`. It can never be changed afterwards, so this is the moment to pick a different one if wanted (it means a one-line change to `applicationId` first).
3. Accept Play App Signing when prompted and let Google generate the app signing key. The committed keystore is then only the upload key.

### 4. Upload the first build to Internal testing

1. Download `PolishEasy-release.aab` from the `latest` release.
2. Play Console: Test and release, Testing, Internal testing, Create new release, upload the AAB, add release notes, save and roll out.
3. On the Testers tab add your own Google account email, then open the opt-in link on your phone and install from Play.

Internal testing works within minutes and needs no review. Each later upload needs a newer AAB, which every default-branch push provides.

### 5. Before going public

Play Console's dashboard lists these and blocks production until they are done:

- Store listing: short description (80 characters), full description, 512 x 512 icon, 1024 x 500 feature graphic, at least 2 phone screenshots.
- App content: privacy policy URL, ads declaration (none), content rating questionnaire, target audience, data safety form (the app collects nothing and has no internet permission).
- Personal accounts created since November 2023 must first run a closed test with at least 12 testers opted in for 14 days in a row, then apply for production access.

Console menu names shift over time; the requirements above are as of mid-2026.
