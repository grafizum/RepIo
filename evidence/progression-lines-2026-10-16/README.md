# Progression popup's lines — measured, fixed, re-measured (2026-10-16)

Owner, 2026-10-16: *"The 'lines' i wanted you to fix graphically werent in analytics it was in the
progression panel that shows when the user did max level of an exercise. So revert the analytics
graph and fix it in the progression. I dont only want the lines reverted in analytics, i want the
whole design reverted."*

The "progression panel that shows when the user did max level of an exercise" is the **evolution
popup** (`ui/components/EvolutionTreeDialog.kt`, titled *"New exercise progression!"*, subtitle
*"An exercise can be progressed once you reach its top level"*), which fires after a workout in
which an exercise reached its top rung.

## How it was seen

The popup only appears when a workout completes with a mastered exercise, so it is driven through
the project's debug-only harness:

    adb shell am start -n com.grafizum.repio.debug/com.grafizum.repio.DebugEvolutionPreviewActivity \
        --es exercise "Push-Up"

Device: Pixel_7a emulator (emulator-5554, 1080×2400 @ 420 dpi), APK
`RepIo-v1.7.6-v59-debug.apk`. Geometry read two ways: `uiautomator dump` for the node bounds, and
a pixel probe over the screenshot (`tools/inspect_shot.py`'s PNG decoder + colour-run scans in
`/tmp/probe*.py` for this pass).

Node bounds at the moment of the capture:

| node | bounds (px) |
|---|---|
| trunk card "Push-Up" | [96, 897] – [983, 1232] |
| branch 1 (selected) | [217, 1253] – [983, 1567] |
| branch 2 | [217, 1588] – [983, 1902] |
| branch 3 | [217, 1923] – [983, 2230] |
| branch 4 (below the fold) | reports bounds `[0,0][0,0]` in the tree |

## The defect, in numbers — before

The spine (the vertical drop) ran **from y = 504 to y ≈ 1236** at x = 214–220 (7 px, `#D7CCC8`):

- **504 is the scroll container's own top** — 393 px ABOVE the trunk card's top and 728 px above
  its bottom, so the line crossed the dialog's subtitle and the entire trunk card (its name,
  its "Finished — top level reached" line and its ladder).
- It **stopped at the trunk's bottom edge** and never reached the branch cards below it, so the
  three real arms (measured at y = 1410, 1745, 2076, each 14–16 px into its card) hung off
  nothing.

Cause: the last row's reported rect is an empty one (a card outside the scroll viewport), and
`TreeElbows` took the spine's far end from `arms.last()` without asking whether that arm could be
drawn. The arm itself was already guarded, so the empty row's arm vanished while the spine was
still aimed at it — at a plausible coordinate, in the right colour, with nothing failing.

Captured: `before-pushup.png`.

## The fix

`TreeElbows` now gates every row three ways before it becomes an arm — the rect must have area
(`!rect.isEmpty`), the arm must land past the spine, and it must sit below the drop — and the
spine ends on `arms.maxOf { it.y }`, i.e. the lowest arm that is genuinely drawn. Each arm keeps
its own row index for its colour, so a dropped row cannot shift the colours of the ones above it.

## After, re-measured on the same device and the same harness

| check | before | after |
|---|---|---|
| stripe above the trunk (y 500–1000 at x 217) | continuous line, 504→896 | none — only the subtitle's own glyphs |
| line through the trunk card (y 1000–1226) | continuous | none — only the card's text and its ladder |
| spine below the trunk | absent | present, trunk bottom (dot at y ≈ 1228–1236) down through both gaps between cards (y 1568–1587 and 1903–1922, unbroken) |
| arms into cards | drawn, but attached to nothing | unchanged: 19 px into each card at y = 1410 / 1745 / 2076, selected card in the brand colour, the others in the neutral |
| arm of the below-the-fold card | phantom (aimed the spine away) | none until it is laid out |

Captured: `after-pushup.png`.

**Not seen on the owner's phone this pass**: the SM-S906B was attached at the start of the pass
and had disconnected by the time the fix was ready to install, so the numbers above are the
emulator's. The fix is geometry, not device-specific, and the APK is the same one the phone would
have been given.

## The Analytics graph: reverted, not redesigned

The 2026-10-16 task that put the weekly chart's trend line on the bars' own scale is reverted in
full — `WeeklyOutputChart` is back to exactly the design it had before it (line in its own band at
`height * 0.4f - volume/max * height * 0.2f`, alpha 0.2, no dots, bars scaled inline), the
`weeklyBarHeight` / `weeklyDayCenterX` helpers are gone with it, and `WeeklyOutputTrendTest` —
which existed only to hold that geometry — is deleted. Verified by diffing the chart against
commit `65d17fe`: the only remaining difference is one trailing space on a blank line.

## Verification on the final tree

`./gradlew :composeApp:testDebugUnitTest :composeApp:lintDebug --rerun-tasks`, exit 0 under
pipefail. **865 tests / 0 failures / 0 errors / 0 skipped** across 124 XML reports, every report
newer than every source file (the +4 over the pre-guard pass's 861 is `EvolutionConnectorGuardTest`
itself). Lint: **0 errors, 60 warnings**, none on a line this task touched — the two
`WorkoutComponents` warnings sit at lines 341/3697 while the chart diff is at line 5700+, and the
only `EvolutionTreeDialog` report item is informational at line 109, outside the guard-gate diff.
