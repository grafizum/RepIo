package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fine-grained equipment tag layer: every pool exercise carries precise
 * requirement sets, the welcome-screen presets map to real tag grants, and
 * the two reported bugs (Decline Bench Press for a dumbbells-no-bench user,
 * Bench Dips for a user without a chair) stay dead.
 */
class ExerciseEquipmentTagsTest {

    private val bwOnly = setOf<String>()
    private val bwBands = setOf(ExerciseEquipmentTags.RESISTANCE_BANDS)
    private val bwPullUpBar = setOf(ExerciseEquipmentTags.PULL_UP_BAR)
    // A chair and a bench are separate tiles; the legacy tag expanded to both.
    private val bwChair = setOf(ExerciseEquipmentTags.CHAIR)
    private val bwBench = setOf(ExerciseEquipmentTags.BENCH)
    private val dumbbellsOnly = setOf(
        ExerciseEquipmentTags.DUMBBELLS_ONLY, ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR
    )
    private val dumbbellsBench = dumbbellsOnly + ExerciseEquipmentTags.DUMBBELLS_AND_BENCH

    private fun fullGymPreset(): List<String> = listOf(ExerciseEquipmentTags.PRESET_FULL_GYM)
    private fun dumbbellPreset(): List<String> = listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS)
    private fun bodyweightPreset(): List<String> = listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT)

    // ---------- 1. Every pool exercise resolves to known, satisfiable tags ----------

    @Test
    fun everyPoolExerciseHasAtLeastOneRequirementSet() {
        val unknown = ExerciseCatalog.ENTRIES.filter {
            ExerciseEquipmentTags.requirementsFor(it.name).isEmpty()
        }
        assertTrue("No requirement sets for: ${unknown.take(5).map { it.name }}", unknown.isEmpty())
    }

    @Test
    fun allEmittedTagsAreKnown() {
        val emitted = ExerciseCatalog.ENTRIES
            .flatMap { ExerciseEquipmentTags.requirementsFor(it.name) }
            .flatten()
            .toSet()
        val unknown = emitted - ExerciseEquipmentTags.ALL_TAGS.toSet()
        assertTrue("Classifier emitted unknown tags: $unknown", unknown.isEmpty())
    }

    @Test
    fun fullGymGrantsEverything() {
        val granted = ExerciseEquipmentTags.allowedTagsFor(fullGymPreset())
        ExerciseCatalog.ENTRIES.forEach { entry ->
            assertTrue(
                "Full gym must unlock ${entry.name}",
                ExerciseEquipmentTags.isAvailable(entry.name, granted)
            )
        }
    }

    // ---------- 2. The spec's worked examples ----------

    @Test
    fun specExamplesResolveToTheirDocumentedTags() {
        // Push-Up -> none_required
        assertEquals(listOf<Set<String>>(emptySet()), ExerciseEquipmentTags.requirementsFor("Push-Up"))
        // Decline Bench Press -> dumbbells on a BENCH, nothing else.
        // REVERSED 2026-10-08 twice over: it used to accept a chair as a second
        // seat (the owner's reported leak, "I only have chair and dumbell and yet
        // the system asks me to do an inclinde dumbell press"), and it also
        // accepted a rack. The rack alternative was a copy of the BARBELL row's
        // gate - "Decline Barbell Bench Press" is its own catalog entry - and it
        // meant a barbell-only user was offered a movement filed under the
        // Dumbbell family.
        assertEquals(
            listOf(setOf(ExerciseEquipmentTags.BENCH, ExerciseEquipmentTags.DUMBBELLS_ONLY)),
            ExerciseEquipmentTags.requirementsFor("Decline Bench Press")
        )
        // Dips on furniture: a chair OR a bench, never the merged legacy tag.
        assertEquals(
            listOf(setOf(ExerciseEquipmentTags.CHAIR), setOf(ExerciseEquipmentTags.BENCH)),
            ExerciseEquipmentTags.requirementsFor("Bench Dips")
        )
        assertTrue(
            ExerciseEquipmentTags.CHAIR_OR_BENCH !in
                ExerciseEquipmentTags.requirementsFor("Bench Dips").flatten()
        )
        // Goblet Squat -> dumbbells_only
        assertEquals(
            listOf(setOf(ExerciseEquipmentTags.DUMBBELLS_ONLY)),
            ExerciseEquipmentTags.requirementsFor("Goblet Squat")
        )
        // Dumbbell Bench Press -> dumbbells on a BENCH. A chair is not one.
        assertEquals(
            listOf(setOf(ExerciseEquipmentTags.BENCH, ExerciseEquipmentTags.DUMBBELLS_ONLY)),
            ExerciseEquipmentTags.requirementsFor("Dumbbell Bench Press")
        )
        // Dumbbell Floor Press -> dumbbells_and_floor (NO bench needed)
        assertEquals(
            listOf(setOf(ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR)),
            ExerciseEquipmentTags.requirementsFor("Dumbbell Floor Press")
        )
    }

    // ---------- 3. The two reported bugs stay dead ----------

    @Test
    fun declineBenchPressIsBlockedWithoutASeatEvenWithDumbbells() {
        val granted = ExerciseEquipmentTags.allowedTagsFor(dumbbellPreset())
        assertFalse(
            "Decline Bench Press must require a bench (dumbbells alone are not enough)",
            ExerciseEquipmentTags.isAvailable("Decline Bench Press", granted)
        )
        assertTrue(
            "dumbbells and a bench are two tiles, and together they are enough",
            ExerciseEquipmentTags.isAvailable("Decline Bench Press",
                dumbbellsOnly + ExerciseEquipmentTags.BENCH)
        )
        assertFalse(
            "a chair is NOT a bench: it has no adjustable decline angle, and the " +
                "owner reported this exact leak on 2026-10-08",
            ExerciseEquipmentTags.isAvailable("Decline Bench Press",
                dumbbellsOnly + ExerciseEquipmentTags.CHAIR)
        )
        // And a barbell kit must NOT reach it: this row is the DUMBBELL decline
        // press, and "Decline Barbell Bench Press" is the barbell one.
        assertFalse(
            "a barbell kit must use the barbell row, not the dumbbell one",
            ExerciseEquipmentTags.isAvailable("Decline Bench Press",
                setOf(ExerciseEquipmentTags.BARBELL_AND_RACK))
        )
        assertTrue(
            "the barbell row is reachable by the barbell kit",
            ExerciseEquipmentTags.isAvailable("Decline Barbell Bench Press",
                setOf(ExerciseEquipmentTags.BARBELL_AND_RACK))
        )
    }

    @Test
    fun benchDipsRequireAStableChairOrBench() {
        assertFalse(
            "Bench Dips must be blocked for a user with no chair/bench",
            ExerciseEquipmentTags.isAvailable("Bench Dips", bwOnly)
        )
        assertTrue(ExerciseEquipmentTags.isAvailable("Bench Dips", bwChair))
        // A user without a chair still gets a triceps movement from the chain.
        val sub = WorkoutTemplates.substituteExercise("Bench Dips", bodyweightPreset())
        assertEquals("Diamond Push Ups", sub)
    }

    // ---------- 4. Scenario availability ----------

    @Test
    fun bodyweightOnlyBlocksAllWeightedAndStationExercises() {
        val weighted = ExerciseCatalog.ENTRIES.filter {
            it.equipment in listOf("Barbell", "Dumbbell", "Kettlebell", "Cable", "Machine")
        }
        weighted.forEach { entry ->
            assertFalse(
                "Bodyweight-only must block ${entry.name}",
                ExerciseEquipmentTags.isAvailable(entry.name, bwOnly)
            )
        }
    }

    @Test
    fun dumbbellsOnlyBlocksBarbellMachineAndCable() {
        val granted = ExerciseEquipmentTags.allowedTagsFor(dumbbellPreset())
        ExerciseCatalog.ENTRIES.filter {
            it.equipment in listOf("Barbell", "Machine", "Cable")
        }.forEach { entry ->
            assertFalse(
                "Dumbbells-only must block ${entry.name}",
                ExerciseEquipmentTags.isAvailable(entry.name, granted)
            )
        }
        // Floor work is exactly the point of the dumbbells_no_bench preset.
        assertTrue(ExerciseEquipmentTags.isAvailable("Dumbbell Floor Press", granted))
        assertFalse(
            "Incline work needs the bench tier",
            ExerciseEquipmentTags.isAvailable("Incline Dumbbell Press", granted)
        )
    }

    @Test
    fun homeCheckboxesUnlockExactlyTheirTier() {
        // Pull-up bar: unlocks the hanging family, not dips.
        assertTrue(ExerciseEquipmentTags.isAvailable("Pull-Up", bwPullUpBar))
        assertTrue(ExerciseEquipmentTags.isAvailable("Hanging Leg Raise", bwPullUpBar))
        assertFalse(ExerciseEquipmentTags.isAvailable("Straight Bar Dips", bwPullUpBar))
        // Dip bars unlock dips but not pull-ups.
        assertTrue(ExerciseEquipmentTags.isAvailable("Straight Bar Dips", setOf(ExerciseEquipmentTags.DIP_BARS)))
        assertFalse(ExerciseEquipmentTags.isAvailable("Pull-Up", setOf(ExerciseEquipmentTags.DIP_BARS)))
        // Bands unlock the banded pool plus band-assisted pull-ups.
        assertTrue(ExerciseEquipmentTags.isAvailable("Banded Squat", bwBands))
        assertTrue(ExerciseEquipmentTags.isAvailable("Assisted Pull Ups (Band)", bwBands))
        assertFalse(ExerciseEquipmentTags.isAvailable("Pull-Up", bwBands))
        // Rings/TRX tier.
        assertTrue(ExerciseEquipmentTags.isAvailable("Ring Push-Up", setOf(ExerciseEquipmentTags.GYMNASTIC_RINGS)))
        assertTrue(ExerciseEquipmentTags.isAvailable("TRX Row", setOf(ExerciseEquipmentTags.GYMNASTIC_RINGS)))
        // Chair tier: incline/decline push-up variants, step-ups, box jumps.
        assertTrue(ExerciseEquipmentTags.isAvailable("Decline Push-Up", bwChair))
        assertTrue(ExerciseEquipmentTags.isAvailable("Step Ups", bwChair))
        // Wall tier.
        assertTrue(ExerciseEquipmentTags.isAvailable("Wall Push Ups", setOf(ExerciseEquipmentTags.WALL)))
        assertTrue(ExerciseEquipmentTags.isAvailable("Wall Sit", setOf(ExerciseEquipmentTags.WALL)))
    }

    @Test
    fun floorPushUpFamilySurvivesStrictBodyweightOnly() {
        // Chest volume on a strict home build rides the floor push-up family.
        listOf(
            "Push-Up", "Knee Push Ups", "Wide Grip Push Ups", "Close Grip Push Ups",
            "Diamond Push Ups", "Archer Push Ups", "Clap Push-Ups",        ).forEach { name ->
            assertTrue(
                "$name must need no kit",
                ExerciseEquipmentTags.isAvailable(name, bwOnly)
            )
        }
        // …while its elevated siblings do not.
        assertFalse(ExerciseEquipmentTags.isAvailable("Decline Push-Up", bwOnly))
        assertFalse(ExerciseEquipmentTags.isAvailable("Wall Push Ups", bwOnly))
    }

    @Test
    fun householdLookalikesNeedNoGear() {
        // "Chair Pose" is a yoga pose — its name lies about the gear.
        assertTrue(ExerciseEquipmentTags.isAvailable("Chair Pose", bwOnly))
        assertTrue(ExerciseEquipmentTags.isAvailable("Plank", bwOnly))
        // An inverted row needs something sturdy to hang from: chair tier.
        assertTrue(ExerciseEquipmentTags.isAvailable("Inverted Row", bwChair))
        assertFalse(ExerciseEquipmentTags.isAvailable("Inverted Row", bwOnly))
    }

    // ---------- 5. Presets, legacy categories and migration ----------

    @Test
    fun presetsMapToTheirTagGrants() {
        assertEquals(emptySet<String>(), ExerciseEquipmentTags.presetTags(ExerciseEquipmentTags.PRESET_BODYWEIGHT))
        assertEquals(
            setOf(ExerciseEquipmentTags.PULL_UP_BAR),
            ExerciseEquipmentTags.presetTags(ExerciseEquipmentTags.PRESET_BW_PULLUP_BAR)
        )
        assertEquals(
            ExerciseEquipmentTags.ALL_TAGS.toSet(),
            ExerciseEquipmentTags.presetTags(ExerciseEquipmentTags.PRESET_FULL_GYM)
        )
    }

    @Test
    fun emptyAndAllSelectionsStayUnrestricted() {
        assertEquals(
            ExerciseEquipmentTags.ALL_TAGS.toSet(),
            ExerciseEquipmentTags.allowedTagsFor(emptyList())
        )
        assertEquals(
            ExerciseEquipmentTags.ALL_TAGS.toSet(),
            ExerciseEquipmentTags.allowedTagsFor(listOf(WorkoutTemplates.EQUIPMENT_ALL))
        )
    }

    @Test
    fun legacyCategoriesExpandToTheirTagFamilies() {
        assertEquals(
            setOf(
                ExerciseEquipmentTags.DUMBBELLS_ONLY,
                ExerciseEquipmentTags.DUMBBELLS_AND_BENCH,
                ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
            ),
            ExerciseEquipmentTags.allowedTagsFor(listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL))
        )
        assertEquals(
            setOf(ExerciseEquipmentTags.RESISTANCE_BANDS),
            ExerciseEquipmentTags.allowedTagsFor(listOf(WorkoutTemplates.EQUIPMENT_BANDS))
        )
    }

    @Test
    fun legacySelectionsMigrateToPresetsOrCustom() {
        assertEquals(
            listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT),
            ExerciseEquipmentTags.migrateLegacy(listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT))
        )
        assertEquals(
            listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS),
            ExerciseEquipmentTags.migrateLegacy(listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL))
        )
        assertEquals(
            listOf(ExerciseEquipmentTags.PRESET_FULL_GYM),
            ExerciseEquipmentTags.migrateLegacy(listOf(WorkoutTemplates.EQUIPMENT_ALL))
        )
        // Already-migrated values pass through untouched (idempotent).
        assertEquals(
            listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS),
            ExerciseEquipmentTags.migrateLegacy(listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS))
        )
        // Multi-category legacy picks become a custom tag list.
        val migrated = ExerciseEquipmentTags.migrateLegacy(
            listOf(WorkoutTemplates.EQUIPMENT_DUMBBELL, WorkoutTemplates.EQUIPMENT_BANDS)
        )
        assertEquals(ExerciseEquipmentTags.PRESET_CUSTOM, migrated.first())
        assertTrue(migrated.contains(ExerciseEquipmentTags.RESISTANCE_BANDS))
    }

    // ---------- 6. Generator integration: scenarios produce clean plans ----------

    private fun generate(days: Int, equipment: List<String>, duration: String = WorkoutTemplates.DURATION_STANDARD) =
        WorkoutTemplates.generateAssistedProgram(
            bodyParts = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core"),
            selectedDays = listOf(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
            ).take(days),
            intensity = "Medium",
            workoutGoal = "goal_muscle_gain",
            equipment = equipment,
            duration = duration
        )

    @Test
    fun bodyweightBuildContainsNoWeightedExercises() {
        val program = generate(5, bodyweightPreset())
        assertTrue(program.exercises.isNotEmpty())
        val granted = ExerciseEquipmentTags.allowedTagsFor(bodyweightPreset())
        val offenders = program.exercises.map { it.name }.distinct().filter {
            !ExerciseEquipmentTags.isAvailable(it, granted)
        }
        assertTrue("Bodyweight build offered: $offenders", offenders.isEmpty())
    }

    @Test
    fun dumbbellBuildContainsNoBarbellMachineOrCableExercises() {
        val program = generate(4, dumbbellPreset())
        assertTrue(program.exercises.isNotEmpty())
        val granted = ExerciseEquipmentTags.allowedTagsFor(dumbbellPreset())
        val offenders = program.exercises.map { it.name }.distinct().filter {
            !ExerciseEquipmentTags.isAvailable(it, granted)
        }
        assertTrue("Dumbbells-only build offered: $offenders", offenders.isEmpty())
    }

    @Test
    fun chairBenchPushUpVariantsStayOutOfBuildsWithoutAChair() {
        val program = generate(3, bodyweightPreset(), WorkoutTemplates.DURATION_QUICK)
        val granted = ExerciseEquipmentTags.allowedTagsFor(bodyweightPreset())
        listOf("Bench Dips", "Decline Push-Up", "Incline Push-Up", "Step Ups", "Straight Bar Dips")
            .forEach { blocked ->
                assertFalse(
                    "$blocked must not appear without a chair/bench",
                    program.exercises.any { it.name.equals(blocked, ignoreCase = true) }
                )
            }
        program.exercises.forEach { assertTrue(ExerciseEquipmentTags.isAvailable(it.name, granted)) }
    }

    @Test
    fun emptyEquipmentSelectionKeepsTheUnrestrictedPool() {
        val program = generate(3, emptyList(), WorkoutTemplates.DURATION_QUICK)
        assertTrue(program.exercises.map { it.name }.distinct().size > 5)
    }

    @Test
    fun bandAssistedChainWalksWhenPullUpBarIsMissing() {
        // Bodyweight + bands: the plain Pull-Up stays (it needs nothing?? no —
        // it needs the bar). The chain must land on the band-assisted variant.
        val sub = WorkoutTemplates.substituteExercise("Pull-Up", listOf(ExerciseEquipmentTags.PRESET_BW_BANDS))
        assertEquals("Assisted Pull Ups (Band)", sub)
        // With a pull-up bar granted, the original stays.
        val kept = WorkoutTemplates.substituteExercise("Pull-Up", listOf(ExerciseEquipmentTags.PRESET_BW_PULLUP_BAR))
        assertEquals("Pull-Up", kept)
    }

    @Test
    fun barbellBenchWalksDownToAnAvailableBenchFreePress() {
        val sub = WorkoutTemplates.substituteExercise("Barbell Bench Press", dumbbellPreset())
        assertEquals("Dumbbell Floor Press", sub)
    }

    @Test
    fun premadeMappingSurvivesStrictBodyweight() {
        // mapToEquipmentFiltered's gate: a template full of barbell/machine
        // work collapses to its available remainder (never to weighted names).
        val granted = ExerciseEquipmentTags.allowedTagsFor(bodyweightPreset())
        val names = listOf("Barbell Bench Press", "Leg Press", "Push-Up", "Bench Dips")
        val resolved = names.mapNotNull {
            if (ExerciseEquipmentTags.isAvailable(it, granted)) it
            else WorkoutTemplates.substituteExercise(it, bodyweightPreset())
        }
        assertTrue(resolved.contains("Push-Up"))
        assertFalse(resolved.contains("Barbell Bench Press"))
        assertFalse(resolved.contains("Bench Dips"))
        assertFalse(resolved.contains("Leg Press"))
    }
}
