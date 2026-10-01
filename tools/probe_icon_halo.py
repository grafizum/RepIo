#!/usr/bin/env python3
"""Isolate the launcher's MASK: what the user actually sees is the adaptive
icon cropped and masked, not the raw 432x432 foreground.

The complaint is a "thin white bad quality border between the clean orange and
the background". The plate is the region between the artwork strokes, so the
hairline is a property of the plate's EDGE as seen after the launcher crops to
the 72/108 safe zone and masks it. Rendering that crop is the only way to see
the ring the user sees; the full-size PNG hides it because the ring sits in the
margin that gets cropped away.

Prints, for each theme, the luminance profile along the horizontal midline
after the standard adaptive-icon crop, so a bright 1-2px line is visible as
numbers rather than a description.
"""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np
from PIL import Image

RES = Path(__file__).resolve().parents[1] / "composeApp/src/main/res"
NS = "{http://schemas.android.com/apk/res/android}"
THEMES = ["orange", "ocean", "flame", "rose", "purple", "retro",
          "azure", "crimson", "emerald", "beige"]


def color_value(name: str):
    colors = (RES / "values" / "colors.xml").read_text(encoding="utf-8")
    m = re.search(rf'<color name="{name}">#([0-9A-Fa-f]{{6}})</color>', colors)
    v = int(m.group(1), 16)
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def adaptive_bg(theme: str) -> str:
    root = ET.parse(RES / "mipmap-anydpi-v26" / f"ic_launcher_{theme}.xml").getroot()
    for child in root:
        if child.tag.endswith("background"):
            return child.get(f"{NS}drawable")
    raise SystemExit("no background")


def adaptive_crop(img: Image.Image) -> Image.Image:
    """The launchers crop the adaptive icon to the middle 108/144 = 75%."""
    w, h = img.size
    inset_x, inset_y = int(w * 0.125), int(h * 0.125)
    return img.crop((inset_x, inset_y, w - inset_x, h - inset_y))


def main() -> None:
    for theme in THEMES:
        name = adaptive_bg(theme).split("/")[-1]
        bg = color_value(name)
        fg = Image.open(RES / "drawable-xxxhdpi" / f"ic_launcher_fg_{theme}.png").convert("RGBA")
        comp = Image.alpha_composite(
            Image.new("RGBA", fg.size, tuple(bg) + (255,)), fg
        )
        cropped = adaptive_crop(comp)
        arr = np.asarray(cropped.convert("RGBA"), dtype=np.float32)
        lum = arr[..., :3].mean(-1)
        sat = arr[..., :3].max(-1) - arr[..., :3].min(-1)
        h, w = lum.shape
        mid = h // 2
        row = lum[mid]
        row_sat = sat[mid]

        # Compress the midline into buckets so a 1-2px line shows as a spike.
        print(f"\n=== {theme}  bg={name}  (after the 75% adaptive crop) ===")
        step = max(1, w // 72)
        art_lum = float(lum[row_sat >= 25].mean()) if (row_sat >= 25).any() else 0.0
        plate_lum = float(np.median(row[row_sat < 25]))
        flagged = False
        for i in range(0, w, step):
            v = float(row[i])
            s = float(row_sat[i])
            if s >= 25:
                ch = "A"
            elif v >= 235:
                ch = "#"          # near-white: the suspect
                flagged = True
            elif v >= 150:
                ch = "+"
            elif v >= 60:
                ch = "o"
            elif v >= 30:
                ch = "."
            else:
                ch = " "
            print(f"   {i:3d} {v:6.1f} {ch}", end="\n" if i % 8 == step - 1 else "")
        print(f"   artwork_lum={art_lum:.1f} plate_lum={plate_lum:.1f}"
              f"{'  <-- NEAR-WHITE ON THE MIDLINE' if flagged else ''}")


if __name__ == "__main__":
    main()
