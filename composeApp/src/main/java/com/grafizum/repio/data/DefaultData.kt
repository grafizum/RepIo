package com.grafizum.repio.data

object DefaultData {

    // The default 5-day layout follows the classic body-part split (chest /
    // back / legs / shoulders / arms) that most research-grounded hypertrophy
    // templates converge on: one concentrated, fresh session per major, the
    // leg day fixed midweek, and arm work paired onto the pressing and pulling
    // days so every muscle still trains twice a week.
    val dayTemplates = listOf(
        DayTemplate("MONDAY", "chest_title", "chest_desc", false),
        DayTemplate("TUESDAY", "back_title", "back_desc", false),
        DayTemplate("WEDNESDAY", "legs_core_title", "legs_desc", false),
        DayTemplate("THURSDAY", "shoulders_title", "shoulders_desc", false),
        DayTemplate("FRIDAY", "arms_title", "arms_desc", false),
        DayTemplate("SATURDAY", "rest_recovery_day", "rest_desc", true),
        DayTemplate("SUNDAY", "rest_recovery_day", "rest_desc", true)
    )

    val routineDays = listOf(
        RoutineDayEntity("MONDAY", "Monday — Chest", "chest_desc", false),
        RoutineDayEntity("TUESDAY", "Tuesday — Back", "back_desc", false),
        RoutineDayEntity("WEDNESDAY", "Wednesday — Legs & Core", "legs_desc", false),
        RoutineDayEntity("THURSDAY", "Thursday — Shoulders", "shoulders_desc", false),
        RoutineDayEntity("FRIDAY", "Friday — Arms", "arms_desc", false),
        RoutineDayEntity("SATURDAY", "Saturday — Rest", "rest_desc", true),
        RoutineDayEntity("SUNDAY", "Sunday — Rest", "rest_desc", true)
    )

    val routineExercises = listOf(
        // MONDAY — Chest + triceps (pressing rides together; research-backed
        // chest-day layout: flat compound first, incline for the upper fibers,
        // a fly for stretch-loaded isolation, then triceps assistance).
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Barbell Bench Press",
            muscleGroup = "Chest",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Incline Dumbbell Press",
            muscleGroup = "Pectorals",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Fly",
            muscleGroup = "Pectorals",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Dumbbell Tricep Extension",
            muscleGroup = "Triceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "MONDAY",
            name = "Bench Dips",
            muscleGroup = "Triceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),

        // TUESDAY — Back + biceps (the pull pairing; heavy vertical pull,
        // heavy horizontal row, a width pulldown, then elbow flexion).
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Pull-Up",
            muscleGroup = "Lats",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Bent-Over Barbell Row",
            muscleGroup = "Back",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Lat Pulldown",
            muscleGroup = "Lats",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell Bicep Curl",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "TUESDAY",
            name = "Dumbbell Hammer Curl",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),

        // WEDNESDAY — THE leg day, midweek where recovery slots it (48h to
        // the upper-body work on both sides). Full evidence-based layout:
        // squat pattern, hip hinge, unilateral quad work, posterior chain,
        // calf raise, and core stability to finish.
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Barbell Back Squat",
            muscleGroup = "Legs",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Romanian Deadlift",
            muscleGroup = "Legs",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Dumbbell Lunge",
            muscleGroup = "Quadriceps",
            targetSets = 3,
            targetReps = "5–15 each leg",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Lying Leg Curl",
            muscleGroup = "Hamstrings",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Standing Calf Raise",
            muscleGroup = "Calves",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "WEDNESDAY",
            name = "Plank",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "30–60 sec",
            videoUrl = "",
            orderIndex = 6
        ),

        // THURSDAY — Shoulders + traps: overhead press first, side and rear
        // delt isolation (the rear delts recover from Tuesday's rows), then
        // traps and a final triceps tap.
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Shoulder Press",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Lateral Raise",
            muscleGroup = "Shoulders",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Reverse Fly",
            muscleGroup = "Rear Deltoids",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Dumbbell Shrug",
            muscleGroup = "Upper Traps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "THURSDAY",
            name = "Cable Tricep Pushdown",
            muscleGroup = "Triceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),

        // FRIDAY — Arms: curls and extensions supersetted (the classic arm
        // day), with compound chin-ups opening so the biceps are worked from
        // a fully stretched position first.
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Chin-Ups",
            muscleGroup = "Lats",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 1
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Barbell Curl",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "8–18",
            videoUrl = "",
            orderIndex = 2
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Skull Crusher",
            muscleGroup = "Triceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 3
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Dumbbell Hammer Curl",
            muscleGroup = "Biceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 4
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Cable Tricep Pushdown",
            muscleGroup = "Triceps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 5
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Barbell Shrug",
            muscleGroup = "Upper Traps",
            targetSets = 3,
            targetReps = "10–20",
            videoUrl = "",
            orderIndex = 6
        ),
        RoutineExerciseEntity(
            dayOfWeek = "FRIDAY",
            name = "Crunches",
            muscleGroup = "Abs",
            targetSets = 3,
            targetReps = "15–25",
            videoUrl = "",
            orderIndex = 7
        )
    )
}
