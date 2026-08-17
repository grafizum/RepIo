# Remake Equipment Selection UI

Refactor the equipment selection in `FirstTimeSetupScreen.kt` to use a switch-like behavior for "Full Gym" and separate toggles for specific equipment.

## Proposed Changes

### [composeApp]

#### [MODIFY] [FirstTimeSetupScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/screens/FirstTimeSetupScreen.kt)

- Extract the "Full Gym" option into a separate UI element (likely a `Row` with a `Switch`).
- Display the remaining equipment options ("Bodyweight", "Barbell", "Dumbbell") as a grid of buttons below the "Full Gym" toggle.
- Update the selection logic:
    - Toggling "Full Gym" ON clears all specific equipment and selects "Gym".
    - Toggling "Full Gym" OFF (if it was the only selection) selects a default specific equipment (e.g., "Bodyweight") to avoid an empty state.
    - Selecting any specific equipment while "Full Gym" is ON will toggle "Full Gym" OFF and select the specific equipment.
    - Deselecting the last specific equipment could either toggle "Full Gym" ON or prevent deselection.

## Verification Plan

### Manual Verification
- Deploy the app to a device/emulator.
- Navigate to the "Assisted Build" tab in the setup screen.
- Verify that "Full Gym" acts as a master switch.
- Verify that adding specific equipment turns OFF "Full Gym".
- Verify that turning ON "Full Gym" clears specific equipment.
- Verify the visual "on/off switch" look.
