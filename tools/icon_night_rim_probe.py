#!/usr/bin/env python3
"""Measure the rim that the owner sees as a white border in dark mode.

    ~/sdgen/venv/bin/python tools/icon_night_rim_probe.py

The launcher crops an adaptive icon to its middle 75%, so what survives around
the artwork IS the plate's rim. This reports, for both the light and the night
foreground, the luminance of the plate pixels in a ring around the artwork and
of the artwork itself, so "there is a white border" becomes a number instead of
an impression.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp/src/main/res"
BG = np.array([0x12, 0x12, 0x12], dtype=np.float32)


def analyse(path: Path, label: str) -> dict[str, float]:
    img = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
    rgb, alpha = img[..., :3], img[..., 3] / 255.0
    # Composite over the launcher's dark backdrop, then crop the way the
    # launcher crops: the middle 75% of the 108dp canvas is all that is visible.
    comp = rgb * alpha[..., None] + BG * (1 - alpha[..., None])
    h, w = comp.shape[:2]
    inset = int(min(h, w) * 0.125)
    comp = comp[inset:h - inset, inset:w - inset]
    lum = comp @ np.array([0.2126, 0.7152, 0.0722], dtype=np.float32)

    mx, mn = comp.max(axis=2), comp.min(axis=2)
    saturation = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    artwork = saturation > 0.25          # the coloured mark
    plate = ~artwork                       # everything neutral: plate, rim, halo

    edge = np.zeros_like(plate)
    for _ in range(14):                    # dilate the artwork to get its surround
        edge |= np.roll(edge, 1, 0) | np.roll(edge, -1, 0) | np.roll(edge, 1, 1) | np.roll(edge, -1, 1)
    surround = plate & ~edge

    return {
        "label": label,
        "artwork_lum": float(lum[artwork].mean()) if artwork.any() else 0.0,
        "surround_lum": float(lum[surround].mean()) if surround.any() else 0.0,
        "surround_p99": float(np.percentile(lum[surround], 99)) if surround.any() else 0.0,
        "surround_max": float(lum[surround].max()) if surround.any() else 0.0,
    }


def main() -> None:
    targets = [
        (RES / "drawable-xxxhdpi/ic_launcher_fg_orange.png", "light icon (white plate)"),
        (RES / "drawable-night-xxxhdpi/ic_launcher_fg_orange.png", "night icon (dark plate)"),
    ]
    for path, label in targets:
        if not path.is_file():
            print(f"{label}: MISSING {path}")
            continue
        r = analyse(path, label)
        print(
            f"{r['label']:<28} artwork lum {r['artwork_lum']:6.1f} | "
            f"plate around it: mean {r['surround_lum']:5.1f}  p99 {r['surround_p99']:5.1f}  "
            f"max {r['surround_max']:5.1f}"
        )


if __name__ == "__main__":
    main()
