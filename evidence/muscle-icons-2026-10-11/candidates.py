#!/usr/bin/env python3
"""Candidates for the muscle glyphs: ISOLATED BODY PARTS, in the equipment set's language.

Owner, 2026-10-11, after the first attempt (one full figure with the chosen part
ink'd thick): *"absolutely not i want clean muscle group icons of body parts, arm
leg etc etc. not some wierd sht that you just drew in the preview tab. remake them
to actually match with the icons you drew for the equipments"*.

So the target is the equipment strip: stroke-only line art, 1.6-2.2 weights, round
caps and joins, one shape per idea, filling the 24x24 canvas. Nothing filled, nothing
thick, no body-with-a-region-marked.

Each candidate is (pathData, width); width 0 fills. Only the winner is written into
res/drawable.

Usage: python3 evidence/muscle-icons-2026-10-11/candidates.py
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

# ---------------------------------------------------------------- the drawings
# 24x24, stroke only, 1.8 default.

# A flexed arm, drawn as ONE closed outline like the bench and the kettlebell: the
# forearm rises on the left into a fist, the upper arm leaves to the right.
ARM = [
    (
        "M20.8,12.0 "
        "C17.6,10.4 13.2,10.3 10.2,11.6 "
        "C9.2,12.0 8.9,12.5 8.9,13.4 "
        "L8.9,8.4 "
        "C8.9,6.2 8.0,5.1 6.4,5.1 "
        "C4.8,5.1 3.9,6.2 3.9,8.4 "
        "L3.9,13.8 "
        "C3.9,15.7 5.0,16.9 6.8,17.3 "
        "C11.2,18.1 16.4,18.1 20.8,17.3 "
        "Z",
        1.8,
    ),
]

# The same arm, but with the fist read as a head over a flexed forearm: a small
# circle welded on the top of the limb.
ARM2 = [
    (
        "M20.6,12.2 "
        "C17.4,10.6 13.0,10.5 10.0,11.8 "
        "C9.0,12.3 8.7,12.8 8.7,13.7 "
        "L8.7,9.0 "
        "C8.7,6.8 7.8,5.6 6.2,5.6 "
        "C4.6,5.6 3.7,6.8 3.7,9.0 "
        "L3.7,13.9 "
        "C3.7,15.8 4.8,17.0 6.6,17.4 "
        "C11.0,18.2 16.2,18.2 20.6,17.4 "
        "Z",
        1.8,
    ),
    ("M6.2,4.6 m-1.5,0 a1.5,1.5 0 1 0 3.0,0 a1.5,1.5 0 1 0 -3.0,0", 1.8),
]

# Two legs, drawn as the one closed outline a body actually has there: waist, outer
# thighs, outer calves, feet, inner edges, and the crotch notch between them.
LEG = [
    (
        "M6.0,3.6 "
        "L18.0,3.6 "
        "C18.6,7.0 18.4,10.4 17.8,13.6 "
        "C17.3,16.2 17.3,18.2 17.4,19.6 "
        "L20.4,19.6 "
        "L20.4,21.8 "
        "L13.9,21.8 "
        "L14.2,11.4 "
        "C13.7,10.0 12.9,9.4 12.0,9.4 "
        "C11.1,9.4 10.3,10.0 9.8,11.4 "
        "L10.1,21.8 "
        "L3.6,21.8 "
        "L3.6,19.6 "
        "L6.6,19.6 "
        "C6.7,18.2 6.7,16.2 6.2,13.6 "
        "C5.6,10.4 5.4,7.0 6.0,3.6 "
        "Z",
        1.8,
    ),
]

# The same legs but no feet, a shorter line - for comparison.
LEG2 = [
    (
        "M6.0,3.6 "
        "L18.0,3.6 "
        "C18.6,7.2 18.2,10.8 17.6,14.0 "
        "C17.1,16.8 17.2,19.4 17.3,21.6 "
        "L14.4,21.6 "
        "L14.2,11.4 "
        "C13.7,10.0 12.9,9.4 12.0,9.4 "
        "C11.1,9.4 10.3,10.0 9.8,11.4 "
        "L9.6,21.6 "
        "L6.7,21.6 "
        "C6.8,19.4 6.9,16.8 6.4,14.0 "
        "C5.8,10.8 5.4,7.2 6.0,3.6 "
        "Z",
        1.8,
    ),
]

CHEST = [
    # The upper chest alone: shoulders, neck notch, and the two pectorals.
    ("M8.8,6.0 C9.7,5.7 10.8,5.6 12,5.6 C13.2,5.6 14.3,5.7 15.2,6.0", 1.8),
    ("M8.8,6.0 C6.8,6.5 5.4,7.6 4.7,9.4 C4.2,10.9 4.4,12.4 5.0,13.6", 1.8),
    ("M15.2,6.0 C17.2,6.5 18.6,7.6 19.3,9.4 C19.8,10.9 19.6,12.4 19.0,13.6", 1.8),
    ("M5.0,13.6 C9.0,14.7 15.0,14.7 19.0,13.6", 1.8),
    ("M11.6,8.3 C9.5,7.5 7.3,7.9 6.5,9.6 C6.0,11.0 6.5,12.5 7.6,13.3", 1.8),
    ("M12.4,8.3 C14.5,7.5 16.7,7.9 17.5,9.6 C18.0,11.0 17.5,12.5 16.4,13.3", 1.8),
]

CHEST2 = [
    # The same, with the head left on: more context, more like a person.
    ("M12,3.0 m-1.9,0 a1.9,1.9 0 1 0 3.8,0 a1.9,1.9 0 1 0 -3.8,0", 1.8),
    ("M12,4.9 V5.6", 1.8),
    ("M8.8,6.0 C9.7,5.7 10.8,5.6 12,5.6 C13.2,5.6 14.3,5.7 15.2,6.0", 1.8),
    ("M8.8,6.0 C6.8,6.5 5.4,7.6 4.7,9.4 C4.2,10.9 4.4,12.4 5.0,13.6", 1.8),
    ("M15.2,6.0 C17.2,6.5 18.6,7.6 19.3,9.4 C19.8,10.9 19.6,12.4 19.0,13.6", 1.8),
    ("M5.0,13.6 C9.0,14.7 15.0,14.7 19.0,13.6", 1.8),
    ("M11.6,8.3 C9.5,7.5 7.3,7.9 6.5,9.6 C6.0,11.0 6.5,12.5 7.6,13.3", 1.8),
    ("M12.4,8.3 C14.5,7.5 16.7,7.9 17.5,9.6 C18.0,11.0 17.5,12.5 16.4,13.3", 1.8),
]

BACK = [
    # The back: wide at the shoulders, tapering to the waist, spine down the middle
    # and a shoulder blade either side.
    ("M5.0,6.6 C7.6,5.8 10.2,5.6 12,5.6 C13.8,5.6 16.4,5.8 19.0,6.6", 1.8),
    ("M5.0,6.6 C5.2,9.8 6.4,13.0 8.3,15.2 C9.3,16.4 10.6,17.2 12,17.6", 1.8),
    ("M19.0,6.6 C18.8,9.8 17.6,13.0 15.7,15.2 C14.7,16.4 13.4,17.2 12,17.6", 1.8),
    ("M12,6.2 V17.4", 1.8),
    ("M8.3,8.2 C7.9,10.1 8.5,12.0 9.8,13.1", 1.8),
    ("M15.7,8.2 C16.1,10.1 15.5,12.0 14.2,13.1", 1.8),
]

CORE = [
    # The abdomen on its own: a compact block with the divisions of the abs.
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

SHOULDERS = [
    # Head, neck, and a wide deltoid yoke: the top of the body, cut off at the
    # armpits.
    ("M12,3.2 m-1.9,0 a1.9,1.9 0 1 0 3.8,0 a1.9,1.9 0 1 0 -3.8,0", 1.8),
    ("M12,5.1 V6.0", 1.8),
    ("M9.8,6.0 C7.6,6.4 5.8,7.6 5.0,9.6 C4.5,11.0 4.6,12.2 5.1,13.4", 1.8),
    ("M14.2,6.0 C16.4,6.4 18.2,7.6 19.0,9.6 C19.5,11.0 19.4,12.2 18.9,13.4", 1.8),
    ("M9.8,6.0 C10.6,6.3 11.3,6.4 12,6.4 C12.7,6.4 13.4,6.3 14.2,6.0", 1.8),
    ("M5.1,13.4 C8.9,14.5 15.1,14.5 18.9,13.4", 1.8),
    ("M8.6,8.2 C8.0,9.6 8.0,11.2 8.6,12.4", 1.8),
    ("M15.4,8.2 C16.0,9.6 16.0,11.2 15.4,12.4", 1.8),
]

SHOULDERS2 = [
    # Deltoids only: two rounded caps on a yoke, no head.
    ("M4.8,13.6 C4.2,10.6 5.2,8.0 7.2,6.8 C8.4,6.1 9.8,5.9 10.8,6.4", 1.8),
    ("M19.2,13.6 C19.8,10.6 18.8,8.0 16.8,6.8 C15.6,6.1 14.2,5.9 13.2,6.4", 1.8),
    ("M10.8,6.4 C11.2,7.0 12.8,7.0 13.2,6.4", 1.8),
    ("M12,7.0 V9.0", 1.8),
    ("M12,9.0 C13.6,9.0 15.0,10.2 15.4,11.8", 1.8),
    ("M12,9.0 C10.4,9.0 9.0,10.2 8.6,11.8", 1.8),
]

GROUPS: list[tuple[str, list[list[tuple[str, float]]]]] = [
    ("Arms", [ARM, ARM2]),
    ("Legs", [LEG, LEG2]),
    ("Chest", [CHEST, CHEST2]),
    ("Back", [BACK]),
    ("Core", [CORE]),
    ("Shoulders", [SHOULDERS, SHOULDERS2]),
]

INK_LIGHT = "#4F46E5"
INK_DARK = "#818CF8"


def svg_from_paths(paths: list[tuple[str, float]], color: str, px: int) -> str:
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
    return svg_from_paths(
        [
            (el.attrib[f"{A}pathData"], float(el.attrib.get(f"{A}strokeWidth", 0)))
            for el in root
            if f"{A}pathData" in el.attrib
        ],
        color,
        px,
    )


def main() -> int:
    kit = "\n".join(
        f'<div class="kit"><div class="cap">{n}</div>{eq_svg(d, 22)}{eq_svg(d, 74)}</div>'
        for n, d in EQUIPMENT
    )
    cards = []
    for group, cands in GROUPS:
        for i, c in enumerate(cands, start=1):
            cards.append(f"""
      <section class="card">
        <h2>{group} <span class="alt">candidate {i}</span></h2>
        <div class="row">
          {svg_from_paths(c, INK_LIGHT, 22)}<span class="cap">22</span>
          {svg_from_paths(c, INK_LIGHT, 44)}<span class="cap">44</span>
          {svg_from_paths(c, INK_LIGHT, 88)}<span class="cap">judge</span>
        </div>
        <div class="row dark">
          {svg_from_paths(c, INK_DARK, 22)}<span class="cap">22 dark</span>
          {svg_from_paths(c, INK_DARK, 60)}<span class="cap">60 dark</span>
          {svg_from_paths(c, INK_DARK, 88)}<span class="cap">judge dark</span>
        </div>
      </section>""")
    html = f"""<!doctype html>
<meta charset="utf-8">
<title>Muscle glyph candidates v3 - RepIo</title>
<style>
  body {{ margin:0; padding:24px; background:#EFEBE6; color:#2B2723;
         font:14px/1.5 -apple-system, Segoe UI, Roboto, sans-serif; }}
  h1 {{ font-size:19px; margin:0 0 6px; }}
  p.intro {{ max-width:80ch; color:#5C554E; margin:0 0 18px; }}
  h2 {{ font-size:13px; margin:0 0 8px; }}
  h2 .alt {{ color:#8A837B; font-weight:400; }}
  .kits {{ display:flex; flex-wrap:wrap; gap:8px; margin-bottom:22px; }}
  .kit {{ width:96px; background:#fff; border:1px solid #DED7CF; border-radius:10px;
          padding:8px 4px 4px; text-align:center; }}
  .kit svg {{ display:block; margin:0 auto; }}
  .grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(240px,1fr)); gap:14px; }}
  .card {{ background:#fff; border:1px solid #DED7CF; border-radius:12px; padding:12px; }}
  .row {{ display:flex; align-items:center; gap:10px; background:#F4F1ED;
          border-radius:9px; padding:8px 10px; margin-bottom:6px; }}
  .row.dark {{ background:#1E1E1E; }}
  .row.dark .cap {{ color:#9A9A9A; }}
  .cap {{ font-size:10px; text-transform:uppercase; letter-spacing:.03em; color:#8A837B; }}
</style>
<h1>Body-part glyphs, in the equipment set's language</h1>
<p class="intro">
  Stroke only, 1.8 weight, round caps, nothing filled - the same vocabulary as the kit strip
  underneath. Each group is one isolated part: no figure around it.
</p>
<div class="kits">{kit}</div>
<div class="grid">
{"".join(cards)}
</div>
"""
    out = Path(__file__).with_name("candidates.html")
    out.write_text(html, encoding="utf-8")
    print(f"wrote {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
