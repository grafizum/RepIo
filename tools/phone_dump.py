#!/usr/bin/env python3
"""Dump the attached phone's UI tree as `label  [x1,y1][x2,y2]` lines.

Driving the app by remembered coordinates is how a device walk rots: this prints
what is on the screen right now, so a tap can be aimed at a label instead.

    python3 tools/phone_dump.py <serial> [--grep TEXT]

Compose reports `clickable="false"` for every node, so there is no --clicks view
here: the actionable nodes are found by their label, or by the content description
the component was given (this is how the stage dropdown's arrow is reached - see
`level_stage_dropdown`).

BEFORE EVERY TAP: check `dumpsys window | grep mCurrentFocus` is this app. The
phone belongs to the owner and is usually in his hand; a tap aimed at the app can
land in whatever else is open (it happened once, into Instagram).
"""
from __future__ import annotations

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
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    serial = sys.argv[1]
    grep = None
    if "--grep" in sys.argv:
        grep = sys.argv[sys.argv.index("--grep") + 1].lower()

    raw = dump(serial)
    # uiautomator sometimes returns the trailing "UI hierchary dumped to" line.
    raw = raw[raw.index("<"):]
    root = ET.fromstring(raw)
    for node in root.iter():
        text = node.get("text") or ""
        desc = node.get("content-desc") or ""
        label = text if text.strip() else desc
        if not label.strip():
            continue
        if grep and grep not in label.lower():
            continue
        bounds = re.findall(r"\d+", node.get("bounds", ""))
        if len(bounds) != 4:
            continue
        print(f"{label[:46]:48} [{bounds[0]},{bounds[1]}][{bounds[2]},{bounds[3]}]")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
