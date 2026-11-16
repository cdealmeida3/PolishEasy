#!/usr/bin/env python3
"""Makes the Clips the Authored Content needs, with a free offline voice.

A stopgap until issue #17 brings the tested generation command. See docs/audio.md.

    python tools/placeholder-audio.py --voice /path/to/pl_PL-gosia-medium.onnx

Needs the `piper-tts` package and the `ffmpeg` and `ffprobe` commands. Clips whose text is no
longer spoken are removed; a Clip that already has its file is left alone, so its id never changes.
"""

import argparse
import json
import re
import subprocess
import sys
import tempfile
import unicodedata
import wave
from pathlib import Path

CONTENT = Path(__file__).resolve().parent.parent / "content"
GAP = "___"
PLAIN = str.maketrans("ąćęłńóśźż", "acelnoszz")
# The voice rushes some one-syllable words ("nie" comes out at 0.14 s), so a text spoken in less
# than MIN_SPEECH seconds is spoken again more slowly, trying each of these length scales in turn.
MIN_SPEECH = 0.35
LENGTH_SCALES = (1.0, 1.5, 2.0, 3.0, 4.0)


def audio_key(text):
    """The same rule as `audioKey` in the engine's Audio.kt."""
    return " ".join(unicodedata.normalize("NFC", text).lower().split())


def spoken(exercise):
    """The same table as `spoken(exercise)` in the engine's Audio.kt."""
    kind = exercise["type"]
    if kind == "choose_translation":
        return [exercise["prompt"]]
    if kind in ("type_answer", "word_tiles"):
        return exercise["acceptedAnswers"]
    if kind == "conjugation":
        sentence = exercise.get("sentence")
        return [sentence.replace(GAP, form) if sentence else form for form in exercise["acceptedAnswers"]]
    if kind == "listen_choose":
        return [exercise["text"]]
    if kind == "listen_type":
        return exercise["acceptedAnswers"][:1]
    sys.exit(f"Exercise {exercise['id']} has the type {kind}, which this script does not know")


def spoken_texts(course):
    """Every Polish text the course can speak, once each, in Path order."""
    texts = {}
    for level in course["levels"]:
        for unit in level["units"]:
            for path in unit["dailySets"]:
                for exercise in json.loads((CONTENT / path).read_text("utf-8"))["exercises"]:
                    for text in spoken(exercise):
                        texts.setdefault(audio_key(text), text)
    return texts


def new_id(text, taken):
    slug = re.sub(r"[^a-z0-9]+", "-", audio_key(text).translate(PLAIN)).strip("-")[:48].strip("-") or "clip"
    candidate, number = slug, 1
    while candidate in taken:
        number += 1
        candidate = f"{slug}-{number}"
    return candidate


def duration(path):
    shown = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
        check=True, capture_output=True, text=True,
    )
    return float(shown.stdout.strip())


def speak(voice, text, target):
    """Writes [text] to the WAV file [target], slowed down if the voice rushes it."""
    from piper import SynthesisConfig

    for scale in LENGTH_SCALES:
        with wave.open(str(target), "wb") as out:
            voice.synthesize_wav(text, out, syn_config=SynthesisConfig(length_scale=scale))
        with wave.open(str(target), "rb") as written:
            if written.getnframes() / written.getframerate() >= MIN_SPEECH:
                return


def record(voice, text, target):
    with tempfile.TemporaryDirectory() as scratch:
        spoken_wav = Path(scratch) / "in.wav"
        speak(voice, text, spoken_wav)
        # The last 20 ms fade out, because the voice can stop mid-vowel. Then 80 ms of silence
        # before and 200 ms after, so a player never clips the first sound.
        subprocess.run(
            ["ffmpeg", "-v", "error", "-y", "-i", str(spoken_wav),
             "-af", "areverse,afade=t=in:d=0.02,areverse,adelay=80:all=1,apad=pad_dur=0.2",
             "-ac", "1", "-ar", "24000",
             "-c:a", "libopus", "-b:a", "32k", "-application", "voip", "-map_metadata", "-1",
             "-fflags", "+bitexact", "-flags:a", "+bitexact", str(target)],
            check=True,
        )
    seconds = duration(target)
    if not 0.2 <= seconds <= 20:
        target.unlink()
        sys.exit(f"The Clip of \"{text}\" is {seconds:.2f} s long, which cannot be right")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--voice", required=True, help="the Piper voice, a .onnx file with its .onnx.json beside it")
    args = parser.parse_args()

    course = json.loads((CONTENT / "course.json").read_text("utf-8"))
    manifest = CONTENT / course["audio"]
    audio_dir = manifest.parent
    audio_dir.mkdir(parents=True, exist_ok=True)
    texts = spoken_texts(course)

    existing = json.loads(manifest.read_text("utf-8"))["clips"] if manifest.exists() else []
    kept = [clip for clip in existing if audio_key(clip["text"]) in texts]
    removed = [clip for clip in existing if audio_key(clip["text"]) not in texts]
    have = {audio_key(clip["text"]) for clip in kept}
    taken = {clip["id"] for clip in kept}
    added = []
    for key, text in texts.items():
        if key not in have:
            clip = {"id": new_id(text, taken), "text": text}
            taken.add(clip["id"])
            added.append(clip)
    clips = sorted(kept + added, key=lambda clip: clip["id"])

    missing = [clip for clip in clips if not (audio_dir / f"{clip['id']}.ogg").exists()]
    if missing:
        from piper import PiperVoice

        voice = PiperVoice.load(args.voice)
        for clip in missing:
            record(voice, clip["text"], audio_dir / f"{clip['id']}.ogg")

    for file in audio_dir.glob("*.ogg"):
        if file.stem not in taken:
            file.unlink()
    manifest.write_text(json.dumps({"clips": clips}, ensure_ascii=False, indent=2) + "\n", "utf-8")

    for label, group in (("Added", added), ("Removed", removed)):
        for clip in group:
            print(f"{label} {clip['id']}: {clip['text']}")
    print(f"{len(clips)} Clips: {len(added)} added, {len(removed)} removed, {len(kept)} kept, {len(missing)} recorded.")


if __name__ == "__main__":
    main()
