# Implementation Plan - Consolidate Exercises and Dynamic Naming

Consolidate exercises like "Dumbbell Squat" and "Barbell Squat" into a single "Squat" base with an equipment property. Implement dynamic naming that reflects the chosen equipment and whether weights are added to bodyweight exercises.

## User Review Required

> [!IMPORTANT]
> The database schema will be updated to include an `equipment` field in `RoutineExerciseEntity` and `LoggedSetEntity`. Existing data will default to "Bodyweight" or be inferred from the exercise name during a one-time migration logic if possible, though simple default to "Bodyweight" is safer. I will attempt to infer equipment from existing names during the migration.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [WorkoutEntities.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutEntities.kt)
- Add `equipment: String = "Bodyweight"` to `RoutineExerciseEntity`.
- Add `equipment: String = "Bodyweight"` to `LoggedSetEntity`.

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- Update exercise lists to use base names (e.g., "Squat" instead of "Barbell Squat").
- Update `isBodyweightExercise` to be more robust.
- Update `getAllExerciseNames` to return unique base names.
- Add `getAvailableEquipmentForExercise(name: String): List<String>` helper.

### [UI Components]

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)
- Update `translateExerciseName` to accept `equipment: String` and `weightKg: Double = 0.0`.
- Implement dynamic naming: `Name (Equipment)` or `Name (w/ weights)`.
- Update `ExerciseEditDialog` to include an equipment selection dropdown.
- Update `ExpandableExerciseCard` and other components to pass the new `equipment` field to `translateExerciseName`.

### [Screens and ViewModels]

#### [MODIFY] [WorkoutViewModel.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/viewmodel/WorkoutViewModel.kt)
- Update `logCurrentSetAndAdvance` to include the `equipment` from the current exercise.
- Update `saveExercise` to handle the new `equipment` field.

#### [MODIFY] [WorkoutNowScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt)
- Update displays to use the new dynamic naming logic.

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)
- Ensure equipment is passed to UI components.

## Verification Plan

### Automated Tests
- No existing automated tests were found for UI logic, so manual verification is key.
- I will check if the project builds successfully after schema changes (Room migrations).

### Manual Verification
1. Open Exercise Edit Dialog:
   - Verify that "Squat" is available.
   - Verify that an equipment dropdown appears (Bodyweight, Dumbbell, Barbell, etc.).
   - Change equipment and verify the name updates in the UI (e.g., "Squats (Barbell)").
2. Log a Bodyweight Exercise (e.g., "Crunches"):
   - Log with 0kg: Should show "Crunches".
   - Log with 5kg: Should show "Crunches (w/ weights)".
3. Verify that old exercises like "Barbell Squat" are correctly displayed as "Squat (Barbell)" if I can implement a migration or just refactor the templates.
