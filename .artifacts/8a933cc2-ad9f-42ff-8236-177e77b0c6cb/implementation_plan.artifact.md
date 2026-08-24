# Fix Timeline Symbols Positioning in Today's Schedule

The timeline symbols (dots/icons) on the `HomeScreen` have incorrect positioning and missing line segments. Specifically:
1. The line segments connecting the dots are broken: each item only draws a line *below* its dot, but not *above* it, leaving a gap.
2. The vertical alignment between the symbols and the exercise name text is slightly off (28dp box vs ~24dp text height).

## Proposed Changes

### [composeApp]

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/HomeScreen.kt)
- Update the `drawBehind` logic to draw line segments both above and below the dot/icon.
- Adjust the `Box` height for the symbols and the corresponding `startY`/`dotY` to 24dp/12dp to match the `titleMedium` line height.
- Ensure the line above an item uses the completion color of the *previous* item for a smoother transition, or at least ensure continuity.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to the Home screen.
- Verify that the timeline line is continuous from the first exercise to the last.
- Verify that the dots/icons are perfectly aligned with the center of the exercise names.
- Verify that expanding an exercise doesn't break the timeline line.
