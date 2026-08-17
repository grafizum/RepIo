# Integrate Goal-Driven Science-Based Workout Creation

This plan aims to enhance the workout generation logic to be truly "optimal" and "science-based," ensuring the chosen goal (Strength, Bodybuilding, etc.) deeply influences the program structure, volume, and exercise selection. It also adds "Science Insights" to show the user the "thinking" behind the generated workout.

## User Review Required

> [!IMPORTANT]
> The "Assisted Build" algorithm will be significantly updated. It will now attempt to distribute muscle groups across multiple days for higher frequency (2x/week) when enough training days are selected, following modern hypertrophy research.

> [!NOTE]
> A new "Science Insights" section will be added to the setup screen to explain the reasoning behind the generated program.

## Proposed Changes

### [Workout Data & Templates]

#### [MODIFY] [WorkoutTemplates.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt)
- Add `ScienceInsight` data class (title, description, citation).
- Update `GeneratedProgram` to include `scienceInsights: List<ScienceInsight>`.
- Overhaul `generateAssistedProgram` to:
    - Distribute volume more effectively (Frequency > 1x/week).
    - Prioritize Compound movements at the start of sessions.
    - Tailor set/rep ranges and exercise types specifically to the `workoutGoal`.
    - Generate science-based justifications for the program structure.

### [UI Components & Screens]

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)
- Add an `AnimatedVisibility` block in the Assisted Build tab to display `Science Insights` from the generated program.
- Enhance the `Goal Selection` UI to provide more detailed feedback on how each goal changes the workout "thinking."

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)
- Add a `ScienceInsightCard` component to display individual insights with a "Science" badge.

## Verification Plan

### Automated Tests
- Run `gradleApp:assembleDebug` to ensure no compilation errors after refactoring `GeneratedProgram`.
- Add a unit test to `WorkoutTemplatesTest` (if exists, or create one) to verify that a 3-day full-body program correctly assigns major muscle groups multiple times.

### Manual Verification
1. Open the "First Time Setup" screen.
2. Go to "Assisted Build."
3. Select a goal (e.g., "Strength") and equipment.
4. Verify that the generated workout prioritizes compound movements (Barbell) and explains this choice in the new "Science Insights" section.
5. Change intensity and verify that weekly volume adjustments are reflected in the insights.
