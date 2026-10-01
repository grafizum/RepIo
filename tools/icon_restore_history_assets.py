#!/usr/bin/env python3
"""Restore the historical launcher artwork, exactly, with no recolouring.

WHY THIS EXISTS
---------------
The owner reviewed three commits in GitHub and ruled on them:

  * "the light icons are the correct ones with the white background"
  * "for the dark icons also copy the exactly same for now but change only one
    the orange one ... and replace it with the one in the download folder"
  * "no changing of colors no nothing"

So this is deliberately NOT the vividness/recolour pipeline. Nothing is
recoloured, nothing is boosted, and nine of the ten dark icons are byte-identical
copies of their light artwork. That is not an oversight: it is the instruction,
and it is also the cleanest possible way to see whether the OLD artwork is the
one that was wanted, with orange as the single variable.

WHICH COMMIT EACH THEME'S LIGHT ICON COMES FROM, AND WHY IT IS NOT ONE ANSWER
----------------------------------------------------------------------------
`f09bf58` and `b7d0ec4` carry IDENTICAL artwork - the second commit re-toned only
the plate, copying artwork pixels 1:1, and the alpha channels are byte-identical
to prove it. The difference is the PLATE:

    theme    artwork identical   f09bf58 plate   b7d0ec4 plate
    6 themes          yes            193.5 (white)    193.5 (white)
    flame/ocean/
    rose/purple       no             193.5 (white)     63.2 (dark)

So taking `b7d0ec4` wholesale would give four themes a DARK plate in light mode -
the exact complaint that sent us back to the history in the first place. Those
four therefore come from `f09bf58`, whose artwork is the same drawing on the white
plate. `retro` is identical in both and is taken from `b7d0ec4`.

DARK MODE
---------
Every theme's dark icon is its own light icon, byte for byte, EXCEPT orange,
which becomes the owner's own `ic_launcher_fg_orange_dark_1296.png` render at
every density. No hue rotation, no saturation floor, no greyscale copy for retro:
the owner's words were "no changing of colors no nothing".

Run from the repo root:  python tools/icon_restore_history_assets.py [--write]
"""
from __future__ import annotations

import io
import subprocess
import sys
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp" / "src" / "main" / "res"
OWNER_RENDER = Path.home() / "Downloads" / "ic_launcher_fg_orange_dark_1296.png"

THEMES = [
    "orange", "azure", "crimson", "emerald", "beige",
    "flame", "ocean", "rose", "purple", "retro",
]

# The commit whose artwork each theme's LIGHT icon is taken from. See the module
# docstring: b7d0ec4 gave four themes a dark plate, so those predate it.
LIGHT_SOURCE = {
    "orange": "b7d0ec4", "azure": "b7d0ec4", "crimson": "b7d0ec4",
    "emerald": "b7d0ec4", "beige": "b7d0ec4", "retro": "b7d0ec4",
    "flame": "f09bf58", "ocean": "f09bf58", "rose": "f09bf58", "purple": "f09bf58",
}

# The themes whose DARK icon is the owner's own render.
#
# Orange because it is the theme it was drawn for. Flame because on a DARK system
# the launcher entry that is actually enabled is Flame (RepIoApplication picks
# FLAME when the system is dark), so the icon on the home screen is Flame's - and
# with only Orange carrying the render, the owner saw the historical artwork and
# reported the render as "not applied".
OWNER_RENDER_THEMES = {"orange", "flame"}

FG_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
MIPMAP_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

ARTWORK_MIN_SAT = 40


def git_show(commit: str, path: str) -> Image.Image | None:
    blob = subprocess.run(
        ["git", "show", f"{commit}:{path}"], capture_output=True, cwd=REPO
    ).stdout
    return Image.open(io.BytesIO(blob)).convert("RGBA") if blob else None


def plate_luminance(img: Image.Image) -> float:
    """Luminance of the plate in the corner, where no artwork sits."""
    a = np.asarray(img.convert("RGBA")).astype(float)
    region = a[:60, :60]
    opaque = region[..., 3] > 0.98
    if not opaque.any():
        return float("nan")
    px = region[..., :3][opaque]
    return float((px @ np.array([0.2126, 0.7152, 0.0722])).mean())


def dominant_artwork_colour(img: Image.Image) -> tuple[int, int, int] | None:
    from collections import Counter

    a = np.asarray(img.convert("RGBA")).astype(np.int16)
    rgb, alpha = a[..., :3], a[..., 3]
    mask = (alpha >= 250) & ((rgb.max(-1) - rgb.min(-1)) >= ARTWORK_MIN_SAT)
    if not mask.any():
        return None
    return tuple(int(v) for v in Counter(map(tuple, rgb[mask])).most_common(1)[0][0])


def write(img: Image.Image, sub: str, name: str, size: int) -> None:
    d = RES / sub
    d.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.LANCZOS).save(d / name, "PNG")


def main(write_files: bool) -> int:
    if not OWNER_RENDER.exists():
        print(f"the owner's render is missing: {OWNER_RENDER}")
        return 1
    owner = Image.open(OWNER_RENDER).convert("RGBA")
    print(f"owner's render: {OWNER_RENDER.name} {owner.size}")

    failures = 0
    for theme in THEMES:
        commit = LIGHT_SOURCE[theme]
        # The LIGHT icon is the historical artwork, taken at its own density so
        # no resampling happens on the way in.
        light_src: dict[str, Image.Image | None] = {}
        for density in FG_SIZES:
            light_src[density] = git_show(
                commit,
                f"composeApp/src/main/res/drawable-{density}/ic_launcher_fg_{theme}.png",
            )
        base = light_src["xxxhdpi"]
        if base is None:
            print(f"  {theme}: no asset at {commit}, SKIPPED")
            failures += 1
            continue
        pl = plate_luminance(base)
        art = dominant_artwork_colour(base)
        print(f"  {theme:8} from {commit}  plateLum={pl:6.1f}  artwork={art}")

        if pl < 120:
            print(f"    <-- {theme} would show a DARK plate in light mode")
            failures += 1

        # LIGHT is always the historical artwork. The owner's render replaces the
        # DARK icon of one theme only - an earlier version of this script wrote
        # the render to BOTH modes for orange, which quietly destroyed the
        # historical light icon the owner had asked to keep.

        for density, fg in FG_SIZES.items():
            light_img = light_src[density]
            if light_img is None:
                print(f"    missing {density} artwork at {commit}")
                failures += 1
                continue
            dark_img = (owner.resize((fg, fg), Image.LANCZOS)
                        if theme in OWNER_RENDER_THEMES else light_img)
            if write_files:
                write(light_img, f"drawable-{density}", f"ic_launcher_fg_{theme}.png", fg)
                write(dark_img, f"drawable-night-{density}", f"ic_launcher_fg_{theme}.png", fg)
        for density, mp in MIPMAP_SIZES.items():
            hist = git_show(
                commit,
                f"composeApp/src/main/res/mipmap-{density}/ic_launcher_{theme}.png",
            )
            light_img = hist if hist is not None else base.resize((mp, mp), Image.LANCZOS)
            dark_img = (owner.resize((mp, mp), Image.LANCZOS)
                        if theme in OWNER_RENDER_THEMES else light_img)
            if write_files:
                write(light_img, f"mipmap-{density}", f"ic_launcher_{theme}.png", mp)
                write(dark_img, f"mipmap-night-{density}", f"ic_launcher_{theme}.png", mp)

    print(f"\n{failures} problem(s)")
    if not write_files:
        print("dry run; pass --write to apply")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main("--write" in sys.argv))