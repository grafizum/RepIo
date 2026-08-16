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
            name = "Barbell bent-over row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–12",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "One-arm dumbbell row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "10–15 each side",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell pullover",
            muscleGroup = "Back",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–15",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Hammer curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell shoulder press",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–15",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell lateral raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "12–20",
            videoUrl = "",
            orderIndex = 7
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Crunches (sit-ups)",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 8
        ),

        // TUESDAY
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Barbell floor press",
            muscleGroup = "Chest",
            targetSets = 3,
            targetReps = "8–15",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell floor press",
            muscleGroup = "Chest",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Barbell bent-over row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–12",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell row",
            muscleGroup = "Back",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Barbell curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–15",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Hammer curls",
            muscleGroup = "Biceps",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Leg raises",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 7
        ),

        // WEDNESDAY
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Barbell squats",
            muscleGroup = "Legs",
            targetSets = 3,
            targetReps = "8–12",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Romanian deadlifts",
            muscleGroup = "Legs",
            targetSets = 3,
            targetReps = "8–12",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Dumbbell lunges",
            muscleGroup = "Legs",
            targetSets = 2,
            targetReps = "10 each leg",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Dumbbell shoulder press",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–15",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Lateral raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "12–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Crunches (sit-ups)",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Plank",
            muscleGroup = "Abs",
            targetSets = 2,
            targetReps = "30–60 sec",
            videoUrl = "",
            orderIndex = 7
        ),

        // THURSDAY
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Barbell row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–12",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "One-arm dumbbell row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell pullover",
            muscleGroup = "Back",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Barbell curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–12",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Hammer curls",
            muscleGroup = "Biceps",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Lateral raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "12–20",
            videoUrl = "",
            orderIndex = 7
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Rear-delt dumbbell fly",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "12–20",
            videoUrl = "",
            orderIndex = 8
        ),

        // FRIDAY
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell shoulder press",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–15",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Lateral raises",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "12–20",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Rear-delt fly",
            muscleGroup = "Shoulders",
            targetSets = 2,
            targetReps = "12–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Barbell row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell curls",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Hammer curls",
            muscleGroup = "Biceps",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Overhead dumbbell extension",
            muscleGroup = "Triceps",
            targetSets = 2,
            targetReps = "10–15",
            videoUrl = "",
            orderIndex = 7
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Crunches (sit-ups)",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 8
        )
    )
}
