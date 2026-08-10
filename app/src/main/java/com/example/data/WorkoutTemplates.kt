package com.example.data

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val description: String,
    val badge: String,
    val daysCountText: String,
    val days: List<RoutineDayEntity>,
    val exercises: List<RoutineExerciseEntity>
)

object WorkoutTemplates {

    val template5DaySplit = WorkoutTemplate(
        id = "5day_hypertrophy",
        name = "5-Day Hypertrophy Split",
        description = "High-volume body-part split focusing on muscle growth and balanced symmetry across 5 workout days.",
        badge = "Most Popular",
        daysCountText = "5 Workout Days + 2 Rest Days",
        days = DefaultData.routineDays,
        exercises = DefaultData.routineExercises
    )

    val templatePPL = WorkoutTemplate(
        id = "ppl_6day",
        name = "Push / Pull / Legs (PPL)",
        description = "Classic PPL routine separating pushing muscles (chest/shoulders/triceps), pulling muscles (back/biceps), and leg muscles.",
        badge = "Strength & Mass",
        daysCountText = "6 Workout Days + 1 Rest Day",
        days = listOf(
            RoutineDayEntity("MONDAY", "Monday — Push (Chest, Shoulders, Triceps)", "Heavy chest pressing and overhead shoulder work", false),
            RoutineDayEntity("TUESDAY", "Tuesday — Pull (Back & Biceps)", "Heavy rows, pulldowns, and bicep hypertrophy", false),
            RoutineDayEntity("WEDNESDAY", "Wednesday — Legs & Core", "Squats, lunges, leg curls, and core stability", false),
            RoutineDayEntity("THURSDAY", "Thursday — Push (Chest, Shoulders, Triceps)", "Volume pressing and lateral raise volume", false),
            RoutineDayEntity("FRIDAY", "Friday — Pull (Back & Biceps)", "Rowing variations, rear delts, and bicep curls", false),
            RoutineDayEntity("SATURDAY", "Saturday — Legs & Core", "Deadlifts, Romanian deadlifts, calves, and abs", false),
            RoutineDayEntity("SUNDAY", "Sunday — Rest & Recovery", "Active rest, foam rolling, and mobility", true)
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
        name = "4-Day Upper / Lower Split",
        description = "Balanced 4-day split training all upper body muscles twice a week and all lower body muscles twice a week with ample recovery time.",
        badge = "Optimal Recovery",
        daysCountText = "4 Workout Days + 3 Rest Days",
        days = listOf(
            RoutineDayEntity("MONDAY", "Monday — Upper Body Power", "Heavy chest, back, shoulders, and arm pressing", false),
            RoutineDayEntity("TUESDAY", "Tuesday — Lower Body & Core", "Squats, deadlifts, and abs", false),
            RoutineDayEntity("WEDNESDAY", "Wednesday — Active Rest", "Rest and mobility work", true),
            RoutineDayEntity("THURSDAY", "Thursday — Upper Body Hypertrophy", "Volume chest flyes, rows, lateral raises, and biceps", false),
            RoutineDayEntity("FRIDAY", "Friday — Lower Body Hypertrophy", "Lunges, Romanian deadlifts, calves, and core", false),
            RoutineDayEntity("SATURDAY", "Saturday — Rest & Recovery", "Active rest", true),
            RoutineDayEntity("SUNDAY", "Sunday — Rest & Recovery", "Rest", true)
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
        name = "3-Day Full Body Classic",
        description = "Efficient 3-day full body training hitting every muscle group each session. Great for busy schedules and compound strength gains.",
        badge = "Time Efficient",
        daysCountText = "3 Workout Days + 4 Rest Days",
        days = listOf(
            RoutineDayEntity("MONDAY", "Monday — Full Body A", "Squat, Press, Row, Core", false),
            RoutineDayEntity("TUESDAY", "Tuesday — Rest & Active Recovery", "Rest", true),
            RoutineDayEntity("WEDNESDAY", "Wednesday — Full Body B", "Lunge, Shoulder Press, Pullover, Biceps", false),
            RoutineDayEntity("THURSDAY", "Thursday — Rest & Active Recovery", "Rest", true),
            RoutineDayEntity("FRIDAY", "Friday — Full Body C", "Romanian Deadlift, Chest Press, Arms, Core", false),
            RoutineDayEntity("SATURDAY", "Saturday — Rest & Recovery", "Rest", true),
            RoutineDayEntity("SUNDAY", "Sunday — Rest & Recovery", "Rest", true)
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
