package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The build distributes body parts across the week by *training frequency*,
 * which the frequency literature (Schoenfeld et al. 2016 Sports Med;
 * Schoenfeld, Grgic & Krieger 2019; Grgic et al. 2019) supports: 2x/week per
 * muscle beats 1x/week, while volume-equated higher frequencies add little.
 *
 * Every tier below keeps that ~2x/week floor — they only change how the week
 * is arranged to get there:
 *
 *   <=3 days -> full-body rotation (a split would push muscles to 1x/week)
 *   4-5 days -> upper/lower alternation (each half twice a week)
 *   6-7 days -> push/pull/legs cycled x2 (pattern sessions, still 2x/week)
 *
 * The old round-robin gave a muscle trained on one day exactly 1x/week
 * regardless of the plan's length; a naive one-part-per-week bro split at
 * 6-7 days would do the same, so neither is acceptable at any tier.
 */
class SplitStrategyTest {

    private val allParts = listOf(
        "Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes",
        "Biceps", "Triceps", "Abs", "Shoulders"
    )

    private fun generate(parts: List<String>, days: List<String>) =
        WorkoutTemplates.generateAssistedProgram(
            bodyParts = parts,
            selectedDays = days,
            intensity = "Medium",
            workoutGoal = "goal_bodybuilding"
        )

    /** The broad body parts a day works, read back from the stored rows. */
    private fun partsOfDay(program: WorkoutTemplates.GeneratedProgram, day: String): Set<String> =
        program.exercises.filter { it.dayOfWeek == day }
            .mapNotNull { WorkoutTemplates.getTopicForMuscle(it.muscleGroup).takeIf { t -> t != "Other" } }
            .toSet()

    /** Broad topics the six major muscles resolve to (e.g. Quadriceps -> Legs). */
    private val majorTopics = setOf("Chest", "Back", "Legs")

    private val allTopics = allParts.map { WorkoutTemplates.getTopicForMuscle(it) }.toSet()

    // ---------- Full-body tier (<=3 days) ----------

    @Test
    fun twoDaysTrainTheMajorsTwiceAWeek() {
        val program = generate(allParts, listOf("MONDAY", "THURSDAY"))
        listOf("MONDAY", "THURSDAY").forEach { day ->
            val parts = partsOfDay(program, day)
            // Each full-body day works most of the pool, majors included.
            assertTrue("expected a full-body day on $day, got $parts", parts.size >= 5)
            assertTrue(
                "the major topics $majorTopics must all be trained on $day, got $parts",
                parts.containsAll(majorTopics)
            )
        }
        // => every major muscle's weekly frequency is 2x — the evidence-backed floor.
    }

    @Test
    fun threeDaysAlsoUseFullBodyRotation() {
        val program = generate(allParts, listOf("MONDAY", "WEDNESDAY", "FRIDAY"))
        listOf("MONDAY", "WEDNESDAY", "FRIDAY").forEach { day ->
            val parts = partsOfDay(program, day)
            assertTrue(
                "expected majors on $day, got $parts",
                parts.containsAll(majorTopics)
            )
        }
    }

    @Test
    fun shortWeekStillEmitsTheFullBodyInsight() {
        val program = generate(allParts, listOf("MONDAY", "THURSDAY"))
        assertTrue(
            "a <=3-day build should explain the full-body frequency strategy",
            program.scienceInsights.any { it.descriptionKey == "insight_fullbody_desc" }
        )
    }

    @Test
    fun shortWeekStillTrainsEveryPartSomewhere() {
        val program = generate(allParts, listOf("MONDAY", "THURSDAY"))
        val trained = partsOfDay(program, "MONDAY") + partsOfDay(program, "THURSDAY")
        assertEquals(
            "rotation must still cover all picked parts, was $trained",
            allTopics,
            trained
        )
    }

    // ---------- Upper/lower tier (4-5 days) ----------

    @Test
    fun fourDaysAlternateUpperAndLowerHalves() {
        val days = listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY")
        val program = generate(allParts, days)
        val dayParts = days.associateWith { partsOfDay(program, it) }

        val lowerDays = dayParts.values.count { "Legs" in it }
        val upperDays = dayParts.values.count { "Legs" !in it }
        // A 4-day build alternates the two halves: two lower, two upper sessions.
        assertEquals("expected 2 lower days, got $dayParts", 2, lowerDays)
        assertEquals("expected 2 upper days, got $dayParts", 2, upperDays)
        // Halves stay focused: every lower day is legs-only, every upper day
        // works only above the waist.
        val lowerSets = dayParts.values.filter { "Legs" in it }.toSet()
        assertTrue("lower days must be legs-only, got $lowerSets", lowerSets.all { it == setOf("Legs") })
        val upperSets = dayParts.values.filter { "Legs" !in it }.toSet()
        assertTrue(
            "upper days must not train legs, got $upperSets",
            upperSets.all { "Legs" !in it }
        )
    }

    @Test
    fun upperLowerWeekTrainsEveryPartTwice() {
        val days = listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY")
        val program = generate(allParts, days)
        allParts.forEach { part ->
            val count = program.exercises.count { it.muscleGroup == part || getTopicMatches(it.muscleGroup, part) }
            assertEquals(
                "$part should be trained on exactly 2 days (2x/week), was $count",
                2,
                program.exercises.filter {
                    it.muscleGroup == part || WorkoutTemplates.getTopicForMuscle(it.muscleGroup) ==
                        WorkoutTemplates.getTopicForMuscle(part)
                }.map { it.dayOfWeek }.distinct().size
            )
        }
    }

    @Test
    fun fiveDaysAddAMajorMuscleVolumeDay() {
        val days = listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        val program = generate(allParts, days)
        val dayParts = days.associateWith { partsOfDay(program, it) }
        // No picked day is left empty.
        assertTrue("every picked day must have work, got $dayParts", dayParts.values.all { it.isNotEmpty() })
        // The 5th slot tops up the majors instead of inventing a new split.
        val fifth = dayParts.getValue(days[4])
        assertTrue("5th day should carry major topics, got $fifth", fifth.containsAll(majorTopics))
    }

    @Test
    fun upperLowerWeekEmitsTheUpperLowerInsight() {
        val program = generate(allParts, listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"))
        assertTrue(
            "a 4-5 day build should explain the upper/lower strategy",
            program.scienceInsights.any { it.descriptionKey == "insight_upperlower_desc" }
        )
        assertTrue(
            "upper/lower weeks are not full-body rotations",
            program.scienceInsights.none { it.descriptionKey == "insight_fullbody_desc" }
        )
    }

    // ---------- Dedicated tier (6-7 days, cycled PPL) ----------

    @Test
    fun sixDaysCycleEveryPartTwiceAWeek() {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        val program = generate(allParts, days)
        // Every topic repeats on exactly 2 days — except "Arms", which spans
        // two pattern buckets (biceps rows land on pull days, triceps rows on
        // push days), so it is checked per precise muscle below.
        allTopics.filter { it != "Arms" }.forEach { topic ->
            val trainingDays = program.exercises.filter {
                WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == topic
            }.map { it.dayOfWeek }.distinct()
            assertEquals(
                "$topic must repeat on exactly 2 days at the 6-day cadence (2x/week), was $trainingDays",
                2,
                trainingDays.size
            )
        }
        listOf("Biceps", "Triceps").forEach { muscle ->
            val trainingDays = program.exercises.filter { it.muscleGroup == muscle }
                .map { it.dayOfWeek }.distinct()
            assertEquals(
                "$muscle must repeat on exactly 2 days at the 6-day cadence, was $trainingDays",
                2,
                trainingDays.size
            )
        }
        // All picked topics are still covered.
        val trained = days.flatMap { partsOfDay(program, it) }.toSet()
        assertEquals("all topics must be covered, was $trained", allTopics, trained)
    }

    @Test
    fun sixDaysPairIntoPushPullLegsSessions() {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        val program = generate(allParts, days)
        val dayParts = days.map { partsOfDay(program, it) }
        // The cycle repeats: day i and day i+3 work the same topics.
        (0..2).forEach { i ->
            assertEquals(
                "day ${days[i]} and ${days[i + 3]} should repeat the same pattern",
                dayParts[i],
                dayParts[i + 3]
            )
        }
        // And no day is empty.
        assertTrue("no cycle day may be empty", dayParts.all { it.isNotEmpty() })
    }

    @Test
    fun sevenDaysGiveCoreAnExtraSession() {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val program = generate(allParts, days)
        val coreDays = program.exercises.filter {
            WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == "Core"
        }.map { it.dayOfWeek }.distinct()
        assertEquals(
            "core should train 3x in a 7-day cycle (both legs days + the 7th), was $coreDays",
            3,
            coreDays.size
        )
    }

    @Test
    fun dedicatedWeekEmitsThePPLInsight() {
        val program = generate(allParts, listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"))
        assertTrue(
            "a 6-7 day build should explain the push/pull/legs cycle",
            program.scienceInsights.any { it.descriptionKey == "insight_ppl_desc" }
        )
        assertTrue(
            "dedicated weeks are not upper/lower alternations",
            program.scienceInsights.none { it.descriptionKey == "insight_upperlower_desc" }
        )
    }

    // ---------- Guard shared by every tier ----------

    @Test
    fun noTierLeavesAMajorBelowTwiceAWeekFrequency() {
        // The evidence-backed floor applies to the majors: on every cadence
        // each major topic trains on at least 2 days. On full-body weeks the
        // secondaries rotate by design and get indirect work from the daily
        // compound majors; on split weeks the sixDays test checks every part.
        listOf(
            listOf("MONDAY", "THURSDAY"),
            listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"),
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        ).forEach { days ->
            val program = generate(allParts, days)
            majorTopics.forEach { topic ->
                val distinctDays = program.exercises.filter {
                    WorkoutTemplates.getTopicForMuscle(it.muscleGroup) == topic
                }.map { it.dayOfWeek }.distinct().size
                assertTrue(
                    "$topic must train >=2x/week on a ${days.size}-day plan, was $distinctDays",
                    distinctDays >= 2
                )
            }
        }
    }

    private fun getTopicMatches(muscleGroup: String, part: String): Boolean =
        WorkoutTemplates.getTopicForMuscle(muscleGroup) == WorkoutTemplates.getTopicForMuscle(part)
}
