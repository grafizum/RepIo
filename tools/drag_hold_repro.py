#!/usr/bin/env python3
"""Drive a long-press drag on the edit-day list and watch it while the finger HOLDS.

A held finger is the whole point. `input swipe` cannot express the gesture this
app has to survive — it is one continuous move, so the finger is never parked at
a border, and the edge glide (which only runs while the finger rests inside the
scroll band) never starts. `input motionevent` can park it: DOWN, wait out the
long press, walk to the border, then send NOTHING and let the list glide under a
finger that has not moved.

The UI tree is dumped at three moments — on arrival at the band, on each second
of the hold, and after the lift — so the ORDER it prints is the local list order
and the `[x,y]` of the dragged card is its layout slot, not its painted offset.

    python3 tools/drag_hold_repro.py <serial> --x 400 --from 1120 --to 2100 [--hold 4]

BEFORE RUNNING: the app must be on the edit-day screen with enough exercises to
scroll, and `dumpsys window | grep mCurrentFocus` must be this app.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from phone_dump import dump as phone_dump_xml  # noqa: E402


def motion(serial: str, action: str, x: int, y: int) -> None:
    subprocess.run(
        ["adb", "-s", serial, "shell", "input", "motionevent", action, str(x), str(y)],
        check=True,
        capture_output=True,
    )


def snapshot(serial: str, path: Path) -> list[str]:
    """The tree as `label  [x,y][x,y]` lines, the way tools/phone_dump.py prints it."""
    raw = phone_dump_xml(serial)
    raw = raw[raw.index("<"):]
    lines: list[str] = []
    for node in ET.fromstring(raw).iter():
        text = node.get("text") or ""
        desc = node.get("content-desc") or ""
        name = text if text.strip() else desc
        bounds = re.findall(r"\d+", node.get("bounds", ""))
        if name.strip() and len(bounds) == 4:
            lines.append(f"{name[:46]:48} [{bounds[0]},{bounds[1]}][{bounds[2]},{bounds[3]}]")
    path.write_text("\n".join(lines) + "\n")
    return lines


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("serial")
    p.add_argument("--x", type=int, required=True)
    p.add_argument("--from-y", type=int, required=True, dest="y0")
    p.add_argument("--to-y", type=int, required=True, dest="y1")
    p.add_argument("--hold", type=float, default=4.0)
    p.add_argument("--out", default="/tmp/drag_hold")
    p.add_argument("--steps", type=int, default=12)
    args = p.parse_args()

    out = Path(args.out)
    long_press_ms = 1200

    print(f"DOWN at ({args.x},{args.y0}) — holding {long_press_ms}ms for the long press")
    motion(args.serial, "DOWN", args.x, args.y0)
    time.sleep(long_press_ms / 1000)

    for i in range(1, args.steps + 1):
        y = args.y0 + (args.y1 - args.y0) * i // args.steps
        motion(args.serial, "MOVE", args.x, y)
        time.sleep(0.1)

    settled = snapshot(args.serial, out.with_name(out.name + "_0_arrive.txt"))
    print("\n--- on arrival at the band (finger parked from here on) ---")
    print("\n".join(settled[:8]))

    ticks = int(args.hold)
    for t in range(1, ticks + 1):
        time.sleep(1.0)
        now = snapshot(args.serial, out.with_name(f"{out.name}_{t}_hold.txt"))
        print(f"\n--- hold +{t}s (no input events sent) ---")
        print("\n".join(now[:8]))

    motion(args.serial, "UP", args.x, args.y1)
    time.sleep(1.5)
    after = snapshot(args.serial, out.with_name(out.name + "_after_lift.txt"))
    print("\n--- after the lift ---")
    print("\n".join(after[:8]))
    print(f"\ndumps written to {out}_*.txt")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
