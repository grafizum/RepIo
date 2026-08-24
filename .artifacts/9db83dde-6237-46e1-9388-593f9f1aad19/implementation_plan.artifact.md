# Implementation Plan - Customize Button Pressed Feedback

The goal is to remove the default grey ripple effect ("grey button pressed box") from all buttons and replace it with either no feedback or a subtle, theme-matching animation (like a slight scale-down) using Compose's `LocalIndication`.

## User Review Required

- The ripple effect will be disabled globally for all components that use `LocalIndication` (Buttons, IconButtons, Clickables, etc.).
- A custom "Scale" animation will be introduced to provide visual feedback without the grey box.

## Proposed Changes

### Theme

#### [MODIFY] [Theme.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/theme/Theme.kt)
- Define a custom `NoRippleIndication` or `ScaleIndication`.
- Update `WorkoutTrackerTheme` to provide this indication via `CompositionLocalProvider`.

## Verification Plan

### Manual Verification
- Deploy the app and tap various buttons (e.g., "Workout Now", exercise cards, icon buttons).
- Verify that the grey ripple box is no longer visible.
- Verify that there is either a subtle scale animation or no "box" effect at all.
