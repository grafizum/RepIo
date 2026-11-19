#!/usr/bin/env python3
"""THE BOARD: the owner's own drawings beside the glyphs that are supposed to be them.

Owner, 2026-10-13, looking at the previous hand-composed glyphs: *"your ... images drawn are
not at all the same as the images that you are showing, remake the ... things based on the
images"*. That is a claim about a picture, so the only evidence that answers it is a picture:
every card here puts the reference drawing, the OLD glyph and the TRACED glyph in one row, at
the real 22dp the filter row uses and blown up to 88px so the stroke itself can be judged.

The drawn columns are read straight out of the drawables - Android vector path data is SVG
path data, so each <path> maps 1:1 and nothing is redrawn for the picture. The reference
columns are the PNGs from /home/matrix/Downloads/Muscles, copied into ./refs so this page is
self-contained and the trace stays reproducible.

Usage: python3 evidence/muscle-icons-2026-10-13/build_board.py   # writes index.html
"""
from __future__ import annotations

import base64
import xml.etree.ElementTree as ET
from pathlib import Path

A = "{http://schemas.android.com/apk/res/android}"
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
DRAWABLES = ROOT / "composeApp/src/main/res/drawable"
REFS = HERE / "refs"
OLD = HERE / "previous"

PRIM = "#8C6239"  # the theme primary, exactly what equipmentGlyphTint returns

EQUIPMENT = [
    ("Barbell", "ic_eq_barbell"), ("Bench", "ic_eq_bench"), ("Cable", "ic_eq_cable"),
    ("Leg ext.", "ic_eq_chair"), ("Dip bars", "ic_eq_dipbars"),
    ("Kettlebell", "ic_eq_kettlebell"), ("Machine", "ic_eq_machine"),
    ("Pull-up", "ic_eq_pullup"), ("Rack", "ic_eq_rack"), ("Rings", "ic_eq_rings"),
]

# group -> (drawable stem, reference drawing, what the drawing shows)
GROUPS = [
    ("Arms", "ic_muscle_arms", "muskel.png", "the flexed arm, elbow to wrist"),
    ("Core", "ic_muscle_core", "abs.png", "the abdomen plate: outline, pec V, the grid"),
    ("Legs", "ic_muscle_legs", "bein.png", "one leg, hip to foot"),
    ("Shoulders", "ic_muscle_shoulders", "schultern.png", "the delt cap and trapezius"),
    ("Chest", "ic_muscle_chest", "korperteil.png", "the torso frame with the pec slab"),
    ("Back", "ic_muscle_back", "body-part.png", "the torso frame with the lat yoke"),
]


def svg(path: Path, color: str, px: int) -> str:
    root = ET.parse(path).getroot()
    body = []
    for el in root:
        d = el.attrib.get(f"{A}pathData")
        if d is None:
            continue
        parts = [f'd="{d}"']
        parts.append(f'fill="{color}"' if f"{A}fillColor" in el.attrib else 'fill="none"')
        if f"{A}strokeColor" in el.attrib:
            parts.append(f'stroke="{color}"')
        for src, dst in (("strokeWidth", "stroke-width"), ("strokeLineCap", "stroke-linecap"),
                         ("strokeLineJoin", "stroke-linejoin")):
            if f"{A}{src}" in el.attrib:
                parts.append(f'{dst}="{el.attrib[f"{A}{src}"]}"')
        body.append("<path " + " ".join(parts) + " />")
    return (f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" '
            f'xmlns="http://www.w3.org/2000/svg">{"".join(body)}</svg>')


def png(name: str, px: int) -> str:
    data = base64.b64encode((REFS / name).read_bytes()).decode()
    return (f'<img width="{px}" height="{px}" src="data:image/png;base64,{data}" '
            f'alt="{name}">')


def filter_row(drawable: str, surface: str) -> str:
    return f"""
    <div class="row" style="background:{surface}">
      <span class="rowlabel">Muscle Group</span>
      <span class="val">{svg(DRAWABLES / f"{drawable}.xml", PRIM, 22)}</span>
      <span class="spacer"></span>
      <span class="toggle">Show more <span class="chev">&#8964;</span></span>
    </div>"""


def card(group: str, stem: str, ref: str, what: str) -> str:
    new = f'<div class="cell">{svg(DRAWABLES / f"{stem}.xml", PRIM, 88)}{svg(DRAWABLES / f"{stem}.xml", PRIM, 22)}</div>'
    old = ""
    if (OLD / f"{stem}.xml").exists():
        old = (f'<div class="cell">{svg(OLD / f"{stem}.xml", PRIM, 88)}'
               f'{svg(OLD / f"{stem}.xml", PRIM, 22)}</div>')
    return f"""
    <section class="card">
      <h2>{group}<span class="what">{what}</span></h2>
      <div class="strip">
        <div class="cell ref">{png(ref, 88)}{png(ref, 22)}</div>
        {old}
        {new}
      </div>
      <div class="cap"><span>the drawing</span><span>before</span><span>now &mdash; exactly what the filter row draws</span></div>
      {filter_row(stem, "#F4F1ED")}
    </section>"""


def main() -> None:
    cards = "\n".join(card(*g) for g in GROUPS)
    kit = "".join(
        f'<div class="kit"><div class="cap">{name}</div>'
        f'{svg(DRAWABLES / f"{d}.xml", PRIM, 22)}{svg(DRAWABLES / f"{d}.xml", PRIM, 88)}</div>'
        for name, d in EQUIPMENT
    )
    html = f"""<!doctype html>
<meta charset="utf-8">
<title>Muscle group glyphs - traced from the drawings</title>
<style>
  body {{ margin:0; padding:26px; background:#EFEBE6; color:#2B2723;
         font:15px/1.5 -apple-system, "Segoe UI", Roboto, sans-serif; }}
  h1 {{ font-size:20px; margin:0 0 6px; }}
  h2 {{ font-size:15px; margin:0 0 12px; display:flex; align-items:baseline; gap:10px; }}
  h2 .what {{ font-size:11px; font-weight:400; text-transform:uppercase; letter-spacing:.06em;
              color:#8A837B; }}
  p.intro {{ max-width:82ch; margin:0 0 20px; color:#5C554E; }}
  .grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(330px,1fr)); gap:16px; }}
  .card {{ background:#fff; border:1px solid #DED7CF; border-radius:14px; padding:14px; }}
  .strip {{ display:flex; gap:14px; align-items:flex-end; }}
  .cell {{ flex:0 0 auto; text-align:center; }}
  .cell svg, .cell img {{ display:block; margin:0 auto; }}
  .cell.ref img {{ opacity:.9; }}
  .cap {{ display:flex; gap:14px; font-size:10px; letter-spacing:.03em; text-transform:uppercase;
          color:#8A837B; margin:8px 0 0; }}
  .cap span {{ width:88px; }}
  .cap span:last-child {{ width:auto; }}
  .row {{ display:flex; align-items:center; gap:8px; margin-top:14px; border-radius:10px;
          padding:10px 12px; border:1px solid #DED7CF; }}
  .rowlabel {{ font-weight:700; }}
  .val {{ display:flex; }}
  .spacer {{ flex:1; }}
  .toggle {{ font-size:12px; font-weight:700; }}
  .kits {{ display:flex; flex-wrap:wrap; gap:8px; }}
  .kit {{ background:#fff; border:1px solid #DED7CF; border-radius:10px; padding:8px 6px 4px;
          text-align:center; }}
  .kit svg {{ display:block; margin:0 auto; }}
  .kit .cap {{ display:block; width:auto; margin:4px 0 0; }}
</style>
<h1>Muscle group glyphs &mdash; traced from the drawings</h1>
<p class="intro">
  Each card: <b>the owner's drawing</b>, <b>the glyph that was in the app</b>, and <b>the traced
  glyph</b>. The drawing column is the source PNG; the other two are read straight out of
  <code>composeApp/src/main/res/drawable/ic_muscle_*.xml</code>, so they are the same path data
  the app renders, tinted with the theme primary. The big one is 88px so the line can be judged;
  the small one next to it is the real 22dp of the filter row, and the row underneath is how it
  reads collapsed. The kit glyphs at the bottom are the style authority: stroke only, round caps,
  1.6, one 24dp box.
</p>
<section class="card" style="margin-bottom:18px">
  <h2>The equipment set &mdash; the hand these sit in</h2>
  <div class="kits">{kit}</div>
</section>
<div class="grid">
{cards}
</div>
"""
    (HERE / "index.html").write_text(html, encoding="utf-8")
    print(f"wrote {HERE / 'index.html'}")

    refs = "\n".join(
        f'<figure><div class="crop">{png(ref, 260)}</div>'
        f'<figcaption>{group}: <code>{ref}</code></figcaption></figure>'
        for group, _, ref, _ in GROUPS
    )
    # The drawings as they actually are, big: several of these plates carry a ruled frame
    # and body plates that ASCII renders as boxes, and the only way to know which is which
    # is to look at them.
    (HERE / "refs.html").write_text(f"""<!doctype html>
<meta charset="utf-8"><title>The reference drawings</title>
<style>
  body {{ margin:0; padding:24px; background:#fff; color:#2B2723;
         font:15px/1.5 -apple-system, "Segoe UI", Roboto, sans-serif; }}
  h1 {{ font-size:19px; margin:0 0 4px; }}
  p {{ max-width:80ch; color:#5C554E; margin:0 0 20px; }}
  .row {{ display:flex; flex-wrap:wrap; gap:18px; }}
  figure {{ margin:0; background:#EFEBE6; border-radius:12px; padding:12px; text-align:center; }}
  .crop img {{ display:block; }}
  figcaption {{ margin-top:8px; font-size:12px; color:#5C554E; }}
  code {{ font-size:11px; background:#fff; padding:1px 5px; border-radius:5px; }}
</style>
<h1>The reference drawings</h1>
<p>Copied from /home/matrix/Downloads/Muscles, at 260px, so the frame and the anatomy can both
be seen. These are the files the six glyphs are traced from.</p>
<div class="row">
{refs}
</div>
""", encoding="utf-8")
    print(f"wrote {HERE / 'refs.html'}")


if __name__ == "__main__":
    main()
