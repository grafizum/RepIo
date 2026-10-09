#!/usr/bin/env python3
"""Find nodes that overlap without one containing the other.

A layout "looks off" most often because two things that should sit apart are
drawn over each other — an icon over its own label, a chip over a heading, a
floating button over the last row. Those are geometric facts, so they can be
read out of a uiautomator dump without looking at the screen.

Broken out below the window edge as well, since an element that extends past the
screen is usually the same mistake seen from the other side.

Usage: python tools/find_overlaps.py ui.xml [--min-area 2500]
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
    min_area = 2500
    args = sys.argv[2:]
    for i, a in enumerate(args):
        if a == "--min-area":
            min_area = int(args[i + 1])

    tree = ET.parse(path)
    nodes: list[tuple[str, tuple[int, int, int, int], list[int]]] = []

    def walk(node, chain):
        for child in list(node):
            if child.tag != "node":
                continue
            b = bounds(child)
            if b:
                label = (child.get("text") or "").strip() or (child.get("content-desc") or "").strip()
                ident = len(nodes)
                nodes.append((label or f"<{child.get('class','').split('.')[-1]}>", b, chain + [ident]))
                walk(child, chain + [ident])
            else:
                walk(child, chain)

    def walk_container(node, chain):
        for child in list(node):
            if child.tag != "node":
                continue
            b = bounds(child)
            if b:
                label = (child.get("text") or "").strip() or (child.get("content-desc") or "").strip()
                ident = len(nodes)
                nodes.append((label or f"<{child.get('class','').split('.')[-1]}>", b, chain + [ident]))
                walk_container(child, chain + [ident])
            else:
                walk_container(child, chain)

    walk_container(tree.getroot(), [])

    def related(a, b):
        # Ancestor/descendant pairs are allowed to overlap: that is what nesting
        # IS. The chains are lists of ids, so this is a set comparison, not
        # `a in b` - which would test for the list itself as an element.
        sa, sb = set(a), set(b)
        return sa <= sb or sb <= sa

    def inter_area(p, q):
        x0, y0 = max(p[0], q[0]), max(p[1], q[1])
        x1, y1 = min(p[2], q[2]), min(p[3], q[3])
        return 0 if x1 <= x0 or y1 <= y0 else (x1 - x0) * (y1 - y0)

    print(f"=== {path}: {len(nodes)} nodes")
    hits = 0
    for i in range(len(nodes)):
        li, bi, ci = nodes[i]
        for j in range(i + 1, len(nodes)):
            lj, bj, cj = nodes[j]
            if related(ci, cj):
                continue
            a = inter_area(bi, bj)
            if a < min_area:
                continue
            hits += 1
            print(
                f"  overlap {a:>7}px²  {li[:30]:<30} {bi}  <->  {lj[:30]:<30} {bj}"
            )
            if hits >= 40:
                print("  ...")
                return 0
    if hits == 0:
        print("  no overlapping non-related nodes")

    print("\n-- nodes extending past the window edge")
    # Width is taken from the root's own bounds.
    root = tree.getroot().find("node")
    rw = bounds(root)[2] if root is not None and bounds(root) else 1080
    for label, b, _ in nodes:
        if b[0] < 0 or b[2] > rw:
            print(f"  {label[:40]:<40} {b}  (window 0..{rw})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
