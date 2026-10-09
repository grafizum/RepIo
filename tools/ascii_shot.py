#!/usr/bin/env python3
"""Render a screenshot as a letter map so a layout can be read from a shell.

Each cell of the map is the nearest of the most common colours in the frame, so
you can see where a card, a button, a label or a divider actually sits, and
whether it lines up with anything else.

Usage: python tools/ascii_shot.py shot.png [--cols 60] [--rows 60] [--crop y0 y1]
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from inspect_shot import load_png  # noqa: E402

LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"


def main() -> int:
    path = sys.argv[1]
    cols, rows = 60, 60
    crop = None
    args = sys.argv[2:]
    for i, a in enumerate(args):
        if a == "--cols":
            cols = int(args[i + 1])
        if a == "--rows":
            rows = int(args[i + 1])
        if a == "--crop":
            crop = (int(args[i + 1]), int(args[i + 2]))

    w, h, rgb = load_png(path)
    y0, y1 = crop if crop else (0, h)

    counts: dict[tuple[int, int, int], int] = {}
    for y in range(y0, y1, 3):
        for x in range(0, w, 3):
            c = rgb(x, y)
            counts[c] = counts.get(c, 0) + 1
    top = [c for c, _ in sorted(counts.items(), key=lambda kv: -kv[1])][:len(LETTERS)]

    def letter(px):
        return LETTERS[top.index(px)] if px in top else "?"

    print(f"{path}: {w}x{h}  rows {y0}-{y1}")
    for i, c in enumerate(top[:12]):
        n = counts[c]
        print(f"  {LETTERS[i]} = #{c[0]:02X}{c[1]:02X}{c[2]:02X}  {100.0*n/sum(counts.values()):5.1f}%")
    print("  ? = any other colour")

    print()
    bar = "     " + "".join(str((i // 10) % 10) for i in range(cols))
    print(bar)
    print("     " + "".join(str(i % 10) for i in range(cols)))
    for ry in range(rows):
        y = y0 + int((ry + 0.5) * (y1 - y0) / rows)
        line = "".join(letter(rgb(int((rx + 0.5) * w / cols), y)) for rx in range(cols))
        print(f"{y:>4} {line}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
