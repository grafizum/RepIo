# Scientific Workout Goal Adaptation Plan

This plan refines the workout generation and template adaptation logic to align with scientific research for different training goals. It ensures that exercise selection, volume (sets), and intensity (reps) are optimized for the user's chosen objective.

## Proposed Changes

### Logic & Data Modeling

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- **Enhance `getGoalReps`**:
    - Update rep ranges to strictly follow scientific literature (e.g., ACSM guidelines) for Strength, Hypertrophy, and Endurance.
    - Implement "Volume Compensation": If `targetSets` is capped by the user's `maxSets` or internal limits, increase the rep range to maintain effective total work (Effective Reps).
- **Refine `generateAssistedProgram`**:
    - Improve exercise prioritization per goal:
        - `Strength/Olympic`: Prioritize Barbell Compounds (Squats, Presses, Cleans).
        - `Bodybuilding`: Prioritize a mix of Compounds and High-Tension Isolation (Cables, Machines).
        - `Functional/Athletic`: Prioritize Uni-lateral and Multi-planar movements.
        - `Calisthenics`: Strict bodyweight prioritization.
    - Adjust `setsPerExercise` more dynamically based on both Goal and Intensity.

### UI Components

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)
- **Fix Template Preview**: Implement the `previewTemplateModal` dialog that is currently broken. This dialog will now show the *adapted* version of the premade templates (adjusted for the selected goal).
- **Live Adaptation in UI**: Ensure the "Save & Start" logic correctly applies goal-based rep/set overrides to Premade templates, just like it does for Assisted builds.
- **Goal Impact Summary**: Add a small info box near the Goal selector explaining the scientific shift (e.g., "Strength: Focus on heavy 1-5 rep compounds").

## Verification Plan

### Manual Verification
1.  **Test Goal Swaps**:
    - Select **Strength** goal: Verify exercises shift to heavy compounds and reps move to 1-5 range.
    - Select **Bodybuilding** goal: Verify reps move to 8-12 range.
    - Select **Calisthenics** goal: Verify exercises shift to bodyweight variants where possible.
2.  **Test Set Capping**:
    - Set **Max Sets** to 2 and goal to **Bodybuilding**. Verify that the rep range increases (e.g., from 8-12 to 12-15) to compensate for low sets.
3.  **Verify Premade Adaptation**:
    - Select a Premade template and a Strength goal. Open the "Preview Breakdown" (once fixed) and verify it shows Strength-appropriate reps.
