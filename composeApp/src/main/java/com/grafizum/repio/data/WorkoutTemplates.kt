package com.grafizum.repio.data

import java.util.Calendar

data class DayTemplate(
    val dayOfWeek: String,
    val titleKey: String,
    val descKey: String,
    val isRestDay: Boolean
)

data class WorkoutTemplate(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val badgeKey: String,
    val daysCountKey: String,
    val daysCountArgs: Pair<Int, Int>, // (Workout Days, Rest Days)
    val dayTemplates: List<DayTemplate>,
    val exercises: List<RoutineExerciseEntity>
)

object WorkoutTemplates {

    // Equipment categories used to organize the exercise pool.
    const val EQUIPMENT_ALL = "All"
    const val EQUIPMENT_BODYWEIGHT = "Bodyweight"
    const val EQUIPMENT_DUMBBELL = "Dumbbell"
    const val EQUIPMENT_BARBELL = "Barbell"
    const val EQUIPMENT_GYM = "Gym" // Cables & Machines

    /** All equipment categories (excluding [EQUIPMENT_ALL]). */
    val allEquipmentCategories = listOf(
        EQUIPMENT_BODYWEIGHT,
        EQUIPMENT_DUMBBELL,
        EQUIPMENT_BARBELL,
        EQUIPMENT_GYM
    )

    data class ScienceInsight(
        val titleKey: String,
        val descriptionKey: String,
        val citation: String = ""
    )

    data class GeneratedProgram(
        val days: List<RoutineDayEntity>,
        val exercises: List<RoutineExerciseEntity>,
        val unsatisfiedParts: List<String>,
        val scienceInsights: List<ScienceInsight> = emptyList()
    )

    val allWorkoutGoals = listOf(
        "goal_bodybuilding",
        "goal_strength",
        "goal_calisthenics",
        "goal_functional",
        "goal_olympic",
        "goal_powerbuilding",
        "goal_crossfit",
        "goal_athletic"
    )

    /**
     * Week-length tiers for the program structure. Every tier keeps each muscle
     * at ~2x/week — the frequency floor the meta-analyses support (Schoenfeld
     * et al. 2016; Schoenfeld, Grgic & Krieger 2019) — they only change HOW the
     * week is arranged to get there:
     *  - <=3 days: full-body rotation (a split would push muscles to 1x/week).
     *  - 4-5 days: upper/lower alternation (focused sessions, ~2x/week each).
     *  - 6-7 days: dedicated push/pull/legs cycle (spreads the high weekly
     *    volume across movement patterns without dropping any muscle's
     *    frequency — a 1x/week body-part-per-week split at this cadence would
     *    undo the frequency benefit the same research established).
     */
    private const val FULL_BODY_MAX_DAYS = 3
    private const val UPPER_LOWER_MAX_DAYS = 5

    private const val STRATEGY_FULL_BODY = 0
    private const val STRATEGY_UPPER_LOWER = 1
    private const val STRATEGY_PPL = 2

    /**
     * Which push/pull/legs bucket a body part trains on the split tiers. The
     * setup picker sends precise parts; the broad aliases keep programmatic
     * callers (and the tests) working.
     */
    private fun splitBucketFor(part: String): String? = when (part) {
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Triceps", "Shoulders" -> "push"
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors", "Biceps", "Forearms", "Back", "Arms" -> "pull"
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Legs" -> "legs"
        "Abs", "Obliques", "Core" -> "core"
        else -> null
    }

    /**
     * Parts the upper/lower alternation assigns to the lower sessions (the
     * core parts rotate with upper so ab work opens each upper day).
     */
    private val LOWER_BODY_PARTS = setOf(
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Legs"
    )

    /**
     * The two independent knobs the build is parameterised by:
     *  - [workoutGoal] owns the SCHEME: rep range, rest, exercise selection.
     *  - [intensity] owns the EFFORT & VOLUME: RPE label, sets, exercises per
     *    muscle, and the suggested starting load.
     * Each axis is owned by exactly one control, so nothing double-dips.
     */
    fun generateAssistedProgram(
        bodyParts: List<String>, 
        selectedDays: List<String>,
        intensity: String,
        workoutGoal: String = "goal_bodybuilding",
        includedExercises: List<String> = emptyList(),
        excludedExercises: List<String> = emptyList(),
        minSets: Int = 0,
        maxSets: Int = 0,
        equipment: List<String> = emptyList()
    ): GeneratedProgram {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val routineDays = mutableListOf<RoutineDayEntity>()
        val routineExercises = mutableListOf<RoutineExerciseEntity>()
        val unsatisfiedParts = mutableListOf<String>()
        val insights = mutableListOf<ScienceInsight>()

        if (bodyParts.isEmpty() || selectedDays.isEmpty()) return GeneratedProgram(emptyList(), emptyList(), emptyList(), emptyList())

        // 1. VOLUME STRATEGY (frequency-driven distribution)
        val majorMuscles = listOf("Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes")
        val expandedBodyParts = bodyParts.toMutableList()

        // Distribute parts across selected days, strategy chosen by training
        // frequency (Schoenfeld et al. 2016 Sports Med; Schoenfeld, Grgic & Krieger
        // 2019 J Sports Sci; Grgic et al. 2019): a muscle trained 2x/week grows
        // more than 1x/week, while volume-equated higher frequencies add little —
        // so a split is only the better tool once the week has enough days for
        // each part to still recover and every day to stay focused.
        //  - 4+ days: body-part split (Mon chest, Tue arms, Wed legs, …). Each
        //    part's weekly volume lands in one fresh, concentrated session —
        //    higher per-set quality and more total volume per part.
        //  - 3- days: full-body rotation. The major muscles the user picked are
        //    trained on EVERY day (2-3x/week, the evidence-backed sweet spot)
        //    and the remaining parts rotate across days, because concentrating
        //    them into single sessions would mean every muscle is hit only 1x/week.
        // Distribute parts across selected days with the strategy the week's
        // length supports (Schoenfeld et al. 2016 Sports Med; Schoenfeld, Grgic &
        // Krieger 2019 J Sports Sci; Grgic et al. 2019): a muscle trained
        // 2x/week grows more than 1x/week, while volume-equated higher
        // frequencies add little — so every tier below is arranged to keep the
        // ~2x/week floor, and they differ only in how the week is structured:
        //  - <=3 days: full-body rotation. Concentrating parts into sessions
        //    would drop every muscle to 1x/week, so the major muscles the user
        //    picked are trained on EVERY day (2-3x/week each) and the rest
        //    rotate across days.
        //  - 4-5 days: upper/lower alternation. Enough days exist to give each
        //    half its own fresh, focused session and still hit it twice a week
        //    (Mon lower / Tue upper / Thu lower / Fri upper).
        //  - 6-7 days: dedicated push/pull/legs cycle. The high weekly volume an
        //    advanced cadence carries is spread across movement patterns while
        //    every muscle still repeats ~2x/week — a one-part-per-week
        //    arrangement would undo the frequency benefit the same research
        //    established.
        val partsPerDay = List(selectedDays.size) { mutableListOf<String>() }
        val strategy = when {
            selectedDays.size <= FULL_BODY_MAX_DAYS -> STRATEGY_FULL_BODY
            selectedDays.size <= UPPER_LOWER_MAX_DAYS -> STRATEGY_UPPER_LOWER
            else -> STRATEGY_PPL
        }
        when (strategy) {
            STRATEGY_UPPER_LOWER -> {
                // Alternation: all lower parts go to the first lower day, all
                // other parts to the first upper day; the next days repeat the
                // halves in pairs (Mon lower / Tue upper / Thu lower / Fri upper
                // for the 4-day case). Core parts ride with upper so ab work
                // still lands twice a week. A 5th training day becomes a major-
                // muscle volume day: the biggest muscles tolerate the highest
                // weekly frequency, so it tops them up to 3x/week instead of
                // leaving a picked day empty.
                expandedBodyParts.forEach { part ->
                    val slot = if (part in LOWER_BODY_PARTS) 0 else 1
                    partsPerDay[slot % selectedDays.size].add(part)
                }
                expandedBodyParts.forEach { part ->
                    val slot = if (part in LOWER_BODY_PARTS) 2 else 3
                    partsPerDay[slot % selectedDays.size].add(part)
                }
                if (selectedDays.size >= 5) {
                    partsPerDay[4].addAll(expandedBodyParts.filter { it in majorMuscles })
                }
            }
            STRATEGY_PPL -> {
                // Push/pull/legs CYCLED: every push part trains on both push
                // days (0 and 3), pull on 1 and 4, legs — plus core — on 2 and
                // 5. That keeps every muscle at 2x/week while the week's high
                // volume is spread across six focused, pattern-based sessions.
                // A 7th day takes the core parts as an extra session.
                expandedBodyParts
                    .map { part -> part to (splitBucketFor(part) ?: "push") }
                    .sortedWith(compareBy({ it.second }, { it.first }))
                    .forEach { (part, bucket) ->
                        val slotOffset = when (bucket) {
                            "pull" -> 1
                            "legs", "core" -> 2
                            else -> 0
                        }
                        partsPerDay[slotOffset % selectedDays.size].add(part)
                        partsPerDay[(slotOffset + 3) % selectedDays.size].add(part)
                        if (bucket == "core" && selectedDays.size == 7) {
                            partsPerDay[6].add(part)
                        }
                    }
            }
            else -> {
                val majors = expandedBodyParts.filter { it in majorMuscles }
                val secondaries = expandedBodyParts.filter { it !in majorMuscles }
                secondaries.forEachIndexed { index, part ->
                    partsPerDay[index % selectedDays.size].add(part)
                }
                // Majors go on every day; [sortedParts] below already orders them first.
                partsPerDay.forEach { it.addAll(majors) }
            }
        }

        // 2. VOLUME RECOMMENDATION
        // Difficulty is the ONLY control over volume: Easy 3, Medium 4, Hard 5 sets.
        // The goal decides the rep range and rest, never the set count.
        var setsPerExercise = when(intensity) {
            "Easy" -> 3
            "Hard" -> 5
            else -> 4
        }

        // The advanced "sets allocation" setting is an explicit override, so it
        // still wins over the difficulty default.
        if (minSets > 0) setsPerExercise = kotlin.math.max(setsPerExercise, minSets)
        if (maxSets > 0) setsPerExercise = kotlin.math.min(setsPerExercise, maxSets)
        setsPerExercise = setsPerExercise.coerceIn(1, 10)

        // 3. INSIGHT GENERATION
        insights.add(ScienceInsight(
            titleKey = "insight_volume_title",
            descriptionKey = when(intensity) {
                "Hard" -> "insight_volume_hard_desc"
                "Easy" -> "insight_volume_easy_desc"
                else -> "insight_volume_medium_desc"
            },
            citation = "Schoenfeld et al. (2017)"
        ))

        if (expandedBodyParts.size > selectedDays.size) {
            insights.add(ScienceInsight(
                titleKey = "insight_frequency_title",
                descriptionKey = "insight_frequency_desc",
                citation = "Dankel et al. (2017)"
            ))
        }

        // Short week: the full-body rotation is exactly why the muscles still get
        // their 2x/week frequency — say so, so the day titles listing several parts
        // read as deliberate rather than cluttered.
        if (strategy == STRATEGY_FULL_BODY) {
            insights.add(ScienceInsight(
                titleKey = "insight_fullbody_title",
                descriptionKey = "insight_fullbody_desc",
                citation = "Schoenfeld et al. (2016)"
            ))
        }

        // Upper/lower tier: each half is trained in its own fresh session twice
        // a week — the same frequency floor, just arranged across more days.
        if (strategy == STRATEGY_UPPER_LOWER) {
            insights.add(ScienceInsight(
                titleKey = "insight_upperlower_title",
                descriptionKey = "insight_upperlower_desc",
                citation = "Schoenfeld et al. (2016)"
            ))
        }

        // Dedicated tier: spreading the week's high volume across push/pull/legs
        // sessions keeps per-session quality high without dropping frequency.
        if (strategy == STRATEGY_PPL) {
            insights.add(ScienceInsight(
                titleKey = "insight_ppl_title",
                descriptionKey = "insight_ppl_desc",
                citation = "Schoenfeld et al. (2017)"
            ))
        }

        insights.add(ScienceInsight(
            titleKey = "insight_goal_title",
            descriptionKey = "insight_${workoutGoal}_desc",
            citation = "ACSM Guidelines"
        ))

        insights.add(ScienceInsight(
            titleKey = "insight_rest_title",
            descriptionKey = "insight_rest_${workoutGoal}_desc",
            citation = "Henselmans et al. (2014)"
        ))

        // 5. PROGRAM GENERATION
        val usedIncluded = mutableSetOf<String>()
        val exerciseOccurrenceCount = mutableMapOf<String, Int>()

        days.forEach { dayName ->
            val workoutIndex = selectedDays.indexOf(dayName)
            val isWorkout = workoutIndex != -1
            
            if (isWorkout) {
                val currentParts = partsPerDay[workoutIndex]
                val satisfiedPartsOnThisDay = mutableListOf<String>()
                val usedOnThisDay = mutableSetOf<String>()
                
                val sortedParts = currentParts.sortedByDescending { part ->
                    if (part in majorMuscles) 1 else 0
                }
                
                sortedParts.forEachIndexed { partIndex, part ->
                    var available = getExercisesForPart(part)

                    // Restrict the pool to the equipment the user actually has
                    // access to (empty = every category). Falls back to the full
                    // pool for a body part with no matching equipment so the day
                    // is never left empty.
                    if (equipment.isNotEmpty()) {
                        val allowed = available.filter { getEquipmentForExercise(it) in equipment }
                        if (allowed.isNotEmpty()) available = allowed
                    }

                    // HARD FILTERING BASED ON GOAL & INTENSITY
                    available = when(workoutGoal) {
                        "goal_calisthenics" -> {
                            val bw = getBodyweightExercises(part)
                            if (bw.isNotEmpty()) bw else available.filter { !it.contains("Barbell", true) && !it.contains("Machine", true) }
                        }
                        "goal_crossfit" -> {
                            val cf = available.filter { 
                                it.contains("Thruster", true) || it.contains("Clean", true) || 
                                it.contains("Snatch", true) || it.contains("Burpee", true) || 
                                it.contains("Kettlebell", true) || it.contains("Box Jump", true) ||
                                it.contains("Wall Ball", true) || it.contains("Muscle-up", true) ||
                                it.contains("Devil Press", true) || it.contains("Sandbag", true)
                            }
                            if (cf.isNotEmpty()) {
                                // Prioritize barbell CF movements
                                val bbCf = cf.filter { it.contains("Barbell", true) }
                                if (bbCf.isNotEmpty()) bbCf else cf
                            } else available
                        }
                        "goal_strength", "goal_olympic" -> {
                            val heavy = available.filter { 
                                it.contains("Barbell", true) || it.contains("Squat", true) || 
                                it.contains("Press", true) || it.contains("Deadlift", true) || 
                                it.contains("Clean", true) || it.contains("Snatch", true) || 
                                it.contains("Military Press", true) || it.contains("Zercher", true) ||
                                it.contains("Good Morning", true)
                            }
                            if (heavy.isNotEmpty()) {
                                // Exclude obvious accessories from the "Heavy" pool if we have better options
                                val primaryHeavy = heavy.filter { !it.contains("Curl", true) && !it.contains("Extension", true) && !it.contains("Lateral", true) }
                                if (primaryHeavy.isNotEmpty()) primaryHeavy else heavy
                            } else {
                                val secondary = available.filter { it.contains("Dumbbell", true) }
                                if (secondary.isNotEmpty()) secondary else available
                            }
                        }
                        "goal_bodybuilding", "goal_powerbuilding" -> {
                            // If intensity is HARD, prefer Barbell variations for primary lifts
                            val preferred = if (intensity == "Hard" && part in majorMuscles) {
                                available.filter { it.contains("Barbell", true) || it.contains("Bench Press", true) || it.contains("Squat", true) }
                            } else emptyList()

                            val bb = available.filter { 
                                it.contains("Cable", true) || it.contains("Machine", true) || 
                                it.contains("Incline", true) || it.contains("Decline", true) ||
                                it.contains("Flyes", true) || it.contains("Curl", true) || 
                                it.contains("Extension", true) || it.contains("Pushdown", true) ||
                                it.contains("Skull", true) || it.contains("Dips", true) ||
                                it.contains("Chin-up", true) || it.contains("Raise", true) ||
                                it.contains("Pec Deck", true) || it.contains("Preacher", true)
                            }
                            if (preferred.isNotEmpty()) preferred + bb else if (bb.isNotEmpty()) bb else available
                        }
                        "goal_functional", "goal_athletic" -> {
                            val func = available.filter { 
                                it.contains("Lunge", true) || it.contains("Step-up", true) || 
                                it.contains("Single-leg", true) || it.contains("Thruster", true) ||
                                it.contains("Kettlebell", true) || it.contains("One-arm", true) ||
                                it.contains("Dynamic", true) || it.contains("Plank", true) ||
                                it.contains("Medicine Ball", true) || it.contains("Rotational", true) ||
                                it.contains("Box Jump", true) || it.contains("Turkish Get-up", true) ||
                                it.contains("Copenhagen", true) || it.contains("Pistol", true)
                            }
                            if (func.isNotEmpty()) func else available
                        }
                        else -> available
                    }

                    available = available.filter { it !in excludedExercises }

                    // Suggest on-part exercises first: at Easy difficulty only ONE
                    // exercise is picked per part, and the unsorted pool could offer a
                    // cross-listed movement (Dips under Chest) whose stored PRIMARY
                    // muscle is another group - which flipped the setup's balance
                    // warning to "incomplete" with no user-visible cause. Ordering by
                    // stored primary muscle matching the part keeps the default build
                    // green at every difficulty, and changing ONLY the equipment later
                    // re-ranks within the allowed pool the same way, so a green build
                    // stays green (and a red one only changes for a real reason).
                    available = available.sortedBy { name ->
                        val primary = getMuscleGroupForExercise(name)
                        if (getTopicForMuscle(primary) == getTopicForMuscle(part)) 0 else 1
                    }

                    if (available.isEmpty() && includedExercises.none { exerciseMatchesPart(it, part) }) {
                        unsatisfiedParts.add(part)
                    } else {
                        satisfiedPartsOnThisDay.add(part)
                        
                        val exerciseLimit = when(intensity) {
                            "Hard" -> 3
                            "Medium" -> 2
                            else -> 1
                        }
                        
                        var exercisesPicked = 0
                        val matchingIncluded = includedExercises.filter { 
                            exerciseMatchesPart(it, part) && it !in usedIncluded &&
                            available.contains(it)
                        }
                        
                        matchingIncluded.take(exerciseLimit).forEach { exName ->
                            val occurrence = exerciseOccurrenceCount.getOrPut(exName) { 0 } + 1
                            exerciseOccurrenceCount[exName] = occurrence
                            
                            routineExercises.add(RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayName,
                                name = exName,
                                // Store the exercise's own precise primary muscle, not the
                                // broad part the picker chose from: Push-ups offered by the
                                // bodyweight pool under "Front Deltoids" are still a chest
                                // movement, and storing them as a deltoid is what made rows
                                // display as shoulders while every resolver said Chest.
                                muscleGroup = getMuscleGroupForExercise(exName).ifBlank { part },
                                targetSets = setsPerExercise,
                                // Goal owns reps + rest; difficulty owns the effort label.
                                targetReps = getGoalReps(exName, workoutGoal),
                                targetIntensity = getGoalIntensity(workoutGoal, intensity, exName),
                                targetWeightKg = 0.0,
                                restSeconds = getGoalRestSeconds(workoutGoal, exName),
                                transitionRestSeconds = 90, 
                                videoUrl = "",
                                orderIndex = (partIndex * 10) + exercisesPicked,
                                tempo = ""
                            ))
                            usedIncluded.add(exName)
                            usedOnThisDay.add(exName)
                            exercisesPicked++
                        }

                        if (exercisesPicked < exerciseLimit) {
                            val candidates = available.filter { it !in usedIncluded && it !in usedOnThisDay }
                            val pool = if (candidates.isNotEmpty()) candidates else available
                            
                            pool.take(exerciseLimit - exercisesPicked).forEach { exName ->
                                val occurrence = exerciseOccurrenceCount.getOrPut(exName) { 0 } + 1
                                exerciseOccurrenceCount[exName] = occurrence
                                
                                routineExercises.add(RoutineExerciseEntity(
                                    id = 0,
                                    dayOfWeek = dayName,
                                    name = exName,
                                    // Same as above: the stored group is the precise primary
                                    // muscle ("Pectorals"), never the broad picker part.
                                    muscleGroup = getMuscleGroupForExercise(exName).ifBlank { part },
                                    targetSets = setsPerExercise,
                                    // Goal owns reps + rest; difficulty owns the effort label.
                                    targetReps = getGoalReps(exName, workoutGoal),
                                    targetIntensity = getGoalIntensity(workoutGoal, intensity, exName),
                                    targetWeightKg = 0.0,
                                    restSeconds = getGoalRestSeconds(workoutGoal, exName),
                                    transitionRestSeconds = 90,
                                    videoUrl = "",
                                    orderIndex = (partIndex * 10) + exercisesPicked,
                                    tempo = ""
                                ))
                                usedOnThisDay.add(exName)
                                exercisesPicked++
                            }
                        }
                    }
                }
                
                val dayExs = routineExercises.filter { it.dayOfWeek == dayName }
                val sortedDayExs = dayExs.sortedWith(compareBy(
                    { getExercisePriority(it.name) },
                    { it.orderIndex }
                ))
                
                routineExercises.removeAll(dayExs)
                routineExercises.addAll(sortedDayExs.mapIndexed { i, ex -> ex.copy(orderIndex = i + 1) })

                // A split day names its one or two parts; a full-body day works most
                // of the picked pool, so the title is capped to keep it readable.
                val distinctParts = satisfiedPartsOnThisDay.distinct()
                val partsLabel = if (distinctParts.size > 4) {
                    distinctParts.take(3).joinToString(", ") + ", …"
                } else {
                    distinctParts.joinToString(", ")
                }
                routineDays.add(RoutineDayEntity(
                    dayOfWeek = dayName,
                    title = if (satisfiedPartsOnThisDay.isNotEmpty()) "$dayName — $partsLabel" else "$dayName — Recovery",
                    description = "push_desc", 
                    isRestDay = satisfiedPartsOnThisDay.isEmpty()
                ))

            } else {
                routineDays.add(RoutineDayEntity(
                    dayOfWeek = dayName,
                    title = "$dayName — Rest",
                    description = "rest_desc", 
                    isRestDay = true
                ))
            }
        }

        return GeneratedProgram(
            routineDays,
            routineExercises.map { it.copy(targetReps = enforceRepRange(it.targetReps)) },
            unsatisfiedParts.distinct(),
            insights
        )
    }

    /**
     * Every exercise must span at least [MIN_REP_RANGE_SPAN] reps so the reps
     * control has a usable range. 4 keeps the real schemes intact — a strength
     * block stays "1–5" instead of being widened into something meaningless.
     * Accepts formats like "8–12", "8-12", "10 each leg", "30 sec" and single values.
     * Time-based entries ("30 sec") are returned unchanged.
     */
    const val MIN_REP_RANGE_SPAN = 4

    fun parseRepRange(targetReps: String): Pair<Int, Int>? {
        if (targetReps.contains("sec", ignoreCase = true)) return null
        val matches = Regex("\\d+").findAll(targetReps).toList()
        return when {
            matches.size >= 2 -> {
                val v1 = matches.first().value.toInt()
                val v2 = matches.last().value.toInt()
                minOf(v1, v2) to maxOf(v1, v2)
            }
            matches.size == 1 -> {
                val v = matches.first().value.toInt()
                v to v
            }
            else -> null
        }
    }

    /** Widens a reps string so that max - min >= [MIN_REP_RANGE_SPAN], preserving the center of the range. */
    fun enforceRepRange(targetReps: String): String {
        val range = parseRepRange(targetReps) ?: return targetReps
        val (minVal, maxVal) = range
        if (maxVal - minVal >= MIN_REP_RANGE_SPAN) return targetReps
        val mid = (minVal + maxVal) / 2
        val newMin = (mid - MIN_REP_RANGE_SPAN / 2).coerceAtLeast(1)
        val newMax = newMin + MIN_REP_RANGE_SPAN
        val suffix = targetReps.substringAfterLast(Regex("\\d+").findAll(targetReps).last().value, "")
        return "$newMin–$newMax$suffix"
    }

    /** The auto-selected rep count for an exercise: the middle of its target range. */
    fun getMiddleRep(targetReps: String): Int {
        val (minVal, maxVal) = parseRepRange(targetReps) ?: return 10
        return (minVal + maxVal) / 2
    }

    /** Builds a reps string spanning [MIN_REP_RANGE_SPAN] centered on [reps] (clamped at 1). */
    fun repRangeAround(reps: Int): String {
        val minVal = (reps - MIN_REP_RANGE_SPAN / 2).coerceAtLeast(1)
        return "$minVal–${minVal + MIN_REP_RANGE_SPAN}"
    }

    private fun getExercisePriority(name: String): Int {
        val n = name.lowercase()
        return when {
            n.contains("barbell squat") || (n.contains("deadlift") && !n.contains("romanian") && !n.contains("stiff")) || 
            n.contains("clean") || n.contains("snatch") || n.contains("thruster") -> 1
            n.contains("barbell bench press") || n.contains("military press") || n.contains("overhead press") -> 2
            n.contains("barbell") -> 3
            n.contains("row") || n.contains("pull-up") || n.contains("chin-up") -> 4
            n.contains("dumbbell") -> 5
            n.contains("cable") || n.contains("machine") -> 6
            else -> 7
        }
    }

    private fun isHeavyCompound(name: String): Boolean {
        val n = name.lowercase()
        // Exercises that safe and productive for low-rep strength work
        return (n.contains("barbell") || n.contains("military press")) && 
               (n.contains("squat") || (n.contains("deadlift") && !n.contains("romanian")) || 
                n.contains("bench press") || n.contains("overhead press") || 
                n.contains("military press") || n.contains("press behind the neck") ||
                n.contains("clean") || n.contains("snatch"))
    }

    /**
     * Rest between sets, decided by the training goal alone (plus whether the
     * movement is a heavy compound). ACSM/NSCA-style: long rest for maximal
     * strength and power, short rest for endurance and conditioning work.
     */
    fun getGoalRestSeconds(goal: String, exerciseName: String = ""): Int {
        val isCompound = if (exerciseName.isNotEmpty()) isHeavyCompound(exerciseName) else true
        return when(goal) {
            "goal_strength", "goal_olympic" -> if (isCompound) 240 else 90
            "goal_powerbuilding" -> if (isCompound) 180 else 75
            "goal_bodybuilding" -> 90
            "goal_functional", "goal_athletic" -> 60
            "goal_calisthenics", "goal_crossfit" -> 45
            else -> 60
        }
    }

    /**
     * The effort cue shown for an exercise. Difficulty is the only input: the
     * goal decides which words fit, but Easy / Medium / Hard decide the level.
     */
    fun getGoalIntensity(goal: String, intensity: String, exerciseName: String): String {
        val isCompound = isHeavyCompound(exerciseName)
        return when(intensity) {
            "Hard" -> {
                if (goal == "goal_strength" || goal == "goal_olympic") {
                    if (isCompound) "Very Heavy" else "Heavy"
                } else if (goal == "goal_bodybuilding") {
                    "To Failure"
                } else {
                    "High Intensity"
                }
            }
            "Easy" -> "Light"
            else -> "Moderate"
        }
    }

    /**
     * The rep range for an exercise, decided by the training goal alone.
     *
     * Difficulty does not move reps: a "Hard" strength squat and an "Easy"
     * strength squat use the same 1–5 scheme, they differ in sets and load.
     * That is what keeps the three axes from fighting each other.
     */
    fun getGoalReps(exerciseName: String, goal: String): String {
        if (isExerciseTimeBased(exerciseName)) return "30 sec"

        val isCompound = isHeavyCompound(exerciseName)
        val isIsolation = exerciseName.lowercase().let {
            it.contains("curl") || it.contains("extension") || it.contains("lateral") ||
            it.contains("raise") || it.contains("pushdown") || it.contains("kickback") ||
            it.contains("fly")
        }

        // Even in a strength block, accessories stay in a hypertrophy range —
        // low reps on single-joint work is not productive or safe.
        val range = when(goal) {
            "goal_strength", "goal_olympic" -> if (isCompound) 1..5 else 8..12
            "goal_powerbuilding" -> if (isCompound) 3..6 else 8..12
            "goal_bodybuilding" -> if (isIsolation) 10..15 else 8..12
            "goal_functional", "goal_athletic" -> 10..15
            "goal_calisthenics", "goal_crossfit" -> 12..20
            else -> 8..12
        }
        return enforceRepRange("${range.first}–${range.last}")
    }

    /**
     * Re-parameterizes a replaced exercise row from the user's settings — the
     * same defaults [generateAssistedProgram] would have given the incoming
     * exercise — instead of letting the new row inherit the old one's sets,
     * reps and rest. Swapping Squats for Planks used to keep "8–12" reps and
     * a barbell's rest timer; now the swap lands the way the plan would have
     * built the exercise in the first place: the goal owns reps and rest, the
     * difficulty owns sets and the effort label, and the load resets (the
     * workout screen still suggests the user's last-used weight for that
     * exercise when it has one).
     *
     * Pure so the setup builder can pass the goal/difficulty it is still
     * editing, while the routine screen passes the persisted ones.
     * [minSets]/[maxSets] are the user's sets-allocation overrides (0 =
     * unset), applied exactly as the generator applies them.
     */
    fun parameterizeReplacement(
        original: RoutineExerciseEntity,
        newName: String,
        newMuscleGroup: String,
        workoutGoal: String,
        intensity: String,
        minSets: Int = 0,
        maxSets: Int = 0,
        videoUrl: String = ""
    ): RoutineExerciseEntity {
        var setsPerExercise = when (classifyIntensity(intensity)) {
            "Easy" -> 3
            "Hard" -> 5
            else -> 4
        }
        if (minSets > 0) setsPerExercise = kotlin.math.max(setsPerExercise, minSets)
        if (maxSets > 0) setsPerExercise = kotlin.math.min(setsPerExercise, maxSets)

        return original.copy(
            id = 0L,
            name = newName,
            muscleGroup = newMuscleGroup,
            targetSets = setsPerExercise.coerceIn(1, 10),
            targetReps = getGoalReps(newName, workoutGoal),
            targetIntensity = getGoalIntensity(workoutGoal, intensity, newName),
            targetWeightKg = 0.0,
            restSeconds = getGoalRestSeconds(workoutGoal, newName),
            transitionRestSeconds = 90,
            videoUrl = videoUrl,
            tempo = ""
        )
    }

    private var _cachedExerciseNames: List<String>? = null

    fun getAllExerciseNames(): List<String> {
        _cachedExerciseNames?.let { return it }
        val muscles = getAllMuscleGroups()
        val allRaw = muscles.flatMap { 
            getBodyweightExercises(it) + getDumbbellExercises(it) + getBarbellExercises(it) + getGymExercises(it)
        }
        val result = allRaw.distinct().sorted()
        _cachedExerciseNames = result
        return result
    }

    fun getAllMuscleGroups(): List<String> {
        val categories = getBroadMuscleGroups()
        val detailed = listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back", "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs", "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Pectorals")
        return (categories + detailed).distinct().sorted()
    }

    /**
     * The broad body parts a user picks a substitute exercise by.
     *
     * [getAllMuscleGroups] also returns 19 precise muscles (Lats, Pectorals,
     * Rear Deltoids…). Those are the right granularity for labelling what an
     * exercise trains, but the wrong one for filtering: someone replacing a row
     * wants to say "another back movement", not "another Lats movement", and a
     * 25-entry dropdown makes that choice harder, not more precise.
     */
    fun getBroadMuscleGroups(): List<String> =
        listOf("Arms", "Back", "Chest", "Core", "Legs", "Shoulders")

    /** Lowercase keys of [getBroadMuscleGroups], for the lookup in [exercisesForMuscleGroup]. */
    private val broadMuscleGroupKeys =
        listOf("arms", "back", "chest", "core", "legs", "shoulders")

    fun getTopicForMuscle(muscle: String): String {
        val mg = muscle.lowercase().trim()
        return when {
            mg.contains("chest") || mg.contains("pectoral") -> "Chest"
            // "lat" must not swallow "Lateral Deltoids"
            mg.contains("back") || (mg.contains("lat") && !mg.contains("lateral")) || mg.contains("trap") || mg.contains("erector") -> "Back"
            mg.contains("leg") || mg.contains("quad") || mg.contains("hamstring") || mg.contains("glute") || mg.contains("calve") || mg.contains("adductor") || mg.contains("abductor") -> "Legs"
            mg.contains("shoulder") || mg.contains("delt") -> "Shoulders"
            mg.contains("bicep") || mg.contains("tricep") || mg.contains("arm") || mg.contains("forearm") -> "Arms"
            mg.contains("abs") || mg.contains("core") || mg.contains("oblique") || mg.contains("abdominal") -> "Core"
            else -> "Other"
        }
    }

    /**
     * Returns the exercises available for [muscleGroup] that use [equipment].
     * Pass [EQUIPMENT_ALL] (default) to include every equipment category.
     */
    fun getExercisesForEquipment(equipment: String, muscleGroup: String): List<String> {
        // The muscle pool decides WHICH muscles an exercise trains; the equipment
        // table decides WHICH kit it needs. Filtering the pool through the table
        // keeps the two independent, so an exercise can no longer leak into an
        // equipment category just because a muscle list mentioned it.
        val allForGroup = exercisesForMuscleGroup(muscleGroup)
        val filtered = if (equipment == EQUIPMENT_ALL) allForGroup
                       else allForGroup.filter { getEquipmentForExercise(it) == equipment }
        return filtered.distinct().sorted()
    }

    /**
     * Every pool exercise that trains [muscleGroup].
     *
     * A precise group is answered by its own pool list, which lists *every*
     * muscle an exercise works. A broad body part cannot be answered that way:
     * the pools are deliberately liberal (the triceps list carries the dumbbell
     * floor press, the biceps list carries chin-ups), so unioning them would
     * offer a chest press under "Arms" and put chin-ups under both "Arms" and
     * "Back". A body part is instead answered by the exercise's own
     * authoritative body part — the one its badge already shows — which makes
     * the six parts a clean partition of the pool.
     */
    private fun exercisesForMuscleGroup(muscleGroup: String): List<String> {
        if (muscleGroup.trim().lowercase() !in broadMuscleGroupKeys) {
            return getExercisesForPart(muscleGroup)
        }
        val topic = getTopicForMuscle(muscleGroup)
        return getAllExerciseNames().filter {
            getTopicForMuscle(getMuscleGroupForExercise(it)) == topic
        }
    }

    /**
     * Every exercise available for an equipment category across all muscle
     * groups (used when the muscle group filter is set to "Any").
     */
    fun getAllExercisesForEquipment(equipment: String): List<String> {
        return getAllExerciseNames()
            .filter { equipment == EQUIPMENT_ALL || getEquipmentForExercise(it) == equipment }
    }

    /**
     * Returns the equipment category an exercise belongs to (one of the
     * EQUIPMENT_* constants), or "" when it isn't part of the built-in pool
     * (e.g. a user-created custom exercise).
     */
    fun getEquipmentForExercise(name: String): String {
        val clean = name.trim().lowercase()
        if (clean.isEmpty()) return ""
        return exerciseEquipment[clean] ?: ""
    }

    fun getAlternativeExercises(currentExerciseName: String, muscleGroup: String, equipment: String = EQUIPMENT_ALL): List<String> {
        return getExercisesForEquipment(equipment, muscleGroup)
            .filter { it.lowercase() != currentExerciseName.lowercase() }
            .sorted()
    }

    /**
     * The precise (non-topic) muscle groups of the built-in pool, in lookup
     * order. Exercises legitimately appear under several groups (rows under
     * Lats / Upper Back / Rear Deltoids, presses under Chest / Triceps, …);
     * the first match wins for a generic lookup.
     */
    private val detailedMuscleGroups = listOf(
        "Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back",
        "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs",
        "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves",
        "Hip Adductors", "Hip Abductors", "Chest", "Pectorals"
    )

    /**
     * Authoritative primary muscle for every exercise in the built-in pool.
     *
     * The pool lists an exercise under every muscle it works (rows appear under
     * Lats, Upper Back *and* the deltoids), so "first pool hit" cannot separate
     * the primary mover from a secondary one — that is exactly how Dumbbell Rows
     * used to be labelled as a shoulder exercise. This table states the single
     * muscle each exercise is primarily for, and is the source of truth for both
     * the broad body part and the precise muscle shown in the UI.
     *
     * A test asserts that every pool exercise appears here exactly once and that
     * no entry names an exercise outside the pool.
     */
    private val exercisePrimaryMuscles: Map<String, String> = listOf(
        "Pectorals" to listOf(
            "Barbell Bench Presses", "Barbell Floor Presses", "Cable Crossovers", "Cable Flyes",
            "Chest Press Machines", "Decline Barbell Presses",
            "Decline Dumbbell Presses", "Dips (Chest Focus)", "Dumbbell Bench Presses", "Dumbbell Floor Presses",
            "Dumbbell Flyes", "Floor Presses (Machine)", "Hammer Strength Chest Presses",
            "Incline Barbell Presses", "Incline Dumbbell Presses", "Low Cable Flyes",
            "Machine Flyes", "Pec Decks", "Push-ups", "Svend Presses (Cable)"
        ),
        "Lats" to listOf(
            "Archer Pull-ups", "Assisted Pull-up Machines",
            "Barbell Pullovers", "Barbell Rows", "Chin-ups", "Close Grip Lat Pulldowns",
            "Dumbbell Pullovers", "Dumbbell Rows", "Hammer Strength Rows", "L-sit Pull-ups",
            "Lat Pulldowns", "Muscle-ups (BW)", "Pull-ups", "Seated Rows",
            "Straight Arm Pulldowns", "V-Bar Pulldowns", "Wide Grip Pull-ups"
        ),
        "Upper Back" to listOf(
            "Chest Supported Machine Rows", "Inverted Rows", "Machine Rows", "Scapular Pull-ups",
            "Scapular Push-ups", "Seated Wide Rows", "Superman Extensions", "T-Bar Rows",
            "Wall Slides", "Y-W-T Raises"
        ),
        "Upper Traps" to listOf(
            "Barbell Shrugs", "Cable Shrugs", "Dumbbell Shrugs", "Handstand Shrugs",
            "Hise Shrugs (Machine)", "Machine Shrugs", "Smith Machine Shrugs"
        ),
        "Rear Deltoids" to listOf(
            "Barbell Face Pulls", "Cable Face Pulls", "Cable Rear Delt Flyes",
            "Cable Rows (Rear Delt Focus)", "Dumbbell Face Pulls", "Dumbbell Rear Delt Flyes",
            "Rear Delt Decks", "Rear Delt Floor Flyes", "Reverse Pec Decks",
            "Reverse Snow Angels"
        ),
        "Lateral Deltoids" to listOf(
            "Behind-the-back Cable Lateral Raises", "Cable Lateral Raises",
            "Cross Cable Lateral Raises", "Dumbbell Lateral Raises", "Dumbbell Y-Raises",
            "Egyptian Lateral Raises (Cable)", "Machine Lateral Raises",
            "Upright Barbell Rows", "Y-Raises (Cable)"
        ),
        "Front Deltoids" to listOf(
            "Arnold Presses", "Barbell Clean & Presses", "Barbell Front Raises",
            "Barbell Overhead Presses", "Barbell Push Presses", "Barbell Strict Presses",
            "Cable Front Raises", "Dumbbell Clean & Presses", "Dumbbell Front Raises",
            "Dumbbell Shoulder Presses", "Dumbbell Thrusters", "Hammer Strength Shoulder Presses",
            "Handstand Holds", "Landmine Presses", "Military Presses", "Pike Holds",
            "Plank-to-Down-Dogs", "Shoulder Press Machines", "Shoulder Taps", "Smith Machine Presses"
        ),
        "Spinal Erectors" to listOf(
            "Back Extensions", "Barbell Back Extensions", "Barbell Deadlifts",
            "Barbell Rack Pulls", "Bird-Dogs", "Dumbbell Back Extensions",
            "Dumbbell Deadlifts", "Reverse Hypers", "Smith Machine Deadlifts", "Supermans"
        ),
        "Biceps" to listOf(
            "Barbell Curls", "Barbell Spider Curls", "Bayesian Curls (Cable)", "Cable Curls",
            "Concentration Curls", "Drag Curls (Cable)", "Dumbbell Curls", "Dumbbell Hammer Curls",
            "Hammer Cable Curls", "High Cable Curls", "Incline Dumbbell Curls",
            "Inverted Curls", "Machine Curls", "Preacher Curls",
            "Spider Curls", "Underhand Inverted Rows", "Zottman Curls"
        ),
        "Triceps" to listOf(
            "Barbell Overhead Extensions", "Barbell Skullcrushers", "Cable Kickbacks",
            "Close Grip Barbell Presses", "Dips", "Dumbbell Kickbacks",
            "Dumbbell Overhead Extensions", "Dumbbell Skullcrushers", "Machine Dips",
            "Overhead Cable Extensions", "Rope Pushdowns",
            "Smith Machine JM Presses", "Tricep Pushdowns", "V-Bar Pushdowns", "Weighted Dips"
        ),
        "Forearms" to listOf(
            "Barbell Reverse Curls", "Barbell Reverse Wrist Curls", "Barbell Wrist Curls",
            "Cable Wrist Curls", "Dead Hangs", "Dumbbell Pinch Grips", "Dumbbell Reverse Curls",
            "Dumbbell Wrist Curls", "False Grip Holds", "Farmer's Walks", "Finger Push-ups",
            "Fist Squeezes", "Towel Hangs", "Wrist Curl Machines", "Wrist Rollers"
        ),
        "Abs" to listOf(
            "Ab Wheels", "Barbell Crunches", "Barbell Leg Raises", "Barbell Rollouts",
            "Barbell Sit-ups", "Bicycle Crunches", "Cable Crunches", "Cable Knee Raises",
            "Captain's Chair Leg Raises", "Crunches", "Decline Sit-ups (Weighted)", "Dragon Flags",
            "Dumbbell Crunches", "Dumbbell Leg Raises", "Dumbbell V-Ups", "Garhammer Raises",
            "Hanging Leg Raises", "Hollow Body Holds", "L-Sits", "Landmine Rainbows", "Leg Raises",
            "Machine Crunches", "Mountain Climbers", "Planks", "Rope Crunches", "V-Ups"
        ),
        "Obliques" to listOf(
            "Barbell Side Bends", "Barbell Windmills", "Cable Pallof Presses", "Cable Side Bends",
            "Cable Woodchoppers", "Dumbbell Russian Twists", "Dumbbell Side Bends",
            "Dumbbell Windmills", "Dumbbell Woodchoppers", "Hanging Side Raises",
            "Landmine Twists", "Rotary Torso Machines", "Russian Twists", "Side Plank Crunches",
            "Side Plank Rotations", "Side Planks", "Windshield Wipers"
        ),
        "Quadriceps" to listOf(
            "Barbell Front Squats", "Barbell Lunges", "Barbell Squats", "Barbell Step-ups",
            "Box Jumps", "Dumbbell Lunges", "Dumbbell Split Squats", "Dumbbell Squats",
            "Dumbbell Step-ups", "Goblet Squats", "Hack Squats", "Leg Extensions", "Leg Presses",
            "Lunges", "Pendulum Squats", "Sissy Squats", "Smith Machine Squats", "Split Squats",
            "Squats", "Step-ups", "V-Squat Machines", "Wall Sits"
        ),
        "Hamstrings" to listOf(
            "Barbell Good Mornings", "Barbell Romanian Deadlifts", "Cable Pull-throughs",
            "Dumbbell Good Mornings", "Dumbbell Leg Curls", "Dumbbell Romanian Deadlifts",
            "Glute Ham Raises", "Good Mornings (Machine)", "Leg Curls",
            "Nordic Curls", "Seated Leg Curls",
            "Sliding Leg Curls", "Smith Machine RDLs", "Stiff Leg Barbell Deadlifts",
            "Stiff Leg Dumbbell Deadlifts"
        ),
        "Glutes" to listOf(
            "Barbell Glute Bridges", "Barbell Hip Thrusts", "Donkey Kicks",
            "Dumbbell Glute Bridges", "Dumbbell Hip Thrusts", "Fire Hydrants", "Frog Pumps",
            "Glute Bridge Machines", "Glute Bridges", "Glute Kickbacks", "Glute Presses (Machine)"
        ),
        "Calves" to listOf(
            "Barbell Calf Raises", "Calf Raises", "Donkey Calf Raises",
            "Dumbbell Calf Raises", "Jumping Jacks", "Leg Press Calf Raises", "Seated Calf Raises",
            "Smith Machine Calf Raises", "Standing Calf Raises", "Tibialis Raises", "Tip-toe Walks"
        ),
        "Hip Adductors" to listOf(
            "Adductor Machines", "Cable Adductions", "Copenhagen Planks", "Cossack Squats",
            "Seated Adduction Machines", "Sumo Squats"
        ),
        "Hip Abductors" to listOf(
            "Abductor Machines", "Barbell Side Lunges", "Cable Abductions", "Clamshells",
            "Dirty Dogs", "Dumbbell Side Lunges", "Lateral Cable Leg Raises", "Lateral Plank Walks",
            "Seated Abduction Machines", "Side Lying Clamshells (Machine)", "Side Lying Leg Raises",
            "Side Plank Leg Raises", "Side Steps"
        )
    ).flatMap { (muscle, names) -> names.map { it.trim().lowercase() to muscle } }.toMap()

    /** The precise primary muscle for [name], or "" when it isn't in the pool. */
    fun getPrimaryMuscleForExercise(name: String): String =
        exercisePrimaryMuscles[name.trim().lowercase()] ?: ""

    /**
     * The exercise names covered by the authoritative primary-muscle table,
     * sorted. Exposed so the pool integrity tests can prove the table has no
     * gaps (a pool exercise with no entry), no strays (an entry for a name that
     * isn't in the pool) and no duplicates.
     */
    fun getPrimaryMuscleTableNames(): List<String> = exercisePrimaryMuscles.keys.sorted()

    /**
     * Authoritative equipment for every exercise in the built-in pool.
     *
     * Every exercise is listed under exactly ONE category here, so the equipment
     * filter can never guess from the name ("Dumbbell" in the title) and can never
     * pick up an exercise just because a muscle group happens to mention it. The
     * four categories are what the user actually owns:
     *  - [EQUIPMENT_BODYWEIGHT]: no kit beyond a bar/bench/box (push-ups, pull-ups,
     *    dips, planks, bodyweight squats/lunges, hip work, …)
     *  - [EQUIPMENT_DUMBBELL]: anything that needs a dumbbell (hammer curls, goblet
     *    squats, farmer's walks, devil presses, …)
     *  - [EQUIPMENT_BARBELL]: barbell, EZ bar and landmine work (deadlifts, RDLs,
     *    military press, skullcrushers, landmine presses, T-bar rows, …)
     *  - [EQUIPMENT_GYM]: cables and machines (pulldowns, cable flyes, leg press,
     *    hack squat, Smith machine, pec deck, …)
     *
     * The pool functions above still describe which muscles an exercise trains;
     * this table decides which equipment it needs. A test asserts that the two
     * agree on the exercise set and that no exercise sits in two categories.
     */
    private val exerciseEquipment: Map<String, String> = listOf(
        EQUIPMENT_BODYWEIGHT to listOf(
            "Archer Pull-ups", "Bicycle Crunches", "Bird-Dogs", "Back Extensions", "Box Jumps", "Calf Raises",
            "Chin-ups", "Clamshells", "Copenhagen Planks", "Cossack Squats", "Crunches",
            "Dead Hangs", "Decline Sit-ups (Weighted)", "Dips", "Dips (Chest Focus)",
            "Dirty Dogs", "Donkey Kicks", "Dragon Flags", "False Grip Holds", "Finger Push-ups",
            "Fire Hydrants", "Fist Squeezes", "Frog Pumps", "Garhammer Raises", "Glute Bridges",
            "Handstand Holds", "Handstand Shrugs", "Hanging Leg Raises", "Hanging Side Raises",
            "Hollow Body Holds", "Inverted Curls", "Inverted Rows", "Jumping Jacks", "L-Sits",
            "L-sit Pull-ups", "Lateral Plank Walks", "Leg Raises", "Lunges", "Mountain Climbers",
            "Muscle-ups (BW)", "Nordic Curls", "Plank-to-Down-Dogs", "Planks", "Pike Holds",
            "Pull-ups", "Push-ups", "Reverse Snow Angels", "Russian Twists", "Scapular Pull-ups",
            "Scapular Push-ups", "Shoulder Taps", "Side Lying Leg Raises", "Side Plank Crunches",
            "Side Plank Leg Raises", "Side Plank Rotations", "Side Planks", "Side Steps",
            "Sissy Squats", "Sliding Leg Curls", "Split Squats", "Squats", "Step-ups",
            "Sumo Squats", "Superman Extensions", "Supermans", "Tibialis Raises", "Tip-toe Walks",
            "Towel Hangs", "Underhand Inverted Rows", "V-Ups", "Wall Sits", "Wall Slides",
            "Weighted Dips", "Wide Grip Pull-ups", "Windshield Wipers", "Y-W-T Raises"
        ),
        EQUIPMENT_DUMBBELL to listOf(
            "Arnold Presses", "Concentration Curls", "Decline Dumbbell Presses",
            "Dumbbell Back Extensions", "Dumbbell Bench Presses", "Dumbbell Calf Raises", "Dumbbell Clean & Presses",
            "Dumbbell Crunches", "Dumbbell Curls", "Dumbbell Deadlifts", "Dumbbell Face Pulls",
            "Dumbbell Floor Presses", "Dumbbell Flyes", "Dumbbell Front Raises",
            "Dumbbell Glute Bridges", "Dumbbell Good Mornings", "Dumbbell Hammer Curls",
            "Dumbbell Hip Thrusts", "Dumbbell Kickbacks", "Dumbbell Lateral Raises",
            "Dumbbell Leg Curls", "Dumbbell Leg Raises", "Dumbbell Lunges",
            "Dumbbell Overhead Extensions", "Dumbbell Pinch Grips", "Dumbbell Pullovers",
            "Dumbbell Rear Delt Flyes", "Dumbbell Reverse Curls", "Dumbbell Romanian Deadlifts",
            "Dumbbell Rows", "Dumbbell Russian Twists", "Dumbbell Shoulder Presses",
            "Dumbbell Shrugs", "Dumbbell Side Bends", "Dumbbell Side Lunges",
            "Dumbbell Skullcrushers", "Dumbbell Split Squats",
            "Dumbbell Squats", "Dumbbell Step-ups", "Dumbbell Thrusters", "Dumbbell V-Ups",
            "Dumbbell Windmills", "Dumbbell Woodchoppers", "Dumbbell Wrist Curls",
            "Dumbbell Y-Raises", "Farmer's Walks", "Goblet Squats", "Incline Dumbbell Curls",
            "Incline Dumbbell Presses", "Preacher Curls", "Rear Delt Floor Flyes", "Spider Curls",
            "Stiff Leg Dumbbell Deadlifts", "Zottman Curls"
        ),
        EQUIPMENT_BARBELL to listOf(
            "Barbell Back Extensions", "Barbell Bench Presses", "Barbell Calf Raises",
            "Barbell Clean & Presses", "Barbell Crunches", "Barbell Curls",
            "Barbell Deadlifts", "Barbell Face Pulls", "Barbell Floor Presses",
            "Barbell Front Raises", "Barbell Front Squats", "Barbell Glute Bridges",
            "Barbell Good Mornings", "Barbell Hip Thrusts",
            "Barbell Leg Raises", "Barbell Lunges", "Barbell Overhead Extensions",
            "Barbell Overhead Presses", "Barbell Pullovers",
            "Barbell Push Presses", "Barbell Rack Pulls", "Barbell Reverse Curls",
            "Barbell Reverse Wrist Curls", "Barbell Rollouts", "Barbell Romanian Deadlifts",
            "Barbell Rows", "Barbell Shrugs", "Barbell Side Bends", "Barbell Side Lunges",
            "Barbell Sit-ups", "Barbell Skullcrushers", "Barbell Spider Curls",
            "Barbell Squats", "Barbell Step-ups", "Barbell Strict Presses", "Barbell Windmills",
            "Barbell Wrist Curls", "Close Grip Barbell Presses", "Decline Barbell Presses",
            "Incline Barbell Presses", "Landmine Presses", "Landmine Rainbows", "Landmine Twists",
            "Military Presses", "Stiff Leg Barbell Deadlifts", "T-Bar Rows", "Upright Barbell Rows"
        ),
        EQUIPMENT_GYM to listOf(
            "Ab Wheels", "Abductor Machines", "Adductor Machines", "Assisted Pull-up Machines",
            "Bayesian Curls (Cable)", "Behind-the-back Cable Lateral Raises", "Cable Abductions",
            "Cable Adductions", "Cable Crossovers", "Cable Crunches", "Cable Curls",
            "Cable Face Pulls", "Cable Flyes", "Cable Front Raises", "Cable Kickbacks",
            "Cable Knee Raises", "Cable Lateral Raises", "Cable Pallof Presses",
            "Cable Pull-throughs", "Cable Rear Delt Flyes", "Cable Rows (Rear Delt Focus)",
            "Cable Shrugs", "Cable Side Bends", "Cable Woodchoppers", "Cable Wrist Curls",
            "Captain's Chair Leg Raises", "Chest Press Machines",
            "Chest Supported Machine Rows", "Close Grip Lat Pulldowns", "Cross Cable Lateral Raises",
            "Donkey Calf Raises", "Drag Curls (Cable)", "Egyptian Lateral Raises (Cable)",
            "Floor Presses (Machine)", "Glute Bridge Machines", "Glute Ham Raises",
            "Glute Kickbacks", "Glute Presses (Machine)", "Good Mornings (Machine)", "Hack Squats",
            "Hammer Cable Curls", "Hammer Strength Chest Presses", "Hammer Strength Rows",
            "Hammer Strength Shoulder Presses", "High Cable Curls", "Hise Shrugs (Machine)",
            "Lateral Cable Leg Raises", "Lat Pulldowns", "Leg Curls", "Leg Extensions",
            "Leg Press Calf Raises", "Leg Presses", "Low Cable Flyes",
            "Machine Crunches", "Machine Curls", "Machine Dips", "Machine Flyes",
            "Machine Lateral Raises", "Machine Rows", "Machine Shrugs", "Overhead Cable Extensions",
            "Pec Decks", "Pendulum Squats", "Rear Delt Decks", "Reverse Hypers", "Reverse Pec Decks",
            "Rope Crunches", "Rope Pushdowns", "Rotary Torso Machines", "Seated Abduction Machines",
            "Seated Adduction Machines", "Seated Calf Raises", "Seated Leg Curls", "Seated Rows",
            "Seated Wide Rows", "Shoulder Press Machines", "Side Lying Clamshells (Machine)",
            "Smith Machine Calf Raises", "Smith Machine Deadlifts", "Smith Machine JM Presses",
            "Smith Machine Presses", "Smith Machine RDLs", "Smith Machine Shrugs",
            "Smith Machine Squats", "Standing Calf Raises", "Straight Arm Pulldowns",
            "Svend Presses (Cable)", "Tricep Pushdowns", "V-Bar Pulldowns", "V-Bar Pushdowns",
            "V-Squat Machines", "Wrist Curl Machines", "Wrist Rollers", "Y-Raises (Cable)"
        )
    ).flatMap { (equipment, names) -> names.map { it.trim().lowercase() to equipment } }.toMap()

    /** Every exercise covered by the authoritative equipment table, sorted. */
    fun getEquipmentTableNames(): List<String> = exerciseEquipment.keys.sorted()

    private fun isExerciseInGroup(name: String, group: String): Boolean {
        return getGymExercises(group).any { it.equals(name, ignoreCase = true) } ||
            getBarbellExercises(group).any { it.equals(name, ignoreCase = true) } ||
            getDumbbellExercises(group).any { it.equals(name, ignoreCase = true) } ||
            getBodyweightExercises(group).any { it.equals(name, ignoreCase = true) }
    }

    fun getMuscleGroupForExercise(name: String): String {
        // The authoritative table wins: it separates the primary mover from the
        // secondary muscles the exercise is also listed under in the pool.
        getPrimaryMuscleForExercise(name).takeIf { it.isNotEmpty() }?.let { return it }

        val cleanName = name.trim().lowercase()

        for (g in detailedMuscleGroups) {
            if (isExerciseInGroup(name, g)) return g
        }
        
        return when {
            cleanName.contains("bench") || cleanName.contains("chest") || cleanName.contains("fly") -> "Chest"
            cleanName.contains("row") || (cleanName.contains("lat") && !cleanName.contains("lateral")) || cleanName.contains("pull") -> "Lats"
            cleanName.contains("curl") -> "Biceps"
            cleanName.contains("press") && (cleanName.contains("shoulder") || cleanName.contains("overhead")) -> "Shoulders"
            cleanName.contains("squat") || cleanName.contains("leg") -> "Quadriceps"
            cleanName.contains("abs") || cleanName.contains("crunch") || cleanName.contains("plank") -> "Abs"
            else -> "Other"
        }
    }

    /**
     * The best-known precise muscle for [name]. For pool exercises this is the
     * authoritative primary muscle from [exercisePrimaryMuscles], so the result
     * never depends on the order the pool happens to list an exercise in
     * (a row stays "Lats" no matter that it is also listed under the deltoids).
     * Only names outside the pool fall back to a pool scan, which prefers a
     * muscle whose topic matches [preferredTopic] (the topic of the stored
     * group) and otherwise takes the first match.
     * Returns "" when the exercise is not part of the built-in pool.
     */
    fun getPrimaryMuscleGroupForExercise(name: String, preferredTopic: String): String {
        if (name.isBlank()) return ""
        getPrimaryMuscleForExercise(name).takeIf { it.isNotEmpty() }?.let { return it }
        val preferred = mutableListOf<String>()
        val others = mutableListOf<String>()
        for (g in detailedMuscleGroups) {
            if (isExerciseInGroup(name, g)) {
                if (getTopicForMuscle(g) == preferredTopic) preferred.add(g) else others.add(g)
            }
        }
        return preferred.firstOrNull() ?: others.firstOrNull() ?: ""
    }

    /**
     * Whether [exerciseName] belongs to the selected body [part]. The exercise's
     * precise primary muscle counts as a match, and so does the same broad topic
     * (a Chest day still accepts an exercise now labelled "Pectorals", a Legs day
     * accepts "Quadriceps").
     */
    private fun exerciseMatchesPart(exerciseName: String, part: String): Boolean {
        val precise = getMuscleGroupForExercise(exerciseName)
        if (precise.equals(part, ignoreCase = true)) return true
        val preciseTopic = getTopicForMuscle(precise)
        return preciseTopic != "Other" && preciseTopic == getTopicForMuscle(part)
    }

    private fun getExercisesForPart(bodyPart: String): List<String> {
        return (getBodyweightExercises(bodyPart) + getDumbbellExercises(bodyPart) + getBarbellExercises(bodyPart) + getGymExercises(bodyPart)).distinct()
    }

    private fun getBodyweightExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Push-ups", "Dips")
        // Precise-chest alias: the same bodyweight pool the broad "Chest" chip uses.
        "pectorals" -> getBodyweightExercises("chest")
        "front deltoids" -> listOf("Push-ups", "Shoulder Taps", "Handstand Holds", "Pike Holds")
        "lateral deltoids" -> listOf("Plank-to-Down-Dogs", "Side Plank Rotations", "Lateral Plank Walks", "Side Plank Leg Raises", "Wall Slides")
        "rear deltoids" -> listOf("Reverse Snow Angels")
        "lats" -> listOf("Supermans", "Reverse Snow Angels")
        "upper back" -> listOf("Superman Extensions", "Y-W-T Raises")
        "upper traps" -> listOf("Scapular Push-ups", "Wall Slides", "Handstand Shrugs")
        "spinal erectors" -> listOf("Supermans", "Bird-Dogs", "Back Extensions", "Planks")
        "biceps" -> listOf("Inverted Curls")
        "triceps" -> listOf("Dips", "Push-ups")
        "forearms" -> listOf("Fist Squeezes", "Finger Push-ups")
        // Bicycle Crunches appear under both Abs and Obliques: the alternating
        // knee-to-elbow movement trains the rectus abdominis and the obliques,
        // and users naturally search for it among crunch-style core exercises.
        "abs" -> listOf("Crunches", "Bicycle Crunches", "Leg Raises", "Planks", "L-Sits", "V-Ups", "Hollow Body Holds", "Mountain Climbers", "Dragon Flags", "Windshield Wipers")
        "obliques" -> listOf("Russian Twists", "Bicycle Crunches", "Side Planks", "Windshield Wipers", "Side Plank Crunches")
        "quadriceps" -> listOf("Squats", "Lunges", "Step-ups", "Box Jumps", "Wall Sits")
        "hamstrings" -> listOf("Glute Bridges", "Nordic Curls", "Sliding Leg Curls")
        "glutes" -> listOf("Glute Bridges", "Step-ups", "Donkey Kicks", "Fire Hydrants", "Frog Pumps", "Split Squats")
        "calves" -> listOf("Calf Raises", "Jumping Jacks", "Box Jumps", "Tip-toe Walks")
        "hip adductors" -> listOf("Squats", "Copenhagen Planks", "Cossack Squats")
        "hip abductors" -> listOf("Clamshells", "Side Steps", "Dirty Dogs", "Side Plank Leg Raises", "Side Lying Leg Raises")
        "shoulders" -> listOf("Push-ups", "Shoulder Taps", "Handstand Holds", "Pike Holds")
        "back" -> listOf("Supermans", "Bird-Dogs")
        "legs" -> listOf("Squats", "Lunges", "Glute Bridges")
        "arms" -> listOf("Dips", "Inverted Curls")
        "core" -> listOf("Planks", "Leg Raises")
        else -> emptyList()
    }

    private fun getDumbbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Dumbbell Bench Presses", "Dumbbell Floor Presses", "Dumbbell Flyes", "Incline Dumbbell Presses", "Decline Dumbbell Presses", "Dumbbell Pullovers")
        "pectorals" -> getDumbbellExercises("chest")
        "front deltoids" -> listOf("Dumbbell Front Raises", "Dumbbell Shoulder Presses", "Arnold Presses", "Dumbbell Clean & Presses", "Dumbbell Thrusters")
        "lateral deltoids" -> listOf("Dumbbell Lateral Raises", "Dumbbell Rows", "Dumbbell Y-Raises")
        "rear deltoids" -> listOf("Dumbbell Rear Delt Flyes", "Dumbbell Face Pulls", "Dumbbell Rows", "Rear Delt Floor Flyes")
        "lats" -> listOf("Dumbbell Rows", "Dumbbell Pullovers")
        "upper back" -> listOf("Dumbbell Rows", "Dumbbell Face Pulls", "Dumbbell Shrugs")
        "upper traps" -> listOf("Dumbbell Shrugs")
        "spinal erectors" -> listOf("Dumbbell Deadlifts", "Dumbbell Back Extensions", "Dumbbell Good Mornings", "Dumbbell Romanian Deadlifts")
        "biceps" -> listOf("Dumbbell Curls", "Dumbbell Hammer Curls", "Incline Dumbbell Curls", "Concentration Curls", "Zottman Curls", "Spider Curls", "Preacher Curls")
        "triceps" -> listOf("Dumbbell Overhead Extensions", "Dumbbell Kickbacks", "Dumbbell Skullcrushers", "Dumbbell Floor Presses", "Dumbbell Shoulder Presses")
        "forearms" -> listOf("Dumbbell Wrist Curls", "Dumbbell Reverse Curls", "Farmer's Walks", "Dumbbell Pinch Grips")
        "abs" -> listOf("Dumbbell Crunches", "Dumbbell Russian Twists", "Dumbbell Side Bends", "Dumbbell Leg Raises", "Dumbbell Woodchoppers", "Dumbbell V-Ups")
        "obliques" -> listOf("Dumbbell Russian Twists", "Dumbbell Side Bends", "Dumbbell Windmills")
        "quadriceps" -> listOf("Dumbbell Squats", "Dumbbell Lunges", "Dumbbell Split Squats", "Dumbbell Step-ups")
        "hamstrings" -> listOf("Dumbbell Romanian Deadlifts", "Dumbbell Leg Curls", "Stiff Leg Dumbbell Deadlifts", "Dumbbell Good Mornings")
        "glutes" -> listOf("Dumbbell Glute Bridges", "Dumbbell Squats", "Dumbbell Hip Thrusts", "Dumbbell Step-ups")
        "calves" -> listOf("Dumbbell Calf Raises")
        "hip adductors" -> listOf("Dumbbell Squats")
        "hip abductors" -> listOf("Dumbbell Side Lunges")
        "shoulders" -> listOf("Dumbbell Shoulder Presses", "Dumbbell Lateral Raises", "Dumbbell Front Raises", "Dumbbell Thrusters", "Arnold Presses")
        "back" -> listOf("Dumbbell Rows", "Dumbbell Pullovers")
        "legs" -> listOf("Dumbbell Squats", "Dumbbell Lunges", "Dumbbell Romanian Deadlifts")
        "arms" -> listOf("Dumbbell Curls", "Dumbbell Hammer Curls", "Dumbbell Overhead Extensions", "Dumbbell Kickbacks")
        "core" -> listOf("Dumbbell Crunches", "Dumbbell Side Bends", "Dumbbell Russian Twists")
        else -> listOf("Dumbbell Thrusters", "Devil Presses", "Dumbbell Snatches")
    }

    private fun getBarbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Presses", "Barbell Floor Presses", "Incline Barbell Presses", "Decline Barbell Presses", "Close Grip Barbell Presses")
        "pectorals" -> getBarbellExercises("chest")
        "front deltoids" -> listOf("Military Presses", "Barbell Front Raises", "Barbell Overhead Presses", "Barbell Push Presses", "Landmine Presses", "Barbell Clean & Presses", "Barbell Strict Presses")
        "lateral deltoids" -> listOf("Upright Barbell Rows", "Military Presses")
        "rear deltoids" -> listOf("Barbell Rows", "Barbell Face Pulls")
        "lats" -> listOf("Barbell Rows", "Barbell Pullovers")
        "upper back" -> listOf("Barbell Rows", "Barbell Shrugs")
        "upper traps" -> listOf("Barbell Shrugs")
        "spinal erectors" -> listOf("Barbell Deadlifts", "Barbell Good Mornings", "Barbell Rack Pulls", "Barbell Back Extensions", "Stiff Leg Barbell Deadlifts")
        "biceps" -> listOf("Barbell Curls", "Barbell Reverse Curls", "Barbell Spider Curls")
        "triceps" -> listOf("Close Grip Barbell Presses", "Barbell Skullcrushers", "Barbell Overhead Extensions")
        "forearms" -> listOf("Barbell Wrist Curls", "Barbell Reverse Wrist Curls")
        "abs" -> listOf("Barbell Rollouts", "Landmine Rainbows", "Barbell Sit-ups", "Barbell Leg Raises", "Barbell Crunches")
        "obliques" -> listOf("Barbell Side Bends", "Landmine Twists", "Barbell Windmills")
        "quadriceps" -> listOf("Barbell Squats", "Barbell Front Squats", "Barbell Lunges", "Barbell Step-ups", "Barbell Push Presses")
        "hamstrings" -> listOf("Barbell Romanian Deadlifts", "Stiff Leg Barbell Deadlifts", "Barbell Good Mornings")
        "glutes" -> listOf("Barbell Hip Thrusts", "Barbell Deadlifts", "Barbell Glute Bridges", "Barbell Front Squats", "Barbell Step-ups")
        "calves" -> listOf("Barbell Calf Raises")
        "hip adductors" -> listOf("Barbell Deadlifts", "Barbell Squats")
        "hip abductors" -> listOf("Barbell Side Lunges")
        "shoulders" -> listOf("Military Presses", "Barbell Front Raises", "Upright Barbell Rows", "Barbell Clean & Presses", "Barbell Strict Presses")
        "back" -> listOf("Barbell Rows", "Barbell Deadlifts", "Barbell Pullovers")
        "legs" -> listOf("Barbell Squats", "Barbell Romanian Deadlifts", "Barbell Lunges", "Barbell Deadlifts", "Barbell Front Squats")
        "arms" -> listOf("Barbell Curls", "Close Grip Barbell Presses", "Barbell Skullcrushers", "Barbell Reverse Curls")
        "core" -> listOf("Barbell Rollouts", "Barbell Side Bends", "Landmine Twists")
        else -> listOf("Barbell Clean & Presses", "Barbell Snatches", "Barbell Clean & Jerks", "Barbell Hang Clean", "Barbell Power Snatch")
    }

    private fun getGymExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Presses", "Cable Flyes", "Incline Barbell Presses", "Chest Press Machines", "Pec Decks", "Low Cable Flyes", "Dips (Chest Focus)", "Weighted Dips", "Machine Dips", "Machine Flyes", "Cable Crossovers", "Hammer Strength Chest Presses", "Svend Presses (Cable)", "Floor Presses (Machine)")
        "pectorals" -> getGymExercises("chest")
        "front deltoids" -> listOf("Military Presses", "Shoulder Press Machines", "Arnold Presses", "Cable Front Raises", "Landmine Presses", "Hammer Strength Shoulder Presses", "Smith Machine Presses")
        "lateral deltoids" -> listOf("Cable Lateral Raises", "Machine Lateral Raises", "Behind-the-back Cable Lateral Raises", "Cross Cable Lateral Raises", "Y-Raises (Cable)", "Egyptian Lateral Raises (Cable)")
        "rear deltoids" -> listOf("Rear Delt Decks", "Cable Rear Delt Flyes", "Reverse Pec Decks", "Cable Rows (Rear Delt Focus)")
        "lats" -> listOf("Lat Pulldowns", "Seated Rows", "Pull-ups", "Chin-ups", "Wide Grip Pull-ups", "L-sit Pull-ups", "Archer Pull-ups", "V-Bar Pulldowns", "Straight Arm Pulldowns", "Hammer Strength Rows", "Close Grip Lat Pulldowns", "Assisted Pull-up Machines", "Muscle-ups (BW)")
        "upper back" -> listOf("Barbell Rows", "T-Bar Rows", "Machine Rows", "Inverted Rows", "Scapular Pull-ups", "Seated Wide Rows", "Chest Supported Machine Rows", "Cable Face Pulls")
        "upper traps" -> listOf("Dumbbell Shrugs", "Smith Machine Shrugs", "Cable Shrugs", "Machine Shrugs", "Hise Shrugs (Machine)")
        "spinal erectors" -> listOf("Back Extensions", "Reverse Hypers", "Good Mornings (Machine)", "Smith Machine Deadlifts")
        "biceps" -> listOf("Barbell Curls", "Preacher Curls", "Cable Curls", "Concentration Curls", "Bayesian Curls (Cable)", "Chin-ups", "Hammer Cable Curls", "High Cable Curls", "Machine Curls", "Drag Curls (Cable)", "Underhand Inverted Rows")
        "triceps" -> listOf("Tricep Pushdowns", "Dips", "Rope Pushdowns", "Machine Dips", "Weighted Dips", "Overhead Cable Extensions", "V-Bar Pushdowns", "Cable Kickbacks", "Smith Machine JM Presses")
        "forearms" -> listOf("Wrist Rollers", "Dead Hangs", "Towel Hangs", "Farmer's Walks", "Wrist Curl Machines", "Cable Wrist Curls", "False Grip Holds")
        "abs" -> listOf("Cable Crunches", "Russian Twists", "Hanging Leg Raises", "Machine Crunches", "Ab Wheels", "Captain's Chair Leg Raises", "Garhammer Raises", "Cable Knee Raises", "Rope Crunches", "Decline Sit-ups (Weighted)")
        "obliques" -> listOf("Cable Woodchoppers", "Russian Twists", "Side Plank Crunches", "Cable Side Bends", "Landmine Twists", "Cable Pallof Presses", "Rotary Torso Machines", "Hanging Side Raises")
        "quadriceps" -> listOf("Barbell Squats", "Leg Presses", "Leg Extensions", "Hack Squats", "Sissy Squats", "Goblet Squats", "Smith Machine Squats", "V-Squat Machines", "Pendulum Squats")
        "hamstrings" -> listOf("Leg Curls", "Seated Leg Curls", "Glute Ham Raises", "Nordic Curls", "Smith Machine RDLs", "Cable Pull-throughs")
        "glutes" -> listOf("Goblet Squats", "Glute Kickbacks", "Abductor Machines", "Cable Pull-throughs", "Glute Bridge Machines", "Glute Presses (Machine)")
        "calves" -> listOf("Standing Calf Raises", "Seated Calf Raises", "Donkey Calf Raises", "Leg Press Calf Raises", "Smith Machine Calf Raises", "Tibialis Raises")
        "hip adductors" -> listOf("Adductor Machines", "Sumo Squats", "Cable Adductions", "Seated Adduction Machines", "Copenhagen Planks")
        "hip abductors" -> listOf("Abductor Machines", "Lateral Cable Leg Raises", "Cable Abductions", "Seated Abduction Machines", "Side Lying Clamshells (Machine)")
        "shoulders" -> listOf("Military Presses", "Shoulder Press Machines", "Cable Lateral Raises")
        "back" -> listOf("Lat Pulldowns", "Seated Rows", "Barbell Rows", "Pull-ups", "Chin-ups", "Inverted Rows")
        "legs" -> listOf("Barbell Squats", "Leg Presses", "Leg Curls", "Leg Extensions")
        "arms" -> listOf("Barbell Curls", "Tricep Pushdowns", "Cable Curls", "Dips", "Chin-ups")
        "core" -> listOf("Cable Crunches", "Hanging Leg Raises", "Cable Woodchoppers", "Machine Crunches")
        else -> emptyList()
    }

    val template5DaySplit = WorkoutTemplate(
        id = "5day_hypertrophy",
        nameKey = "hypertrophy_5day_name",
        descriptionKey = "hypertrophy_5day_desc",
        badgeKey = "most_popular",
        daysCountKey = "workout_days_format_pl",
        daysCountArgs = 5 to 2,
        dayTemplates = listOf(
            DayTemplate("MONDAY", "pull_title", "pull_desc", false),
            DayTemplate("TUESDAY", "push_title", "push_desc", false),
            DayTemplate("WEDNESDAY", "legs_core_title", "legs_desc", false),
            DayTemplate("THURSDAY", "upper_hypertrophy_title", "upper_power_desc", false),
            DayTemplate("FRIDAY", "lower_hypertrophy_title", "lower_power_desc", false),
            DayTemplate("SATURDAY", "rest_recovery_day", "rest_desc", true),
            DayTemplate("SUNDAY", "rest_recovery_day", "rest_desc", true)
        ),
        exercises = DefaultData.routineExercises
    )

    val templatePPL = WorkoutTemplate(
        id = "ppl_6day",
        nameKey = "ppl_6day_name",
        descriptionKey = "ppl_6day_desc",
        badgeKey = "strength_mass",
        daysCountKey = "workout_days_format_pl",
        daysCountArgs = 6 to 1,
        dayTemplates = listOf(
            DayTemplate("MONDAY", "push_title", "push_desc", false),
            DayTemplate("TUESDAY", "pull_title", "pull_desc", false),
            DayTemplate("WEDNESDAY", "legs_core_title", "legs_desc", false),
            DayTemplate("THURSDAY", "push_title", "push_desc", false),
            DayTemplate("FRIDAY", "pull_title", "pull_desc", false),
            DayTemplate("SATURDAY", "legs_core_title", "legs_desc", false),
            DayTemplate("SUNDAY", "rest_recovery_day", "rest_desc", true)
        ),
        exercises = listOf(
            RoutineExerciseEntity(0, "MONDAY", "Barbell Floor Presses", "Chest", 4, "8–18", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Presses", "Shoulders", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Lateral Raises", "Shoulders", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Overhead Extensions", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Rows", "Back", 4, "8–18", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Rows", "Back", 3, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Curls", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Hammer Curls", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "WEDNESDAY", "Barbell Squats", "Legs", 4, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Barbell Romanian Deadlifts", "Legs", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Lunges", "Legs", 3, "5–15 each leg", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Crunches", "Abs", 3, "15–25", "", 0.0, 45, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Floor Presses", "Chest", 4, "10–20", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Lateral Raises", "Shoulders", 4, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dips", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell Rows", "Back", 4, "8–18", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Pullovers", "Back", 3, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell Curls", "Biceps", 3, "8–18", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "SATURDAY", "Barbell Squats", "Legs", 4, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "SATURDAY", "Leg Raises", "Abs", 3, "10–20", "", 0.0, 45, 90, "", 2)
        )
    )

    val templateUpperLower = WorkoutTemplate(
        id = "upper_lower_4day",
        nameKey = "upper_lower_4day_name",
        descriptionKey = "upper_lower_4day_desc",
        badgeKey = "optimal_recovery",
        daysCountKey = "workout_days_format_pl",
        daysCountArgs = 4 to 3,
        dayTemplates = listOf(
            DayTemplate("MONDAY", "upper_power_title", "upper_power_desc", false),
            DayTemplate("TUESDAY", "lower_power_title", "lower_power_desc", false),
            DayTemplate("WEDNESDAY", "rest_recovery_day", "rest_desc", true),
            DayTemplate("THURSDAY", "upper_hypertrophy_title", "upper_power_desc", false),
            DayTemplate("FRIDAY", "lower_hypertrophy_title", "lower_power_desc", false),
            DayTemplate("SATURDAY", "rest_recovery_day", "rest_desc", true),
            DayTemplate("SUNDAY", "rest_recovery_day", "rest_desc", true)
        ),
        exercises = listOf(
            RoutineExerciseEntity(0, "MONDAY", "Barbell Floor Presses", "Chest", 4, "6–16", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Rows", "Back", 4, "6–16", "", 0.0, 120, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Presses", "Shoulders", 3, "8–18", "", 0.0, 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Curls", "Biceps", 3, "8–18", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Squats", "Legs", 4, "6–16", "", 0.0, 150, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Romanian Deadlifts", "Legs", 3, "8–18", "", 0.0, 120, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Planks", "Abs", 3, "45 sec", "", 0.0, 45, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Floor Presses", "Chest", 3, "10–20", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Rows", "Back", 3, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Lateral Raises", "Shoulders", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Hammer Curls", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Overhead Extensions", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 5),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Lunges", "Legs", 3, "5–15 each leg", "", 0.0, 60, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Leg Raises", "Abs", 3, "10–20", "", 0.0, 45, 90, "", 2)
        )
    )

    val templateFullBody = WorkoutTemplate(
        id = "fullbody_3day",
        nameKey = "fullbody_3day_name",
        descriptionKey = "fullbody_3day_desc",
        badgeKey = "time_efficient",
        daysCountKey = "workout_days_format_pl",
        daysCountArgs = 3 to 4,
        dayTemplates = listOf(
            DayTemplate("MONDAY", "full_body_a_title", "full_body_a_desc", false),
            DayTemplate("TUESDAY", "rest_recovery_day", "rest_desc", true),
            DayTemplate("WEDNESDAY", "full_body_b_title", "full_body_b_desc", false),
            DayTemplate("THURSDAY", "rest_recovery_day", "rest_desc", true),
            DayTemplate("FRIDAY", "full_body_c_title", "full_body_c_desc", false),
            DayTemplate("SATURDAY", "rest_recovery_day", "rest_desc", true),
            DayTemplate("SUNDAY", "rest_recovery_day", "rest_desc", true)
        ),
        exercises = listOf(
            RoutineExerciseEntity(0, "MONDAY", "Barbell Squats", "Legs", 3, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Floor Presses", "Chest", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Rows", "Back", 3, "8–18", "", 0.0, 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Lateral Raises", "Shoulders", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "MONDAY", "Crunches", "Abs", 3, "15–25", "", 0.0, 45, 90, "", 5),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Lunges", "Legs", 3, "5–15 each leg", "", 0.0, 60, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Shoulder Presses", "Shoulders", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Rows", "Back", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Curls", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell Romanian Deadlifts", "Legs", 3, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Floor Presses", "Chest", 3, "10–20", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Hammer Curls", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Overhead Extensions", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Leg Raises", "Abs", 3, "10–20", "", 0.0, 45, 90, "", 5)
        )
    )

    val allTemplates = listOf(template5DaySplit, templatePPL, templateUpperLower, templateFullBody)

    fun isExerciseTimeBased(name: String): Boolean {
        val cleanName = name.trim().lowercase()
        return cleanName.contains("plank") || cleanName.contains("hold") || cleanName.contains("rest")
    }

    fun getRecommendedRepsForExercise(name: String): String {
        if (isExerciseTimeBased(name)) return "30"
        val cleanName = name.trim().lowercase()
        val muscleGroup = getMuscleGroupForExercise(name)
        val rawRange = when {
            cleanName.contains("squat") || cleanName.contains("deadlift") || 
            cleanName.contains("bench") || cleanName.contains("press") ||
            cleanName.contains("row") || cleanName.contains("pull-up") -> "8–12"
            muscleGroup in listOf("Biceps", "Triceps", "Lateral Deltoids", "Rear Deltoids", "Calves") -> "10–15"
            muscleGroup == "Abs" || muscleGroup == "Obliques" -> "15–20"
            else -> "8–12"
        }
        return enforceRepRange(rawRange)
    }

    /**
     * True when [reps] parses cleanly but sits entirely outside the recommended
     * band for [exerciseName] — a soft nudge, not a validation error: the row
     * still saves. A PARTIAL overlap (e.g. 8–20 vs the 8–12 recommendation) is
     * fine, as is anything unparseable.
     */
    fun isOutsideRecommendedRepRange(exerciseName: String, reps: String): Boolean {
        if (isExerciseTimeBased(exerciseName)) return false
        val typed = parseRepRange(reps) ?: return false
        val recommended = parseRepRange(getRecommendedRepsForExercise(exerciseName)) ?: return false
        val (tMin, tMax) = typed
        val (rMin, rMax) = recommended
        return tMax < rMin || tMin > rMax
    }

    /**
     * The RPE value shown for each Easy/Medium/Hard difficulty level. Chosen to
     * sit inside the same bands [classifyIntensity] uses (<=7 = Easy, 9+ = Hard).
     */
    fun getRpeForIntensity(intensity: String): Int = when (intensity) {
        "Easy" -> 6
        "Hard" -> 9
        else -> 8
    }

    /** Classifies an intensity string into "Easy", "Medium" or "Hard" based on RPE, RIR or keywords. */
    fun classifyIntensity(intensity: String): String {
        val t = intensity.lowercase()
        val rpeMatch = Regex("rpe\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*rpe").find(t)
        if (rpeMatch != null) {
            val raw = rpeMatch.groupValues[1].ifBlank { rpeMatch.groupValues[2] }
            val rpe = raw.toDoubleOrNull()
            if (rpe != null) {
                return when {
                    rpe >= 9.0 -> "Hard"
                    rpe <= 7.0 -> "Easy"
                    else -> "Medium"
                }
            }
        }
        val rirMatch = Regex("rir\\s*(\\d+)|(\\d+)\\s*rir").find(t)
        if (rirMatch != null) {
            val raw = rirMatch.groupValues[1].ifBlank { rirMatch.groupValues[2] }
            val rir = raw.toIntOrNull()
            if (rir != null) {
                return when {
                    rir <= 1 -> "Hard"
                    rir >= 3 -> "Easy"
                    else -> "Medium"
                }
            }
        }
        return when {
            t.contains("hard") || t.contains("heavy") || t.contains("failure") || t.contains("max") || t.contains("intense") -> "Hard"
            t.contains("easy") || t.contains("light") || t.contains("warm") -> "Easy"
            else -> "Medium"
        }
    }

    /**
     * Adapts an auto-selected reps range to the intensity/difficulty the user typed:
     * Hard / high RPE shifts the rep center down, Easy / low RPE shifts it up.
     * Single values (e.g. time-based entries) are returned unchanged.
     */
    fun adaptRepsToIntensity(recommended: String, intensity: String): String {
        val range = parseRepRange(recommended) ?: return recommended
        val (minVal, maxVal) = range
        if (minVal == maxVal) return recommended
        val center = (minVal + maxVal) / 2
        val adaptedCenter = when (classifyIntensity(intensity)) {
            "Hard" -> (center - 3).coerceAtLeast(1)
            "Easy" -> center + 3
            else -> center
        }
        return repRangeAround(adaptedCenter)
    }

    // Starting loads by equipment class, before difficulty/style scaling. These are
    // deliberately conservative first-session numbers: the point is a sane starting
    // point the user adjusts, not a 1RM estimate.
    private const val BASE_LOAD_BARBELL_COMPOUND = 40.0
    private const val BASE_LOAD_BARBELL = 20.0
    private const val BASE_LOAD_DUMBBELL_COMPOUND = 10.0
    private const val BASE_LOAD_DUMBBELL_ISOLATION = 6.0
    private const val BASE_LOAD_MACHINE = 20.0
    private const val BASE_LOAD_DEFAULT = 10.0

    /**
     * A suggested starting weight for an exercise, adapted to the chosen difficulty
     * (Easy / Medium / Hard). Difficulty is the only input, so the estimate can't
     * be pulled in two directions by two different controls.
     *
     * Returns 0.0 when there is nothing to load — bodyweight and timed movements —
     * which the UI already reads as "no weight".
     *
     * This is a fallback only: a weight the plan set explicitly, or one the user has
     * already worked up to for this exercise, always wins over this estimate.
     */
    fun getStartingWeightKg(
        exerciseName: String,
        intensity: String
    ): Double {
        if (exerciseName.isBlank()) return 0.0
        if (isBodyweightExercise(exerciseName)) return 0.0
        if (isExerciseTimeBased(exerciseName)) return 0.0

        val difficultyFactor = when (classifyIntensity(intensity)) {
            "Easy" -> 0.75
            "Hard" -> 1.25
            else -> 1.0
        }
        return roundToPlate(baseLoadForExercise(exerciseName) * difficultyFactor)
    }

    /** Base load for an exercise, picked from the equipment it needs. */
    private fun baseLoadForExercise(name: String): Double {
        val n = name.lowercase().trim()
        val isDumbbell = n.contains("dumbbell") || n.contains("db ") || n.contains("goblet")
        val isMachine = n.contains("machine") || n.contains("cable") || n.contains("smith") ||
            n.contains("pec deck") || n.contains("pulldown") || n.contains("pushdown") ||
            n.contains("crossover") || n.contains("rope") || n.contains("hammer strength")
        // Isolation = single-joint movements, loaded much lighter than compounds.
        val isIsolation = n.contains("curl") || n.contains("extension") || n.contains("lateral") ||
            n.contains("raise") || n.contains("pushdown") || n.contains("kickback") ||
            n.contains("fly") || n.contains("flye") || n.contains("shrug") || n.contains("calf")
        return when {
            n.contains("barbell") && isHeavyCompound(n) -> BASE_LOAD_BARBELL_COMPOUND
            n.contains("barbell") -> BASE_LOAD_BARBELL
            isDumbbell && isIsolation -> BASE_LOAD_DUMBBELL_ISOLATION
            isDumbbell -> BASE_LOAD_DUMBBELL_COMPOUND
            isMachine -> BASE_LOAD_MACHINE
            else -> BASE_LOAD_DEFAULT
        }
    }

    /**
     * Chip semantics for the equipment filter.
     *
     * [EQUIPMENT_ALL] is not a separate type — it is the empty selection, which the
     * generator reads as "every type allowed". It is mutually exclusive with the
     * specific chips, so the result is never a mix of All and a type:
     *  - tapping All clears the selection (everything allowed),
     *  - tapping a type while All is on narrows the build to just that type,
     *  - tapping a selected type removes it, and clearing the last one falls back to All,
     *  - ticking every type by hand is also just "no restriction", so it collapses to All.
     */
    fun toggleEquipmentSelection(
        selected: List<String>,
        tapped: String,
        allOptions: List<String>
    ): List<String> = when {
        tapped == EQUIPMENT_ALL -> emptyList()
        selected.isEmpty() -> listOf(tapped)
        selected.contains(tapped) -> selected - tapped
        else -> (selected + tapped).let { next ->
            if (next.size == allOptions.size) emptyList() else next
        }
    }

    /** Rounds to a loadable step: 1 kg on the light dumbbells, 2.5 kg on the rest. */
    private fun roundToPlate(kg: Double): Double {
        if (kg <= 0.0) return 0.0
        val step = if (kg < 10.0) 1.0 else 2.5
        val rounded = kotlin.math.round(kg / step) * step
        return kotlin.math.max(step, kotlin.math.round(rounded * 10.0) / 10.0)
    }

    fun getRecommendedSetsForExercise(name: String): Int {
        val cleanName = name.trim().lowercase()
        return when {
            cleanName.contains("deadlift") -> 2
            cleanName.contains("plank") || cleanName.contains("hold") -> 2
            else -> 3
        }
    }

    fun isBodyweightExercise(name: String): Boolean {
        val lowerName = name.lowercase().trim()
        
        // 1. Explicit common bodyweight patterns
        if (lowerName.contains("push-up") || 
            lowerName.contains("pull-up") || 
            lowerName.contains("chin-up") || 
            lowerName.contains("dip") ||
            lowerName.contains("plank") || 
            lowerName.contains("crunch") ||
            lowerName.contains("leg raise") || 
            lowerName.contains("sit-up") ||
            lowerName.contains("muscle-up") ||
            lowerName.contains("burpee") ||
            lowerName.contains("inverted row") ||
            lowerName.contains("dead hang") ||
            lowerName.contains("australian pull-up") ||
            lowerName.contains("false grip") ||
            lowerName.contains("inverted curl") ||
            (lowerName.contains("squat") && !lowerName.contains("barbell") && !lowerName.contains("dumbbell") && !lowerName.contains("machine") && !lowerName.contains("hack") && !lowerName.contains("goblet")) ||
            (lowerName.contains("lunge") && !lowerName.contains("db") && !lowerName.contains("dumbbell") && !lowerName.contains("barbell")) ||
            lowerName.contains("(bw)") || 
            lowerName.contains("bodyweight")
        ) return true

        val allBW = getAllMuscleGroups().flatMap { getBodyweightExercises(it) }
        return allBW.any { it.equals(name, ignoreCase = true) }
    }

    /**
     * Consecutive days with at least one logged set, counting back from today.
     * If today has no workout yet, the streak is counted from yesterday so a
     * workout done yesterday still keeps the streak alive. Returns 0 when there
     * is no streak.
     */
    fun calculateConsecutiveWorkoutDays(loggedSets: List<LoggedSetEntity>): Int {
        if (loggedSets.isEmpty()) return 0
        val workoutDayKeys = loggedSets.map { dateKey(it.timestamp) }.toSet()

        val cursor = Calendar.getInstance().apply { startOfDay() }
        if (dateKey(cursor.timeInMillis) !in workoutDayKeys) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var count = 0
        while (dateKey(cursor.timeInMillis) in workoutDayKeys) {
            count++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    /**
     * Scheduled workout days (routine days that are not rest days) which have
     * passed without a single logged set. Counted from the Monday of the week
     * containing the first logged set (or the current week when nothing has
     * been logged yet) up to yesterday, so today isn't marked as skipped while
     * it can still be completed. Starts at 0 when there is no schedule.
     */
    fun calculateSkippedWorkoutDays(
        loggedSets: List<LoggedSetEntity>,
        routineDays: List<RoutineDayEntity>
    ): Int {
        val scheduledDays = routineDays
            .filter { !it.isRestDay }
            .mapNotNull { dayOfWeekToCalendarDay(it.dayOfWeek) }
            .toSet()
        if (scheduledDays.isEmpty()) return 0

        val loggedKeys = loggedSets.map { dateKey(it.timestamp) }.toSet()

        val start = Calendar.getInstance().apply { startOfDay() }
        if (loggedSets.isNotEmpty()) {
            start.timeInMillis = loggedSets.minOf { it.timestamp }
            start.startOfDay()
        }
        // Align to the Monday of the start week
        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1)
        }

        val today = Calendar.getInstance().apply { startOfDay() }
        var skipped = 0
        val cursor = start.clone() as Calendar
        while (cursor.before(today)) {
            if (cursor.get(Calendar.DAY_OF_WEEK) in scheduledDays &&
                dateKey(cursor.timeInMillis) !in loggedKeys
            ) {
                skipped++
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return skipped
    }

    private fun dayOfWeekToCalendarDay(dayOfWeek: String): Int? = when (dayOfWeek.uppercase()) {
        "MONDAY" -> Calendar.MONDAY
        "TUESDAY" -> Calendar.TUESDAY
        "WEDNESDAY" -> Calendar.WEDNESDAY
        "THURSDAY" -> Calendar.THURSDAY
        "FRIDAY" -> Calendar.FRIDAY
        "SATURDAY" -> Calendar.SATURDAY
        "SUNDAY" -> Calendar.SUNDAY
        else -> null
    }

    /** Normalizes a timestamp to a (year, day-of-year) key for date comparisons. */
    private fun dateKey(timestamp: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            startOfDay()
        }
        return cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
    }

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
