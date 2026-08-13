# RepIo Multiplatform Foundation

Convert the existing native Android project into a **Kotlin Multiplatform (KMP)** project. This allows sharing logic and UI between Android and iOS while using GitHub Actions as a "Remote Mac" for iOS verification.

## User Review Required

> [!IMPORTANT]
> This transition involves renaming the main `:app` module to `:composeApp`. This is the standard naming convention for KMP projects. Your Android Studio run configurations may need to be re-selected after the change.

> [!CAUTION]
> We will be upgrading **Room to 2.7.0-alpha01** (or later) because KMP support for Room is only available in the newer alpha versions.

## Proposed Changes

### Build System & Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/guido/StudioProjects/RepIo/gradle/libs.versions.toml)
- Add Compose Multiplatform plugin and library versions.
- Add Kotlin Multiplatform plugin.
- Add KMP-compatible versions of Room, Coroutines, and DataStore.
- Add GitLive Firebase SDK (KMP alternative to native Firebase).

#### [MODIFY] [build.gradle.kts (root)](file:///C:/Users/guido/StudioProjects/RepIo/build.gradle.kts)
- Apply the Kotlin Multiplatform and Compose Multiplatform plugins.

#### [RENAME & MODIFY] [composeApp/build.gradle.kts](file:///C:/Users/guido/StudioProjects/RepIo/app/build.gradle.kts)
- Transform the module from `com.android.application` to `kotlin("multiplatform")`.
- Configure `androidTarget()`, `iosArm64()`, and `iosSimulatorArm64()`.
- Set up shared `commonMain` dependencies and platform-specific `androidMain`/`iosMain` sets.

---

### Project Structure

#### [MOVE] Source Files
- Move `app/src/main` to `composeApp/src/androidMain`.
- Create `composeApp/src/commonMain/kotlin`.
- Move `WorkoutViewModel`, `WorkoutRepository`, and `RoutineEntity` classes to `commonMain` to start sharing logic.
- Move UI screens (`HomeScreen`, `RoutineScreen`, etc.) to `commonMain` (Compose Multiplatform).

---

### Shared Infrastructure

#### [MODIFY] [WorkoutRepository.kt](file:///C:/Users/guido/StudioProjects/RepIo/app/src/main/java/com/grafizum/repio/data/WorkoutRepository.kt)
- Remove Android-specific `Application` or `Context` dependencies where possible.
- Use `expect/actual` pattern for platform-specific storage paths.

---

### Automation (The "Cloud Mac")

#### [NEW] [.github/workflows/ios-verify.yml](file:///C:/Users/guido/StudioProjects/RepIo/.github/workflows/ios-verify.yml)
- Create a workflow that runs on `macos-latest`.
- Sets up Java and Ruby (for CocoaPods/Xcode).
- Runs `./gradlew :composeApp:assembleDebugIosSimulatorArm64Framework` to verify that the iOS version compiles successfully on every push.

## Verification Plan

### Automated Tests
- Run `./gradlew :composeApp:connectedAndroidTest` to ensure Android functionality remains intact.
- Monitor GitHub Actions to confirm the iOS framework build passes.

### Manual Verification
- Run the app on a physical Android device from Android Studio to verify no regressions in the UI or data logging.
- Inspect the generated `iosApp` folder (to be added in a later step) to ensure the shared framework is being produced correctly.
