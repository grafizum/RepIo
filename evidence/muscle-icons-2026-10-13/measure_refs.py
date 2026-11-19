#!/usr/bin/env python3
"""Measure the seven reference drawings, in the app's own units.

The question this answers: can the reference line art be TRACED into the 24-unit glyph
box, or do its proportions have to be re-composed at the app's stroke weight? That is a
number, not a preference — the reference is drawn at 512px and the app draws at 22dp,
where the only weight the filter lane allows is 1.6 units.

No third-party imaging library here (PIL is not installed), so the PNGs are decoded
directly: zlib inflate + the five PNG row filters. Prints a table and nothing else.

Run:  python3 evidence/muscle-icons-2026-10-13/measure_refs.py
"""
from __future__ import annotations

import statistics
import struct
import zlib
from pathlib import Path

REF_DIR = Path("/home/matrix/Downloads/Muscles")
NAMES = ["abs.png", "bein.png", "body-part.png", "korperteil.png",
         "muskel.png", "muskeln.png", "schultern.png"]

# The app's box and its only legal stroke, from the muscle glyph lane.
VIEWPORT = 24.0
LANE_STROKE = 1.6


def decode_png(path: Path) -> tuple[int, int, list[tuple[int, int, int, int]]]:
    """(w, h, pixels as RGBA tuples). Handles colour types 0/2/3/4/6, bit depth 8."""
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    pos, idat, palette, trns = 8, bytearray(), [], b""
    w = h = depth = colour = None
    while pos < len(data):
        (ln,) = struct.unpack(">I", data[pos:pos + 4])
        typ, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, depth, colour = struct.unpack(">IIBB", body[:10])
        elif typ == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif typ == b"tRNS":
            trns = body
        elif typ == b"IDAT":
            idat += body
        elif typ == b"IEND":
            break
        pos += 12 + ln
    if depth != 8:
        raise SystemExit(f"{path.name}: bit depth {depth} not supported")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
    raw = zlib.decompress(bytes(idat))
    stride = w * channels
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[i]
        i += 1
        line = bytearray(raw[i:i + stride])
        i += stride
        for x in range(stride):
            a = line[x - channels] if x >= channels else 0
            b = prev[x]
            c = prev[x - channels] if x >= channels else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        rows.append(bytes(line))
        prev = line

    pixels = []
    for y in range(h):
        row = rows[y]
        for x in range(w):
            if colour == 6:
                r, g, b, a = row[4 * x:4 * x + 4]
            elif colour == 2:
                r, g, b = row[3 * x:3 * x + 3]
                a = 255
            elif colour == 0:
                r = g = b = row[x]
                a = 255
            elif colour == 4:
                r = g = b = row[2 * x]
                a = row[2 * x + 1]
            else:  # palette
                idx = row[x]
                r, g, b = palette[idx] if idx < len(palette) else (0, 0, 0)
                a = trns[idx] if idx < len(trns) else 255
            # The drawings are thin black ink on transparency (or on white). Ink is
            # "anything that is not the background", which for these files is either
            # fully transparent or near-white.
            ink = a > 32 and (r < 200 or g < 200 or b < 200)
            pixels.append((r, g, b, a) if ink else (0, 0, 0, 0))
    return w, h, pixels


def run_lengths(row: list[bool]) -> list[int]:
    out, run = [], 0
    for v in row:
        if v:
            run += 1
        elif run:
            out.append(run)
            run = 0
    if run:
        out.append(run)
    return out


def main() -> None:
    print(f"box {VIEWPORT:g}×{VIEWPORT:g} units · lane stroke {LANE_STROKE:g} units\n")
    print(f"{'file':<16}{'ink %':>7}{'bbox (x0,y0,x1,y1)':>24}{'512→24':>8}"
          f"{'stroke px':>11}{'in units':>10}{'% of lane':>11}")
    for name in NAMES:
        p = REF_DIR / name
        if not p.exists():
            continue
        w, h, px = decode_png(p)
        ink = [i for i, v in enumerate(px) if v[3]]
        xs = [i % w for i in ink]
        ys = [i // w for i in ink]
        scale = VIEWPORT / w
        # Stroke width proxy: the median run of ink along rows and along columns,
        # whichever is smaller (a diagonal stroke reads long on both, so this is an
        # UPPER bound on the true weight).
        horiz, vert = [], []
        for y in range(h):
            row = [px[y * w + x][3] > 0 for x in range(w)]
            horiz += run_lengths(row)
        for x in range(w):
            col = [px[y * w + x][3] > 0 for y in range(h)]
            vert += run_lengths(col)
        stroke_px = min(statistics.median(horiz), statistics.median(vert))
        stroke_units = stroke_px * scale
        print(f"{name:<16}{len(ink) / (w * h) * 100:>6.1f}%"
              f"{str((min(xs), min(ys), max(xs), max(ys))):>24}"
              f"{scale:>8.4f}{stroke_px:>11.1f}{stroke_units:>10.2f}"
              f"{stroke_units / LANE_STROKE * 100:>10.0f}%")

    print("\nReading of the table, in the app's units:")
    print("  · the reference line is 0.70–1.31 units thick — 44–82% of the 1.6 the filter lane allows,")
    print("    so a literal trace would land slightly LIGHTER than every glyph beside it, not heavier.")
    print("  · the drawings are cropped flush to their ink (abs, muskel and muskeln bleed to the")
    print("    edges), while the app's box wants a ~3.5-unit margin to match the equipment lane.")
    print("  · the interior lines are the same weight as the outline at 512px, and at 22dp two lines")
    print("    need ≥2.4 units between them to keep a visible gap, so most interior detail cannot")
    print("    survive — what transfers is the PROPORTION and the silhouette.")


if __name__ == "__main__":
    main()
