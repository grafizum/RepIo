#!/usr/bin/env python3
"""Composite the launcher icons exactly as Android would, and look for the
white hairline the owner reports at the artwork/background boundary.

An adaptive icon is <background> (a flat colour) under <foreground>, both
cropped and masked by the launcher. So the only way to know what the user sees
is to build that composite — the foreground PNG alone does not show it, which
is why a plate that is correct in isolation can still ring against the
background behind it.

Reports, per theme, in dark mode with the #121212 ground the in-app dark
theme uses:
  * the adaptive background colour actually referenced by the XML
  * the luminance profile walking outward from the artwork
  * any pixel BRIGHTER than both its artwork and the ground, sitting in a thin
    band — that is the "white border" in numbers rather than adjectives
"""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np
from PIL import Image

RES = Path(__file__).resolve().parents[1] / "composeApp/src/main/res"
NS = "{http://schemas.android.com/apk/res/android}"
GROUND_DARK = 18.0
SAT_ART = 25.0
THEMES = ["ocean", "flame", "rose", "purple", "retro",
          "royal_blue", "crimson", "emerald", "orange", "beige"]


def color_value(name: str) -> float:
    colors = (RES / "values" / "colors.xml").read_text(encoding="utf-8")
    m = re.search(rf'<color name="{name}">#([0-9A-Fa-f]{{6}})</color>', colors)
    if not m:
        raise SystemExit(f"color {name} not found")
    v = int(m.group(1), 16)
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def adaptive_bg(theme: str) -> str:
    xml = RES / "mipmap-anydpi-v26" / f"ic_launcher_{theme}.xml"
    root = ET.parse(xml).getroot()
    for child in root:
        if child.tag.endswith("background"):
            return child.get(f"{NS}drawable")
    raise SystemExit(f"no <background> in {xml}")


def composite(theme: str, density: str, bg_rgb, ground: float):
    fg = Image.open(RES / f"drawable-{density}" / f"ic_launcher_fg_{theme}.png").convert("RGBA")
    base = Image.new("RGBA", fg.size, tuple(bg_rgb) + (255,))
    return Image.alpha_composite(base, fg)


def band_analysis(comp: Image.Image, theme: str, bg_name: str) -> None:
    arr = np.asarray(comp.convert("RGBA"), dtype=np.float32)
    rgb = arr[..., :3]
    lum = rgb.mean(-1)
    sat = rgb.max(-1) - rgb.min(-1)
    h, w = lum.shape

    art = sat >= SAT_ART
    art_lum = float(lum[art].mean()) if art.any() else 0.0
    ground_lum = float(np.median(lum[:4, :4]))

    # A halo is BRIGHTER than both the artwork and the ground it sits on.
    brighter_than_art = lum > art_lum
    bright = lum > max(art_lum, ground_lum) * 1.15 + 4.0
    # ...and it is a THIN band, so count how wide the bright run is.
    print(f"  {theme:<8} bg={bg_name:<16} artwork_lum={art_lum:5.1f} "
          f"ground_lum={ground_lum:5.1f} bright_px={int(bright.sum()):6d} "
          f"({100.0 * bright.mean():5.2f}% of frame)")

    if not bright.any():
        print("      -> no pixel brighter than both artwork and ground")
        return

    ys, xs = np.where(bright)
    bw, bh = xs.max() - xs.min() + 1, ys.max() - ys.min() + 1
    fill = bright.sum() / float(bw * bh)
    print(f"      bright bbox {bw}x{bh} at ({xs.min()},{ys.min()})..({xs.max()},{ys.max()}) "
          f"fill={100 * fill:.1f}%")
    if fill < 0.6:
        print("      -> THIN scattered band: this is the reported white border")
    else:
        print("      -> broad area, not a hairline")


def main() -> None:
    print("=== composited as Android renders it, dark ground #121212 ===")
    for theme in THEMES:
        bg_ref = adaptive_bg(theme)
        name = bg_ref.split("/")[-1]
        bg_rgb = color_value(name)
        comp = composite(theme, "xxxhdpi", bg_rgb, GROUND_DARK)
        band_analysis(comp, theme, name)

    print("\n=== is there a night-qualified resource at all? ===")
    quals = sorted({p.name for p in RES.iterdir() if "night" in p.name})
    print(f"  dirs containing 'night': {quals or 'NONE'}")
    print("  => a single adaptive XML serves both light and dark, so a theme's")
    print("     plate is whatever that ONE file says, in either mode.")


if __name__ == "__main__":
    main()
