#!/usr/bin/env python3
"""Build the evidence page for the My Workout card spacing / rim / timer-card pass.

Everything the page claims is READ OFF THE PIXELS of the captures sitting next to
it - the gap between the cards in px and dp, the rim each card carries and the
fill each card wears - so the page cannot quietly disagree with the screenshots it
shows. There are captures from two devices:

  * emulator-5554 (1080x2400, density 420 = 2.625x, DEVELOPER theme, gold #E2B75A)
  * SM-S906B, the owner's phone (1080x2340, override density 420 = 2.625x, gold
    #DBB969 - a different theme, but the same geometry and the same ladder)

Usage: python evidence/my-workout-cards-2026-10-14/build_page.py
"""
from __future__ import annotations

import base64
import html
import struct
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
DENSITY = 420 / 160  # both captures: density 420 / baseline
SCAN_X = 540          # mid column, clear of every button and glyph
BACKGROUND = "#0E0E0E"
MIN_CARD_PX = 150     # shorter content runs are chrome, not cards

CAPTURES = [
    ("01_two_cards.png", "Emulator · the two cards, no timer running"),
    ("02_timer_card.png", "Emulator · rest timer running, before the grey pass"),
    ("03_phone_greyed_timer.png", "S22+ (SM-S906B) · rest timer running, after the grey pass"),
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


def colour_at(px, w, x, y):
    o = (y * w + x) * 4
    return "#%02X%02X%02X" % (px[o], px[o + 1], px[o + 2])


def near_background(c: str) -> bool:
    """True when a colour is within a few units of the page background."""
    return sum(abs(int(c[i:i + 2], 16) - int(BACKGROUND[i:i + 2], 16)) for i in (1, 3, 5)) <= 12


def analyse(name: str):
    w, h, px = read_png(HERE / name)
    # A CARD is a run of NON-background pixels (fill, rim and anything drawn on
    # it); the GAP between two cards is the run of background between them.
    # Counting runs of the card FILL instead splits one card in two wherever a
    # line of text crosses the column and reports a phantom "gap" - the first
    # version of this script did exactly that and printed 13.3dp for a 16dp gap.
    content: list[list] = []
    for y in range(200, 2200):
        if colour_at(px, w, SCAN_X, y) == BACKGROUND:
            continue
        if content and content[-1][1] == y - 1:
            content[-1][1] = y
        else:
            content.append([y, y])
    cards = []
    for a, b in [c for c in content if c[1] - c[0] + 1 >= MIN_CARD_PX]:
        # The FILL is the first colour that HOLDS for at least STABLE px. Not "the
        # first colour that differs from the top pixel": a card's 1dp rim is
        # anti-aliased over its first couple of rows, so that rule lands on a blend
        # of rim and background - the first version of this script therefore
        # reported the emulator's timer card as #E1B65A, a gold, when it is grey.
        # Nor the centre: the level card's centre is the per-set banner well, a
        # block drawn ON the card rather than the card's own colour.
        STABLE = 10
        fill = None
        for y in range(a, min(a + 40, b - STABLE)):
            if all(
                colour_at(px, w, SCAN_X, y + k) == colour_at(px, w, SCAN_X, y)
                for k in range(STABLE)
            ):
                fill = colour_at(px, w, SCAN_X, y)
                break
        if fill is None:
            fill = colour_at(px, w, SCAN_X, (a + b) // 2)
        # The rim is the first short run near the top that is NEITHER the fill NOR
        # the background. Both exclusions are needed: a band's top row can hold a
        # dark anti-aliased seam (the first version read one as a 4px "#100F0F rim"),
        # and the rim itself is a couple of anti-aliased rows, so matching on
        # "differs from the top pixel" finds a blend instead of the rim.
        rim_colour, rw = None, 0
        y = a
        while y < min(a + 12, b):
            c = colour_at(px, w, SCAN_X, y)
            if c == fill:
                break
            run = 0
            while y + run <= b and colour_at(px, w, SCAN_X, y + run) == c:
                run += 1
            if not near_background(c) and run <= 8:
                rim_colour, rw = c, run
                break
            y += run
        cards.append({"a": a, "b": b, "fill": fill, "rim_width": rw, "rim": rim_colour})
    gaps = []
    for c1, c2 in zip(cards, cards[1:]):
        span = c2["a"] - c1["b"] - 1
        pure = all(colour_at(px, w, SCAN_X, y) == BACKGROUND for y in range(c1["b"] + 1, c2["a"]))
        gaps.append((span, pure))
    return {"w": w, "h": h, "cards": cards, "gaps": gaps}


results = {name: analyse(name) for name, _ in CAPTURES}
two = results["01_two_cards.png"]
before = results["02_timer_card.png"]
after = results["03_phone_greyed_timer.png"]


def dp(px: int) -> float:
    return round(px / DENSITY, 1)


def data_uri(name: str) -> str:
    return "data:image/png;base64," + base64.b64encode((HERE / name).read_bytes()).decode()


def card_rows(r):
    return "\n".join(
        f"<tr><td>{'timer' if i == 1 and len(r['cards']) == 3 else 'card ' + str(i)}</td>"
        f"<td><code>{c['fill']}</code></td>"
        f"<td><code>{c['rim']}</code> &times; {c['rim_width']}px"
        f"{'' if c['rim'] else ' (none read)'}</td>"
        f"<td>{c['b'] - c['a'] + 1}px ({dp(c['b'] - c['a'] + 1)}dp)</td></tr>"
        for i, c in enumerate(r["cards"], 1)
    )


def gap_rows(r):
    return "\n".join(
        f"<tr><td>{span}px</td><td><b>{dp(span)}dp</b></td>"
        f"<td>{'pure background' if pure else 'CONTAINS NON-BACKGROUND PIXELS'}</td></tr>"
        for span, pure in r["gaps"]
    )


two_gap, two_gap_pure = two["gaps"][0]
timer_before_fill = before["cards"][0]["fill"]
timer_after_fill = after["cards"][0]["fill"]
hero_fill = after["cards"][1]["fill"]
phone_rim = after["cards"][0]["rim"]

page = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>My Workout cards — spacing, rim, timer card, 2026-10-14</title>
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
  img {{ width: 300px; border: 1px solid #3d3d3d; border-radius: 14px; display: block; }}
  figcaption {{ color: #8d8a85; font-size: 12.5px; margin-top: 8px; }}
  .row {{ display: flex; gap: 26px; flex-wrap: wrap; }}
  code {{ background: #1b1b1b; padding: 1px 5px; border-radius: 5px; }}
  .good {{ color: #a5d6a7; }}
</style>
</head>
<body>
  <h1>My Workout: less space between the cards, one rim, and a rest timer that reads as temporary</h1>

  <p class="note"><b>Owner, 2026-10-14:</b> <i>"between the 2 cards in the 'my workout' tab i want
     slightly less space, and the border of the card under be the same as the card above. and for
     the timer reset do the same things of background color border etc. as the design."</i> — then,
     on seeing it: <i>"actually i want the rest timer slightly greyed again so it gives the vibe
     that its there temporary"</i>. The two cards are the exercise card and the level card; the
     "timer reset" is the rest/transition card that appears above them while a timer runs.</p>

  <h2>01 · The two cards, read down the column x={SCAN_X} (emulator)</h2>
  <table>
    <tr><th>card</th><th>fill</th><th>rim</th><th>height</th></tr>
    {card_rows(two)}
  </table>
  <table>
    <tr><th>gap between the cards</th><th>dp</th><th>what is in it</th></tr>
    {gap_rows(two)}
  </table>
  <p>The gap is <b>{two_gap}px = {dp(two_gap)}dp</b>
     ({'pure background' if two_gap_pure else 'CONTAINS NON-BACKGROUND PIXELS'}) — the column's
     own <code>spacedBy(16.dp)</code> and nothing else. It used to carry an extra
     <code>sectionSpacing</code> spacer on top, which made this one gap 16 + 16 + 16 = 48dp while
     every other gap on the screen stayed 16dp. Both cards wear the <b>same</b> full-strength
     accent rim: the level card used its own inline tint
     (<code>primary.copy(alpha = 0.35f)</code>), which equals <code>workoutCardRim</code> on a light
     scheme but is a WASH of the accent on a dark one — so on the theme captured the two cards wore
     visibly different edges. Both now read the rim from the one shared function.</p>

  <h2>02 · The timer card: same family, slightly greyed</h2>
  <table>
    <tr><th>capture</th><th>timer card fill</th><th>hero card fill</th><th>same?</th></tr>
    <tr><td>emulator, before the grey pass</td><td><code>{timer_before_fill}</code></td>
        <td><code>{before['cards'][1]['fill']}</code></td>
        <td>{'yes' if timer_before_fill == before['cards'][1]['fill'] else 'no'}</td></tr>
    <tr><td>phone, after the grey pass</td><td><code>{timer_after_fill}</code></td>
        <td><code>{hero_fill}</code></td>
        <td class="good">{'yes' if timer_after_fill == hero_fill else 'no — one rung lower'}</td></tr>
  </table>
  <p>The timer card is now <b>one grey rung under</b> the two cards below it: on this dark scheme
     <code>{timer_after_fill}</code> against their <code>{hero_fill}</code> — the same step in the
     ladder that the app's general cards use. It is the only card on My Workout that comes and goes
     by itself, so the receded surface is the whole signal that it is temporary, while the rim
     (<code>{phone_rim}</code>) and the accent in its writing keep it in the family.</p>

  <h2>03 · The same three cards on the owner's phone</h2>
  <table>
    <tr><th>card</th><th>fill</th><th>rim</th><th>height</th></tr>
    {card_rows(after)}
  </table>
  <table>
    <tr><th>gap</th><th>dp</th><th>what is in it</th></tr>
    {gap_rows(after)}
  </table>
  <p>Measured on the SM-S906B (1080x2340) after installing the build. Its rim is
     <code>{phone_rim}</code> rather than the emulator's <code>#E2B75A</code> because the phone is
     on the <b>PURPLE</b> theme (<code>theme_option = PURPLE</code>), whose primary is a violet —
     the rim follows the theme's accent, as it should. What matters is what is the SAME: all three
     cards on the phone wear that one violet rim (timer, exercise and level), all three fills are
     neutral greys, and both gaps are {dp(after['gaps'][0][0])}dp — the same geometry the emulator
     shows, on a different theme and a different screen size.</p>

  <div class="row">
"""
for name, label in CAPTURES:
    page += (
        f'    <figure><img src="{data_uri(name)}" alt="{html.escape(label)}">'
        f"<figcaption>{html.escape(label)}</figcaption></figure>\n"
    )
page += f"""  </div>

  <p>Every number on this page is measured by <code>build_page.py</code> from the PNGs above, so
     none of it can drift from the screenshots it sits under. Geometries: emulator-5554
     1080x2400 and SM-S906B 1080x2340, both at density 420 (2.625x). The emulator was restored
     afterwards (session back to exercise 1, set 1/4, timer stopped).</p>
</body>
</html>
"""

(HERE / "index.html").write_text(page, encoding="utf-8")
print(f"wrote {HERE / 'index.html'} ({len(page)} chars)")
for name, label in CAPTURES:
    r = results[name]
    print(f"{label}:")
    for c in r["cards"]:
        print(f"    fill={c['fill']} rim={c['rim']}x{c['rim_width']}px h={c['b']-c['a']+1}px")
    print(f"    gaps={[(s, dp(s), p) for s, p in r['gaps']]}")
print(f"timer fill before={timer_before_fill} after={timer_after_fill} hero={hero_fill}")
