# Device walk, 2026-10-16 — the five changes of the level-system pass

Run twice: on a **Pixel_7a emulator (emulator-5554, 1080x2400, light BEIGE theme, developer
scheme off)** and on the owner's **SM-S906B (R3CT30TW4GZ, 1080x2340, dark/developer scheme)**, both
with the same debug APK `RepIo-v1.7.6-v59-debug.apk`. The phone was driven after the owner freed it
(it was first found locked behind a secure keyguard, then in use with a banking app in the
foreground - nothing was tapped until he said go).

| # | Check | Emulator | Owner's phone (SM-S906B) |
|---|-------|----------|--------------------------|
| 1 | Machines group: nine per-machine ticks | "Machines" header + the nine tiles; tapping the leg-press tile changed **61.2%** of that tile's pixels and **0.0%** of its neighbours | **57.1%** and **0.0%** — same group, same nine captions incl. the new "Other machine" (`phone-machines-group.png`, `...-legpress-ticked.png`) |
| 2 | Rep floor ramp `1 → 4 → 8 → 12` | Push-Up ladder 1 / 4 / 12 / 17 / 22 / 25 (max), level-3 rung 8/8/8; Plank's duration ladder 15/30/45/60/90 s | **identical** ($`phone-pushup-ladder.png`, `phone-pushup-ramp.png`) |
| 3 | Timed clock card mid-run | "TIMED SET" 30s → **25s** after 5 s (control reading "Pause") → **16s** after 11 s | **not re-run** — see below |
| 4 | Progression line on the bars' own scale | line ramps to **y=1157 = the top of Friday's bar**, vertex x≈681 vs the bar's centre 667..670 | on the owner's own 4-day week (82 sets): the line's highest vertex sits at **y=1099, the exact top of the tallest bar**, and never rises above a bar it disagrees with (`phone-analytics-line.png`) |
| 5 | Drag: under the finger, drops on release | held card centre 1697 vs finger 1700 (**2px**); parked-glide case: list travelled three rows, card still under the finger; DB unchanged while held | held card centre 1276 vs finger 1330 (**54px, inside one row**) on the dark theme; DB order **unchanged while held** and committed on the lift (Monday: Pullover slot 1 → 2) (`phone-drag-held.png`) |

The earlier leg of this pass on the phone also contributed `routine-rows.png` (a routine row reads
"4 sets × New" — the level, or New), `picker-open.png` / `picker-after-pick.png` (the list folds on
the pick) and `restart-dialog-in-session.png` / `restart-dialog-completed.png` (both restart entries
ask first).

## Why check 3 was not repeated on the phone

None of the owner's three training days contains a timed exercise, and the clock card only renders
for TODAY's card in a live session. Reaching it on the phone would therefore mean writing a Plank
into his routine AND moving his session's current-exercise pointer — his phone carries a real,
completed Friday session (32 sets logged 13:12-14:53) and 82 logged sets, and a verification that
edits the owner's own training data is worse than one that does not. The clock is verified on the
emulator with the identical APK and the identical code path, and the duration ladder that feeds it
(Plank: 15/30/45/60/90 s) was read on the phone through the picker.

## The phone was left exactly as found

Every phone check was either read-only or was undone before the walk ended, and the database was
compared before and after each one: `routine_exercises` 45 rows before and after, `logged_sets` 82
before and after, and Monday's order restored to
`Dumbbell Pullover, Dumbbell Shoulder Press, Dumbbell Lateral Raise, …` after the drag measured a
reorder. The picker and the equipment wizard were both left through **Cancel/Close**, never Save.

## How #5 was measured

`tools/drag_hold_measure.py` parks the finger with `input motionevent` (a held finger sends no
events, which is the state under test) and reads the held card back out of a screenshot. A
uiautomator dump cannot answer this question: it reports every row's LAYOUT slot, and the held card
is painted away from its slot by `translationY`, so a drag in progress looks identical to one that
moved nothing. The card is drawn at `scale 1.05 / alpha 0.8` (RoutineScreen), so the tool finds it
as the row that is ~5% wider than a resting one and whose surface is the 0.8 blend
(`#F6F3F0` against a resting `#F4F1ED`), then compares its painted centre with the finger's y.

    python3 tools/drag_hold_measure.py emulator-5554 --from-y 1590 --to-y 950 --x 500 --waits 0.8,3.5

The command leaves the finger DOWN (that is the state being measured) and prints the exact
`input motionevent UP` line to send; `--waits 0.8,3.5` measures twice, which is what catches the
auto-scroll glide travelling the list under a motionless finger.

## Logs

`logs/emulator.log` — the emulator's own boot log. Screenshots are full-frame PNGs from
`adb exec-out screencap -p`.
