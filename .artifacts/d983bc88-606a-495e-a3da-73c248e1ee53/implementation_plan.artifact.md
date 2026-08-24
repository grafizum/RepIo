# Implementation Plan - UI Improvements for Custom Build Section

Move the select and "+" buttons to the exercises count row and enhance the day title in the Custom Build section of the welcome screen.

## User Review Required

> [!NOTE]
> The "Day of the Week" title will be increased to `headlineLarge` and centered as requested.
> The "Select All" functionality will now share a row with the "Delete/Cancel" buttons when in selection mode, and the "Exercises" count will share a row with the "Select/Add" buttons in normal mode.

## Proposed Changes

### UI Screens

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)

- Update the Custom Build tab content (`selectedTab == 2` block):
    - Change the day title layout from a `Row` to a centered `Text` and increase size to `headlineLarge`.
    - Move the `IconButton` row (Checklist and Add buttons) from the day title section down to the exercises count section.
    - Consolidate the selection mode controls (Delete/Close) into the same row as the exercise count/selection controls.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to the "Custom Build" tab in the welcome screen.
- Verify that the Day of the Week title (e.g., "Monday") is large and centered.
- Verify that the "Select" (Checklist) and "+" (Add) buttons are now on the same row as the "Exercises (N)" text.
- Enter selection mode and verify that the "Deselect All" button and the "Delete/Cancel" buttons are correctly positioned on the same row.
