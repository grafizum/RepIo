#!/usr/bin/env python3
"""The picks board: the winning chest and back, plus two shoulders candidates.

Round two of the variant pass (variants_board.py). c2 and b3 won their columns by
eye; none of the shoulders candidates did, so this board renders the two strongest
shoulders ideas - s4 (arms at the sides, refined hem) and s5 (arms spread up-and-out,
the pose the OWNER's own reference screenshot uses for person-arms-spread) - beside
the picked chest and back, all at judging size.

Output: evidence/muscle-icons-2026-10-11/picks.png (2x).

Usage: python3 evidence/muscle-icons-2026-10-11/picks_board.py
"""
from __future__ import annotations

import subprocess
from pathlib import Path

OUT = Path(__file__).resolve().parent

BOARD: list[tuple[str, str, list[tuple[str, str]]]] = [
    (
        "CHEST",
        "#DC2626",
        [
            (
                "c2 bar+pecs",
                "M3.8,8.6 C7.0,6.6 9.8,6.0 12,6.6 C14.2,6.0 17.0,6.6 20.2,8.6 "
                "M12,6.6 L12,13.6 "
                "M4.6,10.4 C5.6,13.6 8.2,15.4 11.0,15.4 "
                "M19.4,10.4 C18.4,13.6 15.8,15.4 13.0,15.4",
            ),
        ],
    ),
    (
        "BACK",
        "#2563EB",
        [
            (
                "b3 arms cut",
                "M9.8,3.6 L14.2,3.6 L14.2,6.2 C16.4,6.6 18.4,7.6 19.8,9.2 L20.6,12.6 "
                "C20.8,14.0 20.6,15.2 20.2,16.2 C18.6,16.8 16.6,17.0 15.0,16.6 "
                "C14.6,18.2 14.4,19.6 14.4,20.8 C13.2,21.2 12.4,21.4 12,21.4 "
                "C11.6,21.4 10.8,21.2 9.6,20.8 "
                "C9.6,19.6 9.4,18.2 9.0,16.6 C7.4,17.0 5.4,16.8 3.8,16.2 "
                "C3.4,15.2 3.2,14.0 3.4,12.6 L4.2,9.2 C5.6,7.6 7.6,6.6 9.8,6.2 Z "
                "M12,8.4 L12,18.6 "
                "M7.4,10.4 C8.4,12.6 9.2,14.6 9.7,16.4 M16.6,10.4 C15.6,12.6 14.8,14.6 14.3,16.4",
            ),
        ],
    ),
    (
        "SHOULDERS",
        "#DB2777",
        [
            (
                "s4 arms down",
                "M10.0,4.4 L14.0,4.4 L14.0,7.4 "
                "C15.8,7.6 17.6,8.4 19.0,9.8 C20.4,11.2 21.0,13.0 20.8,15.2 "
                "L21.0,19.6 L17.6,19.8 L17.2,15.8 "
                "C16.4,17.0 15.4,18.2 14.4,19.4 C13.8,20.2 13.0,20.8 12,21.0 "
                "C11.0,20.8 10.2,20.2 9.6,19.4 C8.6,18.2 7.6,17.0 6.8,15.8 "
                "L6.4,19.8 L3.0,19.6 L3.2,15.2 "
                "C3.0,13.0 3.6,11.2 5.0,9.8 C6.4,8.4 8.2,7.6 10.0,7.4 Z "
                "M7.8,10.6 C7.0,12.4 6.8,14.2 7.2,16.0 M16.2,10.6 C17.0,12.4 17.2,14.2 16.8,16.0",
            ),
            (
                "s5 arms spread",
                "M10.8,6.4 L13.2,6.4 L13.2,8.8 "
                "C14.6,9.1 15.8,9.7 16.6,10.6 "
                "L20.6,5.4 C21.5,4.6 21.5,3.6 20.7,3.3 C19.9,3.0 19.2,3.6 18.9,4.4 "
                "L17.0,11.6 L15.7,12.9 "
                "C15.5,14.6 15.3,16.4 15.5,18.2 "
                "C15.2,19.8 14.0,20.9 12,21.2 "
                "C10.0,20.9 8.8,19.8 8.5,18.2 "
                "C8.7,16.4 8.5,14.6 8.3,12.9 "
                "L7.0,11.6 L5.1,4.4 "
                "C4.8,3.6 4.1,3.0 3.3,3.3 C2.5,3.6 2.5,4.6 3.4,5.4 "
                "L7.4,10.6 "
                "C8.2,9.7 9.4,9.1 10.8,8.8 Z "
                "M17.6,9.4 C18.4,8.7 19.0,8.1 19.6,7.4 "
                "M6.4,9.4 C5.6,8.7 5.0,8.1 4.4,7.4",
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
    body = "".join(
        f'<path d="{" ".join(s)}" fill="none" stroke="{color}" stroke-width="1.8" '
        f'stroke-linecap="round" stroke-linejoin="round" />'
        for s in subs
    )
    return f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">{body}</svg>'


def main() -> None:
    cell, gap, pad, label_h = 200, 30, 30, 34
    rows, cols = 2, 3
    w = pad * 2 + cols * cell + (cols - 1) * gap
    h = pad * 2 + 40 + rows * (cell + label_h) + (rows - 1) * gap
    parts = ['<rect width="100%" height="100%" fill="#F7F4EF"/>']
    for ci, (part, tint, cands) in enumerate(BOARD):
        x = pad + ci * (cell + gap)
        parts.append(
            f'<text x="{x + cell/2}" y="{pad + 18}" font-family="sans-serif" '
            f'font-size="17" font-weight="700" fill="#333" text-anchor="middle">{part}</text>'
        )
        for ri, (label, paths) in enumerate(cands):
            y = pad + 40 + ri * (cell + label_h + gap)
            parts.append(f'<rect x="{x}" y="{y}" width="{cell}" height="{cell + label_h}" rx="12" fill="#FFFFFF" stroke="#E3DDD3"/>')
            parts.append(icon_svg(paths, tint, cell - 36).replace('<svg ', f'<svg x="{x + 18}" y="{y + 14}" ', 1))
            parts.append(
                f'<text x="{x + cell/2}" y="{y + cell + 22}" font-family="sans-serif" '
                f'font-size="14" fill="#555" text-anchor="middle">{label}</text>'
            )
    svg = f'<svg width="{w}" height="{h}" viewBox="0 0 {w} {h}" xmlns="http://www.w3.org/2000/svg">{"".join(parts)}</svg>'
    src = OUT / "picks.svg"
    src.write_text(svg)
    png = OUT / "picks.png"
    subprocess.run(["rsvg-convert", "-w", str(w * 2), "-h", str(h * 2), "-o", str(png), str(src)], check=True)
    print(f"wrote {png} ({w}x{h} @2x)")


if __name__ == "__main__":
    main()
