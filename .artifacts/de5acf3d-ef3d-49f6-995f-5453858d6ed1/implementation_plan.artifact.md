# Implementation Plan - Compact Assisted Build UI and 2-Column Body Parts

This plan outlines the changes to compact the "Assisted Build" UI in `FirstTimeSetupScreen.kt` and change the "Target Body Parts" selection to a two-column layout.

## User Review Required

> [!NOTE]
> The "Target Body Parts" categories will now be displayed in a two-column grid. The individual muscle groups within each category will still be displayed in a FlowRow when expanded.

## Proposed Changes

### [UI Components]

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)

- **Assisted Build Content Layout**:
    - Reduce vertical spacing between sections from `16.dp` to `8.dp`.
    - Reduce vertical spacing within sections (header to content) from `8.dp` to `4.dp`.
- **BodyPartsSelectionBlock**:
    - Use `FlowRow` to display categories in 2 columns.
    - Reduce card padding from `10.dp` to `6.dp`.
    - Reduce vertical arrangement spacing between categories from `6.dp` to `4.dp`.
    - Adjust badge and icon sizes for a more compact look.
- **EquipmentSelectionBlock**:
    - Reduce vertical arrangement spacing from `8.dp` to `4.dp`.
    - Reduce item height from `48.dp` to `40.dp`.
- **IntensitySelectionBlock**:
    - Reduce padding from `8.dp` to `6.dp`.
- **OnboardingStepHeader**:
    - Reduce padding and text sizes slightly if needed (will check if it's already compact enough).

## Verification Plan

### Manual Verification
1.  Navigate to the "Assisted Build" tab in the `FirstTimeSetupScreen`.
2.  Verify that the "Target Body Parts" categories are displayed in two columns.
3.  Verify that the entire UI feels more compact.
4.  Test expanding and collapsing body part categories.
5.  Test selecting/deselecting body parts and other options to ensure functionality is preserved.
