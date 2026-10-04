# Agent Guide — RepIo

## Versioning (bump ONCE per release, not per fix)

**`version.properties` at the repo root is the single source of truth** for `VERSION_NAME`
and `VERSION_CODE`. Never hardcode a version anywhere else.

`VERSION_NAME` is bumped **once per release** — right before the signed bundle is built —
not for every bug or fix made since the last release:

```bash
./gradlew bumpVersion -Ptype=feature -Pnote="Short description of the release"
```

| `-Ptype`      | Bump                                    | Example          |
| ------------- | --------------------------------------- | ---------------- |
| `feature`     | minor + 1, patch unchanged              | 1.6.4 → 1.7.4    |
| `fix`         | patch + 1                               | 1.7.4 → 1.7.5    |
| `breaking`    | major + 1, patch unchanged              | 1.7.4 → 2.0.4    |

Choose the type by the release's dominant intent, not by diff size:

- User-visible new capability or visual/UX work → `feature`
- Correcting existing behaviour, polish, bug fixes → `fix`
- Data migration, removed public API, anything that breaks existing users → `breaking`

Do **not** run `bumpVersion` per work session or per bug fix — intermediate work rides
along under the next release's single bump. VERSION_CODE is NOT bumped by `bumpVersion`:

### VERSION_CODE is claimed by BUILDING a release — and `--dry-run` must never do it

`composeApp/build.gradle.kts` increments VERSION_CODE as a **configuration-time side
effect** whenever the requested task names contain `assembleRelease` or
`bundleRelease`, so each signed build gets a unique code. `bumpVersion` deliberately
does not touch it, or the two would double count.

The trap: this writes to **`version.properties`, which is TRACKED in git**, and the
trigger was task-NAME substring matching only. So `./gradlew :composeApp:assembleRelease
--dry-run` — which builds **nothing**, it only prints the task graph — silently mutated
a committed file. Measured: two dry-runs took VERSION_CODE 48 → 50 with no build. The
guard is now `&& !gradle.startParameter.isDryRun`; both the no-op case and the real
build were checked after the change (dry-run holds at 48, a real `assembleRelease`
claims 49).

Consequences to keep in mind:

- **This is a side effect at configuration time, not a task action.** It runs once per
  Gradle invocation, so a second release build in the same session legitimately claims
  another code. That is intended ("one code per signed build"), not a double count.
- **Do not restore the increment to a `doFirst` on a task without thinking about
  `--dry-run` again** — the flag applies to the task action too, so the guard has to
  live wherever the write happens.
- After any experiment that requests a release task, check
  `git status version.properties` and restore it. A burned code is harmless to users
  (Play only requires increasing) but it dirties a tracked file and makes the next
  real release start from a number nobody shipped.

### Grafizum dashboard

`bumpVersion` reports the new version to the Grafizum dashboard when these are set in
the process environment or in the git-ignored `.env`:

```
GRAFIZUM_DASHBOARD_URL=https://api.grafizum.com/v1/events
GRAFIZUM_DASHBOARD_TOKEN=gz_live_agent_...
GRAFIZUM_PROJECT_ID=project_4k096a683a6r0q3b700n3z5q   # optional; defaults to RepIo
```

**The URL must be the `v1/events` route, not the API root.** The command center has no
app-version endpoint, so a bump is recorded as a verified activity event
(`type = "app_version"`, version fields in `meta`) on the RepIo project. `POST` to the bare
host answers `404 Route not found: POST /`, and every state-changing call also needs an
`Idempotency-Key` header — the task derives one from the version, so re-reporting the same
version is a no-op.

Keep these in `.env` only — **do not** add them to `.env.example`. The secrets plugin
reads that file too, and an active key with an empty value makes it generate invalid
Java (`public static final String X = ;`), which breaks compilation. Both keys are
listed in the plugin's `ignoreList` so the token is never compiled into the app either.

The report is a `POST` with a Bearer token, an `Idempotency-Key`, and this body (the
dashboard's `eventCreateSchema`):

```json
{
  "type": "app_version",
  "project_id": "project_4k096a683a6r0q3b700n3z5q",
  "message": "RepIo 1.5.4 (version code 38) - fix: what changed",
  "reason": "Version bump reported by the bumpVersion Gradle task on ...",
  "meta": {
    "app": "RepIo",
    "packageName": "com.grafizum.repio",
    "versionName": "1.5.4",
    "versionCode": 38,
    "changeType": "fix",
    "note": "What changed",
    "reportedAt": "2026-09-15T10:30:00+02:00"
  }
}
```

If the variables are unset, or the dashboard is unreachable, the report is skipped and the
local bump still stands. A dashboard failure must never block a version bump.

## Responsive layout — one adaptive system, never per-device numbers

Every size the UI uses comes from `WindowSize` (`ui/components/Responsive.kt`), resolved from the
window itself. Read it with `LocalWindowSize.current`; never hard-code a dp value in a screen.

`windowSizeFor(widthDp, heightDp, fontScale)` **interpolates** each token between three anchors
(320dp phone → 400dp phone → 1000dp+ window) rather than picking from a bucket, so a 360dp and a
411dp phone each get the spacing that suits them:

- Width drives spacing and typography; height drives the blocks that consume vertical space
  (`dayChipHeight`, `donutHeight`). Do not cross the axes.
- Continuous means continuous: `WindowSizeTest` sweeps 320→1200dp one dp at a time and fails if any
  token moves by more than 1dp between neighbouring sizes. Adding a token means adding its anchor
  row *and* its continuity case.
- `WindowWidthClass`, `isNarrow` and `isShort` are for **structural** decisions only (scroll vs.
  squeeze, one column vs. two, dense header). Never pick a dimension with them.
- The user's `fontScale` grows the boxes that hold text, damped by half, so large fonts cannot clip
  inside a fixed-height row.

Sizing rules that caused real bugs — do not undo them:

- Content with an intrinsic minimum needs a **definite** height, and its screen must scroll. A
  `weight(1f)` slice of "whatever is left" collapses to ~0dp on a short window (this is how the
  analytics donut card vanished), and a bare `heightIn(min = …)` lets a child grow to the window's
  whole height (this is how the day chips reached 388dp and pushed the list below to zero).
- In a 7-across row, Material's 48dp minimum *width* cannot win: 7 × 48dp fills a phone's content
  column and erases every gap. Opt out for those chips only, with
  `LocalMinimumInteractiveComponentSize provides Dp.Unspecified`, and keep their height ≥ 48dp.
- `AutoScaleText` centres its text inside its box — a `fillMaxWidth` box otherwise parks the label
  against the left edge.

## A theme id is a PERSISTED STRING — renaming one needs a migration

`theme_option` in SharedPreferences holds the theme id, and the enabled launcher
component is a second, separate piece of state. Retiring an id therefore breaks
**two** things on the device, and fixing only the first is the trap:

1. the stored id matches nothing in `resolveColorScheme` and falls through to `else`;
2. the enabled component (`MainActivityAzure`) no longer exists, so **NO launcher
   activity resolves at all** and the app has no icon. Writing a valid theme id to
   the preference does nothing for this — only `switchIcon()` repairs it.

So a retired id resolves to **null — "no stored choice"** — which routes the user
through the *same fresh-install path* a new user takes. That path picks the
system-appropriate default (Flame on dark, Beige on light) **and** calls
`switchIcon`. `Theme.kt`'s `RETIRED_THEME_OPTIONS` + `isRetiredThemeOption()` /
`migrateThemeOption()` are applied at three read paths:

- `RepIoApplication.onCreate` — earliest reader, and **the only one that can repair
  the icon.** It clears the retired id *and* `auto_theme_selected` together. Clearing
  the flag is the load-bearing half: the fresh-install branch is guarded by
  `!hasAutoSelected`, so leaving it true skips the branch and the icon is never
  repaired — silently, because nothing fails. Verified on-device only; see below.
- `WorkoutViewModel`'s init — a belt-and-braces guard for a value that arrived some
  other way (backup restore, downgrade/upgrade). It **drops** the id rather than
  translating it, because it cannot know the system appearance and the correct
  fallback is the system default.
- `resolveColorScheme` / `WorkoutTrackerTheme` — so a stale value reaching any other
  caller still resolves. Both must fall back to `defaultThemeFor(isDark)`, **not** to
  `themeOption` itself, or one branch quietly undoes the failsafe while the colours
  come from the default.

`ThemeRenameTest` (9 cases) pins it, including that the set holds **dead ids only**
and never a mapping to a successor.

**Do not "improve" this into `mapOf(old to new)`.** A rename map reads tidier,
passes every behavioural case (a translated id *does* resolve to something), keeps
the user on a theme they never picked, and — being a pure preference rewrite — does
nothing for the missing icon. Verified on the SM-S906B: with `AZURE` written back
into the prefs by hand and `auto_theme_selected=true`, one launch cleared the id,
wrote `FLAME` (night mode), and re-enabled `MainActivityFlame` in place of the dead
component; setting `EMERALD` the same way and relaunching left it `EMERALD`.

One retirement so far: **`AZURE`** (2026-10-02), the theme being renamed to
`ROYAL_BLUE`. The theme's colour was always `#007AFF`, hue **211°** — a vivid blue,
not azure. True azure (`#007FFF`) is **210°**, so "make it actually azure" was a 1°
change that also moved the theme *toward* OCEAN (hue 188°, only 75/441 away), making
the two harder to tell apart in the picker. **The colour was fine; the name was
wrong.** Retro is intentionally greyscale (no brand colour in either set) and is to
be remade from scratch.

Two traps when a theme name becomes multi-word:

- `MainActivityRoyalBlue` -> `ic_launcher_royal_blue`. The class-suffix no longer
  equals the resource name, so any test pairing them has to normalise the word
  break; comparing them directly silently finds *no activity* for the theme.
- A regex keyed on the exact variable that holds the theme (`themeOption`) breaks
  when the migration introduces a second one. Match the **shape** (`when
  (\w+.uppercase())`) or the test fails on a rename rather than on a real defect.

## Exercise localization — store canonical English, display local

The database stores every pool exercise under its **canonical English name** (`Dumbbell
Curls`). That name is the join key the equipment table (`exerciseEquipment`), the primary
muscle table (`exercisePrimaryMuscles`), the logged sets and the built-in templates all
agree on, so it must never change. Translating at *storage* time would orphan every set a
German user has already logged and would split one exercise into two records.

Translations are applied at **display** time instead, keyed by a slug
(`ExerciseLocalization.slugify("Dumbbell Curls") == "dumbbell_curls"`):

- `data/ExerciseLocalization.kt` — slugging, `displayName`, `description`, `allNamesFor`,
  `canonicalFor`, and the window-facing API. Also the reverse index from every known name
  (English *and* translated) back to the canonical one.
- `data/ExerciseTextDe.kt` / `ExerciseTextIt.kt` — one `"slug" to ex("Name", "Method text")`
  entry per pool exercise, **written on a single line**. **A slug typo does not crash anything**
  — it silently leaves one exercise in English — which is why `ExerciseLocalizationTest`
  asserts that no entry is outside the pool, that every entry has a name and a description,
  and that names and descriptions are unique within a language. Add a language by adding a
  table and registering it in `ExerciseLocalization.TABLES` (the tests pick it up
  automatically).
- **Coverage IS asserted again.** The pool was rebuilt from the exercise-database CSV (601
  exercises) and the translated history was retired with the old pool, so coverage was dropped
  while the tables regrew. Both tables are now back to **601 of 601** and
  `ExerciseLocalizationTest.everyTableCoversTheWholePool` locks that in. Adding an exercise to
  the pool without a de/it entry now fails the build — which is the point.
- `translateExerciseName()` is the single display path for an exercise name; `getExerciseDetails()`
  resolves the description the same way and falls back to English, never to blank.
- Search matches the canonical name **and every translation** (`ExerciseSearch.searchCandidates`),
  and always returns the canonical name, so typing `Kniebeugen` finds and stores `Squats`.
  `canonicalFor()` normalizes whatever was typed before muscle detection and before saving.
- Tooling: `python tools/exercise-i18n/extract.py` prints the slug table from the generated
  `data/ExerciseCatalog.kt` (and fails if the pool ever gains an exercise with no description);
  `python tools/exercise-i18n/check.py` reports duplicate names/descriptions per language.
  Both read the current sources — `ExerciseDescriptions.kt` is only a delegate now, so neither
  script parses it.

When the pool changes, extend the tables too. `ExerciseLocalizationTest` will not fail you
for a *missing* entry (see the coverage note above), so run `extract.py` to see how far the
tables have fallen behind the pool.

## The stored kit vocabulary is FINER than the equipment tile grid

The tile grid has 16 tiles (`HOME_EQUIPMENT_TAGS` + `GYM_EQUIPMENT_TAGS`).
`ExerciseEquipmentTags.ALL_TAGS` has 29. The difference is not dead vocabulary —
it is how a full gym is stored: eight selectorized stations (`leg_press_machine`,
`smith_machine`, `chest_press_machine`, …) plus `machine_other`, plus
`barbell_only` and `kettlebell_and_bench`, sit behind three tiles.

That gap caused the owner's "bodyweight and dumbbells still selected machines"
report twice, and it is worth stating as a rule:

- **Anything a stored tag can grant must be visible and removable from the
  grid.** `ExerciseEquipmentTags.tagsCoveredByTile(tile)` is that mapping, and
  it is used in BOTH directions: a tile reads as ticked if the kit grants
  anything in its family, and unticking a tile removes the whole family. Without
  the second half the state is invisible *and* permanent — `toggleTag` rebuilt
  from the same untouched set on every tap.
- **Ticking must stay purely additive.** The tempting counterpart — collapse the
  eight stations into `MACHINE_OTHER` when the tile is ticked — is wrong: the
  stations gate *different* exercises, so it would silently take the leg press
  and the Smith machine away. They are separate tags for a reason.
- **`equipmentTagShortLabelKey` must name EVERY tag in `ALL_TAGS`, not just
  every tile.** The eight stations fell through to the generic `"equipment"`
  string, so anything that rendered a stored tag per label collapsed all eight
  into one unreadable chip — a fifteen-tag full-gym kit reporting itself as
  "Chair, Bench, Dumbbells". The function is the ONE place a tag becomes a
  word, so an unmapped tag there is an invisible one everywhere. There is no
  "Your equipment" card in Settings any more (removed by the owner 2026-10-01),
  but the mapping still has to be complete: it is what the build screen's
  tiles read, and it is the only place the vocabulary is written down.
  `CustomBuildEquipmentGateTest.everyStoredTagCanBeNamedBackToTheUser` and
  `ExerciseSelectionRoundTripTest` hold both halves.
- **Never write the equipment selection back from an empty default.**
  `FirstTimeSetupScreen` once held it in `remember { mutableStateOf(listOf<String>()) }`
  and saved it unconditionally, so one routine rebuild could overwrite a real
  kit with a blank. It is seeded from `WorkoutViewModel.savedEquipmentSelection()`
  and `saveSelectedBuild` only writes `buildEquipment` when the picker was
  actually touched.

## A support word in a name is LOAD-BEARING — the equipment gate reads it (2026-10-01)

The owner asked, on `task_6e584h5d2j1z56173p5m081a`, why “chest supported” survives in
the names when everything else got generalized away, and said **“Explain it and on my signal
change it”** — so nothing was renamed. The answer turned out to be in the gate:

`ExerciseEquipmentTags.requirementsFor` matches the **word** `"chest-supported"` to put the row
on a seat (`return chairOrBenchWith(DUMBBELLS_ONLY)`). The word IS the requirement, not
decoration:

- **Physically** it changes the exercise. A chest-supported row removes the trunk’s job of
  holding itself isometrically, so the lats do the work instead of the lower back and the grip.
  It is the standard prescription when the lower back fatigues first or the grip is the limiter.
- **In the app** it changes the kit. Strip it and the row falls through to `DUMBBELLS_ONLY`, so a
  user with dumbbells and no bench is offered a bench exercise — the equipment-leak class this
  project has now paid for three times (chair/bench split, the coarse-tag round trip, the seven
  mis-categorised rows).

So `generalize.py` moving APPARATUS words only is **load-bearing, not conservative**: 95 of the
601 rows (16 %) carry a support/posture/angle/direction word — Reverse 22, Overhead 16, Seated 15,
Incline 11, Standing 11, Decline 7, Kneeling 6, Chest-Supported 5 — and every one of them changes
either the movement or the kit.

**And punctuation was leaking.** One row is catalogued **“Chest Supported Dumbbell Shrug”**
(space) while its four siblings are hyphenated, so the hyphen-matching branch never saw it and
the app offered a bench-supported shrug to dumbbells-only kits. `requirementsFor` now folds
`"chest supported"` → `"chest-supported"` into a `gateName` used by the rules, so a spelling
difference cannot decide an equipment requirement. Locked by `ChestSupportedGateTest`, which
sweeps every pool row containing “chest supported” in any spelling and asserts each one demands a
seat.

**If the owner signals the rename anyway**, it is a pipeline operation like any other: the word
would have to move from the name into the gate for all five rows, and `ChestSupportedGateTest`
is what stops that from silently un-gating them.

## Exercise names say their equipment — as a qualifier, never as a prefix (owner ruling 2026-10-01)

An exercise name is the **movement plus the apparatus it is done with**. The
first version of this section said the opposite ("name the movement, read
`equipment` instead") and the owner reversed it: with bare names, "Dumbbell Row"
and "Row (Barbell)" and "Bent Over Row" were three strings with nothing in the
name to tell them apart, and the only place the answer lived was a field no
user ever sees.

The rule, as implemented by `tools/exercise-db/generalize.py`:

- **A name of more than three words (`SHORT_NAME_MAX_WORDS = 3`) carries the
  apparatus as a trailing qualifier.** "Bent Over Row (Smith Machine)", "Rear
  Delt Row (Barbell)", "Squat (Barbell)".
- **A name of three words or fewer keeps its apparatus in front.** "Dumbbell
  Row" stays "Dumbbell Row" and never becomes "Row (Dumbbell)". This is the
  whole exception and it exists because "Row (Dumbbell)" is worse to read than
  "Dumbbell Row", not because short names are a different kind of exercise.
- `APPARATUS_PREFIXES` and `QUALIFIER_NAMES` are the two halves of the same
  vocabulary; `"Band"` is in both because the dataset spells some rows "Band
  Assisted Pull Ups" and others "Banded …".

Three rules make this safe, and each one is load-bearing:

- **Only EQUIPMENT words are ever moved** (`APPARATUS_PREFIXES`,
  `QUALIFIER_NAMES`). Angle, grip, posture and support words stay exactly where
  they are: a close-grip and a wide-grip press are two different exercises, and
  "Close-Grip Bench Press" → "Bench Press" would be a false generalization.
- **Names are a primary key, so they must stay unique.** The pool is joined by
  name — `exerciseEquipment`, `exercisePrimaryMuscles`, logged sets, templates,
  and the `progression_state_<name>` / `cache_reps_<name>` preference keys.
  Six "Squat" variants collapsed to one name would silently merge six
  exercises. `build_progression.py` fails the build on a duplicate display name
  *and* on a progression branch pointing at a name that is not in the pool
  (renaming without that check left 26 dangling branch references).
- **Old names keep working.** `ExerciseCatalog.RENAMED` is generated with the
  catalog and is cumulative across the two passes — it carries 220 entries,
  because a routine logged before the first rename has to resolve through both
  to reach today's name. `ExerciseLocalization.canonicalFor` walks it, so a
  routine logged as "Smith Machine Squat" still points at "Squat (Smith
  Machine)" with its history, equipment and progression intact, and
  `allNamesFor` returns the old name so search still answers to how people ask
  for things.

**Still read `equipment`, never the name, for behaviour.** Naming the apparatus
did not make the name a data source — it made it *legible*. `isBodyweightExercise`,
`getExercisePriority` and `isHeavyCompound` read `ExerciseCatalog.equipmentFor(name)`,
and only fall back to guessing from the name for a user's own custom exercise,
which has no catalog row to ask. The first generalization pass proved why: it
removed the word from 62 names that still *are* barbell or dumbbell work, and
every name-based guess silently broke — `isBodyweightExercise` called any "squat"
bodyweight unless the name said otherwise, so the barbell squat got a starting
load of 0.0.

Renaming the pool is a pipeline operation, not a find-and-replace, and the
order matters:

```bash
python tools/exercise-db/build_progression.py        # rename + invariants
python tools/exercise-db/emit_progression_kotlin.py  # catalog, RENAMED map, progression
python tools/exercise-db/apply_rename_to_sources.py  # literals in chains, templates, tests
python tools/exercise-db/rekey_translation_tables.py # de/it slugs
./gradlew :composeApp:testDebugUnitTest
```

`build_progression.py` **overwrites** `out/name_renames.json` with the current
pass only. Merge it back into the existing file (old ∪ new, then a transitive
retarget pass) before emitting, or every routine logged before the previous
rename loses its link to its exercise.

`apply_rename_to_sources.py` rewrites whole string literals only (display and
lowercased) and skips the generated files, which the emitter has already
written. The translated **names** in the de/it tables are deliberately left
alone: they are correct German and Italian that place the apparatus differently
("Bankdrücken mit Langhantel"), and rewriting them is translation work.

## A load is not always a weight — read the row's `unit`, never the word "weight"

81 of the 601 pool rows are loaded with something that cannot be weighed:

- **64 `stack_step` rows** — selectorized machines (cable presses, the leg press, assisted
  pull-ups). The ladder counts pin positions, and "pin 6" is a *different resistance on every
  machine in every gym*. There is no honest conversion, and the CSV does not carry one.
- **17 `resistance_step` rows** — resistance bands, counted 1..10 in
  `ProgressionTables.LADDER_7b2e37`. Bands are sold by colour and picked up by feel, and they
  LOSE tension as they stretch, so step 4 is not a multiple of anything either.

Both used to render through the one kg path, so a cable row asked the user to invent a
kilogram value. `ProgressionEngine.loadUnitFor(name)` is now the single source of truth, and
three things read it:

- the prescription text (`prescriptionDisplayText`) — "Pin 6", "Medium band", not "4 steps";
- the exercise edit field (`loadFieldLabel` / `loadFieldSuffix` / `loadFieldValueText` /
  `loadFieldPlaceholder`) — labelled "Pin"/"Band" with **no** unit suffix, because a count is not
  a measure, and seeded through `loadFieldValueText` rather than `Double.toString()`, so pin 20
  opens as "20" and not "20.0". There are **two** editors (the routine editor in
  `WorkoutComponents.kt` and the first-time-setup one in `FirstTimeSetupScreen.kt`) and the
  second one kept the hardcoded `target_weight` / `"kg"` / `"0.0"` long after the first was
  fixed — found by opening the sheet on a phone, not by reading. All four answers must go
  through the helpers in both places;
- the logged-set summary and the in-workout card (`loggedLoadSummary`), which print a stored
  value in the exercise's own unit so a pin number never comes back as "6 kg".

A count is only stripped of its `.0` when it is whole: a user's own `2.5` in a pin field is
printed in full, because rounding it would silently change what gets saved. Weight keeps its
decimal — 2.5 kg and 62.5 kg are real prescriptions.

`bandTensionOf()` buckets the band ladder's 1..10 into five named tensions (two rungs each,
clamped at both ends) because "step 4" asks the user to know a numbering their gym does not
use. `getStartingWeightKg` returns 0.0 for a step row — that function is a *kilogram* estimate,
and a pin count dressed as a weight is the bug, not the fix. `LoadUnitTest` counts the rows so
a pool change cannot quietly empty either unit.

## CHECK THE HARDWARE BEFORE SAYING A TASK IS BLOCKED

On 2026-10-04 I reported to the dashboard that the SDXL render tasks "need the CUDA box,
which this machine does not have". **That was false.** `nvidia-smi` shows an RTX 4070 Laptop
GPU, 8.3 GB, driver 615.71.09, CUDA 13.4, and `~/sdgen/venv` runs against it (torch
2.14.0+cu130, diffusers 0.40.0, 12 GB of models cached). I had taken the absence of a GPU from
an **old note in the repo** instead of running the command, and a note is not a measurement.

The whole32-pattern render then completed locally in about 11 minutes.

Three rules, because each one has a cheap failure:

1. **`nvidia-smi` before "this machine has no GPU".** One command. A note in the repo is a
   record of what was true on someone's machine on some day.
2. **"Blocked" needs the specific blocker, not a plausible one.** The true blocker on the
   image task was that 24 of 32 patterns had never been rendered — work to do, not an excuse.
   "Needs hardware we do not have" is unfalsifiable until you check.
3. **Say which claim was wrong, in the same place you said it.** The correction stayed on the
   board rather than being quietly overwritten, because the next session needs to know the
   claim was made and retracted, not just that the final note is right.

Two more operational facts from that run, both worth keeping:

- **A backgrounded batch died silently.** No traceback, GPU at 0%, nothing in the log, while
  the same render in the foreground worked every time. Run long batches in chunks whose end
  you can see — a batch that stops reporting is indistinguishable from one still working. This
  is the same lesson as the float sweep below, and it has now cost time twice.
- **The duplicate check is the guard that matters for renders.** On 2026-09-29 all 8 squat
  PNGs came out byte-identical because the per-exercise text sat past SDXL's 77-token CLIP
  window. Verify with `md5sum | sort | uniq -d`, not by looking at one image: 33 files and 33
  unique hashes is the check that would have caught it.

## A hold is measured in SECONDS, and every hold row shares ONE ladder

A stretch has no load and no reps, so its measure is hold time. That is why the 76
`mobility_stretching` rows sat in the CSV's `manual_review` bucket — the generator
refused to invent a load for them rather than guess. `tools/exercise-db/ladder_backfill.py`
fills that gap, and then does a second, separate job that is easy to miss:

**137 rows already HAD a seconds ladder, and they disagreed with each other.** A Plank
climbed 30/45/60/90, a Back Lever 20/30/40, a Banded Hamstring Stretch something else again
— so "level 4" meant a different number of seconds depending on the row, and the number on
the level card was not comparable across the pool. `normalize_duration_rungs` snaps every
seconds row onto one ladder (`DURATION_RUNGS` = 15/30/45/60/90/120), so the same level means
the same duration everywhere.

It is deliberately a **different function** from `apply`, which fills a gap where there is no
ladder. These rows have one and it is wrong, which is a different defect, and conflating the
two means the normalization silently skips every row that already looked populated.

Three rules, each with a way of failing quietly:

- **The ceiling is per `training_category`, never per row.** `mobility_stretching` tops at
  60s (past roughly a minute a static stretch buys tolerance rather than range) while
  `isometric_hold_or_carry` takes the full 120s, because a front lever genuinely *is*
  trained past a minute and a calf stretch is not. There is no honest way to tell one 45s
  stretch ceiling from another by name — and reading names is what the naming rules forbid
  anyway.
- **120s is a CEILING for a hold and only a FLOOR for a repeated movement.** A row whose own
  `maxValue` runs past it (Air Bike reaches 600s) keeps its upper rungs, appended in order.
  Applying the hold rule to intervals deleted a third of that row's range on the strength of
  a rule written about holds.
- **Only `unit == "seconds"` is touched.** `meters` rows share the same
  `distance_or_interval_progression` method, and stretching one to 120 metres is not the
  same decision as holding one for 120 seconds.

The Easy/Medium/Hard starts are stored as a **level index**, so they must be re-pointed at
the new rungs — left alone they name rungs that no longer exist, which is silent. Medium
deliberately lands on 30s: it is the number the prescription is actually written in, so a
beginner is not started past the clinical dose.

A hold has **no Roman pattern** — the stages are reps-per-set, and a 45-second hold has no
reps. Those rows carry an empty pattern and the LEVEL carries the progression, exactly how
the existing `duration_progression` rows work. Inventing reps for a stretch is the bug the
owner reported on the wrist stretch. The one exception is the Olympic lifts, which DO keep a
pattern because they are trained at 1–5 reps (`OLYMPIC_ROMAN`); a snatch prescribed at 8–12
would be a different exercise.

Seven rows still have no ladder, and the build prints each with its reason rather than
hiding it: the four plyometrics (Box Jump, Jump Squat, Plyo Lunge, Plyo Push-Up — they
progress by volume, then height, then intensity, and one ladder cannot express all three),
Nordic Hamstring Curl (eccentric-only, load is trainer-designed from tempo), Rope Climb
(bodyheight is a distance) and Wrist Roller (revolutions, a unit no other row uses).

## Every weighted exercise starts at level 4 — so the picker must not open ON it

Every one of the 75 weighted progression rows has `mediumLevel = 4`, which is **2.0–2.5x its
own level-1 load** (p50 ratio 2.5). That is the CSV, not a defect, and it is not a bug to cap:
a cap broke `dumbbellBicepCurlMediumStartsAtTheCsvStackValue`, because 2kg -> 5kg is an
ordinary medium curl.

The levels below the start are the honest answer to "the starting value is way too high" —
so they have to be *reachable*, and they were not. The level picker opened with
`initialFirstVisibleItemIndex = details.indexOfFirst { it.isCurrent }`, which put level 4 at the
**top** of the viewport and scrolled levels 1–3 above the fold. The owner's report —
"exercises below level 4 are not even shown" — was literally true.

`levelPickerInitialIndex()` keeps a 2-row lead-in above the current rung instead. Two is the
lead-in that makes a ladder read as a ladder: enough to show there is something above the
current rung, without making the dialog a scroller whose position must be re-found each time.
The card's own `+`/`-` arrows are **not** this fix — they walk Roman *stages*, 13 taps per
level, so the picker is the only real path down. `LevelPickerStartPositionTest` locks the open
position and proves every weighted row has a selectable rung below its start.

## The week shape is a user choice — and the options are the owner's three, not ours

The day picker answers "how often". What each day **is** is a separate
`SplitStyle` (`WorkoutTemplates.SplitStyle`: `OPTION_A` — the default —
`OPTION_B`, `OPTION_C`), chosen in setup through `SplitStyleSelector` and
persisted in `setup_split_style` so a goal change regenerates the same shape the
user picked rather than a fresh default. The ids are `"a"/"b"/"c"`, the owner's
own letters; anything unrecognised (including the ids of the pre-option styles)
falls back to `OPTION_A`.

`styleTemplates` is **the owner's table, verbatim**, and it is not ours to
re-derive. Every row of it, and the two day counts it does not name:

| Days | A — balanced | B — upper focus | C — hybrid |
| --- | --- | --- | --- |
| 1 | Full body | Full body | Full body |
| 2 | Full body ×2 | Upper / Lower | Full body ×2 |
| 3 | Full body ×3 | Upper / Lower / Upper | Full body / Upper / Lower |
| 4 | Upper / Lower ×2 | Push / Pull / Legs / Upper | Full body / Push / Pull / Legs |
| 5 | Push / Pull / Legs / Upper / Lower | Push / Pull / Legs / Upper / Arms & core | Upper / Lower / Push / Pull / Legs |
| 6 | Push / Pull / Legs ×2 | … / Arms & core / Weak Point | Upper / Lower / Push / Pull / Legs / Full body |
| 7 | PPL ×2 + Arms & core | … / Weak Point ×2 | … + Arms & core |

Rows 1 and 7 are **not** in the owner's table; they are filled in from each
option's own logic and are commented as such. Row 7 for B is the tight one: the
seventh session cannot be a leg day, so it goes to the weak points, which is the
whole thesis of that option.

`SplitStyleLayoutTest.theWeeksMatchTheSpecifiedTable` writes the table out
literally, and `everyStyleWeekIsValidForEveryDayPick` runs **every (option,
frequency) pair against `validatePlan` for every combination of picked days**,
because the validator treats Monday as following Sunday. Keep both.

Two things the picker states, both read off the layout rather than typed into a
string, because they are the difference between the options:

- **The run of days** (`dayLabelIds`) — "Push · Pull · Legs · Upper · Lower".
  It replaced a row of per-muscle counts ("Chest 2×, Back 2×, …") that said what
  the week *achieves* but not what the days *are*; four identical numbers read
  as one number repeated. One label per day, always, and the test fails on any
  week whose labels do not match its day count.
- **The leg-day count** (`legSessionsPerWeek`) — the number Option B exists to
  change. `optionBGivesTheLegsExactlyOneDayAtEveryFrequency` asserts it stays at
  1 for every frequency from 2 up, so a card can never claim one leg day and
  build two.

**No day may be a single body part.** The owner's first table gave Option B a
5-day week of "Chest / Back / Legs / Shoulders / Arms" — one part per day — and
then rejected it: a whole day of only shoulders, or only arms, is not a mix.
`noOptionHasAnUpperBodyDayThatIsOnePartAndNothingElse` fails the build on any
session with a single topic that is not a leg day. The exception is deliberate: a
**legs-only day is the leg day**, which is what "one leg day" means, and every
option's lower session is exactly that. A pull day (`["Back", "Arms"]`) is a mix
even though it works one major group.

Two naming rules in `sessionLabelId` are easy to get wrong, and each one was
got wrong at least once:

- **"Full body" means the legs AND two other major groups.** An upper session
  also reaches three major groups (chest, back, shoulders) and is still an upper
  day in every week that contains it.
- **The two leg days are different names on purpose.** A plain legs session is
  the lower half of an upper/lower week and reads "Lower"; the push/pull/legs
  leg day carries core and reads "Legs", so a card never says "Lower" under a
  heading that says "Legs". No week mixes the two.

**One name is given rather than derived**: the weak-point day. Its topics are
deliberately the upper body's, and it is named for what it is *for* through
`WorkoutSplitTemplate.namedSessions`. It is currently a real "go again over
everything above the waist" day, **not** yet narrowed to the user's lagging
muscle — the app has no per-user lagging muscle to narrow it to. Making itdynamic is a genuine follow-up; until then, do not let it quietly train
the legs, or Option B's promise breaks.

**Making it dynamic is an OWNER decision, not a cleanup (2026-10-01).** `styleTemplates` is a
static table and `SplitStyleLayoutTest.theWeeksMatchTheSpecifiedTable` writes the owner's own
Option B rows out literally; the weak-point day's *topics* are precisely what that table leaves
unnamed. Narrowing it per user therefore changes what a verified-literal table produces, and
"lagging" has no single definition in the data yet (lowest level depth, lowest weekly volume,
or longest since trained all answer it differently). Do not pick one silently.

`validatePlan`'s adjacency exemption is now **one broad session is enough**, not
two. A broad session (3+ strict topics: a full-body day, or an upper day
covering chest, back and shoulders) spreads its volume thinly, so a focused day
next to it trains a muscle that had a light dose ~24h earlier. Full Body / Push /
Pull / Legs and Push / Pull / Legs / Upper are standard published weeks, and the
principle behind the owner's options is explicit: weekly volume per muscle
(~10–20 hard sets) and progressive overload matter more than the split itself.
**Two focused sessions sharing a strict muscle is still a violation** — that is
the rule doing its real work.

## Core closes the session

Core work is **always last in a session**, on every split and every day count.
`isCoreExercise` reads the catalog's own primary muscle, never the name — "Plank"
and "Dead Bug" say nothing about the mover.

The reason is the core's *role*, not an adaptation claim. It transfers to the
rest of the session by bracing the spine, so freshness is worth least there;
training it first spends the freshest part of the session on the muscle with the
least to gain from it and leaves the loaded work bracing poorly.

Two implementation traps, both guarded by `CoreLastInSessionTest`:

- The core key is the **first** sort key, so it outranks the compound rule. Put
  it second and a "compound" core movement (a loaded carry, a cable chop) gets
  promoted to *lead* the session — the exact opposite of the rule.
- Trimming an over-long day takes from the **end** of that same order, so core
  is what a too-long day loses first. That is deliberate and consistent: core
  is the optional extra, not the main work.

**Do not dress this up as a growth finding.** Nunes et al. (2021), pooling 11
studies and 268 participants over 6–12 weeks, found a combined difference in
hypertrophy between exercise orders of **0.03** — order does not measurably
affect muscle growth (also ACSM 2026 position stand, 137 reviews). This is
session *structure*. The order effect that is real is on **strength**, and it
inverts the folklore: isolation lifts gain ~1.81x more from the first slot than
compounds do, so an isolation-only day is better led by its isolation work than
by the "compounds first" rule the app still uses everywhere else. That change was
NOT made — the owner chose core-last only.

There is deliberately **no "train more legs" control**. "More legs" does not say
whether it means more frequency, more volume or more exercises, it would fight
the evidence, and Option B plus the goal input already cover both ends of it.

## Muscle filters are broad, exercise data stays precise

The filter a user picks a substitute exercise by offers **six broad body parts**
(`WorkoutTemplates.getBroadMuscleGroups()`: Arms, Back, Chest, Core, Legs, Shoulders), not the
19 precise muscles `getAllMuscleGroups()` also returns. Nineteen exact options make the choice
harder, not more precise - someone replacing a row wants "another back movement", not "another
Lats movement".

A broad part means *every exercise whose own body part is that part* - the same body part its
badge shows. That makes the six parts a **clean partition of the pool** (every exercise appears
under exactly one), so nothing is unreachable and nothing is offered twice.

Do not implement it by unioning the per-muscle pool lists. Those lists say which muscles an
exercise *works*, not which it is *for*: the triceps list carries the dumbbell floor press and
the biceps list carries chin-ups, so a union would offer a chest press under "Arms" and put
chin-ups under both "Arms" and "Back". Precise groups keep those pool semantics
(`getExercisesForPart`); only broad parts take the primary-muscle path.

Whatever a broad filter finds, the *stored* muscle group stays precise
(`getMuscleGroupForExercise(name)`), because the label and the Analytics split read it.
`ExerciseMuscleFilterTest` locks all of this down.

## The launcher icon follows the APP THEME, so light and dark are SEPARATE RESOURCES

Android resolves `mipmap-night-anydpi-v26/` OVER `mipmap-anydpi-v26/` whenever the **system** is in dark mode, and it does this before the app gets a say. So while a light theme and its night twin shared ONE resource name, a light app theme on a dark phone always showed the dark plate — which is exactly the owner's report on 2026-10-02: *"on light beige it showsorange dark and the rest also still shows the dark icons not the light once"*. Beige had always been `icon_bg_white`, so this was never about the five dark plates commit `4f2f1a7` introduced: reverting those could not touch it, because the override is Android's resource resolution and not a value in our files.

The fix is to give the two appearances **different resource names**:

| theme kind | themes | icon resource |
| --- | --- | --- |
| light | Beige, Emerald, Crimson, Royal Blue | `ic_launcher_<theme>_light` |
| dark | Flame, Ocean, Rose, Purple, Retro | `ic_launcher_<theme>_dark` |

`_light` has **no** `mipmap-night-*` counterpart, so there is nothing for Android to override and a light theme keeps its light icon on any system setting. `_dark` is a real file rather than a night-qualified one, so a dark theme keeps its dark icon on a light system. Generated by `tools/icons/split_launcher_icons.py` (copies, so the two appearances cannot drift) with a `--check` mode.

Three traps, each of which is a way this silently breaks again:

- **The names must stay separate in the foregrounds and the legacy rasters too.** An adaptive XML pointing at `@drawable/ic_launcher_fg_beige` still resolves the NIGHT drawable on a dark system. That is why the generator also copies the artwork to `ic_launcher_fg_<theme>_light` / `_dark` in the **plain** `drawable-<density>/` dir. Same for `mipmap-<density>/ic_launcher_<theme>_<suffix>.png`, which some launchers and the pre-API-26 path use.
- **`ic_launcher_orange` stays on the system-following name.** It is the `<application android:icon>` fallback shown before any launcher activity exists, and at that point no theme has been chosen — there is nothing to honour.
- **The manifest must actually use the new names.** The split resources are inert until each activity points at one; `everyLauncherActivityUsesTheSeparatedIconName` holds that, and `theIconSplitAgreesWithTheThemesOwnDarkList` reads `Theme.kt`'s own `isDarkThemeUsed` branch so a new theme cannot ship a dark icon on a light UI. `lightIconsCannotBeOverriddenBySystemDarkMode` fails the build if a night-qualified `_light` ever reappears.

## A progression branch is a LADDER RUNG, not a free-text suggestion

The owner asked (2026-10-02) for a progression on every exercise, grounded in
professional practice rather than invented here. The result is
`tools/exercise-db/data/progression_ladders.py`: **125 published orderings** of
pool slugs, easiest to hardest, from which `derive_branches_from_ladders()`
mechanically produces the branches. Coverage went 29 -> **334 of 601**
exercises (454 branch objects).

Three rules keep this from becoming 454 invented chains that read plausibly:

- **A ladder is the only editorial act.** The order is the published one
  (calisthenics press: wall -> incline -> knee -> floor -> decline/deficit ->
  close-grip -> pseudo-planche -> archer -> planche; the pull-up roadmap;
  loaded squat/hinge; isolation by hardening the elbow). Do not hand-write new
  branch dicts — add or reorder a ladder instead.
- **DIRECTION: `entry` is the exercise the user moves TO**, and the row it hangs
  off is the one they leave. `ProgressionEngine.evolutionOffer` looks up
  `sourceEntry.branches.firstOrNull { it.entry == targetName }`, so a branch
  pointing at its own source renders as "you have mastered X -> try X". This was
  wrong on the first derivation and is now asserted for every row by
  `ProgressionBranchCoverageTest`.
- **A branch whose TARGET has no level system is unrenderable and is dropped.**
  `evolutionOffer` returns null for a target that is not progression-ready, so
  the option silently disappears. Seven rows are `manual_review_required` (Box
  Jump, Jump Squat, Plyo Lunge, Plyo Push-Up, Nordic Hamstring Curl, Rope Climb,
  Wrist Roller); `merge_branches()` mirrors `isProgressionReady` in Python to
  keep the two definitions in step.

**Why it is not all 601**, and must not be forced to be: 341 rows are machines
and 66 are stretches/poses. A seated leg curl does not progress *into* another
exercise — it progresses by load, which the ladder already does — so a branch
there would be a chain no coach uses, and the popup would recommend nonsense.
The owner chose "only where a real progression exists". The remaining uncovered
non-machine rows are mostly the **top rung** of their ladder, which is correct:
there is nothing above them in the pool.

Regenerate with:

```bash
PYTHONPATH=tools/exercise-db:tools/exercise-db/data python3 tools/exercise-db/build_progression.py
PYTHONPATH=tools/exercise-db:tools/exercise-db/data python3 tools/exercise-db/emit_progression_kotlin.py
```

## In-app updates - Play is the only authority

`AppUpdateChecker` asks Play Core's `appUpdateInfo` API and the app shows a dismissible snackbar
plus a tray notification when Play reports an update. Four things about it are easy to get wrong:

- **Play only answers for apps it installed itself.** A build installed with `adb install` has
  `installerPackageName == null`, and Play refuses with `InstallException: -10
  ERROR_APP_NOT_OWNED` ("The app is not owned by any user on this device"). No code change can
  produce a nudge for a sideloaded build. To exercise the flow, install from Play - internal
  testing counts - with a **higher version live on the console** than the installed one.
  Otherwise Play answers `UPDATE_NOT_AVAILABLE`, and staying quiet is the correct behaviour.
- The install *source* is deliberately not a gate in our code any more. It used to be, which made
  a sideloaded build fail silently with no way to tell a broken check from an up-to-date app.
  Play already enforces the ownership rule, so its answer is used as-is.
- The check runs on every foreground from `repeatOnLifecycle(RESUMED)`, **not** from an `ON_RESUME`
  observer that launches into `rememberCoroutineScope()`. That scope is cancelled when the
  composition restarts (the splash screen hands over to the app), which silently discarded the
  check on the very launch that mattered.
- `checkForUpdate` rethrows `CancellationException` and logs every other outcome, so "the check
  never ran" can be told apart from "the check ran and found nothing". Watch it with
  `adb logcat -s AppUpdateChecker:*`.

The nudge is quiet for three days after the user taps "Not now" (`NUDGE_COOLDOWN_MS`).

## Known constraint: minSdk is pinned at 23

`minSdk = 23` is the **lowest value that compiles**. Compose requires 21, and the
resolved dependency set requires 23 — `androidx.appcompat` 1.8.0, `androidx.room` 2.8.4,
the Firebase line (`firestore` 26.6.0, `auth` 23.1.0, `ai` 17.17.0, `appcheck` 19.4.1),
and `play-services-base` 18.9.0 all declare `minSdkVersion 23`.

Do not attempt to lower it without also downgrading those dependencies; the manifest
merger fails with `minSdkVersion NN cannot be smaller than version 23`. API 15 is
impossible — Jetpack Compose has no support below 21.

Code that leans on this floor already: `AppUpdateChecker.kt` (relies on
`NetworkCapabilities` always being available) and `composeApp/lint.xml` (bitmap density
folders).

## A float sweep that accumulates never ends — step an integer

`OverallLevelRankTest`'s two rank sweeps walked the scale with
`percent = Math.round((percent + 0.05) * 10.0) / 10.0`. At `2.3` that is
`(2.3500000000000005 * 10.0).roundToInt()` = `23` → `2.3` **again**, so the counter
stopped advancing and the `while` loop span forever. A `while` in a test is not a slow
test; it is a build that never reports, which is indistinguishable from "still
compiling" if you only look at the task line.

Write the sweep as an integer and derive the value:

```kotlin
for (fiftieths in 1..1980) { val percent = fiftieths / 20.0 }
```

Three things this cost, all worth keeping:

- **The suite was green while being incomplete.** 0 failures, no report, no
  `test-results/*.xml` — the run had produced nothing at all. An interrupted or hung
  run and a passing run look the same from `tail` on the Gradle output, so check for
  the XML before believing a result.
- **Assertions below a hanging loop had never run.** `progressToNext == 0.0` was wrong
  and had never been evaluated, because the loop stalled at 2.3 long before the case it
  guards. The bands are `95/7 = 13.571…` wide, so a 0.05 step can never land on a
  boundary — the first sample inside a tier is legitimately a little *past* its floor.
  "Started the band" is a bound (`<= one step / band width`), not an equality.
- **`getString` ignores surplus arguments.** The rank tooltip takes one format argument
  (the letter; the 0-100 figure is backend-only per the 2026-10-02 ruling), but a test
  still passed `(6.4, "S")` and expected the old two-argument shape. The extra `6.4`
  was consumed by `%1$s`, rendering "Overall Level: Rank 6.4", and the test caught it
  only because it asserted the literal English string. When a format string drops an
  argument, every call site that still passes the old pair is wrong and nothing warns.

## A branch is looked up by ENTRY, so a repeated entry is unreachable data

`ProgressionEngine.evolutionOffer` resolves an offer with
`sourceEntry.branches.firstOrNull { it.entry == targetName }` — by **entry name
alone**. `nodes` and `note` are never consulted. So where a source has N rows
sharing one entry, only the first can ever be selected and the rest are dead.

That is not hypothetical. A rung legitimately belongs to more than one ladder (a
dumbbell bench press is the bottom of both the press ladder and the floor-press
ladder), and `derive_branches_from_ladders` emits a branch per ladder, so
`merge_branches` used to concatenate them. Measured: **28 of 334** exercises
offered the same destination twice, and Dumbbell Shoulder Press offered
"Barbell Overhead Press" **three times**. Any picker renders that as three
identical choices.

`merge_branches` now merges by entry, unioning the `nodes` (first-seen order,
capped at `MAX_NODES`) so the surviving row shows the union of the paths instead
of arbitrarily losing one; a same-entry branch with a different `tag` is left
alone. 28 → 0, and branch objects fell 454 → 425 — that drop is the 29
unreachable rows leaving, **not** coverage being lost (334/601 exercises still
have a branch).

Two things that made this invisible and are worth remembering:

- **The pipeline reported `VALIDATION: all rows passed` the whole time.** Nothing
  in `build_progression.py` looks at reachability, so the build cannot catch it.
  `ProgressionBranchCoverageTest.noExerciseOffersTheSameDestinationTwice` can.
- **The old dedup key `(entry, nodes)` looked correct and was not.** Its comment
  said "two DIFFERENT paths to the same exercise are real choices and stay" —
  but the engine resolves by `entry`, so it cannot tell them apart. A dedup key
  has to match the key the *reader* uses, not the key that looks descriptive.

Related: when exporting ladder data for a mockup, **23 branched rows carry no
`maxLevel` at all** — timed and rep-based rows (`chin_tuck_hold`, `dead_hang`)
define `levelValueSequence` instead, and the app derives the length from the
highest level in it. And `levelValueSequence` is a **JSON-encoded string**, not a
list: iterating it yields single characters and silently reproduces the same
wrong answer. Both traps print `null` rather than failing, so
`design/check_options.js` sweeps all 334 rows for `undefined`/`NaN`/`null levels`
instead of trusting one screenshot.

## On a REP-ONLY exercise the LEVEL is the rep count (owner ruling 2026-10-03)

A bodyweight row with no barbell used to walk a ladder of SUBSTITUTION
MOVEMENTS — level 2 was a different exercise — so the level said nothing about
how much work the user was asked to do and the app changed the exercise without
the user choosing to. That is gone: **the level counts reps, one per level, and
the movement only changes when the user accepts a progression offer.**

- `ProgressionEngine.isRepLadder(entry)` is the definition: a progression-ready
  row whose value is a `VARIATION`. Duration rows (a hold, a plank timed in
  seconds) keep their own ladder — their level already means "longer hold", the
  same idea in another unit.
- `repsAtLevel` / `levelForReps` are the two halves of the mapping, from the
  row's own `repMin` (8 when it declares none) to `REP_LADDER_CEILING` = 50.
  The old `repMax` (12 for most rows) is NOT the ceiling any more; the owner
  raised it deliberately. `effectiveMaxLevel` for a rep ladder is the REP CLIMB
  (43 levels at a floor of 8), not the CSV's declared rung count, which was the
  variation ladder's length.
- **The Roman stage carries the distribution, and it is the pool's own shape.**
  `prescribedReps` adds the stage's row MINUS stage I's row to the level's
  count, so the extra reps walk forward one set at a time. Read it off the
  stored pattern rather than a formula: the pattern is the published part, and a
  formula that quietly disagreed with it would be invisible. The owner chose
  "cascade forward" over "add to the last set" when asked — his own worked
  example (L5 stage III = 5 5 6) is one stage off the pattern's real shape, so
  the pattern won. The last stage is `[12,12,12]` — FLAT — because by then every
  set has caught up; asserting a distribution there fails against the data's
  actual shape.
- `prescribedValueAtLevel` returns **null** for a rep ladder. The level names no
  movement, so there is no variation to prescribe; the reps are the whole
  prescription. Anything that wants to print "Dragon Flag" next to a level is
  reading a thing this removed.
- `prescriptionChangesAt` compares REPS for a rep ladder. Comparing values would
  report that no level-up ever changed anything, which is the one answer it must
  never give.

### The offer fires at a CHECKPOINT, and declining silences ONE checkpoint

Owner: "progressions dont have to only show at max level could also show at the
usual level that is recommended base on online analytics and then if the user
chooses no, the exercise could go on with the level higher, lets say max is 25
levels with reps 25… if he doesnt simply keep incrementing to maybe 50 and then
a show progression again".

- `REP_CHECKPOINTS` = **12 / 25 / 50** reps, and they are researched, not picked:
  8-12 is the range the hypertrophy literature supports (Schoenfeld et al.
  2021), so 12 is where more reps stop being the better lever; 15-25 is ACSM's
  local muscular endurance band; 50 is the ceiling for work with no load to
  raise at all. Changing one is an argument about the literature, not a nudge.
- `isProgressionPending(entry, state, declinedAtReps)` is the WHOLE decline
  lifecycle in one pure engine function, which is why it lives there and not in
  the view model: it has no Android dependency, so the lifecycle is swept across
  the pool in `RepProgressionOfferTest` instead of being asserted for Crunches
  alone.
- **`declinedAtReps` is a REP COUNT, not a boolean.** A boolean cannot express
  "not yet", which is the entire feature. The old boolean (permanent decline,
  re-opened only by hand) stays correct for a LOAD row — its ladder genuinely
  ends — and is kept for exactly that case.
- `isMastered` returns true at a CHECKPOINT for a rep ladder, which is what
  makes the post-workout popup appear at 12 reps rather than at the top.
- "Progression available" in My Workout asks `isProgressionAvailable`, the SAME
  question the post-workout card asks. Two answers would produce a label that
  promises an offer the workout does not make.

## A prescription value means nothing without its ValueKind

`EvolutionSide.load` is a bare `Double?` with **no unit attached**. Printing it
with "kg" is how the evolution popup came to render **"Decline Push-Up — Level 4
of 6 — 4 kg"**: Decline Push-Up is a `ValueKind.VARIATION` row, so its level-4
value is the ordinal of a variation in a sequence, and the number 4 was dressed
as a weight on a bodyweight push-up. Found on the device, not by reading.

Always route through `ProgressionEngine.prescribedValueAtLevel`, which types it:

| kind | what to print | helper |
| --- | --- | --- |
| `LOAD` | "60 kg", "Pin 6", "Medium band" | `loggedLoadSummary` |
| `VARIATION` | the variation's own name ("Deficit") | `PrescribedValue.variation` |
| `DURATION` | "30 s" | new `progression_load_seconds` |

`EvolutionPrescriptionTest` sweeps every branch target at the level the popup
opens it on and pins the invariant: **kilograms exist if and only if `kind ==
LOAD`**. When writing that, note the tempting wrong form — "a numeric value means
a weight" is false, because a `DURATION` row carries a number too (a count of
seconds). It is the **unit** that makes a number a weight, and
`prescribedValueAtLevel` sets `loadUnit` only on `LOAD`.

## Verifying a Compose popup on THIS phone

The SM-S906B is PIN-locked, which changes how on-device checks have to be done:

- **`screencap` returns a solid black frame** — the keyguard is a secure window.
  A 15 KB PNG for a 1080x2340 screen means "blocked", not "blank app". Do not
  read it as a rendering failure.
- **`adb shell uiautomator dump` is the way to see a render.** It reads the
  semantics tree, so it shows every `Text` and every `contentDescription` as
  text. That is enough to verify structure, content and numbers — and it is how
  the "4 kg" bug above was caught.
- **The screen drops to Dozing mid-session and wedges the notification shade
  permanently** (focus stays on `NotificationShade`, every capture black).
  `adb reboot` is the only reliable recovery. `settings put system
  screen_off_timeout 2147483647` plus `svc power stayon true` prevents it — put
  both back afterwards.
- **`--es key "Two Words"` splits at the first space.** The device shell
  re-splits the argument, so `entryFor("Chin Tuck Hold")` receives `"Chin"`,
  which looks exactly like missing data. Wrap the whole remote command instead:
  `adb shell "am start -n … --es exercise 'Chin Tuck Hold'"`.
- `tools/inspect_shot.py` summarises a screenshot when one IS obtainable
  (dominant colours, non-background share, coarse ASCII map).

## A debug-source-set activity is the only preview route here

There is no `ui-test-junit4` on the classpath, so a Compose screenshot test
cannot compile, and a popup that only fires after a completed workout cannot be
provoked from adb. `composeApp/src/debug/.../DebugEvolutionPreviewActivity`
renders one from real engine data and **is not in any release build**.

Its theme must be `Theme.App.PostSplash.*`, **not** `Theme.App.Starting`:
the starting theme is a `Theme.SplashScreen`, and an `AppCompatActivity` cannot
`setContent` under it — the real `MainActivity` only survives because
`installSplashScreen()` swaps the theme first. Getting this wrong crashes on
launch with "You need to use a Theme.AppCompat theme".

Also: `@Composable` calls cannot live inside a `semantics { }` lambda or inside
a `joinToString` transform — hoist `stringResource` and `translateExerciseName`
into the enclosing composable first.

## A rep ladder stops where the MOVEMENT stops

The 116 bodyweight rows with `unit == "variation_step"` all declare `repMin = 8` and a
`repMax` of only 12 or 15, so **the CSV carries no max-effort figure for any of them**.
That is the whole reason a flat cap stood in for the number — 25 for everything, 50 for
core, read off the row's muscle — and it was wrong at **both** ends:

- a **Glute Bridge** stopped at 25 reps, when ordinary programming is 30–50;
- a **Handstand Push-Up** was asked for the same 25, when the movement itself ends near 12.

`RepCeilings` now carries an **authored** number for all 116 (owner authorised reading names
for this on 2026-10-04). Three things make that safe, and each was a way this could break
quietly:

- **It is a table, not a rule.** The numbers do not parse a name at runtime, so a rename
  cannot silently re-tier an exercise. A rule reading the name would.
- **The key is the pool SLUG, not the display name.** The 220 cumulative renames in
  `ExerciseCatalog.RENAMED` cannot orphan a row keyed this way.
- **Every rep-ladder row is covered**, and `RepCeilingCoverageTest` fails the build on a
  silent fall-through to the flat cap — the defect the table exists to remove.

Bands are the ones this file already documents: **12..50**. Nothing goes below 12, because 12
is the first `REP_CHECKPOINTS` rung and a ladder that stopped under it could never offer a
progression at all.

**Authoring the table surfaced two real defects**, and both are the kind that only a *short*
or *odd* ladder exposes:

- **`repStepFor` floored to a 1-rep step.** "One rep is basically nothing" is the user's own
  complaint about a 43-rung ladder, and it still holds **wherever the ladder has room**. It
  does not hold of a genuinely short one: a pseudo-planche push-up's authored span is 8→12,
  and forcing a two-rep step there leaves three rungs for a four-rep-wide range. The rule is
  **span-aware** (≥2 once the span is ≥6), not a flat 2.
- **`repRungs` divided DOWN, so a ladder topped out one rung short of its own ceiling.** This
  predates the table: a strict Push-Up stopped at **24** of a declared 25. The rung count now
  rounds **up**, and `repsAtLevel`'s `coerceIn` holds the last rung at the ceiling — which
  means the top rung is the one place a ladder is *not* a plain step, so `RepLadderTest` pins
  the clamp explicitly rather than assuming none exists.

**Verified non-vacuously** by collapsing the whole table to a flat 25. That sabotage also
caught a weak assertion of my own: comparing a list against `sortedDescending` is satisfied by
an **all-equal** list, so a blanket per-muscle number passed a check meant to catch one. It now
asserts *strict* decrease.

Verified on device: **Chin-Ups runs 8/10/12/14/16/18/20 across seven levels and stops on its
authored 20** — which confirms the ceiling *and* the `repRungs` fix, since floor division would
have stopped at 18.

## Every key in `.env` becomes a `BuildConfig` field unless it is ignored

`composeApp/build.gradle.kts` sets `secrets { propertiesFileName = ".env" }`, which turns
**every** key in that file into a Java field in `BuildConfig` — and `BuildConfig` ships inside
the APK. So writing an active value into `.env` for something that is only needed at build
time **publishes that secret to anyone who installs the app**.

`GRAFIZUM_DASHBOARD_URL` / `_TOKEN` / `_PROJECT_ID` are in the plugin's `ignoreList` for this
reason. `GRAFIZUM_SYNC_KEY` — the dashboard's task-mirroring key — was **not**, and adding it
to `.env` would have compiled it into the shipped binary. It is now ignored, with the reason in
a comment beside the others.

Two rules that go with it:

- **Never add a build-time key to `.env.example`.** The plugin reads it as the *default* set,
  and an active key with an empty value makes it emit invalid Java (`public static final String
  X = ;`) and breaks compilation.
- **Prove the absence, don't assume it.** After adding the ignore entry and building, grep the
  generated `BuildConfig` **and** the packaged dex for the value. A fix that is only reasoned
  about is a fix that can be undone by a rename.

## `TODO.md` is a MIRROR with hard size limits, not the knowledge base

`TODO.md` syncs to the Grafizum command center, and the API validates its payload:

| field | limit |
| --- | --- |
| `title` | 500 characters |
| `notes` | 5000 characters |

The sync CLI's `buildPayload` sends `title` and `notes` **verbatim** with no clamp, so a long
entry fails the whole run with a bare `400 Request validation failed` — and the CLI prints only
`error.message`, so the field-level detail is invisible unless you log the response body.
Meanwhile the dashboard's own handbook says *"notes — the detail… Long is fine here."* Those
two facts contradict each other; **the API wins**, and a repo whose notes are long simply cannot
sync.

Note also that the parser **folds deeper-indented continuation lines into the previous key**,
so a note can span several lines and a naive line-length check under-reports badly.

**The division of labour that resolves this:** `AGENTS.md` holds the permanent engineering
rule, `TODO.md` holds the task record. When a note outgrows 5000 characters, the durable
lesson moves into this file and the note keeps the decision, the traps and the verification
status.

**This is now fixed at the source (grafizum-dashboard `2997a11`), so it should not recur** —
but the fix lives in another repo and can be undone, so the limits still bind here:

- The CLI clamps `title`, `notes` and `assignee` (on a word boundary, with a warning naming the
  task and both lengths) and then validates against `syncRunSchema` from `@grafizum/core` —
  **the API's own schema**, not a copy of the numbers. `github.ts` already truncated notes to
  5000 by hand, which is how one sync path learned the limit and the other did not.
- **A long note is a mirror problem, not a reason to stop writing detail.** Keep the decision,
  the traps and the verification status in `TODO.md`; keep the durable rule in this file.
- Their handbook said *"notes — Long is fine here"*, which is exactly what produced 14,000-char
  notes. It now states the real limit.

Diagnosing this at all needs the response body, which the CLI hides. The quickest route is to
preload a `fetch` wrapper that logs any non-2xx body — **measure with the dashboard's own
parser too**, because reimplementing it gave a confidently wrong answer here (0 of 9 offending
entries, against the API's 9).

Running the sync needs `grafizum-dashboard/apps/sync-cli`, and **the CWD must be inside that
workspace** — `node --import tsx` resolves `tsx` from the working directory, not from the
script's own location, so running it from the project root fails with
`ERR_MODULE_NOT_FOUND` for a package that is installed.
