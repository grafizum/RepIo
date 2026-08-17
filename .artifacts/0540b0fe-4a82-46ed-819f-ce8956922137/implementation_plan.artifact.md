# UI Modifications for Premade Build Screen

This plan outlines the changes to remove the "Training Days Per Week" header and the step number "2" from "Exercise Preferences (Optional)" only in the "Premade Build" tab of the `FirstTimeSetupScreen`.

## Proposed Changes

### [Component Name] UI Components

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)

1.  **Modify `OnboardingStepHeader`**:
    *   Update the `stepNumber` parameter to be optional (`Int? = null`).
    *   Conditionally render the step number circle and its following spacer only if `stepNumber` is not null.

2.  **Update "Premade Build" tab content**:
    *   Remove the `OnboardingStepHeader` call for the "Training Days Per Week" section.
    *   Update the `OnboardingStepHeader` call for the "Exercise Preferences (Optional)" section to pass `stepNumber = null`.

## Verification Plan

### Manual Verification
*   Deploy the app and navigate to the `FirstTimeSetupScreen`.
*   Select the **Premade Build** tab.
*   Verify that the "Training Days Per Week" title label is gone.
*   Verify that the "Exercise Preferences (Optional)" header exists but has no number "2" beside it.
*   Switch to the **Assisted Build** tab and verify that its headers still have step numbers (1, 2, 3, etc.) and that "Training Days Per Week" (step 3) is still visible there.
*   Switch to the **Custom Build** tab and verify that its header still has step number 1.
