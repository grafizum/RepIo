# The equipment filter: accent glyphs, no "Other" chip, the welcome-screen kit — 2026-10-11

Owner: *"for the equipments make sure the minimized icons in replacing the exercise and adding
exercise is not pitch black but a theme color maybe a bit more acented than the usual theme
primary color. and mae sure all equipments that are from the welcome screen apply and the (other)
is not shown. And tell me if there is an exercise that wouldnt show at all if all equipments are
selected (simply say yes and no if there are any exercises and if yes how many)"*

## 1. The glyphs wear the theme accent, measured on the render

`01-filter-row-collapsed.png` is `EquipmentFilterCollapsible` alone at the phone's geometry
(1438x3118 = Pixel7Pro qualifier), captured by Roborazzi (native graphics) from a throwaway test
that was deleted again:

* the collapsed row's ink sits at y 52..116;
* **primary `#8C6239` x-runs `(268, 330)` and `(369, 425)`** — two glyph-sized blobs of 62px and
  56px, i.e. the two kit glyphs (Dumbbell, Bodyweight), tinted with the scheme's accent;
* the **black** ink (`#000000`, x 4..236) is the word "Equipment" — the label, not the icons;
* the remaining primary runs (x 488..733) are the "Show more" button.

The row inherited `LocalContentColor` before, which is the near-black body colour on the light
themes: that is the "pitch black" in the report. The tint now comes from one function,
`equipmentGlyphTint()` in `ui/components/EquipmentVisuals.kt`, which returns
`MaterialTheme.colorScheme.primary`. Primary IS the scheme's single accent (the vivid gold in the
developer theme, the brown in Warm Beige), so a tint *more* saturated than primary is a palette
change rather than a layout one — it is a one-line edit in that function.

## 2. "Other" is not shown, and still filters

Same capture: `selectedEquipment` held THREE categories (Dumbbell, Bodyweight, **Other**) and only
**two** glyphs are drawn — no blob and no "+1" counter for the third.

* Drawn chips come from the new `WorkoutTemplates.filterableEquipmentCategories`
  (every category except `Other`), used by both the collapsed row and the expanded chip grid, so
  the chip cannot be tapped and cannot be unticked.
* The FILTER still carries `Other` (`defaultEquipmentFilter` adds it to the kit), because it is not
  equipment the user can lack — it is the category that needs nothing: a plank, a walk, every
  stretch and pose.
* A filter that is ONLY `Other` (what replacing a plank starts from) has no glyph to draw, so the
  row states it in words — the existing `equipment_tag_none_required` string
  ("No equipment needed" / "Kein Equipment nötig" / "Nessun attrezzo").

## 3. The welcome screen's kit applies

Two defects on the setup screen's dialogs, both fixed by one rule —
`WorkoutTemplates.pickerEquipmentSelection(builderSelection, savedSelection)` = the builder's live
selection while it has one, else the kit the welcome screen SAVED:

* the ADD dialog passed the builder's selection alone, so on a build whose equipment step had not
  been touched yet it opened on every category — equipment the user had already said they do not
  own;
* the REPLACE dialog fell back on the DERIVED category list, and a full-gym selection reduces to an
  EMPTY list (`coarseCategoriesFor` reads "every category" as no preference), so "nothing derived"
  was mistaken for "nothing chosen" and the saved kit narrowed the chips while the gate stayed
  open. The rule now reads the RAW stored lists, and the fixtures assert the distinction.

The routine editor and Workout Now already passed the saved kit; they are unchanged.

## 4. Is any exercise unreachable when everything is selected?

**No — 0 of the catalogue's 601 movements.** Measured on the real catalogue, three ways:

| filter setting | reachable |
| --- | --- |
| all eight equipment CHIPS on | **601 of 601** (0 unreachable) |
| every welcome-screen TILE ticked, as the grid grants | **601 of 601** (0 unreachable) |
| the `Other` category dropped | 457 of 601 — **144 hidden** |

No movement is filed under a category that no chip offers, which is what the first line rests on.
The second line is measured through `tagsGrantedByTicking`: reading a tile's own tag alone
under-counts the families it stands for (the eight stations behind "Machines", `barbell_only`
behind the Barbell tile) and reports 59 hidden movements that the real grid does grant.

`EquipmentFilterCoverageTest` pins all four numbers, so a movement added later cannot quietly
become invisible.

## Verification

`./gradlew :composeApp:testDebugUnitTest` — **102 suites / 749 tests / 0 failures / 0 errors /
0 skipped**, exit 0 under `set -o pipefail`, counts read from the JUnit XML. New this pass:
`EquipmentFilterCoverageTest` (6 cases) and two cases in `ExercisePickerBodyTest` (the drawn row
carries the kit's glyphs, never one for `Other`, and the list under it still contains kit-free
movements; the glyph tint is the accent and is not the body colour).

Not device-verified: this is a Robolectric render at the phone's own geometry, not a photograph of
the phone (it was locked on the last attempt). `02-picker-card.png` is the whole picker card in the
light theme for context; both PNGs are unmodified Roborazzi output.
