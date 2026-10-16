#!/usr/bin/env python3
"""Measure where the HELD card is actually painted while the finger is still down.

Owner report, 2026-10-15: *"moving an exercise wih the auto scrolling up and down just places
the exercise somewhere along the way even though i didnt lift the finger."* The UI dump cannot
answer that: uiautomator reports every row's LAYOUT slot, and the held card is painted away from
its slot by `translationY` - so a dump of a drag in progress looks identical to a drag that never
moved anything.

The card is painted with `scaleX/scaleY = 1.05f` and `alpha = 0.8f` (RoutineScreen), and those two
are the signature this reads back out of a screenshot: the held row is ~5% WIDER than every
resting row (about 22px a side on a 1080px-wide phone), and its surface is blended with whatever
is behind it, so it is a hair darker than the resting card colour. The tool therefore measures,
for every scan line, how wide the run of non-background pixels is, takes the resting width as the
mode, and reports the band that is wider than that - that band is the held card, and its centre is
where the finger is supposed to be.

    python3 tools/drag_hold_measure.py <serial> --from-y 1080 --to-y 1500 [--x 500] [--steps 12]

The finger is left DOWN when the tool returns (that is the state being measured); the caller sends
the lift itself - the tool prints the exact command. Rows must be tall enough to see: the routine
editor's own list, not the workout card.
"""
from __future__ import annotations

import argparse
import collections
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from inspect_shot import load_png  # noqa: E402


def motion(serial: str, action: str, x: int, y: int) -> None:
    subprocess.run(
        ["adb", "-s", serial, "shell", "input", "motionevent", action, str(x), str(y)],
        check=True,
        capture_output=True,
    )


def shot(serial: str, path: Path) -> None:
    with path.open("wb") as fh:
        subprocess.run(["adb", "-s", serial, "exec-out", "screencap", "-p"], stdout=fh, check=True)


def differing(a: tuple[int, int, int], b: tuple[int, int, int], tol: int = 5) -> bool:
    return max(abs(a[0] - b[0]), abs(a[1] - b[1]), abs(a[2] - b[2])) > tol


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("serial")
    p.add_argument("--x", type=int, default=500)
    p.add_argument("--from-y", type=int, required=True, dest="y0")
    p.add_argument("--to-y", type=int, required=True, dest="y1")
    p.add_argument("--steps", type=int, default=12)
    p.add_argument("--long-press-ms", type=int, default=1200)
    p.add_argument(
        "--waits",
        default="0.8",
        help="comma-separated seconds to park the finger for before each measurement; a "
             "second, later entry measures the same card after the list has GLIDED under it",
    )
    p.add_argument("--out", default="/tmp/drag_hold_measure.png")
    p.add_argument("--tol", type=int, default=5)
    args = p.parse_args()

    out = Path(args.out)
    print(f"DOWN at ({args.x},{args.y0}), holding {args.long_press_ms}ms for the long press")
    motion(args.serial, "DOWN", args.x, args.y0)
    time.sleep(args.long_press_ms / 1000)
    for i in range(1, args.steps + 1):
        y = args.y0 + (args.y1 - args.y0) * i // args.steps
        motion(args.serial, "MOVE", args.x, y)
        time.sleep(0.12)

    for index, wait in enumerate(w for w in args.waits.split(",") if w):
        # A parked finger sends no events, so this wait is exactly the state the glide loop sees.
        time.sleep(float(wait))
        path = out if index == 0 else out.with_name(
            out.stem + f"_parked{index}" + out.suffix
        )
        picture = measure(args.serial, path, args.tol)
        print(f"\n--- measured {wait}s after the last move (finger parked) ---" if index == 0
              else f"\n--- measured {wait}s later, finger STILL parked, no events sent ---")
        report(picture, args, path)

    print(f"\nfinger is still down at ({args.x},{args.y1}); lift it with: "
          f"adb -s {args.serial} shell input motionevent UP {args.x} {args.y1}")
    return 0


def measure(serial: str, path: Path, tol: int) -> dict:
    shot(serial, path)
    w, h, rgb = load_png(str(path))
    # The app background is whatever the header (above the list) is made of.
    header = collections.Counter(rgb(x, y) for y in range(10, 200, 7) for x in range(0, w, 7))
    background = header.most_common(1)[0][0]

    widths: dict[int, tuple[int, int, int]] = {}
    for y in range(220, h - 240, 2):
        left = right = None
        for x in range(0, w, 2):
            if differing(rgb(x, y), background, tol):
                left = x
                break
        for x in range(w - 1, -1, -2):
            if differing(rgb(x, y), background, tol):
                right = x
                break
        if left is None or right is None:
            continue
        widths[y] = (left, right, right - left)

    if not widths:
        return {"empty": True, "w": w, "h": h, "rgb": rgb, "widths": widths}

    resting = collections.Counter(wd for _, _, wd in widths.values()).most_common(1)[0][0]
    # A ROW, not a bar: the held card is wider than a resting row but still has the list's own
    # margins, so the full-bleed things under the list - the Add Exercise card, the bottom nav -
    # are out. Without that bound the widest band on screen is the nav bar and the answer is
    # always "left behind".
    wide = {y: v for y, v in widths.items() if resting + 8 < v[2] < w - 15}

    if not wide:
        return {"empty": False, "held": False, "w": w, "h": h, "rgb": rgb,
                "widths": widths, "resting": resting, "background": background}

    # The longest contiguous run of wider-than-resting rows is the held card (a shorter run is the
    # rounded top or bottom of a card edge measured mid-round).
    runs: list[list[int]] = []
    for y in sorted(wide):
        if runs and y - runs[-1][-1] <= 2:
            runs[-1].append(y)
        else:
            runs.append([y])
    band = max(runs, key=len)
    resting_colour = collections.Counter(
        rgb(540, y) for y in range(200, h - 240, 4) if widths.get(y, (0, 0, resting))[2] == resting
    ).most_common(1)[0][0]
    return {
        "empty": False,
        "held": True,
        "w": w,
        "h": h,
        "rgb": rgb,
        "widths": widths,
        "resting": resting,
        "background": background,
        "band": (band[0], band[-1]),
        "centre": (band[0] + band[-1]) // 2,
        "widest": max(widths[y][2] for y in band),
        "held_colour": rgb(540, (band[0] + band[-1]) // 2),
        "resting_colour": resting_colour,
    }


def report(picture: dict, args, path: Path) -> None:
    if picture["empty"]:
        print("nothing drawn between the header and the bottom bar - wrong screen?")
        return
    background = picture["background"]
    resting = picture["resting"]
    print(f"background #{background[0]:02X}{background[1]:02X}{background[2]:02X}, "
          f"resting row width {resting}px  ({path})")
    if not picture["held"]:
        print("NO row is wider than a resting one: nothing is painted at the 1.05 scale, "
              "so no card is being held")
        return
    top, bottom = picture["band"]
    centre, widest = picture["centre"], picture["widest"]
    widths = picture["widths"]
    print(f"held card painted rows {top}..{bottom} (centre {centre}), widest {widest}px "
          f"vs {resting}px resting (+{widest - resting}px)")
    for probe in (top + 4, centre, bottom - 4):
        if probe in widths:
            left, right, wd = widths[probe]
            mid = picture["rgb"]((left + right) // 2, probe)
            print(f"  y={probe:5d}  [{left},{right}]  {wd}px  mid #{mid[0]:02X}{mid[1]:02X}{mid[2]:02X}")
    # The held card's own surface is 0.8 alpha over what is behind it, so it must NOT read the
    # same as a resting row - that blend is a second, colour-based signature of the same card.
    held = picture["held_colour"]
    rest = picture["resting_colour"]
    print(f"held surface #{held[0]:02X}{held[1]:02X}{held[2]:02X} vs "
          f"resting #{rest[0]:02X}{rest[1]:02X}{rest[2]:02X}"
          f" ({'blended' if held != rest else 'IDENTICAL - not the held card'})")
    drift = abs(centre - args.y1)
    print(f"finger y={args.y1}, painted centre y={centre}, drift {drift}px")
    print("under the finger" if drift <= 120 else "LEFT BEHIND - the card is not under the finger")


if __name__ == "__main__":
    raise SystemExit(main())
