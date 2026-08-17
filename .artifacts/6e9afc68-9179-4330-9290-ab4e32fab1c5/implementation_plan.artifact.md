# Adjust Title Padding for Better Balance

The goal is to increase the top padding of section titles to match their bottom padding, ensuring a more balanced visual layout. Based on the UI inspection and code analysis, the `OnboardingStepHeader` in `FirstTimeSetupScreen.kt` is the primary candidate, as it currently has 12dp bottom padding but 0dp top padding, leading to an unbalanced appearance in several places (e.g., Steps 4 and 5 in the Assisted Build flow).

## Proposed Changes

### UI Components (FirstTimeSetupScreen)

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)

- Update `OnboardingStepHeader` composable to include `top = 12.dp` padding in its root `Row` modifier, matching the existing `bottom = 12.dp`.
- This will ensure that when headers follow content (like in the Assisted Build flow), they have consistent spacing above and below.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to the "First Time Setup" screen.
- Go through the "Assisted Build" flow.
- Verify that the headers (e.g., "Choose Your Equipment", "Target Body Parts", "Choose Days") have balanced spacing above and below them.
- Check Step 1's title to ensure the added padding doesn't create excessive space when combined with the `LazyColumn`'s `spacedBy`.
