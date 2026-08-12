package com.example.data

import android.app.Application
import com.example.R

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
        intensity: String
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

        days.forEachIndexed { index, dayName ->
            val workoutIndex = workoutDayIndices.indexOf(index)
            val isWorkout = workoutIndex != -1
            
            if (isWorkout) {
                val currentParts = partsPerDay[workoutIndex]
                val satisfiedPartsOnThisDay = mutableListOf<String>()
                
                // 3. Pick exercises per body part for this day
                currentParts.forEachIndexed { partIndex, part ->
                    val available = getExercisesForStrict(equipment, part)
                    if (available.isEmpty()) {
                        unsatisfiedParts.add(part)
                    } else {
                        satisfiedPartsOnThisDay.add(part)
                        // MAJOR groups get more exercises
                        val isMajor = part in listOf("Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes")
                        val exerciseCount = if (isMajor && available.size >= 2) 2 else 1
                        
                        available.take(exerciseCount).forEachIndexed { exIndex, exName ->
                            routineExercises.add(RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayName,
                                name = exName,
                                muscleGroup = part,
                                targetSets = setsPerExercise,
                                targetReps = "8-12",
                                videoUrl = "",
                                orderIndex = (partIndex * 10) + exIndex
                            ))
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
    private fun getBodyweightExercises(bodyPart: String): List<String> = when(bodyPart) {
        "Chest" -> listOf("Push-ups", "Incline Push-ups", "Decline Push-ups")
        "Front Deltoids" -> listOf("Pike Push-ups", "Shoulder Taps")
        "Lateral Deltoids" -> listOf("Plank-to-Down-Dog", "Side Plank Rotations")
        "Rear Deltoids" -> emptyList() // Extremely hard without pulling equipment
        "Lats" -> emptyList() // Requires pull-up bar
        "Upper Back" -> emptyList() // Requires pulling equipment
        "Upper Traps" -> listOf("Scapular Push-ups")
        "Spinal Erectors" -> listOf("Supermans", "Bridges")
        "Biceps" -> emptyList() // Requires pulling equipment
        "Triceps" -> listOf("Dips (Chair)", "Diamond Push-ups", "Close-Grip Push-ups")
        "Forearms" -> listOf("Fist Squeezes", "Towel Hangs (if possible)")
        "Abs" -> listOf("Crunches", "Leg Raises", "Plank")
        "Obliques" -> listOf("Russian Twists", "Bicycle Crunches")
        "Quadriceps" -> listOf("Squats", "Lunges", "Step-ups")
        "Hamstrings" -> listOf("Glute Bridges", "Nordic Hamstring Curls")
        "Glutes" -> listOf("Glute Bridges", "Donkey Kicks", "Fire Hydrants")
        "Calves" -> listOf("Calf Raises", "Single Leg Calf Raises")
        "Hip Adductors" -> listOf("Side Lying Leg Raises", "Sumo Squats")
        "Hip Abductors" -> listOf("Clamshells", "Side Steps")
        else -> emptyList()
    }

    private fun getDumbbellExercises(bodyPart: String): List<String> = when(bodyPart) {
        "Chest" -> listOf("Dumbbell Floor Press", "Dumbbell Flyes", "Incline DB Press")
        "Front Deltoids" -> listOf("Dumbbell Front Raises", "Dumbbell Shoulder Press")
        "Lateral Deltoids" -> listOf("Lateral Raises", "Leaning Lateral Raises")
        "Rear Deltoids" -> listOf("Bent-over Rear Delt Flyes")
        "Lats" -> listOf("One-arm Dumbbell Row", "Dumbbell Pullover")
        "Upper Back" -> listOf("Dumbbell Rows", "Face Pulls (with DBs)")
        "Upper Traps" -> listOf("Dumbbell Shrugs")
        "Spinal Erectors" -> listOf("Dumbbell Deadlifts", "DB Back Extensions")
        "Biceps" -> listOf("Dumbbell Curls", "Hammer Curls", "Incline Curls")
        "Triceps" -> listOf("Overhead Extension", "Kickbacks", "Skullcrushers (DB)")
        "Forearms" -> listOf("Wrist Curls", "Reverse Curls")
        "Abs" -> listOf("Weighted Crunches", "DB Side Bends")
        "Obliques" -> listOf("Russian Twists (with DB)")
        "Quadriceps" -> listOf("Dumbbell Squats", "Goblet Squats", "DB Lunges")
        "Hamstrings" -> listOf("DB Romanian Deadlifts", "Leg Curls (DB)")
        "Glutes" -> listOf("DB Glute Bridges", "DB Sumos")
        "Calves" -> listOf("DB Calf Raises")
        "Hip Adductors" -> listOf("Sumo Squats (DB)")
        "Hip Abductors" -> listOf("Side Lying Leg Raises (with weight)")
        else -> listOf("Thrusters")
    }

    private fun getBarbellExercises(bodyPart: String): List<String> = when(bodyPart) {
        "Chest" -> listOf("Barbell Bench Press", "Barbell Floor Press", "Incline Barbell Press")
        "Front Deltoids" -> listOf("Military Press", "Barbell Front Raises")
        "Lateral Deltoids" -> listOf("Upright Rows", "Wide Grip Military Press")
        "Rear Deltoids" -> listOf("Bent-over Barbell Rows")
        "Lats" -> listOf("Barbell Row", "Barbell Pullovers")
        "Upper Back" -> listOf("Barbell Row", "Pendlay Rows")
        "Upper Traps" -> listOf("Barbell Shrugs")
        "Spinal Erectors" -> listOf("Deadlifts", "Barbell Good Mornings")
        "Biceps" -> listOf("Barbell Curls", "Reverse Barbell Curls")
        "Triceps" -> listOf("Close Grip Press", "Skullcrushers (Barbell)")
        "Forearms" -> listOf("Barbell Wrist Curls")
        "Abs" -> listOf("Barbell Rollouts")
        "Obliques" -> listOf("Barbell Side Bends")
        "Quadriceps" -> listOf("Barbell Squats", "Front Squats", "Barbell Lunges")
        "Hamstrings" -> listOf("Barbell Romanian Deadlifts")
        "Glutes" -> listOf("Barbell Hip Thrusts", "Deadlifts")
        "Calves" -> listOf("Barbell Calf Raises")
        "Hip Adductors" -> listOf("Sumo Deadlifts")
        "Hip Abductors" -> listOf("Barbell Curtsey Lunges")
        else -> listOf("Clean & Press")
    }

    private fun getGymExercises(bodyPart: String): List<String> = when(bodyPart) {
        "Chest" -> listOf("Bench Press", "Cable Flyes", "Incline Press")
        "Front Deltoids" -> listOf("Military Press", "Shoulder Press Machine")
        "Lateral Deltoids" -> listOf("Side Raises", "Cable Lateral Raises")
        "Rear Deltoids" -> listOf("Face Pulls", "Rear Delt Deck")
        "Lats" -> listOf("Lat Pulldown", "Seated Row", "Pull-ups")
        "Upper Back" -> listOf("Barbell Row", "T-Bar Row", "Face Pulls")
        "Upper Traps" -> listOf("Dumbbell Shrugs", "Smith Machine Shrugs")
        "Spinal Erectors" -> listOf("Deadlifts", "Back Extensions Machine")
        "Biceps" -> listOf("Barbell Curls", "Preacher Curls", "Cable Curls")
        "Triceps" -> listOf("Tricep Pushdowns", "Skullcrushers", "Dips")
        "Forearms" -> listOf("Reverse Curls", "Hammer Curls")
        "Abs" -> listOf("Cable Crunches", "Hanging Leg Raises")
        "Obliques" -> listOf("Cable Woodchoppers", "Russian Twists")
        "Quadriceps" -> listOf("Barbell Squats", "Leg Press", "Leg Extensions")
        "Hamstrings" -> listOf("Leg Curls", "Romanian Deadlifts")
        "Glutes" -> listOf("Hip Thrusts", "Glute Kickbacks")
        "Calves" -> listOf("Standing Calf Raises", "Seated Calf Raises")
        "Hip Adductors" -> listOf("Adductor Machine", "Sumo Squats")
        "Hip Abductors" -> listOf("Abductor Machine", "Lateral Cable Leg Raises")
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
            RoutineExerciseEntity(0, "WEDNESDAY", "Crunches", "Abs", 3, "15–20", "", 4),

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
            RoutineExerciseEntity(0, "MONDAY", "Crunches", "Abs", 3, "15–20", "", 5),

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
