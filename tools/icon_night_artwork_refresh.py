#!/usr/bin/env python3
"""Apply the owner's freshly-rendered dark launcher artwork across the dark themes.

WHY THIS EXISTS
---------------
On 2026-10-01 the owner rendered a new, better-quality version of the DARK icon
(`~/Downloads/ic_launcher_fg_orange_dark_1296.png`) and asked for two things:
apply it for ITS theme in the SAME colour, then recolour that icon and copy it
across the other dark themes.

Measured, the new file is the SAME composition at 3x the resolution - the plate
occupies relative x[0.062, 0.933] and the artwork x[0.311, 0.680] in both the new
1296px source and the 432px asset it replaces - so it drops straight in at the
existing per-density sizes with no repositioning. Only the rendering quality
changed, which is exactly what "a fresh good quality new one" means.

WHERE EACH THEME'S DARK ICON LIVES (this is the part that is easy to get wrong)
-------------------------------------------------------------------------------
Two different storage shapes, because of the plate:

  * The five LIGHT-plate themes (orange, azure, crimson, emerald, beige) got a
    night FOREGROUND from tools/icon_night_variants.py. Their dark icon is
    `drawable-night-<d>/ic_launcher_fg_<theme>.png` plus the legacy
    `mipmap-night-<d>/ic_launcher_<theme>.png`, and their LIGHT assets are
    separate and untouched. This tool writes ONLY the night pair, so the light
    icon is left exactly as it is.

  * The five that WERE already-dark (flame, ocean, rose, purple, retro) used to
    be the exception: one dark-plated foreground served both modes, so applying
    the new artwork overwrote the tracked LIGHT file too. That exception was
    itself the bug - it is why those five showed a dark plate in light mode -
    and it is gone. They now carry a night asset like everyone else, so this
    tool writes no light-mode file for any theme.

COLOUR
------
The target colour for every theme is read from the app's OWN current dark icon
rather than typed in, so the recolour cannot drift from the theme palette. The
recolour is a hue rotation plus a saturation/value scale on SATURATED PIXELS
ONLY: the plate is neutral, and the shading inside the artwork is a gradient of
one hue, so rotating hue and scaling S/V reproduces the palette while keeping
every highlight and shadow exactly where the renderer put it. Alpha is never
touched, which is what guarantees the silhouette survives.

A theme whose current artwork has no saturated pixels at all (retro is
monochrome by design) has no colour to match, so it is left in the source
colour and reported, rather than being given an invented one.

Run from the repo root:  python tools/icon_night_artwork_refresh.py [--write]
"""
from __future__ import annotations

import subprocess
import sys
from collections import Counter
from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp" / "src" / "main" / "res"

SOURCE_PNG = Path.home() / "Downloads" / "ic_launcher_fg_orange_dark_1296.png"

# The theme the owner rendered it for. Its existing colour IS the source colour,
# so it is applied untouched - "apply it for the corresponding theme with the
# same colour".
SOURCE_THEME = "orange"

# Every theme's dark icon is now a separate NIGHT asset, so the light icon is a
# different file that this tool never writes.
#
# flame/ocean/rose/purple/retro USED TO be single-asset dark themes: commit
# 4f2f1a7 gave their LIGHT foreground a baked dark plate and pointed their light
# adaptive icon at @color/icon_bg_dark, so five themes that had a white plate
# before (f09bf58) rendered dark in light mode. Their light assets are restored
# to the white-plate artwork and their light XML back to @color/icon_bg_white,
# which makes them night-only themes exactly like the other five.
# LauncherIconNightVariantTest holds all ten in place, including the rule that
# no theme may serve both modes from one asset.
NIGHT_ONLY_THEMES = [
    "orange", "azure", "crimson", "emerald", "beige",
    "flame", "ocean", "rose", "purple", "retro",
]

# No theme serves both modes from one file any more. Kept as an explicit empty
# list because "which themes still overwrite a tracked light asset?" is the
# question that caused the regression, and it must have an answer on the screen.
SINGLE_ASSET_DARK_THEMES: list[str] = []

# retro is monochrome, so its night icon's only reference is BRIGHTNESS, and a
# brightness read from a WHITE light plate is the plate, not the artwork.
MONOCHROME_DARK_REFERENCE = {"retro"}

# Adaptive foreground canvas is 108dp; the legacy launcher raster is 48dp.
FG_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
MIPMAP_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# A pixel counts as "artwork" when it is opaque and clearly coloured. The plate
# is a dark neutral, so a low saturation means plate, not art.
ARTWORK_MIN_SAT = 40

# How vivid the DARK artwork has to be to read as deliberate rather than passive.
#
# The owner's report was "the orange of the dark icon is not so strong like the
# theme, I want the colour more pungent and definitely more visible and not so
# passive". Measured, the source orange was already rgb(249,122,24) at 90.4%
# saturation - the most vivid icon in the set - while the recoloured themes were
# not: purple sat 52.3%, rose 59.3%, beige 59.7%, emerald value 56.5%. A dark
# plate makes a low-contrast artwork read as muddy, so the recoloured themes
# needed a floor that the source orange already cleared.
#
# These are FLOORS, not targets. Saturation and value are raised only as far as
# needed to reach them, so orange is left exactly as the owner rendered it and
# the plate, the shading gradient, the hue and the alpha are all untouched.
PUNGENCY_MIN_SAT = 0.85
PUNGENCY_MIN_VAL = 0.72

# How far a recoloured artwork's dominant colour may sit from the theme's own
# colour before the run is called a failure rather than a success.
COLOUR_TOLERANCE = 26.0

# The vividness lift moves saturation and value AWAY from the theme's palette on
# purpose, so comparing the recoloured result to the palette in RGB stopped
# being a check of anything - it was measuring the lift against itself.
#
# HUE is the axis that still means "this is the theme's colour": scaling S and V
# in HSV cannot move it. So the palette check is a hue check, and vividness is
# asserted separately as a floor. Together they say "the right colour, and
# bright enough to read", which is what the owner asked for.
HUE_TOLERANCE_DEG = 12.0
SAT_FLOOR = PUNGENCY_MIN_SAT * 255.0 - 1.0
VAL_FLOOR = PUNGENCY_MIN_VAL * 255.0 - 1.0

# The boost aims slightly ABOVE the floors. Both the recolour and the lift round
# to 8 bits per channel, so a value aimed exactly at the floor can land a
# fraction under it - crimson's saturation came out 215.6 against a 215.8 floor
# and failed the run on a quantisation artefact. The headroom absorbs that; the
# floors themselves stay strict.
PUNGENCY_HEADROOM = 1.02


def hue_of(rgb: tuple[int, int, int]) -> float:
    """Hue in degrees, 0-360. Achromatic input has no hue and returns -1."""
    hsv = np.asarray(Image.new("RGB", (1, 1), tuple(rgb)).convert("HSV"))[0, 0].astype(float)
    if hsv[1] == 0:
        return -1.0
    return float(hsv[0]) * 360.0 / 255.0


def hue_distance_deg(a: float, b: float) -> float:
    """Circular distance between two hues; 0 and 359 are 1 degree apart, not 359."""
    d = abs(a - b) % 360.0
    return min(d, 360.0 - d)


def dominant_artwork_colour(path: Path) -> tuple[int, int, int] | None:
    """The most common saturated opaque colour of a FILE, i.e. the theme hue."""
    return dominant_colour_of(Image.open(path).convert("RGBA"))


def dominant_colour_of(img: Image.Image) -> tuple[int, int, int] | None:
    """The most common saturated opaque colour, i.e. the theme's artwork hue."""
    arr = np.asarray(img.convert("RGBA")).astype(np.int16)
    rgb, alpha = arr[..., :3], arr[..., 3]
    sat = rgb.max(-1) - rgb.min(-1)
    mask = (alpha >= 250) & (sat >= ARTWORK_MIN_SAT)
    if not mask.any():
        return None
    counts = Counter(map(tuple, rgb[mask]))
    return tuple(int(v) for v in counts.most_common(1)[0][0])


def desaturate(img: Image.Image, max_luminance: float) -> Image.Image:
    """A greyscale copy for a monochrome theme, matched to its own brightness.

    A theme whose artwork is monochrome by design (retro) has no hue to rotate
    onto, so the honest translation of "copy this icon across" is the SAME icon
    with the colour taken OUT - not the source's orange left in place, which is
    what the first run of this tool did and it repainted a grey theme orange.

    The artwork's brightest pixel is scaled onto the reference's brightest
    artwork pixel, so retro keeps its own darkness instead of becoming a pale
    grey icon that is much lighter than the theme it belongs to.
    """
    arr = np.asarray(img.convert("RGBA"))
    rgb = arr[..., :3].astype(np.float32)
    alpha = arr[..., 3]
    sat = rgb.max(-1) - rgb.min(-1)
    mask = (alpha >= 250) & (sat >= ARTWORK_MIN_SAT)
    if not mask.any():
        return img
    lum = rgb @ np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)
    peak = float(lum[mask].max())
    if peak <= 0:
        return img
    scale = max_luminance / peak
    out = rgb.copy()
    # Expand to three channels before indexing: out[mask] selects (N, 3), so a
    # single-channel (H, W, 1) array cannot be assigned into it.
    grey = np.repeat(np.clip(lum * scale, 0, 255)[..., None], 3, axis=2)
    out[mask] = grey[mask]
    return Image.fromarray(
        np.dstack([out.astype(np.uint8), alpha]), mode="RGBA"
    )


def reference_image(theme: str) -> tuple[Image.Image, str]:
    """The theme's CURRENT artwork, read from a source this tool never writes.

    This is what makes the tool idempotent. The first version read the target
    colour out of the very files it was replacing, so a second run read its own
    output: retro had been overwritten with the orange artwork, retro's "theme
    colour" became orange, and retro was dutifully recoloured back to orange
    forever. A pipeline whose target comes from its own output is not a pipeline.

    So each theme's colour is read from somewhere stable:
      * every theme reads its TRACKED light foreground, which carries the same
        artwork colour and which this tool never writes.

    retro is the exception, and for a specific reason: it is the one MONOCHROME
    theme, so it has no colour to match and its only reference is BRIGHTNESS.
    Its light foreground is white-plated, so the peak opaque luminance of that
    file is the white plate (255), not its artwork - using it scaled retro's
    night artwork to near-white and turned a muted grey theme into a glaring
    one. Brightness has to be read from a DARK reference, so retro reads the
    committed dark artwork at `git show HEAD:<path>` instead.
    """
    if theme in MONOCHROME_DARK_REFERENCE:
        rel = f"composeApp/src/main/res/drawable-xxxhdpi/ic_launcher_fg_{theme}.png"
        raw = subprocess.run(["git", "show", f"HEAD:{rel}"], capture_output=True, cwd=REPO)
        if raw.returncode == 0 and raw.stdout:
            tmp = Path("/tmp") / f"icon_ref_{theme}.png"
            tmp.write_bytes(raw.stdout)
            return Image.open(tmp).convert("RGBA"), f"git HEAD:{rel} (dark reference for brightness)"
    if theme in NIGHT_ONLY_THEMES:
        path = RES / "drawable-xxxhdpi" / f"ic_launcher_fg_{theme}.png"
        return Image.open(path).convert("RGBA"), f"{path.parent.name}/{path.name} (tracked, never written here)"
    rel = f"composeApp/src/main/res/drawable-xxxhdpi/ic_launcher_fg_{theme}.png"
    raw = subprocess.run(["git", "show", f"HEAD:{rel}"], capture_output=True, cwd=REPO)
    if raw.returncode == 0 and raw.stdout:
        tmp = Path("/tmp") / f"icon_ref_{theme}.png"
        tmp.write_bytes(raw.stdout)
        return Image.open(tmp).convert("RGBA"), f"git HEAD:{rel}"
    path = RES / "drawable-xxxhdpi" / f"ic_launcher_fg_{theme}.png"
    return Image.open(path).convert("RGBA"), f"{path.parent.name}/{path.name} (HEAD unavailable, fell back to disk)"


def artwork_peak_luminance_from(img: Image.Image) -> float:
    """Brightest NON-PLATE pixel of a greyscale icon: plate or artwork, never both."""
    arr = np.asarray(img.convert("RGBA")).astype(np.float32)
    rgb, alpha = arr[..., :3], arr[..., 3]
    lum = rgb @ np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)
    # In a greyscale icon the plate is the large dim mass; the artwork is what
    # sits above its median, so take the top percentile rather than a hard cut.
    opaque = lum[alpha >= 250]
    if opaque.size == 0:
        return 0.0
    return float(np.percentile(opaque, 99.5))


def recolour(img: Image.Image, target: tuple[int, int, int]) -> Image.Image:
    """Rotate the artwork's hue onto `target`, leaving plate, shading and alpha.

    Works in HSV because the artwork is one hue with a shading gradient: a hue
    rotation moves every shade of it at once, where a per-channel colour matrix
    would tint the highlights and the shadows differently.
    """
    hsv = np.asarray(img.convert("HSV")).astype(np.float32)
    rgb = np.asarray(img.convert("RGB")).astype(np.int16)
    sat_rgb = rgb.max(-1) - rgb.min(-1)
    alpha = np.asarray(img.convert("RGBA"))[..., 3]
    mask = (alpha >= 250) & (sat_rgb >= ARTWORK_MIN_SAT)
    if not mask.any():
        return img

    src_dom = Counter(map(tuple, rgb[mask])).most_common(1)[0][0]
    src_hsv = np.asarray(Image.new("RGB", (1, 1), tuple(src_dom)).convert("HSV"))[0, 0].astype(np.float32)
    tgt_hsv = np.asarray(Image.new("RGB", (1, 1), tuple(target)).convert("HSV"))[0, 0].astype(np.float32)

    out = hsv.copy()
    delta_h = (tgt_hsv[0] - src_hsv[0]) % 256.0
    # Only rotate when there is a hue to rotate to; a grey target would send a
    # coloured artwork to mud, and retro is handled by not being recoloured.
    out[..., 0][mask] = (hsv[..., 0][mask] + delta_h) % 256.0
    if src_hsv[1] > 0:
        out[..., 1][mask] = np.clip(hsv[..., 1][mask] * (tgt_hsv[1] / src_hsv[1]), 0, 255)
    if src_hsv[2] > 0:
        out[..., 2][mask] = np.clip(hsv[..., 2][mask] * (tgt_hsv[2] / src_hsv[2]), 0, 255)

    converted = np.asarray(
        Image.fromarray(out.astype(np.uint8), mode="HSV").convert("RGB")
    )
    # Repaint ONLY the masked pixels, so the plate stays bit-identical.
    base = np.asarray(img.convert("RGB")).copy()
    base[mask] = converted[mask]
    alpha_ch = np.asarray(img.convert("RGBA"))[..., 3]
    return Image.fromarray(np.dstack([base, alpha_ch]), mode="RGBA")


def dominant_artwork_colour_from(img: Image.Image) -> tuple[int, int, int] | None:
    """The same measurement as [dominant_artwork_colour], on an image in hand."""
    return dominant_colour_of(img)


def pungency(img: Image.Image) -> Image.Image:
    """Raise a coloured icon's saturation and value to the vividness floor.

    Multiplicative on S and V in HSV, so the hue - the one thing that makes the
    icon the theme's colour - cannot move, and the plate (unsaturated) and the
    alpha are repainted bit-identical because only masked pixels are written.

    Only the pixels that need it are lifted: a theme already above both floors
    comes back unchanged, which is why the owner's own orange survives as-is.
    """
    hsv = np.asarray(img.convert("HSV")).astype(np.float32)
    rgb = np.asarray(img.convert("RGB")).astype(np.int16)
    sat_rgb = rgb.max(-1) - rgb.min(-1)
    alpha = np.asarray(img.convert("RGBA"))[..., 3]
    mask = (alpha >= 250) & (sat_rgb >= ARTWORK_MIN_SAT)
    if not mask.any():
        return img

    out = hsv.copy()
    s = hsv[..., 1] / 255.0
    v = hsv[..., 2] / 255.0
    aim_sat = min(1.0, PUNGENCY_MIN_SAT * PUNGENCY_HEADROOM)
    aim_val = min(1.0, PUNGENCY_MIN_VAL * PUNGENCY_HEADROOM)
    s_scale = np.where(s > 0, np.maximum(1.0, aim_sat / np.maximum(s, 1e-6)), 1.0)
    v_scale = np.where(v > 0, np.maximum(1.0, aim_val / np.maximum(v, 1e-6)), 1.0)
    out[..., 1] = np.clip(s * s_scale * 255.0, 0, 255)
    out[..., 2] = np.clip(v * v_scale * 255.0, 0, 255)

    converted = np.asarray(
        Image.fromarray(out.astype(np.uint8), mode="HSV").convert("RGB")
    )
    base = np.asarray(img.convert("RGB")).copy()
    base[mask] = converted[mask]
    alpha_ch = np.asarray(img.convert("RGBA"))[..., 3]
    return Image.fromarray(np.dstack([base, alpha_ch]), mode="RGBA")


def verify(
    path: Path,
    expected_size: int,
    target: tuple[int, int, int] | None,
    check_colour: bool,
    monochrome: bool,
) -> list[str]:
    """Check the file ON DISK, not the image we still hold in memory.

    `check_colour` is False for the small densities ON PURPOSE. At 48px the
    artwork is a handful of antialiased pixels blended into the plate, so the
    "dominant saturated colour" of a downscaled icon is an edge artefact and not
    the palette - the first run failed three mdpi files for exactly that reason.
    Colour is therefore checked once per theme, on the largest density, where
    the measurement means something; the smaller rasters are still checked for
    size, plate and artwork presence.

    `monochrome` is its own flag and NOT "target is None": None once meant both
    "the source theme, skip the colour check" and "this theme has no colour",
    and the run then told the orange icon it had gained colour it was supposed
    to have.
    """
    problems = []
    img = Image.open(path).convert("RGBA")
    if img.size != (expected_size, expected_size):
        problems.append(f"size {img.size} != ({expected_size}, {expected_size})")
    arr = np.asarray(img).astype(np.int16)
    rgb, alpha = arr[..., :3], arr[..., 3]
    sat = rgb.max(-1) - rgb.min(-1)
    plate = (alpha >= 250) & (sat < 30)
    # A dark icon must not keep ANY near-white plate: that was the original bug.
    if plate.any() and float(rgb[plate].mean()) > 120:
        problems.append(f"plate luminance {rgb[plate].mean():.0f} > 120 (white residue)")
    if target is not None and check_colour:
        got = dominant_artwork_colour(path)
        if got is None:
            problems.append("no saturated artwork pixels at all")
        else:
            hue_got, hue_want = hue_of(got), hue_of(target)
            if hue_want >= 0 and hue_got >= 0:
                dist = hue_distance_deg(hue_got, hue_want)
                if dist > HUE_TOLERANCE_DEG:
                    problems.append(
                        f"artwork hue {hue_got:.0f}deg is {dist:.0f}deg from the "
                        f"theme hue {hue_want:.0f}deg ({got} vs {target})"
                    )
            arr_rgb = np.asarray(got, dtype=np.float32)
            mx, mn = float(arr_rgb.max()), float(arr_rgb.min())
            sat = (mx - mn) / mx * 255.0 if mx > 0 else 0.0
            if sat < SAT_FLOOR:
                problems.append(
                    f"artwork saturation {sat:.1f} below the vividness floor "
                    f"{SAT_FLOOR:.1f}: the icon reads passive on a dark plate"
                )
            if mx < VAL_FLOOR:
                problems.append(
                    f"artwork value {mx:.1f} below the vividness floor {VAL_FLOOR:.1f}"
                )
    if monochrome:
        # A monochrome theme must STAY monochrome - that is the whole point of it.
        if ((alpha >= 250) & (sat >= ARTWORK_MIN_SAT)).any():
            problems.append("monochrome theme gained colour")
    return problems


def preview(cells: list[tuple[str, Image.Image]], path: Path) -> None:
    cell, pad = 150, 12
    cols = 5
    rows = (len(cells) + cols - 1) // cols
    canvas = Image.new("RGB", (pad + cols * (cell + pad), pad + rows * (cell + pad)), (24, 24, 24))
    for i, (_, im) in enumerate(cells):
        x = pad + (i % cols) * (cell + pad)
        y = pad + (i // cols) * (cell + pad)
        plate = Image.new("RGBA", (cell, cell), (18, 18, 18, 255))
        plate.alpha_composite(im.convert("RGBA").resize((cell, cell)))
        canvas.paste(plate.convert("RGB"), (x, y))
    canvas.save(path, "PNG")
    print(f"preview sheet: {path}")


def main(write: bool) -> int:
    if not SOURCE_PNG.exists():
        print(f"source artwork not found: {SOURCE_PNG}")
        return 1

    source = Image.open(SOURCE_PNG).convert("RGBA")
    source_colour = dominant_colour_of(source)
    print(f"source: {SOURCE_PNG.name} {source.size}")
    print(f"source artwork colour: {source_colour}")

    themes = NIGHT_ONLY_THEMES + SINGLE_ASSET_DARK_THEMES
    targets: dict[str, tuple[int, int, int] | None] = {}
    greys: dict[str, float] = {}
    for theme in themes:
        ref_img, ref_label = reference_image(theme)
        colour = dominant_colour_of(ref_img)
        targets[theme] = colour
        if colour is None:
            greys[theme] = artwork_peak_luminance_from(ref_img)
            note = f"  <- monochrome by design; greyscale copy at peak lum {greys[theme]:.0f}"
        else:
            note = ""
        print(f"  {theme:9s} target {str(colour):18s} from {ref_label}{note}")

    cells = []
    failures = 0
    for theme in themes:
        target = targets[theme]
        if theme == SOURCE_THEME:
            art = source
            state = "applied as-is (the owner's own render, same colour)"
        elif target is None:
            art = desaturate(source, greys[theme])
            state = f"greyscale copy at peak lum {greys[theme]:.0f} (monochrome theme)"
        else:
            art = recolour(source, target)
            # Lift a recoloured theme to the vividness floor. The source theme
            # skips this: it IS the reference, already above both floors.
            art = pungency(art)
            arr = np.asarray(art).astype(np.int16)
            m = (arr[..., 3] >= 250) & ((arr[..., :3].max(-1) - arr[..., :3].min(-1)) >= ARTWORK_MIN_SAT)
            got = Counter(map(tuple, arr[..., :3][m])).most_common(1)[0][0] if m.any() else None
            state = f"recoloured -> {tuple(int(v) for v in got)}"
        print(f"\n{theme}: {state}")
        cells.append((theme, art))

        if not write:
            continue
        if theme in NIGHT_ONLY_THEMES:
            pairs = [(f"drawable-night-{d}", f"ic_launcher_fg_{theme}.png", FG_SIZES[d]) for d in FG_SIZES]
            pairs += [(f"mipmap-night-{d}", f"ic_launcher_{theme}.png", MIPMAP_SIZES[d]) for d in MIPMAP_SIZES]
        else:
            pairs = [(f"drawable-{d}", f"ic_launcher_fg_{theme}.png", FG_SIZES[d]) for d in FG_SIZES]
            pairs += [(f"mipmap-{d}", f"ic_launcher_{theme}.png", MIPMAP_SIZES[d]) for d in MIPMAP_SIZES]
        for sub, name, size in pairs:
            dst_dir = RES / sub
            dst_dir.mkdir(parents=True, exist_ok=True)
            dst = dst_dir / name
            art.resize((size, size), Image.LANCZOS).save(dst, "PNG")
            # Colour is judged once per theme, at the largest density, where the
            # measurement is the palette rather than an antialiasing artefact.
            problems = verify(
                dst, size,
                # The source theme is checked against the SOURCE artwork's own
                # colour, not the colour it replaces: the owner re-rendered it
                # brighter (249,122,24 vs the old 190,81,0), and calling that a
                # mismatch would be reporting the owner's intent as a defect.
                source_colour if theme == SOURCE_THEME else target,
                check_colour=(size == max(FG_SIZES.values())),
                monochrome=(target is None),
            )
            if problems:
                failures += 1
                print(f"    {sub}/{name}  <-- {'; '.join(problems)}")
            else:
                print(f"    {sub}/{name}  {size}px  ok")

    if cells:
        preview(cells, REPO / "tools" / "icon-night-refresh-preview.png")
    print(f"\n{failures} file(s) failed verification")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main("--write" in sys.argv))
