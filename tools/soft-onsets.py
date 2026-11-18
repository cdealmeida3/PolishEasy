#!/usr/bin/env python3
"""Lists the Clips whose first sound should be soft (ś, ź, si-, zi-) but sounds hard (s, z).

The voice sometimes says zima as "zyma", which a learner hears as "sima". Nobody can listen on a
build machine, so this measures instead. A soft ś or ź is hissed with the tongue raised, and its
noise peaks at about 3 to 6 kHz; a hard s or z peaks at about 8 to 10 kHz. The script finds where
each Clip's speech starts, averages the spectrum of its first 50 ms between 2 and 11.5 kHz, and
prints the Clips whose average lies above SOFT_UP_TO, the clearest first.

Run it from the repository root: python3 tools/soft-onsets.py
It needs ffmpeg and numpy. A Clip it lists is worth a new take or a `saidAs` entry (docs/audio.md).
"""
import json
import subprocess
import sys

import numpy as np

RATE = 24000
FRAME = 720  # 30 ms
HOP = 240  # 10 ms
ONSET_FRAMES = 5
SOFT_UP_TO = 7500  # Hz. Soft onsets measured 3,000 to 7,000; hard ones 8,500 and up.


def samples(path):
    pcm = subprocess.run(['ffmpeg', '-v', 'quiet', '-i', path, '-ac', '1', '-ar', str(RATE), '-f', 's16le', '-'],
                         capture_output=True, check=True).stdout
    return np.frombuffer(pcm, dtype=np.int16).astype(float) / 32768


def onset_centroid(path):
    x = samples(path)
    window = np.hanning(FRAME)
    freqs = np.fft.rfftfreq(FRAME, 1 / RATE)
    spectra = [np.abs(np.fft.rfft(x[i:i + FRAME] * window)) ** 2 for i in range(0, len(x) - FRAME, HOP)]
    hiss = np.array([s[freqs > 2500].sum() for s in spectra])
    start = int(np.argmax(hiss > hiss.max() * 0.1))
    total = sum(spectra[start:start + ONSET_FRAMES])
    band = (freqs > 2000) & (freqs < 11500)
    return float((freqs[band] * total[band]).sum() / total[band].sum())


def main():
    clips = json.load(open('content/audio/clips.json', encoding='utf-8'))['clips']
    soft = [c for c in clips if c['text'].lower().lstrip('"„ ')[:2] in ('zi', 'si') or c['text'].lower().lstrip('"„ ')[:1] in 'źś']
    hard = []
    for clip in soft:
        centroid = onset_centroid('content/audio/%s.ogg' % clip['id'])
        if centroid > SOFT_UP_TO:
            hard.append((centroid, clip))
    for centroid, clip in sorted(hard, key=lambda h: -h[0]):
        print('%6.0f Hz  %s: %s' % (centroid, clip['id'], clip['text']))
    print('%d of %d soft-starting Clips sound hard.' % (len(hard), len(soft)))
    return 1 if hard else 0


if __name__ == '__main__':
    sys.exit(main())
