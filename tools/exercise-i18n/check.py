#!/usr/bin/env python3
"""Report duplicate names or descriptions inside the exercise translation tables.

The English pool guarantees one description per movement ("Dumbbell Curls" and
"Concentration Curls" must not read the same), and `ExerciseLocalizationTest` holds the
translations to the same rule. This script is the quick way to see *which* entries clash
while writing them.

Run from the repository root:

    python tools/exercise-i18n/check.py
"""

from __future__ import annotations

import collections
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
ENTRY = re.compile(
    r'"([a-z0-9_]+)"\s+to\s+ex\(\s*\n\s*"([^"]*)",\s*\n\s*"((?:[^"\\]|\\.)*)"\s*\n\s*\)',
    re.M,
)
POOL = re.compile(r'"([A-Z][^"]*)"')

TABLES = {
    "de": ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseTextDe.kt",
    "it": ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseTextIt.kt",
}

failures = 0
for language, path in TABLES.items():
    source = path.read_text(encoding="utf-8")
    rows = ENTRY.findall(source)
    print(f"== {language}: {len(rows)} entries")

    by_description = collections.defaultdict(list)
    by_name = collections.defaultdict(list)
    for slug, name, description in rows:
        by_description[description].append(slug)
        by_name[name.lower()].append(slug)

    for description, slugs in by_description.items():
        if len(slugs) > 1:
            failures += 1
            print(f"  DUP DESCRIPTION: {slugs}")
            print(f"    {description[:90]}...")

    for name, slugs in by_name.items():
        if len(slugs) > 1:
            failures += 1
            print(f"  DUP NAME: {name!r} -> {slugs}")

print("no duplicates" if failures == 0 else f"{failures} problem(s)")
sys.exit(1 if failures else 0)
