#!/usr/bin/env python3
"""Tap a node in a UI dump by its text or content description.

Tapping by hard-coded coordinates is the reason a device walk has to be redone
after every layout change: the card the script aimed at is 40px taller and the
tap lands on the next one. This resolves the label to whatever bounds it has
right now.

Usage: python3 tools/ui_tap.py <serial> <label> [--desc] [--nth N] [--dump]
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


def main() -> int:
    serial = sys.argv[1]
    label = sys.argv[2]
    desc_only = "--desc" in sys.argv
    nth = 0
    if "--nth" in sys.argv:
        nth = int(sys.argv[sys.argv.index("--nth") + 1])

    root = ET.fromstring(dump(serial))
    hits = []
    for node in root.iter("node"):
        text = (node.attrib.get("text") or "").strip()
        desc = (node.attrib.get("content-desc") or "").strip()
        candidate = desc if desc_only else text
        if candidate != label:
            continue
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib["bounds"])
        l, t, r, b = (int(g) for g in m.groups())
        hits.append((l, t, r, b))
    if len(hits) <= nth:
        print(f"no node '{label}' (found {len(hits)})")
        return 1
    l, t, r, b = hits[nth]
    x, y = (l + r) // 2, (t + b) // 2
    print(f"tap '{label}' #{nth} at {x},{y} bounds=[{l},{t}][{r},{b}]")
    subprocess.run(["adb", "-s", serial, "shell", "input", "tap", str(x), str(y)], check=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
