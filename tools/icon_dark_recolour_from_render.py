#!/usr/bin/env python3
"""Recolour the owner's approved dark render onto every remaining dark theme.

WHY THIS EXISTS
---------------
The owner rendered one new dark launcher icon, applied it to the theme it was
drawn for, called the result "absolutely perfect", and then asked for exactly
three things:

  * recolour the primary orange with the other DARK theme colours;
  * "DO NOT change the background color leave it exactly as is";
  * "DO NOT change any of the light theme icons / colors".

So this writes NIGHT assets and nothing else. There is no `--all-modes` flag on
purpose: the light icons are the historical artwork restored from git, and a
colour pass over them would destroy the thing the owner just approved.

WHY THE APPROVED RENDER IS THE REFERENCE, NOT A TEMPLATE TO TONE
-----------------------------------------------------------------
The render's own saturation and brightness are what the owner approved, so the
other themes must MATCH it rather than be pushed further. `recolour()` alone is
used here and the vividness floor (`pungency`) from the earlier pipeline is
deliberately NOT applied: a recoloured theme brighter than the approved orange
would mean the orange is the least vivid icon in the set, which is the exact
complaint that started this. Every dark icon ends up with the render's colour
profile and only its hue differs.

THE PLATE
---------
The render has its own dark plate baked in, and recolouring only touches
SATURATED pixels, so that plate is copied bit-identically and the alpha channel
is never written. "Leave the background exactly as is" is therefore a property of
the algorithm rather than a promise in a report - and the test at the end of
main() checks it on the written files.

THE MONOCHROME THEME
--------------------
retro has no colour to rotate onto. Giving it the source's orange would repaint a
grey theme orange, so it gets a greyscale copy of the same artwork, matched to
retro's own brightness reference. `LauncherIconNightVariantTest` asserts retro
stays monochrome.

Run from the repo root:  python tools/icon_dark_recolour_from_render.py [--write]
"""
from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp" / "src" / "main" / "res"

OWNER_RENDER = Path.home() / "Downloads" / "ic_launcher_fg_orange_dark_1296.png"

# The two themes that already carry the render as drawn. Left alone here.
ALREADY_OWNER_RENDER = {"orange", "flame"}

# retro is monochrome by design.
MONOCHROME = {"retro"}

# retro's brightness reference must come from a DARK icon. Its LIGHT icon is
# white-plated, so reading peak luminance from it returns 255 and the greyscale
# copy comes out near-white - a monochrome theme turned into a glowing one. The
# committed dark asset at HEAD is the stable reference; see
# IconSwitchHelper.referenceImage's equivalent in the recolour pipeline.
RETRO_BRIGHTNESS_REF = "HEAD:composeApp/src/main/res/drawable-xxxhdpi/ic_launcher_fg_retro.png"

# Every dark theme that still needs the artwork, in grid order.
DARK_THEMES = [
    "royal_blue", "crimson", "emerald", "beige",
    "ocean", "rose", "purple", "retro",
]

FG_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
MIPMAP_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def load_recolour_helpers():
    """Reuse the HSV recolour rather than keeping a second implementation.

    The recolour itself is not opinionated - it rotates hue and scales S/V onto a
    target and repaints only saturated pixels. Whether a vividness floor is then
    applied is the caller's decision, and here it is deliberately not.
    """
    path = REPO / "tools" / "icon_night_artwork_refresh.py"
    spec = importlib.util.spec_from_file_location("icon_night_artwork_refresh", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def theme_colour(helpers, theme: str) -> tuple[int, int, int] | None:
    """The theme's own artwork colour, read from its LIGHT icon.

    The light icons are never written by this tool, so they are a stable palette
    reference - the same reason the earlier pipeline read targets from files it
    did not replace. A target taken from the tool's own output is not a target.
    """
    light = RES / "drawable-xxxhdpi" / f"ic_launcher_fg_{theme}.png"
    if not light.exists():
        return None
    return helpers.dominant_artwork_colour(light)


def hue_of(rgb: tuple[int, int, int]) -> float | None:
    """Hue in degrees, or None for an achromatic colour."""
    hsv = np.asarray(
        Image.new("RGB", (1, 1), tuple(rgb)).convert("HSV")
    )[0, 0].astype(float)
    if hsv[1] == 0:
        return None
    return float(hsv[0]) * 360.0 / 255.0


def hsv_to_rgb(h_deg: float, s: float, v: float) -> tuple[int, int, int]:
    """An RGB triple at the given HSV, with h in degrees and s/v in 0..1."""
    arr = np.array(
        [[[round(h_deg / 360.0 * 255), round(s * 255), round(v * 255)]]],
        dtype=np.uint8,
    )
    px = np.asarray(Image.fromarray(arr, mode="HSV").convert("RGB"))[0, 0]
    return tuple(int(c) for c in px)


def neon_target(helpers, palette: tuple[int, int, int] | None):
    """The recolour target: the theme's HUE at the RENDER's saturation and value.

    This is the fix for "the other ones are very dark and moody, not so neon".
    `recolour()` scales saturation and value onto the target as well as rotating
    hue, and the historical palette is far darker than the approved render:

        render (approved)      value 97.6%, saturation 90%
        royal_blue  (0,91,184)      value 72%
        emerald(0,144,125)     value 57%
        purple (80,62,130)     value 51%

    So matching the palette dragged value down by up to 46 points and every
    recoloured theme came out dark and moody while the orange stayed neon. The
    owner approved ONE icon, so the other themes must be that icon in a different
    colour - same brightness, same saturation, different hue. Only the hue comes
    from the palette now.
    """
    if palette is None:
        return None
    h = hue_of(palette)
    if h is None:
        return None
    src_dom = helpers.dominant_colour_of(SOURCE_FOR_TARGET)
    if src_dom is None:
        return None
    src_hsv = np.asarray(
        Image.new("RGB", (1, 1), src_dom).convert("HSV")
    )[0, 0].astype(float)
    return hsv_to_rgb(h, float(src_hsv[1]) / 255.0, float(src_hsv[2]) / 255.0)


def mono_brightness(helpers) -> float:
    """Peak artwork luminance for the monochrome theme, from a DARK reference."""
    import io
    import subprocess

    blob = subprocess.run(
        ["git", "show", RETRO_BRIGHTNESS_REF], capture_output=True, cwd=REPO
    ).stdout
    if not blob:
        raise SystemExit(
            f"cannot read the dark brightness reference: {RETRO_BRIGHTNESS_REF}"
        )
    ref = Image.open(io.BytesIO(blob)).convert("RGBA")
    return helpers.artwork_peak_luminance_from(ref)


def plate_luminance(img: Image.Image) -> float:
    """Mean luminance of the PLATE: the darkest large mass of opaque pixels.

    Not the corner - the render's top-left corner is fully transparent, so an
    earlier corner-based version returned NaN, and `abs(nan - nan) > tolerance`
    is False, so the "did the plate move?" check passed on every run without ever
    comparing anything. A guard that cannot fail is worse than no guard, because
    it reports success.
    """
    a = np.asarray(img.convert("RGBA")).astype(np.int16)
    lum = (a[..., :3].astype(float) @ np.array([0.2126, 0.7152, 0.0722]))
    opaque = lum[a[..., 3] >= 250]
    if opaque.size == 0:
        return float("nan")
    return float(np.percentile(opaque, 25))


def plate_is_bit_identical(src: Image.Image, out: Image.Image) -> bool:
    """Is every plate pixel of `src` still the same RGB in `out`?

    Compared bitwise rather than statistically. Averaging luminance over
    "unsaturated" pixels looked like it moved the plate for the greyscale theme,
    but that was the measurement: greyscaling turns coloured artwork pixels
    unsaturated, so they join the pool and shift the mean while the plate itself
    never changed. Recolouring only ever writes saturated pixels, so this must
    hold exactly, and "exactly" is checkable.
    """
    s = np.asarray(src.convert("RGBA")).astype(np.int16)
    o = np.asarray(out.convert("RGBA")).astype(np.int16)
    if s.shape != o.shape:
        return False
    sat = s[..., :3].max(-1) - s[..., :3].min(-1)
    plate = (s[..., 3] >= 250) & (sat < 30)
    if not plate.any():
        return False
    return bool((s[..., :3][plate] == o[..., :3][plate]).all())


def main(write: bool) -> int:
    if not OWNER_RENDER.exists():
        print(f"the owner's render is missing: {OWNER_RENDER}")
        return 1

    helpers = load_recolour_helpers()
    source = Image.open(OWNER_RENDER).convert("RGBA")
    global SOURCE_FOR_TARGET
    SOURCE_FOR_TARGET = source
    source_plate = plate_luminance(source)
    print(f"source: {OWNER_RENDER.name} {source.size}  plateLum={source_plate:.1f}")
    print(f"already carrying this render as drawn: {sorted(ALREADY_OWNER_RENDER)}")

    failures = 0
    for theme in DARK_THEMES:
        target = theme_colour(helpers, theme)
        if theme in MONOCHROME or target is None:
            grey = mono_brightness(helpers)
            art = helpers.desaturate(source, grey)
            state = f"greyscale copy, peak lum {grey:.0f} (from a DARK reference)"
        else:
            neon = neon_target(helpers, target)
            art = helpers.recolour(source, neon or target)
            got = helpers.dominant_artwork_colour_from(art)
            state = (
                f"hue {hue_of(target):.0f}deg at the render's own S/V -> {got}"
            )
        print(f"\n{theme}: {state}")

        # The plate and the alpha must be identical to the source's.
        src_a = np.asarray(source.convert("RGBA"))
        out_a = np.asarray(art.convert("RGBA"))
        if not (src_a[..., 3] == out_a[..., 3]).all():
            print("    <-- ALPHA CHANGED; the silhouette would not survive")
            failures += 1
        if not plate_is_bit_identical(source, art):
            print("    <-- PLATE CHANGED; the background must stay exactly as it is")
            failures += 1

        if not write:
            continue
        for density, size in FG_SIZES.items():
            d = RES / f"drawable-night-{density}"
            d.mkdir(parents=True, exist_ok=True)
            art.resize((size, size), Image.LANCZOS).save(
                d / f"ic_launcher_fg_{theme}.png", "PNG"
            )
        for density, size in MIPMAP_SIZES.items():
            d = RES / f"mipmap-night-{density}"
            d.mkdir(parents=True, exist_ok=True)
            art.resize((size, size), Image.LANCZOS).save(
                d / f"ic_launcher_{theme}.png", "PNG"
            )

    print(f"\n{failures} problem(s)")
    if not write:
        print("dry run; pass --write to apply")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main("--write" in sys.argv))