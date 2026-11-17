#!/usr/bin/env python3
"""Makes the pictures of Practice from Microsoft's Fluent Emoji, 3D style (MIT licence).

Reads tools/pictures/sources.json, which names each picture file and where it comes from in
github.com/microsoft/fluentui-emoji, downloads what is missing and writes it as a 256 px WebP:

  pictures   -> content/pictures/            the pictures of Items (listed in pictures.json)
  gameIcons  -> app/src/main/res/drawable-nodpi/   the icon of each Game

Run it from the repository root: python3 tools/make-pictures.py [--all]
Without --all only files that do not exist yet are made. It needs Pillow (pip install pillow).
"""
import io
import json
import pathlib
import sys
import time
import urllib.parse
import urllib.request

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/"
SIZE = 256
TARGETS = {"pictures": ROOT / "content" / "pictures", "gameIcons": ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"}


def fetch(path):
    url = SOURCE + urllib.parse.quote(path)
    for attempt in range(5):
        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                return response.read()
        except Exception as failure:  # the host sometimes refuses a burst of requests
            if attempt == 4:
                raise SystemExit(f"Could not fetch {url}: {failure}")
            time.sleep(2 * (attempt + 1))


def main():
    remake = "--all" in sys.argv
    sources = json.loads((ROOT / "tools" / "pictures" / "sources.json").read_text())
    for kind, folder in TARGETS.items():
        folder.mkdir(parents=True, exist_ok=True)
        for name, path in sources.get(kind, {}).items():
            out = folder / name
            if out.exists() and not remake:
                continue
            image = Image.open(io.BytesIO(fetch(path))).convert("RGBA")
            image = image.resize((SIZE, SIZE), Image.LANCZOS)
            image.save(out, "WEBP", quality=90, method=6)
            print(f"{out.relative_to(ROOT)}  {out.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
