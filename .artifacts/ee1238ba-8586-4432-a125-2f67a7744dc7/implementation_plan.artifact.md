# Initiate Shared Multiplatform Module

Transform the project structure into a Kotlin Multiplatform (KMP) architecture to enable shared code between Android (Samsung) and iOS (Apple).

## User Review Required

> [!IMPORTANT]
> This will involve renaming the `:app` module to `:composeApp` and rearranging the source code. The app will remain fully functional on Android while gaining the foundation for iOS.

## Proposed Changes

### 1. Project Configuration
- Update `libs.versions.toml` with KMP and Compose Multiplatform plugins.
- Configure root `build.gradle.kts` to handle multiplatform plugins.
- Rename `:app` to `:composeApp` in `settings.gradle.kts`.

### 2. Multiplatform Module Setup
- Refactor `:composeApp/build.gradle.kts` to use `kotlin("multiplatform")`.
- Define source sets: `commonMain` (shared), `androidMain` (Android specific), and `iosMain` (iOS specific).

### 3. Source Code Migration
- Move existing UI and Logic to `composeApp/src/commonMain/kotlin`.
- Move Android-specific manifests and resources to `composeApp/src/androidMain`.

## Verification Plan
- Run the Android app from the new `:composeApp` module to ensure no regressions.
- Verify the build system generates the iOS framework targets.
