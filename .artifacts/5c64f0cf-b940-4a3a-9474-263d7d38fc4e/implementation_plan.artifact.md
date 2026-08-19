# Implementation Plan - Enhance Edit Button Visibility in Workout Tab

The goal is to improve the visibility of the "Edit" button next to the day of the week in the `WorkoutNowScreen`. The user reported that it "hardly looks like a button". We will increase its size, make the border more prominent, and adjust the spacing and icon size.

## Proposed Changes

### [UI Components]

#### [MODIFY] [WorkoutNowScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/WorkoutNowScreen.kt)

- Update the `OutlinedButton` in the header section:
    - Increase height from `32.dp` to `36.dp`.
    - Explicitly set `border` to `BorderStroke(1.dp, MaterialTheme.colorScheme.primary)` when not in developer mode to make it more visible.
    - Increase icon size from `14.dp` to `16.dp`.
    - Use `MaterialTheme.typography.labelMedium` for the text instead of `labelSmall`.
    - Add a very subtle background color (`primary.copy(alpha = 0.05f)`) to the outlined button to improve its "button-like" appearance.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to the "Workout" tab.
- Verify that the "Edit" button next to the day name is clearly visible and looks like a button.
- Check both "Developer" and "Normal" skins (themes).
