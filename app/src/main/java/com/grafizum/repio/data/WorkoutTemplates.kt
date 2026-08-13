package com.grafizum.repio.data

import android.app.Application
import com.grafizum.repio.R

data class DayTemplate(
    val dayOfWeek: String,
    val titleResId: Int,
    val descResId: Int,
    val isRestDay: Boolean
)

data class WorkoutTemplate(
    val id: String,
    val nameResId: Int,
    val descriptionResId: Int,
    val badgeResId: Int,
    val daysCountResId: Int,
    val daysCountArgs: Pair<Int, Int>, // (Workout Days, Rest Days)
    val dayTemplates: List<DayTemplate>,
    val exercises: List<RoutineExerciseEntity>
)

object WorkoutTemplates {

    // Science-based volume recommendations (Sets per week)
    val volumeRecommendations = mapOf(
        "Chest" to "10–20 sets (Indirect: Triceps, Front Delts)",
        "Front Deltoids" to "6–10 sets (High indirect work from Chest Press)",
        "Lateral Deltoids" to "10–20 sets (Isolation highly recommended)",
        "Rear Deltoids" to "10–20 sets (Indirect: Rows/Pullups)",
        "Lats" to "10–20 sets (Vertical pulling focus)",
        "Upper Back" to "10–20 sets (Horizontal pulling focus)",
        "Upper Traps" to "4–10 sets (Indirect: Deadlifts/Rows)",
        "Spinal Erectors" to "4–10 sets (Indirect: Deadlifts/Squats)",
        "Biceps" to "8–15 direct sets (Indirect: Back work)",
        "Triceps" to "8–15 direct sets (Indirect: Chest/Shoulder work)",
        "Forearms" to "4–10 sets (High indirect work from all pulling)",
        "Abs" to "5–15 sets (Indirect: Compound stability)",
        "Obliques" to "4–10 sets (Focus on lateral stability)",
        "Glutes" to "6–15 sets (Indirect: Squats/Deadlifts)",
        "Quadriceps" to "10–20 sets (Squat/Press dominant)",
        "Hamstrings" to "6–15 sets (Hinge dominant)",
        "Calves" to "8–15 sets (High frequency often better)",
        "Hip Adductors" to "4–10 sets (Indirect: Deep Squats)",
        "Hip Abductors" to "4–10 sets (Isolation for hip health)"
    )

    // Master Workouts: Groups of related exercises for automatic variation
    val masterWorkouts = mapOf(
        "Push-ups (Master)" to listOf("Push-ups", "Incline Push-ups", "Diamond Push-ups", "Wide Push-ups", "Archer Push-ups", "Decline Push-ups"),
        "Squats (Master)" to listOf("Squats", "Sumo Squats", "Bulgarian Split Squats", "Goblet Squats", "Jump Squats"),
        "Dumbbell Press (Master)" to listOf("Dumbbell Floor Press", "Incline DB Press", "Dumbbell Shoulder Press", "Dumbbell Flyes"),
        "Barbell Press (Master)" to listOf("Barbell Bench Press", "Incline Barbell Press", "Military Press", "Close Grip Press"),
        "Rows (Master)" to listOf("Barbell Row", "One-arm Dumbbell Row", "Seated Row", "Pendlay Rows"),
        "Plank (Master)" to listOf("Plank", "Side Plank", "Plank-to-Down-Dog", "Shoulder Taps"),
        "Deadlift (Master)" to listOf("Deadlifts", "Romanian Deadlifts", "Sumo Deadlifts", "Stiff-Legged Deadlifts"),
        "Curls (Master)" to listOf("Barbell Curls", "Dumbbell Curls", "Hammer Curls", "Preacher Curls", "Reverse Curls")
    )

    fun resolveMasterWorkout(name: String): String {
        return if (name.endsWith("(Master)")) {
            val variations = masterWorkouts[name] ?: return name.removeSuffix(" (Master)")
            // Pick a "best" or random variation. 
            // In a real app, this could be based on history or muscle balance.
            // For now, we'll pick one based on the current day's hash to keep it consistent for the session.
            val seed = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
            variations[seed % variations.size]
        } else {
            name
        }
    }

    data class GeneratedProgram(
        val days: List<RoutineDayEntity>,
        val exercises: List<RoutineExerciseEntity>,
        val unsatisfiedParts: List<String>
    )

    // Helper to generate a dynamic program based on user input
    fun generateAssistedProgram(
        getApplication: Application,
        equipment: List<String>,
        bodyParts: List<String>,
        daysCount: Int,
        intensity: String,
        includedExercises: List<String> = emptyList(),
        excludedExercises: List<String> = emptyList()
    ): GeneratedProgram {
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val routineDays = mutableListOf<RoutineDayEntity>()
        val routineExercises = mutableListOf<RoutineExerciseEntity>()
        val unsatisfiedParts = mutableListOf<String>()

        if (bodyParts.isEmpty()) return GeneratedProgram(emptyList(), emptyList(), emptyList())

        // 1. Determine which days are workout days
        val workoutDayIndices = when(daysCount) {
            1 -> listOf(0)
            2 -> listOf(1, 4)
            3 -> listOf(0, 2, 4)
            4 -> listOf(0, 1, 3, 4)
            5 -> listOf(0, 1, 2, 4, 5)
            6 -> listOf(0, 1, 2, 3, 4, 5)
            7 -> (0..6).toList()
            else -> listOf(0, 2, 4)
        }

        // 2. Map body parts to workout days
        val partsPerDay = List(workoutDayIndices.size) { mutableListOf<String>() }
        bodyParts.forEachIndexed { index, part ->
            partsPerDay[index % workoutDayIndices.size].add(part)
        }

        // Adjust sets based on intensity
        val setsPerExercise = when(intensity) {
            "Easy" -> 2
            "Difficult" -> 4
            else -> 3
        }

        // Track which included exercises have been used to avoid duplicates
        val usedIncluded = mutableSetOf<String>()

        days.forEachIndexed { index, dayName ->
            val workoutIndex = workoutDayIndices.indexOf(index)
            val isWorkout = workoutIndex != -1
            
            if (isWorkout) {
                val currentParts = partsPerDay[workoutIndex]
                val satisfiedPartsOnThisDay = mutableListOf<String>()
                
                // 3. Pick exercises per body part for this day
                currentParts.forEachIndexed { partIndex, part ->
                    var available = getExercisesForStrict(equipment, part)
                    
                    // Apply exclusions
                    available = available.filter { it !in excludedExercises }

                    if (available.isEmpty() && includedExercises.none { getMuscleGroupForExercise(it) == part }) {
                        unsatisfiedParts.add(part)
                    } else {
                        satisfiedPartsOnThisDay.add(part)
                        // MAJOR groups get more exercises
                        val isMajor = part in listOf("Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes")
                        val exerciseLimit = if (isMajor && available.size >= 2) 2 else 1
                        
                        var exercisesPicked = 0
                        
                        // First, try to pick from included exercises that match this body part
                        val matchingIncluded = includedExercises.filter { 
                            getMuscleGroupForExercise(it) == part && it !in usedIncluded 
                        }
                        
                        matchingIncluded.take(exerciseLimit).forEach { exName ->
                            routineExercises.add(RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayName,
                                name = exName,
                                muscleGroup = part,
                                targetSets = setsPerExercise,
                                targetReps = if (exName.contains("Plank", ignoreCase = true)) "30 sec" else "8-12",
                                videoUrl = "",
                                orderIndex = (partIndex * 10) + exercisesPicked
                            ))
                            usedIncluded.add(exName)
                            exercisesPicked++
                        }

                        // Then, fill the rest from the available pool
                        if (exercisesPicked < exerciseLimit) {
                            available.filter { it !in usedIncluded }.take(exerciseLimit - exercisesPicked).forEach { exName ->
                                routineExercises.add(RoutineExerciseEntity(
                                    id = 0,
                                    dayOfWeek = dayName,
                                    name = exName,
                                    muscleGroup = part,
                                    targetSets = setsPerExercise,
                                    targetReps = if (exName.contains("Plank", ignoreCase = true)) "30 sec" else "8-12",
                                    videoUrl = "",
                                    orderIndex = (partIndex * 10) + exercisesPicked
                                ))
                                exercisesPicked++
                            }
                        }
                    }
                }

                val partsLabel = satisfiedPartsOnThisDay.joinToString(", ")
                routineDays.add(RoutineDayEntity(
                    dayOfWeek = dayName,
                    title = if (satisfiedPartsOnThisDay.isNotEmpty()) "$dayName — $partsLabel" else "$dayName — Recovery",
                    description = getApplication.getString(R.string.push_desc),
                    isRestDay = satisfiedPartsOnThisDay.isEmpty()
                ))

            } else {
                routineDays.add(RoutineDayEntity(
                    dayOfWeek = dayName,
                    title = "$dayName — Rest",
                    description = getApplication.getString(R.string.rest_desc),
                    isRestDay = true
                ))
            }
        }

        return GeneratedProgram(routineDays, routineExercises, unsatisfiedParts.distinct())
    }

    fun getAllExerciseNames(): List<String> {
        val groups = getAllMuscleGroups()
        val individual = groups.flatMap { g ->
            getGymExercises(g) + getBarbellExercises(g) + getDumbbellExercises(g) + getBodyweightExercises(g)
        }
        val masters = masterWorkouts.keys.toList()
        return (masters + individual).distinct().sorted()
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
        val topic = getTopicForMuscle(muscleGroup)
        val allForGroup = getGymExercises(topic) + 
                         getBarbellExercises(topic) + 
                         getDumbbellExercises(topic) + 
                         getBodyweightExercises(topic)
        
        val relevantMasters = masterWorkouts.keys.filter { getTopicForMuscle(getMuscleGroupForExercise(it)) == topic }
        
        return (allForGroup + relevantMasters).distinct()
            .filter { it.lowercase() != currentExerciseName.lowercase() }
            .sorted()
    }

    fun getMuscleGroupForExercise(name: String): String {
        val cleanName = name.trim().lowercase()

        // 0. Handle Master Workouts
        if (name.endsWith("(Master)")) {
            val variations = masterWorkouts[name]
            if (!variations.isNullOrEmpty()) {
                return getMuscleGroupForExercise(variations.first())
            }
        }

        // 1. Specific matches in Pools (Returns detailed muscle)
        val detailedGroups = listOf("Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back", "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs", "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Chest")
        for (g in detailedGroups) {
            if (getGymExercises(g).any { it.equals(name, ignoreCase = true) } || 
                getBarbellExercises(g).any { it.equals(name, ignoreCase = true) } || 
                getDumbbellExercises(g).any { it.equals(name, ignoreCase = true) } || 
                getBodyweightExercises(g).any { it.equals(name, ignoreCase = true) }) return g
        }
        
        // 2. Keyword fallback
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

    fun isExerciseTimeBased(name: String): Boolean {
        val cleanName = name.trim().lowercase()
        return cleanName.contains("plank") || cleanName.contains("hold") || cleanName.contains("rest")
    }

    fun getRecommendedRepsForExercise(name: String): String {
        if (isExerciseTimeBased(name)) return "30"
        
        val cleanName = name.trim().lowercase()
        val muscleGroup = getMuscleGroupForExercise(name)
        
        return when {
            // Compounds / Heavy
            cleanName.contains("squat") || cleanName.contains("deadlift") || 
            cleanName.contains("bench") || cleanName.contains("press") ||
            cleanName.contains("row") || cleanName.contains("pull-up") -> "8–12"
            
            // Isolations
            muscleGroup in listOf("Biceps", "Triceps", "Lateral Deltoids", "Rear Deltoids", "Calves") -> "10–15"
            
            // Core
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

    private fun getExercisesForStrict(equipment: List<String>, bodyPart: String): List<String> {
        val pool = mutableListOf<String>()
        if (equipment.contains("Gym")) {
            pool.addAll(getGymExercises(bodyPart))
        } else {
            if (equipment.contains("Barbell")) pool.addAll(getBarbellExercises(bodyPart))
            if (equipment.contains("Dumbbell")) pool.addAll(getDumbbellExercises(bodyPart))
            if (equipment.contains("Bodyweight")) pool.addAll(getBodyweightExercises(bodyPart))
        }
        return pool.distinct()
    }

    // Refined "Science-Based" Exercise Lists
    private fun getBodyweightExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Push-ups", "Incline Push-ups", "Decline Push-ups")
        "front deltoids" -> listOf("Pike Push-ups", "Shoulder Taps")
        "lateral deltoids" -> listOf("Plank-to-Down-Dog", "Side Plank Rotations")
        "rear deltoids" -> emptyList()
        "lats" -> listOf("Pull-ups", "Chin-ups")
        "upper back" -> emptyList()
        "upper traps" -> listOf("Scapular Push-ups")
        "spinal erectors" -> listOf("Supermans", "Bridges")
        "biceps" -> emptyList()
        "triceps" -> listOf("Dips (Chair)", "Diamond Push-ups", "Close-Grip Push-ups")
        "forearms" -> listOf("Fist Squeezes", "Towel Hangs")
        "abs" -> listOf("Crunches (sit-ups)", "Leg Raises", "Plank")
        "obliques" -> listOf("Russian Twists", "Bicycle Crunches")
        "quadriceps" -> listOf("Squats", "Lunges", "Step-ups")
        "hamstrings" -> listOf("Glute Bridges", "Nordic Hamstring Curls")
        "glutes" -> listOf("Glute Bridges", "Donkey Kicks", "Fire Hydrants")
        "calves" -> listOf("Calf Raises", "Single Leg Calf Raises")
        "hip adductors" -> listOf("Side Lying Leg Raises", "Sumo Squats")
        "hip abductors" -> listOf("Clamshells", "Side Steps")
        
        // Broad Category Fallbacks
        "shoulders" -> listOf("Pike Push-ups", "Shoulder Taps")
        "back" -> listOf("Pull-ups", "Supermans")
        "legs" -> listOf("Squats", "Lunges", "Glute Bridges")
        "arms" -> listOf("Dips (Chair)", "Diamond Push-ups")
        "core" -> listOf("Plank", "Crunches (sit-ups)", "Leg Raises")
        
        else -> emptyList()
    }

    private fun getDumbbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Dumbbell Floor Press", "Dumbbell Flyes", "Incline DB Press")
        "front deltoids" -> listOf("Dumbbell Front Raises", "Dumbbell Shoulder Press")
        "lateral deltoids" -> listOf("Lateral Raises", "Leaning Lateral Raises")
        "rear deltoids" -> listOf("Bent-over Rear Delt Flyes")
        "lats" -> listOf("One-arm Dumbbell Row", "Dumbbell Pullover")
        "upper back" -> listOf("Dumbbell Rows", "Face Pulls (with DBs)")
        "upper traps" -> listOf("Dumbbell Shrugs")
        "spinal erectors" -> listOf("Dumbbell Deadlifts", "DB Back Extensions")
        "biceps" -> listOf("Dumbbell Curls", "Hammer Curls", "Incline Curls")
        "triceps" -> listOf("Overhead Extension", "Kickbacks", "Skullcrushers (DB)")
        "forearms" -> listOf("Wrist Curls", "Reverse Curls")
        "abs" -> listOf("Weighted Crunches", "DB Side Bends")
        "obliques" -> listOf("Russian Twists (with DB)")
        "quadriceps" -> listOf("Dumbbell Squats", "Goblet Squats", "DB Lunges")
        "hamstrings" -> listOf("DB Romanian Deadlifts", "Leg Curls (DB)")
        "glutes" -> listOf("DB Glute Bridges", "DB Sumos")
        "calves" -> listOf("DB Calf Raises")
        "hip adductors" -> listOf("Sumo Squats (DB)")
        "hip abductors" -> listOf("Side Lying Leg Raises (with weight)")

        // Broad Category Fallbacks
        "shoulders" -> listOf("Dumbbell Shoulder Press", "Lateral Raises", "Front Raises", "Thrusters")
        "back" -> listOf("Dumbbell Rows", "One-arm Dumbbell Row", "Dumbbell Pullover")
        "legs" -> listOf("Dumbbell Squats", "Goblet Squats", "DB Lunges", "DB Romanian Deadlifts")
        "arms" -> listOf("Dumbbell Curls", "Hammer Curls", "Overhead Extension", "Kickbacks")
        "core" -> listOf("Weighted Crunches", "DB Side Bends", "Russian Twists (with DB)")

        else -> listOf("Thrusters")
    }

    private fun getBarbellExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Barbell Bench Press", "Barbell Floor Press", "Incline Barbell Press")
        "front deltoids" -> listOf("Military Press", "Barbell Front Raises")
        "lateral deltoids" -> listOf("Upright Rows", "Wide Grip Military Press")
        "rear deltoids" -> listOf("Bent-over Barbell Rows")
        "lats" -> listOf("Barbell Row", "Barbell Pullovers")
        "upper back" -> listOf("Barbell Row", "Pendlay Rows")
        "upper traps" -> listOf("Barbell Shrugs")
        "spinal erectors" -> listOf("Deadlifts", "Barbell Good Mornings")
        "biceps" -> listOf("Barbell Curls", "Reverse Barbell Curls")
        "triceps" -> listOf("Close Grip Press", "Skullcrushers (Barbell)")
        "forearms" -> listOf("Barbell Wrist Curls")
        "abs" -> listOf("Barbell Rollouts")
        "obliques" -> listOf("Barbell Side Bends")
        "quadriceps" -> listOf("Barbell Squats", "Front Squats", "Barbell Lunges")
        "hamstrings" -> listOf("Barbell Romanian Deadlifts")
        "glutes" -> listOf("Barbell Hip Thrusts", "Deadlifts")
        "calves" -> listOf("Barbell Calf Raises")
        "hip adductors" -> listOf("Sumo Deadlifts")
        "hip abductors" -> listOf("Barbell Curtsey Lunges")

        // Broad Category Fallbacks
        "shoulders" -> listOf("Military Press", "Barbell Front Raises", "Upright Rows", "Clean & Press")
        "back" -> listOf("Barbell Row", "Deadlifts", "Pendlay Rows")
        "legs" -> listOf("Barbell Squats", "Barbell Romanian Deadlifts", "Barbell Lunges", "Deadlifts")
        "arms" -> listOf("Barbell Curls", "Close Grip Press", "Skullcrushers (Barbell)")
        "core" -> listOf("Barbell Rollouts", "Barbell Side Bends")

        else -> listOf("Clean & Press")
    }

    private fun getGymExercises(bodyPart: String): List<String> = when(bodyPart.lowercase().trim()) {
        "chest" -> listOf("Bench Press", "Cable Flyes", "Incline Press")
        "front deltoids" -> listOf("Military Press", "Shoulder Press Machine")
        "lateral deltoids" -> listOf("Side Raises", "Cable Lateral Raises")
        "rear deltoids" -> listOf("Face Pulls", "Rear Delt Deck")
        "lats" -> listOf("Lat Pulldown", "Seated Row", "Pull-ups")
        "upper back" -> listOf("Barbell Row", "T-Bar Row", "Face Pulls")
        "upper traps" -> listOf("Dumbbell Shrugs", "Smith Machine Shrugs")
        "spinal erectors" -> listOf("Deadlifts", "Back Extensions Machine")
        "biceps" -> listOf("Barbell Curls", "Preacher Curls", "Cable Curls")
        "triceps" -> listOf("Tricep Pushdowns", "Skullcrushers", "Dips")
        "forearms" -> listOf("Reverse Curls", "Hammer Curls")
        "abs" -> listOf("Cable Crunches", "Hanging Leg Raises")
        "obliques" -> listOf("Cable Woodchoppers", "Russian Twists")
        "quadriceps" -> listOf("Barbell Squats", "Leg Press", "Leg Extensions")
        "hamstrings" -> listOf("Leg Curls", "Romanian Deadlifts")
        "glutes" -> listOf("Hip Thrusts", "Glute Kickbacks")
        "calves" -> listOf("Standing Calf Raises", "Seated Calf Raises")
        "hip adductors" -> listOf("Adductor Machine", "Sumo Squats")
        "hip abductors" -> listOf("Abductor Machine", "Lateral Cable Leg Raises")

        // Broad Category Fallbacks
        "shoulders" -> listOf("Military Press", "Shoulder Press Machine", "Side Raises", "Face Pulls")
        "back" -> listOf("Lat Pulldown", "Seated Row", "Barbell Row", "Deadlifts")
        "legs" -> listOf("Barbell Squats", "Leg Press", "Leg Curls", "Romanian Deadlifts")
        "arms" -> listOf("Barbell Curls", "Tricep Pushdowns", "Skullcrushers", "Cable Curls")
        "core" -> listOf("Cable Crunches", "Hanging Leg Raises", "Cable Woodchoppers")

        else -> listOf("Deadlifts")
    }

    val template5DaySplit = WorkoutTemplate(
        id = "5day_hypertrophy",
        nameResId = R.string.hypertrophy_5day_name,
        descriptionResId = R.string.hypertrophy_5day_desc,
        badgeResId = R.string.most_popular,
        daysCountResId = R.string.workout_days_plus_rest,
        daysCountArgs = 5 to 2,
        dayTemplates = listOf(
            DayTemplate("MONDAY", R.string.pull_title, R.string.pull_desc, false),
            DayTemplate("TUESDAY", R.string.push_title, R.string.push_desc, false),
            DayTemplate("WEDNESDAY", R.string.legs_core_title, R.string.legs_desc, false),
            DayTemplate("THURSDAY", R.string.upper_hypertrophy_title, R.string.upper_power_desc, false),
            DayTemplate("FRIDAY", R.string.lower_hypertrophy_title, R.string.lower_power_desc, false),
            DayTemplate("SATURDAY", R.string.rest_recovery_day, R.string.rest_desc, true),
            DayTemplate("SUNDAY", R.string.rest_recovery_day, R.string.rest_desc, true)
        ),
        exercises = DefaultData.routineExercises
    )

    val templatePPL = WorkoutTemplate(
        id = "ppl_6day",
        nameResId = R.string.ppl_6day_name,
        descriptionResId = R.string.ppl_6day_desc,
        badgeResId = R.string.strength_mass,
        daysCountResId = R.string.workout_days_plus_rest,
        daysCountArgs = 6 to 1,
        dayTemplates = listOf(
            DayTemplate("MONDAY", R.string.push_title, R.string.push_desc, false),
            DayTemplate("TUESDAY", R.string.pull_title, R.string.pull_desc, false),
            DayTemplate("WEDNESDAY", R.string.legs_core_title, R.string.legs_desc, false),
            DayTemplate("THURSDAY", R.string.push_title, R.string.push_desc, false),
            DayTemplate("FRIDAY", R.string.pull_title, R.string.pull_desc, false),
            DayTemplate("SATURDAY", R.string.legs_core_title, R.string.legs_desc, false),
            DayTemplate("SUNDAY", R.string.rest_recovery_day, R.string.rest_desc, true)
        ),
        exercises = listOf(
            // MONDAY Push
            RoutineExerciseEntity(0, "MONDAY", "Barbell floor press", "Chest", 4, "8–12", "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell shoulder press", "Shoulders", 3, "8–12", "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell lateral raises", "Shoulders", 3, "12–15", "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Overhead tricep extension", "Triceps", 3, "10–15", "", 4),

            // TUESDAY Pull
            RoutineExerciseEntity(0, "TUESDAY", "Barbell bent-over row", "Back", 4, "8–12", "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "One-arm dumbbell row", "Back", 3, "10–12", "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell curls", "Biceps", 3, "10–12", "", 3),
            RoutineExerciseEntity(0, "TUESDAY", "Hammer curls", "Biceps", 3, "10–15", "", 4),

            // WEDNESDAY Legs
            RoutineExerciseEntity(0, "WEDNESDAY", "Barbell squats", "Legs", 4, "8–12", "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Romanian deadlifts", "Legs", 3, "8–12", "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell lunges", "Legs", 3, "10 each leg", "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Crunches (sit-ups)", "Abs", 3, "15–20", "", 4),

            // THURSDAY Push
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell floor press", "Chest", 4, "10–12", "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell lateral raises", "Shoulders", 4, "12–20", "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Tricep dips / extensions", "Triceps", 3, "10–15", "", 3),

            // FRIDAY Pull
            RoutineExerciseEntity(0, "FRIDAY", "Barbell row", "Back", 4, "8–12", "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell pullover", "Back", 3, "10–15", "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell curls", "Biceps", 3, "8–12", "", 3),

            // SATURDAY Legs
            RoutineExerciseEntity(0, "SATURDAY", "Barbell squats", "Legs", 4, "8–12", "", 1),
            RoutineExerciseEntity(0, "SATURDAY", "Leg raises", "Abs", 3, "12–20", "", 2)
        )
    )

    val templateUpperLower = WorkoutTemplate(
        id = "upper_lower_4day",
        nameResId = R.string.upper_lower_4day_name,
        descriptionResId = R.string.upper_lower_4day_desc,
        badgeResId = R.string.optimal_recovery,
        daysCountResId = R.string.workout_days_plus_rest,
        daysCountArgs = 4 to 3,
        dayTemplates = listOf(
            DayTemplate("MONDAY", R.string.upper_power_title, R.string.upper_power_desc, false),
            DayTemplate("TUESDAY", R.string.lower_power_title, R.string.lower_power_desc, false),
            DayTemplate("WEDNESDAY", R.string.rest_recovery_day, R.string.rest_desc, true),
            DayTemplate("THURSDAY", R.string.upper_hypertrophy_title, R.string.upper_power_desc, false),
            DayTemplate("FRIDAY", R.string.lower_hypertrophy_title, R.string.lower_power_desc, false),
            DayTemplate("SATURDAY", R.string.rest_recovery_day, R.string.rest_desc, true),
            DayTemplate("SUNDAY", R.string.rest_recovery_day, R.string.rest_desc, true)
        ),
        exercises = listOf(
            // MONDAY Upper Power
            RoutineExerciseEntity(0, "MONDAY", "Barbell floor press", "Chest", 4, "6–10", "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell bent-over row", "Back", 4, "6–10", "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell shoulder press", "Shoulders", 3, "8–12", "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Barbell curls", "Biceps", 3, "8–12", "", 4),

            // TUESDAY Lower Power
            RoutineExerciseEntity(0, "TUESDAY", "Barbell squats", "Legs", 4, "6–10", "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Romanian deadlifts", "Legs", 3, "8–10", "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Plank", "Abs", 3, "45 sec", "", 3),

            // THURSDAY Upper Hypertrophy
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell floor press", "Chest", 3, "10–12", "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "One-arm dumbbell row", "Back", 3, "10–12", "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell lateral raises", "Shoulders", 3, "12–15", "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Hammer curls", "Biceps", 3, "10–15", "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Overhead tricep extension", "Triceps", 3, "10–15", "", 5),

            // FRIDAY Lower Hypertrophy
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell lunges", "Legs", 3, "10 each leg", "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Leg raises", "Abs", 3, "12–20", "", 2)
        )
    )

    val templateFullBody = WorkoutTemplate(
        id = "fullbody_3day",
        nameResId = R.string.fullbody_3day_name,
        descriptionResId = R.string.fullbody_3day_desc,
        badgeResId = R.string.time_efficient,
        daysCountResId = R.string.workout_days_plus_rest,
        daysCountArgs = 3 to 4,
        dayTemplates = listOf(
            DayTemplate("MONDAY", R.string.full_body_a_title, R.string.full_body_a_desc, false),
            DayTemplate("TUESDAY", R.string.rest_recovery_day, R.string.rest_desc, true),
            DayTemplate("WEDNESDAY", R.string.full_body_b_title, R.string.full_body_b_desc, false),
            DayTemplate("THURSDAY", R.string.rest_recovery_day, R.string.rest_desc, true),
            DayTemplate("FRIDAY", R.string.full_body_c_title, R.string.full_body_c_desc, false),
            DayTemplate("SATURDAY", R.string.rest_recovery_day, R.string.rest_desc, true),
            DayTemplate("SUNDAY", R.string.rest_recovery_day, R.string.rest_desc, true)
        ),
        exercises = listOf(
            // MONDAY
            RoutineExerciseEntity(0, "MONDAY", "Barbell squats", "Legs", 3, "8–12", "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell floor press", "Chest", 3, "8–12", "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Barbell bent-over row", "Back", 3, "8–12", "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell lateral raises", "Shoulders", 3, "12–15", "", 4),
            RoutineExerciseEntity(0, "MONDAY", "Crunches (sit-ups)", "Abs", 3, "15–20", "", 5),

            // WEDNESDAY
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell lunges", "Legs", 3, "10 each leg", "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell shoulder press", "Shoulders", 3, "8–12", "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "One-arm dumbbell row", "Back", 3, "10–12", "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell curls", "Biceps", 3, "10–12", "", 4),

            // FRIDAY
            RoutineExerciseEntity(0, "FRIDAY", "Romanian deadlifts", "Legs", 3, "8–12", "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell floor press", "Chest", 3, "10–12", "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Hammer curls", "Biceps", 3, "10–15", "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Overhead tricep extension", "Triceps", 3, "10–15", "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Leg raises", "Abs", 3, "12–20", "", 5)
        )
    )

    val allTemplates = listOf(template5DaySplit, templatePPL, templateUpperLower, templateFullBody)
}
