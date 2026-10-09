#!/usr/bin/env python3
"""Drive one emulator through every window size class and flag layout defects.

Owner, 2026-10-08: "now make sure these screen adaptations are for all the screen
sizes also for some like a tablet that would be bigger."

`wm size` / `wm density` change the window the app is actually laid out in, so the
same AVD can stand in for a small phone, a tablet in portrait and a tablet in
landscape - no second system image needed. For each configuration the app is
relaunched and every bottom-bar tab is visited; each screen is then checked for the
three defects a UI dump can prove: text painted over text, content crossing its
parent's edge, and a label squeezed narrower than the string needs.

Usage: python3 tools/window_sweep.py [serial]
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

SERIAL = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
PKG = "com.grafizum.repio"

# (name, width px, height px, density dpi, font scale)
CONFIGS = [
    ("small phone 360x640", 720, 1280, 320, "1.0"),
    ("phone 411x914 (S22+)", 1080, 2340, 420, "1.0"),
    ("tablet portrait 800x1280", 1600, 2560, 320, "1.0"),
    ("tablet landscape 1280x800", 2560, 1600, 320, "1.0"),
    ("tablet + large font 1.8", 1600, 2560, 320, "1.8"),
    ("landscape phone 914x411", 2400, 1080, 420, "1.0"),
    ("landscape phone + font 1.8", 2400, 1080, 420, "1.8"),
]

TABS = ["Home", "Workout", "Analytics", "Settings"]


def adb(*args, timeout=60):
    return subprocess.run(["adb", "-s", SERIAL, *args], capture_output=True,
                          text=True, timeout=timeout).stdout


def dump_tree():
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/sweep.xml")
        xml = adb("exec-out", "cat", "/sdcard/sweep.xml")
        if xml.strip().startswith("<?xml"):
            try:
                return ET.fromstring(xml)
            except ET.ParseError:
                pass
        time.sleep(1.5)
    return None


def bounds(node):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    return tuple(int(g) for g in m.groups()) if m else None


def text_nodes(root):
    """Every node with visible text: (element, text, bounds)."""
    out = []
    for n in root.iter("node"):
        t = n.get("text") or ""
        b = bounds(n)
        if t.strip() and b:
            out.append((n, t, b))
    return out


def parent_of(root):
    parent = {}
    for p in root.iter("node"):
        for c in p:
            parent[c] = p
    return parent


def inspect(root):
    """Return (overlaps, escaping, squeezed) descriptions for one screen."""
    nodes = text_nodes(root)
    parent = parent_of(root)
    overlaps, escaping, squeezed = [], [], []
    for i, (_, t1, b1) in enumerate(nodes):
        for _, t2, b2 in nodes[i + 1:]:
            # Vertical overlap of more than a third of the shorter box is real text on text.
            vy = min(b1[3], b2[3]) - max(b1[1], b2[1])
            hx = min(b1[2], b2[2]) - max(b1[0], b2[0])
            if vy > 0.34 * min(b1[3] - b1[1], b2[3] - b2[1]) and hx > 0:
                overlaps.append(f"{t1!r} x {t2!r}")
    for el, t, b in nodes:
        p = parent.get(el)
        pb = bounds(p) if p is not None else None
        if pb and (b[2] > pb[2] + 2 or b[3] > pb[3] + 2):
            escaping.append(f"{t!r} {b} in {pb}")
        # Latin text needs roughly 0.5x its box height per character.
        need = len(t) * (b[3] - b[1]) * 0.5
        if need > 60 and (b[2] - b[0]) < 0.8 * need:
            squeezed.append(f"{t!r} w={b[2]-b[0]} need~{int(need)}")
    return overlaps, escaping, squeezed


def main():
    problems = 0
    for name, w, h, dpi, font in CONFIGS:
        adb("shell", f"wm size {w}x{h}")
        adb("shell", f"wm density {dpi}")
        adb("shell", f"settings put system font_scale {font}")
        time.sleep(2)
        adb("shell", "am", "force-stop", PKG)
        adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
        time.sleep(9)
        root = dump_tree()
        print(f"\n=== {name} (font {font}) ===")
        if root is None:
            print("  !! no dump - app may have crashed")
            problems += 1
            continue
        crashed = "FATAL EXCEPTION" in adb("logcat", "-d", "-t", "400")
        if crashed:
            print("  !! FATAL EXCEPTION in logcat")
            problems += 1
        for tab in TABS:
            nav = None
            for n in root.iter("node"):
                if (n.get("content-desc") or n.get("text")) == tab:
                    nav = bounds(n)
                    if nav:
                        break
            if nav is None:
                print(f"  {tab}: not in the bar (skipped)")
                continue
            adb("shell", "input", "tap", str((nav[0] + nav[2]) // 2), str((nav[1] + nav[3]) // 2))
            time.sleep(4)
            r = dump_tree()
            if r is None:
                print(f"  {tab}: no dump")
                problems += 1
                continue
            texts = text_nodes(r)
            wide = max((b[2] for _, _, b in texts), default=0)
            overlaps, escaping, squeezed = inspect(r)
            flag = "" if not (overlaps or escaping or squeezed) else "  <-- CHECK"
            print(f"  {tab}: {len(texts)} strings, right-most x={wide}, "
                  f"overlap={len(overlaps)} escaping={len(escaping)} squeezed={len(squeezed)}{flag}")
            for o in overlaps[:3]:
                print(f"      overlap: {o}")
            for o in escaping[:3]:
                print(f"      escaping: {o}")
            for o in squeezed[:5]:
                print(f"      squeezed: {o}")
            problems += len(overlaps) + len(escaping)
    print(f"\nTOTAL hard problems (overlap + escaping): {problems}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
