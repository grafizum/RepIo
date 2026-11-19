#!/usr/bin/env python3
"""Sketchpad for the muscle glyphs: candidates, rendered, side by side.

Not part of the app - nothing here is a drawable. This is the pitch sheet: several
drawings of the same body part at once, so the choice is made by looking at them rather
than by imagining them, and only the winner is written into res/drawable.

A candidate is a list of strokes. Each stroke is (pathData, width):
width 0 fills the path, anything else strokes it at that width.

Usage: python3 evidence/muscle-icons-2026-10-11/sketch.py
"""
from __future__ import annotations

from pathlib import Path

INK = "#111111"
Stroke = tuple[str, float]

# ---------------------------------------------------------------------------
# The figure: one body, drawn once. Everything else is what gets inked on it.
# ---------------------------------------------------------------------------
HEAD: Stroke = ("M12,2.9 m-2.1,0 a2.1,2.1 0 1 0 4.2,0 a2.1,2.1 0 1 0 -4.2,0", 1.8)
NECK: Stroke = ("M12,5.0 V5.8", 1.8)
TORSO: Stroke = (
    "M6.8,6.4 C8.2,5.8 10.5,5.6 12,5.6 C13.5,5.6 15.8,5.8 17.2,6.4 "
    "C17.4,9.4 16.6,12.2 15.9,13.9 C15.5,15.0 15.7,15.7 15.7,16.2 "
    "H8.3 C8.3,15.7 8.5,15.0 8.1,13.9 C7.4,12.2 6.6,9.4 6.8,6.4 Z",
    1.8,
)
ARM_L: Stroke = ("M7.6,7.0 C6.2,9.8 5.5,12.8 5.5,15.8", 1.8)
ARM_R: Stroke = ("M16.4,7.0 C17.8,9.8 18.5,12.8 18.5,15.8", 1.8)
LEG_L: Stroke = ("M10.1,16.3 L9.6,22.8", 1.8)
LEG_R: Stroke = ("M13.9,16.3 L14.4,22.8", 1.8)

FIGURE = [HEAD, NECK, TORSO, ARM_L, ARM_R, LEG_L, LEG_R]

# The inked region per group - heavier than the figure it sits on.
ARMS_INK: list[Stroke] = [
    ("M7.6,7.0 C6.2,9.8 5.5,12.8 5.5,15.8", 3.8),
    ("M16.4,7.0 C17.8,9.8 18.5,12.8 18.5,15.8", 3.8),
]
LEGS_INK: list[Stroke] = [
    ("M10.1,16.0 L9.6,22.8", 4.2),
    ("M13.9,16.0 L14.4,22.8", 4.2),
]
# Two deltoid caps at the corners of the shoulder line - round, and SHORT, so they
# are not read as the arms (which run the whole way down the figure).
SHOULDERS_INK: list[Stroke] = [
    ("M8.8,7.0 C7.5,7.6 6.7,8.7 6.2,10.0", 4.6),
    ("M15.2,7.0 C16.5,7.6 17.3,8.7 17.8,10.0", 4.6),
]
# The two pectorals, reaching from the sternum out to the sides of the chest.
CHEST_INK: list[Stroke] = [
    ("M11.2,8.0 C9.4,7.4 8.2,7.6 7.4,8.6 C6.8,10.2 6.9,12.0 7.6,13.0 "
     "C9.0,14.2 10.4,13.6 11.2,12.2 Z", 0),
    ("M12.8,8.0 C14.6,7.4 15.8,7.6 16.6,8.6 C17.2,10.2 17.1,12.0 16.4,13.0 "
     "C15.0,14.2 13.6,13.6 12.8,12.2 Z", 0),
]
# The abdomen: the torso below the ribs, above the hips.
CORE_INK: list[Stroke] = [
    ("M8.4,11.6 H15.6 C15.9,11.6 16.0,11.9 16.0,12.4 V14.8 "
     "C16.0,15.5 15.7,15.9 15.0,15.9 H9.0 C8.3,15.9 8.0,15.5 8.0,14.8 "
     "V12.4 C8.0,11.9 8.1,11.6 8.4,11.6 Z", 0),
]
# The back: the whole trunk from the armpits down, lats included - the one region
# that is neither the chest's plates nor the abdomen's block.
BACK_INK: list[Stroke] = [
    ("M7.4,8.2 H16.6 C16.8,11.0 16.4,13.6 15.9,14.6 C15.5,15.6 15.7,16.0 15.7,16.2 "
     "H8.3 C8.3,16.0 8.5,15.6 8.1,14.6 C7.6,13.6 7.2,11.0 7.4,8.2 Z", 0),
]

# candidate name -> (figure, ink)
CANDIDATES: dict[str, tuple[list[Stroke], list[Stroke] | None]] = {
    "arms inked": (FIGURE, ARMS_INK),
    "legs inked": (FIGURE, LEGS_INK),
    "shoulders inked": (FIGURE, SHOULDERS_INK),
    "chest inked": (FIGURE, CHEST_INK),
    "core inked": (FIGURE, CORE_INK),
    "back inked": (FIGURE, BACK_INK),
    "nothing inked (the Any state)": (FIGURE, None),
}


def svg(strokes: list[Stroke], px: int, ink: str = INK, overlay: list[Stroke] | None = None) -> str:
    body = []
    for d, w in strokes + (overlay or []):
        if w == 0:
            body.append(f'<path d="{d}" fill="{ink}" />')
        else:
            body.append(
                f'<path d="{d}" fill="none" stroke="{ink}" stroke-width="{w}" '
                f'stroke-linecap="round" stroke-linejoin="round" />'
            )
    return (
        f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" '
        f'xmlns="http://www.w3.org/2000/svg">{"".join(body)}</svg>'
    )


def card(title: str, figure: list[Stroke], ink: list[Stroke] | None) -> str:
    return f"""
    <section class="card">
      <h2>{title}</h2>
      <div class="row">
        <div class="cell">{svg(figure, 22, overlay=ink)}<div class="cap">22px</div></div>
        <div class="cell">{svg(figure, 44, overlay=ink)}<div class="cap">2x</div></div>
        <div class="cell">{svg(figure, 66, overlay=ink)}<div class="cap">3x</div></div>
        <div class="cell big">{svg(figure, 110, overlay=ink)}<div class="cap">judge the shape</div></div>
      </div>
    </section>"""


def main() -> int:
    cards = "\n".join(card(t, f, i) for t, (f, i) in CANDIDATES.items())
    html = f"""<!doctype html>
<meta charset="utf-8">
<title>Muscle glyph candidates - RepIo</title>
<style>
  body {{ margin:0; padding:24px; background:#EFEBE6; color:#2B2723;
         font:15px/1.5 -apple-system, Segoe UI, Roboto, sans-serif; }}
  h1 {{ font-size:19px; margin:0 0 16px; }}
  .grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(320px,1fr)); gap:16px; }}
  .card {{ background:#fff; border:1px solid #DED7CF; border-radius:14px; padding:14px; }}
  h2 {{ font-size:13px; margin:0 0 10px; }}
  .row {{ display:flex; align-items:center; gap:12px; }}
  .cell {{ background:#F4F1ED; border-radius:10px; padding:8px; text-align:center; }}
  .cap {{ font-size:10px; text-transform:uppercase; letter-spacing:.03em; color:#8A837B; }}
</style>
<h1>Candidate glyphs - one body, the chosen part inked</h1>
<div class="grid">
{cards}
</div>
"""
    out = Path(__file__).with_name("sketch.html")
    out.write_text(html, encoding="utf-8")
    print(f"wrote {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
