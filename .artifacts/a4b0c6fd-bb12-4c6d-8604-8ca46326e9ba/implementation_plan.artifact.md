# Knowledge Discovery Engine & Mental Model Integration

This plan implements the "Curiora Knowledge Discovery Engine" upgrade, refactoring the app's content structure to prioritize **Breadth over Mastery** and high-quality mental models as per the user's core product philosophy.

## Proposed Changes

### Data Layer refactor

#### [MODIFY] [TopicModel.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/model/TopicModel.kt)
- Redefine `Topic` model to match the new high-quality schema: `hook`, `big_picture`, `main_components`, `how_it_works`, `memorize_mode`, and `logic_mode`.
- Remove legacy `TopicSection` and `QuizQuestion` (integrated into the new modes).

#### [MODIFY] [TopicEntity.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/local/TopicEntity.kt)
- Update Room entity to store the new content structure as serialized JSON fields or individual columns.

### UI & UX Logic refactor

#### [MODIFY] [TopicExplanationHelper.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/ui/util/TopicExplanationHelper.kt)
- Remove hardcoded logic for specific IDs.
- Implement generic extraction from the model's new `memorizeMode` and `logicMode` fields.

#### [MODIFY] [TopicDetailSheet.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/ui/components/TopicDetailSheet.kt)
- Update the layout to render the "Hook" and "Big Picture" prominently.
- Add components for "Main Components" and the "How It Works" sequence.
- Refactor the "Memorize" vs "Logic" mode views to follow the new pedagogy (problem-solving, predictions, memory hooks).

### Knowledge Pool Expansion

#### [MODIFY] [TopicDataSeed.kt](file:///C:/Users/guido/StudioProjects/Curiora/app/src/main/java/com/grafizum/curiora/data/local/TopicDataSeed.kt)
- Replace legacy seed topics with a comprehensive batch of 15+ topics following the new philosophy:
    - How does a car work?
    - How does the internet work?
    - How do banks make money?
    - Why does ice float?
    - How does GPS know where you are?
    - How does an elevator work?
    - Why does the sky change color?
    - How do phishing scams work?
    - (and more from the user's provided list)

## Verification Plan

### Automated Verification
- Verify JSON serialization/deserialization for the new `Topic` model.
- Run database migrations or fresh install to verify Room schema update.

### Manual Verification
1.  **Big Picture Test**: Open "How a car works". Verify the "Big Picture" explains the system before any details appear.
2.  **Mode Differentiation**:
    - Toggle **Memorize Mode**: Check for direct facts, memory hooks, and a quick recall question.
    - Toggle **Logic Mode**: Check for a starting challenge/question, reasoning steps, and a reveal.
3.  **Instructional Style**: Verify that paragraphs are short, analogies are used, and jargon is explained immediately.
4.  **Content Breadth**: Browse the "Home" tab to verify the new batch of high-quality topics is available.
