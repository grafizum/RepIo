# Workout Groups for Muscle Group Specific Replacement

This plan refines the exercise replacement logic to ensure that alternative exercises stay within the same muscle group, avoiding overly broad categorization (e.g., mixing Biceps and Triceps under "Arms").

## User Review Required

> [!NOTE]
> I am tightening the alternative exercise suggestions. Currently, replacing a Bicep exercise might suggest a Tricep exercise because both are under the "Arms" topic. The new logic will strictly suggest exercises from the same specific muscle group (e.g., only Bicep exercises for a Bicep replacement).

## Proposed Changes

### [Workout Logic]

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)

- Refine `getAlternativeExercises` to:
    1. Check if the exercise belongs to a `masterWorkouts` group. If so, return variations of that specific group first.
    2. Use the granular `muscleGroup` directly instead of converting it to a broad "Topic" (via `getTopicForMuscle`).
    3. Filter the generic exercise pools (`getGymExercises`, etc.) using the specific muscle group.
- Expand `masterWorkouts` with more common exercise variations (e.g., Lunges, Pull-ups, Lateral Raises).

## Verification Plan

### Manual Verification
1.  Navigate to **My Routine**.
2.  Tap on a **Bicep** exercise (e.g., Barbell Curls) and select **Replace**.
3.  Verify that the suggested alternatives are only Bicep exercises (e.g., Hammer Curls, Dumbbell Curls) and NO Tricep exercises.
4.  Repeat for a **Chest** exercise and verify suggested alternatives are all Chest-related.
5.  Verify that variations of the same exercise (from `masterWorkouts`) are prioritized if applicable.
