# The level lock chip, anchored to the card's content column — 2026-10-11

Owner: *"change the positioning a bit of the level lock to the right so that it doesnt look like
its randomly floating and not anchored somehow or aligned somewhere"*.

## What was wrong, as a number

Measured on the phone (`evidence/phone-2026-10-10/16_monday_now.png`, SM-S906B, 1080px @ 420dpi),
the header row of the live card read:

| edge | x (px) | who |
| --- | --- | --- |
| card's inner left | 96 | the level box `Level 4 │ I` starts here |
| level box right | 322 | |
| lock chip | 617 .. 852 | the control in question |
| pencil glyph | 917 .. 956 | |
| card's content column | 983 | where `NEXT UP` ends, 20dp inside the card |

So the chip's right edge stopped **50dp short** of the one column the rest of the card is built on,
**112dp** of empty card sat behind its left edge, and **25dp** of the pencil's *invisible* 48dp
padding sat in front of it. Every displayed relationship was to something invisible.

## The change: the order, not a distance

Parked **left** of the pencil, the chip's right edge can only ever land on the pencil's padding -
the pencil's tap target has ~14dp of padding on each side, so no nudge closes that gap without
eating the target. With the **pencil first and the chip last**:

* the chip's right edge **is** the card's content column (the same x `NEXT UP` ends on);
* the pencil tucks in behind it 2dp away, at ≈8dp of visible air - the pair still reads as one
  cluster at the edge;
* the chip is the one control the owner asked users to *understand* ("show it somehow else so that
  the user can understand"), so it is what closes the header.

Files:

* `composeApp/src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt` — the pair's order
  swapped, and the pencil's `+6dp` shift turned into `.padding(end = 6.dp)` + `CenterEnd`.
  It used to be a `.offset` on the 48dp box, which now would drag the target 4dp **into** the chip
  and hand the taps nearest the chip to the lock instead of the pencil.
* `composeApp/src/main/java/com/grafizum/repio/ui/screens/LevelSystemGuideScreen.kt` — the guide's
  example row is meant to *be* the card's drawing, so it was swapped too.
* `composeApp/src/test/java/com/grafizum/repio/LevelCardLockPlacementTest.kt` — the permanent guard,
  rewritten around the new geometry (see below).

## Verification

`LevelCardLockPlacementTest` (Robolectric, native graphics, Pixel7Pro qualifier), printed numbers:

```
locked=false: chip [298.86, 390.57] (91.71dp wide), pencil glyph [270.86, 290.86] in target
              [248.86, 296.86], air glyph->ring 8.0dp, chip right edge 0.14dp from the card's
              content column (390.71), chip left at 73% of the card
locked=true : chip [314.57, 390.57] (76.0dp wide), pencil glyph [286.57, 306.57] in target
              [264.57, 312.57], air glyph->ring 8.0dp, chip right edge 0.14dp from the card's
              content column (390.71), chip left at 77% of the card
```

Asserted: the chip's right edge lands on `rootRight - cardPadding` (1.5dp tolerance); the pencil's
target ends 2dp before the chip (no overlap, in either lock state); the visible glyph->ring air
stays inside 4..12dp; the chip is past the card's midline; the pencil keeps its 48dp square; and
the chip stays wider than the pencil's target + 20dp (they remain distinguishable).

Full unit suite: **741 tests / 101 suites / 0 failures / 0 errors / 0 skipped**, exit 0 with
`set -o pipefail` (`./gradlew :composeApp:testDebugUnitTest`).

Installed on the phone as part of this pass: `RepIo-v1.7.5-v53-debug.apk` → `SM-S906B`, installed
successfully at 13:39.

## The two PNGs in this folder, and how to make them again

`01-card-unlocked.png` / `02-card-locked.png` are Robolectric renders of `LevelPrescriptionCard` at
the phone's own geometry (411dp @ 420dpi), captured with a throwaway test that was deleted again:

```kotlin
composeTestRule.onRoot().captureRoboImage(filePath = "/tmp/card_unlocked.png")
```

run as `./gradlew :composeApp:testDebugUnitTest --tests "…TempCardShot" -Proborazzi.test.record=true`.
The capture is a no-op without that property (the placement test measures bounds instead, which is
why it needs no golden).

## Limit of this evidence

**No on-device photo in this folder.** At 13:38 the phone answered the app launch with its lock
screen (`Device locked`, "Oma & Opa mode" home screen), so `uiautomator` could only see the lock
screen and the RepIo screenshots from 2026-10-10 are the newest real ones. The APK carrying this
change **is** installed, so unlocking the phone and opening Workout Now shows exactly the render
above. The numbers above are layout measurements, not a photograph of pixels on the phone's panel.
