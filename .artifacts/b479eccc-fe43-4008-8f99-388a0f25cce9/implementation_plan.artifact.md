# Implementation Plan - Themed App Icons

Update the app's launcher icons and logos to match the new design (dumbbell + pulse line) provided in the image. The new design will be implemented as a `VectorDrawable` to support theming and maintain a "less realistic" (flat) texture while preserving the long shadow and colors.

## User Review Required

> [!IMPORTANT]
> The new icon will include a **long shadow** (45 degrees) as seen in the provided image. This shadow will be implemented as a semi-transparent black layer to work across all themed backgrounds.

> [!NOTE]
> All existing color variants (Azure, Beige, Crimson, Emerald, Flame, Ocean, Orange, Purple, Retro, Rose) will be updated to use the new shape, each with its respective primary theme color.

## Proposed Changes

### Icon Design

The new icon will consist of:
1.  **Dumbbell**: A horizontal bar with two weighted blocks on each side (inner taller, outer shorter).
2.  **Pulse Line**: A sharp zigzag line below the dumbbell.
3.  **Long Shadow**: A 45-degree projection of the dumbbell and pulse line.

---

### [Component] Android Resources (Icons)

#### [MODIFY] [ic_launcher_foreground.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/drawable/ic_launcher_foreground.xml)
Update the base foreground with the new paths and shadow.

#### [MODIFY] all themed foregrounds (Azure, Beige, etc.)
- [ic_launcher_foreground_azure.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/drawable/ic_launcher_foreground_azure.xml)
- [ic_launcher_foreground_beige.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/drawable/ic_launcher_foreground_beige.xml)
- [ic_launcher_foreground_crimson.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/drawable/ic_launcher_foreground_crimson.xml)
- ... (and all other variants)

#### [MODIFY] [ic_main_logo_foreground.xml](file:///C:/Users/guido/StudioProjects/RepIo/composeApp/src/main/res/drawable/ic_main_logo_foreground.xml) and its variants
Ensure the in-app logos match the new design.

---

## Verification Plan

### Automated Verification
- Run `gradlew assembleDebug` to ensure all XML files are valid and the project builds.

### Manual Verification
- Deploy the app and toggle between themes to verify that the icon color updates correctly and the shape matches the new design.
- Inspect the icon in the Android Studio Layout Inspector or Preview (if available) to verify the "less realistic" texture and shadow.
