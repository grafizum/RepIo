# Level-system & equipment — Phase 0 audit

**Date:** 2026-10-08 · **Board tasks:** "Level 1 is too high for most exercises…",
"Inverted row requires a machine… (update the database in general)", "I only have chair
and dumbbell yet the system asks me to do an incline dumbbell press".

Phase 0 is **measurement only** — nothing in the database has been changed. Every number
below comes from the app's own catalog, equipment gate and progression engine.

## How to reproduce

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*LevelSystemAuditReportTest*"
```

That writes `composeApp/build/reports/level-system-audit/rows.csv` (one row per pool
exercise, 17 columns) and `summary.txt`. The test asserts only two invariants — the pool
is 601 rows and every one participates in the level system — so it cannot be "passed" by
softening a threshold.

## Pool shape

| Bucket | Count |
| --- | --- |
| Total rows | 601 |
| Progression-ready | **601** (none outside the level system) |
| Load rows (kg / pin / band) | 149 |
| Duration rows (seconds) | 137 |
| Other (variation / bodyweight reps) | 315 |

## Finding 1 — the level-1 loads are mostly fine; the *start level* is the problem

Level-1 loads by equipment (kg rows):

| Equipment | n | min | median | max |
| --- | --- | --- | --- | --- |
| Barbell | 89 | 10 | **20** (the empty bar) | 20 |
| Dumbbell | 77 | 2 | **2** (per hand) | 10 |
| Kettlebell | 56 | 4 | 6 | 16 |
| Machine | 19 | 10 | 10 | 10 |
| Other | 8 | 2.5 | 2.5 | 30 |

The authored **level 1 is already a beginner value** (2 kg dumbbells, a 20 kg empty bar).
What the user actually sees first is **level 4** — the CSV's medium rung, which is
**2.0–2.5× level 1** on every weighted row (median ratio **2.00**, `n=250`). So a fresh
dumbbell row opens at ~8 kg per hand, which reads as "level 1 is too high".

**RESOLVED 2026-10-08:** the picker stays at Medium. The owner's actual requirement is to
**re-author each ladder's minimum and maximum from published references** ("minimum weight
or rep or anything for that specific exercise and the max weight, rep, time etc. for each
of those 500+ exercises"), so no ladder opens on a weight a beginner cannot lift. That is
Phase 2 — see [level-system-phase2-level-rebuild.md](level-system-phase2-level-rebuild.md).

So the level-1 numbers DO get rewritten wholesale — per exercise, with a cited source each,
not by a global cap.

## Finding 2 — 287 rows carry 4,684 levels that change nothing

The audit reproduces the figure already recorded in `AGENTS.md`:

- **rows affected: 287**
- **total dead levels: 4,684**

These are levels whose prescribed load equals the level below (`effectiveMaxLevel` runs
past the authored ladder and clamps at the researched ceiling). Fixing it means ending a
ladder where the ceiling is reached — which moves `effectiveMaxLevel` for 287 rows and
therefore every user's **Overall Level**. That is an owner decision with a migration story,
not a silent cleanup.

## Finding 3 — 12 loaded movements leak to a "chair, no bench" kit

With a kit of **dumbbells + a chair (no bench)**, 80 dumbbell rows are offered. Twelve of
them are **bench movements** whose angle or elbow support is the point:

```
Chest Supported Dumbbell Shrug
Chest-Supported Dumbbell Row
Close-Grip Dumbbell Bench Press
Decline Bench Press
Decline Dumbbell Fly
Dumbbell Bench Press
Dumbbell Bench Pull
Incline Dumbbell Curl
Incline Dumbbell Fly
Incline Dumbbell Press        <- the one the owner reported
Incline Hammer Curl
Single-Arm Chest-Supported Dumbbell Row
```

**Root cause:** `ExerciseEquipmentTags.requirementsFor` gates all of them with
`chairOrBenchWith(DUMBBELLS_ONLY)` — i.e. *{chair + dumbbells} OR {bench + dumbbells}* — so
a chair satisfies an incline press. A chair is not a bench; for an incline/decline/chest-
supported **loaded** movement it is not a valid substitute.

Deliberately **excluded** from the leak list: furniture movements where a chair genuinely
works (Incline/Decline Push-Up, Bench Dips, Decline Crunch, Bench Leg Pull-In) and the
~14 "Bench … Stretch / Pose" rows. Those are correctly gated.

## Proposed Phase 1 (equipment) — targeted, then data-driven

1. **Immediate:** for the 12 angle/support movements, require `BENCH` (drop the chair
   alternative) instead of `chairOrBenchWith`. One rule change, pinned by a sweeping test.
2. **Data model:** give every pool row an explicit authored requirement in the pipeline
   CSV, and make `requirementsFor` a *fallback for user-created exercises only*. The gate
   currently infers a row's kit from words in its name, which is the design that produced
   three documented leak episodes.
3. **Test:** sweep every pool row — each is reachable by some kit, and no row requires
   furniture its own equipment category does not imply.

## Proposed Phase 2 (levels)

1. Decide Finding 1's lever (start level vs medium re-authoring).
2. Decide the clamping fix (Finding 2) together with the Overall-Level migration.
3. Re-ground the ladders through the existing pipeline
   (`ladder_backfill.py` → `build_progression.py` → `emit_progression_kotlin.py`), with
   sources cited per tier rather than 601 hand-written numbers.

## Open decisions for the owner

- **Finding 1:** which lever (start at level 1 / cap the opening jump / re-author medium)?
- **Finding 2:** accept the Overall-Level shift and end ladders at the ceiling?
- **Phase 1:** do the 12-row targeted fix now, or wait and land the explicit-requirement
  data model in one pass?

---

## Filter consistency: every dimension, not just furniture (2026-10-08)

Phase 1 item 3 above is now a **standing** sweep rather than a one-off, because the
chair/bench report was one instance of a class that also ran in the opposite
direction. `FilterConsistencyAuditTest` asserts four properties and writes the full
map to `composeApp/build/reports/level-system-audit/filter-leaks.txt`:

| # | Property | What it caught |
|---|---|---|
| 1 | every tag any requirement names is granted by **ticking** some tile | `kettlebell_and_bench`, `dumbbells_and_floor`, and the eight selectorized stations were demanded but granted by no tick — every row behind them was **invisible** to a tile-built kit |
| 2 | no family's pure kit reaches another family's row | one row: `Decline Bench Press` [Dumbbell] also accepted a rack |
| 3 | the strict bodyweight kit reaches nothing that *names* kit or furniture | `Decline Crunch` (no surface) and nine `Bench …` stretches |
| 4 | every preset × every split leaves no part without a gated exercise | **no gate bug**; one data gap, below |

**Fixes landed** (all in `data/ExerciseEquipmentTags.kt`, each pinned by a test):

- `Decline Bench Press` now requires `{bench + dumbbells}` only. The rack
alternative was the *barbell* row's gate; `Decline Barbell Bench Press` and
`Decline EZ-Bar Bench Press` are their own catalog entries, so a barbell-only user
was being offered a row filed under the Dumbbell family.
- `Chest-Supported Kettlebell Row` now asks `{bench, kettlebell_only}` — the same
shape as the dumbbell bench rows. No tile's tick grants `kettlebell_and_bench`, so
a user who ticked Kettlebell **and** Bench could never reach it.
- `Decline Crunch` requires furniture. It fell through the bodyweight branch to the
**empty** requirement and was therefore offered to a user with no furniture at all.
- The nine `Bench …` stretches (chest, lat, calf, adductor, glute, couch, figure-4,
ankle, Bulgarian split) require furniture. They are catalogued under `Other`, whose
branch never read the word, so the empty kit reached them. The gear check stays
**before** the furniture check, or `Stair Climber` would become a flight of stairs.
- Ticking **Machines** now grants the eight stations the tile stands for, and ticking
**Dumbbells** grants the floor variant. Both are needed only with no extra kit, and
tick-then-untick is asserted to be a no-op so removal still works — the reason the
grant was narrowed in the first place was removal, and the narrowing fixed removal
by breaking display. This is the one item here that reverses a documented choice;
see AGENTS.md for the corrected rule.

**Left alone deliberately.** The ~14 `Bench …` stretches that a *chair* genuinely
serves stay chair-OR-bench, and the incline/decline **push-ups** are untouched: the
furniture is only something to put a hand or a foot on for those.

**One real gap remains, and it is data, not a filter.** No biceps movement in the pool
has an empty requirement — every biceps row needs dumbbells, a bar, a cable, a
kettlebell, rings, a pull-up bar or a machine — so an equipment-free user's pull/arm
day silently loses that slot. The audit pins the gap exactly (`Biceps` and nothing
else), which fails both when a new hole appears and when the gap is filled. Filling it
needs an authored equipment-free row (a self-resisted or towel/bag curl), which is a
data decision, not a gate change.
