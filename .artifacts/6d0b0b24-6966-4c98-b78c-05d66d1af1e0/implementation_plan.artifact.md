# Smooth Transition for Reps/Time Buttons in Add Exercise Dialog

This plan implements a smooth transition for the "Reps" and "Time" buttons in the "Add Exercise" dialog. When an exercise name is selected or typed, the app will automatically detect if it is time-based and hide the irrelevant button, allowing the relevant one to expand across the full width of the container.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- Add `isExerciseTimeBased(name: String): Boolean` helper function to identify time-based exercises (e.g., Plank).

### [UI Layer]

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)
- In `ExerciseEditDialog`:
    - Use `LaunchedEffect(name)` to automatically update `isTimeBased` when the exercise name changes.
    - Wrap the "Reps" and "Time" buttons in `AnimatedVisibility` within the `Row`.
    - Apply `Modifier.animateContentSize()` to the `Row` for a smooth transition.
    - Logic: Show both buttons if the name is blank; otherwise, show only the detected type.

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)
- Apply the same changes as `RoutineScreen.kt` to the `AddExerciseDialog`.

## Verification Plan

### Automated Tests
- I will check if the project builds successfully after the changes.

### Manual Verification
1. Open the "Add Exercise" or "Edit Exercise" dialog.
2. Select or type "Plank". Verify the "Reps" button disappears and the "Time" button expands smoothly.
3. Select or type "Push-ups". Verify the "Time" button disappears and the "Reps" button expands smoothly.
4. Clear the exercise name. Verify both buttons appear again (if applicable, or keep the last state if preferred - the user instructions say "when selecting", so showing both when blank is a good default).
