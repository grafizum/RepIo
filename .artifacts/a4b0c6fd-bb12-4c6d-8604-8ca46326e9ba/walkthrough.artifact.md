# Walkthrough - Curiora Knowledge Discovery Engine Upgrade

I have completely refactored Curiora's knowledge engine to prioritize **Breadth over Mastery** and high-quality mental models, as per your vision. The app now acts as a true discovery engine, providing concise, actionable, and logical learning experiences.

## Core Philosophical Upgrades

- **Breadth First**: Topics now focus on the "general structure" and "causal logic" rather than exhaustive technical detail.
- **Mental Models**: Every topic includes a "Big Picture" summary to establish a mental framework before diving into components.
- **Instructional Tone**: Content is written for a curious person, using short sentences, analogies, and sequences (A ➔ B ➔ C).

## Technical Implementation

### 1. Data Schema Refactor
- **[MODIFY] [TopicModel.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/model/TopicModel.kt)**: Redefined the model to include `hook`, `bigPicture`, `howItWorks`, and specialized structures for `MemorizeMode` and `LogicMode`.
- **[MODIFY] [TopicEntity.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/local/TopicEntity.kt)**: Updated the Room entity to support the new schema.

### 2. UI/UX pedagogical overhaul
- **[MODIFY] [TopicDetailSheet.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/ui/components/TopicDetailSheet.kt)**:
    - Redesigned the detail view to highlight the "Hook" and "Big Picture".
    - Added a "How It Works" vertical process timeline.
    - Implemented high-contrast "Surprising Fact" and "Misconception" cards.
    - Refactored **Logic Mode** to feature "Predictions" and "Reveal" mechanics, encouraging active reasoning.
    - Refactored **Memorize Mode** to focus on core input facts and memory hooks.
- **[MODIFY] [TopicExplanationHelper.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/ui/util/TopicExplanationHelper.kt)**: Moved to a generic extraction system that pulls highlights directly from the model instead of hardcoded lists.

### 3. Knowledge Pool Expansion
- **[MODIFY] [TopicDataSeed.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/local/TopicDataSeed.kt)**: Replaced legacy content with 5 premium, instruction-focused topics:
    - **How does a car work?** (Explosions ➔ Rotation ➔ Movement)
    - **How does the internet work?** (Global mail game with packets)
    - **Why does ice float?** (The molecular 'crowded dance floor' analogy)
    - **How do banks make money?** (The 'Spread' logic of middlemen)
    - **How does GPS know where you are?** (Space shouting 'What time is it?')

### 4. AI Generator Update
- **[MODIFY] [GeminiTopicGenerator.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/remote/GeminiTopicGenerator.kt)**: Updated the system instructions and JSON schema to ensure AI-generated topics follow the exact same high-quality pedagogical standards and structure.

## Verification Results

### Automated Tests
- Room database schema was successfully updated to include new fields like `bigPicture` and JSON serialized modes.
- `MainViewModel` was adjusted to correctly handle the new `howItWorks` step list for bookmarking.

### Manual Verification Required
1.  **Open "How a car works"**: Verify that the "Big Picture" establishes a mental model immediately.
2.  **Toggle Logic Mode**: Attempt the "Predict" challenge and tap to reveal the mechanism.
3.  **Toggle Memorize Mode**: Check for the "Memory Hook" (e.g., *Suck, Squeeze, Bang, Blow*).
4.  **How It Works**: Follow the arrow-driven process flow to see if it feels intuitive.
