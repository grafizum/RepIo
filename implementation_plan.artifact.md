# Branding and Loading Screen Update

This plan updates the app's default launcher icon to a "light" version with an orange border and configures a dark-mode splash screen using an orange icon asset.

## User Review Required

> [!IMPORTANT]
> - The "light" icon (currently beige) will be set as the default launcher icon.
> - An orange border will be added to the light icon's background.
> - A dark-mode specific splash screen will be created that uses the orange icon asset, replacing the current "green" (neon lime) default.

## Proposed Changes

### Icon Assets

#### [NEW] [ic_launcher_background_light.xml](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/drawable/ic_launcher_background_light.xml)
- Create a new vector drawable for the light icon background with a `#FDFCFB` fill and a `#FF6D00` (orange) stroke to serve as the border.

#### [MODIFY] [ic_launcher_beige.xml](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/mipmap-anydpi-v26/ic_launcher_beige.xml)
- Update to use `@drawable/ic_launcher_background_light` instead of the solid color.

### Manifest and Defaults

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/AndroidManifest.xml)
- Set `com.example.MainActivityBeige` to `enabled="true"`.
- Set `com.example.MainActivityOrange` to `enabled="false"`.

### Splash Screen Themes

#### [NEW] [themes.xml (night)](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/res/values-night/themes.xml)
- Create a dark-mode theme resource.
- Override `Theme.App.Starting.Beige` (and `Theme.App.Starting` for safety) to use:
    - `windowSplashScreenAnimatedIcon`: `@drawable/ic_launcher_foreground_orange`
    - `windowSplashScreenBackground`: `#121212` (or `@color/ic_launcher_background`)

## Verification Plan

### Automated Tests
- Build the project: `gradlew assembleDebug`

### Manual Verification
- Deploy to device/emulator.
- Check the app icon: It should be the light (beige) dumbbell with an orange border.
- Open the app in **Light Mode**: Should show the beige splash screen.
- Open the app in **Dark Mode**: Should show a dark splash screen with the **orange** dumbbell icon.
