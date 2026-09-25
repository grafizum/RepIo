# v5 Progression Database Integration — Internal Note

**Date:** 2026-09-19 · **Branch:** `experiment/csv-exercise-db`

## What changed

The built-in exercise pool was **completely replaced** with the v5 progression
database (`exercise_progression_database_v5.csv`, 601 rows). The old
megaGymDataset pool (1,470 exercises) is gone; the v5 CSV is now the single
authoritative source for which exercises exist, what they train, and how each
one progresses.

### Tables / files changed

| File | Change |
| --- | --- |
| `tools/exercise-db/data/exercise_progression_database_v5.csv` | The source CSV, checked in. |
| `tools/exercise-db/build_progression.py` | NEW — import/validate/map pipeline. |
| `tools/exercise-db/emit_progression_kotlin.py` | NEW — Kotlin emitter (replaces `emit_kotlin.py`). |
| `composeApp/.../data/ExerciseCatalog.kt` | REGENERATED — 601 entries with `id`, name, precise muscle, equipment category, training type, mechanic, description. |
| `composeApp/.../data/ExerciseProgression.kt` | NEW — the per-exercise progression payload (all JSON pre-parsed into Kotlin lists). |
| `composeApp/.../data/ProgressionTables.kt` | NEW — deduped Roman patterns + level ladders (1 pattern, 22 ladders cover all 601 rows). |
| `composeApp/.../data/ProgressionEngine.kt` | NEW — the level-up system (see below). |
| `composeApp/.../data/WorkoutTemplates.kt` | Starting loads now come from the CSV prescriptions; beginner builds filter to `beginner_build_eligible`; template/default exercise names updated to v5 spellings. |
| `composeApp/.../ui/viewmodel/WorkoutViewModel.kt` | Persists `level,stage` per exercise; suggests the prescription's load and per-set targets; advances levels after completed stages. |
| `composeApp/.../ui/screens/WorkoutNowScreen.kt` | Shows the current set's prescribed target reps next to the plan range (developer skin also shows `L{n} · {stage}`). |
| `composeApp/.../data/DefaultData.kt` | Default routine uses v5 exercise names. |

No Room schema change: the routine/log tables (`routine_days`,
`routine_exercises`, `logged_sets`) are untouched, so existing logged workouts
keep working as-is. Historical logs are never rewritten — progression applies
from now on.

### Import stats

- **601 rows imported**, all validated (JSON parses, stage/rep pairs agree
  with the Roman table, validation flags coherent). A row that fails any
  check fails the whole import (`build_progression.py` exits non-zero and
  lists the exact rows).
- **Taxonomy mapping:** v5 `equipment_category` → the app's 8 chips
  (`machine_or_cable` split by `equipment_raw` into Cable/Machine); v5
  anatomical muscles → the app's 19 precise muscles (`hip_flexors` folds into
  Abs; ambiguous multi-mover rows resolved by movement pattern).
- **Descriptions:** the v5 CSV has none. **208 of 601** merged from the old
  catalog: 147 by exact slug, 8 by plural/punctuation normalization, 3 by
  token reorder, 50 via a reviewed alias table (e.g. `Lat Pulldown` →
  `Lat Pull-Down`, `Banded Good Morning` → `Band Good Morning`,
  `Pec Deck` → `Pec Dec`). **393 rows have no description** — they are new
  rows the old pool never had; texts are a TODO (see unmatched_v5.csv).

### Exercises needing manual review

`tools/exercise-db/out/unmatched_v5.csv` lists the 393 rows without a merged
description. Notable gaps: `Ab Wheel Rollout` (alias maps it but the old row
carried a duplicate-family text — verify), `Air Bike`, `Machine Assisted
Dips`, `Assisted Pull Ups`, most `Banded …`/`Bench …` stretches and the
machine/cable isolation work. Adding a description = one alias entry or one
row in the old catalog; re-run the two pipeline scripts.

## How the progression logic works

- **Starting prescription.** Easy/Medium/Hard map to the CSV's
  `easy_/medium_/hard_start_value` (+ main level, stage, target reps). The
  live-workout screen suggests that load; a remembered per-exercise weight
  still wins, then the plan's explicit weight, then the prescription, then
  the legacy equipment-class estimate (custom exercises only).
- **Rep progression.** Each exercise carries a Roman pattern table
  (`standard_3x8-12_v1`: I `[8,8,8]` → II `[9,8,8]` → … → XIII `[12,12,12]`).
  The current set's target comes from the pattern row; no Roman numerals are
  shown outside the developer skin.
- **Level-up.** When every set of the current stage met its target (the CSV's
  `meet_or_exceed_every_set_at_required_load`), the stage advances; at the
  top stage the level advances and the load moves to the next value of the
  explicit level ladder (`level_value_sequence_json`).
- **Large jumps.** The jump class is derived from the ladder's own values
  (small <10%, moderate 10–30%, large 30–75%, very_large ≥75% — boundaries
  measured from the CSV). Large/very-large jumps reset reps to the row's
  `post_jump_target_reps_json`.
- **Data-error contract.** A missing/ragged pattern row returns null → no
  auto-progress, fallback to the routine's generic target. Rows with
  `requires_coach_review` or a failed validation status never participate.
- **Persistence.** `progression_state_<name>` in SharedPreferences as
  `level,stage`; first session derives it from the setup difficulty.
  Replacing the routine resets it.

## Testing

- `ProgressionEngineTest` — prescriptions, stage walk, level-up + reset,
  ladder end, coach-review exclusion, state round-trip, Roman-table
  consistency for every generated row.
- All pinned data tests updated to v5 names (`Barbell Back Squat`,
  `Dumbbell Bicep Curl`, `Chin-Ups`, `Bench Dips`, …). **192 tests, green.**
- Manual smoke matrix from the brief (curl / bench / assisted dips / push-up
  / air bike) is covered by the engine tests for prescription + progression
  + reset behaviour; on-device session walk still to be done by hand.

## Known limitations / TODOs

1. **393 descriptions missing** — the biggest UX gap; needs authoring or a
   new description source for the v5 rows.
2. Exercise localization tables (de/it) are empty (clean slate from the
   previous rebuild); refill keyed by the new slugs.
3. `include_in_cardio_v1` / `include_in_mobility_v1` are imported but not
   yet used to gate builds (strength filter uses `beginner_build_eligible`).
4. The variation ladders (`variation_sequence_json`, e.g. Push-Up's 17
   rungs) are stored but not surfaced; the CSV flags them for coach review
   anyway, so they are excluded from progression until reviewed.
5. Machine stack steps are ordinal pin positions (CSV note), shown as-is;
   a kg display would be wrong and is deliberately avoided.

## Frontend — level system in the UI (2026-09-19)

### My Workout tab (`WorkoutNowScreen` / `WorkoutViewModel`)

The reactive parameter loader in `WorkoutViewModel` now fills five new
`WorkoutNowState` fields per exercise — `progressionLevel`, `progressionStage`,
`progressionStageTargetReps`, `progressionPrescribed` (a typed
`ProgressionEngine.PrescribedValue`), and `progressionIsFirstSession`. The
stored level/stage is part of the loader's change key, so a persisted level-up
refreshes the display without any routine-entity change. The UI never calls the
engine for values and never sees raw JSON, jump ratios, or flags.

What the header now shows, under the exercise name:

- A **"Level X — Stage Y"** chip (Roman numeral for the stage; the guide
  explains what it means).
- The **prescribed value** next to it, localized by unit kind: "8 kg each"
  (per-dumbbell), "12 kg" (barbell/total), "4 steps" (machine stack / band),
  a variation name (bodyweight ladders), or "30 s" (duration).
- A **"Target: 9 / 8 / 8"** line with the whole stage's per-set targets.
- The existing "• N reps" marker stays — it leads the sets line and updates
  as sets are logged.
- The debug skin additionally shows the raw `L<n> · <stage>` label and a
  "(fresh)" marker while no progression state is stored yet.

Supporting engine additions (`ProgressionEngine`): `ValueKind`/`LoadUnit`,
`prescribedValueAtLevel()`, `variationAtLevel()` (clamps past the named
ladder, mirroring `valueAtLevel`), `prescriptionChangesAt()`. The emitter now
pre-parses `variation_sequence_json` into a per-level `variations` list, so
still no runtime JSON parsing.

### Level System Guide (Settings)

A new expandable **"How levels & stages work"** card sits above the RPE/RIR
FAQ card in Settings (`LevelSystemGuideScreen.kt`), following the existing
expandable-FAQ pattern; the three cards are mutually exclusive. It contains
five graphical examples, each a small visual plus a 1–2 sentence caption:

1. **What is a level?** — three progressively longer level bars.
2. **What is a stage?** — stage I/II/III boxes with `8/8/8 → 9/8/8 → 9/9/8`.
3. **Level-up and rep reset** — `12/12/12` → `load up` → `8/8/8` rows.
4. **Big jumps, smaller reps** — same shape with a 6 kg → 10 kg jump.
5. **Changing your starting difficulty** — Easy | Medium | Hard pills.

`LevelSystemGuideScreen` also provides a standalone scrollable wrapper if the
guide ever needs its own route.

### Strings

All copy lives in `values/strings.xml` (`progression_*`, `level_system_guide_*`,
`*_example_*` keys). de/it carry a commented draft translation block that can
be uncommented once reviewed — missing keys fall back to English automatically.

### UI TODOs

- Level-up celebration animation on `lastSetLeveledUp` (field exists, UI
  currently surfaces the new state only).
- Difficulty re-selection before the first set of an exercise is read from
  setup intensity; a per-exercise override picker is a natural follow-up.
- The guide visuals use plain Surface/Box drawing; polish pass welcome.

## Level adjustment + onboarding (2026-09-19)

### Per-exercise level control (My Workout)

The level chip on the live card gained **− / + arrows**
(`adjustExerciseLevel` → `setExerciseLevel`, clamped by the new
`ProgressionEngine.clampLevel` to the exercise's ladder). A manual move
persists `level, 1` — Stage I, the fresh start the CSV's reset policy implies
— and `refreshProgressionDisplayNow()` recomputes the card immediately
(stage target, prescribed load, current set target). History is never touched;
only future sessions change. Non-ready rows (coach review, manual review) show
no arrows.

### "Adjust Your Levels" bulk screen (`AdjustLevelsScreen.kt`)

Reached from the **Adjust levels** button in the My Workout header and from
the onboarding CTA. Contains:

- A compact **"How levels work"** reminder (three mini guide cards via
  `LevelGuideReminderCards()`, reusing the guide graphics) plus a text button
  to the full guide.
- **All easier / All harder** bulk buttons — one rung per adjustable exercise,
  clamped per exercise; confirmed by a snackbar ("All levels decreased by 1").
- A lazy list of the routine's exercises: name, "≈ 8 kg each" / "≈ Incline
  Push-Up" preview at the current rung, and a − / Level N / + stepper.
  Changes persist instantly; an **Undo** snackbar reverts the last single or
  bulk change (a bulk pass remembers every previous level).
- **Done** returns to the caller.

The screen is a third branch of `WorkoutTabScreen` (sub-tab 2), so it animates
with the existing tab transition and needs no nav-graph changes.

### Mandatory guide onboarding

`WorkoutViewModel` exposes `shouldShowLevelGuide`, backed by the
`has_seen_level_guide_v5` boolean in app settings. The flag has no legacy
default, so **every install — fresh or updating — sees the guide exactly
once**; `markLevelGuideSeen()` sets it. `MainActivity` gates on it before the
first-time-setup branch: the full-screen `LevelGuideOnboardingScreen` shows
the five guide examples and offers **Got it** (dismiss) and **Got it → Adjust
your levels** (marks seen, opens `WorkoutTabScreen` with
`startOnAdjustLevels = true`). `rearmLevelGuide()` flips the flag back when a
new routine replaces the old one, re-offering the walkthrough. The Settings
card remains available anytime.

### TODOs

- The adjust screen reads `allRoutineExercises` (the whole routine); a
  "today only" filter is a cheap follow-up if the list feels long.
- Bulk buttons clamp silently at each exercise's ladder end (an exercise at
  max stays put); a per-exercise "already at max" summary could be added.
- de/it strings for the adjust flow and onboarding are drafted (commented)
  in the resource files, same as the guide strings.

## Level-first live card rework (2026-09-19)

The bulk "Adjust Your Levels" screen and the custom-mode opt-out were removed;
their jobs moved into the live card itself:

- **Level display**: a big `Level N` with the Roman stage as its subtitle,
  flanked by − / + arrows that step STAGES (I → II → III …) within the level.
  The level itself changes through earned progression or the details dialog.
- **Level details dialog** (tap the level chip): every level of the exercise
  with its prescribed load/variation and stage-I reps, tap-to-jump for large
  adjustments, plus the **level lock** switch (`level_lock_enabled`): when on,
  completed stages do not advance the level/stage — for deloads, travel, or
  equipment that stops progression. Manual stepping still works under lock.
- **One merged card**: the former steppers card is gone; the live card now
  carries per-set target pills (one per stage pattern entry, active/filled
  states), the read-only weight/reps display (manual weight entry and
  per-rep nudging removed — the level is the control), and the log button.
- The My Workout header button is now **"Levels"**, opening a compact guide
  popup (`LevelGuideReminderCards`).
- Rest timer fix: an expired-but-unreported alarm no longer parks the
  countdown at "1" — `tickTimer` shows it finished immediately and lets the
  (possibly late) alarm report do the advance.

## 3 sets universally; workout goal is scheme-only (2026-09-19)

Guided mode now has one fixed set count, and the workout goal no longer touches
volume at all:

- **`WorkoutTemplates.GUIDED_SETS_PER_EXERCISE = 3`** — the single constant.
  `generateAssistedProgram` and `parameterizeReplacement` lost their
  `minSets`/`maxSets` parameters and the Easy 3 / Medium 4 / Hard 5 volume
  ladder is gone; every generated row gets `targetSets = 3`.
- **"Sets allocation" UI removed** from first-time setup (the
  `SetsAllocationBlock`/`SetsInput` composables and the
  `sets_allocation`/`min_sets`/`max_sets`/`no_limit`/`sets_changeable_later`
  strings are deleted). The `setup_min_sets`/`setup_max_sets` prefs are no
  longer read or written; stale values from older installs are simply ignored.
- **Goal stays, de-scope'd to the scheme axis**: it still owns target reps and
  rest (and exercise selection in the assisted build). It never changed set
  count — that is now enforced by construction. No
  `@Deprecated` marker: the field is live, just cosmetic/scheme-only.
- **DefaultData** demo rows all carry 3 sets (the 11 two-set rows were
  normalized; historical logged workouts are untouched).
- **Engine**: `setCount` continues to derive from the stage pattern width; a
  new test pins every generated Roman pattern to be exactly 3 targets wide.
  `easy/medium/hard_target_reps_json` in the source CSV were already
  3-element everywhere, so no regeneration was needed.
- **Volume insight** now reads "Every exercise uses 3 working sets" (one
  string, `insight_volume_desc`; the three per-difficulty variants are gone).
  The level guide intro mentions the 3-set standard as well.

TODO: if a future version wants per-exercise set counts again, reintroduce
them as a column on `exercise_progression` in the CSV pipeline, not as a
user-facing knob.

## Levels popup parity + read-only live card (2026-09-19)

- The My Workout "Levels" popup now shows the **exact Settings guide** —
  `LevelSystemGuideContent` (intro + all five graphical examples), same title
  string, scrollable inside the dialog. The old three-card
  `LevelGuideReminderCards` recap is no longer referenced.
- The Levels header button gained a `start = 6.dp` gap so it never abuts the
  day title, matching the header's other action buttons.
- The stage/target-reps line moved **into the level block** (directly under
  the Level/Roman chip): "8 kg each · 3 sets · 9 / 8 / 8" reads as one unit.
- The second card is now a **read-only prescription summary** (load + set
  targets). Manual weight/reps editing is gone from Guided mode — the level
  (arrows or the details dialog) is the only control; Edit Routine remains
  the path for swapping an exercise.
- Day titles no longer show the "Arms, Chest, …" subtitle (per-exercise
  muscle badges carry that info).
