#!/usr/bin/env python3
"""Build the exercise-i18n extract from the app's own English source.

The English exercise pool (names + equipment + primary muscle) lives in
`WorkoutTemplates.kt`, and the English method descriptions live in
`ExerciseDescriptions.kt`. This script joins them into one table keyed by a
stable slug, which is what every locale's `exn_<slug>` / `exd_<slug>` string
resource is named after.

Run from the repository root:

    python tools/exercise-i18n/extract.py

Outputs `tools/exercise-i18n/extract.tsv` (slug, English name, English
description) and reports any pool exercise that has no description, plus any
description that no longer belongs to the pool.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
KOTLIN = ROOT / "composeApp/src/main/java/com/grafizum/repio"
TEMPLATES = KOTLIN / "data/WorkoutTemplates.kt"
DESCRIPTIONS = KOTLIN / "ui/components/ExerciseDescriptions.kt"
OUT = pathlib.Path(__file__).resolve().parent / "extract.tsv"


def slugify(name: str) -> str:
    """Deterministic resource-key slug: letters/digits kept, runs of anything
    else folded to a single underscore. The apostrophe in "Farmer's Walks"
    therefore disappears rather than becoming a stray separator."""
    slug = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    return re.sub(r"_+", "_", slug)


def pool_names() -> list[str]:
    src = TEMPLATES.read_text(encoding="utf-8")
    start = src.index("private val exerciseEquipment: Map<String, String>")
    end = src.index("getEquipmentTableNames", start)
    literals = re.findall(r'"([^"]+)"', src[start:end])
    equipment = {"All", "Bodyweight", "Dumbbell", "Barbell", "Gym"}
    names, seen = [], set()
    for literal in literals:
        if literal in equipment or literal in seen:
            continue
        seen.add(literal)
        names.append(literal)
    return names


def descriptions() -> dict[str, str]:
    src = DESCRIPTIONS.read_text(encoding="utf-8")
    body = src[src.index("EXERCISE_DESCRIPTIONS") :]
    found: dict[str, str] = {}
    for key, value in re.findall(r'^\s*"([^"]+)"\s+to\s+"((?:[^"\\]|\\.)*)"', body, re.M):
        found[key.lower()] = value.replace('\\"', '"')
    return found


def main() -> int:
    names = pool_names()
    desc = descriptions()

    slugs: dict[str, str] = {}
    collisions: list[tuple[str, str, str]] = []
    for name in names:
        slug = slugify(name)
        if slug in slugs:
            collisions.append((slug, slugs[slug], name))
        slugs[slug] = name

    rows = [(slugify(n), n, desc.get(n.lower(), "")) for n in names]
    missing = [n for n in names if not desc.get(n.lower())]
    orphans = sorted(set(desc) - {n.lower() for n in names})

    OUT.write_text(
        "".join(f"{slug}\t{name}\t{text}\n" for slug, name, text in rows),
        encoding="utf-8",
    )

    print(f"pool exercises      : {len(names)}")
    print(f"description entries : {len(desc)}")
    print(f"rows written        : {len(rows)} -> {OUT.relative_to(ROOT)}")
    if collisions:
        print(f"\nSLUG COLLISIONS ({len(collisions)}):")
        for slug, first, second in collisions:
            print(f"  {slug}: {first!r} vs {second!r}")
    else:
        print("slug collisions     : none")
    if missing:
        print(f"\nPOOL EXERCISES WITHOUT A DESCRIPTION ({len(missing)}):")
        for name in missing:
            print(f"  {name}")
    else:
        print("pool coverage       : every exercise has a description")
    if orphans:
        print(f"\nDESCRIPTIONS NOT IN THE POOL ({len(orphans)}):")
        for name in orphans:
            print(f"  {name}")

    return 1 if (collisions or missing) else 0


if __name__ == "__main__":
    sys.exit(main())
