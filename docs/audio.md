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

A listening Exercise must have its Clip, or the content does not load. Everywhere else a missing Clip only hides the speaker icon, but the build's [content check](content-checks.md) requires the shipped content to have a Clip for every spoken text, a file for every Clip, and nothing left over, and reports each missing Clip and stray file. **After adding or changing Polish text in the content, make its audio as described below**, or the build fails.

## Making the Clips

```
./gradlew :audio:makeClips
```

One command makes a Clip for every Polish text the course speaks, with one cloud voice, and removes every Clip and audio file that nothing speaks any more. It needs the voice service's key in the environment variable `GOOGLE_TTS_API_KEY` (see "Getting the key" below) and the `ffmpeg` and `ffprobe` commands, with libopus. It is run by hand: no build and no CI job makes audio.

| Switch | What it does |
|---|---|
| `--args="--dry-run"` | Prints what a run would make and remove and how many characters it would send. Needs no key and changes nothing. |
| `--args="--voices"` | Lists the Polish voices the service offers and marks the one in use. |

Only new or changed text is sent. A run on an unchanged repository sends nothing, touches no file and needs no key.

### The voice is recorded in one place

`audio-voice.json`, at the repository root, holds everything that decides how a Clip sounds: the service, the voice's name, the sample rate, the speaking rate, and how the sound is encoded (Opus at 32 kbit/s, the last 20 ms faded out, 80 ms of silence before and 200 ms after so a player never clips the first or last sound).

The voice is Google Cloud Text-to-Speech, **Chirp 3 HD**, `pl-PL-Chirp3-HD-Aoede`. One voice speaks everything in v1.

### How the command knows what to send

`audio-recordings.json`, also at the root, records for each Clip id a fingerprint of the text and the settings that made its file. Only the command writes it. For each text the course speaks:

- **kept**: its Clip has a file, and the fingerprint recorded for it is the one the current text and settings give;
- **made**: it has no Clip yet (the new id is a slug of the text, with `-2` and so on if taken), or its file is missing, or its fingerprint is missing or different.

A Clip that stays keeps its id whatever happens to its file. So:

- changing `audio-voice.json` (another voice, another bitrate) makes every Clip again, under the same ids;
- to record one Clip again, delete its `.ogg` and run the command;
- a text that changes, even only in punctuation, is a new Clip, and the old one is removed.

See [ADR-0009](adr/0009-each-clip-is-tied-to-the-settings-that-made-it.md) for why the fingerprints are in a file of their own.

If the service fails part-way, the command keeps and records what it finished, says how many are left and exits with an error. Running it again carries on from there. A Clip shorter than 0.2 s or longer than 20 s stops the run: that is silence or a runaway, not speech.

### After a run

The command ends by saying what to do next. After adding Clips for new text, that is the rest of "After changing content" in [content-checks.md](content-checks.md): record the new identifiers, run the content checker, run the Polish check and record the review. Then commit `content/audio/` and `audio-recordings.json`. A run that only made existing Clips again (a new voice) needs only the commit.

It also lists the shortest Clips it made. Nobody can hear the result on a build machine, and a very short word (*a*, *i*, *nie*) is where a voice is most likely to go wrong, so listen to those first on a phone.

## Getting the key

This is for the project's owner. It takes about ten minutes. Google's console changes its wording now and then, so a name below may differ slightly.

**What it costs.** Chirp 3 HD voices are free for the first 1 million characters each month, then US$30 per million (Google's [pricing page](https://cloud.google.com/text-to-speech/pricing), read on 2026-11-16). The whole course is about 6,000 characters, so making every Clip uses well under 1% of one month's free allowance. Google still asks for a payment card to turn the service on. `--dry-run` prints the number of characters before anything is sent.

1. In the [Google Cloud console](https://console.cloud.google.com/), create a project (any name) and attach a billing account to it.
2. Turn on the **Cloud Text-to-Speech API** for the project ("APIs & Services", "Library").
3. Under "APIs & Services", "Credentials", create an **API key**. Restrict it to the Cloud Text-to-Speech API.
4. Sensible, not required: under "Billing", "Budgets & alerts", set a budget alert of $1 on the project, so any charge at all sends an email.
5. Put the key where the command can read it, under the name `GOOGLE_TTS_API_KEY`:
   - for cloud sessions, in the project's cloud environment (Project settings, the Cloud environment menu, the gear beside the selected environment), as a secret or environment variable of that name. Sessions started afterwards have it;
   - on a computer, `export GOOGLE_TTS_API_KEY=...` in the shell before running the command.

**Never paste the key into a chat, an issue or a file in the repository.** The command reads it only from the environment, sends it only in a request header, and never prints or stores it. If a key does leak, delete it under "Credentials" and make another.

Without the key the command stops before changing anything and says so, naming this section.

## The placeholder voice (in use until the key is added)

The audio in the repository today is a stand-in made by a free offline voice. **Until the Clips have been made once with `makeClips`, keep using this script after changing Polish text.** It and this section are deleted when that run lands. Nothing here needs a key.

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
