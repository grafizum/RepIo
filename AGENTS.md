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
