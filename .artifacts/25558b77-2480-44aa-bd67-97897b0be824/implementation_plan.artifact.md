# Implementation Plan - Add Exercise Descriptions in Editor and Replacement Dialogs

The goal is to ensure that exercise descriptions shown in the "Routine Editor" (for new and existing exercises) and in the "Replace Exercise" dialog match the descriptions shown in the "Home Tab".

## Proposed Changes

### [Routine Editor & Replacement Dialogs]

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)

1.  **Update `ExerciseEditDialog`**:
    *   Replace the usage of `getExerciseStaticDescription(name)` with `getExerciseDetails(name, muscleGroup).first`.
    *   This ensures that the description shown when editing or adding an exercise matches the one in the Home Tab.
2.  **Update "Replace Exercise" Dialog**:
    *   For each alternative exercise in the list, display its description using `getExerciseDetails(altName, exercise.muscleGroup).first`.
    *   Structure the UI to show the description below the exercise name in the alternative exercise cards.

## Verification Plan

### Manual Verification
1.  **Open the Routine Editor**:
    *   Navigate to the "Routine" tab (Editor).
    *   Tap "Add Exercise" or edit an existing one.
    *   Verify that the description shown matches what is displayed in the Home Tab for that exercise.
2.  **Replace an Exercise**:
    *   In the Routine Editor, tap "Replace" on an exercise.
    *   Verify that each alternative exercise in the list shows its description.
    *   Confirm these descriptions match the ones seen in the Home Tab.
3.  **Check Generic Descriptions**:
    *   Select an exercise that doesn't have a specific description (e.g., a custom name).
    *   Verify that it shows the generic description (e.g., "A targeted exercise to build and strengthen your [Muscle Group] muscles.") if that's what `getExerciseDetails` returns.
