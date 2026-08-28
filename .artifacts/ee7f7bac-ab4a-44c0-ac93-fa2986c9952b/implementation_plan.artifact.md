# Implementation Plan - Repio Points Analytics

This plan adds "Repio Points" as a new gamified metric for workout analytics. It includes calculation logic for weighted and bodyweight exercises, formatting for large numbers ("k" notation), and updates to the Analytics tab.

## User Review Required

> [!NOTE]
> The "Repio Points" metric replaces the previous "Total Volume" (kg) on the main analytics screen for a more gamified experience.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- Add `calculateRepioPoints(exerciseName: String, weightKg: Double, reps: Int): Double` helper.
- Implementation:
  - If time-based: 0 points.
  - If bodyweight and weight is 0: `1 * reps`.
  - Otherwise: `weight * reps`.

### [UI Components]

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)
- Add `formatRepioPoints(points: Double): String` utility.
- Update `InteractiveVolumeBarChart`:
  - Change labels from "Volume" to "Repio Points".
  - Add Y-axis labels with "k" notation for grid lines.
  - Format the selected bar's value using the new notation.

### [Analytics Screen]

#### [MODIFY] [AnalyticsScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/AnalyticsScreen.kt)
- Update `dailyVolumeMap` to use `calculateRepioPoints`.
- Update `totalVolume` (renamed conceptually to `totalRepioPoints`) calculation.
- Update `totalVolumeForExercise` calculation.
- Update `StatCard` values to use `formatRepioPoints`.
- Change "Total Volume" strings to "Repio Points".

### [Resources]

#### [MODIFY] [strings.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/values/strings.xml)
- Add `repio_points` string.
- Update `total_volume` to "Repio Points" (or add a new one if needed).
- Add `points_label` string.

## Verification Plan

### Automated Tests
- I will add a scratch script to verify the calculation logic for various inputs (BW vs Weighted, 0kg vs >0kg).

### Manual Verification
1.  Open Analytics tab.
2.  Check if "Total Volume" StatCard is now "Repio Points" and shows "k" notation if > 1000.
3.  Interact with the Bar Chart and verify "Repio Points" is shown for each day.
4.  Verify Y-axis labels are visible on the chart.
5.  Log a bodyweight exercise (e.g. Push-ups) with 0kg and verify it adds points (1 per rep).
6.  Log the same exercise with added weight (e.g. 10kg) and verify points are `10 * reps`.
