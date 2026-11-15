# Releasing to Google Play

Every push to the default branch builds a signed App Bundle and release APK and attaches them to the rolling [`latest` release](https://github.com/cdealmeida3/PolishEasy/releases/tag/latest), next to the debug APK:

- `PolishEasy-release.aab`: upload this to Google Play.
- `PolishEasy-release.apk`: the same build, for installing directly on a phone.
- `PolishEasy-release-mapping.txt`: the R8 mapping file for reading crash stack traces.

Uploading to Play is a manual step.

## How signing works

- The upload keystore and its passwords live only in four GitHub Actions secrets. Nothing is committed, and `.gitignore` blocks `*.jks` and `*.keystore`.
- The keystore is decoded only on pushes to the default branch. Pull requests and other branches still compile and minify the release build, so an R8 break is caught early, but leave it unsigned.
- While the secrets are missing, the build still passes and only the debug APK is published.
- `versionCode` is the run number of `.github/workflows/build.yml`. Play needs a higher code for every upload, so do not rename that file or move the release build to another workflow: the counter would restart at 1.

## Steps to do by hand

Steps 1 to 3 are needed once. Until they are done, CI publishes only the debug APK.

### 1. Create the upload keystore (once, 2 minutes)

Needs a JDK for `keytool` (Android Studio includes one). In a terminal, in a folder outside the repository:

```
keytool -genkeypair -v -storetype PKCS12 -keystore polisheasy-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

It asks for a password and for name and organisation fields. Pick a strong password; the name fields can be anything. Save the file and the password in a password manager. With Play App Signing a lost upload key can be reset through Play support, but it takes days.

### 2. Turn the keystore into text

macOS:

```
base64 -i polisheasy-upload.jks | pbcopy
```

Linux:

```
base64 -w 0 polisheasy-upload.jks
```

Windows PowerShell:

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("polisheasy-upload.jks")) | Set-Clipboard
```

### 3. Add four repository secrets

On GitHub: the repository, then Settings, Secrets and variables, Actions, New repository secret
(https://github.com/cdealmeida3/PolishEasy/settings/secrets/actions).

| Name | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | the text from step 2 |
| `RELEASE_KEYSTORE_PASSWORD` | the password from step 1 |
| `RELEASE_KEY_ALIAS` | `upload` |
| `RELEASE_KEY_PASSWORD` | the same password (a PKCS12 keystore uses one password for both) |

The next push to the default branch, or a manual re-run of the latest Build run, then attaches the signed AAB and APK to the `latest` release.

### 4. Try the release APK on a phone

Download `PolishEasy-release.apk` from the `latest` release, install it, and play one Daily Set from start to summary. Cloud builds have no emulator, so this is the only check of the minified app on a real device. If it crashes on launch, the fix is keep rules for `com.polisheasy.engine.**` in `app/proguard-rules.pro`.

### 5. Create the Play developer account (once)

1. Go to https://play.google.com/console/signup and sign in with the Google account that should own the app.
2. Choose Personal (or Organisation if there is a registered company; that needs a D-U-N-S number).
3. Pay the one-time 25 USD fee and complete identity verification with a government ID. Personal accounts are also asked to verify an Android device through the Play Console mobile app. Verification can take a few days.

### 6. Create the app in Play Console

1. All apps, Create app. Name PolishEasy, default language, App, Free.
2. The package name is taken from the first uploaded bundle: `com.polisheasy.app`. It can never be changed afterwards, so this is the moment to pick a different one if wanted (it means a one-line change to `applicationId` first).
3. Accept Play App Signing when prompted and let Google generate the app signing key. The keystore from step 1 is then only the upload key.

### 7. Upload the first build to Internal testing

1. Download `PolishEasy-release.aab` from the `latest` release.
2. Play Console: Test and release, Testing, Internal testing, Create new release, upload the AAB, add release notes, save and roll out.
3. On the Testers tab add your own Google account email, then open the opt-in link on your phone and install from Play.

Internal testing works within minutes and needs no review. Each later upload needs a newer AAB, which every default-branch push provides.

### 8. Before going public

Play Console's dashboard lists these and blocks production until they are done:

- Store listing: short description (80 characters), full description, 512 x 512 icon, 1024 x 500 feature graphic, at least 2 phone screenshots.
- App content: privacy policy URL, ads declaration (none), content rating questionnaire, target audience, data safety form (the app collects nothing and has no internet permission).
- Personal accounts created since November 2023 must first run a closed test with at least 12 testers opted in for 14 days in a row, then apply for production access.

Console menu names shift over time; the requirements above are as of mid-2026.
