#!/usr/bin/env python3
"""Dry-run the name generalization against the current pool.

Prints the full old -> new table so the rename can be reviewed before the
generated Kotlin changes. Nothing is written.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generalize import build_rename_map  # noqa: E402

CATALOG = Path(__file__).resolve().parent / "out" / "catalog_v5.json"


def main() -> None:
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    entries = data if isinstance(data, list) else data.get("entries", data.get("exercises", []))
    names = [e.get("name", "") for e in entries if isinstance(e, dict)]
    renames = build_rename_map(names)
    bare = sum(1 for old, new in renames.items() if "(" not in new)
    print(f"pool size      : {len(names)}")
    print(f"names renamed  : {len(renames)}")
    print(f"  fully bare   : {bare}")
    print(f"  with qualifier: {len(renames) - bare}\n")
    width = max(len(o) for o in renames)
    for old in sorted(renames):
        print(f"  {old:<{width}}  ->  {renames[old]}")


if __name__ == "__main__":
    main()
