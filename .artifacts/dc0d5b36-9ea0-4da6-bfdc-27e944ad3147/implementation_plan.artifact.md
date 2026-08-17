# Add "Add Workout" and "Add Exercise" buttons to Routine Screen

This plan adds more intuitive way to add exercises to the workout routine. It includes a placeholder for empty days and a dedicated "Add" item at the end of the exercise list.

## User Review Required

> [!NOTE]
> The "Add Workout" placeholder will replace the static "No exercises scheduled" text on empty days.
> A new "Add Exercise" card will be appended to the list of exercises for easier access.

## Proposed Changes

### Routine Screen

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)

- Replace the empty state text with a clickable "add a workout" component (label + plus sign).
- Add an "Add Exercise" list item at the end of the exercises list when not in selection mode.
- Use a consistent design that respects the current theme and "Developer Mode" (LocalDeveloperSkin).

## Verification Plan

### Automated Tests
- N/A (UI visual changes)

### Manual Verification
1. Open the app and navigate to the **Workout Routine** (My Workout) tab.
2. Select a day with no exercises (e.g., a rest day). Verify the "add a workout" label with a plus sign is visible and clickable, opening the add dialog.
3. Select a day with exercises. Scroll to the bottom and verify a new "Add Exercise" card is present.
4. Toggle **Selection Mode** and verify the "Add Exercise" card disappears.
5. Toggle **Developer Mode** (if accessible) and verify the buttons respect the square-border style.
