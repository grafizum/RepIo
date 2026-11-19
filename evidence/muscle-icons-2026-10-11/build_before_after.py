#!/usr/bin/env python3
"""Before/after board: the hand-drawn muscle glyphs vs the computed ones.

Owner, 2026-10-12: *"i dont see ANY changes in the preview ... apply it
geometrically by coding and show the new design to me."* The generated drawables
kept each part's identity on purpose, so the two sets LOOK related - this board
puts them side by side, at judge size, so the difference is visible: the left
column is the old hand-placed curve data, the right column is what
math_glyphs.py computed today (read live from res/drawable).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
DRAWABLES = REPO / "composeApp/src/main/res/drawable"
INK = "#8C6239"  # Warm Beige - the theme primary both rows wear in the app

# The hand-drawn path data, exactly as it sat in res/drawable before --write.
OLD = {
    "chest": [
        "M5.4,8.0 C8.6,6.2 13.4,5.6 17.6,6.8 C19.8,7.6 20.8,9.2 20.4,11.0 C19.6,14.2 14.6,16.8 9.0,16.0 C7.0,15.6 5.6,14.4 5.2,12.4 C5.0,11.0 5.1,9.4 5.4,8.0 Z",
        "M7.4,9.6 C10.4,9.0 14.4,9.2 17.6,10.2",
        "M7.6,14.0 C10.4,13.6 14.2,12.8 17.2,11.4",
    ],
    "shoulders": [
        "M15.9,6.8 C13.9,5.2 10.9,5.0 8.7,6.6 C6.9,8.0 6.1,10.4 6.4,13.0 C6.7,15.6 6.8,18.2 6.8,20.8 L12.9,20.8 C13.0,18.2 13.2,15.6 13.4,13.2 C14.1,11.0 15.3,8.6 15.9,6.8",
        "M15.9,6.8 C17.1,6.0 18.3,5.8 19.3,6.2",
        "M10.4,9.4 C9.8,11.6 9.9,14.0 10.5,16.0",
        "M7.9,17.4 C9.3,18.4 11.1,18.4 12.5,17.4",
    ],
    "legs": [
        "M10.8,3.4 C9.0,5.6 8.2,8.8 8.8,11.8 C9.1,13.6 8.9,15.4 9.2,17.0 C9.4,18.4 8.5,19.5 7.1,20.3 C6.1,20.7 5.4,21.0 5.6,21.4 C5.8,21.7 6.8,21.7 8.0,21.7 L13.6,21.7 C15.1,21.7 15.9,20.8 15.6,19.6 C15.4,18.7 14.9,18.2 15.1,17.2 C15.6,15.5 16.8,14.6 16.4,12.6 C16.1,10.4 16.6,7.4 15.8,3.4",
        "M9.6,7.6 C10.6,9.4 11.6,10.8 13.2,11.6",
        "M9.6,13.6 C10.4,14.6 11.4,15.0 12.4,14.7",
        "M13.4,14.6 C14.4,15.8 14.8,17.0 14.6,18.2",
    ],
    "back": [
        "M18.1,8.4 C15.9,6.6 12.5,6.0 9.5,7.4 C6.9,8.6 5.3,11.0 5.5,13.6 C5.8,16.6 7.9,19.6 11.1,21.0 C12.9,21.6 15.1,21.2 16.5,19.8 C17.9,18.2 18.4,15.6 18.3,13.2 C18.3,11.4 18.2,9.8 18.1,8.4 Z",
        "M8.2,10.6 C9.6,13.2 11.4,15.6 13.8,17.4",
        "M6.6,14.4 C8.4,16.6 10.6,18.4 13.0,19.6",
    ],
    "core": [
        "M7.0,4.8 C9.4,3.8 14.6,3.8 17.0,4.8 C18.2,7.6 18.4,11.0 17.6,14.2 C17.0,17.0 16.2,19.2 15.2,20.4 C14.6,21.1 13.6,21.4 12.8,21.0 C12.4,20.8 12.2,20.3 12,19.6 C11.8,20.3 11.6,20.8 11.2,21.0 C10.4,21.4 9.4,21.1 8.8,20.4 C7.8,19.2 7.0,17.0 6.4,14.2 C5.6,11.0 5.8,7.6 7.0,4.8 Z",
        "M12,6.6 L12,18.4",
        "M7.8,9.4 C9.8,10.2 14.2,10.2 16.2,9.4",
        "M7.6,13.0 C9.7,13.8 14.3,13.8 16.4,13.0",
        "M8.4,16.6 C10.1,17.3 13.9,17.3 15.6,16.6",
    ],
    "arms": [
        "M14.6,7.6 C15.6,6.8 16.0,5.4 15.4,4.3 C14.7,2.9 12.5,3.1 12.1,4.7 C10.5,5.2 9.4,6.3 8.6,7.9 C7.2,10.7 5.4,13.7 5.1,16.3 C4.8,18.5 6.3,20.1 8.6,20.4 C12.6,20.9 17.4,20.6 21.0,19.7",
        "M11.9,8.7 C11.8,10.1 11.8,11.5 11.9,12.8",
        "M11.3,14.7 C12.0,12.0 14.6,10.6 16.8,11.9 C17.6,12.4 18.1,13.1 18.4,13.7 C19.0,12.9 19.9,12.3 20.9,12.3",
        "M13.4,17.1 C14.8,18.0 16.6,18.0 18.0,17.2",
    ],
}

NOTES = {
    "arms": "the reference - unchanged, the style authority",
    "chest": "4 circumcircle arcs + fibers converging at the shoulder insertion",
    "shoulders": "top of an arm: true half-circle delt cap, tangent-solved stump cut across, concentric head arc + crease",
    "legs": "Catmull-Rom muscle edges (C1), circumcircle instep/toe/heel, straight sole",
    "back": "closed Catmull-Rom ring; spine-side points collinear -> exactly straight edge",
    "core": "flanks mirrored x -> 24-x (machine symmetry); arch apex exactly x=12",
}

ORDER = ["arms", "chest", "shoulders", "legs", "back", "core"]


def new_paths(name: str) -> list[str]:
    xml = (DRAWABLES / f"ic_muscle_{name}.xml").read_text()
    return re.findall(r'android:pathData="([^"]+)"', xml)


def svg(paths: list[str], size: int, muted: bool = False) -> str:
    body = "\n".join(
        f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="1.6" '
        'stroke-linecap="round" stroke-linejoin="round"/>'
        for d in paths
    )
    op = ' opacity="0.45"' if muted else ""
    return (
        f'<svg width="{size}" height="{size}" viewBox="0 0 24 24"{op} '
        f'aria-hidden="true">{body}</svg>'
    )


def main() -> int:
    cards = []
    for name in ORDER:
        old, new = OLD[name], new_paths(name)
        cards.append(
            f"""
    <div class="card">
      <h2>{name}</h2>
      <div class="pair">
        <figure>{svg(old, 132, muted=True)}<figcaption>BEFORE - hand-placed curves</figcaption></figure>
        <figure>{svg(new, 132)}<figcaption class="new">AFTER - computed geometry</figcaption></figure>
      </div>
      <div class="row">{svg(old, 22, muted=True)}{svg(new, 22)}</div>
      <p>{NOTES[name]}</p>
    </div>"""
        )
    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Muscle glyphs - before/after</title>
<style>
  body {{ background: #F3EEE7; color: #2A2118; font-family: system-ui, sans-serif;
         margin: 0; padding: 32px; }}
  h1 {{ margin: 0 0 4px; }}
  .sub {{ color: #6B5B47; margin: 0 0 24px; max-width: 70ch; line-height: 1.5; }}
  .grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
          gap: 18px; }}
  .card {{ background: #FBF8F4; border-radius: 16px; padding: 18px 20px;
          box-shadow: 0 1px 3px rgba(60,40,20,.08); }}
  .card h2 {{ margin: 0 0 10px; font-size: 17px; text-transform: capitalize; }}
  .pair {{ display: flex; gap: 12px; justify-content: space-between; }}
  figure {{ margin: 0; text-align: center; }}
  figcaption {{ font-size: 11px; color: #6B5B47; margin-top: 6px; }}
  figcaption.new {{ color: {INK}; font-weight: 700; }}
  .row {{ display: flex; gap: 10px; align-items: center; justify-content: center;
         margin-top: 12px; padding-top: 10px; border-top: 1px dashed #D8CCBB; }}
  .row::before {{ content: "real size"; font-size: 10px; color: #6B5B47;
                 margin-right: auto; }}
  p {{ font-size: 12px; color: #6B5B47; margin: 12px 0 0; line-height: 1.45; }}
</style>
</head>
<body>
  <h1>Muscle group icons - before / after</h1>
  <p class="sub">Left of each card: the curves as placed by hand. Right: the same body
  part rebuilt through coding - every curve a computed object (circumcircle arcs,
  closed-form tangent circles, Catmull-Rom runs, exact mirror symmetry), emitted by
  <code>evidence/muscle-icons-2026-10-11/math_glyphs.py</code> straight into
  res/drawable. The arm is the reference and did not change.</p>
  <div class="grid">{''.join(cards)}
  </div>
</body>
</html>
"""
    out = HERE / "before_after.html"
    out.write_text(html)
    print(f"wrote {out} ({len(html)} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
