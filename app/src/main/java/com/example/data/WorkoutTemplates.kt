package com.example.data

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
