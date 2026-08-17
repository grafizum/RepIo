# Smooth Transition for Reps/Time Switch in Add Exercise Dialog

Improve the user experience when switching between "Reps" and "Time" in the exercise dialogs by implementing a smooth sliding toggle instead of the current disappearing/expanding buttons. This will be more visually appealing and intuitive while remaining resource-efficient.

## User Review Required

> [!IMPORTANT]
> The toggle will now always show both "Reps" and "Time" options even after an exercise is selected. This allows users to manually override the exercise type if they wish (e.g., tracking a bodyweight exercise by time instead of reps).

## Proposed Changes

### UI Components

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)
- Add a new internal `@Composable` called `RepsTimeToggle`.
- It will use a `Box` with a sliding `Surface` indicator.
- Use `animateDpAsState` for the sliding animation and `animateColorAsState` for text colors.
- Replace the existing `Row` + `AnimatedVisibility` logic in `ExerciseEditDialog` with this new component.
- Remove the `!isExpanded` restriction to keep the toggle always accessible.

### Screens

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)
- Replace the `Row` + `AnimatedVisibility` logic in its local `AddExerciseDialog` with the new `RepsTimeToggle`.

## Verification Plan

### Manual Verification
1. Open the **Home** tab and edit an existing exercise.
2. Verify that both "Reps" and "Time" are visible in the dialog.
3. Click "Time". Verify the slider moves smoothly to the right and the input field changes to a time dropdown.
4. Click "Reps". Verify the slider moves smoothly back to the left and the input field changes to a text field.
5. Search for a time-based exercise (e.g., "Plank"). Verify the toggle automatically switches to "Time" with a smooth animation.
6. Repeat the same in the **First Time Setup** flow (Onboarding).
