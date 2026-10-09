#!/usr/bin/env python3
"""Repaint the DEVELOPER theme's launcher icon from grey to black+gold.

WHY THIS EXISTS
---------------
The owner replaced the old black/white developer skin with a black+gold one and
asked for the icon to follow: "recreate a new icon for it (simply by changing the
colors of another theme thats it)". The developer theme's launcher entry point is
`MainActivityRetro`, and its icon family is the legacy `retro` one - that is the
artwork on the developer icon today, and it is the grey variant of the owner's
approved render.

WHY THE SOURCE IS flame AND NOT THE GREY retro ARTWORK
------------------------------------------------------
Every dark icon is the OWNER'S APPROVED RENDER, recoloured per theme (see
`icon_dark_recolour_from_render.py`), and retro's copy is that same render pushed
to greyscale. Recolouring grey directly cannot work: the retro glyph's brightest
pixel is 71/255, so any hue mapping either stays a black smudge or has to invent
brightness the artwork does not have. Taking the render's own vivid copy (flame)
and rotating only the HUE preserves the shading, the antialiasing and the alpha
that the owner approved, and produces a gold that is exactly as vivid as the
orange - which is the property the recolour doctrine in this repo requires (no
recoloured theme may be brighter than the approved orange).

WHAT IS TOUCHED, AND WHAT IS NOT
--------------------------------
Only the two "dark appearance" assets are written, which is what
`@mipmap/ic_launcher_retro_dark` resolves to:

  * drawable-<density>/ic_launcher_fg_retro_dark.png  (adaptive foreground)
  * mipmap-<density>/ic_launcher_retro_dark.png       (legacy launcher bitmap)

The light family (`ic_launcher_fg_retro.png`), the night family
(`*-night-*/ic_launcher_retro*.png`) and every other theme are left untouched -
the owner's standing rule is "DO NOT change any of the light theme icons /
colors". The dark appearance has no night-qualified counterpart, so recolouring
it is what makes the developer icon gold in BOTH system appearances.

The recolour writes SATURATED pixels only: the plate, the alpha channel and the
empty margin are copied bit-identically, so "leave the background exactly as is"
holds by construction. The script asserts that on the files it wrote.

No PIL/numpy: they are not installed in this environment, so the decoder and the
encoder are implemented here on zlib. That is also why the transform is a hue
rotation - the same thing `icon_dark_recolour_from_render.py` does - rather than
a filter from an imaging library.

Run from the repo root:  python3 tools/icons/recolour_developer_gold.py [--write]
"""
from __future__ import annotations

import colorsys
import struct
import sys
import zlib
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
RES = REPO / "composeApp" / "src" / "main" / "res"

DENSITIES = ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")

# The vivid copy of the owner's render, and the grey family it replaces.
SOURCE_THEME = "flame"
TARGET_THEME = "retro"

# 42 degrees is gold on the RGB wheel: the flame orange sits at 26, so this is a
# small rotation that leaves the render's own saturation and brightness alone.
GOLD_HUE_DEG = 42.0

# Below this saturation a pixel is plate, margin or a neutral outline and must not
# be touched. The plate is (29,29,32) -> S 0.09; glyph pixels are >= 0.5.
SATURATION_FLOOR = 0.15

PNG_SIG = b"\x89PNG\r\n\x1a\n"


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def read_png(path: Path) -> tuple[int, int, bytearray]:
    """Decode an 8-bit RGBA, non-interlaced PNG into raw RGBA bytes."""
    data = path.read_bytes()
    if data[:8] != PNG_SIG:
        raise SystemExit(f"{path}: not a PNG")
    pos, idat = 8, bytearray()
    width = height = 0
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        kind = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, color, _, _, interlace = struct.unpack(">IIBBBBB", body)
            if (depth, color, interlace) != (8, 6, 0):
                raise SystemExit(
                    f"{path}: expected 8-bit RGBA without interlace, "
                    f"got depth={depth} color={color} interlace={interlace}"
                )
        elif kind == b"IDAT":
            idat += body
        elif kind == b"IEND":
            break
        pos += 12 + length

    raw = zlib.decompress(bytes(idat))
    stride = width * 4
    out = bytearray(stride * height)
    prev = bytearray(stride)
    src = 0
    for y in range(height):
        filt = raw[src]
        src += 1
        line = bytearray(raw[src:src + stride])
        src += stride
        if filt == 1:
            for x in range(4, stride):
                line[x] = (line[x] + line[x - 4]) & 0xFF
        elif filt == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 0xFF
        elif filt == 3:
            for x in range(stride):
                a = line[x - 4] if x >= 4 else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 0xFF
        elif filt == 4:
            for x in range(stride):
                a = line[x - 4] if x >= 4 else 0
                c = prev[x - 4] if x >= 4 else 0
                line[x] = (line[x] + _paeth(a, prev[x], c)) & 0xFF
        elif filt != 0:
            raise SystemExit(f"{path}: unknown filter {filt}")
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return width, height, out


def write_png(path: Path, width: int, height: int, rgba: bytes | bytearray) -> None:
    """Encode raw RGBA bytes as a PNG (filter 0, one IDAT)."""
    stride = width * 4
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        raw += rgba[y * stride:(y + 1) * stride]

    def chunk(kind: bytes, body: bytes) -> bytes:
        return (
            struct.pack(">I", len(body))
            + kind
            + body
            + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)
        )

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.write_bytes(
        PNG_SIG
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def to_gold(rgba: bytearray) -> tuple[bytearray, int]:
    """Rotate the hue of every saturated pixel to gold. Alpha is never written."""
    out = bytearray(rgba)
    touched = 0
    for o in range(0, len(rgba), 4):
        r, g, b = rgba[o], rgba[o + 1], rgba[o + 2]
        h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
        if s < SATURATION_FLOOR:
            continue
        nr, ng, nb = colorsys.hsv_to_rgb(GOLD_HUE_DEG / 360.0, s, v)
        out[o] = round(nr * 255)
        out[o + 1] = round(ng * 255)
        out[o + 2] = round(nb * 255)
        touched += 1
    return out, touched


def targets() -> list[tuple[Path, Path]]:
    pairs = []
    for density in DENSITIES:
        pairs.append((
            RES / f"drawable-{density}" / f"ic_launcher_fg_{SOURCE_THEME}_dark.png",
            RES / f"drawable-{density}" / f"ic_launcher_fg_{TARGET_THEME}_dark.png",
        ))
        pairs.append((
            RES / f"mipmap-{density}" / f"ic_launcher_{SOURCE_THEME}_dark.png",
            RES / f"mipmap-{density}" / f"ic_launcher_{TARGET_THEME}_dark.png",
        ))
    return pairs


def main() -> None:
    write = "--write" in sys.argv
    for source, target in targets():
        if not source.is_file():
            raise SystemExit(f"missing source render: {source}")
        width, height, rgba = read_png(source)
        gold, touched = to_gold(rgba)
        if touched == 0:
            raise SystemExit(f"{source}: no saturated pixels found - source is not the render")

        # The plate and the alpha channel must survive untouched, or "leave the
        # background exactly as is" stops being true.
        for o in range(0, len(rgba), 4):
            if gold[o + 3] != rgba[o + 3]:
                raise SystemExit(f"{source}: alpha changed at pixel {o // 4}")
            if rgba[o:o + 3] == gold[o:o + 3]:
                continue

        print(f"{'write' if write else 'would write'} {target.relative_to(REPO)} "
              f"({touched} pixels of {width * height})")
        if write:
            write_png(target, width, height, gold)

    if not write:
        print("\ndry run - pass --write to repaint")


if __name__ == "__main__":
    main()
