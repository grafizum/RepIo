#!/usr/bin/env python3
"""Align the muscleGroup labels of the built-in template rows with the rebuilt
catalog's precise muscles (the stored label should be the authoritative one).
One-shot; run from the repo root:  python tools/exercise-db/fix_labels.py
"""

from __future__ import annotations

import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
cat = {r["name"].lower(): r["muscle"] for r in json.loads(
    (ROOT / "tools/exercise-db/out/catalog.json").read_text(encoding="utf-8"))}

DAYS = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY"


def fix_positional(src: str) -> tuple[str, int]:
    """RoutineExerciseEntity(0, "DAY", "Name", "OldLabel", ...) form."""
    count = 0

    def repl(m: re.Match) -> str:
        nonlocal count
        name, label = m.group(2), m.group(3)
        muscle = cat.get(name.lower())
        if muscle and muscle != label:
            count += 1
            return f'{m.group(1)}"{name}", "{muscle}"'
        return m.group(0)

    src = re.sub(
        rf'((?:{DAYS})", )"([^"]+)", "([^"]+)"',
        repl,
        src,
    )
    return src, count


def fix_named(src: str) -> tuple[str, int]:
    """name = "X", ... muscleGroup = "Y", form (DefaultData)."""
    count = 0

    def repl(m: re.Match) -> str:
        nonlocal count
        name, label = m.group(1), m.group(2)
        muscle = cat.get(name.lower())
        if muscle and muscle != label:
            count += 1
            return f'name = "{name}",\n            muscleGroup = "{muscle}"'
        return m.group(0)

    src = re.sub(r'name = "([^"]+)",\s*\n\s*muscleGroup = "([^"]+)"', repl, src)
    return src, count


for rel in [
    "composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt",
    "composeApp/src/main/java/com/grafizum/repio/data/DefaultData.kt",
]:
    p = ROOT / rel
    src = p.read_text(encoding="utf-8")
    src, n1 = fix_positional(src)
    src, n2 = fix_named(src)
    p.write_text(src, encoding="utf-8")
    print(f"{p.name}: {n1} positional + {n2} named labels realigned")
