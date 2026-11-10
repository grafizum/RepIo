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

## 2026-11-10 — what the level ladder actually does (and a wrong turn, corrected)

Owner prompt: *"do the levels right now"*. The first reading of the data was wrong. It is
recorded here so nobody repeats it.

**THE WRONG FIRST READING.** The CSV carries a precomputed `level_value_sequence_json` per
row (10-11 rungs, family templates such as barbell 20→60) and nothing in the pipeline
regenerates it. Measured against the declared `level_1_value` /
`maximum_progression_value`, **297 of 314 load rows "disagreed" with their own sequence**,
so the conclusion drawn was that the 2026-10-08 rebuilds (floor and ceiling) had written
numbers the ladder never reads — that the app still shipped a 20 kg curl floor and a 60 kg
bench ceiling.

**That conclusion was false, and `ProgressionEngine` says so in its own code:**

- `constantStepLadder(entry)` returns `(base = level1, step = the smallest of the row's own
  increment and its finest authored step)`, quantised to 0.5 kg for kg units.
- `valueAtLevel`, for a LOAD row, returns `base + step * (level - 1)`. The authored sequence
  is **not** the path the ladder walks — owner ruling 2026-10-13: *"the increase of the
  weight should always be the same … add more levels since the max will be the same but the
  steps in between will be more"*.
- `effectiveMaxLevel` derives the RUNG COUNT from the ceiling:
  `round((top - base) / step) + 1`.

So the declared floor and ceiling are exactly what ships: the 10 kg EZ-bar curl floor does
reach users, and the bench starts at 20 kg and climbs a flat 2.5 kg per level to its 100 kg
ceiling. The authored sequence survives only as (a) the fallback for a row that declares no
increment and (b) the source of the finest step.
`LevelLadderTest.everyRungIsExactlyOneStepAboveTheOneBelowIt` pins this, and it failed the
moment the sequences were regenerated with escalating steps — which is how the mistake was
caught before it went anywhere.

A regenerator (`rebuild_level_rungs.py`, escalating 2.5→5→10 steps) was written, run, and
**reverted**: the CSV was restored from `exercise_progression_database_v5.csv.prerungs-backup`,
the generated Kotlin re-emitted, and the five level suites went green again. Nothing in this
tree carries the wrong change, and the wrong-premise guard script was deleted rather than
left failing on correct data.

**WHAT IS ACTUALLY STALE — a tidy-up, not a bug.** 297 of 314 rows still carry a legacy rung
list that no longer matches their declared floor and ceiling. It is not walked, but it
misleads anyone reading the CSV and it supplies `finestAuthored`. If it is ever rewritten it
must be to the SAME uniform ladder the engine builds (`base + step * (level - 1)`), and it
must keep a row's last authored value BELOW the declared top: `LevelLadderTest` asserts the
ceiling rung is heavier than `levelValues.last()`.

**WHAT REMAINS FOR THIS CARD — nothing about rungs.** The ladder already opens at each row's
own floor and climbs in equal steps to its sourced ceiling. If level 1 still feels too heavy
on some exercise, the lever is that exercise's FLOOR: authoring the sourced min/max table
(Tier A/B of this plan), which is what the card asks for. The ceiling half is already done
by `rebuild_maxima.py`.

## Not in this phase

- The `effectiveMaxLevel` clamp fix (287 rows / 4,684 dead levels) — owner deferred it.
- Overall Level is computed from the kilogram ladder, so re-authoring values *will* move
  users' Overall Level. That is unavoidable and needs one sentence in the release note.

## 2026-11-10 — Tier A authored: the sourced table exists

Owner prompt: *"do the level task now"*. `tools/exercise-db/data/level_minmax_sources.csv`
now holds one row per primary lift (40 rows): name, unit, min, min_source, max, max_source.
`tools/exercise-db/level_minmax.py` loads it inside `build_progression.py`, and **an empty
source, an unknown name, a unit mismatch, a minimum below the implement's smallest real
weight, or fewer than four rungs of room all fail the build** — an uncited number cannot
ship. That loader is also the guard the plan asked for in step 4; it reads the same numbers
the app reads, so it cannot drift from them the way a hand-written test can.

Two values move per row: `level1` (first rung) and `maxValue` (top). The increment is
untouched, because the engine builds the rungs itself.

**13 rows changed.** The ones the family rules had got wrong:

| row | was | now | why |
| --- | --- | --- | --- |
| Romanian Deadlift | 180 | 140 | the deadlift rule matched "deadlift" first: an RDL is the hinge standard (1.75xBW), not the competition pull (2.25x) |
| Stiff Leg Deadlift, EZ-Bar RDL, Smith RDL | 180 | 140 | same rule-order bug |
| Bent-Over Barbell Row | 60 | 80 | row standard 1.00xBW; the rule's pattern never matched "bent-over barbell row" |
| T-Bar Row | 60 | 80 | supported row, same standard |
| Close-Grip Bench Press | 100 | 90 | close-grip is ~90% of the flat bench |
| Incline Barbell Bench Press | 100 | 90 | incline is ~90% of the flat bench |
| Sumo Squat | 180 | 140 | "sumo" matched the deadlift rule; a sumo squat is a squat |
| Pause Squat | 60 | 120 | no rule matched; a paused squat keeps ~85-90% of the squat ceiling |
| Heel-Elevated Squat | 60 | 140 | no rule matched; it is a squat |
| Smith Machine Split Squat | 140 | 100 | a split squat is single-leg (1.25x), not a bilateral squat (1.75x) |
| Dumbbell Shoulder Press | 36 | 28 | per-hand press 0.35xBW; 36 was the compound cap, which is a bench, not a press |

**The 11 curated elite ceilings this table displaces.** `progression_branches.CEILING_OVERRIDES`
carried elite marks (squat 220, deadlift 250, bench 180, press 80, row 140, RDL 200, lunge 140,
DB bench 60/hand, DB press 45/hand, goblet 60) which the engine prefers over `maxValue` when it
sizes a ladder. Two caps for one lift is the same contradiction that produced the wrong turn
recorded above, so the sourced top now wins for the rows this table covers and those elite
marks no longer size them (squat: 81 levels -> 49, deadlift 93 -> 65, bench 65 -> 33). They are
listed here so restoring any of them is a one-line decision, not an excavation — **that is the
owner's call**, if the elite marks were meant to be the top.

**Migration.** A user keeps their level NUMBER; only the kilograms behind it change, so
Overall Level moves. One sentence in the release note, as "Not in this phase" already requires.
