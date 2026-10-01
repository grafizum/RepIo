#!/usr/bin/env python3
"""Measure the launcher icon foregrounds for the light halo the owner reports.

The complaint is "the border between the clean orange and the background has a
thin white bad-quality border which ruins the whole icon", in DARK mode.

An adaptive icon composites <background> (a flat #121212) under
<foreground>. So a white line can only come from the FOREGROUND asset itself -
either opaque near-white pixels of its own, or semi-transparent light pixels
that are neither the artwork nor the plate. This finds both and reports where
they are, rather than guessing from a screenshot nobody here can see.
"""
from __future__ import annotations

from collections import Counter
from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parents[1] / "composeApp/src/main/res"
DENSITY = "xxxhdpi"
THEMES = ["ocean", "crimson", "emerald", "azure", "flame", "orange", "purple", "rose", "beige", "retro"]


def classify(r, g, b, a):
    """Bucket one pixel into the roles the icon script already uses."""
    if a < 24:
        return "transparent"
    spread = max(r, g, b) - min(r, g, b)
    if spread >= 25:
        return "artwork"          # the saturated orange strokes
    lum = 0.299 * r + 0.587 * g + 0.114 * b
    if lum >= 240:
        return "near_white"       # the suspect: opaque plate showing through
    if lum >= 170:
        return "light_neutral"    # bevel / inner shadow
    return "dark"


def main() -> None:
    for theme in THEMES:
        path = RES / f"drawable-{DENSITY}" / f"ic_launcher_fg_{theme}.png"
        if not path.exists():
            print(f"{theme}: MISSING {path}")
            continue
        im = Image.open(path).convert("RGBA")
        w, h = im.size
        counts = Counter()
        # Where are the near-white pixels? A healthy plate is a filled shape
        # interior; a halo is a thin RIM, so the bounding box tells them apart.
        xs, ys = [], []
        pixels = im.load()
        for y in range(h):
            for x in range(w):
                r, g, b, a = pixels[x, y]
                role = classify(r, g, b, a)
                counts[role] += 1
                if role == "near_white":
                    xs.append(x)
                    ys.append(y)
        total = w * h
        pct = lambda k: f"{100.0 * counts[k] / total:5.2f}%"
        print(f"\n=== {theme}  {w}x{h} ===")
        print(
            f"  artwork {pct('artwork')}  near_white {pct('near_white')}  "
            f"light_neutral {pct('light_neutral')}  dark {pct('dark')}  "
            f"transparent {pct('transparent')}"
        )
        if xs:
            bw, bh = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1
            area = len(xs)
            fill = area / float(bw * bh)
            print(
                f"  near_white bbox = {bw}x{bh} at ({min(xs)},{min(ys)})..({max(xs)},{max(ys)}), "
                f"fill = {100 * fill:.1f}% of its bbox"
            )
            # A thin rim has a LOW fill ratio; a solid plate is nearly full.
            if fill < 0.85:
                print("    -> RIM, not a solid plate: this is the halo")
            else:
                print("    -> solid plate, not a halo")
        else:
            print("  no near-white pixels at all")


if __name__ == "__main__":
    main()
