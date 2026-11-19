#!/usr/bin/env python3
"""Build the exercise-i18n extract from the app's own English source.

The English exercise pool — canonical name, equipment, primary muscle and the
method description — all live in the generated `ExerciseCatalog.kt`, which is
rebuilt from the v5 progression database. (`ExerciseDescriptions.kt` is only a
thin delegate to it now, so it is no longer a source to parse.) This script
reads that one table and joins it with the slug every locale's
`exn_<slug>` / `exd_<slug>` string resource is named after.

Run from the repository root:

    python tools/exercise-i18n/extract.py

Outputs `tools/exercise-i18n/extract.tsv` (slug, English name, English
description) and reports any pool exercise that has no description, plus any
slug collision the naming scheme would produce.
"""

from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
KOTLIN = ROOT / "composeApp/src/main/java/com/grafizum/repio"
CATALOG = KOTLIN / "data/ExerciseCatalog.kt"
OUT = pathlib.Path(__file__).resolve().parent / "extract.tsv"

# ExerciseCatalogEntry(catalogSlug, name, primaryMuscle, equipment, pattern, type, description)
ENTRY = re.compile(
    r'ExerciseCatalogEntry\(\s*"([^"]*)",\s*"([^"]*)",\s*"[^"]*",\s*"[^"]*",\s*"[^"]*",\s*"[^"]*",\s*"((?:[^"\\]|\\.)*)"',
    re.M,
)


def slugify(name: str) -> str:
    """Deterministic resource-key slug: letters/digits kept, runs of anything
    else folded to a single underscore. The apostrophe in "Farmer's Walks"
    therefore disappears rather than becoming a stray separator."""
    slug = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    return re.sub(r"_+", "_", slug)


def catalog() -> list[tuple[str, str, str]]:
    """(canonical name, description, catalog slug) for every pool exercise."""
    src = CATALOG.read_text(encoding="utf-8")
    rows = []
    for catalog_slug, name, description in ENTRY.findall(src):
        rows.append((name, description.replace('\\"', '"'), catalog_slug))
    return rows


def main() -> int:
    entries = catalog()
    names = [name for name, _, _ in entries]
    desc = {name.lower(): text for name, text, _ in entries}
    slugs: dict[str, str] = {}
    collisions: list[tuple[str, str, str]] = []
    for name in names:
        slug = slugify(name)
        if slug in slugs:
            collisions.append((slug, slugs[slug], name))
        slugs[slug] = name

    rows = [(slugify(n), n, desc.get(n.lower(), "")) for n in names]
    missing = [n for n in names if not desc.get(n.lower())]

    OUT.write_text(
        "".join(f"{slug}\t{name}\t{text}\n" for slug, name, text in rows),
        encoding="utf-8",
    )

    print(f"pool exercises      : {len(names)}")
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

    return 1 if (collisions or missing) else 0


if __name__ == "__main__":
    sys.exit(main())
