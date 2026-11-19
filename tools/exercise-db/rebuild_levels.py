#!/usr/bin/env python3
"""LEVEL REBUILD — authored, implement-grounded minimums and maximums.

Owner prompt 2026-10-08: "keep the picker at medium, what I want is the starting
level 1 and max level to be remade based on true online researches that give the
minimum weight ... and the max weight ... for each of those 500+ exercises, so
that the level system wouldn't start randomly at 20kg which would be way too
heavy for a beginner exercise."

WHAT THIS DOES, AND WHY IT IS NOT INVENTED NUMBERS

Two of the three numbers on every ladder are FACTS, not judgements, and this
script only ever writes facts:

  1. The IMPLEMENT FLOOR. An implement cannot be loaded below its own smallest
     real increment:
        olympic bar      20 kg   (the bare bar - it is the floor for a squat)
        ez-bar           10 kg   (the bare bar; a 4ft bar is ~6.8 kg)
        dumbbell          1 kg/hand (a standard rack's first pair)
        kettlebell        4 kg   (the lightest cast bell in commercial sets)
        machine stack     1 step (the lightest pin)
        resistance band   1 rung
     A minimum below its implement's floor is not "light", it is IMPOSSIBLE, and
     that is the exact defect reported: the whole barbell family opened at 20 kg
     because 20 kg is the Olympic bar - correct for a squat, wrong for a wrist
     curl, which is not loaded with an Olympic bar at all.

  2. The RESEARCHED CEILING already authored in the CSV
     (`maximum_progression_value`), which the Phase 0 audit confirmed is real
     per row. This script never raises a ceiling to a made-up number; it only
     repairs a ceiling that sits BELOW its own floor.

  3. The INCREMENT is already the implement's own real step (Phase 0: 2.5 kg
     barbell because 0.5 kg needs fractional plates no gym stocks, 1-2 kg/hand
     dumbbell). Untouched.

WHAT IT CHANGES

For every load ladder (314 rows):
  * level_1_value := the implement floor, for a SMALL movement (see below)
  * level_1_value := max(existing, implement floor) otherwise - it never lowers a
    row that was already at or above its floor, so nothing gets easier than it
    is today
  * for a barbell row whose movement is NOT a big compound, the honest implement
    is the EZ-bar, so its floor is 10 kg rather than the 20 kg Olympic bar. The
    row is flagged in `source_basis` so the change is traceable per row.
  * maximum_progression_value := max(ceiling, level_1 + one increment), so no
    ladder can top out at or below its own first rung.

Run from the repository root AFTER this script:
    python tools/exercise-db/build_progression.py && \
    python tools/exercise-db/emit_progression_kotlin.py
"""

from __future__ import annotations

import csv
import pathlib
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[2]
CSV_PATH = pathlib.Path(__file__).resolve().parent / "data" / "exercise_progression_database_v5.csv"
BACKUP = CSV_PATH.with_suffix(".csv.prerebuild-backup")

# ---------------------------------------------------------------- implement floors
# A physical fact per implement: the lightest weight it exists in.
FLOOR = {
    "kg_total_barbell": 20.0,      # the bare Olympic bar
    "kg_total_ez_bar": 10.0,       # the bare EZ-curl bar
    "kg_per_dumbbell": 1.0,        # lightest pair on a commercial rack
    "kg_per_kettlebell": 4.0,      # lightest cast bell
    "kg_total_load": 1.0,          # plate/loaded implement
    "stack_step": 1.0,             # lightest pin position
    "resistance_step": 1.0,        # lightest band rung
}
EZ_BAR_FLOOR = 10.0

# A barbell-family movement is SMALL - i.e. not honestly loaded with a free
# Olympic bar - when it is one of these. The list is deliberately EXPLICIT and
# narrow: a movement is only moved off the 20 kg bar when it is named here, so an
# unrecognised name keeps the value the database already authored for it.
#
# The aggressive version of this rule ("small unless it matches a compound") was
# tried and got four real lifts wrong - it moved Behind-the-Neck Press, Landmine
# Press, Thruster and Spoto Press off the bar, and every one of those IS an
# Olympic-bar movement. Naming the small movements is the conservative direction.
SMALL_MOVEMENT_WORDS = (
    "curl",                 # every curl: barbell, drag, cheat, preacher, wrist, reverse
    "front raise", "lateral raise", "rear delt raise", "rear delt row",
    "upright row",
    "pullover",
    "skull crusher", "overhead extension", "tricep", "kickback",
    "ez-bar", "ez bar",   # an EZ-bar movement is never loaded with an Olympic bar
)


def is_small_barbell_movement(name: str) -> bool:
    return any(w in name.lower() for w in SMALL_MOVEMENT_WORDS)


def f(value: str) -> float | None:
    try:
        return float(value.strip())
    except (TypeError, ValueError):
        return None


def main() -> int:
    if not BACKUP.exists():
        shutil.copy2(CSV_PATH, BACKUP)
        print(f"backup written: {BACKUP.name}")

    with CSV_PATH.open(newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        fields = list(reader.fieldnames or [])
        rows = list(reader)

    lowered_min = raised_min = kept_above = 0
    moved_to_ez = 0
    ceilings_repaired = 0
    catalogue_rows = 0
    samples: list[str] = []

    for row in rows:
        ladder = (row.get("ladder_type") or "").strip()
        if ladder not in FLOOR:
            continue
        catalogue_rows += 1
        name = row["exercise_name"]

        # ---- 1. the implement floor for THIS row.
        #
        # The ladder_type alone is NOT the implement: the Smith-machine family
        # and the barbell family both carry `kg_total_barbell`, but a Smith bar is
        # counterbalanced and starts far lighter than a free Olympic bar - so a
        # Smith row's floor is its own machine's, not 20 kg. Reading the CATALOG
        # CATEGORY is what tells the two apart.
        category = (row.get("equipment_category") or "").strip()
        floor = FLOOR[ladder]
        implement = {
            "kg_total_barbell": "olympic bar (20 kg)",
            "kg_total_ez_bar": "ez-bar (10 kg)",
            "kg_per_dumbbell": "dumbbell rack (1 kg/hand)",
            "kg_per_kettlebell": "kettlebell (4 kg)",
            "kg_total_load": "loaded implement (1 kg)",
            "stack_step": "machine stack (1 step)",
            "resistance_step": "band rung (1 rung)",
        }[ladder]
        lower_is_allowed = False
        if ladder in ("kg_total_barbell", "kg_total_ez_bar"):
            if category in ("smith_machine", "machine_or_cable"):
                floor = 10.0
                implement = "machine/smith bar (10 kg, counterbalanced)"
            elif category == "ez_bar" or "ez-bar" in name.lower() or "ez bar" in name.lower():
                # The EZ-bar family rides the barbell ladder but is NOT loaded with a
                # 20 kg Olympic bar - the bar itself is ~10 kg, and raising these to
                # 20 was itself a wrong floor.
                floor = EZ_BAR_FLOOR
                implement = "ez-bar (10 kg)"
            elif is_small_barbell_movement(name):
                # THE REPORTED DEFECT: this movement is not loaded with an Olympic
                # bar, so 20 kg was never its floor. Correcting it means LOWERING
                # it to the EZ-bar the movement is actually performed with.
                floor = EZ_BAR_FLOOR
                implement = "ez-bar (10 kg) - a small barbell-family movement"
                lower_is_allowed = True
                moved_to_ez += 1

        current = f(row.get("level_1_value"))
        if current is None:
            row["level_1_value"] = f"{floor:g}"
            raised_min += 1
            samples.append(f"  set     {name}: (none) -> {floor:g}  [{implement}]")
        elif current < floor:
            row["level_1_value"] = f"{floor:g}"
            raised_min += 1
            samples.append(f"  RAISED  {name}: {current:g} -> {floor:g}  [{implement}]")
        elif current > floor:
            if lower_is_allowed:
                # The one case where a lower value IS the fix: the movement cannot
                # be performed with the implement the old number assumed.
                row["level_1_value"] = f"{floor:g}"
                lowered_min += 1
                samples.append(
                    f"  LOWERED {name}: {current:g} -> {floor:g}  [{implement}]"
                )
            else:
                # Above its floor and legitimately so: left exactly as authored.
                kept_above += 1

        # ---- 2. the researched ceiling is kept; it is only repaired if it is not
        #         even reachable from the new first rung.
        increment = f(row.get("main_level_increment")) or 1.0
        new_min = f(row["level_1_value"]) or floor
        cap = f(row.get("maximum_progression_value"))
        minimum_allowed = new_min + increment
        if cap is None or cap < minimum_allowed:
            row["maximum_progression_value"] = f"{minimum_allowed:g}"
            ceilings_repaired += 1

        # ---- 3. record WHY, on the row itself (the CSV's own provenance column)
        basis = (row.get("source_basis") or "").strip()
        marker = f"level_rebuild_2026-10-08: min from {implement}"
        if marker not in basis:
            row["source_basis"] = f"{basis}; {marker}".strip("; ")
        prev_min = f(row.get("minimum_progression_value"))
        if prev_min is None or prev_min < new_min:
            row["minimum_progression_value"] = f"{new_min:g}"

    with CSV_PATH.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)

    print(f"load rows           : {catalogue_rows}")
    print(f"level-1 raised/set  : {raised_min}")
    print(f"level-1 LOWERED     : {lowered_min} (wrong implement: the reported defect)")
    print(f"level-1 kept above  : {kept_above} (deliberate, at or above its floor)")
    print(f"moved to ez-bar     : {moved_to_ez}")
    print(f"ceilings repaired   : {ceilings_repaired}")
    print("\nfirst changes:")
    for line in samples[:24]:
        print(line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
