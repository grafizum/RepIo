#!/usr/bin/env python3
"""Build the evidence page for taming the dark themes' primary actions.

Every number is READ OFF THE PIXELS of the two FLAME captures sitting next to
this file - the share of the frame the vivid accent covers before and after, the
button's own fill, and the contrast of its label on that fill - so the page
cannot disagree with the screenshots it shows.

Captures: emulator-5554, 1080x2400, density 420, FLAME theme (the owner's own
"very heavy on the eye" example).

Usage: python evidence/dark-themes-tamed-2026-10-14/build_page.py
"""
from __future__ import annotations

import base64
import html
import struct
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ACCENT = "FF6D00"  # FLAME's primary, the vivid orange of the report
BUTTON_BOX = (45, 1950, 1035, 2082)  # the pinned primary action, from uiautomator bounds
FRAMES = [
    ("01_flame_before.png", "Before · the accent AT FULL STRENGTH as the button's fill"),
    ("02_flame_after.png", "After · a TONE of the accent as the fill, the accent as the label"),
]


def read_png(path: Path):
    d = path.read_bytes()
    i, w, h, idat = 8, 0, 0, b""
    while i < len(d):
        ln = struct.unpack(">I", d[i:i + 4])[0]
        typ = d[i + 4:i + 8]
        data = d[i + 8:i + 8 + ln]
        i += 12 + ln
        if typ == b"IHDR":
            w, h = struct.unpack(">II", data[:8])
        elif typ == b"IDAT":
            idat += data
        elif typ == b"IEND":
            break
    raw = zlib.decompress(idat)
    bpp, stride, out, prev, pos = 4, w * 4, bytearray(), bytearray(w * 4), 0
    for _ in range(h):
        f = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos + stride]); pos += stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                line[x] = (line[x] + ((line[x - bpp] if x >= bpp else 0) + prev[x]) // 2) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if pa <= pb and pa <= pc else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        out += line
        prev = line
    return w, h, out


def hex_at(px, w, x, y):
    o = (y * w + x) * 4
    return "%02X%02X%02X" % (px[o], px[o + 1], px[o + 2])


def lum(hexc: str) -> float:
    def chan(v: int) -> float:
        c = v / 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (int(hexc[i:i + 2], 16) for i in (0, 2, 4))
    return 0.2126 * chan(r) + 0.7152 * chan(g) + 0.0722 * chan(b)


def contrast(a: str, b: str) -> float:
    la, lb = lum(a), lum(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)


def analyse(name: str):
    w, h, px = read_png(HERE / name)
    accent_px = sum(
        1
        for y in range(h)
        for x in range(w)
        if hex_at(px, w, x, y) == ACCENT
    )
    # The button's own fill: the modal colour inside its bounds.
    counts: dict[str, int] = {}
    x0, y0, x1, y1 = BUTTON_BOX
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = hex_at(px, w, x, y)
            counts[c] = counts.get(c, 0) + 1
    top = sorted(counts.items(), key=lambda kv: -kv[1])
    return {
        "w": w, "h": h,
        "accent_share": 100 * accent_px / (w * h),
        "button_fill": top[0][0],
        "button_label": next(c for c, _ in top if c != top[0][0] and c != "0E0E0E"),
    }


results = {name: analyse(name) for name, _ in FRAMES}
before = results["01_flame_before.png"]
after = results["02_flame_after.png"]


def data_uri(name: str) -> str:
    return "data:image/png;base64," + base64.b64encode((HERE / name).read_bytes()).decode()


page = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Taming the dark themes' primary actions — 2026-10-14</title>
<style>
  :root {{ color-scheme: dark; }}
  body {{ margin: 0; padding: 28px; background: #0e0e0e; color: #edebe8;
         font: 15px/1.55 -apple-system, "Segoe UI", Roboto, sans-serif; }}
  h1 {{ font-size: 21px; margin: 0 0 6px; }}
  h2 {{ font-size: 14px; margin: 30px 0 10px; color: #e2b75a;
        letter-spacing: .08em; text-transform: uppercase; }}
  p {{ margin: 0 0 14px; color: #b8b4ad; max-width: 84ch; }}
  .note {{ border-left: 2px solid #e2b75a; padding-left: 14px; }}
  table {{ border-collapse: collapse; margin: 0 0 18px; font-size: 13.5px; }}
  th, td {{ text-align: left; padding: 4px 18px 4px 0; border-bottom: 1px solid #262626; }}
  th {{ color: #8d8a85; font-size: 11.5px; letter-spacing: .07em; text-transform: uppercase; }}
  figure {{ margin: 0 0 22px; }}
  img {{ width: 320px; border: 1px solid #3d3d3d; border-radius: 14px; display: block; }}
  figcaption {{ color: #8d8a85; font-size: 12.5px; margin-top: 8px; }}
  .row {{ display: flex; gap: 26px; flex-wrap: wrap; }}
  code {{ background: #1b1b1b; padding: 1px 5px; border-radius: 5px; }}
  .good {{ color: #a5d6a7; }}
  .bad {{ color: #ff8a80; }}
  .swatch {{ display: inline-block; width: 52px; height: 16px; border-radius: 4px;
             vertical-align: text-bottom; border: 1px solid #3d3d3d; }}
</style>
</head>
<body>
  <h1>The dark themes' primary action: a tone of the accent, not the accent</h1>

  <p class="note"><b>Owner, 2026-10-14:</b> <i>"the other dark themes are too strong ... i want them
     to be less strong on the eye especially if the primary color is shown orange in the my workout
     tab it is very heavy on the eye, id prefer to keep it less strong if possible"</i>. Both
     captures below are the FLAME theme's My Workout, on emulator-5554, one build apart.</p>

  <h2>What was heavy, measured rather than estimated</h2>
  <table>
    <tr><th>frame</th><th>vivid accent <code>#{ACCENT}</code></th><th>the button's own fill</th></tr>
    <tr>
      <td>before</td>
      <td class="bad">{before['accent_share']:.2f}% of the screen</td>
      <td><span class="swatch" style="background: #{before['button_fill']}"></span>
          <code>#{before['button_fill']}</code></td>
    </tr>
    <tr>
      <td>after</td>
      <td class="good">{after['accent_share']:.2f}% of the screen</td>
      <td><span class="swatch" style="background: #{after['button_fill']}"></span>
          <code>#{after['button_fill']}</code></td>
    </tr>
  </table>
  <p>The vivid orange falls from <b>{before['accent_share']:.2f}%</b> of the frame to
     <b>{after['accent_share']:.2f}%</b> — a
     <b>{(1 - after['accent_share'] / before['accent_share']) * 100:.0f}% cut</b> in how much of the
     screen is saturated accent. What replaces it is the button's own fill, a deep tone of the same
     hue (<code>#{after['button_fill']}</code>, which is
     {contrast(after['button_fill'], '0E0E0E'):.2f}:1 from the canvas — a panel, not a glare), with
     the accent kept as the label: <code>#{after['button_label']}</code> on it, which is
     <b>{contrast(after['button_label'], after['button_fill']):.2f}:1</b>.</p>

  <h2>What is left of the accent, and why</h2>
  <p>The {after['accent_share']:.2f}% that remains is the accent doing a job: the button's label and
     icon, the two workout cards' 1dp rims, the level number, the progress bar. Those are lines and
     words. The thing removed was the accent used as an AREA, which is the same rule the cards had
     already adopted — <code>workoutCardFill</code> is a tint and <code>workoutCardRim</code> a line —
     and the primary action was the last place still breaking it.</p>

  <h2>Scope</h2>
  <ul>
    <li><b>Toned:</b> the dark themes that are not the developer skin — FLAME, PURPLE, OCEAN, ROSE.</li>
    <li><b>Untouched:</b> DEVELOPER (the hand-tuned accent gold the owner asked for) and the four
        LIGHT themes (BEIGE, EMERALD, CRIMSON, ROYAL_BLUE), whose accents never glared.</li>
    <li><b>Every</b> solid primary button on My Workout goes through one function
        (<code>workoutActionButtonColors()</code>) — the save-set button, Restart, Start workout
        again, previous/next, and the demo buttons — because ten independent call sites are exactly
        how the slab got there.</li>
  </ul>

  <div class="row">
"""
for name, label in FRAMES:
    page += (
        f'    <figure><img src="{data_uri(name)}" alt="{html.escape(label)}">'
        f"<figcaption>{html.escape(label)}</figcaption></figure>\n"
    )
page += f"""  </div>

  <p>Held by three cases in <code>DarkThemeContrastTest</code>: the tone must not be the raw accent
     and must be quieter than it, the label must clear 4.5:1 on the tone in every dark theme, and the
     developer skin plus the light themes must keep their solid fill. Reverting the tone to the raw
     accent fails two of them.</p>
</body>
</html>
"""

(HERE / "index.html").write_text(page, encoding="utf-8")
print(f"wrote {HERE / 'index.html'} ({len(page)} chars)")
print(f"before: accent {before['accent_share']:.2f}%  button fill #{before['button_fill']}  label #{before['button_label']}")
print(f"after : accent {after['accent_share']:.2f}%  button fill #{after['button_fill']}  label #{after['button_label']}")
print(f"label on tone: {contrast(after['button_label'], after['button_fill']):.2f}:1")
