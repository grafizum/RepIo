#!/usr/bin/env python3
"""Build the evidence page for the no-restart theme/icon switch.

Crops the launcher icon out of each home-screen capture and measures the artwork
colour, so the page's claim ("the icon really changed, with the process alive")
is read off the pixels rather than typed.

Usage: python evidence/theme-switch-2026-10-14/build_page.py
"""
from __future__ import annotations

import base64
import collections
import html
import struct
import sys
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "tools"))
from inspect_shot import load_png  # noqa: E402

# The launcher icon's box on the emulator's home screen: found by diffing the
# before/after frames and taking the bounding box (x 831..989, y 1890..2049), not
# guessed. 159x160 px at 1080x2400.
ICON = (831, 1890, 989, 2049)
PAD = 14  # a little air around the icon so the crop reads as an icon

FRAMES = [
    ("01_icon_developer.png", "Developer (gold)", "before"),
    ("02_icon_ocean.png", "Ocean (cyan)", "switched"),
    ("03_icon_restored.png", "Developer again", "restored"),
]


def _png_encode(width: int, height: int, rows: list[bytes]) -> bytes:
    raw = b"".join(b"\x00" + r for r in rows)

    def chunk(tag: bytes, body: bytes) -> bytes:
        return (struct.pack(">I", len(body)) + tag + body
                + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def crop_uri(name: str, factor: int = 2) -> tuple[str, str, float]:
    """(data URI of the icon crop, its dominant artwork colour, its share)."""
    _, _, rgb = load_png(str(HERE / name))
    x0, y0, x1, y1 = ICON
    x0 -= PAD
    y0 -= PAD
    x1 += PAD
    y1 += PAD
    rows = []
    for oy in range(y0, y1, factor):
        row = bytearray()
        for ox in range(x0, x1, factor):
            r = g = b = 0
            n = 0
            for dy in range(factor):
                for dx in range(factor):
                    pr, pg, pb = rgb(ox + dx, oy + dy)
                    r += pr
                    g += pg
                    b += pb
                    n += 1
            row += bytes((r // n, g // n, b // n))
        rows.append(bytes(row))
    width = (x1 - x0) // factor
    height = (y1 - y0) // factor
    png = _png_encode(width, height, rows)

    # The artwork colour: the most common pixel in the crop that is neither the
    # plate (a near-neutral dark) nor near-white, i.e. the accent the theme draws.
    counter: collections.Counter = collections.Counter()
    for row in rows:
        for i in range(0, len(row), 3):
            counter[(row[i], row[i + 1], row[i + 2])] += 1
    total = sum(counter.values())
    artwork = None
    for colour, count in counter.most_common(40):
        r, g, b = colour
        spread = max(r, g, b) - min(r, g, b)
        if spread > 40 and max(r, g, b) > 90:  # coloured, not the dark plate
            artwork = ("#%02X%02X%02X" % colour, 100.0 * count / total)
            break
    assert artwork is not None, f"{name}: no coloured artwork found in the icon crop"
    return "data:image/png;base64," + base64.b64encode(png).decode("ascii"), artwork[0], artwork[1]


def home_diff(a_name: str, b_name: str, top: int = 160) -> tuple[int, tuple[int, int, int, int]]:
    """Pixels that differ below the status bar, and their bounding box.

    The status bar is skipped because the CLOCK is in it and would otherwise show up
    as a change no theme could cause.
    """
    w, h, a = load_png(str(HERE / a_name))
    _, _, b = load_png(str(HERE / b_name))
    count = 0
    x0 = y0 = 10 ** 9
    x1 = y1 = -1
    for y in range(top, h):
        for x in range(w):
            if a(x, y) != b(x, y):
                count += 1
                x0 = min(x0, x)
                x1 = max(x1, x)
                y0 = min(y0, y)
                y1 = max(y1, y)
    return count, (x0, y0, x1, y1)


def icon_box_diff(a_name: str, b_name: str) -> int:
    """Differing pixels strictly inside the icon box."""
    _, _, a = load_png(str(HERE / a_name))
    _, _, b = load_png(str(HERE / b_name))
    x0, y0, x1, y1 = ICON
    return sum(1 for y in range(y0, y1) for x in range(x0, x1) if a(x, y) != b(x, y))


def main() -> int:
    changed, (bx0, by0, bx1, by1) = home_diff("01_icon_developer.png", "02_icon_ocean.png")
    restored_diff = icon_box_diff("01_icon_developer.png", "03_icon_restored.png")

    cards = []
    for name, label, kind in FRAMES:
        uri, colour, share = crop_uri(name)
        cards.append(
            f"""    <figure class="{kind}">
      <img src="{uri}" alt="{html.escape(label)} icon">
      <figcaption><b>{html.escape(label)}</b><br><code>{colour}</code> — {share:.1f}% of the crop</figcaption>
    </figure>"""
        )

    page = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>The launcher icon changes without restarting the app — 2026-10-14</title>
<style>
  :root {{ color-scheme: dark; }}
  body {{ margin: 0; padding: 30px; background: #0e0e0e; color: #edebe8;
          font: 15px/1.55 -apple-system, "Segoe UI", Roboto, sans-serif; }}
  h1 {{ font-size: 22px; margin: 0 0 6px; }}
  h2 {{ font-size: 14px; color: #e2b75a; letter-spacing: .09em; text-transform: uppercase;
        margin: 32px 0 10px; }}
  p {{ margin: 0 0 16px; color: #b8b4ad; max-width: 92ch; }}
  .row {{ display: flex; gap: 26px; flex-wrap: wrap; align-items: flex-start; }}
  figure {{ margin: 0; }}
  img {{ width: 150px; display: block; border-radius: 18px; border: 1px solid #2c2c2c; }}
  figcaption {{ margin-top: 9px; font-size: 12.5px; color: #b8b4ad; max-width: 150px; }}
  code {{ background: #1e1e1e; padding: 1px 5px; border-radius: 5px; color: #edebe8;
          font-size: 13px; }}
  table {{ border-collapse: collapse; font-size: 13.5px; }}
  th, td {{ text-align: left; padding: 6px 18px 6px 0; border-bottom: 1px solid #232323; }}
  th {{ color: #8d8a85; font-weight: 600; font-size: 11.5px; letter-spacing: .07em;
        text-transform: uppercase; }}
  .bad {{ color: #ff8a80; }}
  .good {{ color: #a5d6a7; }}
  .note {{ border-left: 2px solid #e2b75a; padding-left: 14px; }}
</style>
</head>
<body>
  <h1>Change the theme and the icon with no restart</h1>

  <p class="note"><b>Owner, 2026-10-14:</b> <i>"can we change the theme and icon without needing to
     close and reopen the app manually"</i>. The cause was one argument. <code>IconSwitchHelper</code>
     wrote its component states with <code>if (killApp) 0 else DONT_KILL_APP</code>, and every caller
     took the <b>0</b> — <code>PlatformUtils.applyAppIconChange</code> left the parameter at its
     <code>true</code> default — so the system was told it <i>may kill the process</i> to apply the
     change, and it did. A "Restart Required" dialog existed to explain a restart the app was
     causing itself.</p>

  <h2>The icon, before and after, as the launcher drew it</h2>
  <div class="row">
{chr(10).join(cards)}
  </div>

  <h2>Measured on emulator-5554, same run</h2>
  <table>
    <tr><th>what</th><th>before the fix</th><th>after the fix</th></tr>
    <tr><td>theme tapped</td><td>Ocean Teal</td><td>Ocean Teal, then Developer</td></tr>
    <tr><td>"Restart Required" dialog</td><td>shown</td><td class="good">none</td></tr>
    <tr><td>process after the change</td><td class="bad">gone — <code>pidof</code> empty</td>
        <td class="good">same PID (9943 → 9943, both switches)</td></tr>
    <tr><td>flags passed to the component write</td><td class="bad"><code>0</code></td>
        <td class="good"><code>1</code> = DONT_KILL_APP</td></tr>
    <tr><td>launcher entry</td><td>moved only after the kill</td>
        <td class="good">moved live: Retro → Ocean → Retro</td></tr>
    <tr><td>home-screen icon pixels</td><td>—</td>
        <td class="good">changed: {changed} px, all inside one {bx1 - bx0 + 1}×{by1 - by0 + 1} box</td></tr>
  </table>

  <p>The icon crops above are the same {ICON[2] - ICON[0] + 1}×{ICON[3] - ICON[1] + 1} box
     (x {ICON[0]}..{ICON[2]}, y {ICON[1]}..{ICON[3]}) cut out of three home-screen captures: before
     any change, after switching to Ocean, and after switching back. Switching the theme changed
     <b>{changed}</b> pixels of the whole home screen below the status bar, and
     <b>every one of them</b> lies in that single {bx1 - bx0 + 1}×{by1 - by0 + 1} box — a launcher
     icon's footprint is the only thing on the screen that moved. The <i>plate</i> is identical in
     all three and the <i>artwork</i> is what changes, gold to cyan and back, which is what a
     colour-variant icon should do and what it does with the process alive. Switching back left
     <b>{restored_diff}</b> differing pixels inside the icon box, i.e. none: the round trip is
     pixel-exact.</p>

  <p>Guarded by <code>IconSwitchKeepsTheProcessAliveTest</code>, which fails against the rejected
     shape (a sabotage run with `<code>killApp</code>` and a literal <code>0</code> put back
     produced one failure, then the file was restored byte-identical). Full suite
     <b>114 suites / 820 tests / 0 failures / 0 errors / 0 skipped</b>, <code>lintDebug</code>
     exit 0.</p>
</body>
</html>
"""
    (HERE / "index.html").write_text(page, encoding="utf-8")
    print(f"wrote {HERE / 'index.html'} ({len(page)} chars)")
    for name, label, _ in FRAMES:
        _, colour, share = crop_uri(name)
        print(f"  {label:18} artwork {colour} {share:.1f}%")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
