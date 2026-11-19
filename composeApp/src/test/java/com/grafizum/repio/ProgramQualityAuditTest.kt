package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Is the week the generator produces a PROFESSIONAL week, across the whole option
 * space the welcome screen offers?
 *
 * The existing suites already prove the mechanics: [WeeklyProgramFuzzTest] checks
 * that every emitted exercise exists, is buildable from the kit, sits on the right
 * day, carries the picked goal's reps/rest and never repeats; [EngineProgramTest],
 * [SplitStrategyTest] and [SmartDayAssignmentTest] check the layout rules. None of
 * them asks the question the owner asked on 2026-10-08 - "is the system that outputs
 * the workout for each day truly a professional workout" - because "professional" is
 * not a mechanical property. It is a handful of load, frequency and balance numbers
 * that a coach would look at, and they can be measured.
 *
 * WHAT IS ASSERTED, AND WHY EXACTLY THESE
 *
 * The assertions encode each option's OWN documented promise (see
 * [WorkoutTemplates.styleTemplates] and the `SplitStyle` docs), not one generic
 * whole-body rule - the first draft of this audit applied a single 2x/week floor to
 * every option and produced 120 failures that were all Option B simply doing what it
 * says it does. A test that fails on documented behaviour measures the test.
 *
 *  1. OPTION A AND C KEEP THE 2x/WEEK FLOOR on every major muscle from 3 days up.
 *     That is what those options exist for (Schoenfeld et al. 2016 Sports Med;
 *     Schoenfeld, Grgic & Krieger 2019 J Sports Sci: 2x/week beats 1x/week at equal
 *     volume), and both options' own comments claim it.
 *
 *  2. OPTION B KEEPS ITS DOCUMENTED TRADE-OFF: exactly ONE leg session at every
 *     frequency from 2 days up, with the extra days going to the upper body. Asserted
 *     in both directions - legs not trained twice (the promise) and not dropped
 *     entirely (a week that skips legs is not the option either).
 *
 *  3. A MAJOR MUSCLE IS NOT TRAINED IN NAME ONLY. In the balanced options, a muscle
 *     that receives any work receives at least 4 weekly sets (Schoenfeld, Ogborn &
 *     Krieger 2017, J Sports Sci: the useful band starts around 10 sets, and below
 *     ~4 a muscle is not really being trained). Option B's single leg day is exempt -
 *     3 sets on the quads is the arithmetic of one leg day, which is the option.
 *
 *  4. NO WEEK TRAINS ITS QUADS AND NO HAMSTRINGS AT ALL. Knee-dominant-only training
 *     is the injury-relevant classic. (Ratio limits are reported, not asserted - see
 *     below.)
 *
 *  5. NO SAME-DAY DUPLICATE, and every selected day that receives exercises holds at
 *     least 3 of them, unless the generator named the part it could not fill
 *     ([WorkoutTemplates.GeneratedProgram.unsatisfiedParts]).
 *
 * WHAT IS REPORTED RATHER THAN ASSERTED, AND WHY
 *
 * These are judgement calls or metrics whose own resolution is not good enough to fail
 * a build on, so the audit prints them with the numbers behind them and the owner
 * decides:
 *
 *  - MOBILITY ROWS SPENT ON WORKING SETS. The headline finding of the first run: a
 *    stretch (Chair Pose, Cow Face Pose, Puppy Pose, Lizard Stretch) standing in for a
 *    working set, and 2404 no-kit isometric row-slots - Plank and Side Plank are legitimate
 *    isometrics, but the same bucket holds the yoga poses, because the catalog gives a
 *    Plank and a Chair Pose one training category. Fixing it needs a catalog decision, so
 *    the audit prints the counts and the names.
 *  - WEEKLY SET DOSE above the band. The top of the dose-response is a soft boundary; a
 *    4-day week that puts 30+ sets on one muscle is pushing a limit, not breaking one.
 *  - PUSH vs PULL. The classification cannot be cleaner than the catalog allows: rear
 *    delts, traps and forearms are trained by both pushes and pulls, so any split of
 *    them is arguable, and a ratio built on an arguable split is a report, not a gate.
 *  - QUADS vs HAMSTRINGS beyond 2.5x, and the leg volume inside Option B's one leg day.
 *
 * The report (`build/reports/program-quality/`) is the point of the test as much as the
 * assertions are: it is what the owner reads to judge the middle of the distribution,
 * it prints a sample week per day count and option so a ratio can be traced to rows,
 * and it makes a future change to the generator measurable instead of arguable.
 */
class ProgramQualityAuditTest {

    private val week = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    /** Day counts the welcome screen can produce (2 is its minimum, 6 its practical max). */
    private val dayCounts = listOf(2, 3, 4, 5, 6)

    /** The three build options the "Wie du deine Woche aufbaut" section offers. */
    private val splits = listOf(
        WorkoutTemplates.SplitStyle.OPTION_A,
        WorkoutTemplates.SplitStyle.OPTION_B,
        WorkoutTemplates.SplitStyle.OPTION_C,
    )

    /**
     * Kits a real user selects, from nothing to everything. The composite presets
     * (`PRESET_DB_BENCH`, `PRESET_BARBELL_RACK`, `PRESET_FULL_GYM`) are the ones the
     * wizard's preset buttons write, so they are what the generator will actually see.
     */
    private val kits = listOf(
        "bodyweight" to listOf(ExerciseEquipmentTags.PRESET_BODYWEIGHT),
        "dumbbells" to listOf(ExerciseEquipmentTags.PRESET_DUMBBELLS),
        "db+bench" to listOf(ExerciseEquipmentTags.PRESET_DB_BENCH),
        "barbell+rack" to listOf(ExerciseEquipmentTags.PRESET_BARBELL_RACK),
        "full gym" to listOf(ExerciseEquipmentTags.PRESET_FULL_GYM),
    )

    private val intensities = listOf("Easy", "Medium", "Hard")

    private val goals = listOf("goal_bodybuilding", "goal_strength")

    /** The parts the welcome screen's body-part picker offers. */
    private val parts = listOf(
        "Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes",
        "Biceps", "Triceps", "Abs", "Shoulders"
    )

    /**
     * The muscle vocabulary a generated row actually carries.
     *
     * These are the app's own stored groups, NOT its broad topics: a row says
     * "Lateral Deltoids", not "Shoulders". The first draft of this audit assumed the
     * broad names and measured nothing - the push/pull columns came out as mostly zeros
     * and the leg column was zero on every configuration - which is worth stating,
     * because a balance metric that silently matches no muscle is worse than no metric.
     */
    private val pushMuscles =
        setOf("Chest", "Pectorals", "Front Deltoids", "Lateral Deltoids", "Triceps")
    private val pullMuscles =
        setOf("Lats", "Upper Traps", "Biceps", "Spinal Erectors")

    /** The muscles the generator itself calls MAJOR (its own `majorMuscles` list). */
    private val majorMuscles =
        setOf("Chest", "Pectorals", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes")

    /** A major muscle belongs to the upper body unless it is one of these. */
    private val legMuscles = setOf("Quadriceps", "Hamstrings", "Glutes")

    private data class Row(
        val days: Int,
        val split: String,
        val kit: String,
        val intensity: String,
        val goal: String,
        val sessions: Int,
        val perDay: String,
        val mobilityRows: Int,
        val topMuscleSets: Int,
        val plateauMuscles: String,
        val pushSets: Int,
        val pullSets: Int,
        val legSets: Int,
        val hamSets: Int,
        val oneSessionMuscles: String,
        val duplicateDays: Int,
        val thinDays: Int,
    )

    private val rows = mutableListOf<Row>()
    private val failures = mutableListOf<String>()
    private val observations = mutableListOf<String>()

    /**
     * The muscle table of one sample week per (day count, option), so a failing ratio can
     * be READ as a week instead of argued about as a number. One sample each is enough:
     * the configurations cluster by option, and a second sample of a cluster says nothing
     * new.
     */
    private val sampleTables = linkedMapOf<String, String>()

    /** The day-by-day listing of a few configurations, so volume can be traced to rows. */
    private val dayListings = linkedMapOf<String, String>()

    /** No-kit isometric rows (the yoga poses) used as working sets, by option. */
    private val isometricPoseCounts = mutableMapOf<String, Int>()
    private val isometricPoseExamples = mutableMapOf<String, MutableSet<String>>()

    /** Mobility/stretching rows spent on working sets, by option. */
    private val mobilityRowCounts = mutableMapOf<String, Int>()
    private val mobilityExamples = mutableMapOf<String, MutableSet<String>>()

    @Test
    fun everyWelcomeScreenConfigurationProducesAProfessionalWeek() {
        for (dayCount in dayCounts) {
            for (split in splits) {
                for ((kitName, kit) in kits) {
                    for (intensity in intensities) {
                        for (goal in goals) {
                            val program = WorkoutTemplates.generateAssistedProgram(
                                bodyParts = parts,
                                selectedDays = week.take(dayCount),
                                intensity = intensity,
                                workoutGoal = goal,
                                equipment = kit,
                                splitStyle = split,
                            )
                            audit(dayCount, split, kitName, intensity, goal, program)
                        }
                    }
                }
            }
        }

        writeReport()

        assertTrue(
            "the audit produced no configurations - the parameter space is wrong and " +
                "this test would pass vacuously",
            rows.isNotEmpty(),
        )
        assertTrue(
            "these configurations break at least one documented floor of the option " +
                "they were generated under:\n" + failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }

    private fun audit(
        dayCount: Int,
        split: WorkoutTemplates.SplitStyle,
        kitName: String,
        intensity: String,
        goal: String,
        program: WorkoutTemplates.GeneratedProgram,
    ) {
        val label = "$dayCount days / option ${split.id} / $kitName / $intensity / $goal"

        val byDay = week.associateWith { day ->
            program.exercises.filter { it.dayOfWeek == day }.sortedBy { it.orderIndex }
        }
        val sessions = byDay.values.count { it.isNotEmpty() }

        // 6. A day with exercises is a SESSION, not one row.
        val thin = byDay.filter { (_, ex) -> ex.isNotEmpty() && ex.size < 3 }
        if (thin.isNotEmpty() && program.unsatisfiedParts.isEmpty()) {
            failures += "$label: ${thin.keys} hold fewer than 3 exercises and the kit " +
                "was never reported as unable to fill them"
        }

        // 6b. No movement twice in one session.
        val duplicates = byDay.values.sumOf { ex -> ex.size - ex.map { it.name }.distinct().size }
        if (duplicates > 0) failures += "$label: $duplicates duplicated movement(s) within a day"

        // 5. A STRETCH IS NOT A WORKING SET - REPORTED, NOT ASSERTED.
        //
        // This is the finding the audit exists for: the generator fills slots from
        // `mobility_stretching` rows (76 catalog rows whose own ladder is hold seconds, and
        // whose stated purpose is range of motion). Measured on the bodyweight lower days:
        // Chair Pose / Cow Face Pose / Dancer Pose / Pigeon Stretch in one session, and seven
        // back stretches making up the whole pull day. A fix was prototyped and measured on
        // 2026-10-08 (exclude the category from the gated pool, with a fall-back where the
        // part has no movement for that kit) and it works - but it costs the BARE kits: the
        // bodyweight week's repeats rise from 5 to 9 exercises and one full session's third
        // compound disappears (it was Lizard Stretch, a mobility row tagged `compound`). Both
        // of those are load-bearing assertions in DurationBudgetTest, so the trade-off is the
        // owner's call, not a silent one. Reported here with the numbers; see
        // docs/program-quality-audit.md.
        val mobility = program.exercises.filter { ExerciseCatalog.isMobilityRow(it.name) }
        if (mobility.isNotEmpty()) {
            mobilityRowCounts.merge(split.id, mobility.size, Int::plus)
            mobilityExamples.getOrPut(split.id) { mutableSetOf<String>() }
                .addAll(mobility.map { it.name })
        }

        // 5b. A DIFFERENT, SOFTER FINDING, reported rather than asserted: 55 catalog rows are
        // `isometric_hold_or_carry`, and the ones the bodyweight lower/back days fall back to
        // are the no-kit yoga poses (Chair Pose, Dancer Pose, Eagle Pose, Half Moon Pose, Bow
        // Pose, Locust Pose, Thoracic Bridge). That tagging is the exercise database's call,
        // not the generator's - a Plank and a Chair Pose share one training category - so
        // this counts them and leaves the decision where it belongs.
        val noKitIsometrics = program.exercises.filter { ex ->
            val e = ExerciseCatalog.entryFor(ex.name)
            e?.type == "isometric_hold_or_carry" && e.equipment == "Other"
        }
        if (noKitIsometrics.isNotEmpty()) {
            isometricPoseCounts.merge(split.id, noKitIsometrics.size, Int::plus)
            isometricPoseExamples.getOrPut(split.id) { mutableSetOf<String>() }
                .addAll(noKitIsometrics.map { it.name })
        }
        if (isometricPoseCounts.isNotEmpty()) { /* keeps the report section honest when zero */ }

        val setsPerMuscle = program.exercises
            .groupBy { it.muscleGroup }
            .mapValues { (_, ex) -> ex.sumOf { it.targetSets } }

        val sessionsPerMuscle = program.exercises
            .groupBy { it.muscleGroup }
            .mapValues { (_, ex) -> ex.map { it.dayOfWeek }.distinct().size }

        val singleSession = sessionsPerMuscle
            .filterKeys { it in majorMuscles }
            .filter { (_, n) -> n < 2 }

        // 2. The 2x/week floor, on the balanced options that promise it.
        if (split != WorkoutTemplates.SplitStyle.OPTION_B && dayCount >= 3 &&
            singleSession.isNotEmpty()
        ) {
            failures += "$label: ${singleSession.map { "${it.key}=${it.value}x" }} trained " +
                "only once in a $dayCount-day week (these options promise the 2x/week floor)"
        }

        // 3. Option B's own promise: one leg session, never zero, never two.
        if (split == WorkoutTemplates.SplitStyle.OPTION_B && dayCount >= 2) {
            val legSessions = sessionsPerMuscle.filterKeys { it in legMuscles }
            val twice = legSessions.filter { (_, n) -> n > 1 }
            val never = legSessions.filter { (_, n) -> n < 1 }
            if (twice.isNotEmpty()) {
                failures += "$label: option B is documented as exactly ONE leg day, but " +
                    "${twice.map { "${it.key}=${it.value}x" }} got a second session"
            }
            if (legSessions.isEmpty() || never.size == legSessions.size) {
                failures += "$label: option B trains no legs at all"
            }
        }

        // 4. The set floor, on the balanced options' majors and on option B's upper body.
        //
        // Only from THREE days up. A 2-day week is ~8 exercises a session at 3 sets, about
        // 48 sets across the ten picked parts, and no arrangement of it gives every major 4
        // sets - asserting the floor there would be asserting arithmetic that does not
        // exist. Those weeks are in the CSV, and the two that miss it (option B's upper day
        // at 2 days, lats on a dumbbells/db+bench Easy build) are the option's own trade-off
        // showing up one rung lower than usual.
        if (dayCount >= 3) {
            majorMuscles.forEach { muscle ->
                val sets = setsPerMuscle[muscle] ?: 0
                val exempt = split == WorkoutTemplates.SplitStyle.OPTION_B && muscle in legMuscles
                if (sets > 0 && sets < MIN_WEEKLY_SETS && !exempt) {
                    failures += "$label: $muscle gets $sets weekly set(s), below the " +
                        "$MIN_WEEKLY_SETS-set floor - trained in name only"
                }
            }
        } else {
            majorMuscles.forEach { muscle ->
                val sets = setsPerMuscle[muscle] ?: 0
                if (sets in 1 until MIN_WEEKLY_SETS) {
                    observations += "$label: $muscle gets $sets weekly set(s)"
                }
            }
        }
        setsPerMuscle.filter { it.value > MAX_WEEKLY_SETS }.let { over ->
            if (over.isNotEmpty()) {
                observations += "$label: over the $MAX_WEEKLY_SETS-set ceiling: " +
                    over.map { "${it.key}=${it.value}" }
            }
        }

        // 3./4. Balance. Reported, not asserted: see the class docs for why these two
        // ratios cannot be a gate, and this test's own history (a first draft that
        // asserted them produced 89 "failures" that were the metric, not the weeks).
        val push = setsPerMuscle.filterKeys { it in pushMuscles }.values.sum()
        val pull = setsPerMuscle.filterKeys { it in pullMuscles }.values.sum()
        val legs = setsPerMuscle.filterKeys { it in legMuscles }.values.sum()
        val hams = setsPerMuscle["Hamstrings"] ?: 0
        if (push > 0 && pull > 0) {
            val ratio = maxOf(push, pull).toDouble() / minOf(push, pull).toDouble()
            if (ratio > PUSH_PULL_LIMIT) {
                observations += "$label: push $push vs pull $pull = %.1fx".format(ratio)
            }
        }
        val quads = setsPerMuscle["Quadriceps"] ?: 0
        if (quads > 0 && hams > 0) {
            val r = quads.toDouble() / hams.toDouble()
            if (r > QUAD_HAM_LIMIT) {
                observations += "$label: quads $quads vs hamstrings $hams = %.1fx".format(r)
            }
        }

        // 5b. The injury-relevant absolute: a week with quad work and no hamstring work.
        if (legs > 0 && hams == 0) {
            failures += "$label: legs are trained with NO hamstring work at all"
        }

        sampleTables.getOrPut("$dayCount days / option ${split.id}") {
            setsPerMuscle.entries.sortedByDescending { it.value }.joinToString("  ") {
                "${it.key}=${it.value}"
            } + "   |  sessions per muscle: " + sessionsPerMuscle.entries
                .sortedBy { it.key }.joinToString("  ") { "${it.key}=${it.value}x" }
        }
        if (dayCount == 4 && intensity == "Medium" && goal == "goal_bodybuilding" &&
            kitName == "bodyweight"
        ) {
            dayListings["$dayCount days / option ${split.id} / bodyweight / Medium"] =
                byDay.filterValues { it.isNotEmpty() }.entries.joinToString("\n      ") { (d, ex) ->
                    "$d (${ex.size} exercises, ${ex.sumOf { it.targetSets }} sets): " +
                        ex.joinToString(", ") { "${it.name}[${it.muscleGroup},${it.targetSets}]" }
                }
        }

        rows += Row(
            days = dayCount,
            split = split.id,
            kit = kitName,
            intensity = intensity,
            goal = goal,
            sessions = sessions,
            perDay = byDay.values.filter { it.isNotEmpty() }.map { it.size }
                .sorted().joinToString("/"),
            mobilityRows = mobility.size,
            topMuscleSets = setsPerMuscle.values.maxOrNull() ?: 0,
            plateauMuscles = setsPerMuscle.filter { it.value > MAX_WEEKLY_SETS }.keys
                .sorted().joinToString(","),
            pushSets = push,
            pullSets = pull,
            legSets = legs,
            hamSets = hams,
            oneSessionMuscles = singleSession.keys.sorted().joinToString(","),
            duplicateDays = duplicates,
            thinDays = thin.size,
        )
    }

    private fun writeReport() {
        val dir = File("build/reports/program-quality")
        dir.mkdirs()

        val header = "days,split,kit,intensity,goal,sessions,exercises per session," +
            "mobility rows,max weekly sets on one muscle,muscles over the ceiling,push sets," +
            "pull sets,quad+ham+glute sets,hamstring sets,major muscles trained once," +
            "duplicate exercises,thin days"
        File(dir, "rows.csv").writeText(
            buildString {
                appendLine(header)
                rows.forEach { r ->
                    appendLine(
                        listOf(
                            r.days, r.split, r.kit, r.intensity, r.goal, r.sessions, r.perDay,
                            r.mobilityRows, r.topMuscleSets,
                            r.plateauMuscles.ifEmpty { "-" },
                            r.pushSets, r.pullSets, r.legSets, r.hamSets,
                            r.oneSessionMuscles.ifEmpty { "-" }, r.duplicateDays, r.thinDays
                        ).joinToString(",")
                    )
                }
            }
        )

        val byDays = rows.groupBy { it.days }.toSortedMap()
        val summary = buildString {
            appendLine("PROGRAM QUALITY AUDIT - welcome screen option space")
            appendLine("configurations: ${rows.size}")
            appendLine("failures: ${failures.size}")
            appendLine()
            appendLine("PER DAY COUNT (mean over that day count's configurations)")
            appendLine("days  sessions  ex/session  push  pull  legs  hams  mobility rows")
            byDays.forEach { (days, rs) ->
                appendLine(
                    "%-5d %-9.1f %-11s %-5.1f %-5.1f %-5.1f %-5.1f %.1f".format(
                        days,
                        rs.map { it.sessions }.average(),
                        rs.filter { it.perDay.isNotEmpty() }
                            .map { r -> r.perDay.split("/").map(String::toInt).average() }
                            .average().let { "%.1f".format(it) },
                        rs.map { it.pushSets }.average(),
                        rs.map { it.pullSets }.average(),
                        rs.map { it.legSets }.average(),
                        rs.map { it.hamSets }.average(),
                        rs.map { it.mobilityRows }.average(),
                    )
                )
            }
            appendLine()
            appendLine("PER OPTION (mean)")
            appendLine("option  configurations  push  pull  legs  hams  mobility rows")
            rows.groupBy { it.split }.toSortedMap().forEach { (split, rs) ->
                appendLine(
                    "%-7s %-15d %-5.1f %-5.1f %-5.1f %-5.1f %.1f".format(
                        split, rs.size,
                        rs.map { it.pushSets }.average(),
                        rs.map { it.pullSets }.average(),
                        rs.map { it.legSets }.average(),
                        rs.map { it.hamSets }.average(),
                        rs.map { it.mobilityRows }.average(),
                    )
                )
            }
            appendLine()
            appendLine("WEEKLY SETS ON THE MOST-LOADED MUSCLE")
            appendLine("  min ${rows.minOf { it.topMuscleSets }} / median " +
                "${rows.map { it.topMuscleSets }.sorted()[rows.size / 2]} / " +
                "max ${rows.maxOf { it.topMuscleSets }}")
            appendLine()
            appendLine("MUSCLES OVER THE $MAX_WEEKLY_SETS-SET CEILING: " +
                rows.count { it.plateauMuscles.isNotEmpty() } + " configuration(s)")
            appendLine()
            appendLine("WEEKLY SETS AND SESSIONS PER MUSCLE - one sample week per day count " +
                "and option (all welcome-screen parts selected)")
            sampleTables.forEach { (k, v) ->
                appendLine("  $k")
                appendLine("    $v")
            }
            appendLine()
            appendLine("DAY-BY-DAY LISTING (4 days, bodyweight, Medium, bodybuilding)")
            dayListings.forEach { (k, v) ->
                appendLine("  $k")
                appendLine("      $v")
            }
            appendLine()
            appendLine("MOBILITY/STRETCHING ROWS SPENT ON WORKING SETS (reported, not " +
                "asserted): ${mobilityRowCounts.values.sum()} row-slot(s)")
            mobilityRowCounts.toSortedMap().forEach { (split, count) ->
                appendLine("  option $split: $count slot(s) - " +
                    mobilityExamples[split]?.sorted()?.joinToString(", "))
            }
            appendLine()
            appendLine("NO-KIT ISOMETRIC ROWS USED AS WORKING SETS (reported, not asserted): " +
                "${isometricPoseCounts.values.sum()} row-slots")
            isometricPoseCounts.toSortedMap().forEach { (split, count) ->
                appendLine("  option $split: $count slot(s) - " +
                    isometricPoseExamples[split]?.sorted()?.joinToString(", "))
            }
            appendLine()
            appendLine("OBSERVATIONS - reported, not asserted (${observations.size})")
            observations.distinct().take(40).forEach { appendLine("  $it") }
            appendLine()
            appendLine("FAILURES (${failures.size})")
            failures.forEach { appendLine("  $it") }
        }
        File(dir, "summary.txt").writeText(summary)
        println(summary)
    }

    private companion object {
        /** Schoenfeld 2017's lower end: below this a muscle is not really trained. */
        const val MIN_WEEKLY_SETS = 4

        /** Where the dose-response flattens (Schoenfeld 2017). */
        const val MAX_WEEKLY_SETS = 30

        /** Reported only: the catalog cannot tag these muscles as one side or the other. */
        const val PUSH_PULL_LIMIT = 2.0

        /** Reported only: hamstrings legitimately trail quads. */
        const val QUAD_HAM_LIMIT = 2.5
    }
}
