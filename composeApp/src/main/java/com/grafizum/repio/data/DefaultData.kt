package com.grafizum.repio.data

object DefaultData {

    val dayTemplates = listOf(
        DayTemplate("MONDAY", "pull_title", "pull_desc", false),
        DayTemplate("TUESDAY", "push_title", "push_desc", false),
        DayTemplate("WEDNESDAY", "legs_core_title", "legs_desc", false),
        DayTemplate("THURSDAY", "upper_hypertrophy_title", "upper_power_desc", false),
        DayTemplate("FRIDAY", "lower_hypertrophy_title", "lower_power_desc", false),
        DayTemplate("SATURDAY", "rest_recovery_day", "rest_desc", true),
        DayTemplate("SUNDAY", "rest_recovery_day", "rest_desc", true)
    )

    val routineDays = listOf(
        RoutineDayEntity("MONDAY", "Monday — Pull", "pull_desc", false),
        RoutineDayEntity("TUESDAY", "Tuesday — Push", "push_desc", false),
        RoutineDayEntity("WEDNESDAY", "Wednesday — Legs & Core", "legs_desc", false),
        RoutineDayEntity("THURSDAY", "Thursday — Upper Hypertrophy", "upper_power_desc", false),
        RoutineDayEntity("FRIDAY", "Friday — Lower Hypertrophy", "lower_power_desc", false),
        RoutineDayEntity("SATURDAY", "Saturday — Rest", "rest_desc", true),
        RoutineDayEntity("SUNDAY", "Sunday — Rest", "rest_desc", true)
    )

    val routineExercises = listOf(
        // MONDAY
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Barbell Rows",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Rows",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "10–20 each side",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Pullovers",
            muscleGroup = "Back",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Hammer Curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Shoulder Presses",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Lateral Raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 7
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Crunches",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 8
        ),

        // TUESDAY
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Barbell Bench Presses",
            muscleGroup = "Chest",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell Bench Presses",
            muscleGroup = "Chest",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Barbell Rows",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell Rows",
            muscleGroup = "Back",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Barbell Curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell Hammer Curls",
            muscleGroup = "Biceps",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Leg Raises",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 7
        ),

        // WEDNESDAY
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Barbell Squats",
            muscleGroup = "Legs",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Barbell Romanian Deadlifts",
            muscleGroup = "Legs",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Dumbbell Lunges",
            muscleGroup = "Legs",
            targetSets = 2,
            targetReps = "5–15 each leg",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Dumbbell Shoulder Presses",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Dumbbell Lateral Raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Crunches",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Planks",
            muscleGroup = "Abs",
            targetSets = 2,
            targetReps = "30–60 sec",
            videoUrl = "",
            orderIndex = 7
        ),

        // THURSDAY
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Barbell Rows",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Rows",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Pullovers",
            muscleGroup = "Back",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Barbell Curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Hammer Curls",
            muscleGroup = "Biceps",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Lateral Raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 7
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Flyes",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 8
        ),

        // FRIDAY
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Shoulder Presses",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Lateral Raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Flyes",
            muscleGroup = "Shoulders",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Barbell Rows",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Hammer Curls",
            muscleGroup = "Biceps",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Tricep Extensions",
            muscleGroup = "Triceps",
            targetSets = 2,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 7
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Crunches",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 8
        )
    )
}
