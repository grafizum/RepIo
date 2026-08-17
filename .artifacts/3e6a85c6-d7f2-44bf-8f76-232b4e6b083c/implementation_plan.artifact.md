# Recommended Reps and Sets Auto-fill

This plan implements a "recommended reps" and "recommended sets" feature. When a user adds an exercise and selects its name, the app will automatically fill the "Reps" and "Sets" text boxes with recommended values. It also removes the default values from these text boxes and adds placeholders.

## User Review Required

> [!NOTE]
> The recommended values are science-based defaults (e.g., 3 sets, 8-12 reps for compounds, 10-15 for isolations). The user can still override these values.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- Add `getRecommendedRepsForExercise(name: String): String` to provide range-based recommendations.
- Add `getRecommendedSetsForExercise(name: String): Int` to provide default set counts.

### [UI Layer]

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)
- Update `ExerciseEditDialog`:
    - Initialize `targetSetsStr` and `targetRepsValue` as empty strings for new exercises.
    - Add `placeholder` to sets and reps `OutlinedTextField`.
    - Update `LaunchedEffect(name)` to auto-fill recommended values when a name is selected/entered.

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)
- Update `AddExerciseDialog` with the same logic as `RoutineScreen.kt`.

## Verification Plan

### Automated Tests
- Run `gradle_build` to ensure the project still compiles.

### Manual Verification
1. Open the "Add Exercise" dialog in the Workout screen.
2. Observe that "Sets" and "Reps" are empty and show placeholders.
3. Start typing an exercise name (e.g., "Barbell squats").
4. Observe that "Sets" fills with "3" and "Reps" fills with "8-12".
5. Change the name to "Bicep Curls".
6. Observe that "Reps" updates to "10-15".
7. Repeat the same in the First Time Setup screen.
