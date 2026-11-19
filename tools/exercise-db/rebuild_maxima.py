#!/usr/bin/env python3
"""LEVEL REBUILD, part 2 — the MAXIMUM of every load ladder.

Owner prompt 2026-10-08: "make sure you do the maxima task now so the maximum of
an exercise is correct".

WHAT WAS WRONG

Every row carried a `maximum_progression_value` copied from its FAMILY, not from
the movement. Measured before this script: a Barbell Back Squat, a Barbell Curl
and a Barbell Front Raise all topped out at 60 kg. A 60 kg front raise is not a
credible human ceiling and a 60 kg squat is not a trained one.

WHERE THE NUMBERS COME FROM

The ceiling of a loaded movement is a published multiple of BODYWEIGHT, which is
how professional standards state it - a novice/intermediate/advanced lifter
squats ~0.75x / 1.25x / 1.75x bodyweight, deadlifts ~1x / 1.75x / 2.25x, benches
~0.5x / 1x / 1.25x (the bodyweight-multiple standards published for the classic
lifts by the strength-standards tables, and the %1RM progressions in NSCA's
Essentials of Strength Training and Conditioning). An isolation movement gets a
much smaller multiple because leverage, not effort, is the limit: a lateral raise
never reaches half bodyweight however strong the lifter.

The ABSOLUTE kilogram figure therefore needs a reference bodyweight, and this
database is already authored around an 80 kg male (its existing 120 kg barbell
ceilings and its 2-4 kg kettlebell floors both fit that lifter). So:

    ceiling = coefficient x REFERENCE_BODYWEIGHT_KG

rounded DOWN to the row's own increment, because a ladder can only step in real
implement increments - a 141.6 kg ceiling is a 140 kg top rung.

Each row records the coefficient and its basis in `source_basis`, so any number
can be traced back to the multiple it came from.

This script only ever writes a ceiling for a LOAD ladder; rep and duration rows
are untouched. It never writes a ceiling at or below the row's own first rung.
"""

from __future__ import annotations

import csv
import math
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
CSV_PATH = pathlib.Path(__file__).resolve().parent / "data" / "exercise_progression_database_v5.csv"

REFERENCE_BODYWEIGHT_KG = 80.0

# (pattern, coefficient, basis). FIRST match wins, so the specific patterns must
# come before the general ones.
RULES: list[tuple[str, float, str]] = [
    # --- the classic barbell lifts: published bodyweight multiples, advanced rung
    (r"back squat|front squat|box squat|hack squat|split squat|smith machine squat",
     1.75, "advanced squat standard ~1.75x bodyweight"),
    (r"deadlift|rack pull|sumo", 2.25, "advanced deadlift standard ~2.25x bodyweight"),
    (r"romanian deadlift|stiff-leg|rdl", 1.75, "advanced hinge standard ~1.75x bodyweight"),
    (r"bench press|floor press|spoto", 1.25, "advanced bench standard ~1.25x bodyweight"),
    (r"overhead press|shoulder press|military press|behind the neck press|z press",
     0.70, "advanced overhead-press standard ~0.70x bodyweight"),
    (r"bent-over row|bent over row|pendlay|yates|row \(smith|bench pull",
     1.00, "advanced barbell-row standard ~1.00x bodyweight"),
    (r"good morning", 1.00, "advanced good-morning standard ~1.00x bodyweight"),
    (r"hip thrust|glute bridge|glute drive", 2.00, "loaded hip-extension standard ~2.00x bodyweight"),
    (r"lunge|step up", 1.25, "advanced single-leg standard ~1.25x bodyweight"),
    (r"calf raise", 2.00, "advanced calf standard ~2.00x bodyweight"),
    (r"shrug", 1.50, "advanced shrug standard ~1.50x bodyweight"),
    (r"upright row", 0.60, "advanced upright-row standard ~0.60x bodyweight"),
    (r"wrist curl|wrist roller", 0.35, "forearm-curl standard ~0.35x bodyweight"),
    # --- barbell-family isolation: leverage caps it well below the compound
    (r"preacher curl|drag curl|cheat curl|spider curl|concentration curl|"
     r"barbell curl|reverse curl|ez-bar curl|ez bar curl|close-grip.*curl|curl",
     0.50, "advanced curl standard ~0.50x bodyweight"),
    (r"skull crusher|overhead (tricep|extension)|tricep extension|kickback",
     0.45, "advanced triceps-extension standard ~0.45x bodyweight"),
    (r"pullover", 0.50, "advanced pullover standard ~0.50x bodyweight"),
    (r"front raise", 0.25, "front-raise leverage cap ~0.25x bodyweight"),
    (r"lateral raise|rear delt|reverse fly", 0.20,
     "lateral/rear raise leverage cap ~0.20x bodyweight"),
    # --- dumbbells are PER HAND, and a hand holds about half what a bar takes
    (r"dumbbell.*row|chest-supported.*row", 0.40,
     "per-hand advanced row standard ~0.40x bodyweight"),
    (r"dumbbell.*(press|bench)", 0.35, "per-hand advanced press standard ~0.35x bodyweight"),
    (r"dumbbell.*curl|hammer curl", 0.30, "per-hand advanced curl standard ~0.30x bodyweight"),
    (r"dumbbell.*(lateral|front) raise|dumbbell.*rear delt", 0.15,
     "per-hand raise leverage cap ~0.15x bodyweight"),
    (r"dumbbell.*(extension|tricep)", 0.25, "per-hand triceps standard ~0.25x bodyweight"),
    (r"dumbbell.*(lunge|step)", 0.35, "per-hand single-leg standard ~0.35x bodyweight"),
    (r"dumbbell.*(squat|deadlift|swing|clean|snatch|thruster)", 0.45,
     "per-hand advanced compound standard ~0.45x bodyweight"),
]

# Rows whose ladder unit is a machine pin or a band are NOT kilograms, so a
# bodyweight multiple says nothing about them - their ceiling is the machine's
# stack, which the database already carries. Left alone deliberately.
KG_UNITS = {"kg_total_barbell", "kg_per_dumbbell", "kg_per_kettlebell", "kg_total_load"}


# A PER-HAND ladder (a dumbbell, a kettlebell) is not a barbell ladder, and the
# bodyweight multiple for a bar does not transfer: a hand holds roughly half what
# two hands on a bar do, and the leverage of a dumbbell movement differs again.
# Selecting these FIRST is what stops "Bulgarian Split Squat [dumbbell]" from
# inheriting the 1.75x barbell squat multiple and publishing a 140 kg/hand
# ceiling. Any per-hand row with no specific rule is capped at PER_HAND_CAP.
PER_HAND_RULES: list[tuple[str, float, str]] = [
    (r"bulgarian|split squat|step up|lunge", 0.35, "per-hand single-leg standard ~0.35x bodyweight"),
    (r"squat|deadlift|swing|clean|snatch|thruster|press", 0.45,
     "per-hand advanced compound standard ~0.45x bodyweight"),
    (r"row|pullover", 0.40, "per-hand advanced row standard ~0.40x bodyweight"),
    (r"curl|hammer", 0.30, "per-hand advanced curl standard ~0.30x bodyweight"),
    (r"extension|tricep|skull", 0.25, "per-hand triceps standard ~0.25x bodyweight"),
    (r"lateral raise|front raise|rear delt|reverse fly", 0.15,
     "per-hand raise leverage cap ~0.15x bodyweight"),
    (r"shrug|calf", 0.50, "per-hand shrug/calf standard ~0.50x bodyweight"),
]
PER_HAND_CAP = 0.45


def rule_for(name: str, per_hand: bool) -> tuple[float, str] | None:
    n = name.lower()
    if per_hand:
        for pattern, coefficient, basis in PER_HAND_RULES:
            if re.search(pattern, n):
                return coefficient, basis
        # No specific rule: the safest per-hand reading is the compound cap
        # rather than a barbell multiple.
        return PER_HAND_CAP, "per-hand fallback cap ~0.45x bodyweight"
    for pattern, coefficient, basis in RULES:
        if re.search(pattern, n):
            return coefficient, basis
    return None


def f(value: str) -> float | None:
    try:
        return float(value.strip())
    except (TypeError, ValueError):
        return None


def main() -> int:
    with CSV_PATH.open(newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        fields = list(reader.fieldnames or [])
        rows = list(reader)

    written = unmapped = raised = lowered = unreachable = 0
    samples: list[str] = []

    for row in rows:
        unit = (row.get("level_1_unit") or "").strip()
        ladder = (row.get("ladder_type") or "").strip()
        if ladder not in KG_UNITS and unit not in KG_UNITS:
            continue
        name = row["exercise_name"]
        per_hand = unit.startswith("kg_per_") or ladder.startswith("kg_per_")
        rule = rule_for(name, per_hand)
        if rule is None:
            unmapped += 1
            continue
        coefficient, basis = rule
        step = f(row.get("main_level_increment")) or 1.0
        floor = f(row.get("level_1_value"))
        if floor is None:
            continue

        raw = coefficient * REFERENCE_BODYWEIGHT_KG
        # A ladder steps in real implement increments, so the ceiling is the
        # largest whole number of steps that still fits under the multiple.
        steps = math.floor((raw - floor) / step)
        ceiling = floor + steps * step
        if ceiling <= floor:
            # The multiple is below the movement's own first rung (a light lift
            # with a heavy implement): keep one step of range rather than a
            # ladder that cannot climb.
            ceiling = floor + step
            unreachable += 1
        if unit.startswith("kg_per_"):
            # Per-hand units are authored per hand and the multiple is per hand.
            pass

        previous = f(row.get("maximum_progression_value"))
        if previous is None or ceiling > previous:
            raised += 1
        elif ceiling < previous:
            lowered += 1
        row["maximum_progression_value"] = f"{ceiling:g}"
        written += 1

        basis_text = (row.get("source_basis") or "").strip()
        marker = f"maxima_2026-10-08: {coefficient:g} x {REFERENCE_BODYWEIGHT_KG:g}kg ({basis})"
        if marker not in basis_text:
            row["source_basis"] = f"{basis_text}; {marker}".strip("; ")

        if len(samples) < 22:
            samples.append(
                f"  {name:38} -> max {ceiling:g} {unit}  (was {previous if previous is not None else '-'})"
            )

    with CSV_PATH.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)

    print(f"load rows given a ceiling : {written}")
    print(f"  raised / lowered        : {raised} / {lowered}")
    print(f"  multiple below first rung: {unreachable} (kept one step of range)")
    print(f"no rule matched (left as authored): {unmapped}")
    print("\nsample:")
    for line in samples:
        print(line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
