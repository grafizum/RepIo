# Plan — long level ladders (100 to 500 levels)

Status: **PLAN ONLY.** Nothing in this document is implemented. It was written after the
owner's report on 2026-10-09:

> "i wasnt expecting to only see 6 levels for an exercise, i want you to think of a plan on
> how we can have lots of levels for up to 200 or 300 or 500 for like body builders so it
> gives more of an accomplished goal to reach level 100 or so (only as a plan for now dont
> do anything with this idea)."

The trigger was real: the level panel for Superman lists six rungs, because Superman's ladder
*is* six rungs. The question is what a bigger number should mean.

---

## 1. Why the ladders are short today (measured, not guessed)

From `tools/exercise-db/out/progression_v5.json` (601 rows, the data the app ships):

| what | value |
| --- | --- |
| rows per declared ladder length | 4 → 76 rows, 5 → 16, 6 → 156, 10 → 31, 11 → 12, 15 → 64, 20 → 57, 30 → 115, 40 → 61 |
| rows with a researched load ceiling | 478 of 601 |
| unit families | seconds 137, variation_step 123, kg_total_barbell 101, kg_per_dumbbell 76, stack_step 64, kg_per_kettlebell 52, resistance_step 17, kg 16, meters 10, kg_total_load 5 |

Two different things cap a ladder, and they are easy to confuse:

1. **The declared length** (`maxLevel` in the row): 4 for plyometrics, 6 for most bodyweight
   rows (Superman: 6), 30 for a machine stack, 40 for a curated barbell ladder.
2. **The humane ceiling** ([ProgressionEngine.effectiveMaxLevel]) — the number the app
   actually stops at, and the number the level panel lists. It is derived from
   `ceiling.load` (e.g. Barbell Bench Press: ladder starts at 20 kg, ceiling 180 kg) or from
   the rep climb for rep ladders (top rung 50 reps). Owner ruling 2026-10-04: *"i want each
   exercise to have their cealing to be humane and nothing over humanly possible"*.

So a long ladder is not a display problem. It is a question of **how many genuinely different
prescriptions exist between "first session" and "as strong as a human gets"**, and today the
answer for a curl is 30 and for a Superman it is 6.

### What a smaller step would buy (kg rows only, ceiling − start ÷ step)

| step | median levels | max | rows that reach 100+ |
| --- | --- | --- | --- |
| 2.5 kg | 37 | 61 | 0 of 16 |
| 1.0 kg | 91 | 151 | 1 of 16 |
| 0.5 kg | 181 | 301 | 9 of 16 |

Read that carefully: the reason a barbell lift has 37 levels and not 300 is the plate on the
bar, not the app. Halving the step doubles the count and halves the meaning of each level.

---

## 2. The design question, stated plainly

A level must be **a prescription the user can actually take** and **a step they can feel**. Any
scheme that reaches 100+ must answer both, or "level 100" becomes a spreadsheet number.

Three levers exist:

- **L — load.** More rungs between floor and ceiling (smaller increments, micro-plates).
- **R — reps.** More rungs per load (double progression: the app already runs 13 stages per
  level; the user sees the five rounded ones).
- **W — work.** Sets, tempo, density, range of motion, unilateral variants, rest. This is the
  lever with the most room and the least machinery today.

## 3. Options

### A. Finer load increments (L only)
Feed the existing ladder a 0.5–1 kg grid instead of 2.5 kg.
- Buys: ~2–5x the levels on the 292 kg-unit rows. Median 181 at 0.5 kg.
- Costs: a home lifter with a pair of 5 kg plates cannot take those steps, so the ladder
  would prescribe unloadable rungs. Progress on paper, none on the bar.
- Verdict: useful as an ingredient, dishonest as the whole answer.

### B. Compose L × R × W into a "level budget" (all three)
Level = a point on a 2-D (or 3-D) plan, generated from the exercise's own floor, ceiling,
increment and variant space:
`level → (load rung, stage, set count / tempo / density variant)`.
- A curl: 4 → 50 kg at 1 kg (47 rungs) × 2 set counts = ~94 levels.
- A bench: 20 → 180 kg at 2.5 kg (65 rungs) × hard/easy rep bands = ~130.
- A Superman: 3 variation steps × rep climb 8→50 × set counts / tempos = ~40+ levels (this is
  the family that most needs the W lever, since it has neither load nor much variation).
- Buys: 100+ per exercise without inventing fake steps, and every rung is still loadable.
- Costs: the level is no longer "a weight" — it is a prescription tuple. That changes what the
  card prints, what the picker lists, and how `ProgressionEngine.levelUp` decides the next
  rung. It is the real work, and it is a data-model change, not a UI change.

### C. Keep per-exercise ladders honest; make the LONG ladder the career level
Per-exercise ladders stay capped by their own evidence (6–40, as now), and the milestone the
owner wants lives in the **Overall Level** (which already exists on Home): it already sums the
per-exercise levels, so it can legitimately reach 100+ ("Level 100 = 40 exercises trained").
- Buys: the achievement feeling with no new prescription machinery, and nothing to re-tune.
- Costs: it does not give a single exercise 500 rungs, which is what was asked.
- Verdict: worth doing *in addition* (see Phase 1.5) but not as the answer.

### D. Prestige / titles per ladder
Whatever the length, add named milestones (Level 25 Iron, 50 Bronze, …) and derive a
"career" number. Purely presentational, and it is what makes a long ladder feel like a climb.

## 4. Recommended shape

**B as the engine, A as one of its levers, C+D as the achievement layer.**

Concretely:

1. Give every ladder a **level budget** computed from its own data:
   `budget = rungs(load grid) × rep bands × work variants`, clamped to a per-family cap
   (e.g. 200 for load ladders, 100 for rep ladders, 60 for pure variation ladders), and
   never past the humane ceiling.
2. Generate the ladder as a **list of prescriptions** (not a formula read at call time), so
   the picker, the card and the ceiling rule all read the same table. This is what makes
   200 rows storable, testable and re-tunable.
3. Store levels as **indices into that table** and keep the existing `(level, stage)`
   preferences working by mapping the old index onto the new table (an explicit migration
   table, because a stored "level 4" for a bench must not silently become a different weight).
4. Make the headline number the ladder length: "Level 42 of 137", and put the named
   milestones from D on the way. The Overall Level (C) becomes the 100+ number for Home.

### What must change, file by file (when this is built)
- `ProgressionEngine.effectiveMaxLevel` / `maxLevel` / `valueAtLevel` — become readers of the
  generated table instead of calculators over `(floor, increment, ceiling)`.
- `ProgressionEngine.levelUp` — advances the table index; the stage reset and the rep-scaling
  rules (still needed for rep ladders) move into the generator.
- `WorkoutViewModel.progressionMaxLevel`, `levelDetails`, `patternStageCount` — unchanged in
  shape, but their numbers grow, so `LevelDetailsDialog` must stay lazy (it is) and the
  picker's opening window (2 rungs of lead-in) still has to make sense on a 200-row list.
- `tools/exercise-db/*` — the CSV/JSON gains the variant and band columns the generator needs;
  this is where the tuning lives, and it is already generated + validated.
- Strings/UI: "Level N of M", milestone names, and a level-up acknowledgement worth having at
  a milestone (the queued "celebrate the advance" prompt is a natural companion).

## 5. Phasing

| phase | deliverable | success test |
| --- | --- | --- |
| 0 | this document; decide B vs C with the owner | — |
| 1 | generator emits a prescription TABLE per exercise; engine reads it; ladders stay the same length | every existing ladder reproduces today's prescriptions exactly (golden test over all 601 rows) |
| 2 | add the R and W levers (rep bands, set counts, tempo/density) to the generator | a curl and a Superman both exceed 100 levels with every rung loadable/doable |
| 3 | migration of stored `(level, stage)`; "Level N of M" in the card, picker and panel | logging a set for a migrated user lands on the same prescription as before the migration |
| 4 | milestones + overall career level | a fresh user reaches Overall Level 100 in a simulatable number of sessions |
| 5 | tune with real logs (which steps are actually taken?) | measured: median time per level, share of rungs ever used |

## 6. Risks

- **Physiology**: a 1 kg step on a curl is a real step; a 1 kg step on a squat is noise. The
  step must be per-family, and the programme must not promise progress the body cannot deliver
  faster than a training cycle.
- **Loadability**: increments below the smallest plate pair turn levels into fiction
  (Option A's trap). W variants (tempo, pauses, ROM, unilateral) must be offered *before*
  sub-plate weight steps.
- **The ceiling rule**: more levels must not move the ceiling. The ceiling is the anchor; the
  ladder is the path to it.
- **Re-tuning existing users**: 478 rows with ceilings × stored per-exercise states means every
  ladder edit re-maps somebody's position. The migration table in Phase 3 is not optional.
- **Combinatorics in the UI**: 200 rungs must stay a scrolling list with a current marker, not
  a wall of numbers — the rounding rule (`roundedStages`) and the windowed editor already point
  the right way.

## 7. What I would NOT do

- Not multiply levels by shrinking the step alone (A): the ladder would prescribe weights the
  user's plates cannot make.
- Not give short ladders fake filler (variation repeats, tempo noise) just to reach a number.
- Not raise the humane ceiling to make longer ladders possible — the ceiling is the safety
  property, and the owner set it in the other direction on purpose.

## 8. Open questions for the owner

1. Should the 100+ number be **per exercise** (B) or the **Overall Level** (C)? (Recommended:
   B for the exercise, C as the Home headline.)
2. What are the family caps — is 200 enough, or is 500 wanted for the big barbell lifts?
3. Are milestones wanted as titles (Iron/Bronze/Silver/Gold/Elite) or just round numbers?
4. Should a long ladder be **unlocked gradually** (level 100 visible but greyed as a goal), or
   should the whole ladder be visible from day one?
