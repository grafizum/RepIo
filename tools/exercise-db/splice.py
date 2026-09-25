#!/usr/bin/env python3
"""One-shot surgery: rewire WorkoutTemplates.kt + DefaultData.kt onto the
generated ExerciseCatalog. Run once from the repo root:
    python tools/exercise-db/splice.py
Markers disappear after the splice, so re-running is a no-op by design.
"""

from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
TEMPLATES = ROOT / "composeApp/src/main/java/com/grafizum/repio/data/WorkoutTemplates.kt"
DEFAULTS = ROOT / "composeApp/src/main/java/com/grafizum/repio/data/DefaultData.kt"

# Legacy pool name -> rebuilt catalog name (used by the built-in templates).
RENAME = {
    "Barbell Rows": "Bent Over Barbell Row",
    "Dumbbell Rows": "Bent-Over Dumbbell Row",
    "Dumbbell Pullovers": "Dumbbell Pull-Over",
    "Dumbbell Curls": "Dumbbell Curl",
    "Dumbbell Hammer Curls": "Dumbbell Hammer Curl",
    "Dumbbell Shoulder Presses": "Dumbbell Shoulder Press",
    "Dumbbell Lateral Raises": "Dumbbell Lateral Raise",
    "Barbell Bench Presses": "Barbell Bench Press",
    "Barbell Curls": "Barbell Curl",
    "Leg Raises": "Hanging Leg Raise",
    "Barbell Squats": "Barbell Squat",
    "Barbell Romanian Deadlifts": "Romanian Deadlift",
    "Planks": "Plank",
    "Dumbbell Overhead Extensions": "Standing Dumbbell Triceps Extension",
    "Barbell Floor Presses": "Barbell Bench Press",
}


def sub_once(src: str, pattern: str, replacement: str, label: str) -> str:
    new, n = re.subn(pattern, replacement, src, count=1, flags=re.S)
    if n != 1:
        raise SystemExit(f"SPLICE FAILED: marker not found: {label}")
    print(f"  spliced: {label}")
    return new


def rename_names(src: str) -> str:
    count = 0
    for old, new in RENAME.items():
        pat = f'"{re.escape(old)}"'
        n = len(re.findall(pat, src))
        src = src.replace(pat, f'"{new}"')
        count += n
    print(f"  renamed {count} legacy pool-name literals")
    return src


def main() -> None:
    src = TEMPLATES.read_text(encoding="utf-8")

    # --- 1. primary-muscle table -> catalog delegate -----------------------
    src = sub_once(
        src,
        r'/\*\*\n     \* Authoritative primary muscle for every exercise in the built-in pool\..*?to muscle \} \}\.toMap\(\)',
        '/**\n     * Authoritative primary muscle for every exercise in the built-in pool.\n     *\n     * Generated from the exercise-database CSV (see tools/exercise-db/); the\n     * precise muscle is the join key the badges, the analytics split and the\n     * broad body-part filter all read. A test asserts every pool exercise has\n     * exactly one entry and no entry names an exercise outside the pool.\n     */\n    private val exercisePrimaryMuscles: Map<String, String> =\n        ExerciseCatalog.ENTRIES.associate { it.name.lowercase() to it.muscle }',
        "primary muscle table -> ExerciseCatalog",
    )

    # --- 2. equipment table -> catalog delegate ----------------------------
    src = sub_once(
        src,
        r'/\*\*\n     \* Authoritative equipment for every exercise in the built-in pool\..*?to equipment \} \}\.toMap\(\)',
        '/**\n     * Authoritative equipment for every exercise in the built-in pool, one of\n     * [allEquipmentCategories]. Generated from the exercise-database CSV; the\n     * equipment filter never guesses from the name, it reads this table.\n     */\n    private val exerciseEquipment: Map<String, String> =\n        ExerciseCatalog.ENTRIES.associate { it.name.lowercase() to it.equipment }',
        "equipment table -> ExerciseCatalog",
    )

    # --- 3. getAllExerciseNames -> catalog names ---------------------------
    src = sub_once(
        src,
        r'    private var _cachedExerciseNames: List<String>\? = null\n\n    fun getAllExerciseNames\(\): List<String> \{\n.*?\n    \}',
        '    fun getAllExerciseNames(): List<String> = ExerciseCatalog.NAMES',
        "getAllExerciseNames -> ExerciseCatalog.NAMES",
    )

    # --- 4. per-part pool lists -> catalog-driven lookups ------------------
    new_pool_fns = '''    private fun isExerciseInGroup(name: String, group: String): Boolean =
        ExerciseCatalog.muscleFor(name).equals(group, ignoreCase = true)

    /**
     * Every pool exercise whose authoritative body part is [bodyPart].
     *
     * The rebuilt pool carries exactly one precise muscle per exercise, so the
     * broad body parts (Arms, Back, Chest, Core, Legs, Shoulders) are a clean
     * partition: every exercise appears under exactly one part — the one its
     * badge shows. A precise muscle matches itself; a broad topic matches the
     * topic of the exercise's precise muscle.
     */
    private fun getExercisesForPart(bodyPart: String): List<String> {
        val wanted = bodyPart.lowercase().trim()
        return ExerciseCatalog.NAMES.filter { name ->
            val muscle = ExerciseCatalog.muscleFor(name)
            muscle.equals(bodyPart, ignoreCase = true) ||
                (muscle.isNotEmpty() && getTopicForMuscle(muscle).lowercase() == wanted)
        }
    }

    /** Equipment-specific views of [getExercisesForPart], kept for call sites. */
    private fun getBodyweightExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter { getEquipmentForExercise(it) == EQUIPMENT_BODYWEIGHT }

    private fun getDumbbellExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter { getEquipmentForExercise(it) == EQUIPMENT_DUMBBELL }

    private fun getBarbellExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter { getEquipmentForExercise(it) == EQUIPMENT_BARBELL }

    /** The historical "Gym" umbrella: cables and machines. */
    private fun getGymExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter {
            getEquipmentForExercise(it) == EQUIPMENT_CABLE || getEquipmentForExercise(it) == EQUIPMENT_MACHINE
        }
'''
    src = sub_once(
        src,
        r'    private fun isExerciseInGroup\(name: String, group: String\): Boolean \{\n.*?\n    \}\n\n    private fun getBodyweightExercises\(bodyPart: String\): List<String> = when\(bodyPart\.lowercase\(\)\.trim\(\)\) \{\n.*?\n        else -> emptyList\(\)\n    \}\n\n    private fun getDumbbellExercises\(bodyPart: String\): List<String> = when\(bodyPart\.lowercase\(\)\.trim\(\)\) \{\n.*?\n        else -> listOf\("Dumbbell Thrusters", "Devil Presses", "Dumbbell Snatches"\)\n    \}\n\n    private fun getBarbellExercises\(bodyPart: String\): List<String> = when\(bodyPart\.lowercase\(\)\.trim\(\)\) \{\n.*?\n        else -> listOf\("Barbell Clean & Presses", "Barbell Snatches", "Barbell Clean & Jerks", "Barbell Hang Clean", "Barbell Power Snatch"\)\n    \}\n\n    private fun getGymExercises\(bodyPart: String\): List<String> = when\(bodyPart\.lowercase\(\)\.trim\(\)\) \{\n.*?\n        else -> emptyList\(\)\n    \}',
        new_pool_fns.rstrip("\n"),
        "per-part pool lists -> catalog-driven",
    )

    # --- 5. isBodyweightExercise bodyweight scan ---------------------------
    src = sub_once(
        src,
        r'        val allBW = getAllMuscleGroups\(\)\.flatMap \{ getBodyweightExercises\(it\) \}\n        return allBW\.any \{ it\.equals\(name, ignoreCase = true\) \}',
        '        // The rebuilt pool knows its equipment authoritatively; a pool\n        // exercise is bodyweight exactly when the catalog says so.\n        return ExerciseCatalog.equipmentFor(name) == EQUIPMENT_BODYWEIGHT',
        "isBodyweightExercise -> catalog equipment",
    )

    # --- 6. remap legacy names inside the templates ------------------------
    src = rename_names(src)
    TEMPLATES.write_text(src, encoding="utf-8")
    print(f"  wrote {TEMPLATES.relative_to(ROOT)}")

    defaults = DEFAULTS.read_text(encoding="utf-8")
    defaults = rename_names(defaults)
    DEFAULTS.write_text(defaults, encoding="utf-8")
    print(f"  wrote {DEFAULTS.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
