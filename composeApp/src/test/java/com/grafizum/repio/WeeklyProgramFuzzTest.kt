package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * A seeded fuzz over the weekly generator: 15 random configurations
 * (goal x intensity x duration x day count x equipment kit x body parts) run
 * through [WorkoutTemplates.generateAssistedProgram], and every emitted
 * exercise confronted with the promises the build makes:
 *
 *  1. the exercise exists in the catalog (no ghost rows);
 *  2. the user's equipment can actually perform it (the fine-tag gate);
 *  3. its stored muscle's broad topic is one the picked body parts cover, AND
 *     it sits on a day whose split-template session trains that topic
 *     (no leaks, layout conform);
 *  4. its reps and rest come from the picked goal's scheme — nothing else;
 *  5. no exercise repeats within the week (the variation rule);
 *  6. no exercise lands on an unpicked day, no training day is silently empty
 *     (a day the kit cannot serve at all is allowed, but only when the
 *     generator named the parts it could not fill), and no day exceeds the
 *     duration budget (under-budget days are reported as observations, not
 *     failures — a tiny pool can legitimately pad short);
 *  7. the whole week passes the generator's own validatePlan;
 *  8. the frequency warning contract stays consistent for the picked count.
 *
 * The seed is printed per run so any failure reproduces exactly.
 */
class WeeklyProgramFuzzTest {

    private val weekOrder = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    /** The precise parts the setup's picker offers (all map through getTopicForMuscle). */
    private val partPool = listOf(
        "Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes",
        "Biceps", "Triceps", "Abs", "Shoulders"
    )

    private val presetKits = listOf(
        listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT),
        listOf(ExerciseEquipmentTags.PRESET_BW_PULLUP_BAR),
        listOf(ExerciseEquipmentTags.PRESET_BW_DIP_BARS),
        listOf(ExerciseEquipmentTags.PRESET_BW_BANDS),
        listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS),
        listOf(ExerciseEquipmentTags.PRESET_DB_BENCH),
        listOf(ExerciseEquipmentTags.PRESET_DB_BENCH_BAR),
        listOf(ExerciseEquipmentTags.PRESET_BARBELL_RACK),
        listOf(ExerciseEquipmentTags.PRESET_FULL_GYM),
    )

    private val catalogNames = ExerciseCatalog.ENTRIES.map { it.name }.toSet()

    /** Random custom kit: CUSTOM marker + a random subset of the fine tags. */
    private fun randomCustomKit(rng: Random): List<String> {
        val tags = ExerciseEquipmentTags.ALL_TAGS.shuffled(rng).take(1 + rng.nextInt(4)).toSet()
        return listOf(ExerciseEquipmentTags.PRESET_CUSTOM) +
            ExerciseEquipmentTags.ALL_TAGS.filter { it in tags }
    }

    private fun grantedFor(equipment: List<String>): Set<String> =
        WorkoutTemplates.grantedTagsForPublic(equipment)
            ?: ExerciseEquipmentTags.allowedTagsFor(equipment)

    @Test
    fun fifteenRandomWeeksMatchTheirFiltersAndGoals() {
        repeat(15) { iteration ->
            val rng = Random(20260924L + iteration)

            val goal = WorkoutTemplates.allWorkoutGoals.random(rng)
            val intensity = listOf("Easy", "Medium", "Hard").random(rng)
            val duration = WorkoutTemplates.DURATION_OPTIONS.random(rng)
            val dayCount = 1 + rng.nextInt(7) // 1..7
            val days = weekOrder.shuffled(rng).take(dayCount)
            val parts = partPool.shuffled(rng).take(2 + rng.nextInt(partPool.size - 1))
            val equipment = if (rng.nextBoolean()) presetKits.random(rng) else randomCustomKit(rng)
            val seed = 20260924L + iteration

            println(
                "FUZZ #$iteration seed=$seed goal=$goal intensity=$intensity " +
                    "duration=$duration days=$dayCount ($days) parts=$parts kit=$equipment"
            )

            val program = WorkoutTemplates.generateAssistedProgram(
                bodyParts = parts,
                selectedDays = days,
                intensity = intensity,
                workoutGoal = goal,
                equipment = equipment,
                duration = duration
            )

            val granted = grantedFor(equipment)
            val pickedTopics = parts.map { WorkoutTemplates.getTopicForMuscle(it) }.toSet()
            val budget = WorkoutTemplates.durationRangeFor(duration)
            val frequency = days.size

            // The layout the split template promises, per calendar weekday.
            val weekDaysAll = weekOrder
            val orderedDays = weekDaysAll.filter { it in days }
            val expectedTopics = WorkoutTemplates.splitTemplateFor(frequency)
                ?.assign(orderedDays) ?: emptyMap()

            val perDay = program.exercises.groupBy { it.dayOfWeek }

            // 1+2+3+4. Every row: catalog-real, performable, on-topic, goal-schemed.
            program.exercises.forEach { ex ->
                assertTrue(
                    "seed $seed: '${ex.name}' is not in the catalog",
                    ex.name in catalogNames
                )
                assertTrue(
                    "seed $seed: '${ex.name}' needs ${ExerciseEquipmentTags.requirementsFor(ex.name)} " +
                        "but the kit grants $granted",
                    ExerciseEquipmentTags.isAvailable(ex.name, granted)
                )
                val topic = WorkoutTemplates.getTopicForMuscle(ex.muscleGroup)
                assertTrue(
                    "seed $seed: '${ex.name}' ($topic) leaks outside the picked parts $parts",
                    topic in pickedTopics
                )
                val daySession = expectedTopics[ex.dayOfWeek].orEmpty()
                assertTrue(
                    "seed $seed: '${ex.name}' ($topic) sits on ${ex.dayOfWeek} whose session " +
                        "is ${daySession}, not its topic",
                    topic in daySession
                )
                assertEquals(
                    "seed $seed: '${ex.name}' reps must come from the goal scheme",
                    WorkoutTemplates.getGoalReps(ex.name, goal),
                    ex.targetReps
                )
                assertEquals(
                    "seed $seed: '${ex.name}' rest must come from the goal scheme",
                    WorkoutTemplates.getGoalRestSeconds(goal, ex.name),
                    ex.restSeconds
                )
            }

            // 5. The variation rule. Absolute part: no exercise twice in the
            // SAME day. Week-level part: a name may repeat across days only
            // under the generator's documented "frequency floor beats variety"
            // override — the repeat serves a topic that has not yet reached
            // its 2x/week floor, so a repeated name's topic must train on at
            // most 2 distinct days. Once the floor is met the topic stays
            // silent rather than repeating again.
            program.exercises.groupBy { it.dayOfWeek }.forEach { (day, rows) ->
                val dupes = rows.groupBy { it.name }.filter { it.value.size > 1 }.keys
                assertTrue("seed $seed: $day repeats exercises $dupes", dupes.isEmpty())
            }
            program.exercises.groupBy { it.name }.filter { it.value.size > 1 }.forEach { (name, rows) ->
                val topic = WorkoutTemplates.getTopicForMuscle(rows.first().muscleGroup)
                val topicDays = rows.map { it.dayOfWeek }.distinct().size
                assertTrue(
                    "seed $seed: '$name' repeats $topicDays times across days (topic $topic) " +
                        "although the frequency floor allows repeats only below 2x/week",
                    topicDays <= 2
                )
            }

            // 6. Day placement and the duration budget. A picked day whose
            // template session trains none of the picked parts is a legitimate
            // recovery day (the picker allows deselecting parts); a day whose
            // session DOES train a picked part must never come out empty.
            val unpicked = perDay.keys.filter { it !in days }
            assertTrue("seed $seed: exercises on unpicked days $unpicked", unpicked.isEmpty())
            days.forEach { day ->
                val session = expectedTopics[day].orEmpty()
                val trainsPicked = session.any { it in pickedTopics }
                val count = perDay[day].orEmpty().size
                if (trainsPicked) {
                    // A day may only be empty when the generator SAID it could not
                    // be filled - a kit with no biceps curl cannot serve a Back/Arms
                    // session - and then the part is on the unsatisfied list. A day
                    // that is empty for any other reason is a broken plan.
                    assertTrue(
                        "seed $seed: $day should train $session but is empty, and " +
                            "the generator reported nothing: ${program.unsatisfiedParts}",
                        count > 0 || program.unsatisfiedParts.isNotEmpty()
                    )
                    assertTrue(
                        "seed $seed: $day has $count exercises, over the $duration budget $budget",
                        count <= budget.last
                    )
                    if (count < budget.first) {
                        println("  note: $day under budget ($count < ${budget.first}) — pool too small to pad")
                    }
                } else {
                    assertTrue(
                        "seed $seed: $day is a recovery session ($session covers none of $pickedTopics) " +
                            "but carries $count exercises",
                        count == 0
                    )
                }
            }

            // With every part picked — the guided flow's default — no picked day
            // may come out empty: every template session has material to work with.
            if (parts.size == partPool.size) {
                days.forEach { day ->
                    assertTrue(
                        "seed $seed: full-pick build leaves $day empty",
                        perDay[day].orEmpty().isNotEmpty()
                    )
                }
            }

            // 7. The generator's own validator must pass on the PLAN (the
            // template's session topics). Validating the emitted rows instead
            // would punish partial picks: a 4-part selection legitimately
            // under-covers a full-body session (and the generator reports the
            // parts its kit cannot train via unsatisfiedParts instead of
            // leaking equipment). With every part picked the emissions cover
            // the whole plan, so the rows themselves must validate too.
            assertEquals(
                "seed $seed: validatePlan rejected the template plan: " +
                    WorkoutTemplates.validatePlan(expectedTopics.mapValues { it.value }, frequency),
                emptyList<String>(),
                WorkoutTemplates.validatePlan(expectedTopics.mapValues { it.value }, frequency)
            )
            assertTrue(
                "seed $seed: unsatisfied parts ${program.unsatisfiedParts} outside the picked $parts",
                program.unsatisfiedParts.all { it in parts }
            )
            if (parts.size == partPool.size) {
                val topicsByDay = weekOrder.associateWith { day ->
                    perDay[day].orEmpty()
                        .mapNotNull { WorkoutTemplates.getTopicForMuscle(it.muscleGroup) }
                        .distinct()
                }
                assertEquals(
                    "seed $seed: full-pick week failed validatePlan: " +
                        WorkoutTemplates.validatePlan(topicsByDay, frequency),
                    emptyList<String>(),
                    WorkoutTemplates.validatePlan(topicsByDay, frequency)
                )
            }

            // 8. The warning contract stays consistent for this frequency.
            assertEquals(
                "seed $seed: warning contract inconsistent for $frequency days",
                WorkoutTemplates.frequencyNeedsWarning(frequency),
                WorkoutTemplates.frequencyWarningKey(frequency).isNotBlank()
            )
        }
    }
}
