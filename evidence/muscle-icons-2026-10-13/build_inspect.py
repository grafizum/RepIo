#!/usr/bin/env python3
"""Build a self-contained page showing the two things a redraw has to reconcile.

Left: the seven reference PNGs from /home/matrix/Downloads/Muscles, embedded as data
URIs (a registered workspace HTML file is served on its own, so sibling assets 404 —
that is why the earlier board showed broken images in the Preview tab).
Right: the SIX glyphs the app ships right now, converted from their Android vector XML
into inline SVG so both can be looked at in one screen at the SAME size.

Run:  python3 evidence/muscle-icons-2026-10-13/build_inspect.py
"""
from __future__ import annotations

import base64
import html
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / "inspect.html"
REF_DIR = Path("/home/matrix/Downloads/Muscles")
DRAWABLES = ROOT / "composeApp/src/main/res/drawable"

REFS = ["abs.png", "bein.png", "body-part.png", "korperteil.png",
        "muskel.png", "muskeln.png", "schultern.png"]

# What the app needs. Reported at 24dp, which is the only size that matters.
GROUPS = ["arms", "back", "chest", "core", "legs", "shoulders"]

PATH_RE = re.compile(r'<path(?P<body>[^>]*?)/>', re.S)
ATTR_RE = re.compile(r'android:(?P<k>pathData|strokeWidth|strokeColor|fillColor)="(?P<v>[^"]*)"')


def vector_to_svg(path: Path) -> tuple[str, list[str]]:
    """(svg, notes) for one vector drawable, drawn the way Android draws it."""
    text = path.read_text(encoding="utf-8")
    strokes, widths, fills = [], [], []
    for m in PATH_RE.finditer(text):
        attrs = {a.group("k"): a.group("v") for a in ATTR_RE.finditer(m.group("body"))}
        d = attrs.get("pathData")
        if not d:
            continue
        if attrs.get("strokeColor"):
            strokes.append(d)
            widths.append(float(attrs.get("strokeWidth", "1.6")))
        if attrs.get("fillColor"):
            fills.append(d)
    w = max(widths) if widths else 1.6
    body = "".join(
        f'<path d="{d}" fill="none" stroke="currentColor" stroke-width="{w}" '
        f'stroke-linecap="round" stroke-linejoin="round"/>'
        for d in strokes
    )
    body += "".join(f'<path d="{d}" fill="currentColor"/>' for d in fills)
    svg = f'<svg viewBox="0 0 24 24" width="100%" height="100%">{body}</svg>'
    notes = [f"{len(strokes)} stroke path(s), width {w:g}", "filled paths: none" if not fills
             else f"{len(fills)} filled"]
    return svg, notes


def tile(inner: str, cls: str) -> str:
    return f'<div class="tile {cls}">{inner}</div>'


def main() -> None:
    ref_cards = []
    for name in REFS:
        src = REF_DIR / name
        if not src.exists():
            continue
        b64 = base64.b64encode(src.read_bytes()).decode()
        img = f'<img src="data:image/png;base64,{b64}" alt="{html.escape(name)}">'
        ref_cards.append(
            f'<div class="card"><h2>{html.escape(name)}</h2>'
            f'<div class="tiles">{tile(img, "dark")}{tile(img, "light")}</div>'
            f'<div class="meta">{src.stat().st_size // 1024} KB · 512×512</div></div>'
        )

    glyph_cards = []
    for g in GROUPS:
        f = DRAWABLES / f"ic_muscle_{g}.xml"
        if not f.exists():
            continue
        svg, notes = vector_to_svg(f)
        glyph_cards.append(
            f'<div class="card"><h2>ic_muscle_{g}</h2>'
            f'<div class="tiles">'
            f'{tile(f"<div class=\"glyph\">{svg}</div>", "dark")}'
            f'{tile(f"<div class=\"glyph light\">{svg}</div>", "light")}'
            f'</div><div class="meta">{" · ".join(notes)} · 24dp viewport</div></div>'
        )

    OUT.write_text(f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>Muscle glyphs — references vs what ships</title>
<style>
  :root {{ --ink:#EDEBE8; --muted:#A0A0A0; --gold:#DFB761; --bg:#0A0A0A; --card:#1B1B1B; }}
  * {{ box-sizing:border-box; }}
  body {{ margin:0; padding:28px 32px 64px; background:var(--bg); color:var(--ink);
         font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif; }}
  h1 {{ font-size:22px; margin:0 0 4px; }}
  p.lead {{ color:var(--muted); margin:0 0 26px; max-width:88ch; }}
  h2 {{ font-size:13px; margin:0; padding:12px 14px 8px; font-weight:600; letter-spacing:.01em; }}
  .grid {{ display:grid; grid-template-columns:repeat(auto-fill,minmax(300px,1fr)); gap:18px; }}
  .card {{ background:var(--card); border:1px solid #2F2F2F; border-radius:14px; overflow:hidden; }}
  .tiles {{ display:grid; grid-template-columns:1fr 1fr; }}
  .tile {{ display:grid; place-items:center; height:170px; }}
  .tile.dark {{ background:#0A0A0A; color:#EDEBE8; }}
  .tile.light {{ background:#F4F1EC; color:#1A1A1A; }}
  .tile img {{ width:140px; height:140px; }}
  .tile.light img {{ filter:invert(1); }}
  .glyph {{ color:var(--gold); }}
  .glyph.light {{ color:#8C6239; }}
  .glyph.big {{ width:120px; height:120px; }}
  .glyph.mid {{ width:48px; height:48px; }}
  .glyph.small {{ width:22px; height:22px; }}
  .meta {{ color:var(--muted); font-size:12px; padding:0 14px 12px; }}
  .strip {{ display:flex; gap:14px; align-items:flex-end; margin:0 0 30px; flex-wrap:wrap; }}
  .strip .item {{ text-align:center; color:var(--muted); font-size:11px; }}
  .note {{ margin-top:30px; padding:16px 18px; border-left:3px solid var(--gold);
           background:#1B1B1B; border-radius:0 10px 10px 0; max-width:92ch; }}
</style></head><body>
  <h1>Reference set vs the six glyphs that ship</h1>
  <p class="lead">Left tile = the ink as given (black), right tile = inverted for a light theme.
  The shipped glyphs are drawn here from their own Android XML, tinted the app's accent, at the
  same 24dp box.</p>

  <h1 style="font-size:16px;margin-top:8px">The six, at real sizes</h1>
  <div class="strip">
    {''.join(f'<div class="item"><div class="glyph small" style="margin:0 auto">{vector_to_svg(DRAWABLES / f"ic_muscle_{g}.xml")[0]}</div>22dp</div><div class="item"><div class="glyph mid" style="margin:0 auto">{vector_to_svg(DRAWABLES / f"ic_muscle_{g}.xml")[0]}</div>48dp</div>' for g in GROUPS if (DRAWABLES / f"ic_muscle_{g}.xml").exists())}
  </div>

  <h1 style="font-size:16px">The references</h1>
  <div class="grid">{''.join(ref_cards)}</div>

  <h1 style="font-size:16px;margin-top:34px">What ships now</h1>
  <div class="grid">{''.join(glyph_cards)}</div>

  <div class="note"><strong>Note.</strong> The reference drawings are 512px anatomical line art with
  interior muscle lines; the shipped glyphs are 24px one-part symbols. Any redraw has to decide how
  much of that interior detail survives at 22–24dp, where a 1.6 stroke in a 24-unit box is the only
  weight the filter lane allows.</div>
</body></html>
""", encoding="utf-8")
    print(f"wrote {OUT} ({OUT.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
