#!/usr/bin/env python3
"""Final comparison: the six body-part glyphs at the size the filter row actually draws.

Two of the six are still genuinely open - the chest and the shoulders both want the
top of a torso, so the pair has to be judged TOGETHER at 22px and not one card at a
time. The families below are the four ways to pair the chest and shoulder drawings.

Everything else (arm, legs, back, core) is settled by the earlier rounds: the stroke
lane, the closed-outline style, no fills.

Usage: python3 evidence/muscle-icons-2026-10-11/final.py
"""
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

A = "{http://schemas.android.com/apk/res/android}"
ROOT = Path(__file__).resolve().parents[2]
DRAWABLES = ROOT / "composeApp/src/main/res/drawable"

EQUIPMENT = [
    ("Barbell", "ic_eq_barbell"),
    ("Bench", "ic_eq_bench"),
    ("Cable", "ic_eq_cable"),
    ("Leg ext.", "ic_eq_chair"),
    ("Dip bars", "ic_eq_dipbars"),
    ("Kettlebell", "ic_eq_kettlebell"),
    ("Machine", "ic_eq_machine"),
    ("Pull-up", "ic_eq_pullup"),
    ("Rack", "ic_eq_rack"),
    ("Rings", "ic_eq_rings"),
]

ARMS = [
    (
        "M20.8,11.9 "
        "C17.6,10.3 13.2,10.2 10.2,11.5 "
        "C9.2,12.0 8.9,12.5 8.9,13.4 "
        "L8.9,6.8 "
        "C8.9,5.6 8.6,4.9 7.6,4.9 "
        "C6.4,4.9 4.4,5.1 4.1,6.4 "
        "L3.9,13.7 "
        "C3.9,15.6 5.0,16.8 6.8,17.2 "
        "C11.2,18.0 16.4,18.0 20.8,17.2 Z",
        1.8,
    ),
]

LEGS = [
    (
        "M6.0,3.6 L18.0,3.6 "
        "C18.6,7.0 18.4,10.4 17.8,13.6 "
        "C17.3,16.2 17.3,18.2 17.4,19.6 "
        "L20.4,19.6 L20.4,21.8 L13.9,21.8 "
        "L14.2,11.4 "
        "C13.7,10.0 12.9,9.4 12.0,9.4 "
        "C11.1,9.4 10.3,10.0 9.8,11.4 "
        "L10.1,21.8 L3.6,21.8 L3.6,19.6 L6.6,19.6 "
        "C6.7,18.2 6.7,16.2 6.2,13.6 "
        "C5.6,10.4 5.4,7.0 6.0,3.6 Z",
        1.8,
    ),
]

BACK = [
    ("M5.0,6.6 C7.6,5.8 10.2,5.6 12,5.6 C13.8,5.6 16.4,5.8 19.0,6.6", 1.8),
    ("M5.0,6.6 C5.2,9.8 6.4,13.0 8.3,15.2 C9.3,16.4 10.6,17.2 12,17.6", 1.8),
    ("M19.0,6.6 C18.8,9.8 17.6,13.0 15.7,15.2 C14.7,16.4 13.4,17.2 12,17.6", 1.8),
    ("M12,6.2 V17.4", 1.8),
    ("M8.3,8.2 C7.9,10.1 8.5,12.0 9.8,13.1", 1.8),
    ("M15.7,8.2 C16.1,10.1 15.5,12.0 14.2,13.1", 1.8),
]

CORE = [
    (
        "M8.0,6.4 H16.0 C16.5,6.4 16.8,6.7 16.8,7.2 V16.8 "
        "C16.8,17.3 16.5,17.6 16.0,17.6 H8.0 C7.5,17.6 7.2,17.3 7.2,16.8 "
        "V7.2 C7.2,6.7 7.5,6.4 8.0,6.4 Z",
        1.8,
    ),
    ("M12,6.4 V17.6", 1.8),
    ("M7.2,10.1 H16.8", 1.8),
    ("M7.2,13.9 H16.8", 1.8),
]

# Chest A: the plate alone, pecs drawn, no head.
CHEST_A = [
    ("M8.4,5.9 C9.5,5.6 10.7,5.5 12,5.5 C13.3,5.5 14.5,5.6 15.6,5.9", 1.8),
    ("M8.4,5.9 C6.4,6.4 4.9,7.6 4.2,9.6 C3.7,11.2 4.0,12.8 4.7,14.1", 1.8),
    ("M15.6,5.9 C17.6,6.4 19.1,7.6 19.8,9.6 C20.3,11.2 20.0,12.8 19.3,14.1", 1.8),
    ("M4.7,14.1 C9.0,15.3 15.0,15.3 19.3,14.1", 1.8),
    ("M11.5,8.2 C9.3,7.2 6.9,7.7 6.1,9.5 C5.6,11.0 6.1,12.6 7.3,13.4", 1.8),
    ("M12.5,8.2 C14.7,7.2 17.1,7.7 17.9,9.5 C18.4,11.0 17.9,12.6 16.7,13.4", 1.8),
]

# Chest B: the same with the head left on.
CHEST_B = [
    ("M12,3.0 m-1.9,0 a1.9,1.9 0 1 0 3.8,0 a1.9,1.9 0 1 0 -3.8,0", 1.8),
    ("M12,4.9 V5.5", 1.8),
] + CHEST_A

# Shoulders A: head plus a deltoid yoke.
SHOULDERS_A = [
    ("M12,3.2 m-1.9,0 a1.9,1.9 0 1 0 3.8,0 a1.9,1.9 0 1 0 -3.8,0", 1.8),
    ("M12,5.1 V6.0", 1.8),
    ("M9.8,6.0 C7.6,6.4 5.8,7.6 5.0,9.6 C4.5,11.0 4.6,12.2 5.1,13.4", 1.8),
    ("M14.2,6.0 C16.4,6.4 18.2,7.6 19.0,9.6 C19.5,11.0 19.4,12.2 18.9,13.4", 1.8),
    ("M9.8,6.0 C10.6,6.3 11.3,6.4 12,6.4 C12.7,6.4 13.4,6.3 14.2,6.0", 1.8),
    ("M5.1,13.4 C8.9,14.5 15.1,14.5 18.9,13.4", 1.8),
    ("M8.6,8.2 C8.0,9.6 8.0,11.2 8.6,12.4", 1.8),
    ("M15.4,8.2 C16.0,9.6 16.0,11.2 15.4,12.4", 1.8),
]

# Chest C: the bust, carried on down to the waist. Taller than the shoulder yoke, so
# the two silhouettes cannot be confused at 22px.
CHEST_C = [
    ("M12,3.0 m-1.9,0 a1.9,1.9 0 1 0 3.8,0 a1.9,1.9 0 1 0 -3.8,0", 1.8),
    ("M12,4.9 V5.5", 1.8),
    ("M8.6,5.5 C9.6,5.2 10.7,5.1 12,5.1 C13.3,5.1 14.4,5.2 15.4,5.5", 1.8),
    ("M8.6,5.5 C6.6,6.1 5.2,7.4 4.6,9.4 C4.1,10.9 4.2,12.6 4.4,14.0", 1.8),
    ("M15.4,5.5 C17.4,6.1 18.8,7.4 19.4,9.4 C19.9,10.9 19.8,12.6 19.6,14.0", 1.8),
    ("M4.4,14.0 C4.6,15.6 5.2,16.8 6.0,17.6", 1.8),
    ("M19.6,14.0 C19.4,15.6 18.8,16.8 18.0,17.6", 1.8),
    ("M6.0,17.6 C9.2,18.6 14.8,18.6 18.0,17.6", 1.8),
    ("M11.5,7.8 C9.3,6.8 6.9,7.3 6.1,9.1 C5.6,10.6 6.1,12.2 7.3,13.0", 1.8),
    ("M12.5,7.8 C14.7,6.8 17.1,7.3 17.9,9.1 C18.4,10.6 17.9,12.2 16.7,13.0", 1.8),
]

# Shoulders B: no head. A wide flat yoke whose deltoid caps are the widest thing in
# the glyph, so its silhouette is a different SHAPE from the chest's bust.
SHOULDERS_B = [
    ("M12,5.6 V6.6", 1.8),
    ("M12,6.6 C10.2,6.6 8.6,7.1 7.4,8.1", 1.8),
    ("M7.4,8.1 C5.6,9.4 4.4,11.0 4.1,12.8", 1.8),
    ("M4.1,12.8 C3.8,14.2 4.7,15.3 6.2,15.3", 1.8),
    ("M6.2,15.3 C7.7,15.3 8.7,14.4 9.2,13.1", 1.8),
    ("M12,6.6 C13.8,6.6 15.4,7.1 16.6,8.1", 1.8),
    ("M16.6,8.1 C18.4,9.4 19.6,11.0 19.9,12.8", 1.8),
    ("M19.9,12.8 C20.2,14.2 19.3,15.3 17.8,15.3", 1.8),
    ("M17.8,15.3 C16.3,15.3 15.3,14.4 14.8,13.1", 1.8),
    ("M9.2,13.1 C10.0,12.5 11.0,12.2 12,12.2 C13.0,12.2 14.0,12.5 14.8,13.1", 1.8),
]

# Shoulders D: the head, and a yoke with no chest lines at all - the deltoid caps are
# round and the only things on it, so nothing reads as pectorals.
SHOULDERS_D = [
    ("M12,3.2 m-1.9,0 a1.9,1.9 0 1 0 3.8,0 a1.9,1.9 0 1 0 -3.8,0", 1.8),
    ("M12,5.1 V6.2", 1.8),
    ("M9.9,6.2 C7.4,6.5 5.6,7.7 4.9,9.9 C4.5,11.4 4.8,12.9 5.7,13.9", 1.8),
    ("M14.1,6.2 C16.6,6.5 18.4,7.7 19.1,9.9 C19.5,11.4 19.2,12.9 18.3,13.9", 1.8),
    ("M9.9,6.2 C10.6,6.5 11.3,6.6 12,6.6 C12.7,6.6 13.4,6.5 14.1,6.2", 1.8),
    ("M5.7,13.9 C8.9,15.0 15.1,15.0 18.3,13.9", 1.8),
]

INK_LIGHT = "#4F46E5"
INK_DARK = "#818CF8"

GROUPS = ["Arms", "Legs", "Chest", "Back", "Core", "Shoulders"]


def svg(paths: list[tuple[str, float]], color: str, px: int) -> str:
    body = []
    for d, w in paths:
        if w == 0:
            body.append(f'<path d="{d}" fill="{color}" />')
        else:
            body.append(
                f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{w}" '
                f'stroke-linecap="round" stroke-linejoin="round" />'
            )
    return (
        f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" '
        f'xmlns="http://www.w3.org/2000/svg">{"".join(body)}</svg>'
    )


def eq_svg(drawable: str, px: int, color: str = "#2B2723") -> str:
    root = ET.parse(DRAWABLES / f"{drawable}.xml").getroot()
    return svg(
        [
            (el.attrib[f"{A}pathData"], float(el.attrib.get(f"{A}strokeWidth", 0)))
            for el in root
            if f"{A}pathData" in el.attrib
        ],
        color,
        px,
    )


def family(chest, shoulders, label: str) -> str:
    picks = [ARMS, LEGS, chest, BACK, CORE, shoulders]
    cells = "\n".join(
        f'<div class="cell"><div class="art">{svg(p, INK_LIGHT, 22)}'
        f'{svg(p, INK_LIGHT, 40)}</div><div class="name">{n}</div></div>'
        for p, n in zip(picks, GROUPS)
    )
    cells_dark = "\n".join(
        f'<div class="cell dark"><div class="art">{svg(p, INK_DARK, 22)}'
        f'{svg(p, INK_DARK, 40)}</div><div class="name">{n}</div></div>'
        for p, n in zip(picks, GROUPS)
    )
    return f"""
    <section class="fam">
      <h2>{label}</h2>
      <div class="strip">{cells}</div>
      <div class="strip darkrow">{cells_dark}</div>
    </section>"""


def main() -> int:
    kit = "\n".join(
        f'<div class="kit"><div class="cap">{n}</div>{eq_svg(d, 22)}{eq_svg(d, 74)}</div>'
        for n, d in EQUIPMENT
    )
    html = f"""<!doctype html>
<meta charset="utf-8">
<title>Muscle glyphs, final families - RepIo</title>
<style>
  body {{ margin:0; padding:24px; background:#EFEBE6; color:#2B2723;
         font:14px/1.5 -apple-system, Segoe UI, Roboto, sans-serif; }}
  h1 {{ font-size:19px; margin:0 0 6px; }}
  p.intro {{ max-width:82ch; color:#5C554E; margin:0 0 18px; }}
  h2 {{ font-size:13px; margin:0 0 8px; }}
  .kits {{ display:flex; flex-wrap:wrap; gap:8px; margin-bottom:22px; }}
  .kit {{ width:96px; background:#fff; border:1px solid #DED7CF; border-radius:10px;
          padding:8px 4px 4px; text-align:center; }}
  .kit svg {{ display:block; margin:0 auto; }}
  .fam {{ background:#fff; border:1px solid #DED7CF; border-radius:12px;
          padding:12px; margin-bottom:16px; }}
  .strip {{ display:flex; gap:8px; }}
  .cell {{ flex:1; background:#F4F1ED; border-radius:9px; padding:8px 4px; text-align:center; }}
  .cell.dark {{ background:#1E1E1E; }}
  .cell.dark .name {{ color:#B9B2AB; }}
  .art {{ display:flex; align-items:center; justify-content:center; gap:10px; }}
  .art svg {{ display:block; }}
  .name {{ font-size:10px; text-transform:uppercase; letter-spacing:.04em;
           color:#8A837B; margin-top:6px; }}
  .cap {{ font-size:10px; text-transform:uppercase; letter-spacing:.03em; color:#8A837B; }}
  .darkrow {{ margin-top:8px; }}
</style>
<h1>Muscle glyphs - the family, at the size it is drawn</h1>
<p class="intro">
  Small glyph is 22dp (the real size in the filter row), the larger is 40dp to read the drawing.
  Each strip is one complete family of six, so the chest and the shoulders can be judged against
  each other rather than on their own - they are the only two that want the top of a torso.
</p>
<div class="kits">{kit}</div>
{family(CHEST_C, SHOULDERS_D, "Family 5 - tall bust, shoulders as a bare yoke")}
{family(CHEST_C, SHOULDERS_A, "Family 6 - tall bust, shoulders with the arm lines")}
{family(CHEST_A, SHOULDERS_D, "Family 7 - chest plate, shoulders as a bare yoke")}
"""
    out = Path(__file__).with_name("final.html")
    out.write_text(html, encoding="utf-8")
    print(f"wrote {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
