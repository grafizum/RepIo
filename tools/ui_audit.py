#!/usr/bin/env python3
"""Find layout defects a UI dump can actually prove.

Owner, 2026-10-08 (screenshot of the 1.8x emulator in hand): "the interface is
still thin its not only about a theme or one button its about the general space
formatting depending on each device that needs to be remade."

What the screenshot shows and this tool measures:

  TRUNCATED  text the layout cut off. uiautomator cannot see an ellipsis (Compose
             reports the source string), so this estimates the width the string
             needs from the node's own height and flags anything much narrower.
             Rough by construction - the point is to rank offenders, not to be
             exact.
  OVERLAP    two text boxes that actually intersect. That is a real defect: text
             painted on top of other text (a floating bar over the content, a
             badge over its own chip).
  OVERSIZED  a text box wider or taller than the scrollable parent that holds it,
             i.e. content the user cannot reach.
  ESCAPING   a text box that crosses its parent's right/bottom edge.

Usage: python3 tools/ui_audit.py <serial> [--min-gap 8] [--truncation 0.8]
"""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def dump(serial: str) -> str:
    subprocess.run(
        ["adb", "-s", serial, "shell", "uiautomator", "dump", "/sdcard/ui.xml"],
        check=True,
        capture_output=True,
    )
    return subprocess.run(
        ["adb", "-s", serial, "exec-out", "cat", "/sdcard/ui.xml"],
        check=True,
        capture_output=True,
        text=True,
    ).stdout


def bounds(node):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib["bounds"])
    l, t, r, b = (int(g) for g in m.groups())
    return l, t, r, b


def walk(root):
    """Every node with its parent, by index order."""
    out = []

    def rec(node, parent):
        out.append((node, parent))
        for child in node:
            rec(child, node)

    rec(root, None)
    return out


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    serial = args[0] if args else "emulator-5554"
    min_gap = 8
    truncation = 0.8
    for a in sys.argv[1:]:
        if a.startswith("--min-gap"):
            min_gap = int(a.split("=", 1)[1])
        if a.startswith("--truncation"):
            truncation = float(a.split("=", 1)[1])

    root = ET.fromstring(dump(serial))
    pairs = walk(root)
    nodes = []
    for node, parent in pairs:
        text = (node.attrib.get("text") or "").strip()
        desc = (node.attrib.get("content-desc") or "").strip()
        label = text or ("<desc>" + desc + "</desc>" if desc else "")
        if not label:
            continue
        l, t, r, b = bounds(node)
        if r <= l or b <= t:
            continue
        nodes.append({
            "label": label, "text": text, "l": l, "t": t, "r": r, "b": b,
            "node": node, "parent": parent,
        })

    print(f"=== {len(nodes)} text nodes, {len(pairs)} nodes total ===")
    for n in sorted(nodes, key=lambda n: (n["t"], n["l"])):
        print(f"  [{n['l']:>4},{n['t']:>4}][{n['r']:>4},{n['b']:>4}] "
              f"w={n['r']-n['l']:>4} h={n['b']-n['t']:>4} {n['text'][:64]!r}")

    # ---- TRUNCATED -------------------------------------------------------------
    #
    # A truncated Compose Text still reports its SOURCE string to accessibility, so
    # the ellipsis itself is invisible here. What is not invisible: a truncated box
    # is filled to the edge of its parent, exactly. That conjunction - the box is as
    # wide as the space it was given, and the glyphs it holds would want more - is
    # the signal. Width alone would flag every paragraph, so the parent's own width
    # is the reference.
    print(f"\n=== TRUNCATED (fills its parent AND < {truncation:.0%} of what the text needs) ===")
    worst = []
    for n in nodes:
        text = n["text"]
        parent = n["parent"]
        if not text or len(text) < 4 or parent is None or len(text) > 40:
            continue
        pl, pt, pr, pb = bounds(parent)
        available = pr - pl
        have = n["r"] - n["l"]
        if available <= 0 or have < available * 0.94:
            continue
        h = n["b"] - n["t"]
        font_px = h / 1.35  # one line's box is ~1.35 * the font size
        needed = len(text) * font_px * 0.52
        if have < needed * truncation:
            worst.append((needed - have, n, needed, have, font_px))
    for _, n, needed, have, font_px in sorted(worst, reverse=True, key=lambda x: x[0]):
        print(f"  needs~{needed:>5.0f}px  has {have:>5}px  h={n['b']-n['t']:>3} "
              f"(~{font_px:>4.1f}px font)  {n['text'][:58]!r}")
    if not worst:
        print("  none")

    # ---- OVERLAP ---------------------------------------------------------------
    print("\n=== OVERLAP (text boxes that intersect) ===")
    overs = []
    for i, a in enumerate(nodes):
        for b in nodes[i + 1:]:
            ix = min(a["r"], b["r"]) - max(a["l"], b["l"])
            iy = min(a["b"], b["b"]) - max(a["t"], b["t"])
            if ix <= 1 or iy <= 1:
                continue
            # Small intersections are font-box bleed; large ones are painted on top.
            small = min(a["r"] - a["l"], b["r"] - b["l"]) * min(a["b"] - a["t"], b["b"] - b["t"])
            if ix * iy < 0.25 * small:
                continue
            overs.append((ix * iy, a, b, ix, iy))
    for _, a, b, ix, iy in sorted(overs, reverse=True, key=lambda x: x[0]):
        print(f"  {ix:>4}x{iy:<4} {a['text'][:34]!r}  ON  {b['text'][:34]!r}")
    if not overs:
        print("  none")

    # ---- ESCAPING / OVERSIZED --------------------------------------------------
    print("\n=== ESCAPING its scrolling parent ===")
    escapes = []
    for n in nodes:
        parent = n["parent"]
        while parent is not None and parent.attrib.get("scrollable") != "true":
            parent = next((p for nn, p in pairs if nn is parent), None)
        if parent is None:
            continue
        pl, pt, pr, pb = bounds(parent)
        dx = n["r"] - pr
        dy = n["b"] - pb
        if dx > 2 or dy > 2:
            escapes.append((max(dx, dy), n, parent))
    for _, n, parent in sorted(escapes, reverse=True, key=lambda x: x[0]):
        pl, pt, pr, pb = bounds(parent)
        print(f"  text {n['r']},{n['b']} beyond parent {pr},{pb}  "
              f"{n['text'][:44]!r}")
    if not escapes:
        print("  none")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
