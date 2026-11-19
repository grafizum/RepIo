# The add-exercise card shows exercises again — 2026-10-11

Owner, on the phone: *"i told you to remove the input boxes under the exercises and the 'exercise
(34)' label i want it to actually show all of those exercises in a scroll box kind of like
replacing exercise. not just a label but actually exercises i can select from."*

## 1. The cause, as a measurement

The picker's list asked its parent for *whatever is left* (`Modifier.weight`). Both add-cards draw
the picker inside `AppDialog`'s **default, scrolling** body — and a scrolling body has no leftover
height to divide, so the lazy list was measured with an unbounded height and laid every row out at
**0px**. The owner saw the search field, the two filters and the count line, and nothing to pick.

The replace dialog — the one the owner names as *"kind of like replacing exercise"* — already works
for exactly one reason: it passes `scrollableBody = false`, which gives its list a bounded height.

The suite was green through all of this because `ExercisePickerBodyTest` hosts the picker in a
bounded `Column(fillMaxSize)`, where `weight` resolves normally. A test that hosts it the way the
dialogs do — in a scrolling body — reported:

```
the card's rows have no height inside a scrolling dialog body (0.0px),
so the user sees a count line and nothing to pick from
```

## 2. The fix

* **The list has a height of its own** (`ExercisePicker.kt`): `heightIn(min = 200.dp,
  max = 0.40 × window)`. Bounded information the list can lay out against whether its parent
  scrolls or not. Short pools still wrap and make a short box.
* **The "Exercises (N)" caption is gone.** Its count moved to the list's accessibility description:
  spoken, never drawn. That keeps the strong assertion (`ExercisePickerBodyTest` still compares the
  card against the pool `exercisePickerPool` derives) without a visible label.
* **No input boxes under the list until something is picked.** The level-owned sets / reps / weight
  fields no longer draw in picker mode while the card is still a list of exercises — with nothing
  chosen they could only ever be empty, which reads as a second, wrong way to name the movement.
  They return for a movement the level system has no ladder for (a stretch), which is the fallback
  that keeps such a movement writable.

Reset survives (a way out of a filter is a control, not a caption), and its row is only drawn when
there is something to reset.

## 3. Render evidence

`01-list-in-scrolling-body.png` is Roborazzi (native graphics) at the phone's own geometry
(1438x3118 = Pixel7Pro qualifier), captured from a throwaway test that was deleted again. Measured
off the render by badge colour, one band per exercise row:

| what | value |
| --- | --- |
| rows drawn in the box | **6** |
| each row's badge band | 22dp |
| list band | y 686..1777 = **312dp** |
| the cap (0.40 × 891dp window) | 356dp |

Before the fix that band was 0px tall.

## 4. Verification

* New `AddExerciseCardTest` renders the **dialog** (`ExerciseEditDialog`, add mode, level system on)
  with three cases: the rows have height; no count caption is drawn; the boxes arrive with a pick
  and not before.
* `ExercisePickerBodyTest` gained `theListSurvivesAScrollingDialogBody` — the reproduction.
* Full suite: **104 suites / 759 tests / 0 failures / 0 errors / 0 skipped**, exit 0 under pipefail,
  counts parsed from the JUnit XML.

## 5. Still owed

Not seen on a phone: no adb device was attached to this machine, so this render is Robolectric
output at the phone's geometry rather than a photograph.
