# Class recordings stay on the phone

The book comes with 271 class recordings, 3.6 hours in 27 zips (`lekcja_00.zip` to `lekcja_26.zip`, about 450 MB of MP3). Carlos has them for his class; they belong to the publisher (ADR-0013). He asked for them in the app, to listen and repeat at his own pace.

So the app plays them, and never carries them. The Learner adds them on the phone: Settings, **Class audio**, "Add the class zips" opens Android's own picker, where all the zips can be picked at once, or the folder they are in. The app reads each MP3 in them, matches it to its track by the title tag the publisher gave it ("064 Lekcja 05A, Ćwiczenie 1": the number and the chapter must agree with `tracks.json`), or by its file name when it has no tag, and copies it into `noBackupFilesDir/class-audio/NNN.mp3`. Anything else is left out and counted. Adding them again replaces them; "Remove" deletes them.

What the repository holds about them is what ADR-0013 allows: `content/kpk1/tracks.json` (number, chapter, section letter, exercise number, title as tagged, length) and `segments.json` (each recording's lines, pause to pause, found with ffmpeg's `silencedetect`, timings only), and `listening.json`, tasks written for the app. With those, every chapter's **Słuchaj** lists its recordings by section before any is on the phone, and says how to add them.

## Rules that follow

- **No MP3 in the repository, the content or a package.** The content check fails on an `.mp3` in `content/` (and so does every build, which runs it); an engine test fails on one anywhere in the repository; and the app build fails when an APK or bundle has one in it.
- **The recordings are not backed up.** They live in `noBackupFilesDir`, which Android's backup and device transfer never take, and `backup_rules.xml` / `data_extraction_rules.xml` list only the Learner's own files. A new phone adds them again from the zips.
- **The Path never depends on them.** Daily Sets use the app's own Clips. The recordings and their listening tasks are on the Lesson screen's Słuchaj, beside the sections, not inside them, so a Learner without the zips loses nothing on the Path.
- **They play in the background.** The player is Media3's, behind a `MediaSessionService` with the media notification, so a recording goes on with the screen off or in another app. Leaving the recording's screen in the app stops it.

## Considered options

- *Package the recordings in the APK.* Rejected: it would put the publisher's recordings in the repository and in every release built from it, and add 450 MB to each install.
- *Download them from a link.* Rejected: the app has no network permission at all (it works fully offline), and the recordings would have to be put somewhere public.
- *Re-encode them smaller on the phone.* Not now: 450 MB fits a phone, and the import keeps the publisher's files unchanged. A desktop script to shrink them first can come later if room is short; the import then needs to accept its format.
