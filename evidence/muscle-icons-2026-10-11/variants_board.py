#!/usr/bin/env python3
"""Sketchboard: competing concepts for the three muscle glyphs that would not read.

Four rounds of drawing produced chest-back-shoulders glyphs that looked like faces,
bags, croissants and tridents. Rather than guess one concept per round, this board
renders SEVERAL candidates per part side by side at judging size, so the choice is
made by eye against the reference arm's style (bold outline, stroke 1.8, round caps,
24x24, no fills) instead of one-at-a-time.

All candidates are hand-written SVG paths in the same 24-unit box the app uses.
Output: evidence/muscle-icons-2026-10-11/variants.png (16:9 board).

Usage: python3 evidence/muscle-icons-2026-10-11/variants_board.py
"""
from __future__ import annotations

import subprocess
from pathlib import Path

OUT = Path(__file__).resolve().parent

# (part, tint, [(label, path-data, ) ...])
BOARD: list[tuple[str, str, list[tuple[str, str]]]] = [
    (
        "CHEST",
        "#DC2626",
        [
            (
                "c1 plates",
                "M4.4,7.4 C7.0,6.4 9.6,6.2 11.6,6.6 L11.6,14.6 C9.8,15.4 7.2,15.0 5.6,13.4 C4.4,12.0 4.0,9.6 4.4,7.4 Z "
                "M19.6,7.4 C17.0,6.4 14.4,6.2 12.4,6.6 L12.4,14.6 C14.2,15.4 16.8,15.0 18.4,13.4 C19.6,12.0 20.0,9.6 19.6,7.4 Z "
                "M6.2,9.0 C7.6,8.4 9.4,8.2 10.8,8.4 M17.8,9.0 C16.4,8.4 14.6,8.2 13.2,8.4",
            ),
            (
                "c2 bar+pecs",
                "M3.8,8.6 C7.0,6.6 9.8,6.0 12,6.6 C14.2,6.0 17.0,6.6 20.2,8.6 "
                "M12,6.6 L12,13.6 "
                "M4.6,10.4 C5.6,13.6 8.2,15.4 11.0,15.4 "
                "M19.4,10.4 C18.4,13.6 15.8,15.4 13.0,15.4",
            ),
            (
                "c3 collar W",
                "M4.2,7.2 L12,11.0 L19.8,7.2 "
                "M4.2,7.2 C5.4,11.4 6.2,13.4 8.0,14.8 C9.6,16.0 11.0,15.6 12,14.0 C13.0,15.6 14.4,16.0 16.0,14.8 C17.8,13.4 18.6,11.4 19.8,7.2 "
                "M12,11.0 L12,14.0",
            ),
            (
                "c4 torso+pecs",
                "M4.6,8.4 C5.8,5.8 8.2,4.2 10.6,3.8 L12,5.8 L13.4,3.8 C15.8,4.2 18.2,5.8 19.4,8.4 "
                "M5.4,10.8 C6.8,13.8 9.2,15.4 11.7,15.4 M18.6,10.8 C17.2,13.8 14.8,15.4 12.3,15.4 "
                "M12,8.4 L12,15.4 "
                "M4.6,8.4 L4.8,14.4 C6.0,17.6 8.6,19.6 12,19.6 C15.4,19.6 18.0,17.6 19.2,14.4 L19.4,8.4",
            ),
        ],
    ),
    (
        "BACK",
        "#2563EB",
        [
            (
                "b1 wings",
                "M10.4,3.4 L13.6,3.4 L13.6,5.6 C15.8,6.0 17.8,6.8 19.4,8.2 C20.4,9.2 20.8,10.4 20.6,11.6 "
                "C20.0,13.6 18.6,15.4 17.4,17.0 C16.8,18.0 16.4,19.0 16.2,20.0 C15.4,20.8 13.8,21.2 12,20.6 "
                "C10.2,21.2 8.6,20.8 7.8,20.0 C7.6,19.0 7.2,18.0 6.6,17.0 C5.4,15.4 4.0,13.6 3.4,11.6 "
                "C3.2,10.4 3.6,9.2 4.6,8.2 C6.2,6.8 8.2,6.0 10.4,5.6 Z "
                "M12,7.6 L12,17.6 M6.6,10.8 C7.7,12.8 8.6,14.4 9.3,15.8 M17.4,10.8 C16.3,12.8 15.4,14.4 14.7,15.8",
            ),
            (
                "b2 V+spine",
                "M4.4,7.8 C5.8,5.4 8.2,4.0 10.6,3.6 L12,5.6 L13.4,3.6 C15.8,4.0 18.2,5.4 19.6,7.8 "
                "L17.2,12.0 C16.4,15.0 15.8,17.6 15.4,20.4 C13.9,21.0 12.6,21.2 12,21.2 "
                "C11.4,21.2 10.1,21.0 8.6,20.4 C8.2,17.6 7.6,15.0 6.8,12.0 Z "
                "M12,7.6 L12,18.4",
            ),
            (
                "b3 arms cut",
                "M9.8,3.6 L14.2,3.6 L14.2,6.2 C16.4,6.6 18.4,7.6 19.8,9.2 L20.6,12.6 "
                "C20.8,14.0 20.6,15.2 20.2,16.2 C18.6,16.8 16.6,17.0 15.0,16.6 "
                "C14.6,18.2 14.4,19.6 14.4,20.8 C13.2,21.2 12.4,21.4 12,21.4 C11.6,21.4 10.8,21.2 9.6,20.8 "
                "C9.6,19.6 9.4,18.2 9.0,16.6 C7.4,17.0 5.4,16.8 3.8,16.2 "
                "C3.4,15.2 3.2,14.0 3.4,12.6 L4.2,9.2 C5.6,7.6 7.6,6.6 9.8,6.2 Z "
                "M12,8.4 L12,18.6 "
                "M7.4,10.4 C8.4,12.6 9.2,14.6 9.7,16.4 M16.6,10.4 C15.6,12.6 14.8,14.6 14.3,16.4",
            ),
            (
                "b4 cobra",
                "M10.0,3.4 L14.0,3.4 L14.0,6.0 "
                "C17.0,6.4 19.8,7.6 21.0,9.8 C21.6,11.0 21.4,12.2 20.6,13.0 "
                "C18.6,14.6 16.4,16.8 15.4,19.6 C14.4,21.0 13.0,21.4 12,21.4 "
                "C11.0,21.4 9.6,21.0 8.6,19.6 C7.6,16.8 5.4,14.6 3.4,13.0 "
                "C2.6,12.2 2.4,11.0 3.0,9.8 C4.2,7.6 7.0,6.4 10.0,6.0 Z "
                "M12,8.6 L12,18.2 M7.2,11.4 C8.6,13.4 9.6,15.0 10.2,16.6 M16.8,11.4 C15.4,13.4 14.4,15.0 13.8,16.6",
            ),
        ],
    ),
    (
        "SHOULDERS",
        "#DB2777",
        [
            (
                "s1 yoke",
                "M10.2,3.6 L13.8,3.6 L13.8,6.4 C15.6,6.6 17.4,7.2 18.8,8.4 C20.2,9.6 20.8,11.2 20.6,13.0 "
                "C20.4,14.4 19.8,15.4 19.0,16.0 C17.2,16.4 14.8,16.2 13.4,15.6 L12,14.8 L10.6,15.6 "
                "C9.2,16.2 6.8,16.4 5.0,16.0 C4.2,15.4 3.6,14.4 3.4,13.0 C3.2,11.2 3.8,9.6 5.2,8.4 "
                "C6.6,7.2 8.4,6.6 10.2,6.4 Z "
                "M7.4,9.6 C6.4,11.2 6.2,13.2 6.6,15.2 M16.6,9.6 C17.6,11.2 17.8,13.2 17.4,15.2",
            ),
            (
                "s2 two caps",
                "M12,4.4 L12,8.6 "
                "M5.6,9.8 C7.6,8.6 9.8,8.2 12,8.4 C14.2,8.2 16.4,8.6 18.4,9.8 "
                "M5.6,9.8 C3.8,10.4 3.0,12.2 3.4,14.2 C3.8,16.2 5.4,17.4 7.4,17.2 C9.2,17.0 10.2,15.6 10.2,13.8 "
                "C10.2,12.0 9.2,10.4 5.6,9.8 Z "
                "M18.4,9.8 C20.2,10.4 21.0,12.2 20.6,14.2 C20.2,16.2 18.6,17.4 16.6,17.2 C14.8,17.0 13.8,15.6 13.8,13.8 "
                "C13.8,12.0 14.8,10.4 18.4,9.8 Z "
                "M6.8,17.0 L6.4,19.8 M9.0,17.4 L8.8,20.2 M17.2,17.0 L17.6,19.8 M15.0,17.4 L15.2,20.2",
            ),
            (
                "s3 profile",
                "M13.6,4.0 C10.8,4.4 8.8,5.8 7.4,7.8 C6.0,9.8 5.4,12.4 5.4,15.4 L5.4,19.6 "
                "C7.4,20.6 9.8,21.0 12.4,20.6 L13.4,16.4 "
                "C14.6,15.8 15.4,14.6 15.6,13.0 C15.8,11.0 15.0,9.2 13.4,8.2 "
                "C12.0,7.2 11.4,6.0 11.8,4.2 Z "
                "M7.6,9.6 C9.4,11.2 11.6,12.4 14.0,13.0 "
                "M6.2,14.4 C8.4,15.6 10.6,16.4 13.0,16.6",
            ),
            (
                "s4 raised arms",
                "M10.0,4.4 L14.0,4.4 L14.0,7.4 "
                "C15.8,7.6 17.6,8.4 19.0,9.8 C20.4,11.2 21.0,13.0 20.8,15.2 "
                "L21.0,19.6 L17.6,19.8 L17.2,15.8 "
                "C16.4,17.0 15.4,18.2 14.4,19.4 C13.8,20.2 13.0,20.8 12,21.0 "
                "C11.0,20.8 10.2,20.2 9.6,19.4 C8.6,18.2 7.6,17.0 6.8,15.8 "
                "L6.4,19.8 L3.0,19.6 L3.2,15.2 "
                "C3.0,13.0 3.6,11.2 5.0,9.8 C6.4,8.4 8.2,7.6 10.0,7.4 Z "
                "M7.8,10.6 C7.0,12.4 6.8,14.2 7.2,16.0 M16.2,10.6 C17.0,12.4 17.2,14.2 16.8,16.0",
            ),
        ],
    ),
]


def icon_svg(paths: str, color: str, px: int) -> str:
    d = paths.split()
    subs, cur = [], []
    for tok in d:
        if tok.startswith("M") and cur:
            subs.append(cur)
            cur = []
        cur.append(tok)
    if cur:
        subs.append(cur)
    body = "".join(f'<path d="{" ".join(s)}" fill="none" stroke="{color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />' for s in subs)
    return f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">{body}</svg>'


def main() -> None:
    cell, gap, pad, label_h = 180, 26, 30, 34
    rows, cols = 4, 3
    w = pad * 2 + cols * cell + (cols - 1) * gap
    h = pad * 2 + rows * (cell + label_h) + (rows - 1) * gap + 40
    parts = ['<rect width="100%" height="100%" fill="#F7F4EF"/>']
    # column headers
    for ci, (part, tint, cands) in enumerate(BOARD):
        x = pad + ci * (cell + gap)
        parts.append(f'<text x="{x + cell/2}" y="{pad + 18}" font-family="sans-serif" font-size="17" font-weight="700" fill="#333" text-anchor="middle">{part}</text>')
        for ri, (label, paths) in enumerate(cands):
            y = pad + 40 + ri * (cell + label_h + gap)
            parts.append(f'<rect x="{x}" y="{y}" width="{cell}" height="{cell + label_h}" rx="12" fill="#FFFFFF" stroke="#E3DDD3"/>')
            parts.append(icon_svg(paths, tint, cell - 36).replace(f'width="{cell-36}"', f'width="{cell-36}"', 1).replace(
                f'<svg ', f'<svg x="{x + 18}" y="{y + 14}" ', 1))
            parts.append(f'<text x="{x + cell/2}" y="{y + cell + 22}" font-family="sans-serif" font-size="14" fill="#555" text-anchor="middle">{label}</text>')
    svg = f'<svg width="{w}" height="{h}" viewBox="0 0 {w} {h}" xmlns="http://www.w3.org/2000/svg">{"".join(parts)}</svg>'
    src = OUT / "variants.svg"
    src.write_text(svg)
    png = OUT / "variants.png"
    subprocess.run(["rsvg-convert", "-w", str(w * 2), "-h", str(h * 2), "-o", str(png), str(src)], check=True)
    print(f"wrote {png} ({w}x{h} @2x)")


if __name__ == "__main__":
    main()
