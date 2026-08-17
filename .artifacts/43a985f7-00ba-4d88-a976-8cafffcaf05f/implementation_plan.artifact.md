# Implementation Plan - Smooth Body Part Transitions in Assisted Build

This plan introduces smooth animations when changing target body parts in the Assisted Build flow, focusing on category switching and validation feedback.

## User Review Required

> [!NOTE]
> The transitions use a combination of fading and horizontal sliding to indicate the "switching" nature of category selection. If you prefer a different animation style (e.g., only fading), let me know.

## Proposed Changes

### [UI Components]

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)

- **`BodyPartsSelectionBlock`**:
    - Use `animateColorAsState` for category button colors.
    - Wrap the inner parts list (dropdown area) in `AnimatedContent` keyed by `expandedCategory`.
    - Implement a `transitionSpec` that slides content horizontally based on the category's relative position (or a generic fade + scale).
- **Assisted Build Content**:
    - Wrap the `MissingBodyPartsWarningCard` call in `AnimatedContent` to handle the transition between balanced and warning states smoothly.

## Verification Plan

### Manual Verification
1.  Navigate to the **Assisted Build** tab in the First Time Setup.
2.  Click different category buttons (Arms, Legs, Chest, etc.) and verify that the list of parts transitions smoothly.
3.  Observe that the category button colors animate between active and inactive states.
4.  Toggle body part selections and verify that the `MissingBodyPartsWarningCard` at the bottom transitions smoothly between the green "Balanced" state and the red "Warning" state.
