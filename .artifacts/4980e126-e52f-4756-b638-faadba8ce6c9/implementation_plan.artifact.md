# Implementation Plan - Fix Transition from Welcome Screen to Edit Tab

The goal is to ensure a clean transition when closing the "Welcome Screen" (First Time Setup) after triggering it from the "Edit" tab (via the "New" button). Currently, the app might visibly swap from the "Workout Now" sub-tab to the "Edit" sub-tab, which the user wants to avoid.

## User Review Required

> [!NOTE]
> The fix involves ensuring that the sub-tab state is correctly initialized and respected when the main app UI recomposes after the setup screen is dismissed.

## Proposed Changes

### [Workout ViewModel]

#### [MODIFY] [WorkoutViewModel.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/viewmodel/WorkoutViewModel.kt)
- Update `completeFirstTimeSetup` and `applyWorkoutTemplate` to set the requested sub-tab to 1 (Edit) if the setup was triggered manually (i.e., `isManualReentry` is true).

---

### [UI Screens]

#### [MODIFY] [MyWorkoutTabScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/MyWorkoutTabScreen.kt)
- Change the default value of `initialTab` in `WorkoutTabScreen` to use the current value of `viewModel.requestedWorkoutSubTab.value.tab`. This ensures that when the component is created, it starts on the correct sub-tab immediately without an entry animation from the default (0).

## Verification Plan

### Manual Verification
1. Navigate to the **Edit** tab.
2. Tap the **New** button in the header to open the "Welcome Screen".
3. Either complete the setup or tap the back/cancel button.
4. Verify that the app transitions directly back to the **Edit** tab without showing the "Workout Now" tab or animating a switch between them.
