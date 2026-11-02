# The permanent daily recap — evidence, 2026-10-17

Owner prompt: *"The workout completed doesnt permanently show the recap of the day
what was done what muscle groups were trained on today."*

## What was wrong

The recap was a deliberately in-memory accumulator (`recapStartedAt`,
`recapExercises`, `recapVolumeKg` in `WorkoutViewModel`) — built once when the last
set was logged and dropped with the process. The `isWorkoutCompleted` flag
survived a restart (it is date-stamped in prefs); `workoutRecap` did not. So the
morning after — or after any process death — the completion screen showed a
trophy, a set count, and nothing else, and Home had nothing at all.

## What replaced it

`todaysRecapFromLog(sets, startOfToday)` — a pure top-level function that rebuilds
the whole card from the log table (calendar-day filtered), exposed as
`WorkoutViewModel.todayRecap`. The old accumulator, `buildRecap`, `clearRecap`
and the `workoutRecap` state field are deleted; there is exactly one source now.
The muscle split counts sets *actually logged* (the old version credited every
exercise's planned target sets, done or not), and the card gained the "what was
done" half it never had: one row per exercise with sets, reps and load, in
training order.

## Verification on the physical device (SM-S906B, debug package)

`com.grafizum.repio.debug` (applicationIdSuffix `.debug`) — installed alongside
the owner's real app, which was not touched. The debug install already held a
completed Saturday session (9 exercises, 21 sets, 960 kg), which is exactly the
bug's habitat: under the old build this state rendered NO recap.

| capture | what it shows |
|---|---|
| `mw.png` | Workout Now completion view: trophy + count + **full recap card** (tiles 9/21/960 kg/73 min, split "Where the work went", exercise rows "Dumbbell Deadlift — 4 Sets · 12,12,12,12 · 8 kg", "Glute Bridge — 3 Sets · 12,12,12" with no invented kg) |
| `home_top.png` | Home: the same card directly under the hero banner |
| `home_after_restart.png` | `am force-stop` + relaunch → Home card **still there**, identical numbers |
| `mw_after_restart.png` | `am force-stop` + relaunch → completion view **still there** with the whole card |

Text dumps (uiautomator) were taken at every step; the numbers above are read
from the dumps, not from eyeballing pixels.

## Known cosmetic edge

A single-set exercise row reads "1 Sets · 12" — the row reuses the tile's
`recap_sets` string, which is not a plural. Fixing it means a real plurals
resource plus DE/IT forms; not done here because it is outside the ask.

## Follow-up: the swap (same day)

Owner: *"i want 'session recap' instead of 'todays schedule' i want it to be replace in the home
tab when finishing a workout and not have both shown"*.

Home now swaps the whole schedule section for the recap card the moment every scheduled exercise
has its target sets (`nextUpIndex == -1`, the same signal the "next up" highlighting uses), and
the copy that rode above the Overall Level card is gone — the card has one slot. Measured on the
phone in both states:

- **Mid-state (day short by one set):** the seeded session had Bent-Over Row at 2 of 3 — Home
  showed the schedule and NO recap, i.e. the swap waits for a genuinely finished day.
- **Finished (3rd set added to the sandbox DB via run-as/sqlite, 104 prior sets verified intact
  afterwards):** Home showed hero → Overall Level (Lvl. 14) → Session recap (9 / 22 / 1056 kg),
  no "Today's Schedule" or "Expand All" anywhere on a full scroll — capture
  `home_recap_swapped.png` — and the swapped Home survived `am force-stop` + relaunch.

Suite unchanged and green: 871 tests / 0 failures / 0 errors / 0 skipped, lint 0 errors,
60 warnings (baseline).

## Follow-up: the boxed redesign (same day, third prompt)

Owner: *"maybe we should also display a 'exercised finished for todays' and i want the session
recap remade a bit better to have a more boxed modern multi layered design use the ux design
skill if possible and show me a few previews to apply"*.

Three preview directions (A Double-Bezel Console / B Glass Cascade / C Asymmetric Bento) were
built in `design/recap-redesign-previews.html` in the app's real tokens; the owner picked **A**.
Shipped: the card is now shell (`surface`) → core (`surfaceVariant`) → three boxed layers
(`surfaceContainerHigh`, hairline `outlineVariant`) with spaced rounded muscle segments and
legend chips; Home gained a `FinishedTodayBanner` (new `home_exercises_finished` string in
en/de/it, tinted with the theme's accent). Capture: `home_recap_console_redesign.png` — banner,
boxed stat grid, split layer with chips, list layer, no schedule.

Suite still green: 871 / 0 / 0 / 0, lint 0 errors, 60 warnings (unchanged baseline).

## Correction: the label moves inside the card (same day, fourth prompt)

Owner: *"i want the 'all exercises finished for today' box not be separate and instead be inside
the session recap and instead of showing session recap show that boxed label (changed to be
shorter) and make it slightly bigger i want it to be better visible"*.

`FinishedTodayBanner` is deleted; the card's title slot is now the label — a full-width
accent-tinted strip with a check icon and `titleSmall` ExtraBold "Finished for today" (en) /
"Heute abgeschlossen" (de) / "Completato oggi" (it).

**Rendered, not dumped** — the phone disconnected mid-pass, so a new `RecapCardRenderTest`
(Roborazzi, the `GreetingScreenshotTest` pattern) records
`src/test/screenshots/recap_card_console.png` with the real 22-set session on the **Developer
dark family**. `recap_card_console_render.png` (this folder) is the copy: boxed header on the
#191919 core, stat wells on #1E1E1E, chips, list. Gotcha worth the paragraph in the ledger:
dark does NOT come from `isSystemInDarkTheme` here — `resolveColorScheme("DEFAULT")` is Beige in
both appearances, so a render must pass a dark theme family, exactly like the app itself does.

Suite: **872 / 0 / 0 / 0**, lint 0 errors, 60 warnings (baseline). Not yet eyeballed on the
physical phone — that check waits for the device to reconnect.

## Guards

`TodaysRecapFromLogTest` (6 cases): no-sets-today → no card (and yesterday's
sets cannot impersonate today), counts/volume from what was logged, volume null
when nothing weighted, split counts logged sets and sorts by load, rows keep
training order with distinct positive loads, duration = span of the day's sets.
The span unit bug (ms vs s, which would have printed a 3-minute session as
"3000 min") was caught by this test during the pass.

Full suite **871 tests / 0 failures / 0 errors / 0 skipped** (125 reports), lint
**0 errors, 60 warnings** — the pre-task baseline, none on the new code.
