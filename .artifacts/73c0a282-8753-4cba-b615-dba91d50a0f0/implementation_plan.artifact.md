# Easter Egg Implementation Plan

Add a hidden "Creator" tab that is unlocked via a specific gesture sequence in the Settings/Analytics tabs.

## User Review Required

> [!NOTE]
> The easter egg is triggered by swiping between Settings and Analytics in the sequence: Settings -> Analytics -> Settings -> Analytics -> Settings, followed by tapping the Settings icon in the bottom navigation bar.

## Proposed Changes

### [composeApp]

#### [MODIFY] [App.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/androidMain/java/com/grafizum/repio/App.kt)
- Add `EasterEgg` to the `Screen` sealed class.
- Implement navigation history tracking in `MainAppStructure`.
- Add conditional logic to `navigationItems` to show the 5th tab when unlocked.
- Add the trigger check in the `NavigationBarItem` click handler for the Settings tab.
- Add the `EasterEggScreen` to the `HorizontalPager` content.

#### [NEW] [EasterEggScreen.kt](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/commonMain/kotlin/com/grafizum/repio/ui/screens/EasterEggScreen.kt)
- Create a simple screen displaying the creator's name ("Guido") with some festive UI.

## Verification Plan

### Manual Verification
1. Navigate to the **Settings** tab.
2. Swipe left to **Analytics**.
3. Swipe right back to **Settings**.
4. Swipe left to **Analytics**.
5. Swipe right back to **Settings**.
6. Tap the **Settings icon** in the bottom navigation bar.
7. Verify that a 5th tab ("Creator") appears.
8. Click the "Creator" tab and verify the content.
