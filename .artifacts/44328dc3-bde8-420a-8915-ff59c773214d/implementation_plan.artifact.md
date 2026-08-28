# Persistence for Exercise Changes & Analytics Calendar

This plan addresses two main features:
1.  Ensuring exercise changes (sets, reps, intensity, etc.) made during a workout persist for future weeks.
2.  Adding a workout history calendar to the Analytics screen.

## Proposed Changes

### 1. Workout Now Screen (Persistence)

We will add an "Edit" button to the current exercise card in `WorkoutNowScreen`. This will allow users to modify the exercise parameters (sets, reps, rest, intensity) for the routine template directly while working out.

#### [MODIFY] [WorkoutNowScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt)
- Add an `IconButton` with `Icons.Default.Edit` next to the exercise name.
- When clicked, set `editingExercise` to the current exercise.
- The existing `ExerciseEditDialog` in `WorkoutNowScreen.kt` will handle the saving back to the routine via `viewModel.saveExercise`.

### 2. Analytics Screen (Calendar)

We will add a calendar view to the Analytics screen to visualize workout consistency.

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)
- Add a `WorkoutCalendar` composable that displays a monthly grid.
- It will highlight days that have at least one `LoggedSetEntity` in the `loggedSets` list.
- Support for navigating between months.

#### [MODIFY] [AnalyticsScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/AnalyticsScreen.kt)
- Add a new `IconButton` with a calendar icon (`Icons.Default.CalendarMonth`) to the top header.
- Add a `showCalendarDialog` state to control the visibility of a new `AlertDialog`.
- The dialog will contain the `WorkoutCalendar` component.

## Verification Plan

### Manual Verification
- **Exercise Persistence**:
    1. Start a workout on "Monday".
    2. Click the new "Edit" icon on the current exercise.
    3. Change the "Target Reps" from "8-12" to "10-15".
    4. Save the changes.
    5. Navigate to the "Edit" tab and verify the change is reflected there for Monday.
    6. Change the selected day in "Workout Now" to another day and back to "Monday" to verify the change persists.
- **Analytics Calendar**:
    1. Navigate to the "Analytics" tab.
    2. Click the calendar icon button.
    3. Verify that a calendar dialog appears.
    4. Verify that days where workouts were logged are highlighted (e.g., with a dot or colored background).
    5. Test month navigation.
