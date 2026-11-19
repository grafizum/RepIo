#!/usr/bin/env python3
"""Rekey the equipment-tag name sets onto today's pool names.

WHY THIS EXISTS
---------------
The rename pipeline (tools/exercise-db/) moved the apparatus into the exercise
name - "Curl (Barbell)" became "Barbell Curl", "Bench Press (Dumbbell)" became
"Dumbbell Bench Press". `apply_rename_to_sources.py` rewrites whole string
literals in the chains, the templates and the tests, but the name-based lookup
sets inside ExerciseEquipmentTags.kt were never in its list.

That file decides EQUIPMENT GATING: `requirementsFor` asks "which set is this
name in?" and a name that is in no set silently falls through to the next
branch. A stale name is therefore not a cosmetic duplicate, it is an exercise
that stops requiring the machine it needs:

    SEATED_ROW_SET   2 of 2  stale -> nothing is gated as a seated row
    DUMBBELL_FLOOR_SET 2 of 2 stale -> nothing is gated as dumbbell floor work
    BARBELL_ONLY_SET 14 of 18 stale -> barbell_only gated 4 exercises, not 18

and the Smith set lost three, which is the same shape as the "bodyweight and
dumbbells still selected machines" report in AGENTS.md: a user who owns a Smith
machine is denied Smith exercises because the gate cannot find the name.

It fails SILENTLY. There is no crash and no log line - an unmatched name is an
ordinary miss - which is why 33 of them survived a rename that a test suite
covering the pool could not see.

WHAT IT DOES
------------
Every string literal in the file's `private val *_SET = setOf(...)` blocks is
resolved to its current pool name by following `ExerciseCatalog.RENAMED`
transitively (the map spans two rename passes, so a 2019 name may need two
hops), then matching case-insensitively against the pool.

Anything that cannot be resolved is DELETED rather than guessed, and that is
safe rather than a judgement call: these sets are matched with
`equals(ignoreCase = true)` against the pool, so a name that is not in the pool
can never match anything. It is not a near-miss that might catch a rename later,
it is a literal that does nothing. Two kinds arrive here:

  * renamed and then dropped from the pool - "wide-grip ez-bar curl" and
    "reverse-grip ez-bar curl" both collapsed into "EZ-Bar Curl";
  * never in this pool at all - "machine seated row" and "seated row machine".
    There is no seated-row exercise in the 601, so SEATED_ROW_MACHINE currently
    gates nothing, which is worth knowing rather than leaving as a silent branch.

Rewriting one of these to a plausible neighbour would be guessing a gate, and a
guessed gate is how an exercise ends up requiring the wrong machine.

Run from the repo root:  python tools/exercise-db/rekey_equipment_sets.py [--write]
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
CATALOG = REPO / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseCatalog.kt"
TAGS = REPO / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseEquipmentTags.kt"


def load_pool_and_renames() -> tuple[dict[str, str], dict[str, str]]:
    src = CATALOG.read_text()
    pool = {
        name.lower(): name
        for _, name, _, _ in re.findall(
            r'ExerciseCatalogEntry\(\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)"',
            src,
        )
    }
    # RENAMED is keyed in Title Case ("Curl (Barbell)"), while the equipment
    # sets spell their names in lower case ("curl (barbell)") because they are
    # matched with equals(ignoreCase = true) at runtime. The first version of
    # this tool indexed RENAMED as-is and therefore resolved NOTHING - all 33
    # stale names "failed" for a reason that had nothing to do with the renames.
    renames = {k.lower(): v for k, v in re.findall(r'"([^"]*)"\s+to\s+"([^"]*)"', src)}
    return pool, renames


def canonical(name: str, pool: dict[str, str], renames: dict[str, str]) -> str | None:
    """The pool's current name for `name`, or None when it cannot be resolved.

    Both indexes are keyed lower case, so the walk is case-insensitive
    throughout. RENAMED is cumulative across two passes and maps OLD -> NEW, so
    the walk is transitive and guarded against a cycle.
    """
    seen: set[str] = set()
    current = name.lower()
    while current in renames and current not in seen:
        seen.add(current)
        current = renames[current].lower()
    return pool.get(current)


def set_blocks(src: str) -> list[tuple[str, int, int]]:
    """(name, start, end) of every `private val X_SET = setOf(...)` body.

    The body is found by paren balance rather than by looking for the closing
    line, because several of these sets are written on ONE line and a
    line-oriented pattern silently swallows the set that follows it.
    """
    blocks = []
    for m in re.finditer(r'private val (\w+_SET) = setOf\(', src):
        i, depth = m.end(), 1
        while depth:
            if src[i] == "(":
                depth += 1
            elif src[i] == ")":
                depth -= 1
            i += 1
        blocks.append((m.group(1), m.end(), i - 1))
    return blocks


def main(write: bool) -> int:
    pool, renames = load_pool_and_renames()
    src = TAGS.read_text()

    stale_total = 0
    dropped: list[tuple[str, str]] = []
    # Applied back-to-front so earlier offsets stay valid.
    edits: list[tuple[int, int, str]] = []

    for name, start, end in set_blocks(src):
        body = src[start:end]
        for literal in re.finditer(r'"([^"]*)"', body):
            old = literal.group(1)
            if old.lower() in pool:
                # EVERY entry in these sets must be lower case.
                #
                # `requirementsFor` compares them with `n in SOME_SET`, where
                # `n` is `entry.name.lowercase()` - a case-SENSITIVE `in`. Writing
                # the pool's display casing here ("Machine Bicep Curl" against a
                # lowercased `n`) silently makes every lookup miss, which is how
                # a Machine kit ended up with no biceps exercise at all. The sets
                # are looked up in lower case, so lower case is what they hold.
                if old != old.lower():
                    edits.append(
                        (start + literal.start(1), start + literal.end(1), old.lower())
                    )
                continue
            stale_total += 1
            new = canonical(old, pool, renames)
            if new is None:
                dropped.append((name, old))
                # Drop the WHOLE literal including its quotes, plus the
                # separator, so a set never ends up with a dangling comma.
                s, e = start + literal.start(), start + literal.end()
                edits.append((s, e + (1 if src[e] == "," else 0), ""))
            else:
                # Replace only the TEXT between the quotes. Spanning the quotes
                # instead emits a bare identifier - `Barbell Curl,` - which is
                # not valid Kotlin and does not compile.
                s = start + literal.start(1)
                e = start + literal.end(1)
                edits.append((s, e, new.lower()))

    for name, old in dropped:
        print(f"  DROPPED (not in the pool, cannot match)  {name}: {old!r}")
    print(f"\n{stale_total} stale name(s): {len(edits) - len(dropped)} rekeyed, {len(dropped)} dropped")

    if not write:
        print("dry run; pass --write to apply")
        return 0

    for s, e, new in sorted(edits, reverse=True):
        src = src[:s] + new + src[e:]
    # An emptied set should read setOf(), not setOf( ).
    src = re.sub(r"setOf\(\s+\)", "setOf()", src)
    src = re.sub(r",\s*,", ",", src)
    TAGS.write_text(src)
    print(f"rewrote {len(edits)} literal(s) in {TAGS.name}")

    # Re-read and prove the property the file exists to hold.
    after = TAGS.read_text()
    remaining = []
    for name, start, end in set_blocks(after):
        for literal in re.finditer(r'"([^"]*)"', after[start:end]):
            if literal.group(1).lower() not in pool:
                remaining.append((name, literal.group(1)))
    if remaining:
        print(f"STILL STALE after rewrite: {remaining}")
        return 1
    print("verified: every name in every equipment set is in the pool")
    return 0


if __name__ == "__main__":
    raise SystemExit(main("--write" in sys.argv))