# Skill Observation Log

Observations captured during task-oriented work.

**Status key:** OPEN = not yet actioned | ACTIONED (YYYY-MM-DD) = skill updated/created | DECLINED (YYYY-MM-DD) = user decided not to pursue — resolved statuses always carry their resolution date

---

## 2026-08-11

### Observation 1: Mixed language strings in default resources
**Status:** OPEN
**Date:** 2026-08-11
**Session context:** Removing Midnight Blue and Forest Green themes.
**Skill:** New skill candidate: Resource Cleanup
**Type:** open-source
**Phase/Area:** string management

**Issue:** During the cleanup of `strings.xml`, I noticed that some string values in the default `res/values/strings.xml` (which should be English) were actually in German (e.g., `full_body_b_desc`). This suggests a breakdown in translation workflow or manual copy-paste errors.

**Suggested improvement:** When performing resource cleanup or updates, always cross-reference the default resource file against translation files to ensure language consistency. Implement a check for "foreign" languages in the default resource file.

**Principle:** Default resource files (e.g., `strings.xml`) must be kept in the primary project language (usually English) to avoid confusing fallback behavior for other locales.

---

## 2026-08-12

### Observation 2: Ghost 0B files during package migration
**Status:** OPEN
**Date:** 2026-08-12
**Session context:** Fixing unresolved reference after package migration.
**Skill:** New skill candidate: Package Migration
**Type:** open-source
**Phase/Area:** file synchronization

**Issue:** During a complex package migration, files may be created in the new directory structure as 0B placeholders while the actual content remains in the IDE memory buffer. CLI-based build tools (like Gradle) read the 0B disk state and fail, leading to "unresolved reference" errors even if the code looks correct in the editor.

**Suggested improvement:** When an "unresolved reference" occurs immediately after a file move or package migration, verify the disk size of the target file. If it is 0B, use a write tool to flush the memory buffer to disk.

**Principle:** External build tools are disk-bound; ensure IDE memory state is synchronized to disk before running CLI builds, especially after structural changes.

### Observation 3: Deprecation of LocalLifecycleOwner in UI Platform
**Status:** OPEN
**Date:** 2026-08-12
**Session context:** Fixing build warnings.
**Skill:** New skill candidate: Compose Maintenance
**Type:** open-source
**Phase/Area:** dependency management

**Issue:** `androidx.compose.ui.platform.LocalLifecycleOwner` is now deprecated in favor of `androidx.lifecycle.compose.LocalLifecycleOwner`. Using the deprecated version can lead to subtle bugs and cluttered build logs.

**Suggested improvement:** Proactively migrate `LocalLifecycleOwner` to the `androidx.lifecycle.compose` package.

**Principle:** Keep core Compose utility references updated to their respective domain-specific packages (e.g., moving lifecycle utilities to the lifecycle-compose library).

### Observation 4: Contextual Permission Requests
**Status:** OPEN
**Date:** 2026-08-12
**Session context:** Moving notification permission request.
**Skill:** New skill candidate: Onboarding UX
**Type:** open-source
**Phase/Area:** permissions

**Issue:** Requesting system permissions (like `POST_NOTIFICATIONS`) in `MainActivity.onCreate` causes the dialog to pop up immediately on every launch if not granted, which can be intrusive and lacks context.

**Suggested improvement:** Move permission requests to onboarding screens or specific user actions where the need for the permission can be explained.

**Principle:** Permissions should be requested contextually or during an explained onboarding flow to improve user acceptance and reduce friction.

### Observation 5: Custom Release Artifact Naming
**Status:** OPEN
**Date:** 2026-08-12
**Session context:** Customizing .aab filename format.
**Skill:** New skill candidate: Build Automation
**Type:** open-source
**Phase/Area:** build scripts

**Issue:** Default Android build outputs often include redundant or unclear suffixes (like `-release` or `-unsigned`) in the filename, which may not match a project's release management standards.

**Suggested improvement:** Use the `base { archivesName.set(...) }` block in `build.gradle.kts` to enforce a strictly defined naming convention (e.g., `[AppName].[VersionName].v[VersionCode].aab`). Centralize these values in local variables to ensure they are synchronized between the filename and the actual `versionName`/`versionCode` declarations.

**Principle:** Consistent artifact naming improves release traceability and reduces manual renaming overhead.

---

## 2026-08-13

### Observation 6: Contextual "Replace" Logic for Structured Data
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Adding "Replace" exercise button.
**Skill:** New skill candidate: Smart UI Components
**Type:** open-source
**Phase/Area:** user experience

**Issue:** Users often want to modify a single item in a structured list (like a workout routine) without deleting and re-adding or searching through a global list. Providing alternatives that match the "category" or "property" of the current item reduces cognitive load.

**Suggested improvement:** For list items with categorical properties (e.g., muscle groups), implement a "Replace" action that pre-filters the database/pool for items matching that specific category.

**Principle:** Contextual filtering in editing screens provides a faster and more intuitive customization experience than generic "Edit" or "Add" actions.

### Observation 7: Consistency in Searchable Form Fields
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Making muscle group search consistent with exercise search.
**Skill:** New skill candidate: Design Systems
**Type:** open-source
**Phase/Area:** ui consistency

**Issue:** Inconsistent input methods for similar data types (e.g., a searchable field for exercises but a static dropdown for muscle groups) can confuse users and break their mental model of how the app works.

**Suggested improvement:** Standardize "search-then-select" patterns across all categorical input fields. Provide descriptive placeholders (e.g., "eg. Chest, Back...") to clearly indicate the expected input format and range.

**Principle:** Uniformity in input patterns reduces the user's learning curve and makes complex forms feel more predictable and easier to navigate.

### Observation 8: Removing Visual Friction (Ripples) in Precision Editors
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Removing press animations (grey boxes) from editor.
**Skill:** New skill candidate: UI Aesthetics
**Type:** open-source
**Phase/Area:** visual polish

**Issue:** Default Material ripples (often appearing as grey boxes) can feel "heavy" or distracting in list-based editors where the user is making frequent, precise taps. This is especially true when ripples cover large areas like entire cards.

**Suggested improvement:** For specialized editor windows, consider disabling the default ripple (`indication = null`) on `clickable` elements to provide a more static, "application-like" feel. Use state-based color changes or borders instead of transient animations if feedback is still needed.

**Principle:** In data-heavy or editing-focused UIs, reducing transient animations like ripples can make the interface feel faster and more stable, minimizing visual noise during repetitive tasks.

### Observation 9: Hierarchical Categorization for List Items
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Refactoring muscle group labels to topics.
**Skill:** New skill candidate: Data Modeling
**Type:** open-source
**Phase/Area:** data representation

**Issue:** Detailed data (like specific muscles) is useful for information but can make a list UI feel cluttered and inconsistent when used as primary labels. 

**Suggested improvement:** Implement a "Topic vs. Detail" hierarchy. Use broad, stable categories ("Topics" like Arms, Legs) for persistent UI elements like colored badges and filtering logic. Reserve specific details (Biceps, Triceps) for expanded views or secondary info fields.

**Principle:** Use high-level categories for UI labels to maintain visual harmony, while predicting or storing detailed sub-categories to provide deeper information only when the user explicitly requests more detail (e.g., by expanding a card).

### Observation 10: Component Specialization via Configuration Flags
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Adding exercise descriptions only to the home screen schedule.
**Skill:** New skill candidate: UI Pattern Design
**Type:** open-source
**Phase/Area:** reusability

**Issue:** A generic list component (e.g., an exercise card) may need to show different levels of detail depending on the screen context (summary vs. editor). Hard-coding separate components for each view leads to maintenance duplication.

**Suggested improvement:** Use specialized configuration flags (e.g., `showDescription`) in shared components. This allows the same base UI logic to be reused across the app while adapting its content density to the specific needs of the current screen.

**Principle:** Reusable components should be "pluggable" with visibility flags to support different UI densities without breaking the shared design system.

### Observation 11: Relevance-Based Auto-complete Sorting
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Improving search results ordering.
**Skill:** New skill candidate: Search UX
**Type:** open-source
**Phase/Area:** input fields

**Issue:** Standard "contains" filtering for auto-complete often buries the most relevant results (like exact matches or "starts-with" matches) under a large list of items that simply contain the search string.

**Suggested improvement:** Implement a multi-tier sorting strategy for search previews:
1. Exact matches (case-insensitive) at the very top.
2. Items starting with the input string.
3. Items containing the input string.
4. Alphabetical sorting within each tier.

**Principle:** User intent in auto-complete fields is typically to find the most direct match quickly. Prioritizing matches by "closeness" to the start of the string significantly reduces the time spent scanning a results list.

### Observation 12: Vertical Information Hierarchy in Headers
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Updating day header in Routine screen.
**Skill:** New skill candidate: UI Pattern Design
**Type:** open-source
**Phase/Area:** visual hierarchy

**Issue:** Combining primary labels (e.g., Day Name) and secondary metadata (e.g., Focus/Muscle Groups) in a single line with a delimiter (e.g., "Monday — Chest") can become hard to read as the metadata length increases.

**Suggested improvement:** Split the information vertically using a Column. Maintain a strong typography for the primary label and use a smaller, less contrasting style for the metadata subtitle.

**Principle:** Vertical stacking of related information with distinct typographic weights creates a stronger visual hierarchy and better accommodates varying content lengths compared to single-line delimited formats.

### Observation 13: Progressive Disclosure in Data Entry Forms
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Refining "Add Exercise" dialog UX.
**Skill:** New skill candidate: Smart UI Components
**Type:** open-source
**Phase/Area:** user experience

**Issue:** Showing all data fields (including auto-filled metadata like "Primary Focus" or "Description") in a blank form can create unnecessary visual noise before the user has provided the primary input.

**Suggested improvement:** Use "Progressive Disclosure". Keep secondary metadata fields (like muscle group badges or exercise descriptions) hidden until the primary trigger (e.g., selecting an exercise name) is activated. 

**Principle:** Hiding dependent information until it is contextually relevant reduces cognitive load and makes the initial entry point of a form feel lighter and more approachable.

### Observation 14: Structural Redundancy in Platform-Specific Files
**Status:** OPEN
**Date:** 2026-08-13
**Session context:** Implementing Easter Egg in App.kt vs MainActivity.kt.
**Skill:** New skill candidate: Code Organization
**Type:** open-source
**Phase/Area:** architecture

**Issue:** Duplicate declarations of core navigation components (`Screen`, `MainAppStructure`) across multiple platform files (e.g., `App.kt` and `MainActivity.kt` in `androidMain`) can cause redeclaration errors and build failures.

**Suggested improvement:** Centralize shared UI components into a single source of truth. Use `App.kt` as the primary entry point for the UI and have `MainActivity.kt` delegate to it, avoiding local duplication of the same sealed classes and composables.

**Principle:** Shared structural definitions (navigation items, screen state) should be defined once to prevent sync issues and build-time collisions.

---

## 2026-08-17

### Observation 15: Structural dependency tracking in surgical edits
**Status:** OPEN
**Date:** 2026-08-17
**Session context:** Fixing unresolved `majorMuscles` reference.
**Skill:** New skill candidate: Surgical Refactoring
**Type:** open-source
**Phase/Area:** code modification

**Issue:** When removing a local variable or constant from a function during a surgical edit, there is a risk of leaving "ghost" references in other parts of the same file (e.g., in a `sortedBy` block later in the function). The IDE's error reporting is excellent, but catching this before the tool call prevents build failures.

**Suggested improvement:** Before deleting a declaration, always perform a project-wide (or at least file-wide) `grep` for the symbol to identify dependent blocks.

**Principle:** Deleting a symbol is a structural change; always map the dependency graph of a symbol before removing its definition to maintain build integrity.

---

## 2026-08-20

### Observation 16: Unified Reactive Equipment State
**Status:** OPEN
**Date:** 2026-08-20
**Session context:** Fixing equipment filtering in RepIo.
**Skill:** task-observer
**Type:** open-source
**Phase/Area:** State Management / UX Consistency

**Issue:** Equipment selection was fragmented. Selecting "Barbell" in one tab didn't filter exercises in other tabs or dialogs, leading to machine exercises appearing when not desired. This was because the state was local to specific UI branches and defaults ("Gym") were used elsewhere.

**Suggested improvement:** Hoist critical configuration state (like equipment or goals) to the ViewModel and ensure all UI layers (search, generation, alternatives) react to the same source of truth. Synchronize this state with persistent memory (prefs) in real-time to ensure consistency across app restarts and tab switches.

**Principle:** Shared UI constraints must be anchored to a single, global source of truth. Decoupling local tab state from global behavioral constraints prevents "leaks" where inactive configurations (like "Full Gym" defaults) override active user intent.

---

## 2026-08-24

### Observation 17: Modernizing Visual Feedback with IndicationNodeFactory
**Status:** OPEN
**Date:** 2026-08-24
**Session context:** Customizing button feedback to remove grey ripple.
**Skill:** task-observer
**Type:** open-source
**Phase/Area:** Compose UI / Theming

**Issue:** Older Compose APIs like `IndicationInstance` are deprecated in recent versions (e.g., BOM 2024.10.01). Using them results in compiler warnings and potentially sub-optimal performance. The modern replacement is `IndicationNodeFactory`, which leverages the `Modifier.Node` architecture.

**Suggested improvement:** Proactively use `IndicationNodeFactory` for custom visual feedback. Ensure `equals` and `hashCode` are implemented to satisfy interface requirements and allow for proper caching in `remember`.

**Principle:** When overriding global UI behaviors (like ripples), prefer the latest architectural patterns (like `Modifier.Node`) to ensure future-proofing and better performance characteristics.
