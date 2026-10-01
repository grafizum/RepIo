#!/usr/bin/env python3
"""How many pool exercise names carry an apparatus prefix?

Task on the board: "Names like 'smith machine' at the front of a simple
'bent over row' is too much. Thats a specification not needed ... change the
complete database so the exercises have a generalization."

Prints every catalog name whose leading words name a piece of apparatus, so the
rename set is decided from the data instead of guessed.
"""
from __future__ import annotations

import json
import re
from collections import Counter
from pathlib import Path

CATALOG = Path(__file__).resolve().parent / "out" / "catalog_v5.json"

# Apparatus words that appear as the FIRST word (or first two) of a name.
LEADING = [
    "Smith Machine",
    "Machine",
    "Barbell",
    "Dumbbell",
    "Kettlebell",
    "Cable",
    "Resistance Band",
    "Banded",
    "EZ Bar",
    "Trap Bar",
    "Close Grip",
    "Wide Grip",
    "Incline",
    "Decline",
    "Seated",
    "Standing",
    "Lying",
    "Suspended",
    "Stability Ball",
    "Bosu",
    "TRX",
    "Rope",
    "Sled",
    "Sledgehammer",
    "Medicine Ball",
    "Box Jump",
    "Wall Ball",
    "Nordic",
    "Plyometric",
    "Weighted",
    "Bodyweight",
    "Assisted",
    "Single Arm",
    "One Arm",
    "Alternating",
    "Isometric",
    "Static",
    "Tempo",
    "Paused",
    "Negative",
    "Pulsing",
    "Explosive",
    "Power",
    "Speed",
    "Ballistic",
    "Olympic",
    "Conventional",
    "Sumo",
    "Close-Grip",
]


def main() -> None:
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    entries = data if isinstance(data, list) else data.get("entries", data.get("exercises", []))
    names = [e.get("name", "") if isinstance(e, dict) else str(e) for e in entries]
    print(f"pool size: {len(names)}\n")

    hits: list[tuple[str, str]] = []
    for name in names:
        low = name.lower()
        for lead in LEADING:
            if low.startswith(lead.lower() + " ") or low == lead.lower():
                hits.append((lead, name))
                break

    print(f"names starting with an apparatus/specification word: {len(hits)}")
    print("by leading word:")
    for lead, count in Counter(lead for lead, _ in hits).most_common():
        print(f"  {count:4d}  {lead}")
    print("\nall matching names:")
    for _, name in sorted(hits):
        print("  " + name)


if __name__ == "__main__":
    main()
