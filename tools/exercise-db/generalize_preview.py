#!/usr/bin/env python3
"""What happens to the pool if the apparatus prefix is simply stripped?

Task on the board: names like "Smith Machine Bent Over Row" are a
specification, not a name — generalize the whole database.

The pool is keyed by NAME (exerciseEquipment, exercisePrimaryMuscles, logged
sets and the built-in templates all join on it), so a rename that produces two
identical names silently merges two exercises. This measures that before
anything is edited.
"""
from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

CATALOG = Path(__file__).resolve().parent / "out" / "catalog_v5.json"

# Apparatus / specification words the owner wants gone from the front.
APPARATUS = [
    "Smith Machine", "Machine", "Barbell", "Dumbbell", "Kettlebell", "Cable",
    "EZ-Bar", "EZ Bar", "Trap Bar", "Banded", "Stability Ball", "TRX",
    "Bodyweight", "Box Jump", "Medicine Ball", "Sled", "Rope",
]


def strip_apparatus(name: str) -> str:
    out = name
    changed = True
    while changed:
        changed = False
        for lead in APPARATUS:
            if out.lower().startswith(lead.lower() + " "):
                out = out[len(lead) + 1:]
                changed = True
    return out.strip(" -") or name


def main() -> None:
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    entries = data if isinstance(data, list) else data.get("entries", data.get("exercises", []))
    rows = []
    for e in entries:
        name = e.get("name", "") if isinstance(e, dict) else str(e)
        rows.append((name, strip_apparatus(name)))

    changed = [(a, b) for a, b in rows if a != b]
    print(f"pool size            : {len(rows)}")
    print(f"names that would move: {len(changed)}")

    buckets: dict[str, list[str]] = defaultdict(list)
    for original, stripped in rows:
        buckets[stripped].append(original)

    collisions = {k: v for k, v in buckets.items() if len(v) > 1}
    print(f"colliding names      : {len(collisions)} "
          f"({sum(len(v) for v in collisions.values())} exercises involved)")

    print("\nthe worst collisions:")
    for target, sources in sorted(collisions.items(), key=lambda kv: -len(kv[1]))[:15]:
        print(f"  {target}  <-  {', '.join(sorted(sources))}")

    print("\nexercises that keep a unique name after stripping:")
    safe = [(a, b) for a, b in changed if len(buckets[b]) == 1]
    print(f"  {len(safe)} of {len(changed)}")


if __name__ == "__main__":
    main()
