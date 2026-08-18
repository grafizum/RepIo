# Science-Based Workout Feature Verification Plan

This plan outlines the steps to verify the "science-based" workout generation and general app features of RepIo.

## Features to Verify

- [ ] **First Time Setup (Onboarding)**
    - [ ] Goal Selection (Bodybuilding, Strength, Calisthenics, etc.)
    - [ ] Equipment Selection
    - [ ] Muscle Group Selection
    - [ ] Intensity Selection
    - [ ] Template vs. Assisted Generation
- [ ] **Workout Generation Logic**
    - [ ] Balanced distribution of muscle groups
    - [ ] Goal-appropriate sets/reps/intensity
    - [ ] Heavy compounds prioritized first in the day
    - [ ] Science insights presence and relevance
- [ ] **Main App Features**
    - [ ] Workout Tab (Workout Now & My Workout)
    - [ ] Analytics Screen
    - [ ] Settings (Theme, User Profile)

## Verification Steps

1. **Deploy & Inspect Initial State**: Launch the app and confirm the onboarding triggers.
2. **Test Scenario A: Bodybuilding (Hard)**
    - Select Bodybuilding, all equipment, all muscle groups, "Hard" intensity.
    - Inspect the generated routine.
3. **Test Scenario B: Strength (Medium)**
    - Select Strength, Barbell/Dumbbell only, specific muscle groups, "Medium" intensity.
    - Inspect the generated routine.
4. **Test Scenario C: Calisthenics (Easy)**
    - Select Calisthenics, no equipment, all muscle groups, "Easy" intensity.
    - Inspect the generated routine.
5. **Feature Deep Dive**: Navigate through all tabs (Workout, My Routine, Analytics) to ensure functionality.
6. **Scientific Consistency Check**: Cross-reference the generated workouts with the logic in `WorkoutTemplates.kt` and general sports science principles (volume, frequency, prioritization).

## Tools Used
- `deploy` to run the app.
- `ui_state` to inspect the UI.
- `adb_shell_input` to interact with the device.
- `read_logcat` to monitor internal states if needed.
