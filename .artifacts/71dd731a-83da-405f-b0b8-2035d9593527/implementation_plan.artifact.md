# Keep Reps Range Field Open on Error

The goal is to ensure that the `RepsRangeField` component stays in its expanded (open) state whenever it is in an error state, even if it is not focused and has no valid content. Currently, the field "closes" (displays only the label) if it lacks both focus and content, which can hide the error indicator or separator when invalid input like a lone hyphen (`-`) is present.

## Proposed Changes

### [UI Components]

#### [MODIFY] [WorkoutComponents.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/java/com/grafizum/repio/ui/components/WorkoutComponents.kt)

- Update `RepsRangeField` to include `isError` in the logic that determines if the field should be "open" (expanded).
- Specifically, update `OutlinedTextFieldDefaults.DecorationBox`, the separator `Text`, and the `Box` containing the max `BasicTextField` to depend on `(isAnyFocused || hasContent || isError)`.

## Verification Plan

### Manual Verification
1. Open any exercise edit/add dialog that uses `RepsRangeField` (e.g., in `RoutineScreen` or `FirstTimeSetupScreen`).
2. Type a single hyphen (`-`) in the reps field and then click away to clear focus.
3. Verify that the field remains "open" (showing the separator and both min/max placeholders/fields) and displays the error state (red border/label), rather than closing to a single-field view.
4. Verify that clearing the field entirely (empty string) and clearing focus still allows the field to "close" as expected when no error is present.
