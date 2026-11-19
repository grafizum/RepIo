# Program-quality audit — is the generated week a professional week?

**Date:** 2026-10-08 · **Board task:** owner prompt, same day — *"test each filter and day and day
amounts etc. inside the welcome screen and check if the system that outputs the workout for each
day is truly a professional workout based on your sources"*.

**Reproduce:**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*ProgramQualityAuditTest*"
```

That walks **all 450 welcome-screen configurations** (2–6 days × options A/B/C × 5 kits × 3
intensities × 2 goals, every part picked) and writes:

- `composeApp/build/reports/program-quality/summary.txt` — the readable report
- `composeApp/build/reports/program-quality/rows.csv` — one row per configuration, 17 columns

The test asserts each option's **own documented contract** (2x/week floor for A and C; exactly one
leg day for B; no same-day duplicates; no day under three exercises unless the kit was reported
unsatisfied; no quad work with zero hamstring work; no major muscle under four weekly sets from
three days up) and **reports** everything that is a judgement call, so a soft metric can never be
"passed" by loosening a threshold.

## What the week gets right

- **Frequency.** Options A and C train every major muscle at least twice a week from 3 days up, at
  every kit, intensity and goal — 0 exceptions in 300 configurations. Option B keeps its documented
  trade-off exactly: one leg session at every frequency, never two, never none.
- **Sessions are sessions.** No configuration produced a day with fewer than three exercises, none
  produced a duplicate movement inside a day, and no week trained the quads without any hamstring
  work at all.
- **Structure.** The split templates do what their comments claim; the failures that a first draft
  of this audit reported (120 of them) were all Option B behaving as designed, which is why the
  assertions now encode each option's own promise instead of one generic whole-body rule.

## Finding 1 — a stretch is being spent as a working set (1277 row-slots)

Of the 450 configurations, **1,277 exercise slots are `mobility_stretching` rows** — the 76 catalog
rows whose own progression ladder is hold seconds and whose stated purpose is range of motion. The
generator treats them as ordinary movements:

- Bodyweight lower day (4 days, option A): `Chair Pose`, `Cow Face Pose`, `Dancer Pose`,
  `Pigeon Stretch`, `Clamshell Hold` — five of eight slots.
- Bodyweight pull day (4 days, option B): `Puppy Pose`, `Bow Pose`, `Camel Pose`, `Cat Stretch`,
  `Cat-Cow`, `Child's Pose`, `Chin Tuck Hold`, `Back Extension` — seven stretches.
- The same weeks then report 36 weekly "glute sets" and a `Glutes=36` column that is mostly holds
  and poses.

**Prototyped fix, measured.** `ExerciseCatalog.isMobilityRow(name)` (added, read from the catalog's
own `type` field, never from the name) plus one filter on the **gated** pool in
`WorkoutTemplates` — filter *after* the equipment gate, with a fall-back where the part has no
movement at all:

- mobility row-slots: **1277 → 0** (the only survivor is `Banded IT-Band Stretch`, because Hip
  Abductors has exactly one catalog row);
- bare-kit gaps unchanged (`FilterConsistencyAuditTest`) and no part is dropped
  (`BalancedDefaultBuildTest`) only if the fall-back is applied **after** the gate — filtering the
  raw pool first made a bodyweight-only build lose its back day entirely.

**Why it is not landed yet.** It costs the bare kits, and both costs are asserted elsewhere:

- bodyweight `duration_full` week repeats rise **5 → 9** exercises (`DurationBudgetTest
  .constrainedEquipmentReliesOnVariationsInsteadOfDuplicates`, cap 6);
- one full-session day's third "compound" disappears — it was `Lizard Stretch`, a mobility row
  tagged `compound` in the database (`DurationBudgetTest.fullSessionsKeepACCompoundCoreButAddIsolation`).

The real repair is in the data, not the generator: a kit-free user has **no lat movement at all**
in the pool (the existing `Biceps` gap in `FilterConsistencyAuditTest` is joined by `Lats` and
`Rear Deltoids` the moment stretches stop counting), so the catalog needs real equipment-free pull
movements before the generated week can drop its stretches without repeating itself.

## Finding 2 — the no-kit isometric bucket mixes planks with yoga poses (2404 row-slots)

After the mobility rows, the next fallback is the 55 `isometric_hold_or_carry` rows with no kit.
2,404 slots in the same 450 configurations, and the bucket is not one thing: `Plank`, `Side Plank`,
`Bird Dog Hold`, `Dead Bug Hold` and `Single-Leg Glute Bridge Hold` belong there, `Chair Pose`,
`Dancer Pose`, `Eagle Pose`, `Half Moon Pose`, `Crow Pose`, `Dolphin Pose`, `Three-Legged Downward
Dog` and `Extended Side Angle Pose` do not. `Bow Pose` and `Revolved Chair Pose` are tagged
`compound` and are used as a session's compound lift. The database gives both kinds one training
category, so this is a **catalog decision** (re-tag or re-classify the pose subset), not a
generator bug — the audit prints the counts and the names for the owner to rule on.

## Finding 3 — volume is concentrated, not distributed (reported)

Weekly sets on the single most-loaded muscle run **min 6 / median 21 / max 45** across the space,
and 66 configurations put more than 30 sets on one muscle while another major gets 6. The
dose-response review the generator cites (Schoenfeld, Ogborn & Krieger 2017) puts the productive
band near 10–20 sets per muscle per week and shows the return flattening past ~30 — so the top of
the distribution is past the literature, and the bottom (3–6 sets on a major in the marked option B
weeks) is at the floor. Reported rather than asserted because where the ceiling belongs is a
programming philosophy, not a number the catalog can prove.

---

# On the Pixel 7a — what the welcome screen actually does

**Device:** Pixel 7a (`39211JEHN10619`), debug build 1.7.5 / v52, `adb` + `uiautomator`.
The owner's S22+ was not used. Driver: [tools/welcome_sweep.py](../tools/welcome_sweep.py)
(taps are resolved to the clickable node by label at run time, never by coordinate).
Raw logs and screenshots: `evidence/pixel-2026-10-08/`.

```bash
python3 tools/welcome_sweep.py 39211JEHN10619 inventory   # what the page is made of
python3 tools/welcome_sweep.py 39211JEHN10619 days        # 2..6 days, several day patterns
python3 tools/welcome_sweep.py 39211JEHN10619 tap "<label>" ...
```

## The whole page, top to bottom

`Geführter Aufbau` / `Eigener Aufbau` · **Trainingstage pro Woche** (Mo–So chips) ·
**Wie du deine Woche aufbaust** (Option A/B/C) · **Geräte** (Heimausrüstung: Stuhl, Bank,
Reckstange, Barren, Ringe, Wand, Treppe, Gewichtsranzen, Handtuch; Gym-Ausrüstung: Bänder,
Kurzhanteln, Kniebeuge-Ablage, Langhantel, Kettlebell, Kabel, Maschinen) ·
**Trainingsziel** · **Sitzungslänge** · **Schwierigkeit wählen** · **Weitere Vorlieben** ·
`Speichern`. Nothing is missing and nothing is duplicated (`welcome-inventory.txt`,
`welcome-slide-*.png`).

## Day amounts — 2 to 6 days, two day-patterns each

The chips are read behaviourally (Compose exposes no `selected` flag on them), and **every
tap moved the planned day count by exactly one** in all 20 transitions. A fresh install
starts on **Mo + Mi + Fr**. What the cards promise at each count, as captured on the device:

| days | Option A | Option B | Option C |
|---|---|---|---|
| 2 | Ganzkörper · Ganzkörper (Beintage 2) | Oben · Unten (1) | Ganzkörper · Ganzkörper (2) |
| 3 | Ganzkörper ×3 (3) | Oben · Unten · Oben (1) | Ganzkörper · Oben · Unten (2) |
| 4 | Oben · Unten · Oben · Unten (2) | Drücken · Ziehen · Beine · Oben (1) | Ganzkörper · Drücken · Ziehen · Beine (2) |
| 5 | Drücken · Ziehen · Beine · Oben · Unten (2) | Drücken · Ziehen · Beine · Oben · Arme & Core (1) | Oben · Unten · Drücken · Ziehen · Beine (2) |
| 6 | Drücken · Ziehen · Beine ×2 (2) | Drücken · Ziehen · Beine · Oben · Arme & Core · Schwachstellen (1) | Oben · Unten · Drücken · Ziehen · Beine · Ganzkörper (2) |

`Beintage` is derived, not decorative: it counts the days that train legs, so Option C's
"Ganzkörper · Oben · Unten" reports 2 and Option B reports 1 at every frequency. The chips
also reach **7** days (the screen warns at 7); the sweep's patterns stop at 6.
No warning banner was raised by any day pattern — the frequency warnings only fire at 7.

## Found on the device: the session-length chips printed the wrong unit

`Sitzungslänge` read **"6–8 Min." / "8–10 Min." / "10–12 Min."** — the *exercise* windows
from `durationRangeFor` rendered through the `duration_minutes` string. A 10–12 exercise
session is the longest option the app builds; 10–12 minutes is shorter than its own warm-up,
and the block's own description right underneath says "Ausgewogene Einheiten mit 8–10
Übungen". **Fixed:** `WorkoutTemplates.durationMinutesFor` adds the documented partner
window (30–45 / 45–60 / 60–75), the chip prints it, and
`DurationBudgetTest.durationMinutesMatchTheCoachSpec` pins both windows and their order.
Verified on the Pixel after a reinstall: the chips now read **"30–45 Min." / "45–60 Min." /
"60–75 Min."** (`duration-after-fix.png`).

## The week a real save produces

Fresh install, 4 days (Mo + Mi + Do + Fr), default kit, Muskelaufbau, Standard, Mittel →
`Speichern`. Read back from the Pixel's own database (`routine_exercises`):

- **MONDAY — Upper (9):** Barbell Bench Press 4×8–12 · Barbell Overhead Press 3×8–12 ·
  Dumbbell Lateral Raise 3×10–15 · Close-Grip Bench Press 3×12–15 · Bent-Over Barbell Row
  2×12–15 · Barbell Shrug 2×12–15 · Rear Delt Row 2×12–15 · Dumbbell Bicep Curl 2×12–15 ·
  Dumbbell Fly 2×12–15
- **WEDNESDAY — Lower (9):** Barbell Deadlift 4×8–12 · Leg Press 3×8–12 · Back Extension
  3×10–15 · Crunches 3×12–15 · Barbell Back Squat 2×12–15 · Barbell Calf Raise 2×12–15 ·
  Romanian Deadlift 2×12–15 · Glute Bridge 2×12–15 · Machine Back Extension 2×12–15
- **THURSDAY — Upper (9):** Push-Up 4×8–12 · Dumbbell Shoulder Press 3×8–12 · Barbell
  Upright Row 3×10–15 · Dumbbell Tricep Extension 3×12–15 · Pull-Up 2×12–15 · Dumbbell Shrug
  2×12–15 · Dumbbell Face Pull 2×12–15 · Barbell Curl 2×12–15 · Incline Dumbbell Fly 2×12–15
- **FRIDAY — Lower (9):** Good Morning 4×8–12 · Barbell Hip Thrust 3×8–12 ·
  **Back Lever 3×10–15** · **Plank 3×12–15** · Barbell Lunge 2×12–15 · Dumbbell Calf Raise
  2×12–15 · Nordic Hamstring Curl 2×12–15 · Machine Hip Abduction 2×12–15 ·
  **Sphinx Pose 2×12–15**
- **TUESDAY / SATURDAY / SUNDAY:** rest (`MONDAY — Upper` style titles, `push_desc` /
  `rest_desc` keys, which is the column's own convention — `DefaultData` stores keys there too)

**Every major muscle lands on 2 sessions a week** (Chest, Lats, Front/Lateral/Rear
Deltoids, Upper Traps, Biceps, Triceps, Abs, Quadriceps, Hamstrings, Glutes, Spinal
Erectors, Calves — all 2×), no day repeats a movement, each day leads with a compound and
carries 9 exercises (the Standard window is 8–10), the two upper days and the two lower days
are genuinely different sessions (the second lower day is hinge-led: Good Morning, Nordic
curl, hip thrust), and the push/pull split inside an upper day is balanced.

**What is not professional, on the device, in a mainstream configuration:**

1. **A hold is prescribed in reps.** `Plank 3×12–15`, `Back Lever 3×10–15`. The whole app
   keys "is this a timed movement?" off the row's rep string containing `sec`
   (`WorkoutViewModel`, `WorkoutComponents`, `sets_x_time_pl`), so a plank prescribed as
   `12–15` is asked for *sets and reps* in the workout screen — the timer path never runs.
   The default hand-written routine gets this right (`Plank … "30–60 sec"`).
   Cause: the engine's slot template is positional (`slot.reps` is written to every row in
   the slot), and its only timed-movement test is `isExerciseTimeBased(name)` — a
   name heuristic (`contains("plank") || contains("hold") || contains("rest")`) that
   misses `Back Lever`, every pose, and every stretch. The data that would answer it
   correctly already exists: `ExerciseProgression.forName(name)?.unit == "seconds"`.
2. **A pose is spent as a working set.** `Sphinx Pose` closes the second lower day as
   `2×12–15` under Spinal Erectors — the `mobility_stretching` finding below, reproduced
   in one ordinary tap-through of the welcome screen.

Both are one repair: judge the row by its ladder, not its name. It is **not landed** because
the fix collides with a promise the owner wrote down —
`EngineProgramTest.theTemplatePrefixIsLiteralAndTheWindowIsFilledByTime` asserts the goal
table is the literal PREFIX of every day's slot list, so giving a hold its own seconds
prescription needs that contract restated (the template describes the session's scheme; a
hold is a measurement exception inside it), and the mobility filter costs the bare kits
(see Finding 1). Both are decisions about the prescription model, not typo fixes.

## The suite after these edits

`./gradlew :composeApp:testDebugUnitTest` after the last change: **658 tests, 0 failures,
0 errors** from 88 JUnit XML reports (the `ProgramQualityAuditTest` run alone walks the 450
configurations and writes `build/reports/program-quality/`).

## The grey background behind the beige screen — fixed and verified on the Pixel

`Theme.MyApplication` carried **no `windowBackground`**, so the window fell back to
AppCompat's `DayNight` default — the flat medium grey the owner reported underneath the
light beige level-guide page. `values-night/themes.xml` also *redefined* the style, which
drops the other file's items, so a dark system appearance had nothing to inherit from.

Fix: `android:windowBackground = @color/bg_beige` on `Theme.MyApplication` in **both**
`values/` and `values-night/` (and the base splash theme now flashes beige instead of
flame). Verified two ways:

- **Rendered:** the `Neu! Level-System` page on the Pixel, with the system in LIGHT and then
  in DARK appearance — edges and gaps sample `#FDFCFB` (= `bg_beige`) and the card surface
  `#F9F7F4` in both; no medium-grey band anywhere in the frame
  (`guide-live-light.png`, `dark-mode-guide.png`; the system appearance was left back on
  `auto` afterwards).
- **Shipped artifact:** `aapt2 dump resources` on `RepIo-v1.7.5-v52-debug.apk` shows
  `style/Theme.MyApplication … 0x01010054(@color/bg_beige)` on the unqualified config, so
  every configuration — night included — resolves the beige background.
