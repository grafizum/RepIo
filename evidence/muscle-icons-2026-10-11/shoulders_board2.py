#!/usr/bin/env python3
"""Round four of the variant pass: three OPEN-style SHOULDERS candidates.

Every closed-contour shoulders attempt read as luggage, clothing or bottles; the
winning chest (c2) and back (b3) both work as OPEN strokes, so round four follows
that rule:
  s8  - open shoulder arch (arm cut -> delt -> neck -> delt -> arm cut) with a
        delt crease per side and two short upper-arm ticks below the cuts
  s10 - front torso with CLEAN arm stumps: straight cuts, armpit clearly inboard
        of the cut so arm and torso stop merging
  s12 - side profile: vertical upper arm cut at the bottom, domed delt cap on
        top hooking back toward the neck, one crease under the dome

Output: evidence/muscle-icons-2026-10-11/shoulders2.png (2x)

Usage: python3 evidence/muscle-icons-2026-10-11/shoulders_board2.py
"""
from __future__ import annotations

import subprocess
from pathlib import Path

OUT = Path(__file__).resolve().parent

CANDIDATES = [
    (
        "s8 open arch",
        "#DB2777",
        "M4.6,16.8 C4.4,13.0 5.8,9.8 8.8,8.2 C10.4,7.3 11.4,6.3 11.7,4.6 "
        "L12.3,4.6 "
        "C12.6,6.3 13.6,7.3 15.2,8.2 C18.2,9.8 19.6,13.0 19.4,16.8 "
        "M7.4,11.2 C6.5,12.8 6.2,14.6 6.4,16.6 "
        "M16.6,11.2 C17.5,12.8 17.8,14.6 17.6,16.6 "
        "M6.2,18.0 L6.0,21.0 "
        "M17.8,18.0 L18.0,21.0",
    ),
    (
        "s10 clean stumps",
        "#DB2777",
        "M10.4,4.4 L13.6,4.4 L13.6,7.2 "
        "C15.4,7.6 17.0,8.6 18.2,10.0 "
        "C19.6,11.4 20.4,13.0 20.4,14.8 "
        "L20.4,17.4 "
        "L17.0,17.4 "
        "L17.0,13.6 "
        "C16.8,15.6 16.4,17.8 16.4,20.0 "
        "C15.2,21.0 13.6,21.4 12,21.4 "
        "C10.4,21.4 8.8,21.0 7.6,20.0 "
        "C7.6,17.8 7.2,15.6 7.0,13.6 "
        "L7.0,17.4 "
        "L3.6,17.4 "
        "L3.6,14.8 "
        "C3.6,13.0 4.4,11.4 5.8,10.0 "
        "C7.0,8.6 8.6,7.6 10.4,7.2 Z "
        "M18.6,11.6 C19.2,12.8 19.4,14.0 19.4,15.2 "
        "M5.4,11.6 C4.8,12.8 4.6,14.0 4.6,15.2",
    ),
    (
        "s12 side profile",
        "#DB2777",
        "M6.6,20.6 L6.8,13.0 "
        "C6.9,10.0 9.2,7.4 12.4,6.8 "
        "C14.8,6.4 16.6,7.4 17.4,9.2 "
        "C17.6,9.8 17.7,10.4 17.7,11.0 "
        "L17.0,13.4 "
        "C17.4,15.6 17.4,18.0 17.2,20.6 "
        "L13.0,20.6 "
        "L13.0,16.0 "
        "C12.6,17.2 11.8,18.2 10.6,18.6 "
        "C8.6,19.2 7.2,20.0 6.6,20.6 Z "
        "M8.4,11.6 C10.4,13.0 12.8,13.8 15.2,13.6 "
        "M14.6,6.9 C15.4,5.4 16.8,4.4 18.6,4.2",
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
    cell, gap, pad, label_h = 230, 34, 34, 38
    w = pad * 2 + 3 * cell + 2 * gap
    h = pad * 2 + 40 + cell + label_h
    parts = ['<rect width="100%" height="100%" fill="#F7F4EF"/>']
    parts.append(
        f'<text x="{w/2}" y="{pad + 18}" font-family="sans-serif" font-size="17" '
        f'font-weight="700" fill="#333" text-anchor="middle">SHOULDERS - round four (open style)</text>'
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
    src = OUT / "shoulders2.svg"
    src.write_text(svg)
    png = OUT / "shoulders2.png"
    subprocess.run(["rsvg-convert", "-w", str(w * 2), "-h", str(h * 2), "-o", str(png), str(src)], check=True)
    print(f"wrote {png} ({w}x{h} @2x)")


if __name__ == "__main__":
    main()
