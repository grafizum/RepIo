#!/usr/bin/env python3
"""Round five (final): the two surviving SHOULDERS candidates, s10 vs s15.

s10 - front torso with clean arm stumps (straight cuts, armpit inboard of the cut
      so arm and torso stay separate) plus a delt crease per side.
s15 - the WINNING back outline (b3: neck, slopes, arm cuts, V-taper, hips) re-used
      as the body, with shoulder innards instead: a clavicle chevron under the neck
      and one delt dome arc hugging each shoulder.

Output: evidence/muscle-icons-2026-10-11/shoulders3.png (2x)

Usage: python3 evidence/muscle-icons-2026-10-11/shoulders_board3.py
"""
from __future__ import annotations

import subprocess
from pathlib import Path

OUT = Path(__file__).resolve().parent

CANDIDATES = [
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
        "s15 back body + delt domes",
        "#DB2777",
        "M9.8,3.6 L14.2,3.6 L14.2,6.2 "
        "C16.4,6.6 18.4,7.6 19.8,9.2 L20.6,12.6 "
        "C20.8,14.0 20.6,15.2 20.2,16.2 "
        "C18.6,16.8 16.6,17.0 15.0,16.6 "
        "C14.6,18.2 14.4,19.6 14.4,20.8 "
        "C13.2,21.2 12.4,21.4 12,21.4 "
        "C11.6,21.4 10.8,21.2 9.6,20.8 "
        "C9.6,19.6 9.4,18.2 9.0,16.6 "
        "C7.4,17.0 5.4,16.8 3.8,16.2 "
        "C3.4,15.2 3.2,14.0 3.4,12.6 "
        "L4.2,9.2 C5.6,7.6 7.6,6.6 9.8,6.2 Z "
        "M9.4,8.8 L12,10.6 L14.6,8.8 "
        "M16.6,8.2 C18.4,9.4 19.4,11.2 19.6,13.2 "
        "M7.4,8.2 C5.6,9.4 4.6,11.2 4.4,13.2",
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
    cell, gap, pad, label_h = 240, 40, 36, 40
    w = pad * 2 + 2 * cell + gap
    h = pad * 2 + 44 + cell + label_h
    parts = ['<rect width="100%" height="100%" fill="#F7F4EF"/>']
    parts.append(
        f'<text x="{w/2}" y="{pad + 20}" font-family="sans-serif" font-size="18" '
        f'font-weight="700" fill="#333" text-anchor="middle">SHOULDERS - final round</text>'
    )
    y = pad + 44
    for ci, (label, tint, paths) in enumerate(CANDIDATES):
        x = pad + ci * (cell + gap)
        parts.append(f'<rect x="{x}" y="{y}" width="{cell}" height="{cell + label_h}" rx="12" fill="#FFFFFF" stroke="#E3DDD3"/>')
        parts.append(icon_svg(paths, tint, cell - 44).replace('<svg ', f'<svg x="{x + 22}" y="{y + 18}" ', 1))
        parts.append(
            f'<text x="{x + cell/2}" y="{y + cell + 26}" font-family="sans-serif" '
            f'font-size="15" fill="#555" text-anchor="middle">{label}</text>'
        )
    svg = f'<svg width="{w}" height="{h}" viewBox="0 0 {w} {h}" xmlns="http://www.w3.org/2000/svg">{"".join(parts)}</svg>'
    src = OUT / "shoulders3.svg"
    src.write_text(svg)
    png = OUT / "shoulders3.png"
    subprocess.run(["rsvg-convert", "-w", str(w * 2), "-h", str(h * 2), "-o", str(png), str(src)], check=True)
    print(f"wrote {png} ({w}x{h} @2x)")


if __name__ == "__main__":
    main()
