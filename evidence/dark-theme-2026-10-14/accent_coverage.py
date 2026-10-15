#!/usr/bin/env python3
"""Measure how much of a screen is the theme ACCENT, and where a given grey sits.

The 2026-10-14 dark-theme pass was decided on numbers from a device, not on taste
words, so the numbers live next to the captures they came from.

Usage:
    python3 accent_coverage.py shot.png                # dominant colours + accent share
    python3 accent_coverage.py shot.png --colour 323035   # exact-pixel count + bands
"""
from __future__ import annotations

import collections
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from inspect_shot import load_png  # noqa: E402


def parse(path: str, step: int = 2):
    w, h, rgb = load_png(path)
    counts: collections.Counter = collections.Counter()
    for y in range(0, h, step):
        for x in range(0, w, step):
            counts[rgb(x, y)] += 1
    return w, h, rgb, counts


def hued(c) -> bool:
    """A colour with a channel spread of 8/255 or more carries a hue; greys do not."""
    return max(c) - min(c) >= 8


def main() -> int:
    path = sys.argv[1]
    args = sys.argv[2:]
    w, h, rgb, counts = parse(path)
    total = sum(counts.values())
    print(f"{Path(path).name}: {w}x{h}")

    if "--colour" in args:
        want = args[args.index("--colour") + 1].lstrip("#")
        target = tuple(int(want[i : i + 2], 16) for i in (0, 2, 4))
        exact = sum(1 for y in range(0, h, 2) for x in range(0, w, 2) if rgb(x, y) == target)
        print(f"  exact #{want.upper()}: {100.0 * exact / total:5.2f}% of the frame")
        bands, run = [], None
        for y in range(h):
            n = sum(
                1 for x in range(0, w, 2) if rgb(x, y) == target
            )
            if n > 3:
                run = [y, y, n] if run is None else [run[0], y, run[2] + n]
            else:
                if run and run[1] - run[0] > 3:
                    bands.append(run)
                run = None
        if run and run[1] - run[0] > 3:
            bands.append(run)
        for b in bands:
            share = 100.0 * b[2] * 2 / (w * h)
            print(f"    band y {b[0]:>4}-{b[1]:<4}  ~{share:5.2f}% of the frame")
        return 0

    for c, n in counts.most_common(12):
        tag = "HUE" if hued(c) else "   "
        print(f"  {tag} #{c[0]:02X}{c[1]:02X}{c[2]:02X} {100.0 * n / total:5.1f}%")
    accent = sum(n for c, n in counts.items() if hued(c))
    print(f"  any-hue share: {100.0 * accent / total:.1f}% of the frame")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
