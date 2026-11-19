# AGENT_HANDOFF — RepIo

Cold-start handoff. A new agent session in any terminal should read this first, then
`AGENTS.md` for the project's standing rules.

_Last updated 2026-09-27 by the session that migrated the repo from Windows, then extended by the
session that closed the string-resource gap (see "What was done (2026-09-27, later session)")._

---

## ⚠️ Read this first: the Gradle JVM must be the registered `17`, never `jbr-*`

Two places decide which JVM Gradle runs on, and only one of them is in git:

| Where | File | Holds |
|---|---|---|
| **Repo, tracked** | `.idea/gradle.xml` | `<option name="gradleJvm" .../>` — this is the one that caused the notice |
| Local, per machine | `~/.config/Google/AndroidStudio2026.1.4/options/jdk.table.xml` | the JDK registry (name → homePath) |

`.idea/gradle.xml` **is tracked in git** (it is not in `.gitignore`), so a wrong `gradleJvm`
there reproduces on every machine and every fresh clone. It carried `jbr-25` until `0d67495`.

**The names are a trap.** This Android Studio install has exactly one bundled runtime directory
(`$APPLICATION_HOME_DIR$/jbr`) and it is **JDK 25.0.3**. Both registry entries below point at
that same directory, so picking either one gives you 25 no matter what it is called:

```
name=jbr-17   home=$APPLICATION_HOME_DIR$/jbr    -> JDK 25.0.3
name=jbr-25   home=$APPLICATION_HOME_DIR$/jbr    -> JDK 25.0.3
name=17       home=/usr/lib/jvm/java-17-openjdk  -> JDK 17.0.19   <-- use this
```

Gradle 8.10.2 supports JVM 8..23, so 25 is rejected at import with *"Please Select Gradle JVM
to Import Project"*. `jbr-17` being the correct major version in name only is exactly what
produced `gradleJvm = jbr-25` in the first place.

**If the notice comes back**, check the tracked file first — it is the thing that travels:

```bash
grep gradleJvm .idea/gradle.xml     # want value="17"
```

then the registry:

```bash
python3 - <<'EOF'
import re, pathlib
s = pathlib.Path.home().joinpath('.config/Google/AndroidStudio2026.1.4/options/jdk.table.xml').read_text()
m = re.search(r'<name value="17" />.*?<homePath value="([^"]*)"', s, re.S)
print('OK' if m and 'openjdk-17' in m.group(1) else 'BROKEN -> ' + (m.group(1) if m else '?'))
EOF
```

To repair the registry, replace every `$USER_HOME$/.jdks/openjdk-17` with
`/usr/lib/jvm/java-17-openjdk` (a backup sits next to it as `jdk.table.xml.bak-2026-09-27`),
or use the UI: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.

**Caveat worth knowing:** IntelliJ rewrites *both* files from its in-memory model when it
saves or exits. A running Studio still holding the old value will overwrite a good
`.idea/gradle.xml` — so when the notice is on screen, click **Use JVM 17** (or set Gradle JDK
to `17` in the settings above) rather than just dismissing it. Editing the file underneath a
running Studio only sticks if Studio's own model agrees.

---

## Environment

| | |
|---|---|
| Project | `/home/matrix/StudioProjects/RepIo` (moved here from WSL on 2026-09-27) |
| Branch | `level-system` — **`main` is deliberately parked at `9206e74` and must stay there** |
| HEAD | `eb9398e` (committed, **not pushed**) |
| JDK | `/usr/lib/jvm/java-17-openjdk` (17.0.19) — export `JAVA_HOME` for CLI use |
| Android SDK | `/home/matrix/.local/share/android-sdk` (set in `local.properties`) |
| Platform | Windows 11 → Linux CachyOS; IDE is Android Studio 2026.1.4 at `~/.local/opt/android-studio` |
| Test device | Samsung Galaxy S22 Ultra `SM-S906B`, Android 16 / SDK 36, 1080×2340 @ 450dpi, adb id `R3CT30TW4GZ` |

Always run Gradle as:

```bash
cd /home/matrix/StudioProjects/RepIo
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew :composeApp:testDebugUnitTest      # 358 tests
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug           # needs the phone on USB
```

---

## Run configuration

`.idea/runConfigurations/RepIo.xml` (new, untracked) — an `AndroidRunConfigurationType`
targeting module `RepIo.composeApp`, `MODE=specific_activity`,
`ACTIVITY_CLASS=com.grafizum.repio.MainActivityBeige` (a manifest-declared activity-alias
for the Beige theme), `Android.Gradle.BeforeRunTask` enabled, device dropdown left as
`DEVICE_AND_SNAPSHOT_COMBO_BOX`, `CLEAR_APP_STORAGE=false` so Run never wipes user data.

Before, the only configs (`composeApp`, `New Run Config`) lived in `workspace.xml`, which
is gitignored — so they were machine-local and would not survive a fresh clone.

`local.properties` points `sdk.dir` at this machine's SDK. It is gitignored; do not commit
it or the path will be wrong elsewhere.

---

## The old Windows environment

The machine still has the old SSD: `nvme0n1p3`, NTFS, label `SSD-475GB`, mounted
read-write via `udisksctl mount -b /dev/nvme0n1p3` → `/run/media/matrix/SSD-475GB`
(96 % full, 22 GB free). It was mounted during this session and **is probably still
mounted**.

The real work was done in **WSL Ubuntu 22.04.5**, not on the Windows side. Its root disk is
a 40 GB image that cannot be mounted without root:

```
C:\Users\guido\AppData\Local\wsl\{643dd5e2-af8a-4e12-90ec-3e419d0f6865}\ext4.vhdx
```

Recovered from the NTFS side (Windows user is `guido`):

- `C:\Temp\sync.config.json` → restored to `sync.config.json` in the repo, gitignored, WSL
  paths rewritten for this machine. The `grafizum-dashboard` repo the sync CLI lives in does
  **not** exist here yet.
- `C:\Users\guido\.gitconfig` → the git identity, re-applied as repo-local config:
  `guido <55624562+MatrixZockt@users.noreply.github.com>`. Without it, commits fail.
- The old copy of the project at `C:\Users\guido\StudioProjects\RepIo` sits at the **same
  commit** as this one, so there is nothing to recover from it.
- Any JDK on that SSD is a **Windows build** and cannot be reused here.

---

## Grafizum Command Center

Project `project_4k096a683a6r0q3b700n3z5q` (`repio`), 49 tasks (48 done, 1 open).

Agent token is **not** in the repo — read it from `.env` in the repo root
(`GRAFIZUM_DASHBOARD_TOKEN`). **Use the `.env` copy, not `~/.config/grafizum/agent.env`** — the
latter is revoked and, because the tools `setdefault` over `os.environ`, exporting it silently
breaks every call with a 401 (2026-10-01). `.env` is gitignored; do not commit it.

`GET /v1/tasks` paginates: `?project_id=...&limit=50&offset=N`, and the project holds **70**
tasks, so one unpaginated read is 20 short. See "What was done (2026-10-01…)" below.

```
GET    /v1/projects/{id}                 project record
PATCH  /v1/projects/{id}                 update description/notes  (needs Idempotency-Key)
GET    /v1/projects/{id}/tasks           tasks
GET    /v1/projects/{id}/documents       list docs
GET    /v1/projects/{id}/documents/{path} one doc's content  (path, not id)
GET    /v1/activity?project_id={id}      activity feed
POST   /v1/events                        app_version report (POST only — GET is 404)
```

`DELETE` on a project is blocked with `owner_approval_required`. The project record was
brought up to date on 2026-09-27 (version, 601-exercise pool, translation coverage, branch
state); `overview.md` regenerates from it automatically.

---

## What was done (2026-09-27)

1. **No JDK** — installed `jdk17-openjdk`. Builds and all 358 tests were unverified until
   then.
2. **114 phantom modified files** — CRLF in the working tree against LF blobs, and an index
   whose stat data was destroyed by the file copy (`dev: 0 ino: 0` on every entry, which is
   why `git update-index --refresh` refused to settle it). Added `.gitattributes`
   (`* text=auto eol=lf`, `*.bat eol=crlf`) and renormalized with
   `git add --renormalize .`.
3. **53 German + 53 Italian translations were complete but trapped inside XML comment
   blocks**, so aapt2 dropped them and the whole level system fell back to English. Found
   only by looking at the phone. Both locale files are now at full parity (786 keys each).
4. **Three `ex_*` strings never existed** (`ex_shoulder_press`, `ex_lateral_raise`,
   `ex_shrugs`). The `getString` wrapper in `util/Resources.kt` returns the key when a
   resource is missing, so this rendered the raw key rather than crashing — which is why no
   test caught it.
5. **Both i18n tools were dead.** `extract.py` parsed a constant that no longer exists;
   `check.py`'s regex assumed multi-line entries and reported "0 entries, no duplicates" for
   both languages — a false all-clear. Both work now (601 exercises, 121 entries per locale).
6. `AGENTS.md` claimed the localization test enforces full coverage. It was deliberately
   relaxed for the CSV clean slate, so the claim was false.
7. Grafizum project record updated; `gradle.properties` got
   `android.suppressUnsupportedCompileSdk=36`; `TODO.md`'s `C:/Temp` path fixed.

## What was done (2026-09-27, later session)

The open dashboard task ("Exercise names like bycicle crunches shows in the level card") is
**resolved and closed**. It was reproduced on the S22 Ultra in German and the cause was *not* the
exercise's own name:

- `MasteryCard` and `LevelDetailsDialog` both render through `translateExerciseName` correctly.
- The untranslated line on the level card was the **VARIATION prescription** — the substitution
  movement the ladder offers at the current stage (`rx.variation`, e.g. "Bicycle Crunch" standing
  in for "Crunches"). That is the canonical pool name and it was passed to the card as
  `primaryText` (`WorkoutNowScreen.kt`), never through the translation helper. On the device the
  card read `Fahrrad-Crunch` directly above a raw English `Bicycle Crunch`.
- The line also only repeated the exercise card above it, and because `primaryText != null` it
  suppressed the per-set action banner (`showChips` requires `primaryText == null`) — so it was
  overriding the layout the card is designed around.
- `fcafdf2` returns null for `VARIATION`. `DURATION` still passes through, because for a
  time-based exercise the duration is the prescription rather than a name. Verified on the device:
  the card now reads `Level 1` / `SATZ 1 VON 3` / `8 Wdh.`, no English name anywhere.
- `LevelDetailsDialog` renders the same variation through `prescriptionDisplayText` and was
  **left unchanged by owner choice** — it will still show a raw variation name.

Chasing the same trail also turned up a different bug of the same shape — visible raw text the
user would have reported identically:

1. **Three string keys were never defined.** `util/Resources.kt` returns the key itself on a
   `getIdentifier` miss, so these rendered literally instead of crashing:
   - `incomplete_workout_routine` and `incomplete_workout_desc` — the *unbalanced* branch of
     `MissingBodyPartsWarningCard` in the first-time setup screen. A user whose workout is missing
     a muscle group saw `incomplete_workout_routine` as the card title. Now defined in en/de/it.
   - `rank_badge_content_desc` — the TalkBack description on the rank badge in `HomeScreen` and
     `AnalyticsScreen`, so a screen reader read a raw key. Now defined in en/de/it.
2. **`StringResourceIntegrityTest`** (`composeApp/src/test/`) now fails the build on either
   failure mode, which is what let 1 ship twice before: a key passed to `getString()`/
   `getQuantityString()` that no `strings.xml` defines, and a `values-de`/`values-it` key set
   that differs from the base file. It strips XML comments before reading key names, so
   re-commenting a translation fails the build the way the 53 lost ones should have.
   Both paths were verified by deliberately re-breaking them. It also checks that a string
   formatted with arguments actually declares a `%…s`/`%…f` placeholder.
   Excluded on purpose: `prefs.getString("setup_days", …)` (SharedPreferences default, not a
   resource) and keys built by interpolation (`getString("intensity_${…}")`).

The 471 resource keys reached through the helper are all defined, and all three locales are at
786 keys — parity holds. Dashboard `status.md` was refreshed with all of the above.

### Driving the app from adb (for whoever does this next)

- `adb` is not on `PATH`: `export PATH=$PATH:$HOME/.local/share/android-sdk/platform-tools`.
- The app forces its own stored language on launch (`MainActivity` reacts to the `app_language`
  pref in `app_settings`), so `cmd locale set-app-locales` alone is overwritten on next start.
  To test German, set the pref to `de` and relaunch. **Restore it to unset afterwards** — it was
  unset before, so the app followed the device locale (`en-GB`).
- `uiautomator dump` + parsing `text=` attributes is the only way to *read* the screen; the raw
  variation name was invisible in the source and only showed up in a dump.
- The soft keyboard covers the dialog buttons and silently swallows taps at the old coordinates.
  Check `dumpsys input_method | grep mInputShown` and send `keyevent 4` before tapping
  Speichern, or the tap lands on the IME. This cost several wasted round trips.

## What was done (2026-09-30)

Two things, one finished and one explicitly not.

**1. Exercise translations, 195 -> 233 per language.** de and it are at exact parity
(233 each, no slug in one and missing in the other), 401 unit tests green, and
`tools/exercise-i18n/check.py` reports no duplicate names or descriptions. The batch
took the whole remaining head of the app's own `POPULARITY` ranking, so the top ~100
movements a user meets in their first weeks all carry German and Italian names and
method text: push-up, pull-up, lat pulldown, inverted row, leg press, plank, side
plank, both leg raises, mountain climber, jumping jack, sit-up, jump rope, incline
press and fly, dumbbell fly and pullover, tricep extension, skull crusher, hammer
curl, shoulder press, lateral and front raise, upright row, shrug, face pull, front
squat, squat, lunge, split squat, hip thrust, glute bridge, good morning, hang clean,
Nordic curl, goblet squat, swing. 369 remain per language, now the obscure tail.

These were written as **coaching text, not literal glosses**. The English source
descriptions are encyclopedia entries and a literal translation fills the app with
English prose wearing a German accent; each new entry says what the movement is
actually for. Coverage stays deliberately unasserted (see `AGENTS.md`), so a partial
table is legitimate while the tail is written.

**2. The push-up animation FAILED and is parked.** The owner asked for a short loop,
watched it twice, and rejected it twice: first a quadruped, then a deformed figure.
The owner then said to stop. **Do not pick this up by re-running the same approach.**

The root cause is worth carrying forward: *an OpenPose skeleton contains no body* -
no head size, no torso width, no limb thickness, no neck. Both attempts asked SDXL to
invent a torso around a stick figure while prone, which is its worst case, and all
601 approved library renders are upright, so nothing here is evidence the prone case
was ever solved. Chaining each frame from the previous one (body inherited from the
approved `goblet_squat` render, 0.55 img2img strength) did fix the drift -
frame-to-frame change fell from ~7.6 to ~4.6 - but it cannot fix the first invention,
and the first invention is the deformed one.

Two process lessons, both of which the owner had to point out:

- **A number that cannot come out the other way is not evidence.** The first clip was
  reported as verified because the aspect ratio "reads as a plank" (1.60 vs 1.16). A dog
  has that silhouette. The measurement that mattered - floor contacts, 2 vs 4 - was not
  run until after the owner had already watched the gif.
- **A partially-unread result must be reported as unread**, not as the part of it that
  happens to support the conclusion.

Zero bytes were downloaded for the animation: SDXL and ControlNet OpenPose were
already cached (11.4 GB, from the Sep 14 probe in `~/sdgen`). The cache is still 12 GB
with the same two repos. AnimateDiff was never installed, per the owner's explicit
"i dont want to keep installing sht on my pc".

What survives and is reusable: `enable_model_cpu_offload` **cannot** work on this
machine - SDXL's UNet (5.1 GB fp16) and the SDXL ControlNet (2.5 GB) are both resident
for the whole denoising loop, 7.6 GB of weights on a 7.7 GB card, so it dies around
step 11 of 30 however the allocator is tuned. `enable_sequential_cpu_offload` streams
the layers and fixes it. And in diffusers 0.40 `enable_vae_slicing` / `enable_vae_tiling`
have moved onto the VAE itself, so the pipeline-level call raises `AttributeError`.

---

## What was done (2026-10-01, after the PC restart)

The session's work was all uncommitted at the restart (43 modified + ~40 untracked files, HEAD
still `ce01b6c`); the owner said **do not commit**, so it is still exactly that. Gradle's state
survived the reboot — the suite re-ran as `UP-TO-DATE`, last full run 445 tests / 0 failures.

**The phone came back, so today's unit-tested work was finally looked at.** It holds up:

| Surface | Result |
| --- | --- |
| Home level card | `Overall rank C` renders at x=127, **left** of `L.3.5` — the reverted-crest placement works |
| Analytics level card | same leading rank at x=127, so the two cards agree |
| Level picker | opens at **Level 2** with Level 4 tagged `current` — `levelPickerInitialIndex`'s 2-row lead-in, i.e. the owner's "exercises below level 4 are not even shown" is fixed on hardware |
| Active-set banner | `SET 1 OF 3` / `8 Reps` / `5 kg each` / `8 / 8 / 8 Reps` |
| Locked fields | "Managed by the level system. Switch to custom to make changes here." |

No crash in any logcat buffer across the sweep.

**One real defect found by looking, not by reading: a pin count rendered as a decimal.** The edit
sheet for Machine Chest Fly (a `stack_step` row) showed label `Pin`, no suffix — and value
**`20.0`**, because the field was seeded with `Double.toString()`. Fixed with
`loadFieldValueText` / `loadFieldPlaceholder`, and the **first-time-setup editor was still
hardcoding `target_weight` / `"kg"` / `"0.0"`** — the second of the two editors, left behind
when the first was fixed. Both now read the unit through the same helpers. 5 new cases in
`LoadUnitTest` (451 total, green), verified non-vacuous by sabotaging `isCount` and watching two
fail, and re-verified on the phone: the field now reads `20` under `Pin`.

**Still NOT verified on hardware, deliberately:**

- **`SplitStyleSelector`** — never seen on a device. Reaching the first-time-setup screen means
  `is_first_time=false` → clearing app data, which would destroy the owner's real routine and two
  completed workouts. Not done unilaterally. Backing the data up with `run-as` + tar and
  restoring it afterwards is possible if the owner wants this checked.
- **`EvolutionDialog`** — needs a finished progression to appear; logging sets would mutate the
  owner's history.
- **The night launcher icon** — needs the launcher, not the app.

**Two threads that need the owner, not an agent:**

- **The weak-point day.** `AGENTS.md` has called it "a genuine follow-up" since 2026-09-29, and it
  is still open — but it is now an OWNER decision, not a cleanup: `styleTemplates` is a static
  table that `theWeeksMatchTheSpecifiedTable` pins verbatim, the weak-point day's topics are
  exactly what that table leaves unnamed, and "lagging" has no agreed definition in the data
  (level depth vs. weekly volume vs. staleness all answer differently). Recorded in `AGENTS.md`.
- **THE DASHBOARD STATUS ENUM IS EXACTLY SEVEN VALUES** — `backlog | planned | in-progress |
  blocked | review | done | cancelled` — and the API rejects anything else with a 400 that lists
  them. On 2026-10-01 the owner asked for `pending` and `idle`; neither exists, and both requests
  were mapped to **`planned`** with the owner's own wording kept in the reason string. Do not
  invent a status: a ledger-only value like `pending` cannot be stored on the board, so the two
  sides drift by construction. `TODO.md`'s vocabulary is now pinned to this enum.
- **SIX BOARD WRITES APPLIED 2026-10-01 with the owner's new key**, each verified by a fresh
  paginated re-read rather than by the 200: 2D animation `cancelled`->`planned`; SDXL renders
  `review`->`planned`; push-up animation `backlog`->`planned`; rank badge `review`->`done`;
  pencil and chest-supported `backlog`->`review`. Board is now 70 tasks / 65 done / 5 open /
  0 cancelled, and every one of the 5 is open deliberately.
- **THE TOKEN CHURNED TWICE IN ONE SESSION, so keep this trap in mind.** The first token was
  already revoked; the second worked (read 70 tasks, took the project PATCH at 14:29) and died
  minutes later; the third arrived from the owner at 16:2x. Anything cached about "the token"
  goes stale within the hour, so **probe before concluding**. Always run with
  `env -u GRAFIZUM_DASHBOARD_TOKEN`: `tools/dashboard_*.py` seed from `os.environ` and then apply
  `.env` with `setdefault`, so an exported stale token WINS over the good one. `dashboard_set_status.py`
  also GETs `/agent/instructions` first to open a 24h write gate, and that grant is PER-PROCESS.
  The live key lives in `.env` only (gitignored, mode 600); it is deliberately absent from
  `.env.example`.

- **THE PHONE'S INPUT IS CURRENTLY STUCK.** It dozed mid-session, woke to the lock screen (where
  `screencap` is pure black), and later the NotificationShade window took input focus and would not
  release it — `uiautomator` and every `input tap` go to the shade while `screencap` still works,
  which is how the pencil's colour fixes were measured from screenshots anyway. Not fixed: an
  `adb` restart of SystemUI would clear it and is the owner's call on their own phone. The pencil
  flow is therefore still **unit-tested but never watched running**.
- **`GET /v1/tasks` PAGINATES AT 50 and the project has 70.** `?project_id=` returns
  `meta.total: 70, limit: 50, offset: 0` and a single read silently drops 20 tasks - which is how
  "5 open tasks" looked plausible while a sixth open item existed. Always loop `offset += len(data)`
  until `offset >= meta.total`. The project's own counters (`total_tasks`, `open_tasks`,
  `done_tasks`) come from the project record and are the cheap cross-check: they said 70/5/64 and
  the paginated read agreed.
- **The project record was stale and contained FALSE statements** until the 2026-10-01 sync:
  "121 of 601 translated per language, the remaining 481 fall back to English" (it is 601/601),
  "version code 47" (it is 48), "working tree clean" (93 changed paths), "HEAD eb9398e"
  (it is ce01b6c). Descriptions rot silently - re-derive every number, never edit one in place.

Good news on the handoff's own open list: `app_language` on the device is already back to
`system`, so the German-pinning left over from the level-card investigation is gone.

## Still open

- **Commit the whole uncommitted tree** — the owner was asked on 2026-10-01 and said not yet.
- **Push `level-system`** — local only, and this machine has **no Git credentials at all**
  (no credential helper, `gh` not authenticated, no SSH keys), so the push has to happen
  from an authenticated machine. 7 commits ahead: `eb9398e`, `e06e93c`, `29fc336`,
  `fcafdf2`, `3a3e35a`, `0d67495`.
- **The old SSD is still mounted** at `/run/media/matrix/SSD-475GB`. `udisksctl` is not
  installed and `umount` needs root, so this needs `sudo` from the desktop.
- **The device's `app_language` pref was set to `de`** to verify the level card — DONE, it reads
  `system` again (checked 2026-10-01). A `Fahrrad-Crunch` exercise is still in Sunday's routine.
- **Translation coverage**: 121 of 601 per language; 481 fall back to English.
- **AGP 8.7.3 vs compileSdk 36** is suppressed, not fixed.
- **17 `.aab` files / 194 MB `.git`** still tracked. Untracking rewrites history — owner
  decision.
- **Unmount the old SSD** — see above; needs `sudo`.
- `status.md` on the dashboard was refreshed this session; it is current as of 2026-09-27.
