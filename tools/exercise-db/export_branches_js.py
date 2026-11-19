#!/usr/bin/env python3
"""Export the generated progression branches as `design/_branches.js`.

The progression-window mockups under `design/` read `window.__BRANCHES__` so
the design can be inspected against the REAL branch data rather than invented
rows. That only helps if the export cannot drift from the pipeline, so this
reads the GENERATED `out/progression_v5.json` - the same file
`emit_progression_kotlin.py` turns into `ExerciseProgression.kt` - rather than
re-deriving anything. Two sources of truth would be one too many.

Run it after the pipeline, in this order:

    PYTHONPATH=tools/exercise-db:tools/exercise-db/data python3 tools/exercise-db/build_progression.py
    PYTHONPATH=tools/exercise-db:tools/exercise-db/data python3 tools/exercise-db/emit_progression_kotlin.py
    PYTHONPATH=tools/exercise-db:tools/exercise-db/data python3 tools/exercise-db/export_branches_js.py

`--check` writes nothing and exits non-zero when the file on disk disagrees,
which is the form to use in a build: the mockup then cannot go stale without
failing.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
PROGRESSION_JSON = ROOT / "tools" / "exercise-db" / "out" / "progression_v5.json"
TARGET = ROOT / "design" / "_branches.js"

HEADER = (
    "// GENERATED FILE - do not edit.\n"
    "// Written by tools/exercise-db/export_branches_js.py from\n"
    "// tools/exercise-db/out/progression_v5.json (the same source that becomes\n"
    "// ExerciseProgression.kt). Re-run the pipeline and this script after any\n"
    "// change to the ladder tables; `export_branches_js.py --check` fails if it\n"
    "// has drifted.\n"
)


def load_branches() -> dict[str, dict]:
    if not PROGRESSION_JSON.exists():
        sys.exit(
            f"missing {PROGRESSION_JSON}\n"
            "run build_progression.py first - this script exports the pipeline's\n"
            "output and deliberately does not re-derive it."
        )
    rows = json.loads(PROGRESSION_JSON.read_text(encoding="utf-8"))
    if isinstance(rows, dict):
        rows = rows.get("progression") or rows.get("rows") or list(rows.values())

    out: dict[str, dict] = {}
    for row in rows:
        branches = row.get("branches") or []
        if not branches:
            continue
        name = row.get("name") or row.get("id")

        # maxLevel is NOT the only way a row says how long its ladder is. A
        # loaded row carries `maxLevel`; a timed or rep-based row (a chin tuck
        # hold, a dead hang) carries a `levelValueSequence` and NO maxLevel at
        # all - 23 of the 334 branched rows are like this. Reading only
        # maxLevel exported `null` for them, which a mockup then prints as
        # "max null levels". Derive the length the way the app does: the highest
        # level the sequence actually defines.
        #
        # The sequence arrives as a JSON-encoded STRING, not a list. Iterating it
        # directly yields single characters, which read as an empty ladder and
        # silently produce `maxLevel: None` again - the same wrong answer by a
        # different route - so it is parsed first.
        max_level = row.get("maxLevel")
        if max_level is None:
            raw = row.get("levelValueSequence")
            sequence: list[dict] = []
            if isinstance(raw, str) and raw.strip():
                sequence = json.loads(raw)
            elif isinstance(raw, list):
                sequence = raw
            levels = [s.get("level") for s in sequence if isinstance(s, dict) and s.get("level") is not None]
            max_level = max(levels) if levels else None

        unit = row.get("unit")
        out[name] = {
            "name": name,
            "maxLevel": max_level,
            "unit": unit,
            "branches": [
                {
                    "entry": b["entry"],
                    "nodes": b.get("nodes") or [],
                    "note": b.get("note") or "",
                    "tag": b.get("tag") or "advanced",
                }
                for b in branches
            ],
        }
    return out


def render(branches: dict[str, dict]) -> str:
    payload = json.dumps(branches, ensure_ascii=False, separators=(",", ":"))
    return HEADER + "window.__BRANCHES__=" + payload + ";\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="do not write; exit 1 if design/_branches.js is stale",
    )
    args = parser.parse_args()

    branches = load_branches()
    text = render(branches)

    if args.check:
        current = TARGET.read_text(encoding="utf-8") if TARGET.exists() else ""
        if current != text:
            print(
                f"design/_branches.js is stale - {len(branches)} branches in "
                f"{PROGRESSION_JSON.name} but the file on disk does not match.\n"
                "re-run tools/exercise-db/export_branches_js.py"
            )
            return 1
        print(f"design/_branches.js is current ({len(branches)} exercises)")
        return 0

    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text(text, encoding="utf-8")

    multi = sum(1 for v in branches.values() if len(v["branches"]) > 1)
    print(
        f"wrote {TARGET.relative_to(ROOT)} "
        f"({len(branches)} exercises, {sum(len(v['branches']) for v in branches.values())} branches, "
        f"{multi} need a picker)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())