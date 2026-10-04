#!/usr/bin/env python3
"""Summarise an Android screenshot without needing to look at it.

Reports the dominant colours, how much of the frame is non-background, and a
coarse ASCII map, so an on-device render can be verified from a shell.

Usage: python tools/inspect_shot.py build/reports/shots/tree5.png
"""
from __future__ import annotations

import collections
import struct
import sys
import zlib


def load_png(path: str):
    data = open(path, "rb").read()
    pos, idat = 8, b""
    w = h = bitdepth = colortype = None
    palette = b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos : pos + 4])[0]
        ctype = data[pos + 4 : pos + 8]
        body = data[pos + 8 : pos + 8 + length]
        if ctype == b"IHDR":
            w, h, bitdepth, colortype = struct.unpack(">IIBB", body[:10])
        elif ctype == b"PLTE":
            palette = body
        elif ctype == b"IDAT":
            idat += body
        pos += 12 + length
    if bitdepth != 8:
        raise SystemExit(f"only 8-bit PNGs supported, got {bitdepth}")

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colortype]
    stride = w * channels
    raw = zlib.decompress(idat)
    out = bytearray(h * stride)
    prev = bytearray(stride)
    i = 0
    for y in range(h):
        f = raw[i]
        i += 1
        line = bytearray(raw[i : i + stride])
        i += stride
        if f == 1:
            for x in range(channels, stride):
                line[x] = (line[x] + line[x - channels]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                b = prev[x]
                c = prev[x - channels] if x >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pred = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pred) & 255
        out[y * stride : (y + 1) * stride] = line
        prev = line

    def rgb(x, y):
        o = y * stride + x * channels
        if colortype == 3:
            idx = out[o] * 3
            return tuple(palette[idx : idx + 3])
        px = out[o : o + 3]
        return tuple(px) if len(px) == 3 else (px[0],) * 3

    return w, h, rgb


def main() -> int:
    path = sys.argv[1]
    w, h, rgb = load_png(path)
    print(f"{path}: {w}x{h}")

    counter = collections.Counter()
    for y in range(0, h, 5):
        for x in range(0, w, 5):
            counter[rgb(x, y)] += 1
    total = sum(counter.values())
    print(f"distinct sampled colours: {len(counter)}")
    for colour, n in counter.most_common(8):
        print(f"  #{colour[0]:02X}{colour[1]:02X}{colour[2]:02X}  {100.0*n/total:5.1f}%")

    bg = counter.most_common(1)[0][0]
    ink = sum(n for c, n in counter.items() if c != bg)
    print(f"non-background pixels: {100.0*ink/total:.1f}%")

    # Coarse map: '#' where the pixel differs from the background.
    print("\ncoarse map (rows top->bottom):")
    rows, cols = 30, 40
    for ry in range(rows):
        line = ""
        for rx in range(cols):
            x = int((rx + 0.5) * w / cols)
            y = int((ry + 0.5) * h / rows)
            line += "#" if rgb(x, y) != bg else "."
        print("  " + line)
    return 0


if __name__ == "__main__":
    sys.exit(main())