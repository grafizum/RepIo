# AGENT_HANDOFF — RepIo

Cold-start handoff. A new agent session in any terminal should read this first, then
`AGENTS.md` for the project's standing rules.

_Last updated 2026-09-27 by the session that migrated the repo from Windows, then extended by the
session that closed the string-resource gap (see "What was done (2026-09-27, later session)")._

---

## ⚠️ Read this first: Android Studio clobbers the JDK fix

The Gradle JDK the project needs is registered in Android Studio's **global** config:

```
~/.config/Google/AndroidStudio2026.1.4/options/jdk.table.xml
```

`misc.xml` and `gradle.xml` both ask for a JDK **named `17`**. That name is registered, but
it pointed at the Windows/WSL path `$USER_HOME$/.jdks/openjdk-17`, which does not exist on
Linux — so Gradle sync failed and Run stayed disabled.

It is now repointed at the real JDK:

```
<name value="17" />
<homePath value="/usr/lib/jvm/java-17-openjdk" />     openjdk 17.0.19
```

**IntelliJ rewrites `jdk.table.xml` from memory when it exits.** If Android Studio was open
when this was written, the fix is lost and Gradle sync breaks again. Check first:

```bash
python3 - <<'EOF'
import re, pathlib
s = pathlib.Path.home().joinpath('.config/Google/AndroidStudio2026.1.4/options/jdk.table.xml').read_text()
m = re.search(r'<name value="17" />.*?<homePath value="([^"]*)"', s, re.S)
print('OK' if m and 'openjdk-17' in m.group(1) else 'BROKEN -> ' + (m.group(1) if m else '?'))
EOF
```

To repair, replace every `$USER_HOME$/.jdks/openjdk-17` with `/usr/lib/jvm/java-17-openjdk`
(a backup of the pre-repair file sits next to it as `jdk.table.xml.bak-2026-09-27`), or do it
in the UI: **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.

**Do not** point the Gradle JDK at `jbr-17` or `jbr-25`. Both resolve to
`$APPLICATION_HOME_DIR$/jbr`, which on this install is **JDK 25.0.3** — too new for
AGP 8.7.3 + Kotlin 2.1.0. The misleading name is what produced `gradleJvm = jbr-25` in the
first place.

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

Agent token is **not** in the repo — read it from `~/.config/grafizum/agent.env`
(`GRAFIZUM_DASHBOARD_TOKEN`, mode 600). Do not commit it.

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

The open dashboard task ("Exercise names like bycicle crunches shows in the level card") was traced
end to end and the code path is **correct**: `MasteryCard` and `LevelDetailsDialog` both render
through `translateExerciseName`, and `bicycle_crunch` → `Fahrrad-Crunch` is in the German table. The
report was almost certainly filed while the translations were still trapped in XML comments, which
is fixed in `eb9398e`. It still needs one device check on an Abs day to close.

Chasing it turned up a *different* bug of the same shape — visible raw text the user would have
reported the same way:

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
786 keys — parity holds.

## Still open

- **Push `level-system`** — local only, and the string-resource fix above is uncommitted on top of it.
- **Commit `.idea/runConfigurations/`** — currently untracked.
- **The open dashboard task**: *"Exercise names like bycicle crunches shows in the level
  card."* The code path looks right (`translateExerciseName` in `LevelDetailsDialog.kt:155`
  and `WorkoutNowScreen.kt:1483`; `bicycle_crunch` → `Fahrrad-Crunch` is in the German
  table) but it was **never verified on a device** — the routine on the phone was Legs. Do
  not close it without seeing an Abs day.
- **Translation coverage**: 121 of 601 per language; 481 fall back to English.
- **AGP 8.7.3 vs compileSdk 36** is suppressed, not fixed.
- **17 `.aab` files / 194 MB `.git`** still tracked. Untracking rewrites history — owner
  decision.
- **Unmount the old SSD** when finished with it.
- `status.md` on the dashboard predates the Linux migration and the commented-translation
  fix.
