# Implementation Plan - Remove Master Workouts & Sync Edit Flow

This plan removes the "Master Workouts" concept (automatic exercise variations) and ensures that the UI/UX improvements made to the exercise edit flow are consistently applied to the Home tab and the Live Workout tab.

## User Review Required

> [!IMPORTANT]
> Removing Master Workouts means exercises like "Push-ups (Master)" will no longer automatically cycle through variations (e.g., Diamond, Wide). The user will now have full control over specific exercise selections.

- The **Home Tab** will now support editing, replacing, and deleting exercises directly from the exercise cards, matching the **Routine Tab**'s capabilities.
- The **Live Workout Tab** will gain an "Edit" button for the current exercise, allowing quick adjustments during a session.

## Proposed Changes

### 1. Data & Templates Layer

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- Remove `masterWorkouts` map.
- Remove `resolveMasterWorkout` function.
- Update `getAllExerciseNames` to remove `masters` list.
- Update `getAlternativeExercises` to remove `relevantMasters`.
- Update `getMuscleGroupForExercise` to remove the master workout handling block.

#### [MODIFY] [strings.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/values/strings.xml)
- Remove `ex_master_desc`.
- (And same for `values-it/strings.xml`, `values-de/strings.xml`).

---

### 2. UI Components Layer

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)
- Remove `(Master)` suffix handling in `translateExerciseName`.
- Remove master workout case in `getExerciseDetails`.

---

### 3. Screen Refactoring

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/HomeScreen.kt)
- Remove `resolveMasterWorkout` usage.
- Add state for `editingExercise` and `replacingExercise`.
- Implement `ExerciseEditDialog` and `AlternativeExercisesDialog` (or similar) to match `RoutineScreen`.
- Pass `onEdit`, `onDelete`, `onReplace`, `onSaveVideoUrl` to `ExpandableExerciseCard`.

#### [MODIFY] [WorkoutNowScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt)
- Remove `resolveMasterWorkout` usage.
- Add an "Edit" icon/button next to the current exercise name.
- Add state for `editingExercise` and show `ExerciseEditDialog` when triggered.
- Ensure the live session updates when an exercise is edited (e.g., target reps/sets change).

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)
- (Cleanup) Ensure it uses the shared components correctly after Master workout removal.

## Verification Plan

### Automated Tests
- Run `gradlew test` to ensure no regressions in data parsing.
- (If UI tests exist) Verify cards in Home tab show action buttons.

### Manual Verification
1. Open the app and verify no "(Master)" exercises appear in selection lists.
2. Go to **Home Tab**, expand an exercise, and verify "Edit", "Replace", and "Delete" buttons are present and functional.
3. Start a **Live Workout**, tap the Edit button on the current exercise, change the target reps, and verify the log stepper updates immediately.
4. Verify that switching between Reps and Time in the Edit dialog works and reflects correctly in all tabs.
