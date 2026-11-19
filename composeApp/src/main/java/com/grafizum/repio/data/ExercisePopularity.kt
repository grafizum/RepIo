package com.grafizum.repio.data

/**
 * Popularity ranking for the built-in pool.
 *
 * The pool carries 600+ movements, most of them obscure variations. Ranked
 * alphabetically — or in catalog order — the pickers and the program generator
 * surface "Behind-the-Neck Lat Pulldown" just as readily as "Lat Pulldown",
 * which is exactly what users read as "the recommendations are random".
 *
 * [POPULARITY] is an ordered list of the most commonly programmed exercises in
 * commercial and home programs, most popular first (staple compounds before
 * their variations, barbell/dumbbell/bodyweight staples before machine and
 * cable work, big movers before small accessories). The tail order is
 * alphabetical, so an exercise never changes rank when the table grows.
 *
 * Every name is verified against [ExerciseCatalog.NAMES]; [rankFor] ignores
 * anything that is not, so a renamed catalog entry can never wedge the lookup.
 * The integrity test (ExercisePopularityTest) fails when the pool changes
 * until the table is revisited.
 */
object ExercisePopularity {

    /**
     * The most common exercises, most popular first. Names are the canonical
     * English pool names ([ExerciseCatalog.NAMES]) — the same join key every
     * other table uses.
     */
    private val POPULARITY: List<String> = listOf(
        // --- The global staples ---
        "Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift",
        "Bent-Over Barbell Row", "Barbell Overhead Press",
        "Push-Up", "Pull-Up", "Romanian Deadlift",
        "Dumbbell Bench Press", "Dumbbell Shoulder Press",
        "Dumbbell Bicep Curl", "Dumbbell Lateral Raise",
        "Lat Pulldown", "Leg Press", "Crunches", "Plank",
        // --- Barbell ---
        "Barbell Curl", "Barbell Hip Thrust", "Barbell Shrug",
        "Barbell Lunge", "Barbell Reverse Lunge", "Front Squat",
        "Close-Grip Bench Press", "Barbell Calf Raise",
        "Barbell Front Raise", "Rear Delt Row (Barbell)", "Barbell Upright Row",
        "Good Morning", "Clean and Jerk", "Hang Clean",
        // --- Dumbbell ---
        "Bent-Over Dumbbell Row", "Dumbbell Hammer Curl", "Dumbbell Fly",
        "Dumbbell Tricep Extension", "Incline Dumbbell Press",
        "Incline Dumbbell Fly", "Chest-Supported Dumbbell Row",
        "Dumbbell Pullover", "Dumbbell Lunge", "Dumbbell Squat",
        "Dumbbell Calf Raise", "Dumbbell Front Raise", "Dumbbell Shrug",
        "Dumbbell Face Pull", "Dumbbell Skull Crusher",
        "Dumbbell Front Squat", "Dumbbell Split Squat",
        "Dumbbell Upright Row", "Dumbbell Hip Thrust",
        "Bulgarian Split Squat",
        // --- Bodyweight ---
        "Chin-Ups", "Bench Dips", "Chest Dips", "Side Plank",
        "Hanging Leg Raise", "Lying Leg Raise", "Bodyweight Squat",
        "Bodyweight Calf Raise", "Burpees", "Mountain Climbers",
        "Jumping Jacks", "Sit-Ups", "Bicycle Crunch", "Glute Bridge",
        "Back Extension", "Inverted Row", "Nordic Hamstring Curl",
        "Dead Bug", "Decline Push-Up", "Incline Push-Up",
        "Bodyweight Overhead Press",
        // --- Kettlebell ---
        "Goblet Squat", "Kettlebell Swing", "Bulgarian Split Squat (Kettlebell)",
        "Kettlebell Reverse Lunge",
        // --- Bands (the category's real staples) ---
        "Band Pull Apart", "Banded Squat",
        // --- Other (cardio/kit staples of the "Other" category) ---
        "Jump Rope", "Box Jump", "Ab Wheel Rollout", "Battle Ropes", "Medicine Ball Slam",
        // --- Cable ---
        "Seated Cable Row", "Cable Fly", "Cable Curl", "Cable Tricep Pushdown",
        "Cable Face Pull", "Cable Crunch", "Cable Bent-Over Row",
        "Cable Front Raise", "Cable Hammer Curl",
        "Kneeling Cable Row",
        // --- Machine ---
        "Machine Chest Press", "Machine Shoulder Press", "Machine Chest Fly",
        "Machine Bicep Curl", "Machine Triceps Extension", "Machine Calf Raise",
        "Machine Preacher Curl", "Machine Assisted Dips",
        "Machine Back Extension", "Leg Extension", "Seated Leg Curl",
        "Lying Leg Curl", "Pec Deck", "Seated Calf Raise", "Standing Calf Raise",
        // --- Second-tier staples of the big families (ranked above the tail,
        // below their family's headline movements) ---
        "Dumbbell Romanian Deadlift", "EZ-Bar Curl",
        "Diamond Push Ups", "Machine Hip Abduction",
    )

    /** Lowercase name -> rank (0 = most popular). Built lazily, read hot. */
    private val RANKS: Map<String, Int> = POPULARITY
        .mapIndexed { index, name -> name.lowercase() to index }
        .toMap()

    /**
     * The popularity rank of [name]: 0 for the most common exercise, growing
     * with obscurity. Unranked names (and anything outside the pool) sort
     * after every ranked one, alphabetically among themselves.
     */
    fun rankFor(name: String): Int = RANKS[name.trim().lowercase()] ?: Int.MAX_VALUE

    /** True when [name] appears in the popularity table. */
    fun isRanked(name: String): Boolean = RANKS.containsKey(name.trim().lowercase())

    /**
     * Sorts [names] by popularity (popular first), alphabetical within the
     * same rank, and alphabetically after the ranked block for unranked
     * names. The alphabetical fallback is the raw string order — the same
     * order `List.sorted()` produces — so unranked callers see no change.
     */
    fun sortedByPopularity(names: List<String>): List<String> =
        names.sortedWith(
            compareBy(
                { rankFor(it) },
                { it }
            )
        )

    /** The ranked names, most popular first — exposed for tests and debug. */
    fun rankedNames(): List<String> = POPULARITY.toList()
}
