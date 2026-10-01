#!/usr/bin/env python3
"""Rewrite pre-generalization exercise names in the Kotlin sources.

The rename in generalize.py changes what the pool calls an exercise, and the
name is a join key all over the app: the equipment overrides, the substitution
chains, the default routine and the built-in templates all spell exercises out
as string literals. This rewrites those literals from the generated rename map
so nothing is left pointing at a name the pool no longer has.

Only WHOLE string literals are touched. "Barbell Squat" is rewritten, but the
word never matches inside "Barbell Back Squat", so a name is never half
replaced. Run with --check to report without writing.

    python tools/exercise-db/apply_rename_to_sources.py --check
    python tools/exercise-db/apply_rename_to_sources.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RENAMES = ROOT / "tools/exercise-db/out/name_renames.json"
KOTLIN_ROOT = ROOT / "composeApp/src/main/java/com/grafizum/repio"
TEST_ROOT = ROOT / "composeApp/src/test/java/com/grafizum/repio"

# Only files that spell pool exercises out as literals. The GENERATED files are
# skipped on purpose: they were just re-emitted from the renamed catalog and the
# renamed progression payload, so hand-editing them would be undone by the next
# pipeline run.
TARGET_DIRS = [KOTLIN_ROOT, TEST_ROOT]
SKIP_NAMES = {"ExerciseCatalog.kt", "ExerciseProgression.kt", "ProgressionTables.kt"}


def kotlin_files() -> list[Path]:
    out: list[Path] = []
    for base in TARGET_DIRS:
        for path in base.rglob("*.kt"):
            if path.name in SKIP_NAMES:
                continue
            out.append(path)
    return out


def main() -> int:
    check = "--check" in sys.argv
    if not RENAMES.is_file():
        print(f"missing {RENAMES} - run build_progression.py first")
        return 1
    renames: dict[str, str] = json.loads(RENAMES.read_text(encoding="utf-8"))

    total_hits = 0
    touched: list[tuple[Path, int]] = []
    for path in kotlin_files():
        text = original = path.read_text(encoding="utf-8")
        hits = 0
        for old, new in sorted(renames.items(), key=lambda kv: -len(kv[0])):
            needle = f'"{old}"'
            if needle in text:
                hits += text.count(needle)
                text = text.replace(needle, f'"{new}"')
        # Lowercase keys: the equipment overrides and the name sets are keyed by
        # the lowercased exercise name, and the load/rep heuristics match
        # lowercased substrings. Those are as much a reference to the pool's
        # vocabulary as the display literals are - and they are exactly how a
        # renamed exercise silently loses its starting weight.
        for old, new in sorted(renames.items(), key=lambda kv: -len(kv[0])):
            needle = f'"{old.lower()}"'
            if needle in text:
                hits += text.count(needle)
                text = text.replace(needle, f'"{new.lower()}"')
        if text != original:
            touched.append((path, hits))
            total_hits += hits
            if not check:
                path.write_text(text, encoding="utf-8")

    for path, hits in touched:
        print(f"  {hits:3d}  {path.relative_to(ROOT)}")
    verb = "would rewrite" if check else "rewrote"
    print(f"{verb} {total_hits} name literal(s) across {len(touched)} file(s)")
    if not touched:
        print("nothing to do - every literal already uses the current name")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
