# Agent Guide — RepIo

## Git commits happen ONLY on the owner's say-so (owner rule, 2026-10-06)

Do **not** commit automatically, no matter how complete or verified the work is. Finish
the work, run the checks, report what changed and what was verified, and leave the edits
in the working tree — staged or unstaged, either is fine — until the owner says to commit.
A green suite is not permission to commit; neither is an end-of-task summary, a TODO.md
ledger entry, or an earlier session having committed similar work.

**Pushing already had this rule and it stands:** a push needs an explicit ask too, so a
commit made on the owner's word still stays local until they say push.

The one exception is a commit the owner has ALREADY authorized in the same exchange
("commit this", "commit and push") — that authorization covers exactly what it names and
nothing further.

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

**The refusal list is now EMPTY, closed on the owner's instruction (2026-10-06):**
*"some exercises like chin tuck hold dont have a level system. i want this fixed, lots of
exercises have this error, give me a number as to how many and then fix them all".* The
number was **seven** — the four plyometrics (Box Jump, Jump Squat, Plyo Lunge, Plyo
Push-Up), Nordic Hamstring Curl, Rope Climb and Wrist Roller — and Chin Tuck Hold itself
was a false alarm: it is a duration ladder (15s→120s) and holds were never counted in the
reps grid. Each of the seven now carries an authored **rep** ladder in
`ladder_backfill.py`'s `SKILL_REP_ROWS`, because the movement's progression *is* reps of it
(ground contacts, slow eccentrics, climbs, revolutions); the implement's height, tempo or
load stays the user's own choice, and the CSV's objections ("landing mechanics", "a
distance", "revolutions — a unit no other row uses") were all about the LOAD axis this
ladder never claims. The two `NOT_BACKFILLED_*` tables stay — they are the mechanism a
future gap plugs into — and `reason_for` still reports any row that falls through them.

The seven's floor is **8, not 2–3**, and that is a pool-wide invariant, not taste:
`RepsAtLevel` falls back to `DEFAULT_REP_FLOOR = 8` for a row that declares none, and
`RepProgressionOfferTest` asserts `repsAtLevel(entry, 1) >= 8` across the pool because the
owner's own words were *"1 rep is basically nothing"*. A first pass opening these ladders
at 2–3 reps failed two tests; the fix was to join the skill tier (Muscle-Up, Handstand
Push-Up and Pseudo Planche already ship floor 8 / ceiling 12): span 4 → engine-computed
step 1 → five rungs 8/9/10/11/12, flat stages (the band is `step − 1 = 0`, exactly how
those skill rows behave). Ceiling 12 is the first `REP_CHECKPOINTS` rung, so the offer to
the harder movement fires. `LevelLadderTest.entriesOutsideTheLevelSystemListNothing` pins
the new world: **zero** pool rows outside the system, and the engine's defensive half is
still proved on a synthetic `validationStatus = "manual_review_required"` copy.

## A LOAD rung must be a weight the gym can set — the ladder walks the row's own increment

The level picker read **"40.45454545454545 kg each"** on a dumbbell curl (owner report,
2026-10-06: *"stuff like 40.43 kg should not exist"*). The cause was not the display path: the
value the app SUGGESTED was off-grid, so the user was asked to load a weight that does not exist.
`valueAtLevelWithCeiling`'s stretch zone — the rungs between a ladder's authored top and the
researched human ceiling — divided the remaining growth by a rung count taken from a log ratio:

| row | authored top | ceiling | stretch rungs | old step | now |
| --- | --- | --- | --- | --- | --- |
| Squat | 135 kg | 220 | 87 | 1.609 kg | **2.5 kg** × 34 |
| Dumbbell Bicep Curl | 40 kg/hand | 45 | 11 | 0.4545 kg | **1 kg** × 5 |

Three rules, each with its own way of failing quietly:

- **The step is the row's OWN `increment`** (`ProgressionEngine.loadGridStep`) — barbell 2.5 kg
  total (101 rows: 82 at 2.5, 19 at 5.0), dumbbell 1-2 kg per hand (76), one pin (64), one band
  rung (17). That is the smallest change the *equipment* can make, which is why 0.5 kg is not a
  universal answer: 0.5 kg on a barbell needs fractional plates no commercial gym stocks, and a
  0.5 kg jump on a deadlift is not a lever. `kg_per_kettlebell` and bare `kg` rows take the same
  path.
- **Kilograms are quantised to a half-kilo, and that is a FLOOR, not a target.** Every rung is
  x.0 or x.5 — but a 2.5 kg barbell row keeps 2.5, because a 1 kg jump is not what that row
  progresses by. Pin and band counts stay whole: a fraction of a pin position does not exist.
- **The rung count follows the grid, not a log ratio**: `extra = round((ceiling − ladderTop) /
  step)`, so the researched ceiling is the ladder's LAST rung and no level exists past it. A
  bench therefore ends at 58 levels of 2.5 kg (180 kg), not at 127 levels of 1.61 kg.

**A DURATION ladder is deliberately excluded** — its rungs are seconds, have no increment to
obey, and keep the even spread (`valueKindOf(entry) == ValueKind.LOAD` is the gate). The
seconds/meters ladders were already normalized separately (see the hold section above).

`formatLoadValue` (the display rounder added on the same report) is **not** the fix and does not
make the suggestion loadable: it only stops the noise from reaching the eye. Both halves are
needed because the displayed number and the stored number must be the same number.

`LoadGridTest` (7 cases) sweeps **every one of the 338 load rows** rather than the two that were
reported, asserting each rung is a half-kilo multiple, each stretch step equals the row's own
increment, the ceiling is the top rung, and no stretch level repeats the one below. Verified
non-vacuously by restoring the old arithmetic: it produces `137.125 kg` and two cases fail.

**The mirror of this rule is still open, and it is bigger.** 287 of the 338 load rows have their
authored ladder cut short by the ceiling rather than stretched past it, which CLAMPS every rung
above the ceiling to the same number: **4,684 levels that change nothing** in total. An Arnold
press walks 30 levels of 30 kg, because its ceiling is 30. The fix is the same rule applied in the
other direction — a ladder should END where the ceiling is reached, so mastery fires at 30 kg
rather than after twenty identical levels — but it moves `effectiveMaxLevel` for 287 rows and
therefore every user's Overall Level, so it is an owner decision: reported, not slipped in.

## A weight is STORED in kilograms and SHOWN in the user's unit

Storage is kilograms, like the exercise pool storing canonical English names. A set logged at
40 kg is 40 kg; the unit is a view of it, not a rewrite. Converting the stored rows would round
every logged set a second time and make the same set read differently depending on when you
looked. `WeightUnit` (`data/WeightUnit.kt`) is the whole model, `LocalWeightUnit`
(`ui/components/WorkoutComponents.kt`) provides it, and `weight_unit` in prefs holds it.

**The default comes from the phone's country, once, and is then persisted.** US, LR and MM get
pounds; the UK deliberately does not — it weighs body mass in stones for people and gym plates in
kilograms, so kg is the safer default and one tap fixes it. A choice the user made always wins,
and the country is never re-read, or a trip would silently switch the numbers under them.

### The pound ladder is GENERATED, never rounded onto a grid

This was got wrong twice against real data, so the evidence is written down. A kilogram ladder
steps 2.5 kg; that is 5.51 lb, and the pound rungs have to be pounds a US gym stocks:

| kilogram step | pound step |
| --- | --- |
| 1 kg/hand | 2.5 lb |
| 2 kg/hand, kettlebell | 5 lb |
| 2.5 kg barbell | 5 lb |
| 5 kg barbell (heavy compounds) | 10 lb |

`ProgressionEngine.poundLadder(entry)` builds `baseLb + k x stepLb` up to the converted ceiling,
**rung by rung**, and returns null for everything that is not a weight — a pin and a band rung are
counts, and a bodyweight row has no load. Rung count, mastery and the level clamp all take a
`unit` parameter defaulting to KG, so every existing caller and test is unchanged, and the display
paths pass the user's unit.

**Do not replace this with rounding each kilogram rung onto a pound grid.** Rounding is at the
mercy of how the kilogram ladder happens to sit, and both attempts produced a level-up that
changed nothing — the exact defect removed from the kilogram side:

- a 2.5 lb grid put an **arnold press's levels 1 and 2 both on 10 lb** (its ladder steps 1 kg at
  the bottom, 2.2 lb, which is narrower than the grid's rounding bucket);
- a finer 1.25 lb grid merely **moved the repeat to level 11 (66.25 lb)**.

A generated rung cannot repeat, because each one is the one below plus exactly one increment.
`WeightUnitTest` sweeps **every pound ladder in the pool** — 250 of the 338 load rows carry a
ceiling and a starting load — asserting rungs are on the grid, strictly increasing, and that the
top rung IS the researched ceiling. It is non-vacuous: the rounding versions fail it.

The honest cost, and it is deliberate: a pound ladder has a **different number of rungs** than the
kilogram one (5 lb steps to the same ceiling is not 2.5 kg steps), so a level index means a
slightly different load in each unit. `Overall Level` is computed from the kilogram ladder, so a
user's progress figure does not move when they switch.

### Two halves that both have to be right

- **Values the engine prescribes** arrive already in the user's unit —
  `prescribedValueAtLevel(entry, level, unit)` — so `prescriptionDisplayText` only writes them
down. Converting there as well would convert twice.
- **Values that are stored** (a logged set, `targetWeightKg`, the session tonnage) go through
  `displayLoadForUnit`, and what the user TYPES comes back through `loadInputToKg`. Tonnage uses
  `fromKg` with no grid at all: a session total is not a load anyone sets on a bar.
- `progression_load_lb` / `recap_volume_lb` are in `StringResourceIntegrityTest`'s
  `IDENTICAL_BY_DESIGN`: "lb" is the unit symbol in all three languages.
- The **label strings must follow the unit too** — the field's suffix, the logged-set summary and
the editor's own `loadField*` helpers are the one place a load becomes a word, so a hardcoded
"kg" there reads as a bug on a phone that chose pounds.

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
NOT made — the owner chose core-last only.There is deliberately **no "train more legs" control**. "More legs" does not
say whether it means more frequency, more volume or more exercises, it would fight the
evidence, and Option B plus the goal input already cover both ends of it.

## The welcome screen runs the OWNER'S TEMPLATE ENGINE (2026-10-06)

The owner pasted a "workout template engine" prompt (days → split → exercise
selection → goal slot templates → duration adjust → JSON) and then said: *"apply
the exercise making changes for the welcome screen with the things and information
i gave you, and then update the app on my phone."* That instruction RESOLVES the
three conflicts recorded when the prompt arrived — the owner's newer information
wins over the older rulings **on this path**:

- **The split table is the prompt's, verbatim** (`WorkoutTemplates.engineSplitFor`):
  1-2 days full body, 3 full body ×3, 4 upper/lower ×2, 5 push/pull/legs/upper/lower,
  6 PPL ×2, 7 PPL/upper/lower/arms/core. It is NOT `styleTemplates` — that table and
  its literal-table tests are untouched and still feed everything else; only the
  welcome screen and goal regeneration were redirected.
- **The goal owns SETS as well as reps and rest** on this path (hypertrophy 4×8-12/90s
  …, strength 5×4-6/120s …, endurance 3×15-20/30s …), which overturns the shipped
  principle "the goal owns reps and rest; it never touches set count" **for the engine
  week only**.
- **No same primary muscle on consecutive days** is applied best-effort: it holds on
  every split whose day types differ (the tests assert it for 4-7 days), but it CANNOT
  hold on the prompt's own full_body weeks — the prompt is internally inconsistent
  there, and the full-body repetition is accepted rather than hidden.

`generateEngineProgram` is the function; `generateAssistedProgram` stays and its tests
still pin it. Three call sites moved: FirstTimeSetupScreen's `saveSelectedBuild`, its
`buildMissing` preview (**must** stay the same generator as the save, or the warning
lies about the build), and `WorkoutViewModel.regenerateProgramWithNewGoal`. Exclusions
are case-insensitive partial matches resolved through `ExerciseLocalization.canonicalFor`.

**DURATION IS A TIME RULE, NOT AN EXERCISE COUNT — the owner's same-day correction
(2026-10-06: "the session lenght the user inputs is not correctly applied").** The
first version translated the prompt's "~1 exercise per 10 minutes" into a flat
4/5/6 slots for quick/standard/full. That assumed every slot costs ~10 minutes; these
templates cost very different amounts — a strength slot of 5×4-6 at 120s rest is ~13
minutes, an endurance slot of 3×15-20 at 30s rest is ~5.6 — so a flat count produced
21-minute "30-45 min" days and 31-minute "60-75 min" days. Slots are now added by
`engineSlotsFor`: template first, then the goal's isolation top-up, while the running
cost (`engineSlotCostSeconds`: sets × (reps-aware work + rest) + a 90s transition) is
under the window's MIDPOINT and never past its END. Two bounds make it safe, and each
resolves a conflict inside the prompt itself:

- **The working-sets guard scales with the window** (`engineSetsGuard`: 20/24/28).
  The prompt's flat 20 sets is what collapsed its own 60-75 window onto its 45-60
  one — 20 hard sets of these templates cost ~40-45 minutes, full stop. The newer
  duration instruction wins, exactly like the split table did.
- **The prompt's flat 6-exercise cap is superseded on this path.** Six slots cannot
  fill a 45-60 minute window at these slot costs; the counts now run 5-12 per day
  (the old generator shipped 6-12 for the same windows). The caps test pins the
  window-scaled guard instead of a count.

Verified by `thePickedDurationVisiblyChangesTheWeek`: exercise counts must rise
STRICTLY quick < standard < full for every goal (6/9/10, 5/7/8, 7/9/11, 7/10/12) —
the defect was three durations producing the same week. `EngineProgramTest` (8 cases)
pins the split table verbatim, the literal template PREFIX of every day (top-ups
append, never reorder), the window fill through the SHARED cost function (public so
test and generator cannot drift), the guard, exclusions non-vacuously, the dumbbell
gate structurally, consecutive-day disjointness for 4-7 days, and compound-first
slots conditional on the pool having a compound. 622 tests / 0 failures; the APK was
re-installed on the SM-S906B via `adb install -r` and relaunched clean — but NOT
watched on screen (pattern lock).

## The balance card judges HEALTHY RANGES anchored on the generator, never zero-presence (2026-10-06)

Owner: *"also it says unbalanced workout routine when the suer didnt filter any
muscle groups out, so remake the 'unbalanced' message based on healthy amount of
exercises per week on which days and which muscle groups (by default all unless
the user changes it from the advanced tab)"*.

The old card was a BINARY PRESENCE CHECK per advanced-tab part — any group with
ZERO exercises turned it red. That fired on every engine week under seven days
(the prompt's full-body table has no direct core; only the 7-day split has a core
day), even with nothing filtered. Remade:

- `WeeklyBalanceIssue` + `weeklyBalanceIssues(exercises, scopeGroups)` in
  `WorkoutComponents.kt`: seven balance groups (chest, back, legs, shoulders,
  biceps, triceps, core), each with a healthy weekly RANGE of direct exercises.
  Only out-of-range groups render — each as a line with the count, the DAYS it
  trains on, and the range (`balance_issue_line`; `balance_issue_line_no_days`
  when the count is 0, because a group with no exercises has no days to name).
- **The ranges are anchored on the generator itself, and that IS the rule.**
  Measured over all 315 no-filter engine weeks (1-7 days × three windows × five
  goals × three difficulties): the thinnest week the templates produce is
  1 chest / 1 back / 3 legs / 0 shoulders, arms and core (core is 0 in **270 of
  315** weeks — only 7-day weeks get a core day), and the thickest is
  6 / 12 / 24 / 18 / 12 / 8. So the floors are chest 1, back 1, legs 3, and
  shoulders/biceps/triceps/core **0**, with the ceilings just above the thickest
  template week (9 / 14 / 24 / 20 / 12 / 12 / 10). A floor of 0 is honest, not a
  shrug: the app's own templates legitimately contain none of those groups in a
  week, so "none" alone is not a defect — a pile past a ceiling still is.
  **Do not "tighten" these floors back to 2-3**: that red-flags the app's own
  default output, which is exactly the defect the owner reported.
  `BalancedDefaultBuildTest.everyDefaultEngineWeekIsBalanced` sweeps the whole
  matrix and fails on any such regression.
- **Scope defaults to ALL groups and narrows with the advanced tab**:
  `balanceGroupsForParts(parts)` maps the tab's 19 body parts onto the 7 groups
  (Forearms have no group; an empty selection means all). The tab defaults to
  every part, so an untouched selection checks everything — "all unless the user
  changes it" falls out of the data, with no touched-flag anywhere.
- The card still has teeth where a floor is non-zero: a custom week missing legs
  (floor 3) or chest/back (floor 1) is named with its zero, and junk volume past
  any ceiling is named with its count. The old binary checker's documented
  bodyweight/bands biceps gap is no longer a RED verdict (biceps' floor is 0) —
  it reads as a number inside the healthy band instead.
- `incomplete_workout_desc` lost its `%1$s` (the description takes no argument
  now — a leftover placeholder would throw at runtime), and the per-group detail
  moved into the two line formats, translated in de/it.

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
  the option silently disappears. Until 2026-10-06 seven rows were
  `manual_review_required` (Box Jump, Jump Squat, Plyo Lunge, Plyo Push-Up,
  Nordic Hamstring Curl, Rope Climb, Wrist Roller) and branches into them were
  dropped; they now carry ladders, and those branches **resurrected by
  themselves** in the regenerated data (High Knees → Box Jump, Stability Ball
  Leg Curl → Nordic Hamstring Curl, Plate Pinch → Wrist Roller) — which is the
  rule working, not a side effect to clean up: a reachable target gets its
  branch back the moment it becomes ready. `merge_branches()` still mirrors
  `isProgressionReady` in Python to keep the two definitions in step, and the
  refusal tables it reads are now empty.

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

## The evolution tree's connector runs INTO the card — and has exactly TWO colours

The owner, 2026-10-06, on the progression popup: *"i want the lines not be going out
left but right inside the progression cards."* That part is a rule:

- **The rail sits ON the branch cards' left edge (`spineInset = branchInset`), and each arm
  travels INTO its card, drawn OVER the cards.** Behind them, an arm going anywhere past a
  card's border is covered by the surface it points at — which is why the old arms stopped dead
  at the edge and the rail looked like something the cards were bolted onto from outside. The
  `armInset` is **half** the card's content padding (`CARD_CONTENT_PADDING / 2`, 6dp), NOT the
  full 12dp: at the full padding the arm arrived exactly where the exercise name starts, so the
  line touched the very text it points at — the owner's second pass on the same day: *"the
  progress screen still has the lines too close to the exercises."* Half the padding keeps the
  line visibly inside the card (the first ruling) while the name keeps its clearance. The
  padding itself is one hoisted constant used by both the card and the connector, because two
  hand-typed copies of the number is how the arm came to stop on the text in the first place.
- Drawing the canvas AFTER the cards is load-bearing: move it back into the Box's first position
  and the arms silently disappear behind the cards, with nothing failing. The popup is verifiable
  only through `DebugEvolutionPreviewActivity` (see below) — there are no screenshot tests for it.

**The arm end is MEASURED, and where it is measured is load-bearing too.** `rowRects` are fed
from `BranchCard`'s own `onGloballyPositioned`, placed **below** `.padding(start = inset)`. Handed
in from the call site instead (the natural-looking `modifier = Modifier.onGloballyPositioned { … }`),
it sits ABOVE that padding and reports the **full-width lane** rather than the card, so
`rect.left` came back as the trunk's own edge, `rect.left - origin.x + armInset` fell 87px SHORT of
the spine, and every arm was drawn **backwards into the empty gutter**. Nothing failed: the lines
were still drawn, still two-colour, still attached to a spine sitting on a card's edge — the
opposite of what the owner asked for, and it only showed up when the pixels were measured.
Measured on the SM-S906B on 2026-10-06 (Beige, Push-Up, 5 offers): before the fix the gutter at
x=150 carried a 4px band at every card's centre-y in the row's accent colour and the cards had
none; after it, x 217→251 (the card's left edge + its 12dp content padding) is the row's accent
colour and the gutter is clean background at every y. **That second figure describes the
SUPERSEDED state**: 251 was the text's own start, i.e. the arm ended ON the name, which is what
the owner's "too close" report was about. The arm now stops at 6dp inside the card (18px at the
SM-S906B's density), leaving half the padding as clearance before the text. NOT re-measured on
device — the follow-up session had no `adb` on its machine (exit 127) — so the 6dp is reasoned
from the same measured border, not observed. `TreeElbows` now also refuses to draw an arm
that ends at or behind the spine, so the wrong-direction line cannot come back silently.

**The colours are NOT depth-coded, and that is a settled decision, not an omission.** The same
message also said *"i want the cards to light the color of the lines depending on how deep it
is"*, so a ramp through three theme roles (primary → tertiary → secondary, cycling) was built and
then rejected in the next message:

> dont change the colors of the progression exercises, keep them greyed out ... and have the
> selected exercise be colored actively still in the same primary theme color, not random other
> colors.

So `branchAccent(selected)` returns exactly two things — the theme's neutral for a row and the
brand `primary` for the chosen one — and the connector takes the colour of the card it arrives at,
so a line can never point at a differently coloured box. **Do not reintroduce a depth ramp:** a
diagram that spends three hues on five options reads as decoration, and the only thing the colour
has to say is which one is picked. Five branch rows repeating a hue is the tell — if a ramp ever
returns, that is where it shows first.

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

The CSV's 116 bodyweight rows with `unit == "variation_step"` all declare `repMin = 8` and a
`repMax` of only 12 or 15, so **the CSV carries no max-effort figure for any of them**.
(The seven rows backfilled on 2026-10-06 join them at the same floor 8, so the pool now
has **123** rep ladders — the table and `RepCeilingCoverageTest` cover all 123.)
That is the whole reason a flat cap stood in for the number — 25 for everything, 50 for
core, read off the row's muscle — and it was wrong at **both** ends:

- a **Glute Bridge** stopped at 25 reps, when ordinary programming is 30–50;
- a **Handstand Push-Up** was asked for the same 25, when the movement itself ends near 12.

`RepCeilings` now carries an **authored** number for all 123 (owner authorised reading names
for this on 2026-10-04; the seven backfilled rows were added 2026-10-06). Three things make that safe, and each was a way this could break
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

### The step must hold the Roman climb, or a level-up DEMOTES the reps (2026-10-06)

The step above was span-derived only, and that was a bug the owner found on the device:
"when the rep is max of level 3 (lets say 12) and level 4 comes, it jumps from 12 to 8 as if the
weight would have been increased, but since there is no weight to increase it makes no sense."

The mechanism is the **stage reset**. A level ends on its LAST Roman stage and a level-up resets
the stage to I, so the reps actually go from `repsAtLevel(L) + climb` to `repsAtLevel(L+1)`. When
the span-derived step was SMALLER than the pattern's climb (climb is 4 for most rep rows; a
25-ceiling Push-Up stepped by 2), that reset handed back **fewer** reps — 12+4=16 at the top of
level 3, then 14 at level 4. Core rows happened to be safe only because their step of 4 matched
their climb of 4.

Two changes, and both are needed:

- **`repStepFor` is at least `stageClimb + 1`.** That is what makes "1 level is 5 reps" (the
owner's number) true for every roomy ladder, and the step is still derived per exercise from its
own floor, ceiling and pattern — never its name. `stageClimb(entry)` is the largest
within-level climb over stage I, across the sets.
- **The climb is SCALED into the level's band** (`band = step - 1`) in `prescribedReps`, so the
no-demotion promise holds even where the two disagree. A four-rep-wide skill row cannot hold a
five-rep step AND four rungs, so `repStepCeiling` shrinks the step there and the climb is
compressed to fit. The cascade SHAPE is still the pattern's own; only its magnitude is
normalised.

The two mechanisms cover different rows and each is load-bearing: reverting the step alone left
the invariant tests green (the scaling still holds the band), while reverting the **scaling** made
`levelingUpFromTheLastStageNeverDemotesTheReps` and `theRepCountNeverFallsAnywhereAlongTheLadder`
fail. Both are swept across every rep ladder in the pool, not just Crunches, and both were
verified non-vacuously by that revert.

Two consequences worth keeping in mind:

- **The ladders got SHORTER.** Chin-Ups is now 8/12/16/20 across four levels, not seven —
  decline push-up 8/12/16/20 likewise. That is the owner's "bigger steps" ask, not a
  regression, but the old on-device figure above is superseded by this one.
- **A stored level can sit past its ladder's end**, because the length is derived and this
  ruling shortened every rep ladder. `WorkoutViewModel.currentProgressionState` clamps a stored
  level to `effectiveMaxLevel` at the READ path (the same bound the writer uses), or the device
  shows "Level 7" over a picker that offers four rungs.

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
