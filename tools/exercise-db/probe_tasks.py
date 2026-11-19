#!/usr/bin/env python3
"""Probe the generated progression pool behind three dashboard tasks.

  #7  exercises whose ladder climbs in "steps" instead of weight
  #9  starting values too high / exercises below level 4 never shown
  #4  crunches: reps must rise per level and never drop on a level switch

Reads the emitted Kotlin pool - the same table the app compiles - so the
numbers printed here are the numbers the app shows. Nothing is written.
"""
from __future__ import annotations

import collections
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROG = ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ExerciseProgression.kt"
TABLES = ROOT / "composeApp/src/main/java/com/grafizum/repio/data/ProgressionTables.kt"

# Positional order of the ProgressionEntry constructor, from the data class
# header in ExerciseProgression.kt.
FIELDS = [
    "id", "slug", "method", "valueSource", "unit", "level1", "increment",
    "maxLevel", "minValue", "maxValue", "step", "repMin", "repMax",
    "restSeconds", "defaultSetCount", "easyLevel", "mediumLevel", "hardLevel",
    "easyValue", "mediumValue", "hardValue", "easyStage", "mediumStage",
    "hardStage", "easyReps", "mediumReps", "hardReps", "romanStages",
    "resetPolicy", "resetStageAfterJump", "preJumpReps", "postJumpReps",
    "levelValues", "variations", "isBeginnerFriendly", "isSimpleAppReady",
    "requiresCoachReview", "includeInStrengthV1", "includeInCardioV1",
    "includeInMobilityV1", "beginnerBuildEligible", "validationStatus",
    "branches", "ceiling",
]


def split_args(s: str) -> list[str]:
    """Split a call's argument list on top-level commas."""
    args, depth, cur = [], 0, []
    in_str = False
    for ch in s:
        if in_str:
            cur.append(ch)
            if ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
            cur.append(ch)
        elif ch in "([{":
            depth += 1
            cur.append(ch)
        elif ch in ")]}":
            depth -= 1
            cur.append(ch)
        elif ch == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        args.append("".join(cur).strip())
    return args


def parse() -> list[dict]:
    """Read every ProgressionEntry(...) call.

    Entries are NOT one per line: a row with a long branch list wraps, so this
    scans the whole file with a paren depth counter rather than per line.
    """
    text = PROG.read_text(encoding="utf-8")
    entries, i, n = [], 0, len(text)
    # Entries all start with a quoted id; the data class declaration does not,
    # so requiring the quote is enough to skip it.
    needle = 'ProgressionEntry("'
    while True:
        start = text.find(needle, i)
        if start < 0:
            break
        i = start + len("ProgressionEntry(")
        body_start = i
        depth, in_str, esc = 1, False, False
        while i < n and depth:
            ch = text[i]
            if in_str:
                if esc:
                    esc = False
                elif ch == "\\":
                    esc = True
                elif ch == '"':
                    in_str = False
            elif ch == '"':
                in_str = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        args = split_args(text[body_start:i])
        if len(args) != len(FIELDS):
            sys.exit(f"entry {args[0] if args else '?'}: {len(args)} args, expected {len(FIELDS)}")
        entries.append(dict(zip(FIELDS, args)))
        i += 1
    return entries


def ladders() -> dict[str, list[float]]:
    return {
        name: [float(v) for v in body.split(", ")]
        for name, body in re.findall(
            r"val (LADDER_\w+): List<Double> = listOf\(([^)]*)\)", TABLES.read_text(encoding="utf-8")
        )
    }


def unquote(v: str) -> str:
    return v[1:-1] if v.startswith('"') else v


def main() -> None:
    entries = parse()
    tables = ladders()
    print(f"parsed {len(entries)} progression entries\n")

    ready = [e for e in entries if e["validationStatus"].strip('"') in
             ("valid", "valid_but_review_recommended")]

    # ---- #7 -------------------------------------------------------------
    print("#7  ladder unit across the progression-ready pool")
    units = collections.Counter(unquote(e["unit"]) for e in ready)
    step_units = {"stack_step", "resistance_step"}
    for unit, n in units.most_common():
        mark = "  <-- steps, not weight" if unit in step_units else ""
        print(f"    {unit or '(none)':18} {n:4}{mark}")
    stepped = [e for e in ready if unquote(e["unit"]) in step_units]
    print(f"\n    {len(stepped)} of {len(ready)} rows progress in steps:")
    for e in stepped[:15]:
        ref = e["levelValues"].split(".")[-1]
        vals = tables.get(ref, [])
        print(f"      {unquote(e['id']):34} {unquote(e['unit']):16} ladder={vals[:6]}")
    print()

    # ---- #9 -------------------------------------------------------------
    print("#9  starting level per difficulty (level is what the picker offers)")
    for diff in ("easyLevel", "mediumLevel", "hardLevel"):
        c = collections.Counter(int(e[diff]) for e in ready if e[diff] != "null")
        summary = ", ".join(f"L{k}:{v}" for k, v in sorted(c.items()))
        print(f"    {diff:12} {summary}")
    print()

    print("#9  heaviest LEVEL-1 rung per row (the 'too high' starts)")
    heavy = []
    for e in ready:
        ref = e["levelValues"].split(".")[-1]
        vals = tables.get(ref) or []
        if vals:
            heavy.append((vals[0], unquote(e["unit"]), unquote(e["id"]), ref))
    heavy.sort(reverse=True)
    for first, unit, name, ref in heavy[:15]:
        print(f"    {first:7.1f} {unit:16} {name:32} {ref}")
    print()

    print("#9  rows whose MEDIUM/HARD start is a high level")
    high = [(int(e["hardLevel"]), unquote(e["id"])) for e in ready
            if e["hardLevel"] != "null" and int(e["hardLevel"]) >= 9]
    high.sort(reverse=True)
    print(f"    {len(high)} rows start HARD at level 9+; top 15:")
    for lvl, name in high[:15]:
        print(f"      L{lvl} {name}")

    # ---- #4 -------------------------------------------------------------
    print()
    print("#4  crunch-family rows (reps must rise per level, never drop)")
    for e in ready:
        if "crunch" not in unquote(e["id"]):
            continue
        ref = e["romanStages"].split(".")[-1]
        print(f"    {unquote(e['id']):30} unit={unquote(e['unit']):14} "
              f"roman={ref} ladder={e['levelValues'].split('.')[-1]}")


if __name__ == "__main__":
    main()