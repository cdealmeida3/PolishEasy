# Each Clip's file is tied to the text and voice settings that made it

The command that makes the Clips must send only new or changed text to the voice service, and it must be able to replace every Clip when the voice changes. The file of a Clip says nothing about what made it: the placeholder audio from a free offline voice and a recording from the chosen cloud voice are both just `<id>.ogg`.

So `audio-recordings.json`, at the repository root, records for each Clip id a fingerprint: a hash of the Clip's text and of everything in `audio-voice.json`. A Clip is made again when its file is missing or its fingerprint is missing or different. Only the command writes the file.

We rejected "a file that exists is left alone", which is how the placeholder script worked. It cannot tell a placeholder from a real recording, so replacing the voice would mean deleting every file by hand, and a half-finished replacement would leave two voices in the app with nothing to show which Clip is which.

We rejected a fingerprint field on each Clip in `content/audio/clips.json`. The app reads that file, and how a file was made is the tooling's business, not the app's. The content checker also rejects any other file beside the manifest, so the record sits at the root with the other files only tools read (`released-ids.json`, `polish-reviews.json`).

We rejected hashing the audio files themselves. That shows a file has changed, not whether it is the right voice.

## Consequences

- Changing anything in `audio-voice.json` makes every Clip again, under the same ids. That is how the placeholder audio is replaced: the first run finds no fingerprints.
- A run that is interrupted loses nothing: finished Clips are recorded, and the next run makes the rest.
- A Clip edited by hand, without the command, keeps its old fingerprint and is not noticed. To record one again, delete its `.ogg`.
- Adding a setting to `audio-voice.json` changes every fingerprint. At about 6,000 characters for the whole course that costs nothing.
- The record can always be thrown away: without it every Clip is simply made again.
