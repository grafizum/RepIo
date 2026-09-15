# Agent Guide — RepIo

## Versioning (do this after every work session)

**`version.properties` at the repo root is the single source of truth** for `VERSION_NAME`
and `VERSION_CODE`. Never hardcode a version anywhere else.

Bump `VERSION_NAME` deliberately, based on what actually changed, by running:

```bash
./gradlew bumpVersion -Ptype=feature -Pnote="Short description of what changed"
```

| `-Ptype`      | Bump                                    | Example          |
| ------------- | --------------------------------------- | ---------------- |
| `feature`     | minor + 1, patch unchanged              | 1.2.2 → 1.3.2    |
| `fix`         | patch + 1                               | 1.3.2 → 1.3.3    |
| `breaking`    | major + 1, patch unchanged              | 1.3.2 → 2.0.2    |

Choose the type by intent, not by diff size:

- User-visible new capability or visual/UX work → `feature`
- Correcting existing behaviour, polish, bug fixes → `fix`
- Data migration, removed public API, anything that breaks existing users → `breaking`

Apply this at the end of a work session that changed app behaviour, so the version never
drifts behind the work. Pure chores (docs, comments, test-only, build tooling) do not
need a bump.

### VERSION_CODE

`VERSION_CODE` is incremented automatically by every **Release** build (see the
auto-increment block in `composeApp/build.gradle.kts`), which keeps every Play upload
unique. `bumpVersion` deliberately does **not** touch it — bumping both would double
count, and the gap between two version codes is allowed to span one `VERSION_NAME`
(e.g. `v1.2.2` was released as codes 21 → 38).

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
  entry per pool exercise. **A slug typo does not crash anything** — it silently leaves one
  exercise in English — which is why `ExerciseLocalizationTest` asserts every pool exercise
  has exactly one non-blank entry in every table, that no entry is outside the pool, and that
  names and descriptions are unique within a language. Add a language by adding a table and
  registering it in `ExerciseLocalization.TABLES` (the tests pick it up automatically).
- `translateExerciseName()` is the single display path for an exercise name; `getExerciseDetails()`
  resolves the description the same way and falls back to English, never to blank.
- Search matches the canonical name **and every translation** (`ExerciseSearch.searchCandidates`),
  and always returns the canonical name, so typing `Kniebeugen` finds and stores `Squats`.
  `canonicalFor()` normalizes whatever was typed before muscle detection and before saving.
- Tooling: `python tools/exercise-i18n/extract.py` prints the slug table from the pool (and
  fails if the pool ever gains an exercise with no description);
  `python tools/exercise-i18n/check.py` reports duplicate names/descriptions per language.

When the pool changes, update the tables - `ExerciseLocalizationTest` fails until you do.

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
