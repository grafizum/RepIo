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
                            getMuscleGroupForExercise(it) == part && it !in usedIncluded 
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

    fun getAllExerciseNames(): List<String> {
        val muscles = getAllMuscleGroups()
        val all = mutableListOf<String>()
        muscles.forEach {
            all.addAll(getBodyweightExercises(it))
            all.addAll(getDumbbellExercises(it))
            all.addAll(getBarbellExercises(it))
            all.addAll(getGymExercises(it))
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

    fun getAlternativeExercises(currentExerciseName: String, muscleGroup: String): List<String> {
        val allForGroup = getGymExercises(muscleGroup) + 
                         getBarbellExercises(muscleGroup) + 
                         getDumbbellExercises(muscleGroup) + 
                         getBodyweightExercises(muscleGroup)
        
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
        "chest" -> listOf("Push-ups", "Incline Push-ups", "Decline Push-ups", "Diamond Push-ups", "Archer Push-ups", "One-arm Push-ups (Assisted)", "Chest Dips (Bars)")
        "front deltoids" -> listOf("Pike Push-ups", "Shoulder Taps", "Handstand Push-ups", "Dive Bomber Push-ups", "Handstand Holds")
        "lateral deltoids" -> listOf("Plank-to-Down-Dog", "Side Plank Rotations", "Lateral Plank Walks", "Side Plank Leg Raises", "Wall Slides")
        "rear deltoids" -> listOf("Reverse Snow Angels", "Rear Delt Floor Flyes (BW)", "Scapular Pull-ups")
        "lats" -> listOf("Pull-ups", "Chin-ups", "Australian Pull-ups (Lats)", "Supermans", "Reverse Snow Angels", "Muscle-ups (BW)")
        "upper back" -> listOf("Inverted Rows", "Superman Extensions", "Y-W-T Raises (Floor)", "Scapular Pull-ups", "Face Pulls (BW/Towel)")
        "upper traps" -> listOf("Scapular Push-ups", "Wall Slides", "Handstand Shrugs")
        "spinal erectors" -> listOf("Supermans", "Bridges", "Bird-Dogs", "Reverse Hyperextensions (Floor)", "Plank (Active)")
        "biceps" -> listOf("Inverted Curls (Bodyweight)", "Chin-ups", "Underhand Inverted Rows", "Hammer Curls (BW/Towel)")
        "triceps" -> listOf("Dips (Bars)", "Dips (Chair)", "Diamond Push-ups", "Close-Grip Push-ups", "Bench Dips", "Pseudo Planche Push-ups")
        "forearms" -> listOf("Fist Squeezes", "Finger Push-ups", "Dead Hangs", "False Grip Holds")
        "abs" -> listOf("Crunches (sit-ups)", "Leg Raises", "Plank", "Hanging Leg Raises", "L-Sits", "V-Ups", "Hollow Body Holds", "Mountain Climbers", "Dragon Flags")
        "obliques" -> listOf("Russian Twists", "Bicycle Crunches", "Side Planks", "Windshield Wipers (Floor)", "Side Plank Crunches", "Hanging Side Raises")
        "quadriceps" -> listOf("Squats", "Lunges", "Step-ups", "Pistol Squats", "Bulgarian Split Squats (BW)", "Sissy Squats (BW)", "Wall Sits")
        "hamstrings" -> listOf("Glute Bridges", "Nordic Hamstring Curls", "Single Leg Bridges", "Sliding Leg Curls", "Good Mornings (BW)")
        "glutes" -> listOf("Glute Bridges", "Donkey Kicks", "Fire Hydrants", "Single Leg Glute Bridges", "Frog Pumps", "Bulgarian Split Squat (Glute Focus)")
        "calves" -> listOf("Calf Raises", "Single Leg Calf Raises", "Jumping Jacks", "Box Jumps (BW)", "Donkey Calf Raises (BW)")
        "hip adductors" -> listOf("Side Lying Leg Raises", "Sumo Squats", "Copenhagen Planks")
        "hip abductors" -> listOf("Clamshells", "Side Steps", "Dirty Dogs", "Side Plank Leg Raises")
        "shoulders" -> listOf("Pike Push-ups", "Shoulder Taps", "Handstand Holds", "Pike Hold")
        "back" -> listOf("Pull-ups", "Inverted Rows", "Supermans", "Bird-Dogs")
        "legs" -> listOf("Squats", "Lunges", "Glute Bridges", "Pistol Squats")
        "arms" -> listOf("Chin-ups", "Dips (Bars)", "Inverted Curls")
        "core" -> listOf("Plank", "Leg Raises", "Hanging Leg Raises")
        else -> emptyList()
    }

    private fun getDumbbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Dumbbell Floor Press", "Dumbbell Flyes", "Incline DB Press", "Decline DB Press", "One-arm DB Floor Press", "Around the Worlds (DB)", "Dumbbell Pullover (Chest Focus)")
        "front deltoids" -> listOf("Dumbbell Front Raises", "Dumbbell Shoulder Press", "Arnold Press", "One-arm Arnold Press", "DB Clean & Press", "DB Push Press")
        "lateral deltoids" -> listOf("Lateral Raises", "Leaning Lateral Raises", "Upright DB Rows", "One-arm Lateral Raises", "DB Y-Raises", "Side Lying Lateral Raise")
        "rear deltoids" -> listOf("Bent-over Rear Delt Flyes", "Dumbbell Face Pulls", "Chest Supported Rear Delt Flyes", "DB Rear Delt Row")
        "lats" -> listOf("One-arm Dumbbell Row", "Dumbbell Pullover", "Seal Rows (DB)", "Renegade Rows", "Batwing Rows", "Incline DB Rows")
        "upper back" -> listOf("Dumbbell Rows", "Face Pulls (with DBs)", "Bent-over Shrugs", "Chest Supported DB Rows", "DB Kelso Rows", "Double DB Row")
        "upper traps" -> listOf("Dumbbell Shrugs", "DB Overhead Shrugs", "DB Farmer's Shrugs")
        "spinal erectors" -> listOf("Dumbbell Deadlifts", "DB Back Extensions", "DB Good Mornings", "DB Romanian Deadlifts", "DB Bird-Dog Row")
        "biceps" -> listOf("Dumbbell Curls", "Hammer Curls", "Incline Curls", "Concentration Curls", "Zottman Curls", "Spider Curls (DB)", "Waiter Curls", "Preacher Curls (DB)")
        "triceps" -> listOf("Overhead Extension", "Kickbacks", "Skullcrushers (DB)", "Close Grip DB Press", "DB Tate Press", "JM Press (DB)", "Floor Press (DB Close)")
        "forearms" -> listOf("Wrist Curls", "Reverse Curls", "Farmer's Walks (DB)", "DB Suitcase Carry", "DB Pinch Grips")
        "abs" -> listOf("Weighted Crunches", "DB Side Bends", "Weighted Leg Raises", "DB Russian Twists", "DB Woodchoppers", "DB V-Ups")
        "obliques" -> listOf("Russian Twists (with DB)", "DB Side Bends", "DB Windmills", "DB Saxon Side Bends")
        "quadriceps" -> listOf("Dumbbell Squats", "Goblet Squats", "DB Lunges", "Bulgarian Split Squats (DB)", "DB Step-ups", "DB Hack Squats", "Cyclist Squats (DB)")
        "hamstrings" -> listOf("DB Romanian Deadlifts", "Leg Curls (DB)", "Single Leg DB Deadlifts", "DB Stiff Leg Deadlifts", "DB Good Mornings")
        "glutes" -> listOf("DB Glute Bridges", "DB Sumos", "DB Hip Thrusts", "Single Leg DB Hip Thrusts", "DB Curtsy Lunges", "DB Frog Pumps")
        "calves" -> listOf("DB Calf Raises", "Single Leg DB Calf Raises", "Seated DB Calf Raises", "DB Tip-toe Walks")
        "hip adductors" -> listOf("Sumo Squats (DB)", "Copenhagen Plank (Weighted)", "DB Cossack Squats")
        "hip abductors" -> listOf("Side Lying Leg Raises (with weight)", "DB Side Lunges", "DB Clamshells")
        "shoulders" -> listOf("Dumbbell Shoulder Press", "Lateral Raises", "Front Raises", "Thrusters", "Arnold Press")
        "back" -> listOf("Dumbbell Rows", "One-arm Dumbbell Row", "Dumbbell Pullover")
        "legs" -> listOf("Dumbbell Squats", "Goblet Squats", "DB Lunges", "DB Romanian Deadlifts")
        "arms" -> listOf("Dumbbell Curls", "Hammer Curls", "Overhead Extension", "Kickbacks")
        "core" -> listOf("Weighted Crunches", "DB Side Bends", "Russian Twists (with DB)")
        else -> listOf("Thrusters", "Devil Press", "DB Snatches")
    }

    private fun getBarbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Press", "Barbell Floor Press", "Incline Barbell Press", "Decline Barbell Press", "Close Grip Bench Press", "Wide Grip Bench Press", "Guillotine Press (Barbell)", "Reverse Grip Bench Press", "Pin Press (Chest)")
        "front deltoids" -> listOf("Military Press", "Barbell Front Raises", "Overhead Press", "Push Press", "Behind the Neck Press", "Z-Press (Barbell)", "Sots Press", "Landmine Press (Barbell)", "Kloker Press")
        "lateral deltoids" -> listOf("Upright Rows", "Wide Grip Military Press", "Behind the Neck Press", "Barbell Side Raises (Landmine)", "Snatch Grip High Pull", "Axle Side Raises")
        "rear deltoids" -> listOf("Bent-over Barbell Rows", "Face Pulls (Barbell)", "Barbell Rear Delt Row", "Snatch Grip Row", "Rear Delt Meadows Row")
        "lats" -> listOf("Barbell Row", "Barbell Pullovers", "Meadows Rows", "Landmine Rows", "Corner Rows", "Yates Row", "Reverse Grip Barbell Row", "One-arm Landmine Row")
        "upper back" -> listOf("Barbell Row", "Pendlay Rows", "T-Bar Row", "Yates Rows", "Snatch Grip High Pulls", "Barbell Shrugs (Behind Back)", "Kloker Row", "Rack Pulls (Upper)")
        "upper traps" -> listOf("Barbell Shrugs", "Snatch Grip Shrugs", "Behind the Back Shrugs", "Power Shrugs", "Zercher Shrugs")
        "spinal erectors" -> listOf("Deadlifts", "Barbell Good Mornings", "Rack Pulls", "Zercher Good Mornings", "Jefferson Deadlift", "Barbell Back Extension", "Stiff Leg Deadlift")
        "biceps" -> listOf("Barbell Curls", "Reverse Barbell Curls", "Spider Curls", "Cheat Curls", "Drag Curls", "Zottman Curl (Barbell/Axle)", "Close Grip Barbell Curl", "Preacher Curl (Barbell)")
        "triceps" -> listOf("Close Grip Press", "Skullcrushers (Barbell)", "Barbell JM Press", "Floor Press (Close Grip)", "California Press", "Barbell Overhead Extension", "Board Press", "Pin Press (Triceps)")
        "forearms" -> listOf("Barbell Wrist Curls", "Reverse Barbell Wrist Curls", "Farmer's Walks (Barbell)", "Fat Bar Holds", "Plate Pinches", "Barbell Finger Curls")
        "abs" -> listOf("Barbell Rollouts", "Landmine Rainbows", "Barbell Sit-ups", "Barbell Leg Raises", "Weighted Barbell Crunch")
        "obliques" -> listOf("Barbell Side Bends", "Landmine Twists", "Zercher Side Bends", "Full Contact Twists (Landmine)", "Barbell Windmill")
        "quadriceps" -> listOf("Barbell Squats", "Front Squats", "Barbell Lunges", "Hack Squats (Barbell)", "Zercher Squats", "Barbell Step-ups", "Pause Squats", "Anderson Squats", "Cyclist Squats (Barbell)", "Pin Squats")
        "hamstrings" -> listOf("Barbell Romanian Deadlifts", "Barbell Stiff Leg Deadlifts", "Zercher RDLs", "Barbell Good Mornings (Seated)", "Snatch Grip RDL", "Floating RDLs")
        "glutes" -> listOf("Barbell Hip Thrusts", "Deadlifts", "Sumo Deadlifts", "Barbell Glute Bridges", "Deficit Deadlifts", "Barbell Kas Glute Bridge", "B-Stance RDL (Barbell)")
        "calves" -> listOf("Barbell Calf Raises", "Seated Barbell Calf Raises", "Barbell Donkey Calf Raises", "Barbell Tip-toe Walks")
        "hip adductors" -> listOf("Sumo Deadlifts", "Sumo Squats (Barbell)", "Cossack Squats (Barbell)", "Adductor Pulls (Landmine)")
        "hip abductors" -> listOf("Barbell Curtsey Lunges", "Barbell Side Lunges", "Landmine Lateral Lunge", "B-Stance Glute Bridge (BB)")
        "shoulders" -> listOf("Military Press", "Barbell Front Raises", "Upright Rows", "Clean & Press", "Strict Press", "Kloker Press")
        "back" -> listOf("Barbell Row", "Deadlifts", "Pendlay Rows", "Barbell Pullover")
        "legs" -> listOf("Barbell Squats", "Barbell Romanian Deadlifts", "Barbell Lunges", "Deadlifts", "Front Squats")
        "arms" -> listOf("Barbell Curls", "Close Grip Press", "Skullcrushers (Barbell)", "Reverse Curls")
        "core" -> listOf("Barbell Rollouts", "Barbell Side Bends", "Landmine Twists")
        else -> listOf("Clean & Press", "Snatch", "Clean & Jerk", "Hang Clean", "Power Snatch")
    }

    private fun getGymExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Bench Press", "Cable Flyes", "Incline Press", "Chest Press Machine", "Pec Deck", "Low Cable Flyes", "Dips (Chest Focus)", "Weighted Dips", "Machine Flyes", "Cable Crossover", "Hammer Strength Chest Press", "Svend Press (Cable)", "Floor Press (Machine)")
        "front deltoids" -> listOf("Military Press", "Shoulder Press Machine", "Arnold Press", "Cable Front Raises", "Landmine Press", "Hammer Strength Shoulder Press", "Smith Machine Press")
        "lateral deltoids" -> listOf("Side Raises", "Cable Lateral Raises", "Machine Lateral Raises", "Behind-the-back Cable Lateral Raises", "Cross Cable Lateral Raises", "Y-Raises (Cable)", "Egyptian Lateral Raise (Cable)")
        "rear deltoids" -> listOf("Face Pulls", "Rear Delt Deck", "Cable Rear Delt Flyes", "Rope Face Pulls", "Reverse Pec Deck", "Single-arm Cable Rear Delt Fly", "Cable Row (Rear Delt Focus)")
        "lats" -> listOf("Lat Pulldown", "Seated Row", "Pull-ups", "Chin-ups", "Wide Grip Pull-ups", "L-sit Pull-ups", "Archer Pull-ups", "V-Bar Pulldown", "Straight Arm Pulldowns", "One-arm Lat Pulldown", "Hammer Strength Row", "Close Grip Lat Pulldown", "Assisted Pull-up Machine", "Single-arm Cable Row")
        "upper back" -> listOf("Barbell Row", "T-Bar Row", "Face Pulls", "Machine Rows", "One-arm Seated Row", "Inverted Rows", "Scapular Pull-ups", "Seated Wide Row", "Chest Supported Machine Row", "Cable Face Pulls")
        "upper traps" -> listOf("Dumbbell Shrugs", "Smith Machine Shrugs", "Cable Shrugs", "Machine Shrugs", "Hise Shrugs (Machine)")
        "spinal erectors" -> listOf("Deadlifts", "Back Extensions Machine", "Hyper-extensions", "Reverse Hypers", "Good Mornings (Machine)", "Smith Machine Deadlifts")
        "biceps" -> listOf("Barbell Curls", "Preacher Curls", "Cable Curls", "Concentration Curls", "Bayesian Curls (Cable)", "Chin-ups", "Hammer Cable Curls", "High Cable Curls", "Machine Curls", "Drag Curls (Cable)")
        "triceps" -> listOf("Tricep Pushdowns", "Skullcrushers", "Dips", "Rope Pushdowns", "Machine Dips", "One-arm Cable Pushdowns", "Weighted Dips", "Overhead Cable Extensions", "V-Bar Pushdowns", "Cable Kickbacks", "Smith Machine JM Press")
        "forearms" -> listOf("Reverse Curls", "Hammer Curls", "Wrist Roller", "Dead Hangs", "Towel Hangs", "Farmer's Walks", "Wrist Curl Machine", "Cable Wrist Curls")
        "abs" -> listOf("Cable Crunches", "Hanging Leg Raises", "Machine Crunches", "Ab Wheel", "Captain's Chair Leg Raises", "Garhammer Raise", "Cable Knee Raises", "Rope Crunches", "Decline Sit-ups (Weighted)")
        "obliques" -> listOf("Cable Woodchoppers", "Russian Twists", "Side Plank Crunches", "Cable Side Bends", "Landmine Twists", "Cable Pallof Press", "Rotary Torso Machine")
        "quadriceps" -> listOf("Barbell Squats", "Leg Press", "Leg Extensions", "Hack Squats", "Sissy Squats", "Goblet Squats", "Smith Machine Squats", "V-Squat Machine", "Pendulum Squat", "Leg Press (Single Leg)")
        "hamstrings" -> listOf("Leg Curls", "Romanian Deadlifts", "Seated Leg Curls", "Glute Ham Raise", "Lying Leg Curls", "Nordic Curls", "Smith Machine RDL", "Cable Pull-throughs")
        "glutes" -> listOf("Hip Thrusts", "Glute Kickbacks", "Abductor Machine", "Cable Pull-throughs", "Glute Bridge Machine", "Reverse Hyper (Glute focus)", "Glute Press (Machine)")
        "calves" -> listOf("Standing Calf Raises", "Seated Calf Raises", "Donkey Calf Raises", "Leg Press Calf Raises", "Calf Press on Leg Press", "Smith Machine Calf Raises", "Tibialis Raises")
        "hip adductors" -> listOf("Adductor Machine", "Sumo Squats", "Cable Adductions", "Seated Adduction Machine", "Copenhagen Plank")
        "hip abductors" -> listOf("Abductor Machine", "Lateral Cable Leg Raises", "Cable Abductions", "Seated Abduction Machine", "Side Lying Clamshells (Machine)")
        "shoulders" -> listOf("Military Press", "Shoulder Press Machine", "Side Raises", "Face Pulls", "Cable Lateral Raise")
        "back" -> listOf("Lat Pulldown", "Seated Row", "Barbell Row", "Deadlifts", "Pull-ups", "Inverted Rows")
        "legs" -> listOf("Barbell Squats", "Leg Press", "Leg Curls", "Romanian Deadlifts", "Leg Extensions")
        "arms" -> listOf("Barbell Curls", "Tricep Pushdowns", "Skullcrushers", "Cable Curls", "Dips", "Chin-ups")
        "core" -> listOf("Cable Crunches", "Hanging Leg Raises", "Cable Woodchoppers", "Machine Crunches")
        else -> listOf("Deadlifts", "Squats", "Presses", "Pullovers")
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell floor press", "Chest", 4, "8–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell shoulder press", "Shoulders", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell lateral raises", "Shoulders", 3, "12–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Overhead tricep extension", "Triceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell bent-over row", "Back", 4, "8–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "One-arm dumbbell row", "Back", 3, "10–12", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell curls", "Biceps", 3, "10–12", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "TUESDAY", "Hammer curls", "Biceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "WEDNESDAY", "Barbell squats", "Legs", 4, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Romanian deadlifts", "Legs", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell lunges", "Legs", 3, "10 each leg", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Crunches (sit-ups)", "Abs", 3, "15–20", "", 45, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell floor press", "Chest", 4, "10–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell lateral raises", "Shoulders", 4, "12–20", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Tricep dips / extensions", "Triceps", 3, "10–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell row", "Back", 4, "8–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell pullover", "Back", 3, "10–15", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell curls", "Biceps", 3, "8–12", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "SATURDAY", "Barbell squats", "Legs", 4, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "SATURDAY", "Leg raises", "Abs", 3, "12–20", "", 45, 90, "", 2)
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell floor press", "Chest", 4, "6–10", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell bent-over row", "Back", 4, "6–10", "", 120, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell shoulder press", "Shoulders", 3, "8–12", "", 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Barbell curls", "Biceps", 3, "8–12", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell squats", "Legs", 4, "6–10", "", 150, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Romanian deadlifts", "Legs", 3, "8–10", "", 120, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Plank", "Abs", 3, "45 sec", "", 45, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell floor press", "Chest", 3, "10–12", "", 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "One-arm dumbbell row", "Back", 3, "10–12", "", 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell lateral raises", "Shoulders", 3, "12–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Hammer curls", "Biceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Overhead tricep extension", "Triceps", 3, "10–15", "", 60, 90, "", 5),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell lunges", "Legs", 3, "10 each leg", "", 60, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Leg raises", "Abs", 3, "12–20", "", 45, 90, "", 2)
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell squats", "Legs", 3, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell floor press", "Chest", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Barbell bent-over row", "Back", 3, "8–12", "", 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell lateral raises", "Shoulders", 3, "12–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "MONDAY", "Crunches (sit-ups)", "Abs", 3, "15–20", "", 45, 90, "", 5),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell lunges", "Legs", 3, "10 each leg", "", 60, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell shoulder press", "Shoulders", 3, "8–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "One-arm dumbbell row", "Back", 3, "10–12", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell curls", "Biceps", 3, "10–12", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Romanian deadlifts", "Legs", 3, "8–12", "", 120, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell floor press", "Chest", 3, "10–12", "", 90, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Hammer curls", "Biceps", 3, "10–15", "", 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Overhead tricep extension", "Triceps", 3, "10–15", "", 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Leg raises", "Abs", 3, "12–20", "", 45, 90, "", 5)
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
            lowerName.contains("squat") && !lowerName.contains("barbell") && !lowerName.contains("dumbbell") && !lowerName.contains("machine") && !lowerName.contains("hack") && !lowerName.contains("goblet") ||
            lowerName.contains("lunge") && !lowerName.contains("db") && !lowerName.contains("barbell") ||
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
