#!/usr/bin/env python3
"""Give the LIGHT-plate launcher themes a proper dark-mode icon.

THE BUG
-------
The owner reports, in dark mode, "the border between the clean orange and the
background has a thin white bad-quality border which ruins the whole icon".

Measured, that is not a rendering artefact in the foreground PNG - it is the
PLATE. The icon is an adaptive icon: a flat <background> colour under a
<foreground> drawable, and the foreground's plate is the white surface the
orange artwork sits on.

    theme    adaptive bg   plate lum on the midline
    orange   icon_bg_white     252.7   <- default theme, WHITE plate
    ocean    icon_bg_dark       45.0
    flame    icon_bg_dark       45.0
    rose     icon_bg_dark       45.0
    purple   icon_bg_dark       45.0
    retro    icon_bg_dark       46.0
    azure    icon_bg_white     252.7
    crimson  icon_bg_white     252.7
    emerald  icon_bg_white     252.7
    beige    icon_bg_white     252.7

Five themes already carry the dark plate, five still carry the white one, and
there is NO mipmap-anydpi-v26-night directory at all - so one XML serves both
modes and a light-plate theme shows a white plate on a dark home screen. The
"white border" is that plate's edge against the launcher's dark backdrop, and
the reason it reads as a hairline is the 75% adaptive crop: the plate fills the
masked shape, so all that survives beside the artwork is a rim.

The DEFAULT theme is "DEFAULT", which Theme.kt resolves with isSystemInDarkTheme
- so the single most common configuration, a light-plate default icon on a dark
phone, is the worst case.

THE FIX
-------
Reuse the existing, carefully-built dark-plate converter rather than inventing
a second one (tools/icon-dark-variants.py). The plate map is linear in
luminance, so its blends stay blends and the 3D light-play survives.

This script's job is the part that script deliberately skipped: it only ever
converted the five themes Theme.kt reports as dark and left the light four
alone. Here the night variants are written into drawable-night-*/ and
mipmap-night-*/, so Android picks the dark plate ONLY in dark mode and the
light plate stays for light mode. That is what "not a cheap photoshop" needs:
a real resource qualifier, not one asset trying to be two.

Run from the repo root:  python tools/icon-night-variants.py [--write]
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
# icon-dark-variants.py has hyphens, so it is not importable by name. Load it
# from its path rather than duplicating the plate map - the whole point is that
# the night variants use the SAME converter as the dark themes, so a future
# change to the plate design lands in both.
import importlib.util  # noqa: E402

_spec = importlib.util.spec_from_file_location(
    "icon_dark_variants", Path(__file__).resolve().parent / "icon-dark-variants.py"
)
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)
GROUND = _mod.GROUND
PLATE_CORE = _mod.PLATE_CORE
PLATE_RIM = _mod.PLATE_RIM
SAT_ART = _mod.SAT_ART
convert = _mod.convert

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp" / "src" / "main" / "res"
DENSITIES = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]

# Theme.kt's LIGHT themes, plus DEFAULT (which follows the system and so must
# be correct in BOTH modes). The five dark themes already ship a dark plate.
LIGHT_PLATE_THEMES = ["orange", "azure", "crimson", "emerald", "beige"]
DEFAULT_THEME = "orange"      # android:icon in the manifest's main activity

# The themes that ALREADY carry a dark plate in the light-mode file. They need a
# night XML too, not because the plate is wrong but because a resource that
# exists only unqualified is what Android falls back to when no night variant
# is present - and the fallback is exactly the thing that is easy to get wrong.
# Copying the file makes the dark intent explicit and the set complete.
ALREADY_DARK_THEMES = ["flame", "ocean", "rose", "purple", "retro"]

# A converted plate must not keep ANY near-white. The light plate is ~253, and
# the dark design tops out at PLATE_RIM, so anything above this in a neutral
# pixel is residue from the light plate rather than a deliberate highlight.
WHITE_RESIDUE = 120.0


def has_light_plate(path: Path) -> bool:
    arr = np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)
    rgb, alpha = arr[..., :3], arr[..., 3]
    lum = rgb.mean(-1)
    sat = rgb.max(-1) - rgb.min(-1)
    neutral_opaque = (sat < SAT_ART) & (alpha >= 250)
    if not neutral_opaque.any():
        return False
    return float(lum[neutral_opaque].max()) > 200.0


def residue(path: Path) -> float:
    """Max luminance among neutral OPAQUE pixels, composited over the dark ground."""
    arr = np.asarray(Image.open(path).convert("RGBA"), dtype=np.float32)
    rgb, alpha = arr[..., :3], arr[..., 3]
    a = (alpha / 255.0)[..., None]
    comp = rgb * a + GROUND * (1.0 - a)
    lum = comp.mean(-1)
    sat = comp.max(-1) - comp.min(-1)
    mask = (alpha >= 250) & (sat < SAT_ART)
    return float(lum[mask].max()) if mask.any() else 0.0


def artwork_drift(original: Path, converted: Image.Image) -> float:
    o = np.asarray(Image.open(original).convert("RGBA"), dtype=np.float32)
    c = np.asarray(converted.convert("RGBA"), dtype=np.float32)
    mask = (o[..., 3] >= 250) & ((o[..., :3].max(-1) - o[..., :3].min(-1)) >= SAT_ART)
    if not mask.any():
        return 0.0
    return float(np.abs(c[..., :3][mask] - o[..., :3][mask]).max())


def save_preview(pairs, path: Path) -> None:
    cell, pad = 168, 10
    W = pad + 2 * (cell + pad)
    H = pad + len(pairs) * (cell + pad)
    canvas = Image.new("RGB", (W, H), (90, 90, 90))
    for i, (theme, light_path, dark) in enumerate(pairs):
        y = pad + i * (cell + pad)
        lc = Image.new("RGBA", (cell, cell), (255, 255, 255, 255))
        lc.alpha_composite(Image.open(light_path).convert("RGBA").resize((cell, cell)))
        dc = Image.new("RGBA", (cell, cell), (int(GROUND),) * 3 + (255,))
        dc.alpha_composite(dark.resize((cell, cell)))
        canvas.paste(lc.convert("RGB"), (pad, y))
        canvas.paste(dc.convert("RGB"), (pad + cell + pad, y))
    canvas.save(path, "PNG")
    print(f"preview sheet (light | night): {path}")


def main(write: bool) -> int:
    print(f"light-plate themes needing a night variant: {LIGHT_PLATE_THEMES}")
    print(f"default theme: {DEFAULT_THEME} (Theme.kt: DEFAULT -> isSystemInDarkTheme)\n")

    previews = []
    for theme in LIGHT_PLATE_THEMES:
        for sub, prefix, night_sub in [
            (f"drawable-{d}", "ic_launcher_fg_", f"drawable-night-{d}") for d in DENSITIES
        ] + [
            (f"mipmap-{d}", "ic_launcher_", f"mipmap-night-{d}") for d in DENSITIES
        ]:
            src = RES / sub / f"{prefix}{theme}.png"
            if not src.exists():
                print(f"MISSING: {src}")
                continue
            if not has_light_plate(src):
                print(f"{sub}/{prefix}{theme}.png: already dark, skipped")
                continue

            out = convert(Image.open(src), inverted=False)
            res = artwork_drift(src, out)
            print(
                f"{night_sub}/{prefix}{theme}.png  "
                f"plate->{PLATE_CORE:.0f}..{PLATE_RIM:.0f}  "
                f"artwork drift {res:.0f}"
            )
            if write:
                dst_dir = RES / night_sub
                dst_dir.mkdir(parents=True, exist_ok=True)
                dst = dst_dir / f"{prefix}{theme}.png"
                out.save(dst, "PNG")
                # Verify what actually landed on disk, not what we held in RAM.
                left = residue(dst)
                flag = "  <-- WHITE RESIDUE" if left > WHITE_RESIDUE else ""
                print(f"    wrote {dst.relative_to(REPO)}  "
                      f"neutral-opaque max over #121212 = {left:.1f}{flag}")
            if sub == "drawable-xxxhdpi":
                previews.append((theme, src, out))

    # The adaptive XML needs a night variant too, or Android keeps using the
    # light one and the foreground we just built is never selected.
    # The directory is mipmap-NIGHT-anydpi-v26, not mipmap-anydpi-v26-night:
    # aapt2 rejects the other order as an invalid resource directory name, and
    # the build fails rather than warning.
    if write:
        anydpi_night = RES / "mipmap-night-anydpi-v26"
        anydpi_night.mkdir(parents=True, exist_ok=True)
        for theme in LIGHT_PLATE_THEMES + ALREADY_DARK_THEMES:
            light_xml = RES / "mipmap-anydpi-v26" / f"ic_launcher_{theme}.xml"
            text = light_xml.read_text(encoding="utf-8")
            # Same file, dark background: the plate now lives in the foreground.
            night = text.replace("@color/icon_bg_white", "@color/icon_bg_dark")
            assert "@color/icon_bg_white" not in night, (
                f"{theme}: night icon still references the white background"
            )
            (anydpi_night / light_xml.name).write_text(night, encoding="utf-8")
            print(f"wrote {(anydpi_night / light_xml.name).relative_to(REPO)}")

    if previews:
        save_preview(previews, REPO / "tools" / "icon-night-preview.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main("--write" in sys.argv))
