# Add Input Validation Feedback to Sets Field

The goal is to show a themed error message (toast) when the user attempts to type non-numeric characters in the "Sets" field of the exercise editor, similar to the existing behavior in the "Reps" field.

## User Review Required

> [!NOTE]
> - A new error message "Only numbers are allowed" will be shown for the Sets field.
> - The existing "Only numbers and dashes are allowed" will remain for the Reps field.

## Proposed Changes

### Resources

#### [MODIFY] [strings.xml](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/values/strings.xml)
- Add `<string name="only_numbers_error">Only numbers are allowed</string>`.

#### [MODIFY] [strings.xml (de)](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/values-de/strings.xml)
- Add `<string name="only_numbers_error">Nur Zahlen sind erlaubt</string>`.

#### [MODIFY] [strings.xml (it)](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/values-it/strings.xml)
- Add `<string name="only_numbers_error">Sono ammessi solo numeri</string>`.

### UI Components

#### [MODIFY] [RoutineScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/ui/screens/RoutineScreen.kt)
- Update `ExerciseEditDialog`:
    - Replace `showInvalidCharToast` (Boolean) with `activeToastMessage` (String?) to handle different messages for different fields.
    - Update `targetSetsStr`'s `onValueChange` to detect non-digit input and set the "Only numbers are allowed" message.
    - Update `targetRepsValue`'s `onValueChange` to set the "Only numbers and dashes are allowed" message.
    - Update `ThemedToast` to use the dynamic message.

## Verification Plan

### Automated Tests
- Build the app to ensure no compilation errors.

### Manual Verification
1. Open the "Add Exercise" dialog.
2. Try to type a letter in the "Sets" field.
    - Verify that a popup appears saying "Only numbers are allowed".
3. Try to type a letter in the "Reps" field.
    - Verify that a popup appears saying "Only numbers and dashes are allowed".
