# Level-system remake — Phase 2: sourced per-exercise minimums and maximums

**Owner's requirement (2026-10-08):** *"keep the picker at medium, what I want is the
starting level 1 and max level to be remade based on true online researches that give the
minimum weight or rep or anything for that specific exercise and the max weight, rep, time
etc. for each of those 500+ exercises, so that the level system wouldn't start randomly at
20kg which would be way too heavy for a beginner exercise."*

So the level-1 (minimum) and top (maximum) of **every** ladder is to be grounded in
published figures, per exercise — not the CSV's current heuristics. Phase 0 measured what
ships today; this is the rebuild.

## What is wrong today (from the Phase 0 audit)

| Family | n | level-1 min | level-1 median |
| --- | --- | --- | --- |
| Barbell | 89 | 10 kg | **20 kg** (the Olympic bar) |
| Dumbbell | 77 | 2 kg | 2 kg |
| Kettlebell | 56 | 4 kg | 6 kg |
| Machine | 19 | 10 kg | 10 kg |

The barbell row is the honest report: **20 kg is the Olympic bar**, so it is a real floor
for a squat or a deadlift — but it is *not* a beginner's floor for a **wrist curl, a
preacher curl, a front raise or an upright row**, where the implement should be an EZ-bar
or dumbbells. The rebuild has to know the *implement* the exercise is actually loaded
with, and the *movement's* real beginner load, not one number for "Barbell".

## Deliverable: one row per exercise, with sources

A table (CSV in `tools/exercise-db/data/`) with one row per pool exercise:

```
slug, name, equipment, unit,
min_value, min_unit, min_source, min_note,
max_value, max_unit, max_source, max_note,
rep_min, rep_max, rep_source,        # for rep ladders
hold_min_s, hold_max_s, hold_source  # for duration ladders
```

`min_source` / `max_source` are **required** and must name a real reference (a standards
table, a published novice/advanced load, or a training-textbook range) — an entry with an
empty source is a build failure, which is what stops this drifting back into invented
numbers.

## Source hierarchy (in order)

1. **The exercise's own researched ceiling** already carried in the CSV
   (`maxValue` / `maximum_progression_value`) where it is real, cross-checked.
2. **Movement-family standards** for the common lifts: novice→advanced multiples of
   bodyweight (or a %1RM band) published for squats, deadlifts, presses, rows, pull-ups,
   etc. These give the *ceiling* directly and the *floor* as the novice rung.
3. **ACSM/NSCA beginner loading guidance** for the floor of accessory work
   (a beginner trains in a comfortable ~60–70% 1RM band with an emphasis on technique),
   which translates to a conservative starting load per implement.
4. **Implement floors** — a hard fact, not a judgement: an Olympic bar weighs 20 kg, an
   EZ-bar ~10 kg, a standard dumbbell rack starts at 1–2 kg, a kettlebell at 4–8 kg, a
   selectorized stack's lightest pin is often 5–10 kg. **A minimum can never be below the
   smallest weight the implement exists in**, and where the movement's real floor is below
   its current implement, the ladder must change the *implement it recommends* (barbell
   curl -> EZ-bar/dumbbell) rather than claim a 5 kg "Olympic bar".
5. **Rep ladders:** ACSM's muscular-endurance band (15–25) and the hypertrophy band (8–12)
   give the useful range; the floor stays the pool-wide `DEFAULT_REP_FLOOR` of 8 (owner
   rule: "1 rep is basically nothing").
6. **Holds:** the clinical static-stretch range (15–60 s) for mobility, the full 15–120 s
   band for isometric holds — already the SHARED duration ladder, so only the per-row
   floor/ceiling needs citing.

## Execution, in tiers (not 601 hand-written numbers in one go)

- **Tier A — the ~35 primary lifts** (squat, deadlift, bench, press, row, pull-up, the
  big machines): individually sourced min and max. These are the ones a user notices.
- **Tier B — the remaining load rows (~114):** inherit their movement family's template,
  adjusted by implement. Every one still carries its own source note.
- **Tier C — the 315 rep/variation rows:** family rep-range template + the pool-wide floor.
- **Tier D — the 137 duration rows:** the shared seconds ladder, ceiling by
  `training_category` (already normalised), floor cited once per category.

## Pipeline & verification

1. Author the table, then load it in `tools/exercise-db/build_progression.py` **overriding**
   the CSV's `level_1_value` / `maximum_progression_value` for the covered rows.
2. Re-run `build_progression.py` → `emit_progression_kotlin.py`.
3. Tests that must stay green: `LoadGridTest`, `WeightUnitTest`, `RepProgressionOfferTest`,
   `LevelLadderTest`, `LevelSystemAuditReportTest`.
4. **New test:** every load row's level-1 value is `>=` its implement's smallest real
   weight **and** `<=` a beginner ceiling for its family — i.e. no ladder opens on a weight
   the exercise's own implement cannot even make.
5. **Migration:** the values change but the stored *level numbers* do not, so a user at
   level 4 keeps "level 4" and gets the new level-4 load. State this in the release note.
   The separate dead-level (clamping) fix remains parked per the owner's 2026-10-08 call.

## Not in this phase

- The `effectiveMaxLevel` clamp fix (287 rows / 4,684 dead levels) — owner deferred it.
- Overall Level is computed from the kilogram ladder, so re-authoring values *will* move
  users' Overall Level. That is unavoidable and needs one sentence in the release note.
