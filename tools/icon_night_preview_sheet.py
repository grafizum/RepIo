#!/usr/bin/env python3
"""The dark icon candidates as a LAUNCHER actually shows them.

    ~/sdgen/venv/bin/python tools/icon_night_preview_sheet.py

The first preview drew the icon as a plain square crop, which made the plate's
own edge look like a border and sent the fix in the wrong direction. A launcher
masks an adaptive icon to a squircle and puts it on the user's wallpaper, so
that is what this draws: the mask, the wallpaper, and the real 96px/192px sizes.
A zoom of the artwork edge sits next to each one, because that is where a glow
is actually visible.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parents[1]
RES = REPO / "composeApp/src/main/res"
FIX = REPO / "build/reports/icon-night-fix"
OUT = REPO / "build/reports/icon-night-border-preview.png"

WALL = (0x2B, 0x36, 0x4A, 255)      # a plausible wallpaper, so the mask is visible
LIGHT_BG = (0xFF, 0xFF, 0xFF, 255)


def font(px: int, bold: bool = False):
    names = ["DejaVuSans-Bold.ttf"] if bold else ["DejaVuSans.ttf"]
    for n in names:
        for b in ("/usr/share/fonts/truetype/dejavu/", "/usr/share/fonts/truetype/liberation/"):
            p = Path(b) / n
            if p.exists():
                return ImageFont.truetype(str(p), px)
    return ImageFont.load_default()


def squircle_mask(size: int) -> Image.Image:
    """The launcher's mask: a superellipse, which is what makes an adaptive icon
    read as an icon rather than as a square somebody pasted onto the wallpaper."""
    n = 4.2
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float64)
    cx = cy = (size - 1) / 2
    dx, dy = np.abs(xs - cx) / (size / 2), np.abs(ys - cy) / (size / 2)
    inside = (dx ** n + dy ** n) <= 1.0
    mask = np.zeros((size, size), dtype=np.uint8)
    mask[inside] = 255
    return Image.fromarray(mask, "L")


def launcher(fg: Image.Image, bg, size: int) -> Image.Image:
    """Adaptive icon the way Android builds it: flat bg, foreground, 75% crop."""
    canvas = Image.new("RGBA", (size, size), bg)
    canvas.alpha_composite(fg.convert("RGBA").resize((size, size), Image.LANCZOS))
    inset = int(size * 0.125)
    icon = canvas.crop((inset, inset, size - inset, size - inset))
    icon.putalpha(Image.composite(
        Image.new("L", icon.size, 255), Image.new("L", icon.size, 0), squircle_mask(icon.size[0])
    ))
    return icon


def edge_zoom(fg: Image.Image, bg, box: int = 2) -> Image.Image:
    """A 2x zoom of one edge of the artwork, where a glow is visible."""
    canvas = Image.new("RGBA", (fg.size[0], fg.size[1]), bg)
    canvas.alpha_composite(fg.convert("RGBA"))
    inset = int(canvas.size[0] * 0.125)
    canvas = canvas.crop((inset, inset, canvas.size[0] - inset, canvas.size[1] - inset))
    w = canvas.size[0]
    cx, cy = int(w * 0.30), int(w * 0.30)   # the top-left of the barbell
    half = w // 8
    crop = canvas.crop((max(0, cx - half), max(0, cy - half), cx + half, cy + half))
    return crop.resize((160, 160), Image.NEAREST)


def main() -> None:
    light = Image.open(RES / "drawable-xxxhdpi/ic_launcher_fg_orange.png")
    rows = [
        ("Light mode today", launcher(light, LIGHT_BG, 192), edge_zoom(light, LIGHT_BG)),
        ("Dark mode today", launcher(Image.open(RES / "drawable-night-xxxhdpi/ic_launcher_fg_orange.png"), (0x12, 0x12, 0x12, 255), 192),
         edge_zoom(Image.open(RES / "drawable-night-xxxhdpi/ic_launcher_fg_orange.png"), (0x12, 0x12, 0x12, 255))),
    ]
    for variant, title in (("ramp", "Fixed - dark plate, shading kept"),
                           ("flat", "Fixed - flat plate"),
                           ("bare", "Fixed - mark only, no shadow")):
        p = FIX / f"orange-{variant}-xxxhdpi-432.png"
        if not p.is_file():
            continue
        fg = Image.open(p)
        rows.append((title, launcher(fg, (0x12, 0x12, 0x12, 255), 192), edge_zoom(fg, (0x12, 0x12, 0x12, 255))))

    W, row_h = 1140, 250
    H = 120 + row_h * len(rows)
    img = Image.new("RGBA", (W, H), (0x16, 0x18, 0x1D, 255))
    d = ImageDraw.Draw(img)
    d.text((30, 20), "RepIo dark icon - what the home screen will show", font=font(26, True), fill=(255, 255, 255))
    d.text((30, 56),
           "Left: masked to a launcher squircle on a wallpaper, 192px and 96px. "
           "Right: a 2x zoom of the artwork edge, where the border actually shows.",
           font=font(13), fill=(0x9A, 0x9A, 0xA5))

    for i, (label, icon, zoom) in enumerate(rows):
        y = 110 + i * row_h
        img.alpha_composite(icon, (30, y))
        img.alpha_composite(icon.resize((96, 96), Image.LANCZOS), (240, y + 48))
        d.text((360, y + 40), label, font=font(17, True), fill=(0xEC, 0xEC, 0xF2))
        # Zoom on a neutral card so both dark variants are comparable.
        card = Image.new("RGBA", (180, 180), (0x24, 0x26, 0x2C, 255))
        card.alpha_composite(zoom, (10, 10))
        img.alpha_composite(card, (360, y + 66))
        d.text((556, y + 120), "edge zoom", font=font(12), fill=(0x88, 0x88, 0x92))

    img.convert("RGB").save(OUT, "PNG")
    print(f"preview: {OUT}")


if __name__ == "__main__":
    main()
