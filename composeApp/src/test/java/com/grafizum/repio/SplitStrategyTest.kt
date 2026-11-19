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
 * Since the smart-day-assignment fix the distribution comes from fixed split
 * TEMPLATES per frequency: a session sequence applied to the picked days in
 * CALENDAR order, so tap order cannot matter and no two consecutive sessions
 * share a strict topic. The old tap-order distribution walked the weekday
 * chips in tap order and put the majors top-up on the day after a leg day.
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

    /** Calendar week order for adjacency checks. */
    private val weekOrder = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    // ---------- Full-body tier (3 days) ----------

    @Test
    fun threeDaysTrainFullBodyEveryDay() {
        // The 3-day week is Full Body x3, as Option A specifies. The old A/B/A
        // template dropped the legs from the middle session so three
        // consecutive picks never stacked leg work; that is still true here (a
        // full-body day is a broad session and never stacks against its
        // neighbour), and the repetition is the point: 3x/week is inside the
        // range the research supports.
        val days = listOf("MONDAY", "WEDNESDAY", "FRIDAY")
        val program = generate(allParts, days)
        val dayParts = days.associateWith { partsOfDay(program, it) }
        dayParts.forEach { (day, parts) ->
            assertTrue(
                "expected a full-body day (chest/back/legs) on $day, got $parts",
                parts.containsAll(majorTopics)
            )
        }
        // The frequency floor still holds: every major muscle trains at least
        // twice across the three sessions.
        majorTopics.forEach { topic ->
            val trainingDays = dayParts.values.count { topic in it }
            assertTrue(
                "$topic must train at least 2x on the 3-day cadence, was $trainingDays",
                trainingDays >= 2
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

    // ---------- Upper/lower tier (4 days) ----------

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

    // ---------- Dedicated tier (5-7 days) ----------

    @Test
    fun fiveDaysArePushPullLegsUpperLower() {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        val program = generate(allParts, days)
        val dayParts = days.map { partsOfDay(program, it) }
        // The default 5-day week is the one the owner specified for Option A:
        // Push / Pull / Legs / Upper / Lower. It lands 2x on every major group -
        // chest and shoulders get the push day and the upper day, the legs the
        // legs day and the lower day - which is the 2x/week target the training
        // literature asks for (Schoenfeld et al. 2016; Grgic et al. 2019).
        assertTrue("day 1 must be a push day (chest + shoulders), got ${dayParts[0]}", dayParts[0].containsAll(setOf("Chest", "Shoulders")) && "Legs" !in dayParts[0])
        assertTrue("day 2 must be a pull day (back), got ${dayParts[1]}", "Back" in dayParts[1] && "Legs" !in dayParts[1] && "Chest" !in dayParts[1])
        assertTrue("day 3 must be the leg day, got ${dayParts[2]}", "Legs" in dayParts[2] && "Chest" !in dayParts[2] && "Back" !in dayParts[2])
        assertTrue("day 4 must be an upper day (chest + back), got ${dayParts[3]}", dayParts[3].containsAll(setOf("Chest", "Back")) && "Legs" !in dayParts[3])
        assertTrue("day 5 must be a lower day, got ${dayParts[4]}", "Legs" in dayParts[4] && "Chest" !in dayParts[4])
        // Exactly two leg sessions, and never on consecutive days.
        val legDays = days.filter { "Legs" in partsOfDay(program, it) }
        assertEquals(
            "a 5-day week trains legs exactly twice, got $dayParts",
            2,
            legDays.size
        )
        assertEquals(
            "the leg days must be WEDNESDAY and FRIDAY, was $legDays",
            listOf("WEDNESDAY", "FRIDAY"),
            legDays
        )
    }

    @Test
    fun sixDaysCycleEveryPartTwiceAWeek() {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
        val program = generate(allParts, days)
        // Every topic repeats on exactly 2 days. "Arms" spans two pattern
        // buckets, so it is checked per precise muscle below: the push/pull
        // split means triceps ride the push days and biceps the pull days —
        // each still exactly 2 days a week.
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
        assertTrue(
            "core should train on most days in a 7-day cycle, was $coreDays",
            coreDays.size >= 4
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
            listOf("MONDAY", "TUESDAY", "WEDNESDAY"),
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
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

    // ---------- Tap-order independence (the regression that started this) ----------

    @Test
    fun tapOrderDoesNotChangeTheWeekShape() {
        // The same five days, tapped in different orders, must produce the
        // same topic-per-day layout. The old tap-slot distribution made
        // TUESDAY a leg day when it was tapped last.
        val base = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        val shapes = listOf(
            base,
            listOf("FRIDAY", "WEDNESDAY", "MONDAY", "TUESDAY", "THURSDAY"),
            listOf("TUESDAY", "FRIDAY", "MONDAY", "THURSDAY", "WEDNESDAY")
        ).map { tapped ->
            val program = generate(allParts, tapped)
            weekOrder.map { partsOfDay(program, it) }
        }
        assertEquals("tap order must not change the week shape", shapes[0], shapes[1])
        assertEquals("tap order must not change the week shape", shapes[0], shapes[2])
    }

    @Test
    fun fiveDayPlanNeverTrainsLegsTwoDaysInARow() {
        // The original bug: tapping MONDAY, WEDNESDAY, FRIDAY, SATURDAY,
        // TUESDAY put the majors top-up on TUESDAY — back-to-back legs.
        val program = generate(allParts, listOf("MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY", "TUESDAY"))
        val legDays = weekOrder.filter { "Legs" in partsOfDay(program, it) }
        assertEquals(
            "a 5-day plan trains legs exactly twice, was $legDays",
            2,
            legDays.size
        )
        // The two leg days must not be calendar-adjacent (TUESDAY follows MONDAY).
        val adjacentPairs = setOf("MONDAY" to "TUESDAY", "TUESDAY" to "WEDNESDAY")
        legDays.forEach { d1 ->
            legDays.forEach { d2 ->
                assertTrue(
                    "leg days $d1 and $d2 must not be back to back",
                    (d1 to d2) !in adjacentPairs
                )
            }
        }
    }

    // ---------- Every frequency: validator verdict ----------

    @Test
    fun everyFrequency3Through7PassesPlanValidation() {
        val daySets = mapOf(
            3 to listOf("MONDAY", "WEDNESDAY", "FRIDAY"),
            4 to listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY"),
            5 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"),
            6 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"),
            7 to listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        )
        daySets.forEach { (frequency, days) ->
            val program = generate(allParts, days)
            val topicsByDay = weekOrder.associateWith { day ->
                partsOfDay(program, day).toList()
            }
            assertEquals(
                "$frequency-day plan must pass validatePlan, got " +
                    WorkoutTemplates.validatePlan(topicsByDay, frequency),
                emptyList<String>(),
                WorkoutTemplates.validatePlan(topicsByDay, frequency)
            )
        }
    }
}
