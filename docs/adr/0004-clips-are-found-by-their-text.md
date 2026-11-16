# Clips are found by their text, not named by each Exercise

Every Clip is listed in one manifest, `content/audio/clips.json`, with a stable id and the Polish it says. An Exercise does not name a Clip: the course engine finds the Clip whose text is the Polish in question (`Course.clipFor`), comparing composed letters in lower case with single spaces. Punctuation counts, because *tak?* is not spoken like *tak.*

We rejected an `"audio": "<id>"` field on each Exercise. The feedback of a typing Exercise shows whichever Accepted Answer was matched, so every Accepted Answer of every Exercise would need its own id, and an id could point at a Clip that says something else. Finding by text cannot drift, asks nothing of the author, and gives one Clip to a phrase used by ten Exercises.

## Consequences

- The Clip id is still a stable identifier: it names the file (`<id>.ogg` beside the manifest) and does not change when the same text is used somewhere new.
- Changing a Polish text, even its punctuation, needs a new Clip.
- A listening Exercise whose spoken text has no Clip is rejected when the content loads. A speaker icon whose text has no Clip is simply not shown.
- The build's content check (first a test, since issue #15 the checker in `docs/content-checks.md`) keeps the shipped content and the audio files in step: every text the app can speak has a Clip, every Clip has its file, and no Clip or file is left over.
