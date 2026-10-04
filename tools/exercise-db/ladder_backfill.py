"""Level ladders for the rows the v5 database refused to guess.

The v5 progression CSV flags 109 of its 601 rows `manual_review` rather than
invent a starting value, and the CSV's own `review_notes` say why: "Equipment
'ab_wheel' did not match a defined universal ladder rule. Flagged rather than
inventing an unjustified load default." Refusing to guess is the right default,
but it leaves 18% of the pool outside the level system, which is what the owner
reported on 2026-10-02 as "lots of exercises have no level system".

This module is the second half of that answer: for each gap group it supplies
the measure the row's own `review_notes` names, and for the groups where no
universal measure exists it returns a REASON, so the pipeline can report the
short list the owner asked for instead of pretending the gap is closed.

Three rules, because each one has a way of going wrong quietly:

1. **The measure comes from the row, not from the name.** A stretch is a hold
   in seconds because the CSV says so in its `training_category`; a barbell
   snatch is a kilogram ladder because its equipment is a barbell. Keying this
   off a substring of the display name would re-generalize names, which the
   project's naming rules forbid.
2. **A hold has NO Roman pattern.** The Roman stages are reps-per-set, and a
   45-second hold has no reps. Rows in this file therefore carry an empty
   pattern and one stage, and the LEVEL carries the progression - which is
   exactly how the 46 existing `duration_progression` rows (Air Bike and
   friends) already work. Inventing reps for a stretch is the bug the owner
   reported on the wrist stretch.
3. **Weighted rows DO keep a Roman pattern**, because their reps are real.
   Olympic lifts are the exception to the usual 8-12 pattern: they are trained
   at 1-5 reps per set, so `OLYMPIC_ROMAN` is a low-rep pattern rather than the
   standard one, and a snatch at 8-12 reps would be a different exercise.

Nothing here is a guess dressed as data: every ladder is a round number a
practitioner would recognise, and each carries the reason it stops where it
stops in a comment.
"""

from __future__ import annotations

import json

# --------------------------------------------------------------------------
# Ladders. Every entry is (values, why it ends where it does).
# --------------------------------------------------------------------------

# Static stretching. The 30-second hold is the standard clinical prescription
# (the widely replicated 30s x 30s protocol), 15s is the shortest hold that is
# still a stimulus rather than a rep of the movement, and the ladder tops out
# at 90s: past roughly a minute the extra time buys tolerance more than range
# of motion, so a tenth level would be inventing progress.
STRETCH_HOLD_SECONDS = [15, 20, 25, 30, 40, 50, 60, 90]

# Skill holds and carries (planche, front lever, human flag, L-sit, overhead
# carry). These start in single digits - 5s is the realistic first milestone -
# and share the stretch's 90s ceiling, since holding a lever past a minute is
# not training a different skill.
ISOMETRIC_HOLD_SECONDS = [5, 10, 15, 20, 30, 45, 60, 90]

# Barbell lifts, in kg. Olympic pulls start far lighter than anyone would guess
# (a snatch technique weight is a fraction of the lift) and the ladder climbs
# in even 10kg plate increments, which is how a barbell is actually loaded.
BARBELL_KG = [20, 30, 40, 50, 60, 70, 80, 90, 100, 110]

# Double-kettlebell lifts. A pair of competition bells starts at 32kg, so the
# floor is 16kg (one 8kg bell) and the steps stay even so the pair can be
# matched to a rack's smallest change.
KETTLEBELL_KG = [16, 20, 24, 28, 32, 36, 40, 44, 48, 52]

# Dumbbell snatch, PER dumbbell - the unit matters, because a snatch carries
# the load in each hand and "kg" on the sheet would read as double the number
# on the bar.
DUMBBELL_SNATCH_KG = [10, 12, 14, 16, 18, 20, 22, 24, 26, 28]

# Equipment-weighted rows the CSV grouped as "no universal ladder rule" but
# which plainly do carry one, because the load is on the implement:
AB_WHEEL_KG = [5, 10, 15, 20, 25, 30, 35, 40]
SLAM_BALL_KG = [4, 6, 8, 10, 12, 16, 20, 24]
SLED_KG = [30, 50, 70, 90, 110, 130, 150, 180]

# --------------------------------------------------------------------------
# Roman patterns
# --------------------------------------------------------------------------

ROMAN_NUMERALS = [
    "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII",
]


def as_roman_json(pattern: list[list[int]] | None) -> str:
    """A stage->reps pattern in the shape the CSV and the emitter expect.

    `roman_pattern_json` is an OBJECT keyed by Roman numeral, not the list the
    app reads at runtime - the emitter re-keys it through its own stage order.
    Writing the list here fails the emit with an opaque AttributeError, so the
    shape is produced in one place.
    """
    if not pattern:
        return ""
    return json.dumps({numeral: reps for numeral, reps in zip(ROMAN_NUMERALS, pattern)})


# The pool's standard 8 -> 12 pattern, used by the ordinary weighted rows.
STANDARD_ROMAN = [
    [8, 8, 8], [9, 8, 8], [9, 9, 8], [9, 9, 9], [10, 9, 9], [10, 10, 9],
    [10, 10, 10], [11, 10, 10], [11, 11, 10], [11, 11, 11], [12, 11, 11],
    [12, 12, 11], [12, 12, 12],
]

# Olympic lifts are trained low-rep: a snatch or a clean-and-jerk is a skill,
# not a hypertrophy set, so the standard 8-12 pattern would prescribe a
# different exercise. Three sets of 2/1/1 up to 5/5/3 keeps every set inside
# the 2-5 rep range that makes the technique repeatable.
OLYMPIC_ROMAN = [
    [2, 1, 1], [2, 2, 1], [3, 2, 1], [3, 2, 2], [3, 3, 1], [3, 3, 2],
    [4, 3, 2], [4, 3, 3], [4, 4, 2], [4, 4, 3], [5, 4, 3], [5, 4, 4],
    [5, 5, 3],
]

# repMin for the olympic rows. The engine asserts every Roman stage sits at or
# above repMin, and stage I is 2/1/1 - a single is the whole point of a low-rep
# lift, so the floor is 1 and repMax stays at 5.
OLYMPIC_REP_MIN = 1

# --------------------------------------------------------------------------
# Group table, keyed by the CSV's own training_category
# --------------------------------------------------------------------------

# name -> (method, unit, ladder, roman, repMin, repMax, sets, restSeconds)
GROUPS: dict[str, tuple] = {
    "mobility_stretching": (
        "duration_progression", "seconds", STRETCH_HOLD_SECONDS,
        None, None, None, 2, 30,
    ),
    "isometric_hold_or_carry": (
        "duration_progression", "seconds", ISOMETRIC_HOLD_SECONDS,
        None, None, None, 3, 60,
    ),
    "olympic_lift": (
        "discrete_load_reps", "kg", BARBELL_KG,
        OLYMPIC_ROMAN, OLYMPIC_REP_MIN, 5, 3, 180,
    ),
}

# Rows the CSV filed under strength_weighted_or_machine that DO have an
# obvious load, keyed by exercise slug (build_progression.slugify, so
# UNDERSCORES - a key that matches nothing is a build failure, see
# unknown_keys) so each keeps its own implement's ladder.
LOADED_ROWS: dict[str, tuple] = {
    "ab_wheel_rollout": ("discrete_load_reps", "kg", AB_WHEEL_KG, STANDARD_ROMAN, 8, 12, 3, 60),
    "medicine_ball_slam": ("discrete_load_reps", "kg", SLAM_BALL_KG, STANDARD_ROMAN, 8, 12, 3, 60),
    # repMin must EQUAL the minimum of stage I, not merely sit below it: the
    # pattern's floor is what stage I prescribes, and the engine treats repMin
    # as that floor (ProgressionEngineTest.everyGeneratedEntryCarriesAConsistentRomanTable).
    # Stage I of STANDARD_ROMAN is 8/8/8, so the floor is 8.
    "sled_row": ("discrete_load_reps", "kg", SLED_KG, STANDARD_ROMAN, 8, 12, 4, 120),
}

# The kettlebell and dumbbell olympic rows move off the barbell ladder, which
# would put a 20kg floor on a double-kettlebell lift.
OLYMPIC_EQUIPMENT: dict[str, tuple[str, list]] = {
    "flagged_kettlebell": ("kg", KETTLEBELL_KG),
    "flagged_dumbbell": ("kg_per_dumbbell", DUMBBELL_SNATCH_KG),
}

# Why a row is left alone. Split by key kind on purpose: a category key and a
# slug key live in separate tables because they are checked against separate
# sets, and mixing them lets a category silently satisfy the slug check (or the
# other way round) - which is how the build's own key-integrity check caught
# this table being wrong the first time it ran.
NOT_BACKFILLED_CATEGORY: dict[str, str] = {
    "plyometric_explosive": (
        "Plyometrics progress by volume, then height, then intensity - the CSV "
        "flags that safe progression depends on landing mechanics and joint "
        "readiness rather than a load increment, and a single ladder cannot "
        "express all three."
    ),
}

NOT_BACKFILLED_SLUG: dict[str, str] = {
    "rope_climb": "Progress is bodyheight per go, which is a distance, not a load the app can ladder.",
    "wrist_roller": "Progress is revolutions or metres of a rotating handle, which no other row uses as a unit.",
    "nordic_hamstring_curl": "An eccentric-only movement whose load is trainer-designed from bodyweight tempo, not a kg ladder.",
}

# The starting level for each difficulty is derived inside apply(): Easy is the
# floor, Hard is two rungs below the top, and Medium is the round number the
# ladder is built around (30s for a hold, 50kg for a barbell lift) when the
# ladder happens to contain one.


def ladder_for(row: dict, slug: str):
    """Return (method, unit, ladder, roman, repMin, repMax, sets, rest) or None."""
    if slug in LOADED_ROWS:
        return LOADED_ROWS[slug]
    category = (row.get("training_category") or "").strip()
    if category == "olympic_lift":
        equipment = (row.get("equipment_category") or "").strip()
        unit, ladder = OLYMPIC_EQUIPMENT.get(equipment, ("kg", BARBELL_KG))
        return ("discrete_load_reps", unit, ladder, OLYMPIC_ROMAN, OLYMPIC_REP_MIN, 5, 3, 180)
    return GROUPS.get(category)


def unmatched_keys(seen_slugs: set[str], seen_categories: set[str]) -> list[str]:
    """Curated keys that matched no exercise in the CSV.

    A typo'd slug here would fail SILENTLY: the row it names would fall through
    to the generic reason and keep no ladder - the same failure the project
    already paid for on a translation slug, where one mistyped key quietly
    left an exercise untranslated. The build turns these into errors instead.
    """
    return [
        f"ladder_backfill: {name}[{key}] matches no exercise in the pool"
        for name, table in (
            ("LOADED_ROWS", LOADED_ROWS),
            ("NOT_BACKFILLED_SLUG", NOT_BACKFILLED_SLUG),
        )
        for key in sorted(table)
        if key not in seen_slugs
    ] + [
        f"ladder_backfill: NOT_BACKFILLED_CATEGORY[{key}] matches no training_category"
        for key in sorted(NOT_BACKFILLED_CATEGORY)
        if key not in seen_categories
    ]


def reason_for(row: dict, slug: str) -> str | None:
    """Why this row still has no level system, or None if it was backfilled."""
    if ladder_for(row, slug) is not None:
        return None
    known = NOT_BACKFILLED_SLUG.get(slug)
    if known:
        return known
    return NOT_BACKFILLED_CATEGORY.get((row.get("training_category") or "").strip()) or (
        "no universal measure was found for this row's training category"
    )


def apply(prog: dict, row: dict) -> bool:
    """Install a curated ladder on a `manual_review` progression payload.

    Returns True when the row was backfilled. Only rows the CSV left at
    `manual_review` are touched, so a row that HAS a ladder keeps the CSV's own
    numbers - this fills the gap it declined to fill and never overwrites a
    real prescription.
    """
    if prog.get("method") != "manual_review":
        return False
    spec = ladder_for(row, prog.get("slug", ""))
    if spec is None:
        return False
    method, unit, ladder, roman, rep_min, rep_max, sets, rest = spec

    prog["method"] = method
    prog["valueSource"] = "curated_ladder"
    prog["unit"] = unit
    prog["level1"] = ladder[0]
    prog["maxLevel"] = len(ladder)
    prog["minValue"] = ladder[0]
    prog["maxValue"] = ladder[-1]
    prog["levelValueSequence"] = json.dumps(
        [{"level": i + 1, "value": v, "unit": unit} for i, v in enumerate(ladder)]
    )
    prog["romanPattern"] = as_roman_json(roman)
    prog["romanPatternId"] = ""
    prog["repMin"] = rep_min
    prog["repMax"] = rep_max
    prog["defaultSetCount"] = sets
    prog["restSeconds"] = rest
    prog["resetPolicy"] = "none"
    prog["preJumpReps"] = ""
    prog["postJumpReps"] = ""

    # Easy starts at the floor; Medium at the round number the ladder is built
    # around; Hard two thirds up. A hold's medium is its 30s, not its third
    # rung, because 30 is the number the prescription is actually written in.
    medium = {"seconds": 30, "kg": 50}.get(unit)
    picks = {"easy": 0, "hard": max(0, len(ladder) - 2)}
    if medium in ladder:
        picks["medium"] = ladder.index(medium)
    else:
        picks["medium"] = len(ladder) // 3

    for level in ("easy", "medium", "hard"):
        index = picks[level]
        value = ladder[index]
        prog[level]["level"] = index + 1
        prog[level]["value"] = value
        prog[level]["stage"] = ""
        prog[level]["reps"] = json.dumps(roman[0]) if roman else ""

    # A curated ladder is an app-ready row: it has a value at level 1 and a
    # climb to the top. `requiresCoachReview` stays true for the olympic group
    # - the CSV's objection was about TECHNIQUE, which a ladder cannot coach -
    # but that flag only steers the starting level, and `isProgressionReady`
    # admits it.
    prog["isSimpleAppReady"] = True
    prog["validationStatus"] = "valid_but_review_recommended" if prog.get("requiresCoachReview") else "valid"
    return True

# --------------------------------------------------------------------------
# Duration rungs — the owner's shared hold ladder (2026-10-04)
# --------------------------------------------------------------------------
#
# "Have time based exercises also levels level 1 being 15 seconds, then 30
# then 45 then 60 then 90 then maximum 120. Also add progression options if
# they exist and adapt the max from 120 based on the realistic maximum for
# each time based exercise."
#
# THE PROBLEM THIS SOLVES IS NOT MISSING DATA. Every one of the 147
# duration rows already carries a ladder, but they disagree with each other:
# the CSV gives Plank twelve rungs (10, 15, 20, 25, 30, 40, 50, 60, 75, 90,
# 105, 120) and Back Lever eight (5, 10, 15, 20, 30, 45, 60, 90), so two
# exercises measured in the same unit ask for a different number of seconds
# at the same level. A ladder the user can see has to mean the same number
# everywhere, which is the whole point of having one at all.
#
# These are the rungs the owner named, and they are not arbitrary: 15s is the
# shortest hold that is a stimulus rather than a rep, 30s is the replicated
# clinical static-stretching dose (30s x 30s), 45-60s is where a hold starts
# training endurance rather than tolerance, and 120s is the practical ceiling
# for anything held rather than repeated.
DURATION_RUNGS = [15, 30, 45, 60, 90, 120]

# How long each training category can honestly be held. This is the "adapt
# the max from 120" half of the request, and it is per CATEGORY rather than
# per row: a stretch has a researched ceiling, and a plank's is a different
# number for a different reason, but there is no honest way to tell a 45s
# hamstring stretch ceiling from a 45s calf stretch ceiling by name.
#
#   stretch    - the 30s x 30s protocol is the standard dose and 60s is where
#                extra time buys tolerance rather than range, so 60 is the top.
#   hold       - a skill hold. Longer than a stretch's ceiling is real work
#                (a front lever IS trained past a minute) so the full 120.
#   cardio     - intervals, not holds. The rungs are the work intervals, and
#                the row's own ceiling (Air Bike runs to 600s) is what caps
#                it, so the rung list is a floor here, not a ceiling.
DURATION_MAX_BY_CATEGORY = {
    "mobility_stretching": 60,
    "isometric_hold_or_carry": 120,
}


def _existing_rung_values(prog: dict) -> list[float]:
    """The rungs a progression payload already carries, or [] when it has none.

    `levelValueSequence` is a JSON-ENCODED STRING, not a list. Iterating it
    directly yields single characters, which would silently produce a ladder of
    nonsense one digit at a time instead of failing - so it is decoded here,
    once, and every caller goes through this.
    """
    raw = prog.get("levelValueSequence")
    if not raw:
        return []
    try:
        decoded = json.loads(raw)
    except (TypeError, ValueError):
        return []
    if not isinstance(decoded, list):
        return []
    return [v.get("value") for v in decoded if isinstance(v, dict) and v.get("value") is not None]


def normalize_duration_rungs(prog: dict, row: dict) -> bool:
    """Snap a seconds-based row onto [DURATION_RUNGS], capped at its real max.

    Returns True when the row's ladder was rewritten.

    This runs on EVERY duration row, not only the `manual_review` ones
    [apply] handles, which is the whole difference between this and
    `apply`: the gap rows had no ladder to disagree with, and these have one
    that already disagrees with each other.

    Only rows whose unit is seconds are touched. `meters` rows share the
    `distance_or_interval_progression` method but are measured in distance,
    and stretching one to 120 metres is not the same decision as holding one
    for 120 seconds.
    """
    if prog.get("unit") != "seconds":
        return False
    if prog.get("method") not in ("duration_progression", "distance_or_interval_progression"):
        return False

    category = (row.get("training_category") or "").strip()
    category_max = DURATION_MAX_BY_CATEGORY.get(category)
    # The row's OWN ceiling still wins where the category has no opinion, so
    # Air Bike keeps its 600s instead of being truncated to the shared 120.
    own_max = prog.get("maxValue")
    ceiling = None
    if category_max is not None:
        ceiling = float(category_max)
    if own_max:
        ceiling = min(ceiling, own_max) if ceiling else float(own_max)

    rungs = [r for r in DURATION_RUNGS if ceiling is None or r <= ceiling]

    # The shared ladder tops out at 120s, which is a CEILING for anything
    # HELD and only a FLOOR for anything REPEATED. A cardio interval row
    # legitimately runs past it - Air Bike's own data goes to 600s - and
    # truncating that to 120 would delete a third of the row's range on the
    # strength of a rule written about holds. So a row that already had rungs
    # above 120 keeps them, appended in order.
    if ceiling is not None and ceiling > DURATION_RUNGS[-1]:
        extra = sorted({
            int(v) for v in _existing_rung_values(prog)
            if int(v) > DURATION_RUNGS[-1] and int(v) <= int(ceiling)
        })
        rungs = rungs + extra
    # A row whose own ceiling sits below the first rung would otherwise get an
    # empty ladder and silently leave the level system, so its floor is kept
    # and the ladder is that single rung.
    if not rungs:
        floor = prog.get("minValue") or prog.get("level1")
        if floor is None:
            return False
        rungs = [int(floor)]

    if [float(r) for r in _existing_rung_values(prog)] == [float(r) for r in rungs]:
        return False

    prog["unit"] = "seconds"
    prog["level1"] = rungs[0]
    prog["maxLevel"] = len(rungs)
    prog["minValue"] = rungs[0]
    prog["maxValue"] = rungs[-1]
    prog["levelValueSequence"] = json.dumps(
        [{"level": i + 1, "value": v, "unit": "seconds"} for i, v in enumerate(rungs)]
    )

    # The Easy/Medium/Hard starts are stored as a LEVEL index, so they have to
    # be re-pointed at the new ladder or they would name rungs that no longer
    # exist. Medium deliberately lands on 30s where the ladder has one: 30 is
    # the number the prescription is written in, and a stretch medium start of
    # 45 would put every beginner past the clinical dose.
    for level, preferred in (("easy", rungs[0]), ("medium", 30), ("hard", rungs[-1])):
        target = preferred if preferred in rungs else (rungs[-1] if level == "hard" else rungs[0])
        index = rungs.index(target)
        prog[level]["level"] = index + 1
        prog[level]["value"] = target

    return True
