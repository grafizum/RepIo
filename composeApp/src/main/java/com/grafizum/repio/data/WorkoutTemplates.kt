package com.grafizum.repio.data

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

    fun generateAssistedProgram(
        equipment: List<String>, 
        bodyParts: List<String>, 
        selectedDays: List<String>,
        intensity: String,
        workoutGoal: String = "goal_bodybuilding",
        includedExercises: List<String> = emptyList(),
        excludedExercises: List<String> = emptyList(),
        minSets: Int = 0,
        maxSets: Int = 0
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
                    var available = getExercisesForStrict(equipment, part)
                    
                    // Filter out Pull-ups/Chin-ups from initial generation if no Gym is selected
                    // as they require a bar not guaranteed in bodyweight-only home settings.
                    if (!equipment.contains("Gym")) {
                        available = available.filter { 
                            !it.contains("Pull-up", true) && !it.contains("Chin-up", true) 
                        }
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
                                // If barbell is available, prioritize barbell CF movements
                                val bbCf = cf.filter { it.contains("Barbell", true) }
                                if (bbCf.isNotEmpty() && equipment.contains("Barbell")) bbCf else cf
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
                                it.contains("Copehagen", true) || it.contains("Pistol", true)
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
                                targetReps = getGoalReps(exName, workoutGoal, setsPerExercise),
                                targetIntensity = getGoalIntensity(workoutGoal, intensity, exName),
                                restSeconds = getGoalRestSeconds(workoutGoal, exName),
                                transitionRestSeconds = 90, 
                                videoUrl = "",
                                orderIndex = (partIndex * 10) + exercisesPicked
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
                                    targetReps = getGoalReps(exName, workoutGoal, setsPerExercise),
                                    targetIntensity = getGoalIntensity(workoutGoal, intensity, exName),
                                    restSeconds = getGoalRestSeconds(workoutGoal, exName),
                                    transitionRestSeconds = 90,
                                    videoUrl = "",
                                    orderIndex = (partIndex * 10) + exercisesPicked
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

        return GeneratedProgram(routineDays, routineExercises, unsatisfiedParts.distinct(), insights)
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
            "Easy" -> {
                "Light"
            }
            else -> {
                "Moderate"
            }
        }
    }

    fun getGoalReps(exerciseName: String, goal: String, forcedSets: Int = 0): String {
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
        
        return when {
            forcedSets >= 6 -> {
                "${kotlin.math.max(1, baseRange.first - 2)}–${kotlin.math.max(2, baseRange.last - 2)}"
            }
            forcedSets <= 2 && forcedSets > 0 -> {
                val compensatedStart = baseRange.first + 3
                val compensatedEnd = baseRange.last + 5
                "$compensatedStart–$compensatedEnd"
            }
            else -> "${baseRange.first}–${baseRange.last}"
        }
    }

    fun getAllExerciseNames(equipment: List<String> = emptyList()): List<String> {
        val muscles = getAllMuscleGroups()
        val all = mutableListOf<String>()
        muscles.forEach {
            if (equipment.contains("Gym")) {
                all.addAll(getBodyweightExercises(it))
                all.addAll(getDumbbellExercises(it))
                all.addAll(getBarbellExercises(it))
                all.addAll(getGymExercises(it))
            } else {
                if (equipment.contains("Bodyweight") || equipment.isEmpty()) all.addAll(getBodyweightExercises(it))
                if (equipment.contains("Dumbbell")) all.addAll(getDumbbellExercises(it))
                if (equipment.contains("Barbell")) all.addAll(getBarbellExercises(it))
            }
        }
        return all.distinct().sorted()
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
            mg.contains("back") || mg.contains("lat") || mg.contains("trap") || mg.contains("erector") -> "Back"
            mg.contains("leg") || mg.contains("quad") || mg.contains("hamstring") || mg.contains("glute") || mg.contains("calve") || mg.contains("adductor") || mg.contains("abductor") -> "Legs"
            mg.contains("shoulder") || mg.contains("delt") -> "Shoulders"
            mg.contains("bicep") || mg.contains("tricep") || mg.contains("arm") || mg.contains("forearm") -> "Arms"
            mg.contains("abs") || mg.contains("core") || mg.contains("oblique") || mg.contains("abdominal") -> "Core"
            else -> "Other"
        }
    }

    fun getAlternativeExercises(currentExerciseName: String, muscleGroup: String, equipment: List<String> = emptyList()): List<String> {
        val allForGroup = mutableListOf<String>()
        
        if (equipment.contains("Gym")) {
            allForGroup.addAll(getGymExercises(muscleGroup))
            allForGroup.addAll(getBarbellExercises(muscleGroup))
            allForGroup.addAll(getDumbbellExercises(muscleGroup))
            allForGroup.addAll(getBodyweightExercises(muscleGroup))
        } else {
            if (equipment.contains("Barbell")) allForGroup.addAll(getBarbellExercises(muscleGroup))
            if (equipment.contains("Dumbbell")) allForGroup.addAll(getDumbbellExercises(muscleGroup))
            if (equipment.contains("Bodyweight") || equipment.isEmpty()) allForGroup.addAll(getBodyweightExercises(muscleGroup))
        }
        
        return allForGroup.distinct()
            .filter { it.lowercase() != currentExerciseName.lowercase() }
            .sorted()
    }

    fun getMuscleGroupForExercise(name: String): String {
        val cleanName = name.trim().lowercase()

        val detailedGroups = listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back", "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs", "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Chest")
        for (g in detailedGroups) {
            if (getGymExercises(g).any { it.equals(name, ignoreCase = true) } || 
                getBarbellExercises(g).any { it.equals(name, ignoreCase = true) } || 
                getDumbbellExercises(g).any { it.equals(name, ignoreCase = true) } || 
                getBodyweightExercises(g).any { it.equals(name, ignoreCase = true) }) return g
        }
        
        return when {
            cleanName.contains("bench") || cleanName.contains("chest") || cleanName.contains("fly") -> "Chest"
            cleanName.contains("row") || cleanName.contains("lat") || cleanName.contains("pull") -> "Lats"
            cleanName.contains("curl") -> "Biceps"
            cleanName.contains("press") && (cleanName.contains("shoulder") || cleanName.contains("overhead")) -> "Shoulders"
            cleanName.contains("squat") || cleanName.contains("leg") -> "Quadriceps"
            cleanName.contains("abs") || cleanName.contains("crunch") || cleanName.contains("plank") -> "Abs"
            else -> "Other"
        }
    }

    private fun getExercisesForStrict(equipment: List<String>, bodyPart: String): List<String> {
        val pool = mutableListOf<String>()
        if (equipment.contains("Gym")) {
            pool.addAll(getGymExercises(bodyPart))
            // A gym also has barbells, dumbbells, and space for bodyweight
            pool.addAll(getBarbellExercises(bodyPart))
            pool.addAll(getDumbbellExercises(bodyPart))
            pool.addAll(getBodyweightExercises(bodyPart))
        } else {
            if (equipment.contains("Barbell")) pool.addAll(getBarbellExercises(bodyPart))
            if (equipment.contains("Dumbbell")) pool.addAll(getDumbbellExercises(bodyPart))
            if (equipment.contains("Bodyweight")) pool.addAll(getBodyweightExercises(bodyPart))
        }
        return pool.distinct()
    }

    private fun getBodyweightExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Push-up", "Incline Push-up", "Decline Push-up", "Diamond Push-up", "Archer Push-up", "Dip (Chair)", "Bench Dip")
        "front deltoids" -> listOf("Pike Push-up", "Shoulder Tap", "Handstand Push-up", "Dive Bomber Push-up", "Handstand Hold")
        "lateral deltoids" -> listOf("Plank-to-Down-Dog", "Side Plank Rotation", "Lateral Plank Walk", "Side Plank Leg Raise", "Wall Slide")
        "rear deltoids" -> listOf("Reverse Snow Angel", "Rear Delt Floor Fly (BW)")
        "lats" -> listOf("Superman", "Reverse Snow Angel")
        "upper back" -> listOf("Superman Extension", "Y-W-T Raise (Floor)", "Face Pull (BW/Towel)")
        "upper traps" -> listOf("Scapular Push-up", "Wall Slide", "Handstand Shrug")
        "spinal erectors" -> listOf("Superman", "Bridge", "Bird-Dog", "Back Extension", "Plank (Active)")
        "biceps" -> listOf("Hammer Curl (BW/Towel)")
        "triceps" -> listOf("Dip (Chair)", "Diamond Push-up", "Close-Grip Push-up", "Bench Dip", "Pseudo Planche Push-up")
        "forearms" -> listOf("Fist Squeeze", "Finger Push-up")
        "abs" -> listOf("Crunch", "Leg Raise", "Plank", "L-Sit", "V-Up", "Hollow Body Hold", "Mountain Climber", "Dragon Flag")
        "obliques" -> listOf("Russian Twist", "Bicycle Crunch", "Side Plank", "Windshield Wiper (Floor)", "Side Plank Crunch")
        "quadriceps" -> listOf("Squat", "Lunge", "Step-up", "Pistol Squat", "Bulgarian Split Squat (BW)", "Sissy Squat (BW)", "Wall Sit")
        "hamstrings" -> listOf("Glute Bridge", "Nordic Hamstring Curl", "Sliding Leg Curl", "Good Morning (BW)")
        "glutes" -> listOf("Glute Bridge", "Donkey Kick", "Fire Hydrant", "Frog Pump", "Bulgarian Split Squat (Glute Focus)")
        "calves" -> listOf("Calf Raise", "Jumping Jack", "Box Jump (BW)", "Donkey Calf Raise (BW)")
        "hip adductors" -> listOf("Side Lying Leg Raise", "Sumo Squat", "Copenhagen Plank")
        "hip abductors" -> listOf("Clamshell", "Side Step", "Dirty Dog", "Side Plank Leg Raise")
        "shoulders" -> listOf("Pike Push-up", "Shoulder Tap", "Handstand Hold", "Pike Hold")
        "back" -> listOf("Superman", "Bird-Dog")
        "legs" -> listOf("Squat", "Lunge", "Glute Bridge", "Pistol Squat")
        "arms" -> listOf("Dip (Chair)", "Inverted Curl")
        "core" -> listOf("Plank", "Leg Raise")
        else -> emptyList()
    }

    private fun getDumbbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Dumbbell Floor Press", "Dumbbell Fly", "Incline Dumbbell Press", "Decline Dumbbell Press", "Dumbbell Pullover (Chest Focus)")
        "front deltoids" -> listOf("Dumbbell Front Raise", "Dumbbell Shoulder Press", "Arnold Press", "Dumbbell Clean & Press", "Dumbbell Push Press")
        "lateral deltoids" -> listOf("Dumbbell Lateral Raise", "Leaning Lateral Raise", "Upright Dumbbell Row", "Dumbbell Y-Raise", "Side Lying Lateral Raise")
        "rear deltoids" -> listOf("Bent-over Rear Delt Fly", "Dumbbell Face Pull", "Chest Supported Rear Delt Fly", "Dumbbell Row")
        "lats" -> listOf("Dumbbell Row", "Dumbbell Pullover", "Seal Row (Dumbbell)", "Renegade Row", "Batwing Row", "Incline Dumbbell Row")
        "upper back" -> listOf("Dumbbell Row", "Dumbbell Face Pull", "Bent-over Shrug", "Chest Supported Dumbbell Row", "Dumbbell Kelso Row", "Double Dumbbell Row")
        "upper traps" -> listOf("Dumbbell Shrug", "Dumbbell Overhead Shrug", "Dumbbell Farmer's Shrug")
        "spinal erectors" -> listOf("Dumbbell Deadlift", "Dumbbell Back Extension", "Dumbbell Good Morning", "Dumbbell Romanian Deadlift", "Dumbbell Bird-Dog Row")
        "biceps" -> listOf("Dumbbell Curl", "Hammer Curl", "Incline Curl", "Concentration Curl", "Zottman Curl", "Spider Curl (Dumbbell)", "Waiter Curl", "Preacher Curl (Dumbbell)")
        "triceps" -> listOf("Dumbbell Overhead Extension", "Kickback", "Skullcrusher (Dumbbell)", "Close Grip Dumbbell Press", "Dumbbell Tate Press", "JM Press (Dumbbell)", "Dumbbell Floor Press (Close)")
        "forearms" -> listOf("Dumbbell Wrist Curl", "Dumbbell Reverse Curl", "Farmer's Walk (Dumbbell)", "Dumbbell Suitcase Carry", "Dumbbell Pinch Grip")
        "abs" -> listOf("Weighted Crunch", "Dumbbell Side Bend", "Weighted Leg Raise", "Dumbbell Russian Twist", "Dumbbell Woodchopper", "Dumbbell V-Up")
        "obliques" -> listOf("Dumbbell Russian Twist", "Dumbbell Side Bend", "Dumbbell Windmill", "Dumbbell Saxon Side Bend")
        "quadriceps" -> listOf("Dumbbell Squat", "Goblet Squat", "Dumbbell Lunge", "Bulgarian Split Squat (Dumbbell)", "Dumbbell Step-up", "Dumbbell Hack Squat", "Cyclist Squat (Dumbbell)")
        "hamstrings" -> listOf("Dumbbell Romanian Deadlift", "Dumbbell Leg Curl", "Dumbbell Stiff Leg Deadlift", "Dumbbell Good Morning")
        "glutes" -> listOf("Dumbbell Glute Bridge", "Dumbbell Sumo", "Dumbbell Hip Thrust", "Dumbbell Curtsy Lunge", "Dumbbell Frog Pump")
        "calves" -> listOf("Dumbbell Calf Raise", "Seated Dumbbell Calf Raise", "Dumbbell Tip-toe Walk")
        "hip adductors" -> listOf("Dumbbell Sumo Squat", "Copenhagen Plank (Weighted)", "Dumbbell Cossack Squat")
        "hip abductors" -> listOf("Side Lying Leg Raise", "Dumbbell Side Lunge", "Dumbbell Clamshell")
        "shoulders" -> listOf("Dumbbell Shoulder Press", "Dumbbell Lateral Raise", "Dumbbell Front Raise", "Thruster", "Arnold Press")
        "back" -> listOf("Dumbbell Row", "Dumbbell Pullover")
        "legs" -> listOf("Dumbbell Squat", "Goblet Squat", "Dumbbell Lunge", "Dumbbell Romanian Deadlift")
        "arms" -> listOf("Dumbbell Curl", "Hammer Curl", "Dumbbell Overhead Extension", "Kickback")
        "core" -> listOf("Weighted Crunch", "Dumbbell Side Bend", "Dumbbell Russian Twist")
        else -> listOf("Thruster", "Devil Press", "Dumbbell Snatch")
    }

    private fun getBarbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Press", "Barbell Floor Press", "Incline Barbell Press", "Decline Barbell Press", "Close Grip Bench Press", "Wide Grip Bench Press", "Guillotine Press (Barbell)", "Reverse Grip Bench Press", "Pin Press (Chest)")
        "front deltoids" -> listOf("Military Press", "Barbell Front Raise", "Overhead Press", "Push Press", "Behind the Neck Press", "Z-Press (Barbell)", "Sots Press", "Landmine Press (Barbell)", "Kloker Press")
        "lateral deltoids" -> listOf("Upright Row", "Wide Grip Military Press", "Behind the Neck Press", "Barbell Side Raise (Landmine)", "Snatch Grip High Pull", "Axle Side Raise")
        "rear deltoids" -> listOf("Barbell Row", "Face Pull (Barbell)", "Barbell Rear Delt Row", "Snatch Grip Row", "Rear Delt Meadows Row")
        "lats" -> listOf("Barbell Row", "Barbell Pullover", "Meadows Row", "Landmine Row", "Corner Row", "Yates Row", "Reverse Grip Barbell Row")
        "upper back" -> listOf("Barbell Row", "Pendlay Row", "T-Bar Row", "Yates Row", "Snatch Grip High Pull", "Barbell Shrug (Behind Back)", "Kloker Row", "Rack Pull (Upper)")
        "upper traps" -> listOf("Barbell Shrug", "Snatch Grip Shrug", "Behind the Back Shrug", "Power Shrug", "Zercher Shrug")
        "spinal erectors" -> listOf("Deadlift", "Barbell Good Morning", "Rack Pull", "Zercher Good Morning", "Jefferson Deadlift", "Barbell Back Extension", "Stiff Leg Deadlift")
        "biceps" -> listOf("Barbell Curl", "Reverse Barbell Curl", "Spider Curl", "Cheat Curl", "Drag Curl", "Zottman Curl (Barbell/Axle)", "Close Grip Barbell Curl", "Preacher Curl (Barbell)")
        "triceps" -> listOf("Close Grip Press", "Skullcrusher (Barbell)", "Barbell JM Press", "Floor Press (Close Grip)", "California Press", "Barbell Overhead Extension", "Board Press", "Pin Press (Triceps)")
        "forearms" -> listOf("Barbell Wrist Curl", "Reverse Barbell Wrist Curl", "Farmer's Walk (Barbell)", "Fat Bar Hold", "Plate Pinch", "Barbell Finger Curl")
        "abs" -> listOf("Barbell Rollout", "Landmine Rainbow", "Barbell Sit-up", "Barbell Leg Raise", "Weighted Barbell Crunch")
        "obliques" -> listOf("Barbell Side Bend", "Landmine Twist", "Zercher Side Bend", "Full Contact Twist (Landmine)", "Barbell Windmill")
        "quadriceps" -> listOf("Barbell Squat", "Front Squat", "Barbell Lunge", "Hack Squat (Barbell)", "Zercher Squat", "Barbell Step-up", "Pause Squat", "Anderson Squat", "Cyclist Squat (Barbell)", "Pin Squat")
        "hamstrings" -> listOf("Barbell Romanian Deadlift", "Barbell Stiff Leg Deadlift", "Zercher RDL", "Barbell Good Morning (Seated)", "Snatch Grip RDL", "Floating RDL")
        "glutes" -> listOf("Barbell Hip Thrust", "Deadlift", "Sumo Deadlift", "Barbell Glute Bridge", "Deficit Deadlift", "Barbell Kas Glute Bridge", "B-Stance RDL (Barbell)")
        "calves" -> listOf("Barbell Calf Raise", "Seated Barbell Calf Raise", "Barbell Donkey Calf Raise", "Barbell Tip-toe Walk")
        "hip adductors" -> listOf("Sumo Deadlift", "Sumo Squat (Barbell)", "Cossack Squat (Barbell)", "Adductor Pull (Landmine)")
        "hip abductors" -> listOf("Barbell Curtsey Lunge", "Barbell Side Lunge", "Landmine Lateral Lunge", "B-Stance Glute Bridge (BB)")
        "shoulders" -> listOf("Military Press", "Barbell Front Raise", "Upright Row", "Clean & Press", "Strict Press", "Kloker Press")
        "back" -> listOf("Barbell Row", "Deadlift", "Pendlay Row", "Barbell Pullover")
        "legs" -> listOf("Barbell Squat", "Barbell Romanian Deadlift", "Barbell Lunge", "Deadlift", "Front Squat")
        "arms" -> listOf("Barbell Curl", "Close Grip Press", "Skullcrusher (Barbell)", "Reverse Curl")
        "core" -> listOf("Barbell Rollout", "Barbell Side Bend", "Landmine Twist")
        else -> listOf("Clean & Press", "Snatch", "Clean & Jerk", "Hang Clean", "Power Snatch")
    }

    private fun getGymExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Press", "Cable Fly", "Incline Barbell Press", "Chest Press Machine", "Pec Deck", "Low Cable Fly", "Dip (Chest Focus)", "Weighted Dip", "Chest Dip (Bars)", "Machine Fly", "Cable Crossover", "Hammer Strength Chest Press", "Svend Press (Cable)", "Floor Press (Machine)")
        "front deltoids" -> listOf("Military Press", "Shoulder Press Machine", "Arnold Press", "Cable Front Raise", "Landmine Press", "Hammer Strength Shoulder Press", "Smith Machine Press")
        "lateral deltoids" -> listOf("Side Raise", "Cable Lateral Raise", "Machine Lateral Raise", "Behind-the-back Cable Lateral Raise", "Cross Cable Lateral Raise", "Y-Raise (Cable)", "Egyptian Lateral Raise (Cable)")
        "rear deltoids" -> listOf("Face Pull", "Rear Delt Deck", "Cable Rear Delt Fly", "Rope Face Pull", "Reverse Pec Deck", "Cable Row (Rear Delt Focus)")
        "lats" -> listOf("Lat Pulldown", "Seated Row", "Pull-up", "Chin-up", "Wide Grip Pull-up", "L-sit Pull-up", "Archer Pull-up", "V-Bar Pulldown", "Straight Arm Pulldown", "Hammer Strength Row", "Close Grip Lat Pulldown", "Assisted Pull-up Machine", "Australian Pull-up (Lats)", "Muscle-up (BW)")
        "upper back" -> listOf("Barbell Row", "T-Bar Row", "Face Pull", "Machine Row", "Inverted Row", "Scapular Pull-up", "Seated Wide Row", "Chest Supported Machine Row", "Cable Face Pull")
        "upper traps" -> listOf("Dumbbell Shrug", "Smith Machine Shrug", "Cable Shrug", "Machine Shrug", "Hise Shrug (Machine)")
        "spinal erectors" -> listOf("Deadlift", "Back Extension", "Reverse Hyper", "Good Morning (Machine)", "Smith Machine Deadlift")
        "biceps" -> listOf("Barbell Curl", "Preacher Curl", "Cable Curl", "Concentration Curl", "Bayesian Curl (Cable)", "Chin-up", "Hammer Cable Curl", "High Cable Curl", "Machine Curl", "Drag Curl (Cable)", "Inverted Curl (Bodyweight)", "Underhand Inverted Row")
        "triceps" -> listOf("Tricep Pushdown", "Skullcrusher", "Dip", "Rope Pushdown", "Machine Dip", "Weighted Dip", "Overhead Cable Extension", "V-Bar Pushdown", "Cable Kickback", "Smith Machine JM Press", "Dip (Bars)")
        "forearms" -> listOf("Reverse Curl", "Hammer Curl", "Wrist Roller", "Dead Hang", "Towel Hang", "Farmer's Walk", "Wrist Curl Machine", "Cable Wrist Curl", "False Grip Hold")
        "abs" -> listOf("Cable Crunch", "Hanging Leg Raise", "Machine Crunch", "Ab Wheel", "Captain's Chair Leg Raise", "Garhammer Raise", "Cable Knee Raise", "Rope Crunch", "Decline Sit-up (Weighted)")
        "obliques" -> listOf("Cable Woodchopper", "Russian Twist", "Side Plank Crunch", "Cable Side Bend", "Landmine Twist", "Cable Pallof Press", "Rotary Torso Machine", "Hanging Side Raise")
        "quadriceps" -> listOf("Barbell Squat", "Leg Press", "Leg Extension", "Hack Squat", "Sissy Squat", "Goblet Squat", "Smith Machine Squat", "V-Squat Machine", "Pendulum Squat")
        "hamstrings" -> listOf("Leg Curl", "Romanian Deadlift", "Seated Leg Curl", "Glute Ham Raise", "Lying Leg Curl", "Nordic Curl", "Smith Machine RDL", "Cable Pull-through")
        "glutes" -> listOf("Hip Thrust", "Glute Kickback", "Abductor Machine", "Cable Pull-through", "Glute Bridge Machine", "Reverse Hyper (Glute focus)", "Glute Press (Machine)")
        "calves" -> listOf("Standing Calf Raise", "Seated Calf Raise", "Donkey Calf Raise", "Leg Press Calf Raise", "Calf Press on Leg Press", "Smith Machine Calf Raise", "Tibialis Raise")
        "hip adductors" -> listOf("Adductor Machine", "Sumo Squat", "Cable Adduction", "Seated Adduction Machine", "Copenhagen Plank")
        "hip abductors" -> listOf("Abductor Machine", "Lateral Cable Leg Raise", "Cable Abduction", "Seated Abduction Machine", "Side Lying Clamshell (Machine)")
        "shoulders" -> listOf("Military Press", "Shoulder Press Machine", "Side Raise", "Face Pull", "Cable Lateral Raise")
        "back" -> listOf("Lat Pulldown", "Seated Row", "Barbell Row", "Deadlift", "Pull-up", "Chin-up", "Inverted Row")
        "legs" -> listOf("Barbell Squat", "Leg Press", "Leg Curl", "Romanian Deadlift", "Leg Extension")
        "arms" -> listOf("Barbell Curl", "Tricep Pushdown", "Skullcrusher", "Cable Curl", "Dip", "Chin-up")
        "core" -> listOf("Cable Crunch", "Hanging Leg Raise", "Cable Woodchopper", "Machine Crunch")
        else -> listOf("Deadlift", "Squat", "Press", "Pullover")
    }

    val template5DaySplit = WorkoutTemplate(
        id = "5day_hypertrophy",
        nameKey = "hypertrophy_5day_name",
        descriptionKey = "hypertrophy_5day_desc",
        badgeKey = "most_popular",
        daysCountKey = "workout_days_format",
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
        daysCountKey = "workout_days_format",
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell Floor Press", "Chest", 4, "8–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Lateral Raise", "Shoulders", 3, "12–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Overhead Extension", "Triceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Row", "Back", 4, "8–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Row", "Back", 3, "10–12", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Curl", "Biceps", 3, "10–12", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "TUESDAY", "Hammer Curl", "Biceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "WEDNESDAY", "Barbell Squat", "Legs", 4, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Romanian Deadlift", "Legs", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Lunge", "Legs", 3, "10 each leg", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Crunch", "Abs", 3, "15–20", "", 45, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Floor Press", "Chest", 4, "10–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Lateral Raise", "Shoulders", 4, "12–20", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dip", "Triceps", 3, "10–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell Row", "Back", 4, "8–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Pullover", "Back", 3, "10–15", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell Curl", "Biceps", 3, "8–12", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "SATURDAY", "Barbell Squat", "Legs", 4, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "SATURDAY", "Leg Raise", "Abs", 3, "12–20", "", 45, 90, "", 2)
        )
    )

    val templateUpperLower = WorkoutTemplate(
        id = "upper_lower_4day",
        nameKey = "upper_lower_4day_name",
        descriptionKey = "upper_lower_4day_desc",
        badgeKey = "optimal_recovery",
        daysCountKey = "workout_days_format",
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell Floor Press", "Chest", 4, "6–10", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Row", "Back", 4, "6–10", "", 120, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "8–12", "", 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Curl", "Biceps", 3, "8–12", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Squat", "Legs", 4, "6–10", "", 150, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Romanian Deadlift", "Legs", 3, "8–10", "", 120, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Plank", "Abs", 3, "45 sec", "", 45, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Floor Press", "Chest", 3, "10–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Row", "Back", 3, "10–12", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Lateral Raise", "Shoulders", 3, "12–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Hammer Curl", "Biceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Overhead Extension", "Triceps", 3, "10–15", "", 60, 90, "", 5),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Lunge", "Legs", 3, "10 each leg", "", 60, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Leg Raise", "Abs", 3, "12–20", "", 45, 90, "", 2)
        )
    )

    val templateFullBody = WorkoutTemplate(
        id = "fullbody_3day",
        nameKey = "fullbody_3day_name",
        descriptionKey = "fullbody_3day_desc",
        badgeKey = "time_efficient",
        daysCountKey = "workout_days_format",
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell Squat", "Legs", 3, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Floor Press", "Chest", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Row", "Back", 3, "8–12", "", 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Lateral Raise", "Shoulders", 3, "12–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "MONDAY", "Crunch", "Abs", 3, "15–20", "", 45, 90, "", 5),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Lunge", "Legs", 3, "10 each leg", "", 60, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Row", "Back", 3, "10–12", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Curl", "Biceps", 3, "10–12", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Romanian Deadlift", "Legs", 3, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Floor Press", "Chest", 3, "10–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Hammer Curl", "Biceps", 3, "10–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Overhead Extension", "Triceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Leg Raise", "Abs", 3, "12–20", "", 45, 90, "", 5)
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
        return when {
            cleanName.contains("squat") || cleanName.contains("deadlift") || 
            cleanName.contains("bench") || cleanName.contains("press") ||
            cleanName.contains("row") || cleanName.contains("pull-up") -> "8–12"
            muscleGroup in listOf("Biceps", "Triceps", "Lateral Deltoids", "Rear Deltoids", "Calves") -> "10–15"
            muscleGroup == "Abs" || muscleGroup == "Obliques" -> "15–20"
            else -> "8–12"
        }
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
            (lowerName.contains("lunge") && !lowerName.contains("db") && !lowerName.contains("barbell")) ||
            lowerName.contains("(bw)") || 
            lowerName.contains("bodyweight")
        ) return true

        // 2. Check against the strict bodyweight lists defined in getBodyweightExercises
        // We can do a quick check for common muscle groups first to avoid a full flatMap if possible,
        // but for robustness we check all.
        val allBW = getAllMuscleGroups().flatMap { getBodyweightExercises(it) }
        return allBW.any { it.equals(name, ignoreCase = true) }
    }
}
