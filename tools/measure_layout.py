#!/usr/bin/env python3
"""Find the UI element bounding boxes in an Android screenshot and report how
centred each one is, so "the buttons look off and not centred" becomes numbers.

Usage: python tools/measure_layout.py shot.png [--bg RRGGBB] [--min-area N]
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from inspect_shot import load_png  # noqa: E402


def hexs(rgb) -> str:
    return "#%02X%02X%02X" % rgb


def main() -> int:
    path = sys.argv[1]
    bg = None
    min_area = 400
    args = sys.argv[2:]
    for i, a in enumerate(args):
        if a == "--bg":
            bg = tuple(int(args[i + 1][j : j + 2], 16) for j in (0, 2, 4))
        if a == "--min-area":
            min_area = int(args[i + 1])

    w, h, rgb = load_png(path)

    counts: dict[tuple[int, int, int], int] = {}
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            c = rgb(x, y)
            counts[c] = counts.get(c, 0) + 1
    if bg is None:
        bg = max(counts.items(), key=lambda kv: kv[1])[0]
    print(f"{path}: {w}x{h}  background {hexs(bg)}")

    # 4-connected union-find over "not background" pixels.
    parent = list(range(w * h))
    rank = bytearray(w * h)

    def find(a: int) -> int:
        root = a
        while parent[root] != root:
            root = parent[root]
        while parent[a] != root:
            parent[a], a = root, parent[a]
        return root

    def union(a: int, b: int) -> None:
        ra, rb = find(a), find(b)
        if ra == rb:
            return
        if rank[ra] < rank[rb]:
            ra, rb = rb, ra
        parent[rb] = ra
        if rank[ra] == rank[rb]:
            rank[ra] += 1

    mask = bytearray(w * h)
    for y in range(h):
        base = y * w
        for x in range(w):
            if rgb(x, y) != bg:
                mask[base + x] = 1
                if x and mask[base + x - 1]:
                    union(base + x, base + x - 1)
                if y and mask[base - w + x]:
                    union(base + x, base - w + x)

    bboxes: dict[int, list[int]] = {}
    area: dict[int, int] = {}
    for y in range(h):
        base = y * w
        for x in range(w):
            if not mask[base + x]:
                continue
            r = find(base + x)
            a = area.get(r, 0) + 1
            area[r] = a
            b = bboxes.get(r)
            if b is None:
                bboxes[r] = [x, y, x, y]
            else:
                if x < b[0]:
                    b[0] = x
                if y < b[1]:
                    b[1] = y
                if x > b[2]:
                    b[2] = x
                if y > b[3]:
                    b[3] = y

    rows = []
    for r, b in bboxes.items():
        if area[r] < min_area:
            continue
        x0, y0, x1, y1 = b
        rows.append((y0, x0, b, x1 - x0 + 1, y1 - y0 + 1, area[r]))

    print(f"{len(rows)} boxes with area >= {min_area} px   (screen width {w})")
    print(
        f"{'box(x0,y0,x1,y1)':<28} {'w':>5} {'h':>5} {'gapL':>5} {'gapR':>5} "
        f"{'imb':>6} {'fill%':>6}"
    )
    for y0, x0, b, bw, bh, ar in sorted(rows):
        gl, gr = x0, w - 1 - b[2]
        imb = gl - gr
        flag = "  <== OFF-CENTRE" if abs(imb) > 4 else ""
        print(
            f"{str(tuple(b)):<28} {bw:>5} {bh:>5} {gl:>5} {gr:>5} "
            f"{imb:>+6} {100.0 * ar / (bw * bh):>5.0f}%{flag}"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
