#!/usr/bin/env python3
"""Re-key the de/it translation tables after the name generalization.

The tables are keyed by the slug of the canonical English name, so every
exercise that was renamed now sits under a slug the pool no longer uses - and
the localization test that guards "no entry is outside the pool" fails on each
one. This moves those keys onto the new slug.

The translated NAME and description are left exactly as they are. They are
already correct German and Italian for those movements, and the apparatus sits
in a different place in each language ("Bankdruecken mit Langhantel", "Panca
con bilanciere"); rewriting them is a translation job, not a mechanical one, so
it is left to the translation workstream rather than guessed at here.

    python tools/exercise-db/rekey_translation_tables.py --check
    python tools/exercise-db/rekey_translation_tables.py
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RENAMES = ROOT / "tools/exercise-db/out/name_renames.json"
CATALOG = ROOT / "tools/exercise-db/out/catalog_v5.json"
TABLES = [
    ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseTextDe.kt",
    ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseTextIt.kt",
]


def slugify(name: str) -> str:
    """The same rule as ExerciseLocalization.slugify."""
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def main() -> int:
    check = "--check" in sys.argv
    renames: dict[str, str] = json.loads(RENAMES.read_text(encoding="utf-8"))
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    pool_slugs = {slugify(e["name"]) for e in catalog}

    # Two different display names can still slugify to the same string
    # ("Figure-4 Stretch" and "Figure 4 Stretch"), and the translation tables
    # are keyed by slug, so that has to be impossible before re-keying.
    owners: dict[str, list[str]] = {}
    for e in catalog:
        owners.setdefault(slugify(e["name"]), []).append(e["name"])
    clashing = {s: ns for s, ns in owners.items() if len(ns) > 1}
    if clashing:
        for slug, names in sorted(clashing.items()):
            print(f"  SLUG CLASH {slug}: {names}")
        print("cannot re-key translation tables until the names are disambiguated")
        return 1

    # old canonical name -> new slug
    slug_moves = {slugify(old): slugify(new) for old, new in renames.items()}

    total = 0
    for table in TABLES:
        text = original = table.read_text(encoding="utf-8")
        hits = 0
        for old_slug, new_slug in sorted(slug_moves.items()):
            needle = f'"{old_slug}" to ex('
            if needle in text:
                hits += text.count(needle)
                text = text.replace(needle, f'"{new_slug}" to ex(')
        if text != original:
            total += hits
            print(f"  {hits:3d}  {table.relative_to(ROOT)}")
            if not check:
                table.write_text(text, encoding="utf-8")
    verb = "would re-key" if check else "re-keyed"
    print(f"{verb} {total} translation entr(ies)")

    # Report, for the record, what is still untranslated after the rename.
    for table in TABLES:
        keys = set(re.findall(r'^\s*"([a-z0-9_]+)" to ex\(', table.read_text(encoding="utf-8"), re.M))
        stale = sorted(k for k in keys if k not in pool_slugs)
        print(f"  {table.name}: {len(keys)} entries, {len(stale)} not in the pool")
        for k in stale[:10]:
            print(f"      stale key: {k}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
