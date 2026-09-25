#!/usr/bin/env python3
"""One-shot fix: apply the legacy->catalog renames with PLAIN str.replace
(the splice's re.escape-in-replace was a silent no-op). Run from repo root:
    python tools/exercise-db/replace_names.py
"""

from __future__ import annotations

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]

RENAME = {
    "Barbell Rows": "Bent Over Barbell Row",
    "Dumbbell Rows": "Bent-Over Dumbbell Row",
    "Dumbbell Pullovers": "Dumbbell Pull-Over",
    "Dumbbell Curls": "Dumbbell Curl",
    "Dumbbell Hammer Curls": "Dumbbell Hammer Curl",
    "Dumbbell Shoulder Presses": "Dumbbell Shoulder Press",
    "Dumbbell Lateral Raises": "Dumbbell Lateral Raise",
    "Barbell Bench Presses": "Barbell Bench Press",
    "Barbell Curls": "Barbell Curl",
    "Leg Raises": "Hanging Leg Raise",
    "Barbell Squats": "Barbell Squat",
    "Barbell Romanian Deadlifts": "Romanian Deadlift",
    "Planks": "Plank",
    "Dumbbell Overhead Extensions": "Standing Dumbbell Triceps Extension",
    "Barbell Floor Presses": "Barbell Bench Press",
}

for rel in [
    "composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt",
    "composeApp/src/main/java/com/grafizum/repio/data/DefaultData.kt",
]:
    p = ROOT / rel
    src = p.read_text(encoding="utf-8")
    total = 0
    for old, new in RENAME.items():
        needle = f'"{old}"'
        n = src.count(needle)
        if n:
            src = src.replace(needle, f'"{new}"')
            total += n
            print(f"  {p.name}: {n}x \"{old}\" -> \"{new}\"")
    leftovers = [old for old in RENAME if f'"{old}"' in src]
    p.write_text(src, encoding="utf-8")
    print(f"{p.name}: {total} renamed, leftovers: {leftovers}")
