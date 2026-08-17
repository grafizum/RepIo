# Unify Dark Theme Backgrounds

The goal is to apply the "blackish" background color from the Flame Orange theme to all other dark themes and ensure all theme colors match well with this new black background.

## Proposed Changes

### [Theme & Colors]

#### [MODIFY] [Color.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/theme/Color.kt)
- Redefine the common dark palette to be "true black" for the background and adjust surface/border colors for better contrast.
- `DarkBackground` -> `Color(0xFF000000)`
- `DarkSurface` -> `Color(0xFF121212)`
- `DarkSurfaceVariant` -> `Color(0xFF1E1E1E)`
- `DarkBorder` -> `Color(0xFF2C2C2C)`
- Adjust specific dark accents (like `ElegantPurpleDark`, `OceanDark`, `RoseDark`, `CrimsonDark`) to be consistent or removed if they are replaced by the common dark background.

#### [MODIFY] [Theme.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/theme/Theme.kt)
- Update all dark color schemes (`Purple`, `Flame`, `Ocean`, `Rose`, `Rainbow`, `Developer`) to use the new shared dark palette.
- Tweak `primaryContainer` and `onSurfaceVariant` for these themes to ensure they pop against the true black background.
- Update `WorkoutTrackerTheme` logic to correctly handle the `RAINBOW` theme's light/dark state.

## Verification Plan

### Manual Verification
- Deploy the app and cycle through all dark themes (Flame, Ocean, Rose, Purple, Rainbow, Developer).
- Verify that the background is consistent (black) across all of them.
- Ensure text and UI elements (containers, borders) are clearly visible and aesthetically pleasing against the black background.
