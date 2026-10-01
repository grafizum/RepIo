package com.grafizum.repio

import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.data.WorkoutTemplates.SplitStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every option, every day count, every way of picking the days.
 *
 * The style picker added a second axis to the week (what each day IS, on top of
 * how many days there are), and a layout only exists in the context of the days
 * the user actually ticked: the validator treats Monday as following Sunday, so
 * the same sequence can be clean on Mon-Fri and broken across a weekend. A
 * style that only validates on the tidy day pattern is a style that breaks for
 * whoever picks Sunday and Monday.
 */
class SplitStyleLayoutTest {

    private val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    private val majorGroups = listOf("Chest", "Back", "Shoulders", "Legs")

    /** Every subset of [size] days, so no day pattern is assumed. */    private fun dayPicks(size: Int): List<List<String>> {
        val out = mutableListOf<List<String>>()
        fun walk(start: Int, chosen: MutableList<String>) {
            if (chosen.size == size) {
                out.add(chosen.toList())
                return
            }
            for (i in start until week.size) {
                chosen.add(week[i])
                walk(i + 1, chosen)
                chosen.removeAt(chosen.lastIndex)
            }
        }
        walk(0, mutableListOf())
        return out
    }

    @Test
    fun everyStyleCoversEveryDayCount() {
        for (style in SplitStyle.entries) {
            val templates = WorkoutTemplates.splitTemplatesFor(style)
            for (frequency in 1..7) {
                assertNotNull(
                    "${style.id} has no week for $frequency day(s)/week",
                    templates[frequency]
                )
                assertEquals(
                    "${style.id}@$frequency declares a different session count",
                    frequency,
                    templates[frequency]!!.sequence.size
                )
            }
        }
    }

    @Test
    fun everyStyleWeekIsValidForEveryDayPick() {
        for (style in SplitStyle.entries) {
            for (frequency in 1..7) {
                val sequence = WorkoutTemplates.splitTemplatesFor(style)[frequency]!!.sequence
                dayPicks(frequency).forEach { days ->
                    val topicsByDay = days.mapIndexed { i, day -> day to sequence[i] }.toMap()
                    val failures = WorkoutTemplates.validatePlan(topicsByDay, frequency)
                    assertTrue(
                        "${style.id}@$frequency on ${days.joinToString(",")} is invalid: $failures",
                        failures.isEmpty()
                    )
                }
            }
        }
    }

    @Test
    fun theWeeksMatchTheSpecifiedTable() {
        // The owner's table, row by row, written out literally: a future edit to
        // a layout cannot quietly turn the picker into something else.
        assertEquals(
            listOf("full_body", "full_body"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_A, 2)
        )
        assertEquals(
            listOf("full_body", "full_body", "full_body"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_A, 3)
        )
        assertEquals(
            listOf("upper", "lower", "upper", "lower"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_A, 4)
        )
        assertEquals(
            listOf("push", "pull", "legs", "upper", "lower"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_A, 5)
        )
        assertEquals(
            listOf("push", "pull", "legs", "push", "pull", "legs"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_A, 6)
        )
        assertEquals(
            listOf("upper", "lower"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_B, 2)
        )
        assertEquals(
            listOf("upper", "lower", "upper"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_B, 3)
        )
        assertEquals(
            listOf("push", "pull", "legs", "upper"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_B, 4)
        )
        assertEquals(
            listOf("push", "pull", "legs", "upper", "arms_core"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_B, 5)
        )
        assertEquals(
            listOf("push", "pull", "legs", "upper", "arms_core", "weak_point"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_B, 6)
        )
        assertEquals(
            listOf("full_body", "push", "pull", "legs"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_C, 4)
        )
        assertEquals(
            listOf("upper", "lower", "push", "pull", "legs"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_C, 5)
        )
        assertEquals(
            listOf("upper", "lower", "push", "pull", "legs", "full_body"),
            WorkoutTemplates.dayLabelIds(SplitStyle.OPTION_C, 6)
        )
    }

    @Test
    fun optionBGivesTheLegsExactlyOneDayAndNoSinglePartDay() {
        // The option's whole promise, and the reason it exists: the extra days
        // go to the upper body, never to a second leg session. One day a week is
        // the only week with nothing to spare, so the promise starts at two.
        for (frequency in 2..7) {
            assertEquals(
                "OPTION_B@$frequency must keep exactly one leg day",
                1,
                WorkoutTemplates.legSessionsPerWeek(SplitStyle.OPTION_B, frequency)
            )
        }
        // And it builds what it claims: exactly one session touches the legs,
        // and it is a dedicated leg day.
        val five = WorkoutTemplates.splitTemplateFor(5, SplitStyle.OPTION_B)!!.sequence
        assertEquals(1, five.count { "Legs" in it })
        assertEquals(1, five.count { "Legs" in it && it.count { topic -> topic in majorGroups } == 1 })
    }

    @Test
    fun noOptionHasAnUpperBodyDayThatIsOnePartAndNothingElse() {
        // The owner's revision: a whole day of only shoulders, or only arms, is
        // not a mix. No session collapses into a single upper-body muscle, so
        // the week stops reading as a bodybuilding body-part rotation.
        //
        // A legs-only day is the one exception and it is the point: a dedicated
        // leg day is what "one leg day" means, and every option's lower session
        // is deliberately that. A pull day is back AND arms, so it is a mix
        // even though it works one major group.
        for (style in SplitStyle.entries) {
            for (frequency in 1..7) {
                val lonePart = WorkoutTemplates.splitTemplatesFor(style)[frequency]!!
                    .sequence
                    .filter { topics -> topics.size == 1 && topics.single() != "Legs" }
                assertTrue(
                    "${style.id}@$frequency trains one upper-body part alone on $lonePart",
                    lonePart.isEmpty()
                )
            }
        }
    }

    @Test
    fun optionATrainsEveryMajorMuscleTheSameNumberOfTimes() {
        // The reason OPTION_A is the default: at the day counts where a person
        // can train a muscle twice, every major group gets trained twice. This
        // is the claim its label makes, so it is asserted rather than assumed.
        val major = listOf("Chest", "Back", "Shoulders", "Legs")
        for (frequency in 4..6) {
            val sequence = WorkoutTemplates.splitTemplatesFor(SplitStyle.OPTION_A)[frequency]!!.sequence
            val counts = major.associateWith { topic ->
                sequence.count { topics -> topic in topics }
            }
            assertEquals(
                "OPTION_A@$frequency trains the major groups unevenly: $counts",
                1,
                counts.values.distinct().size
            )
        }
    }

    @Test
    fun everyStyleHasOneDayLabelPerDay() {
        // The picker shows the week as a run of days directly under the day
        // picker, so a label count that disagreed with the day count would be a
        // line the user is being asked to count and that does not add up.
        val known = setOf(
            WorkoutTemplates.SESSION_FULL_BODY, WorkoutTemplates.SESSION_UPPER,
            WorkoutTemplates.SESSION_PUSH, WorkoutTemplates.SESSION_PULL,
            WorkoutTemplates.SESSION_LOWER, WorkoutTemplates.SESSION_LEGS,
            WorkoutTemplates.SESSION_ARMS, WorkoutTemplates.SESSION_ARMS_CORE,
            WorkoutTemplates.SESSION_WEAK_POINT
        )
        for (style in SplitStyle.entries) {
            for (frequency in 1..7) {
                val labels = WorkoutTemplates.dayLabelIds(style, frequency)
                assertEquals(
                    "${style.id}@$frequency shows ${labels.size} day labels for $frequency days",
                    frequency,
                    labels.size
                )
                assertTrue(
                    "${style.id}@$frequency has a day with no name: $labels",
                    labels.all { it in known }
                )
            }
        }
    }

    @Test
    fun theDaysAreNamedForWhatTheyTrain() {
        // The labels replaced a row of per-muscle counts ("Chest 2x, Back 2x,
        // ...") that said what the week achieves but not what the days ARE, so
        // each one has to follow from the topics its session trains.
        assertEquals("upper", WorkoutTemplates.sessionLabelId(listOf("Chest", "Back", "Shoulders", "Arms", "Core")))
        assertEquals("push", WorkoutTemplates.sessionLabelId(listOf("Chest", "Shoulders", "Arms")))
        assertEquals("pull", WorkoutTemplates.sessionLabelId(listOf("Back", "Arms")))
        assertEquals("lower", WorkoutTemplates.sessionLabelId(listOf("Legs")))
        assertEquals("legs", WorkoutTemplates.sessionLabelId(listOf("Legs", "Core")))
        assertEquals("full_body", WorkoutTemplates.sessionLabelId(listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")))
        assertEquals("arms", WorkoutTemplates.sessionLabelId(listOf("Arms")))
        assertEquals("arms_core", WorkoutTemplates.sessionLabelId(listOf("Core", "Arms")))
    }

    @Test
    fun anUnknownStoredStyleFallsBackToTheDefault() {
        assertEquals(SplitStyle.DEFAULT, SplitStyle.fromId(null))
        assertEquals(SplitStyle.DEFAULT, SplitStyle.fromId(""))
        assertEquals(SplitStyle.DEFAULT, SplitStyle.fromId("something-removed"))
        // The ids of the styles these replaced are gone with them; an install
        // that stored one lands on the recommended option rather than on
        // nothing.
        assertEquals(SplitStyle.DEFAULT, SplitStyle.fromId("balanced"))
        assertEquals(SplitStyle.DEFAULT, SplitStyle.fromId("upper_lower"))
        assertEquals(SplitStyle.DEFAULT, SplitStyle.fromId("push_pull_legs"))
        assertEquals(SplitStyle.OPTION_B, SplitStyle.fromId("b"))
        assertEquals(SplitStyle.OPTION_C, SplitStyle.fromId("C"))
    }
}
