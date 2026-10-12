#!/usr/bin/env python3
"""Render the muscle-group drawables as a contact sheet, STRAIGHT FROM THE XML.

The icons are hand-written vector art, so the one thing worth checking is what
they look like - and the only honest way to show that is to read the same
`pathData` the app renders. Android vector path data IS SVG path data, so each
`<path>` maps 1:1 and nothing is redrawn for the picture.

Two rows of cards matter here:

* the EQUIPMENT strip first, because it is the style authority. The muscle glyphs
  were redrawn (2026-10-11) to match it - one isolated body part each, stroke only,
  1.8, round caps, nothing filled, the same 24dp box on the same 24x24 viewport -
  so the page has to show both at once or "matching" is an assertion rather than
  something the eye can check.
* then one card per muscle group: 22dp as drawn, x4 to judge the drawing, on the
  light surface and in the dark theme, with the filter row underneath.

Colours come from the app's own palette (getMuscleGroupColor / muscleGroupTextColor
in ui/components/WorkoutComponents.kt): the 600-step is the ink used on light
surfaces, the light step is the ink used on dark ones.

Usage: python3 evidence/muscle-icons-2026-10-11/build_sheet.py
"""
from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

A = "{http://schemas.android.com/apk/res/android}"
ROOT = Path(__file__).resolve().parents[2]
DRAWABLES = ROOT / "composeApp/src/main/res/drawable"

# The style authority: the kit glyphs the muscle glyphs have to sit beside.
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

# group -> (drawable, light-ink (600 step), dark-ink (light step))
GROUPS = [
    ("Chest", "ic_muscle_chest", "#DC2626", "#F87171"),
    ("Back", "ic_muscle_back", "#2563EB", "#60A5FA"),
    ("Legs", "ic_muscle_legs", "#059669", "#34D399"),
    ("Shoulders", "ic_muscle_shoulders", "#DB2777", "#F472B6"),
    ("Arms", "ic_muscle_arms", "#4F46E5", "#818CF8"),
    ("Core", "ic_muscle_core", "#CA8A04", "#FACC15"),
]


def svg_for(drawable: str, color: str, px: int) -> str:
    """One drawable as inline SVG, tinted - the same shapes the app draws."""
    path = DRAWABLES / f"{drawable}.xml"
    root = ET.parse(path).getroot()
    body = []
    for el in root:
        d = el.attrib.get(f"{A}pathData")
        if d is None:
            continue
        parts = [f'd="{d}"']
        if f"{A}fillColor" in el.attrib:
            parts.append(f'fill="{color}"')
        else:
            parts.append('fill="none"')
        if f"{A}strokeColor" in el.attrib:
            parts.append(f'stroke="{color}"')
        for src, dst in (
            ("strokeWidth", "stroke-width"),
            ("strokeLineCap", "stroke-linecap"),
            ("strokeLineJoin", "stroke-linejoin"),
        ):
            if f"{A}{src}" in el.attrib:
                parts.append(f'{dst}="{el.attrib[f"{A}{src}"]}"')
        body.append("<path " + " ".join(parts) + " />")
    return (
        f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" '
        f'xmlns="http://www.w3.org/2000/svg">{"".join(body)}</svg>'
    )


def row(group: str | None, drawable: str | None, ink: str, surface: str, label: str) -> str:
    """The collapsed filter row as it will read: label, value, Show more."""
    if drawable:
        value = svg_for(drawable, ink, 22)
    else:
        value = f'<span class="value-text" style="color:{ink}">{group or "Any"}</span>'
    return f"""
    <div class="row" style="background:{surface}">
      <span class="rowlabel">{label}</span>
      {value}
      <span class="spacer"></span>
      <span class="toggle">Show more <span class="chev">⌄</span></span>
    </div>"""


def equipment_strip() -> str:
    cells = "\n".join(
        f"""
      <div class="kit">
        <div class="cap">{name}</div>
        {svg_for(drawable, "#2B2723", 22)}
        {svg_for(drawable, "#2B2723", 88)}
      </div>"""
        for name, drawable in EQUIPMENT
    )
    return f"""
    <section class="card wide">
      <h2>The equipment set - the style these have to match</h2>
      <div class="kits">{cells}</div>
    </section>"""


def card(group: str, drawable: str, light_ink: str, dark_ink: str) -> str:
    return f"""
    <section class="card">
      <h2>{group}</h2>
      <div class="pair">
        <div class="onlight">
          <div class="cap">as drawn (22dp)</div>
          {svg_for(drawable, light_ink, 22)}
          <div class="cap">×4 to judge</div>
          {svg_for(drawable, light_ink, 88)}
        </div>
        <div class="ondark">
          <div class="cap">in the dark theme</div>
          {svg_for(drawable, dark_ink, 22)}
          <div class="cap">&nbsp;</div>
          {svg_for(drawable, dark_ink, 88)}
        </div>
      </div>
      {row(group, drawable, light_ink, "#F4F1ED", "Muscle Group")}
    </section>"""


def main() -> int:
    cards = "\n".join(card(*g) for g in GROUPS)
    html = f"""<!doctype html>
<meta charset="utf-8">
<title>Muscle group icons - RepIo</title>
<style>
  body {{ margin:0; padding:28px; background:#EFEBE6; color:#2B2723;
         font:15px/1.5 -apple-system, Segoe UI, Roboto, sans-serif; }}
  h1 {{ font-size:20px; margin:0 0 4px; }}
  p.intro {{ max-width:74ch; margin:0 0 22px; color:#5C554E; }}
  h2 {{ font-size:15px; margin:0 0 10px; }}
  .grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr)); gap:18px; }}
  .card {{ background:#fff; border:1px solid #DED7CF; border-radius:14px; padding:14px; }}
  .wide {{ grid-column:1/-1; margin-bottom:18px; }}
  .kits {{ display:flex; flex-wrap:wrap; gap:10px; }}
  .kit {{ flex:0 0 auto; width:104px; background:#F4F1ED; border-radius:10px;
          padding:10px 6px 6px; text-align:center; }}
  .kit svg {{ display:block; margin:0 auto; }}
  .kit .cap {{ margin-top:8px; }}
  .pair {{ display:flex; gap:12px; }}
  .onlight, .ondark {{ flex:1; border-radius:10px; padding:10px; text-align:center; }}
  .onlight {{ background:#F4F1ED; }}
  .ondark {{ background:#1E1E1E; }}
  .ondark .cap {{ color:#9A9A9A; }}
  .cap {{ font-size:10px; letter-spacing:.03em; text-transform:uppercase; color:#8A837B; margin:6px 0; }}
  .row {{ display:flex; align-items:center; gap:6px; margin-top:14px; border-radius:10px;
          padding:10px 12px; border:1px solid #DED7CF; }}
  .rowlabel {{ font-weight:700; }}
  .spacer {{ flex:1; }}
  .toggle {{ font-size:12px; font-weight:700; }}
  .chev {{ opacity:.7; }}
  .value-text {{ font-weight:700; font-size:13px; }}
  .anyrow {{ margin-top:18px; background:#fff; border:1px solid #DED7CF; border-radius:14px; padding:14px; }}
  code {{ background:#EFEBE6; padding:1px 5px; border-radius:5px; font-size:12px; }}
</style>
<h1>Muscle group icons</h1>
<p class="intro">
  The six glyphs behind the muscle filter's collapsed row, read straight out of
  <code>composeApp/src/main/res/drawable/ic_muscle_*.xml</code> - the same path data the app
  renders, tinted with the app's own per-group ink. Each is ONE body part drawn alone: an arm, a
  pair of legs, a chest, a back, a core, a pair of shoulders. The strip at the top is the
  equipment set they were redrawn to match. Left column is a light surface (Warm Beige), right
  column the dark theme. The small one is the real 22dp size; the large one is there so you can
  judge the drawing itself. The row under each card is how it will read in the filter.
</p>
{equipment_strip()}
<div class="grid">
{cards}
</div>
<div class="anyrow">
  <h2>Any (every muscle)</h2>
  <p class="intro" style="margin:0 0 10px">
    No body part is chosen, so no glyph is drawn - the row falls back to the word, exactly as the
    equipment row falls back to "All equipment" when it has no kit glyph to show.
  </p>
  {row(None, None, "#64748B", "#F4F1ED", "Muscle Group")}
</div>
"""
    out = Path(__file__).with_name("index.html")
    out.write_text(html, encoding="utf-8")
    print(f"wrote {out} ({len(html)} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
