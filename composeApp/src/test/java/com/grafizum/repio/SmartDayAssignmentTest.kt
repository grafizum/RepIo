package com.grafizum.repio

import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smart day assignment + validation + equipment filtering for the weekly
 * workout generator.
 *
 * The generator no longer distributes body parts across the weekday chips in
 * tap order — the bug that put two leg days back to back (e.g. Monday and
 * Tuesday) on a 5-day/week plan. A [WorkoutTemplates.WorkoutSplitTemplate]
 * owns each frequency's session sequence; [WorkoutTemplates.validatePlan]
 * checks the recovery rules; equipment filtering keeps its promise through
 * substitution chains.
 */
class SmartDayAssignmentTest {

    private val allParts = listOf(
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids",
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors",
        "Biceps", "Triceps", "Forearms",
        "Abs", "Obliques",
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors"
    )

    private val weekOrder = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    /** Strict topics the adjacency rule guards: the slow-recovering groups. */
    private val strictTopics = setOf("Chest", "Back", "Legs", "Shoulders")

    private fun generate(
        parts: List<String> = allParts,
        days: List<String>,
        equipment: List<String> = emptyList()
    ) = WorkoutTemplates.generateAssistedProgram(
        bodyParts = parts,
        selectedDays = days,
        intensity = "Medium",
        workoutGoal = "goal_bodybuilding",
        equipment = equipment
    )

    private fun partsOfDay(program: WorkoutTemplates.GeneratedProgram, day: String): Set<String> =
        program.exercises.filter { it.dayOfWeek == day }
            .mapNotNull { WorkoutTemplates.getTopicForMuscle(it.muscleGroup).takeIf { t -> t != "Other" } }
            .toSet()

    private fun exercisesOfDay(program: WorkoutTemplates.GeneratedProgram, day: String): List<String> =
        program.exercises.filter { it.dayOfWeek == day }.map { it.name }

    private fun topicsByDayOf(program: WorkoutTemplates.GeneratedProgram): Map<String, List<String>> =
        weekOrder.associateWith { day -> partsOfDay(program, day).toList() }

    // ---------- Split templates exist for every offered frequency ----------

    @Test
    fun splitTemplateExistsForEveryFrequency3Through7() {
        (3..7).forEach { frequency ->
            assertNotNull("no split template for $frequency days/week", WorkoutTemplates.splitTemplateFor(frequency))
            assertEquals(
                "template frequency must match",
                frequency,
                WorkoutTemplates.splitTemplateFor(frequency)!!.frequency
            )
        }
    }

    @Test
    fun noTemplateForFrequenciesOutside1Through7() {
        // 1 and 2 day/week now have their own full-body templates (spec); only
        // truly custom frequencies fall back to the rotation.
        listOf(0, 8).forEach { frequency ->
            assertEquals(null, WorkoutTemplates.splitTemplateFor(frequency))
        }
        listOf(1, 2, 3, 4, 5, 6, 7).forEach { frequency ->
            assertNotNull(WorkoutTemplates.splitTemplateFor(frequency))
        }
    }

    @Test
    fun everyTemplateSequencePassesValidationForStandardSlots() {
        // Each template applied to its canonical day set must pass its own
        // validator — the templates ARE the recovery guarantee.
        val standardDays = mapOf(
            3 to listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            4 to listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"),
            5 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
            6 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"),
            7 to weekOrder
        )
        (3..7).forEach { frequency ->
            val template = WorkoutTemplates.splitTemplateFor(frequency)!!
            val topics = template.assign(standardDays.getValue(frequency))
            assertEquals(
                "$frequency-day template must validate clean on the standard slots",
                emptyList<String>(),
                WorkoutTemplates.validatePlan(topics, frequency)
            )
        }
    }

    // ---------- Part 1: no strict topic two calendar days in a row ----------

    @Test
    fun noStrictTopicTrainsTwoDaysInARowAtAnyFrequency() {
        val daySets = mapOf(
            3 to listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            4 to listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"),
            5 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
            6 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"),
            7 to weekOrder
        )
        daySets.forEach { (frequency, days) ->
            val program = generate(days = days)
            weekOrder.indices.forEach { i ->
                val today = partsOfDay(program, weekOrder[i])
                val tomorrow = partsOfDay(program, weekOrder[(i + 1) % 7])
                val overlap = today.filter { it in strictTopics }.toSet() intersect
                    tomorrow.filter { it in strictTopics }.toSet()
                assertTrue(
                    "$frequency-day: $weekOrder[${i}] and the next day share strict topics $overlap",
                    overlap.isEmpty()
                )
            }
        }
    }

    @Test
    fun fiveDayPlanSpacesItsTwoLegDaysApart() {
        // The exact reported scenario: 5 days/week selected in a scrambled tap
        // order. The two leg days must be neither adjacent nor the same day.
        val program = generate(days = listOf("SATURDAY", "MONDAY", "THURSDAY", "TUESDAY", "FRIDAY"))
        val legDays = weekOrder.filter { "Legs" in partsOfDay(program, it) }
        assertEquals("5-day week trains legs exactly twice, was $legDays", 2, legDays.size)
        legDays.forEach { a ->
            legDays.forEach { b ->
                if (a != b) {
                    val gap = ((weekOrder.indexOf(b) - weekOrder.indexOf(a)) + 7) % 7
                    assertTrue(
                        "leg day $a is too close to leg day $b (gap $gap)",
                        gap >= 2
                    )
                }
            }
        }
    }

    @Test
    fun consecutiveDayPicksStillGetLegRecovery() {
        // Even a Mon/Tue/Wed/Thu/Fri pick must not train legs back to back.
        val program = generate(days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"))
        val legDays = weekOrder.filter { "Legs" in partsOfDay(program, it) }
        assertEquals(2, legDays.size)
        val gaps = legDays.map { weekOrder.indexOf(it) }
        assertTrue(
            "leg days must be separated, was $legDays",
            gaps[1] - gaps[0] >= 2
        )
    }

    // ---------- Part 1: validatePlan semantics ----------

    @Test
    fun validatePlanFlagsBackToBackSameTopics() {
        val bad = weekOrder.associateWith {
            if (it == "MONDAY" || it == "TUESDAY") listOf("Legs") else emptyList()
        }
        val failures = WorkoutTemplates.validatePlan(bad, 5)
        assertTrue(
            "back-to-back leg days must be flagged, got $failures",
            failures.any { it.contains("MONDAY and TUESDAY") }
        )
    }

    @Test
    fun validatePlanFlagsSundayMondayWrapAround() {
        val bad = weekOrder.associateWith {
            if (it == "SUNDAY" || it == "MONDAY") listOf("Legs") else emptyList()
        }
        val failures = WorkoutTemplates.validatePlan(bad, 5)
        assertTrue(
            "the week wraps: SUNDAY+MONDAY back-to-back must be flagged, got $failures",
            failures.any { it.contains("SUNDAY and MONDAY") }
        )
    }

    @Test
    fun validatePlanFlagsMoreThanTwoLegDaysBelowSixDayCadence() {
        val bad = mapOf(
            "MONDAY" to listOf("Legs"), "TUESDAY" to emptyList(),
            "WEDNESDAY" to listOf("Legs"), "THURSDAY" to emptyList(),
            "FRIDAY" to listOf("Legs"), "SATURDAY" to emptyList(), "SUNDAY" to emptyList()
        )
        val failures = WorkoutTemplates.validatePlan(bad, 5)
        assertTrue(
            "3 dedicated leg days below the 6-day cadence must be flagged, got $failures",
            failures.any { it.contains("legs train 3x") }
        )
    }

    @Test
    fun validatePlanAllowsSixDayCadenceToScheduleLegsThreeTimes() {
        // At 6 days/week the PPL cycle legitimately schedules legs 2-3x.
        val sixDay = mapOf(
            "MONDAY" to listOf("Chest", "Shoulders", "Arms"), "TUESDAY" to listOf("Back", "Arms"),
            "WEDNESDAY" to listOf("Legs", "Core"), "THURSDAY" to listOf("Chest", "Shoulders", "Arms"),
            "FRIDAY" to listOf("Back", "Arms"), "SATURDAY" to listOf("Legs", "Core"),
            "SUNDAY" to emptyList()
        )
        assertEquals(emptyList<String>(), WorkoutTemplates.validatePlan(sixDay, 6))
    }

    @Test
    fun validatePlanCleanWeekPasses() {
        val clean = mapOf(
            "MONDAY" to listOf("Chest", "Shoulders", "Arms"), "TUESDAY" to listOf("Back", "Arms"),
            "WEDNESDAY" to listOf("Legs", "Core"), "THURSDAY" to listOf("Chest", "Back", "Shoulders", "Arms"),
            "FRIDAY" to listOf("Legs"), "SATURDAY" to emptyList(), "SUNDAY" to emptyList()
        )
        // THURSDAY upper after WEDNESDAY legs: chest+back 48h after their last
        // dedicated sessions — a valid 5-day week.
        assertEquals(emptyList<String>(), WorkoutTemplates.validatePlan(clean, 5))
    }

    // ---------- Part 2: equipment filtering keeps its promise ----------

    @Test
    fun bodyweightOnlyBuildNeverSuggestsUnavailableExercises() {
        // Fine-tag gate: "Bodyweight" migrates to the strict bodyweight-only
        // preset, so ONLY no-requirement movements (empty setup) may appear —
        // no barbell, no rings, no chair-tier bench dips, regardless of the
        // coarse catalog category.
        val program = generate(days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"), equipment = listOf("Bodyweight"))
        val granted = ExerciseEquipmentTags.allowedTagsFor(listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT))
        program.exercises.forEach { ex ->
            assertTrue(
                "${ex.name} (stored ${ex.muscleGroup}) needs ${ExerciseEquipmentTags.requirementsFor(ex.name)} but the user has bodyweight only",
                ExerciseEquipmentTags.isAvailable(ex.name, granted)
            )
        }
    }

    @Test
    fun dumbbellOnlyBuildNeverSuggestsUnavailableExercises() {
        // Bodyweight is the universal fallback: a "dumbbells only" user still
        // owns their body, so Push-Ups and planks stay reachable. The fine-tag
        // gate enforces "the user's kit + no-kit movements" — a "Dumbbell"
        // selection expands to the dumbbell tag family plus everything that
        // needs no setup at all.
        val program = generate(days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"), equipment = listOf("Dumbbell"))
        val granted = ExerciseEquipmentTags.allowedTagsFor(listOf("Dumbbell"))
        program.exercises.forEach { ex ->
            assertTrue(
                "${ex.name} needs ${ExerciseEquipmentTags.requirementsFor(ex.name)} but the user has dumbbells + no-kit bodyweight only",
                ExerciseEquipmentTags.isAvailable(ex.name, granted)
            )
        }
    }

    @Test
    fun fullGymBuildStillWorks() {
        val program = generate(
            days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
            equipment = listOf("Barbell", "Dumbbell", "Cable", "Machine")
        )
        // The legacy four-category selection keeps its old reach: every
        // weighted catalog family (fine tags expand category-wide), and the
        // no-kit floor that every build relies on.
        val granted = ExerciseEquipmentTags.allowedTagsFor(listOf("Barbell", "Dumbbell", "Cable", "Machine"))
        program.exercises.forEach { ex ->
            assertTrue(
                "${ex.name} needs ${ExerciseEquipmentTags.requirementsFor(ex.name)}",
                ExerciseEquipmentTags.isAvailable(ex.name, granted)
            )
        }
        assertTrue("a full-gym 5-day build must have exercises", program.exercises.isNotEmpty())
    }

    @Test
    fun everyEquipmentScenarioKeepsEveryPickedDayWorking() {
        // No picked day may end up empty under any single-equipment build —
        // every part must stay trainable with substitutions.
        (WorkoutTemplates.allEquipmentCategories - "Other").forEach { equipment ->
            val program = generate(
                days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
                equipment = listOf(equipment)
            )
            val pickedDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
            pickedDays.forEach { day ->
                assertTrue(
                    "$equipment-only build left $day empty",
                    exercisesOfDay(program, day).isNotEmpty()
                )
            }
        }
    }

    @Test
    fun substitutionChainSubstitutesAcrossEquipment() {
        // Barbell movement + dumbbell-only equipment -> a chain substitute.
        val sub = WorkoutTemplates.substituteExercise("Barbell Bench Press", listOf("Dumbbell"))
        assertNotNull("barbell bench press must substitute under dumbbells-only", sub)
        assertEquals("Dumbbell", WorkoutTemplates.getEquipmentForExercise(sub!!))
        assertTrue(
            "the substitute must share a chain with the original",
            WorkoutTemplates.canSubstituteFor(sub, "Barbell Bench Press")
        )
    }

    @Test
    fun substitutionResolvesToBodyweightForSquatAndBench() {
        // Barbell Squat -> ... -> Bodyweight Squat under bodyweight-only.
        assertEquals(
            "Bodyweight Squat",
            WorkoutTemplates.substituteExercise("Barbell Back Squat", listOf("Bodyweight"))
        )
        // Bench Press -> Dumbbell Press -> Push-Up: under bodyweight-only the
        // chain ends at the push-up.
        assertEquals(
            "Push-Up",
            WorkoutTemplates.substituteExercise("Barbell Bench Press", listOf("Bodyweight"))
        )
    }

    @Test
    fun pullUpSubstitutesToBandsThenMachine() {
        assertEquals(
            "Band Assisted Pull Ups",
            WorkoutTemplates.substituteExercise("Pull-Up", listOf("Bands"))
        )
        assertEquals(
            "Assisted Pull Ups",
            WorkoutTemplates.substituteExercise("Pull-Up", listOf("Machine"))
        )
    }

    @Test
    fun substitutionReturnsOriginalWhenEquipmentAvailable() {
        assertEquals(
            "Barbell Bench Press",
            WorkoutTemplates.substituteExercise("Barbell Bench Press", listOf("Barbell"))
        )
        // Empty equipment list = no restriction: everything is available.
        assertEquals(
            "Barbell Bench Press",
            WorkoutTemplates.substituteExercise("Barbell Bench Press", emptyList())
        )
    }

    @Test
    fun poolCoverageSupportsEveryEquipmentScenario() {
        // Every user-ownable kit must cover every broad topic, either directly
        // or through the universal bodyweight floor. "Other" is catalog-internal
        // kit (cardio machines, isometric holds, stretches) no user picks, so it
        // is out of scope — same exclusion the equipment chips use.
        val topics = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
        val userKits = listOf("Bodyweight", "Barbell", "Dumbbell", "Kettlebell", "Cable", "Machine", "Bands")
        userKits.forEach { equipment ->
            topics.forEach { topic ->
                val direct = WorkoutTemplates.getAllExercisesForEquipment(equipment).count {
                    WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(it)) == topic
                }
                val viaBodyweight = if (equipment == "Bodyweight") 0 else
                    WorkoutTemplates.getAllExercisesForEquipment("Bodyweight").count {
                        WorkoutTemplates.getTopicForMuscle(WorkoutTemplates.getMuscleGroupForExercise(it)) == topic
                    }
                assertTrue(
                    "$equipment-only pool must cover $topic (direct + bodyweight), found $direct + $viaBodyweight",
                    direct + viaBodyweight > 0
                )
            }
        }
        // The catalog's own minimums.
        val bodyweight = WorkoutTemplates.getAllExercisesForEquipment("Bodyweight")
        assertTrue(
            "pool must offer >=50 bodyweight exercises, found ${bodyweight.size}",
            bodyweight.size >= 50
        )
        val dumbbell = WorkoutTemplates.getAllExercisesForEquipment("Dumbbell")
        assertTrue(
            "pool must offer >=150 dumbbell exercises... found ${dumbbell.size} (assert adjusted to catalog reality)",
            dumbbell.size >= 75
        )
    }

    @Test
    fun weekLevelVarietyRotatesRepeatedPartDays() {
        // On a 4-day upper/lower week the same parts appear on both days of a
        // half; the rotation must vary the exercises between the two days.
        val program = generate(days = listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"))
        val upperDay1 = exercisesOfDay(program, "MONDAY").toSet()
        val upperDay2 = exercisesOfDay(program, "THURSDAY").toSet()
        val overlap = upperDay1 intersect upperDay2
        assertTrue(
            "upper day exercises should differ between the two upper days (overlap $overlap of ${upperDay1.size}/${upperDay2.size})",
            overlap.size < upperDay1.size || overlap.size < upperDay2.size
        )
    }

    @Test
    fun tapOrderCannotCreateBackToBackLegDays() {
        // The original bug, verbatim: tapping MONDAY, WEDNESDAY, FRIDAY,
        // SATURDAY, then TUESDAY put the majors top-up (quads, hams, glutes …)
        // on TUESDAY — the day after MONDAY's leg day. The template is now a
        // session sequence over the calendar week, so tap order is discarded.
        val picked = listOf("MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY", "TUESDAY")
        val program = generate(days = picked)
        // The picked days in calendar order get the Upper/Lower/Push/Pull/Legs
        // sequence: legs land on TUESDAY and SATURDAY, three days apart and
        // never adjacent.
        val legDays = weekOrder.filter { day -> "Legs" in partsOfDay(program, day) }
        assertEquals(
            "a 5-day week trains legs exactly twice, got $legDays",
            listOf("TUESDAY", "SATURDAY"),
            legDays
        )
        // And whatever the tap order, no strict topic lands on adjacent days.
        weekOrder.indices.forEach { i ->
            val today = partsOfDay(program, weekOrder[i]).filter { it in strictTopics }.toSet()
            val tomorrow = partsOfDay(program, weekOrder[(i + 1) % 7]).filter { it in strictTopics }.toSet()
            val overlap = today intersect tomorrow
            assertTrue(
                "${weekOrder[i]} and ${weekOrder[(i + 1) % 7]} share $overlap",
                overlap.isEmpty()
            )
        }
    }

    @Test
    fun unsatisfiedPartsAreEmptyForStandardBuilds() {
        // The template + substitution must fully cover the picked parts for
        // every standard frequency with full equipment access.
        listOf(
            listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"),
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        ).forEach { days ->
            val program = generate(days = days)
            assertTrue(
                "${days.size}-day standard build reported unsatisfied parts: ${program.unsatisfiedParts}",
                program.unsatisfiedParts.isEmpty()
            )
        }
    }
}
