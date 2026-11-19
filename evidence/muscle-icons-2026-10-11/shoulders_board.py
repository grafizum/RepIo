#!/usr/bin/env python3
"""Round three of the variant pass: two refined SHOULDERS candidates.

s4 read as a jacket (sleeve-like arms, straight hem); s5 rendered as antlers (tiny
body, arms attached too high). This board renders:
  s6 - front torso with the arms CUT OFF as stumps (cut high, delt creases inside,
       torso hem visible below the cuts so limbs read as limbs)
  s5b - the reference's arms-spread pose, but on a properly wide torso with the
       arms rooted at real shoulder points

Output: evidence/muscle-icons-2026-10-11/shoulders.png (2x)

Usage: python3 evidence/muscle-icons-2026-10-11/shoulders_board.py
"""
from __future__ import annotations

import subprocess
from pathlib import Path

OUT = Path(__file__).resolve().parent

CANDIDATES = [
    (
        "s6 arm stumps",
        "#DB2777",
        "M10.4,4.6 L13.6,4.6 L13.6,7.0 "
        "C15.4,7.4 17.0,8.4 18.0,9.8 "
        "C19.6,11.0 20.4,12.6 20.4,14.4 "
        "L20.2,16.6 "
        "C18.6,17.0 17.2,17.0 16.0,16.6 "
        "L15.4,13.6 "
        "C15.2,15.4 15.0,17.4 15.2,19.4 "
        "C14.6,20.8 13.4,21.4 12,21.4 "
        "C10.6,21.4 9.4,20.8 8.8,19.4 "
        "C9.0,17.4 8.8,15.4 8.6,13.6 "
        "L8.0,16.6 "
        "C6.8,17.0 5.4,17.0 3.8,16.6 "
        "L3.6,14.4 "
        "C3.6,12.6 4.4,11.0 6.0,9.8 "
        "C7.0,8.4 8.6,7.4 10.4,7.0 Z "
        "M16.8,10.6 C17.8,11.8 18.4,13.2 18.6,14.6 "
        "M7.2,10.6 C6.2,11.8 5.6,13.2 5.4,14.6",
    ),
    (
        "s5b spread on torso",
        "#DB2777",
        "M10.6,4.6 L13.4,4.6 L13.4,7.2 "
        "C15.0,7.6 16.0,8.6 16.4,10.0 "
        "L20.4,5.6 "
        "C21.2,4.9 21.2,3.9 20.4,3.6 C19.6,3.3 19.0,3.9 18.8,4.7 "
        "L16.2,13.0 "
        "C16.0,14.8 15.8,16.6 16.0,18.4 "
        "C15.6,20.0 14.2,21.0 12,21.2 "
        "C9.8,21.0 8.4,20.0 8.0,18.4 "
        "C8.2,16.6 8.0,14.8 7.8,13.0 "
        "L5.2,4.7 "
        "C5.0,3.9 4.4,3.3 3.6,3.6 C2.8,3.9 2.8,4.9 3.6,5.6 "
        "L7.6,10.0 "
        "C8.0,8.6 9.0,7.6 10.6,7.2 Z "
        "M17.8,8.4 C18.6,7.7 19.2,7.0 19.6,6.2 "
        "M6.2,8.4 C5.4,7.7 4.8,7.0 4.4,6.2",
    ),
]


def icon_svg(paths: str, color: str, px: int) -> str:
    subs, cur = [], []
    for tok in paths.split():
        if tok.startswith("M") and cur:
            subs.append(cur)
            cur = []
        cur.append(tok)
    if cur:
        subs.append(cur)
    body = "".join(
        f'<path d="{" ".join(s)}" fill="none" stroke="{color}" stroke-width="1.8" '
        f'stroke-linecap="round" stroke-linejoin="round" />'
        for s in subs
    )
    return f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">{body}</svg>'


def main() -> None:
    cell, gap, pad, label_h = 240, 36, 34, 38
    w = pad * 2 + 2 * cell + gap
    h = pad * 2 + 40 + cell + label_h
    parts = ['<rect width="100%" height="100%" fill="#F7F4EF"/>']
    parts.append(
        f'<text x="{w/2}" y="{pad + 18}" font-family="sans-serif" font-size="17" '
        f'font-weight="700" fill="#333" text-anchor="middle">SHOULDERS - round three</text>'
    )
    y = pad + 40
    for ci, (label, tint, paths) in enumerate(CANDIDATES):
        x = pad + ci * (cell + gap)
        parts.append(f'<rect x="{x}" y="{y}" width="{cell}" height="{cell + label_h}" rx="12" fill="#FFFFFF" stroke="#E3DDD3"/>')
        parts.append(icon_svg(paths, tint, cell - 40).replace('<svg ', f'<svg x="{x + 20}" y="{y + 16}" ', 1))
        parts.append(
            f'<text x="{x + cell/2}" y="{y + cell + 24}" font-family="sans-serif" '
            f'font-size="15" fill="#555" text-anchor="middle">{label}</text>'
        )
    svg = f'<svg width="{w}" height="{h}" viewBox="0 0 {w} {h}" xmlns="http://www.w3.org/2000/svg">{"".join(parts)}</svg>'
    src = OUT / "shoulders.svg"
    src.write_text(svg)
    png = OUT / "shoulders.png"
    subprocess.run(["rsvg-convert", "-w", str(w * 2), "-h", str(h * 2), "-o", str(png), str(src)], check=True)
    print(f"wrote {png} ({w}x{h} @2x)")


if __name__ == "__main__":
    main()
