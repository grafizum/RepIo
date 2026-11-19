#!/usr/bin/env python3
"""Build index.html for the dark-cards evidence, measuring the captures.

Every number on the page is SAMPLED from the PNG next to it rather than typed,
because the previous revision of this page was itself the thing that misled the
owner: its "after" frame had been shot BEFORE the bottom bar's elevation tint was
fixed, so the page showed a #2C2C2C bar and called it the current look. A page
whose prose is generated from the frames cannot go stale that way - re-shoot and
re-run, and the text follows.

Usage: python evidence/dark-cards-2026-10-14/build_page.py
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


def _png_encode(width: int, height: int, rows: list[bytes]) -> bytes:
    """Minimal 8-bit truecolour PNG writer (filter type 0 on every row)."""
    raw = b"".join(b"\x00" + r for r in rows)

    def chunk(tag: bytes, body: bytes) -> bytes:
        return (struct.pack(">I", len(body)) + tag + body
                + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def data_uri(name: str, factor: int = 2) -> str:
    """A capture as a self-contained data URI, box-filtered down by [factor].

    The page is opened from the Preview tab, which serves ONE file and cannot
    resolve a sibling <img src> - a relative path there is a broken image, which
    is the same class of failure as the stale frame this page was rebuilt to fix.
    The captures are 1080 px wide and shown at 268, so a 2x box filter is still
    twice the pixels actually displayed.
    """
    w, h, rgb = load_png(str(HERE / name))
    ow, oh = w // factor, h // factor
    rows = []
    for oy in range(oh):
        row = bytearray()
        for ox in range(ow):
            r = g = b = 0
            for dy in range(factor):
                for dx in range(factor):
                    pr, pg, pb = rgb(ox * factor + dx, oy * factor + dy)
                    r += pr
                    g += pg
                    b += pb
            n = factor * factor
            row += bytes((r // n, g // n, b // n))
        rows.append(bytes(row))
    png = _png_encode(ow, oh, rows)
    return "data:image/png;base64," + base64.b64encode(png).decode("ascii")


def shares(name: str, step: int = 5, top: int = 7) -> tuple[str, dict, list]:
    """Dominant colours of one capture, as (size, {hex: pct}, ordered list)."""
    w, h, rgb = load_png(str(HERE / name))
    counter: collections.Counter = collections.Counter()
    for y in range(0, h, step):
        for x in range(0, w, step):
            counter[rgb(x, y)] += 1
    total = sum(counter.values())
    ranked = [("#%02X%02X%02X" % c, 100.0 * n / total) for c, n in counter.most_common(top)]
    return f"{w}x{h}", dict(ranked), ranked


def share(name: str, hexcode: str) -> float:
    """What percentage of a capture is exactly this colour (0.0 when absent)."""
    _, table, _ = shares(name, top=40)
    return table.get(hexcode, 0.0)


def bar(name: str) -> str:
    """The share row under a figure, measured from the frame above it."""
    _, _, ranked = shares(name)
    cells = "".join(
        f'<span class="sw" style="background:{c}"></span><code>{c}</code> {p:.1f}%'
        for c, p in ranked
    )
    return f'<div class="share">{cells}</div>'


BEFORE_BAR = share("11_debug_dev_home.png", "#2C2C2C")
MID_BAR = share("22_dev_home.png", "#1E1E1E")
AFTER_BAR = share("24_dev_home.png", "#1B1B1B")
BEIGE_BAR = share("20_beige_home.png", "#F0EBE5")
AFTER_CARDS = share("24_dev_home.png", "#191919")
BEIGE_CARDS = share("20_beige_home.png", "#F4F1ED")
BEFORE_WORKOUT_BAR = share("13_workout_dev_final.png", "#2C2C2C")
MID_WORKOUT_MERGED = share("23_dev_workout.png", "#1E1E1E")
AFTER_WORKOUT_CARD = share("25_dev_workout.png", "#1E1E1E")
AFTER_WORKOUT_BAR = share("25_dev_workout.png", "#1B1B1B")
BEIGE_WORKOUT_CARD = share("21_beige_workout.png", "#F4F0EB")
BEIGE_WORKOUT_BAR = share("21_beige_workout.png", "#F0EBE5")
# THE REP CARD: the nested banner inside the hero card ("SET 1 OF 4 / 8 reps / 8/8/8/8 Reps").
REP_HOLE = share("26_dev_workout_before_rep.png", "#0E0E0E")
REP_FILL = share("27_dev_workout_after_rep.png", "#191919")
REP_AFTER_PAGE = share("27_dev_workout_after_rep.png", "#0E0E0E")
REP_BEFORE_PAGE = share("26_dev_workout_before_rep.png", "#0E0E0E")
BEIGE_WELL = share("21_beige_workout.png", "#E7DFD5")
BEIGE_WELL_PAGE = share("21_beige_workout.png", "#FDFCFB")

LADDER = [
    ("canvas", "the page", "#0E0E0E", "#0E0E0E", "the page (untouched by design)"),
    ("surface", "sheets, dialogs", "#141414", "#1B1B1B", "sheets, dialogs, chips, inputs"),
    ("surfaceVariant", "the general cards", "#191919", "#1E1E1E",
     "every Material card, the general panels, nested wells"),
    ("DarkBarSurface", "the bottom bar", "#1B1B1B", "\u2014",
     "the bottom navigation bar, and nothing else"),
    ("surfaceContainerHigh", "the hero cards", "#1E1E1E", "#1E1E1E",
     "the workout hero cards \u2014 the front panel, and the only thing on this rung"),
    ("surfaceContainerHighest", "pressed states, the tab pill", "#282828", "#2F2F2F",
     "pressed and dragged states, the selected tab pill"),
]

MIRROR = [
    ("canvas", "#FDFCFB", "1.000", "#0E0E0E", "1.000"),
    ("chips, sheets", "#F9F7F4", "1.044", "#141414", "1.048"),
    ("general cards", "#F4F1ED", "1.099", "#191919", "1.098"),
    ("the bottom bar", "#F0EBE5", "1.157", "#1B1B1B", "1.121"),
    ("hero card", "#F4F0EB", "1.107", "#1E1E1E", "1.158"),
    ("selected tab pill", "#E6DDD4", "1.309", "#282828", "1.309"),
    ("the nested well", "#E7DFD5", "1.297", "#191919", "1.098"),
]


def figure(name: str, heading: str, caption: str) -> str:
    return f"""    <figure>
      <h3>{html.escape(heading)}</h3>
      <img src="{data_uri(name)}" alt="{html.escape(heading)}">
      <figcaption>{caption}</figcaption>
      {bar(name)}
    </figure>"""


def main() -> int:
    figures = {
        "before": figure(
            "11_debug_dev_home.png", "Before — Developer (dark)",
            f"Every Material card and the bottom bar are <code>surface</code> plus an elevation "
            f"tint, and the tint was "
            f"<code>onSurface</code> — a near-white ink — so both composited to "
            f"<code>#2C2C2C</code>: the bar covers <b>{BEFORE_BAR:.1f}%</b> of this frame at "
            f"<code>#2C2C2C</code>, and it is the lightest thing on the screen by a mile.",
        ),
        "after": figure(
            "24_dev_home.png", "After — Developer (dark)",
            f"The tint is the ladder's own top rung (<code>DarkSurfaceHighest</code>) and the rungs "
            f"came down with it, so the bar is <code>#1B1B1B</code> (<b>{AFTER_BAR:.1f}%</b> of the "
            f"frame, <code>#2C2C2C</code> is gone entirely) and the general cards are "
            f"<code>#191919</code> (<b>{AFTER_CARDS:.1f}%</b>) \u2014 darker than the bar, which is "
            f"the order the beige theme has too. (The first attempt at this landed the bar on the "
            f"hero cards' rung, {MID_BAR:.1f}% of #1E1E1E, which fixed the wash and lost the "
            f"split \u2014 see My Workout below.)",
        ),
        "beige": figure(
            "20_beige_home.png", "Beige — the reference (light)",
            f"The target, measured rather than described: bar <code>#F0EBE5</code> "
            f"(<b>{BEIGE_BAR:.1f}%</b>) and general cards <code>#F4F1ED</code> "
            f"(<b>{BEIGE_CARDS:.1f}%</b>) — the same shares as the dark frame above, which is the "
            f"point of the whole pass.",
        ),
        "before_workout": figure(
            "13_workout_dev_final.png", "Before — My Workout",
            f"The same bug here: the hero cards <code>#1E1E1E</code> but the bar still "
            f"<code>#2C2C2C</code> (<b>{BEFORE_WORKOUT_BAR:.1f}%</b>), so the card that had just "
            f"been darkened sat BELOW the chrome around it — the owner's *\"make the card you "
            f"actually change slightly lighter\"*. Landing the bar on the card's rung instead "
            f"(the next frame) fixed the wash and merged the two into one grey, "
            f"<b>{MID_WORKOUT_MERGED:.1f}%</b> of #1E1E1E \u2014 the card was still not lighter "
            f"than anything.",
        ),
        "after_workout": figure(
            "25_dev_workout.png", "After — My Workout",
            f"Hero cards <code>#1E1E1E</code> (<b>{AFTER_WORKOUT_CARD:.1f}%</b>) and the bar "
            f"<code>#1B1B1B</code> (<b>{AFTER_WORKOUT_BAR:.1f}%</b>) \u2014 two rungs, the card the "
            f"lighter one, against beige's own card <code>#F4F0EB</code> "
            f"(<b>{BEIGE_WORKOUT_CARD:.1f}%</b>) over bar <code>#F0EBE5</code> "
            f"(<b>{BEIGE_WORKOUT_BAR:.1f}%</b>). Same shares, same split.",
        ),
        "rep_before": figure(
            "26_dev_workout_before_rep.png", "Before — the rep card",
            f"The banner inside the hero card — <b>SET 1 OF 4</b>, the big rep count, the load "
            f"and the progress row \u2014 was filled with the CANVAS grey, so it covered "
            f"<b>{REP_HOLE:.1f}%</b> of the frame in <code>#0E0E0E</code>: the same colour as the "
            f"page behind the card. A fill equal to the background is not a recessed block, it is "
            f"a hole, and that is what the owner was reading when he said *\"make the rep card "
            f"slightly lighter\"*.",
        ),
        "rep_after": figure(
            "27_dev_workout_after_rep.png", "After — the rep card",
            f"Filled with <code>#191919</code> (<b>{REP_FILL:.1f}%</b>) — the ladder's own card "
            f"rung, one step under the hero card and the same grey every other card in the app "
            f"wears. The page's own share drops from <b>{REP_BEFORE_PAGE:.1f}%</b> to "
            f"<b>{REP_AFTER_PAGE:.1f}%</b>, which is beige's canvas share too.",
        ),
        "rep_beige": figure(
            "21_beige_workout.png", "Beige — the same block",
            f"Its well is <code>#E7DFD5</code>, <b>{BEIGE_WELL:.1f}%</b> of the frame against a "
            f"canvas at <b>{BEIGE_WELL_PAGE:.1f}%</b> — a block you can see, which is the whole "
            f"of the correction.",
        ),
        "beige_workout": figure(
            "21_beige_workout.png", "Beige — My Workout",
            f"Cards <code>#F4F0EB</code> (<b>{BEIGE_WORKOUT_CARD:.1f}%</b>), bar "
            f"<code>#F0EBE5</code> (<b>{BEIGE_WORKOUT_BAR:.1f}%</b>) — the panel-versus-chrome "
            f"relationship, and the shares, the dark frame now copies.",
        ),
    }

    ladder = "\n".join(
        f"      <tr><td><code>{tok}</code></td><td>{wears}</td>"
        f"<td class='now'><code>{now}</code></td><td><code>{was}</code></td></tr>"
        for tok, rung, now, was, wears in LADDER
    )
    mirror = "\n".join(
        f"      <tr><td>{role}</td><td><code>{bc}</code></td><td>{bs}</td>"
        f"<td><code>{dc}</code></td><td>{ds}</td></tr>"
        for role, bc, bs, dc, ds in MIRROR
    )

    page = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Darker chrome, and the dark ladder mirrored off the beige one — 2026-10-14</title>
<style>
  :root {{ color-scheme: dark; }}
  body {{ margin: 0; padding: 30px; background: #0e0e0e; color: #edebe8;
          font: 15px/1.55 -apple-system, "Segoe UI", Roboto, sans-serif; }}
  h1 {{ font-size: 22px; margin: 0 0 6px; }}
  h2 {{ font-size: 14px; color: #e2b75a; letter-spacing: .09em; text-transform: uppercase;
        margin: 34px 0 10px; }}
  h3 {{ font-size: 12px; color: #e2b75a; letter-spacing: .09em; text-transform: uppercase;
        margin: 0 0 7px; }}
  p {{ margin: 0 0 16px; color: #b8b4ad; max-width: 92ch; }}
  .row {{ display: flex; gap: 22px; flex-wrap: wrap; }}
  figure {{ margin: 0 0 20px; }}
  img {{ width: 268px; border: 1px solid #3d3d3d; border-radius: 14px; display: block; }}
  figcaption {{ margin-top: 9px; font-size: 12.5px; color: #b8b4ad; max-width: 268px; }}
  code {{ background: #1e1e1e; padding: 1px 5px; border-radius: 5px; color: #edebe8;
          font-size: 13px; }}
  .share {{ margin-top: 8px; font-size: 11.5px; color: #8d8a85; max-width: 268px; }}
  .sw {{ display: inline-block; width: 9px; height: 9px; border-radius: 2px; margin: 0 3px 0 7px;
         border: 1px solid #00000066; vertical-align: -1px; }}
  .share .sw:first-child {{ margin-left: 0; }}
  table {{ border-collapse: collapse; font-size: 13.5px; margin: 0 0 10px; }}
  th, td {{ text-align: left; padding: 6px 16px 6px 0; border-bottom: 1px solid #232323; }}
  th {{ color: #8d8a85; font-weight: 600; font-size: 11.5px; letter-spacing: .07em;
        text-transform: uppercase; }}
  td.now code {{ background: #2a2419; color: #e2b75a; }}
  .tag {{ display: inline-block; font-size: 11px; padding: 2px 8px; border-radius: 999px;
          border: 1px solid #e2b75a; color: #e2b75a; margin-left: 8px; vertical-align: 2px;
          letter-spacing: .06em; }}
  .note {{ border-left: 2px solid #e2b75a; padding-left: 14px; }}
</style>
</head>
<body>
  <h1>Darker chrome, and a dark ladder mirrored off the beige one</h1>

  <p class="note"><b>Owner, 2026-10-14:</b> *"the grey of the bottom bar and the general cards
     background color is the one you were supposed to make darker ... and make the card you
     actually change slightly lighter? i want it to have the same visuals as the light beige
     theme."* The two surfaces named first are not cards at all — a Material card and the bottom
     bar are both <code>surface</code> <b>plus Material's tonal elevation tint</b>, and on a dark
     scheme that tint was <code>onSurface</code>, a near-<i>white</i> ink. So both composited to
     <code>#2C2C2C</code>, <b>1.42:1 from the canvas</b> where the beige theme's widest neutral
     step is 1.157:1 — which is exactly why the previous pass, which moved the <i>card</i> rung,
     did not touch the thing being complained about.</p>

  <p>Every frame below is a real capture of the debug package on emulator-5554, and every number
     on this page is sampled from the PNG it sits     under (<code>build_page.py</code> measures them and embeds the frames;
     nothing here is typed).</p>

  <h2>Home — the bottom bar and the general cards<span class="tag">fixed</span></h2>
  <div class="row">
{figures['before']}
{figures['after']}
{figures['beige']}
  </div>

  <h2>My Workout — the card that was changed</h2>
  <div class="row">
{figures['before_workout']}
{figures['after_workout']}
{figures['beige_workout']}
  </div>

  <h2>My Workout \u2014 the rep card inside the hero card<span class="tag">fixed</span></h2>
  <div class="row">
{figures['rep_before']}
{figures['rep_after']}
{figures['rep_beige']}
  </div>

  <h2>What the palette is now</h2>
  <table>
    <tr><th>token</th><th>what wears it</th><th>today</th><th>before</th></tr>
{ladder}
  </table>

  <h2>The dark ladder is the beige theme's steps, mirrored</h2>
  <p>A dark palette is not picked by eye, it is <b>derived from the light one</b>: measure each of
     beige's steps away from its canvas, then choose the grey that reproduces that ratio above the
     dark canvas. Its canvas is the theme's <i>lightest</i> tone, the dark theme's is its darkest,
     so the same picture is the same sizes in the other direction.</p>
  <table>
    <tr><th>role</th><th>beige</th><th>step</th><th>dark</th><th>step</th></tr>
{mirror}
  </table>
  <p><b>One pair is deliberately inverted</b>, and it is the pair the owner named: beige's hero
     card (1.107) is a <i>smaller</i> step than its bar (1.157), so a strict mirror would put the
     dark card <i>below</i> the dark bar. He asked for the opposite — bar and general cards down,
     the changed card up — and he is looking at a screen rather than at a table: a card is a panel
     and the bar is chrome, and the panel has to come out in front. So the bar took a rung of its
     own at <code>#1B1B1B</code> (1.121:1) and the hero card kept 1.158:1, which is the only way
     the card could be <i>lighter</i> than the bar without leaving the whisper band the palette
     allows (1.2:1).</p>

  <p><b>The nested well is the largest departure of all, and it is the one the owner caught.</b>
     Beige's well is 1.297:1 from its canvas — the widest step that theme takes — so a strict
     mirror puts the dark well at <code>#282828</code>, <i>lighter</i> than the hero card, i.e. a
     raised highlight rather than the recessed block it reads as in beige. The first fix went the
     other way and landed it exactly on the canvas (<code>#0E0E0E</code>, 1.000:1) — mirror-correct
     by ratio to the CARD, invisible against the PAGE, and the rep banner read as a hole punched
     through the card. It now takes the ladder's card rung (<code>#191919</code>, 1.098:1): the
     same grey every other card in the app wears, one step under the hero card, and visible in its
     own right. The rule this bought is worth keeping: <b>a step that only exists relative to the
     card has to stay visible against the page as well</b>, and the test now asserts both.</p>

  <p>The proof is that the two appearances now carry the <i>same colour shares</i>: on Home the bar
     is <b>{BEFORE_BAR:.1f}% at #2C2C2C → {AFTER_BAR:.1f}% at #1B1B1B</b> against beige's
     <b>{BEIGE_BAR:.1f}%</b>, and the general cards <b>{AFTER_CARDS:.1f}%</b> against beige's
     <b>{BEIGE_CARDS:.1f}%</b>.</p>

  <p>Pinned by <code>theDarkStepsAreTheLightThemesStepsMirrored</code> and
     <code>theCanvasToCardStepReadsAsAFront</code> in <code>DarkThemeContrastTest</code> (17 tests,
     0 failures), with the reasoning in <code>Color.kt</code> (<code>DarkSurfaceHigh</code>),
     <code>Theme.kt</code> (<code>withThemedContainers</code>) and <code>MainActivity.kt</code>
     (<code>navBarFill</code>).</p>
</body>
</html>
"""
    (HERE / "index.html").write_text(page, encoding="utf-8")
    print(f"wrote {HERE / 'index.html'} ({len(page)} chars)")
    print(f"  bar  #2C2C2C before {BEFORE_BAR:.1f}% -> #1B1B1B after {AFTER_BAR:.1f}% "
          f"(beige {BEIGE_BAR:.1f}%)")
    print(f"  card #1E1E1E {AFTER_WORKOUT_CARD:.1f}% vs bar #1B1B1B {AFTER_WORKOUT_BAR:.1f}% "
          f"(beige card {BEIGE_WORKOUT_CARD:.1f}% / bar {BEIGE_WORKOUT_BAR:.1f}%)")
    print(f"  cards #191919 after {AFTER_CARDS:.1f}% (beige {BEIGE_CARDS:.1f}%)")
    print(f"  rep card #0E0E0E hole {REP_HOLE:.1f}% -> #191919 {REP_FILL:.1f}% "
          f"(beige well {BEIGE_WELL:.1f}%); page {REP_BEFORE_PAGE:.1f}% -> {REP_AFTER_PAGE:.1f}% "
          f"(beige page {BEIGE_WELL_PAGE:.1f}%)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
