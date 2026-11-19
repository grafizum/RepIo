#!/usr/bin/env python3
"""Audit a uiautomator dump for the layout faults a thin phone shows first.

Reads the dump of a real screen and reports, in dp, the things that make a
layout look "off": content columns whose left and right insets disagree, nodes
that run past the window, text squeezed into boxes too small to hold it, and
tap targets below Material's minimum.

Usage: python tools/layout_sweep.py ui.xml --width 1080 --density 420
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET


def bounds(node):
    m = re.match(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", node.get("bounds", ""))
    return tuple(map(int, m.groups())) if m else None


def main() -> int:
    path = sys.argv[1]
    width = 1080
    density = 420
    args = sys.argv[2:]
    for i, a in enumerate(args):
        if a == "--width":
            width = int(args[i + 1])
        if a == "--density":
            density = int(args[i + 1])
    px = density / 160.0

    def dp(v):
        return v / px

    tree = ET.parse(path)
    texts: list[tuple[str, tuple[int, int, int, int], tuple[int, int, int, int] | None]] = []
    clickables: list[tuple[str, tuple[int, int, int, int]]] = []

    def walk(node, parent):
        for child in list(node):
            if child.tag != "node":
                continue
            b = bounds(child)
            if b:
                label = (child.get("text") or "").strip()
                if label:
                    pb = bounds(parent) if parent is not None else None
                    texts.append((label, b, pb))
                if child.get("clickable") == "true" or child.get("long-clickable") == "true":
                    clickables.append(((child.get("text") or child.get("content-desc") or "?").strip(), b))
            walk(child, child)

    walk(tree.getroot(), None)

    print(f"=== {path}  window {width}px @ {density}dpi ({dp(width):.0f}dp wide)")
    print(f"text nodes: {len(texts)}   tappable nodes: {len(clickables)}")

    over = [t for t in texts if t[1][0] < 0 or t[1][2] > width]
    print(f"\n-- past the window edge: {len(over)}")
    for label, b, _ in over[:10]:
        print(f"   {label[:40]:<40} x0={b[0]} x1={b[2]}")

    beyond = [t for t in texts if t[2] and (t[1][0] < t[2][0] or t[1][2] > t[2][2])]
    print(f"\n-- text outside its own container: {len(beyond)}")
    for label, b, pb in beyond[:10]:
        print(f"   {label[:34]:<34} text[{b[0]},{b[2]}] container[{pb[0]},{pb[2]}]")

    tiny = [t for t in texts if (t[1][2] - t[1][0]) > 0 and dp(t[1][2] - t[1][0]) < 6]
    print(f"\n-- squeezed under 6dp wide: {len(tiny)}")

    small = [c for c in clickables if dp(min(c[1][2] - c[1][0], c[1][3] - c[1][1])) < 48]
    print(f"\n-- tap targets under 48dp: {len(small)}")
    for label, b in small[:12]:
        print(f"   {label[:40]:<40} {b[2]-b[0]}x{b[3]-b[1]}px = {dp(b[2]-b[0]):.0f}x{dp(b[3]-b[1]):.0f}dp")

    # The content column: the most common (x0, x1) pair among wide text nodes is
    # what the screen is actually aligning to.
    from collections import Counter

    cols = Counter()
    for label, b, _ in texts:
        if b[2] - b[0] > width * 0.5:
            cols[(b[0], b[2])] += 1
    print("\n-- widest text spans (the screen's own columns):")
    for (x0, x1), n in cols.most_common(6):
        gl, gr = x0, width - 1 - x1
        flag = "  <== asymmetric" if abs(gl - gr) > 2 else ""
        print(
            f"   x {x0:>4}..{x1:<4} ({n:>2} nodes, {dp(x1-x0):.0f}dp) "
            f"insets L{dp(gl):.1f} R{dp(gr):.1f}dp{flag}"
        )

    # Header / title nodes: is anything that should sit centred actually centred?
    print("\n-- text whose centre is not the window centre (informational):")
    for label, b, pb in texts:
        ctr = (b[0] + b[2]) / 2
        if abs(ctr - width / 2) > width * 0.05:
            continue
        print(f"   centred: {label[:40]:<40} insets L{dp(b[0]):.0f} R{dp(width-1-b[2]):.0f}dp")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
