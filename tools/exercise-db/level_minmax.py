#!/usr/bin/env python3
"""TIER A of the level rebuild: sourced minimums and maximums per exercise.

Owner prompt 2026-10-08: *"look online what the normal minimum and maximum of
each single exercise is and have it adapt to our database / level system"* —
then on 2026-11-10, *"do the level task now"*.

WHY A SEPARATE TABLE

`rebuild_levels.py` (floors) and `rebuild_maxima.py` (ceilings) derive numbers
from implement facts and movement FAMILIES. That is right for 600 rows, but a
family rule cannot tell a Romanian deadlift from a conventional one, and it has
no way to say *where a number came from* on a specific lift. This table is the
per-exercise answer for the primary lifts: one row each, both numbers, both
sources, no exceptions.

WHAT THE SOURCES ARE

`min_source` and `max_source` are REQUIRED and name a real reference: a
bodyweight-multiple strength standard, a published %1RM / rep band, or a
physical implement fact (a bare Olympic bar is 20 kg — that is not a judgement).
An empty source is a BUILD FAILURE (`build_progression.py` exits non-zero), which
is the thing that stops this table drifting back into invented numbers.

Ceilings are stated as a multiple of bodyweight, converted with the same 80 kg
reference lifter the rest of this database is authored around, and rounded DOWN
to the row's own step because a ladder can only step in real increments.

WHAT THIS MODULE DOES NOT DO

It never invents an increment or a rung count: the engine builds a uniform-step
ladder from `level1` and derives how many rungs fit under the ceiling
(`ProgressionEngine.constantStepLadder` / `effectiveMaxLevel`, owner ruling
2026-10-13). So overriding the two numbers here is the whole change, and the
ladder follows automatically.

The validation below is the guard the plan asked for: a minimum can never be
below the smallest weight its implement exists in, a maximum must sit above the
minimum, and there must be at least four rungs of room between them.
"""

from __future__ import annotations

import csv
import pathlib

CSV_PATH = pathlib.Path(__file__).resolve().parent / "data" / "level_minmax_sources.csv"

REFERENCE_BODYWEIGHT_KG = 80.0

# The lightest weight each ladder unit exists in. A level 1 below this is not an
# easy level, it is a weight the implement cannot make.
IMPLEMENT_FLOOR = {
    "kg_total_barbell": 2.5,     # smallest loadable jump on a bar (1.25 kg/side)
    "kg_per_dumbbell": 1.0,      # lightest pair on a commercial rack
    "kg_per_kettlebell": 4.0,    # lightest cast bell
    "kg_total_load": 1.25,       # smallest plate
    "stack_step": 1.0,           # lightest pin
    "resistance_step": 1.0,      # lightest band rung
    "variation_step": 1.0,       # first variation
}

REQUIRED = ("name", "unit", "min_value", "max_value", "min_source", "max_source")

# A ladder needs room to climb: level 1, at least three steps, and the ceiling.
MIN_RUNGS = 4


def parse_float(value: str | None) -> float | None:
    if value is None or not str(value).strip():
        return None
    try:
        return float(str(value).strip())
    except ValueError:
        return None


def rows() -> list[dict]:
    if not CSV_PATH.exists():
        return []
    with CSV_PATH.open(newline="", encoding="utf-8-sig") as fh:
        return list(csv.DictReader(fh))


def apply(by_name: dict[str, dict]) -> list[str]:
    """Override level1 / maxValue for every covered row.

    [by_name] maps the CSV's own exercise name (pre-generalization) to that
    row's progression dict, which is what the emitter reads. Returns a list of
    problems; any problem fails the build.
    """
    problems: list[str] = []
    applied = changed = 0
    samples: list[str] = []
    seen: set[str] = set()

    for row in rows():
        if any(k is None for k in row):
            problems.append(
                f"level_minmax: '{row.get('name') or '?'}' has more fields than the header "
                "- an unquoted comma in a source splits the row"
            )
            continue

        blank = [k for k in REQUIRED if not (row.get(k) or "").strip()]
        if blank:
            problems.append(
                f"level_minmax: '{row.get('name') or '?'}' has empty {', '.join(blank)} "
                "(a source is required - an uncited number is not allowed)"
            )
            continue

        name = row["name"].strip()
        if name in seen:
            problems.append(f"level_minmax: '{name}' appears twice in the table")
            continue
        seen.add(name)

        prog = by_name.get(name)
        if prog is None:
            problems.append(
                f"level_minmax: '{name}' matches no exercise in the pool "
                "(the name must be the CSV's exercise_name, exactly)"
            )
            continue

        unit = row["unit"].strip()
        if unit != (prog.get("unit") or ""):
            problems.append(
                f"level_minmax: '{name}' declares unit '{unit}' but its ladder is "
                f"'{prog.get('unit')}'"
            )
            continue

        low = parse_float(row["min_value"])
        high = parse_float(row["max_value"])
        if low is None or high is None:
            problems.append(f"level_minmax: '{name}' has a non-numeric min or max")
            continue

        floor = IMPLEMENT_FLOOR.get(unit, 0.0)
        if low < floor:
            problems.append(
                f"level_minmax: '{name}' opens at {low:g} {unit}, below the "
                f"implement's smallest real weight ({floor:g})"
            )
            continue
        if high <= low:
            problems.append(f"level_minmax: '{name}' ceiling {high:g} is not above its floor {low:g}")
            continue

        step = parse_float(str(prog.get("increment"))) or 0.0
        if step <= 0:
            problems.append(f"level_minmax: '{name}' has no positive step to climb by")
            continue
        if (high - low) / step + 1 < MIN_RUNGS:
            problems.append(
                f"level_minmax: '{name}' has only {round((high - low) / step) + 1} rungs "
                f"between {low:g} and {high:g} at a {step:g} step"
            )
            continue

        previous_low = prog.get("level1")
        previous_high = prog.get("maxValue")
        prog["level1"] = low
        prog["maxValue"] = high
        # The floor IS the minimum progression value: one concept, one number.
        prog["minValue"] = low
        # Provenance travels with the row, so the emitted HumanCeiling can name
        # where its load came from instead of claiming the v5 CSV produced it.
        prog["minmaxSource"] = f"{row['min_source'].strip()} / {row['max_source'].strip()}"
        applied += 1
        if previous_low != low or previous_high != high:
            changed += 1
            if len(samples) < 12:
                samples.append(
                    f"  {name:38} level1 {previous_low} -> {low:g}, "
                    f"max {previous_high} -> {high:g} {unit}"
                )

    if problems:
        return problems

    print(
        f"level minmax (Tier A)      : {applied} rows applied, {changed} changed"
        f" ({REFERENCE_BODYWEIGHT_KG:g} kg reference lifter)"
    )
    for line in samples:
        print(line)
    return problems
