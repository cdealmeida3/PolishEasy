# Audio

The app speaks Polish from **Clips**: recordings made ahead of time and packaged with the app, so audio works offline and needs no key or account. See [ADR-0004](adr/0004-clips-are-found-by-their-text.md) for why Clips are found by their text.

## Where Clips live

- `content/audio/clips.json` lists every Clip: a stable `id` and the Polish `text` it says.
- The recording of Clip `dzien-dobry` is `content/audio/dzien-dobry.ogg`, beside the manifest. The format is Opus in an Ogg container, mono, 32 kbit/s: a few KB per phrase.
- `content/course.json` points to the manifest with `"audio": "audio/clips.json"`.

The files are committed. CI does not generate audio.

## How a text finds its Clip

Exercises do not name Clips. The course engine looks up the Clip whose text matches the Polish in question, ignoring capitals and extra spaces but not punctuation. What can be spoken:

| Exercise | Spoken text |
|---|---|
| `choose_translation` | the Polish prompt |
| `type_answer`, `word_tiles` | every Accepted Answer |
| `conjugation` | every Accepted Answer, or the sentence with the gap filled by it |
| `listen_choose` | `text` |
| `listen_type` | the first Accepted Answer |

The `readingAnswers` of a `listen_type` are never spoken and need no Clip.

A listening Exercise must have its Clip, or the content does not load. Everywhere else a missing Clip only hides the speaker icon, but the build's [content check](content-checks.md) requires the shipped content to have a Clip for every spoken text, a file for every Clip, and nothing left over, and reports each missing Clip and stray file. **After adding or changing Polish text in the content, run the script below**, or the build fails.

## Making the Clips

Today's audio is a stand-in made by a free offline voice, until issue #17 brings the tested generation command. Nothing here needs a key.

- Voice: [Piper](https://github.com/OHF-Voice/piper1-gpl) `pl_PL-gosia-medium`, a single speaker trained on a CC0 dataset.
- Tool: `piper-tts` 1.8.0 from PyPI (GPL-3). It is a build tool only; nothing of it ships in the app.
- Also needed: the `ffmpeg` and `ffprobe` commands, with libopus.

Setup, once (the voice is taken from a GitHub release and checked against its hash):

```
python3 -m venv /tmp/piper && /tmp/piper/bin/pip install piper-tts==1.8.0
curl -L -o /tmp/gosia.tar.bz2 https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-pl_PL-gosia-medium.tar.bz2
echo "75bd34dcbdc4dd98d763954756b4b34b4208100497c836381542e4d73dcefa9c  /tmp/gosia.tar.bz2" | sha256sum -c
tar xjf /tmp/gosia.tar.bz2 -C /tmp
```

Then, from the repository root:

```
/tmp/piper/bin/python tools/placeholder-audio.py --voice /tmp/vits-piper-pl_PL-gosia-medium/pl_PL-gosia-medium.onnx
```

The script reads the content, adds a Clip for every spoken text that has none (its id is a slug of the text), records each Clip that has no file, removes Clips and files whose text is no longer spoken, and rewrites `clips.json`. A Clip that already has its file is left alone, so its id and its recording do not change. To record a Clip again, delete its `.ogg` and run the script.

Two things the script does to the voice's output:

- The voice rushes some one-syllable words (*nie* comes out at 0.14 s). A text spoken in under 0.35 s is spoken again more slowly until it is long enough.
- Each Clip fades out over its last 20 ms and gets 80 ms of silence before and 200 ms after, so a player never clips the first or last sound.

## Playing

`ClipPlayer` in the app plays a Clip from the app's assets with AndroidX Media3. "Slow" plays the same file at 0.6 speed with the pitch kept.

With Sound off in Settings the course engine hands the app no Clip, on an Exercise or on feedback, so nothing plays and no speaker icon is drawn. Listening Exercises come in their reading form ([ADR-0007](adr/0007-a-listening-exercise-is-read-in-place.md)).
