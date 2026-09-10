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

    // Training style: how the program is performed, beyond which muscles are hit.
    // Explosive = heavier, fewer reps, fast lifts; Slow = lighter, more reps,
    // controlled tempo; Balanced = standard all-around programming.
    const val TRAINING_STYLE_BALANCED = "balanced"
    const val TRAINING_STYLE_EXPLOSIVE = "explosive"
    const val TRAINING_STYLE_SLOW = "slow"

    val allTrainingStyles = listOf(
        TRAINING_STYLE_BALANCED,
        TRAINING_STYLE_EXPLOSIVE,
        TRAINING_STYLE_SLOW
    )

    fun generateAssistedProgram(
        bodyParts: List<String>, 
        selectedDays: List<String>,
        intensity: String,
        workoutGoal: String = "goal_bodybuilding",
        includedExercises: List<String> = emptyList(),
        excludedExercises: List<String> = emptyList(),
        minSets: Int = 0,
        maxSets: Int = 0,
        trainingStyle: String = TRAINING_STYLE_BALANCED
    ): GeneratedProgram {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val routineDays = mutableListOf<RoutineDayEntity>()
        val routineExercises = mutableListOf<RoutineExerciseEntity>()
        val unsatisfiedParts = mutableListOf<String>()
        val insights = mutableListOf<ScienceInsight>()

        if (bodyParts.isEmpty() || selectedDays.isEmpty()) return GeneratedProgram(emptyList(), emptyList(), emptyList(), emptyList())

        // 1. VOLUME STRATEGY (Equal Distribution)
        val majorMuscles = listOf("Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes")
        val expandedBodyParts = bodyParts.toMutableList()
        
        // Distribute parts across selected days
        val partsPerDay = List(selectedDays.size) { mutableListOf<String>() }
        expandedBodyParts.forEachIndexed { index, part ->
            partsPerDay[index % selectedDays.size].add(part)
        }

        // 2. VOLUME RECOMMENDATION
        val setsBase = when(intensity) {
            "Easy" -> 3 
            "Hard" -> 5 
            else -> 4   
        }
        
        var setsPerExercise = when(workoutGoal) {
            "goal_strength", "goal_olympic" -> setsBase + 1 
            "goal_calisthenics", "goal_crossfit" -> setsBase - 1
            else -> setsBase
        }
        
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

                    if (available.isEmpty() && includedExercises.none { getMuscleGroupForExercise(it) == part }) {
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
                            getMuscleGroupForExercise(it) == part && it !in usedIncluded &&
                            available.contains(it)
                        }
                        
                        matchingIncluded.take(exerciseLimit).forEach { exName ->
                            val occurrence = exerciseOccurrenceCount.getOrPut(exName) { 0 } + 1
                            exerciseOccurrenceCount[exName] = occurrence
                            
                            routineExercises.add(RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayName,
                                name = exName,
                                muscleGroup = part,
                                targetSets = setsPerExercise,
                                targetReps = getGoalReps(exName, workoutGoal, setsPerExercise, trainingStyle),
                                targetIntensity = getGoalIntensity(workoutGoal, intensity, exName, trainingStyle),
                                targetWeightKg = 0.0,
                                restSeconds = getGoalRestSeconds(workoutGoal, exName, trainingStyle),
                                transitionRestSeconds = 90, 
                                videoUrl = "",
                                orderIndex = (partIndex * 10) + exercisesPicked,
                                tempo = getGoalTempo(trainingStyle)
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
                                    muscleGroup = part,
                                    targetSets = setsPerExercise,
                                    targetReps = getGoalReps(exName, workoutGoal, setsPerExercise, trainingStyle),
                                    targetIntensity = getGoalIntensity(workoutGoal, intensity, exName, trainingStyle),
                                    targetWeightKg = 0.0,
                                    restSeconds = getGoalRestSeconds(workoutGoal, exName, trainingStyle),
                                    transitionRestSeconds = 90,
                                    videoUrl = "",
                                    orderIndex = (partIndex * 10) + exercisesPicked,
                                    tempo = getGoalTempo(trainingStyle)
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

                val partsLabel = satisfiedPartsOnThisDay.distinct().joinToString(", ")
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
     * Every exercise must span at least [MIN_REP_RANGE_SPAN] reps.
     * Accepts formats like "8–12", "8-12", "10 each leg", "30 sec" and single values.
     * Time-based entries ("30 sec") are returned unchanged.
     */
    const val MIN_REP_RANGE_SPAN = 10

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
                n.contains("clean") || n.contains("snatch"))
    }

    fun getGoalRestSeconds(goal: String, exerciseName: String = "", trainingStyle: String = TRAINING_STYLE_BALANCED): Int {
        val isCompound = if (exerciseName.isNotEmpty()) isHeavyCompound(exerciseName) else true
        
        val base = when(goal) {
            "goal_strength", "goal_olympic" -> if (isCompound) 240 else 90
            "goal_powerbuilding" -> if (isCompound) 180 else 75
            "goal_bodybuilding" -> 90
            "goal_functional", "goal_athletic" -> 60
            "goal_calisthenics", "goal_crossfit" -> 45
            else -> 60
        }
        // Style tweaks: explosive sets need longer recovery, tempo sets recover faster.
        return when(trainingStyle) {
            TRAINING_STYLE_EXPLOSIVE -> (base * 1.5).toInt()
            TRAINING_STYLE_SLOW -> (base * 0.75).toInt().coerceAtLeast(30)
            else -> base
        }
    }

    fun getGoalIntensity(goal: String, intensity: String, exerciseName: String, trainingStyle: String = TRAINING_STYLE_BALANCED): String {
        val isCompound = isHeavyCompound(exerciseName)
        val base = when(intensity) {
            "Hard" -> {
                if (goal == "goal_strength" || goal == "goal_olympic") {
                    if (isCompound) "Very Heavy" else "Heavy"
                } else if (goal == "goal_bodybuilding") {
                    "To Failure"
                } else {
                    "High Intensity"
                }
            }
            "Easy" -> {
                "Light"
            }
            else -> {
                "Moderate"
            }
        }
        // The training style overrides the effort label so the cue stays short and obvious.
        return when(trainingStyle) {
            TRAINING_STYLE_EXPLOSIVE -> "Explosive"
            TRAINING_STYLE_SLOW -> "Controlled"
            else -> base
        }
    }

    /** Short per-exercise tempo cue derived from the training style ("" = balanced). */
    fun getGoalTempo(trainingStyle: String): String = when(trainingStyle) {
        TRAINING_STYLE_EXPLOSIVE -> "explosive"
        TRAINING_STYLE_SLOW -> "slow"
        else -> ""
    }

    fun getGoalReps(exerciseName: String, goal: String, forcedSets: Int = 0, trainingStyle: String = TRAINING_STYLE_BALANCED): String {
        if (isExerciseTimeBased(exerciseName)) return "30 sec"
        
        val isCompound = isHeavyCompound(exerciseName)
        val isIsolation = exerciseName.lowercase().let { 
            it.contains("curl") || it.contains("extension") || it.contains("lateral") || 
            it.contains("raise") || it.contains("pushdown") || it.contains("kickback") ||
            it.contains("fly")
        }
        
        // Even in strength goals, accessories shouldn't go below 8 reps
        val baseRange = when(goal) {
            "goal_strength", "goal_olympic" -> if (isCompound) 1..5 else 8..12
            "goal_powerbuilding" -> if (isCompound) 3..6 else 8..12
            "goal_bodybuilding" -> if (isIsolation) 10..15 else 8..12
            "goal_functional", "goal_athletic" -> 10..15
            "goal_calisthenics", "goal_crossfit" -> 12..20
            else -> 8..12
        }
        
        // Style nudges the rep range: explosive = heavier & fewer reps,
        // slow/tempo = lighter & more reps. Clamped to stay at 1+ reps.
        val styleRange = when(trainingStyle) {
            TRAINING_STYLE_EXPLOSIVE -> (baseRange.first - 2).coerceAtLeast(1)..(baseRange.last - 3).coerceAtLeast(2)
            TRAINING_STYLE_SLOW -> (baseRange.first + 3)..(baseRange.last + 5)
            else -> baseRange
        }
        
        return when {
            forcedSets >= 6 -> {
                enforceRepRange("${kotlin.math.max(1, styleRange.first - 2)}–${kotlin.math.max(2, styleRange.last - 2)}")
            }
            forcedSets <= 2 && forcedSets > 0 -> {
                val compensatedStart = styleRange.first + 3
                val compensatedEnd = styleRange.last + 5
                enforceRepRange("$compensatedStart–$compensatedEnd")
            }
            else -> enforceRepRange("${styleRange.first}–${styleRange.last}")
        }
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
        val categories = listOf("Arms", "Legs", "Back", "Chest", "Shoulders", "Core")
        val detailed = listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back", "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs", "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors")
        return (categories + detailed).distinct().sorted()
    }

    fun getTopicForMuscle(muscle: String): String {
        val mg = muscle.lowercase().trim()
        return when {
            mg.contains("chest") -> "Chest"
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
        val allForGroup = when (equipment) {
            EQUIPMENT_BODYWEIGHT -> getBodyweightExercises(muscleGroup)
            EQUIPMENT_DUMBBELL -> getDumbbellExercises(muscleGroup)
            EQUIPMENT_BARBELL -> getBarbellExercises(muscleGroup)
            EQUIPMENT_GYM -> getGymExercises(muscleGroup)
            else -> getBodyweightExercises(muscleGroup) + getDumbbellExercises(muscleGroup) + getBarbellExercises(muscleGroup) + getGymExercises(muscleGroup)
        }
        return allForGroup.distinct().sorted()
    }

    /**
     * Every exercise available for an equipment category across all muscle
     * groups (used when the muscle group filter is set to "Any").
     */
    fun getAllExercisesForEquipment(equipment: String): List<String> {
        return getAllMuscleGroups()
            .flatMap { getExercisesForEquipment(equipment, it) }
            .distinct()
            .sorted()
    }

    /**
     * Returns the equipment category an exercise belongs to (one of the
     * EQUIPMENT_* constants), or "" when it isn't part of the built-in pool
     * (e.g. a user-created custom exercise).
     */
    fun getEquipmentForExercise(name: String): String {
        val clean = name.trim().lowercase()
        if (clean.isEmpty()) return ""
        val map = _equipmentCache ?: buildEquipmentCache().also { _equipmentCache = it }
        return map[clean] ?: ""
    }

    private var _equipmentCache: Map<String, String>? = null

    private fun buildEquipmentCache(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        getAllMuscleGroups().forEach { muscle ->
            fun addAll(equipment: String, exercises: List<String>) {
                exercises.forEach { map.getOrPut(it.trim().lowercase()) { equipment } }
            }
            addAll(EQUIPMENT_BODYWEIGHT, getBodyweightExercises(muscle))
            addAll(EQUIPMENT_DUMBBELL, getDumbbellExercises(muscle))
            addAll(EQUIPMENT_BARBELL, getBarbellExercises(muscle))
            addAll(EQUIPMENT_GYM, getGymExercises(muscle))
        }
        return map
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
        "Hip Adductors", "Hip Abductors", "Chest"
    )

    private fun isExerciseInGroup(name: String, group: String): Boolean {
        return getGymExercises(group).any { it.equals(name, ignoreCase = true) } ||
            getBarbellExercises(group).any { it.equals(name, ignoreCase = true) } ||
            getDumbbellExercises(group).any { it.equals(name, ignoreCase = true) } ||
            getBodyweightExercises(group).any { it.equals(name, ignoreCase = true) }
    }

    fun getMuscleGroupForExercise(name: String): String {
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
     * The best-known precise muscle for [name], preferring a muscle whose topic
     * matches [preferredTopic] (normally the topic of the stored muscle group).
     * This keeps the result consistent with what the user recorded (rows stay
     * "Back"-side even though the pool also lists them under Rear Deltoids).
     * When no same-topic muscle exists, the pool's first match is used anyway —
     * the built-in pool is the authoritative real data for what an exercise
     * trains (e.g. flyes stored as "Shoulders" still resolve to "Chest").
     * Returns "" when the exercise is not part of the built-in pool.
     */
    fun getPrimaryMuscleGroupForExercise(name: String, preferredTopic: String): String {
        if (name.isBlank()) return ""
        val preferred = mutableListOf<String>()
        val others = mutableListOf<String>()
        for (g in detailedMuscleGroups) {
            if (isExerciseInGroup(name, g)) {
                if (getTopicForMuscle(g) == preferredTopic) preferred.add(g) else others.add(g)
            }
        }
        return preferred.firstOrNull() ?: others.firstOrNull() ?: ""
    }

    private fun getExercisesForPart(bodyPart: String): List<String> {
        return (getBodyweightExercises(bodyPart) + getDumbbellExercises(bodyPart) + getBarbellExercises(bodyPart) + getGymExercises(bodyPart)).distinct()
    }

    private fun getBodyweightExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Push-ups", "Dips")
        "front deltoids" -> listOf("Push-ups", "Shoulder Taps", "Handstand Holds", "Pike Holds")
        "lateral deltoids" -> listOf("Plank-to-Down-Dogs", "Side Plank Rotations", "Lateral Plank Walks", "Side Plank Leg Raises", "Wall Slides")
        "rear deltoids" -> listOf("Reverse Snow Angels", "Rear Delt Floor Flyes")
        "lats" -> listOf("Supermans", "Reverse Snow Angels")
        "upper back" -> listOf("Superman Extensions", "Y-W-T Raises", "Face Pulls")
        "upper traps" -> listOf("Scapular Push-ups", "Wall Slides", "Handstand Shrugs")
        "spinal erectors" -> listOf("Supermans", "Bridges", "Bird-Dogs", "Back Extensions", "Planks")
        "biceps" -> listOf("Hammer Curls", "Inverted Curls")
        "triceps" -> listOf("Dips", "Push-ups")
        "forearms" -> listOf("Fist Squeezes", "Finger Push-ups")
        // Bicycle Crunches appear under both Abs and Obliques: the alternating
        // knee-to-elbow movement trains the rectus abdominis and the obliques,
        // and users naturally search for it among crunch-style core exercises.
        "abs" -> listOf("Crunches", "Bicycle Crunches", "Leg Raises", "Planks", "L-Sits", "V-Ups", "Hollow Body Holds", "Mountain Climbers", "Dragon Flags", "Windshield Wipers")
        "obliques" -> listOf("Russian Twists", "Bicycle Crunches", "Side Planks", "Windshield Wipers", "Side Plank Crunches")
        "quadriceps" -> listOf("Squats", "Lunges", "Step-ups", "Box Jumps", "Wall Sits")
        "hamstrings" -> listOf("Glute Bridges", "Nordic Curls", "Sliding Leg Curls", "Good Mornings")
        "glutes" -> listOf("Glute Bridges", "Step-ups", "Donkey Kicks", "Fire Hydrants", "Frog Pumps", "Split Squats")
        "calves" -> listOf("Calf Raises", "Jumping Jacks", "Box Jumps")
        "hip adductors" -> listOf("Side Lying Leg Raises", "Squats", "Copenhagen Planks")
        "hip abductors" -> listOf("Clamshells", "Side Steps", "Dirty Dogs", "Side Plank Leg Raises")
        "shoulders" -> listOf("Push-ups", "Shoulder Taps", "Handstand Holds", "Pike Holds")
        "back" -> listOf("Supermans", "Bird-Dogs")
        "legs" -> listOf("Squats", "Lunges", "Glute Bridges")
        "arms" -> listOf("Dips", "Inverted Curls")
        "core" -> listOf("Planks", "Leg Raises")
        else -> emptyList()
    }

    private fun getDumbbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Dumbbell Floor Presses", "Dumbbell Flyes", "Incline Dumbbell Presses", "Decline Dumbbell Presses", "Dumbbell Pullovers")
        "front deltoids" -> listOf("Dumbbell Front Raises", "Dumbbell Shoulder Presses", "Arnold Presses", "Dumbbell Clean & Presses", "Dumbbell Thrusters")
        "lateral deltoids" -> listOf("Dumbbell Lateral Raises", "Dumbbell Rows", "Dumbbell Y-Raises")
        "rear deltoids" -> listOf("Dumbbell Rear Delt Flyes", "Dumbbell Face Pulls", "Dumbbell Rows")
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
        "calves" -> listOf("Dumbbell Calf Raises", "Tip-toe Walks")
        "hip adductors" -> listOf("Dumbbell Squats", "Copenhagen Planks", "Cossack Squats")
        "hip abductors" -> listOf("Side Lying Leg Raises", "Dumbbell Side Lunges", "Clamshells")
        "shoulders" -> listOf("Dumbbell Shoulder Presses", "Dumbbell Lateral Raises", "Dumbbell Front Raises", "Dumbbell Thrusters", "Arnold Presses")
        "back" -> listOf("Dumbbell Rows", "Dumbbell Pullovers")
        "legs" -> listOf("Dumbbell Squats", "Dumbbell Lunges", "Dumbbell Romanian Deadlifts")
        "arms" -> listOf("Dumbbell Curls", "Dumbbell Hammer Curls", "Dumbbell Overhead Extensions", "Dumbbell Kickbacks")
        "core" -> listOf("Dumbbell Crunches", "Dumbbell Side Bends", "Dumbbell Russian Twists")
        else -> listOf("Dumbbell Thrusters", "Devil Presses", "Dumbbell Snatches")
    }

    private fun getBarbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Presses", "Barbell Floor Presses", "Incline Barbell Presses", "Decline Barbell Presses", "Close Grip Barbell Presses")
        "front deltoids" -> listOf("Military Presses", "Barbell Front Raises", "Barbell Overhead Presses", "Barbell Push Presses", "Landmine Presses", "Barbell Clean & Presses", "Barbell Strict Presses")
        "lateral deltoids" -> listOf("Upright Barbell Rows", "Military Presses")
        "rear deltoids" -> listOf("Barbell Rows", "Barbell Face Pulls")
        "lats" -> listOf("Barbell Rows", "Barbell Pullovers")
        "upper back" -> listOf("Barbell Rows", "Barbell Shrugs")
        "upper traps" -> listOf("Barbell Shrugs")
        "spinal erectors" -> listOf("Barbell Deadlifts", "Barbell Good Mornings", "Barbell Rack Pulls", "Barbell Back Extensions", "Stiff Leg Barbell Deadlifts")
        "biceps" -> listOf("Barbell Curls", "Barbell Reverse Curls", "Barbell Spider Curls")
        "triceps" -> listOf("Close Grip Barbell Presses", "Barbell Skullcrushers", "Barbell Overhead Extensions")
        "forearms" -> listOf("Barbell Wrist Curls", "Barbell Reverse Wrist Curls", "Farmer's Walks")
        "abs" -> listOf("Barbell Rollouts", "Landmine Rainbows", "Barbell Sit-ups", "Barbell Leg Raises", "Barbell Crunches")
        "obliques" -> listOf("Barbell Side Bends", "Landmine Twists", "Barbell Windmills")
        "quadriceps" -> listOf("Barbell Squats", "Barbell Front Squats", "Barbell Lunges", "Barbell Step-ups", "Barbell Push Presses")
        "hamstrings" -> listOf("Barbell Romanian Deadlifts", "Stiff Leg Barbell Deadlifts", "Barbell Good Mornings")
        "glutes" -> listOf("Barbell Hip Thrusts", "Barbell Deadlifts", "Barbell Glute Bridges", "Barbell Front Squats", "Barbell Step-ups")
        "calves" -> listOf("Barbell Calf Raises", "Tip-toe Walks")
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
        "chest" -> listOf("Barbell Bench Presses", "Cable Flyes", "Incline Barbell Presses", "Chest Press Machines", "Pec Decks", "Low Cable Flyes", "Dips (Chest Focus)", "Weighted Dips", "Chest Dips (Bars)", "Machine Dips", "Dips (Bars)", "Machine Flyes", "Cable Crossovers", "Hammer Strength Chest Presses", "Svend Presses (Cable)", "Floor Presses (Machine)")
        "front deltoids" -> listOf("Military Presses", "Shoulder Press Machines", "Arnold Presses", "Cable Front Raises", "Landmine Presses", "Hammer Strength Shoulder Presses", "Smith Machine Presses")
        "lateral deltoids" -> listOf("Side Raises", "Cable Lateral Raises", "Machine Lateral Raises", "Behind-the-back Cable Lateral Raises", "Cross Cable Lateral Raises", "Y-Raises (Cable)", "Egyptian Lateral Raises (Cable)")
        "rear deltoids" -> listOf("Face Pulls", "Rear Delt Decks", "Cable Rear Delt Flyes", "Rope Face Pulls", "Reverse Pec Decks", "Cable Rows (Rear Delt Focus)")
        "lats" -> listOf("Lat Pulldowns", "Seated Rows", "Pull-ups", "Chin-ups", "Wide Grip Pull-ups", "L-sit Pull-ups", "Archer Pull-ups", "V-Bar Pulldowns", "Straight Arm Pulldowns", "Hammer Strength Rows", "Close Grip Lat Pulldowns", "Assisted Pull-up Machines", "Australian Pull-ups (Lats)", "Muscle-ups (BW)")
        "upper back" -> listOf("Barbell Rows", "T-Bar Rows", "Face Pulls", "Machine Rows", "Inverted Rows", "Scapular Pull-ups", "Seated Wide Rows", "Chest Supported Machine Rows", "Cable Face Pulls")
        "upper traps" -> listOf("Dumbbell Shrugs", "Smith Machine Shrugs", "Cable Shrugs", "Machine Shrugs", "Hise Shrugs (Machine)")
        "spinal erectors" -> listOf("Deadlifts", "Back Extensions", "Reverse Hypers", "Good Mornings (Machine)", "Smith Machine Deadlifts")
        "biceps" -> listOf("Barbell Curls", "Preacher Curls", "Cable Curls", "Concentration Curls", "Bayesian Curls (Cable)", "Chin-ups", "Hammer Cable Curls", "High Cable Curls", "Machine Curls", "Drag Curls (Cable)", "Inverted Curls (Bodyweight)", "Underhand Inverted Rows")
        "triceps" -> listOf("Tricep Pushdowns", "Skullcrushers", "Dips", "Rope Pushdowns", "Machine Dips", "Weighted Dips", "Overhead Cable Extensions", "V-Bar Pushdowns", "Cable Kickbacks", "Smith Machine JM Presses", "Dips (Bars)")
        "forearms" -> listOf("Reverse Curls", "Hammer Curls", "Wrist Rollers", "Dead Hangs", "Towel Hangs", "Farmer's Walks", "Wrist Curl Machines", "Cable Wrist Curls", "False Grip Holds")
        "abs" -> listOf("Cable Crunches", "Russian Twists", "Hanging Leg Raises", "Machine Crunches", "Ab Wheels", "Captain's Chair Leg Raises", "Garhammer Raises", "Cable Knee Raises", "Rope Crunches", "Decline Sit-ups (Weighted)")
        "obliques" -> listOf("Cable Woodchoppers", "Russian Twists", "Side Plank Crunches", "Cable Side Bends", "Landmine Twists", "Cable Pallof Presses", "Rotary Torso Machines", "Hanging Side Raises")
        "quadriceps" -> listOf("Barbell Squats", "Leg Presses", "Leg Extensions", "Hack Squats", "Sissy Squats", "Goblet Squats", "Smith Machine Squats", "V-Squat Machines", "Pendulum Squats")
        "hamstrings" -> listOf("Leg Curls", "Romanian Deadlifts", "Seated Leg Curls", "Glute Ham Raises", "Lying Leg Curls", "Nordic Curls", "Smith Machine RDLs", "Cable Pull-throughs")
        "glutes" -> listOf("Hip Thrusts", "Goblet Squats", "Glute Kickbacks", "Abductor Machines", "Cable Pull-throughs", "Glute Bridge Machines", "Reverse Hypers (Glute focus)", "Glute Presses (Machine)")
        "calves" -> listOf("Standing Calf Raises", "Seated Calf Raises", "Donkey Calf Raises", "Leg Press Calf Raises", "Calf Press on Leg Press", "Smith Machine Calf Raises", "Tibialis Raises")
        "hip adductors" -> listOf("Adductor Machines", "Sumo Squats", "Cable Adductions", "Seated Adduction Machines", "Copenhagen Planks")
        "hip abductors" -> listOf("Abductor Machines", "Lateral Cable Leg Raises", "Cable Abductions", "Seated Abduction Machines", "Side Lying Clamshells (Machine)")
        "shoulders" -> listOf("Military Presses", "Shoulder Press Machines", "Side Raises", "Face Pulls", "Cable Lateral Raises")
        "back" -> listOf("Lat Pulldowns", "Seated Rows", "Barbell Rows", "Deadlifts", "Pull-ups", "Chin-ups", "Inverted Rows")
        "legs" -> listOf("Barbell Squats", "Leg Presses", "Leg Curls", "Romanian Deadlifts", "Leg Extensions")
        "arms" -> listOf("Barbell Curls", "Tricep Pushdowns", "Skullcrushers", "Cable Curls", "Dips", "Chin-ups")
        "core" -> listOf("Cable Crunches", "Hanging Leg Raises", "Cable Woodchoppers", "Machine Crunches")
        else -> listOf("Deadlifts", "Squats", "Presses", "Pullovers")
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
