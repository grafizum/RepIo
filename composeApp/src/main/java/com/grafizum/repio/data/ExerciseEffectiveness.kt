package com.grafizum.repio.data

/**
 * HOW EFFECTIVE A MOVEMENT IS FOR THE JOB A SLOT ASKS, most effective first.
 *
 * Owner, 2026-10-22 (dashboard task_0z6e4u5i5h2k0e1p2m4e226m): *"Have the random selection of
 * exercises in a day, not be random. Instead have the list of all exercises possible for that
 * certain muscle group and specific muscle, be categorized from the most effective to the least …
 * and then give the user the best possible option based on his filters of equipment etc. And make
 * sure … that exercises are not repetitive."*
 *
 * WHAT "RANDOM" ACTUALLY WAS. Nothing in the generator drew a random number: it was a stable sort
 * whose last key was the input order, and inside one part's pool every row outside
 * [ExercisePopularity] tied and then fell back to ALPHABETICAL. With 601 movements and roughly 150
 * ranked ones, a day's third chest slot could land on "Barbell Guillotine Bench Press" because of
 * its name - indistinguishable from a die roll, and the picks moved whenever the catalogue grew.
 *
 * THE ORDER IS FOUR KEYS, every one a fact about the movement rather than a preference invented
 * here:
 *
 * 1. **What the slot is FOR** ([roleRank]): a session's first two slots lead with a compound and its
 *    last two close on isolation. That is the published template's shape and the reason a chest day
 *    opens on a press rather than a fly.
 * 2. **Compound before isolation** ([isCompound]): for the same muscle and kit, the multi-joint
 *    movement is the one that can carry the working load. It is also what stops the alphabetical
 *    tail from being arbitrary - the two halves of a muscle's pool are ordered by their own role
 *    before anything alphabetical is consulted.
 * 3. **How much the implement can be progressed** ([gearTier]): the loading ceiling, and the
 *    owner's own ruling of 2026-10-09 - *"If i select that i have dumbbells i would naturally want
 *    more dumbbell exercises than bodyweight if thats possible so that it has more effect … the
 *    most effective once come first always"*. The owner's 2026-10-22 note guesses machines at the
 *    top; the ladder keeps the free weights above them (load is what a machine cannot add past its
 *    pin) and what the guess actually asked for - an explicit order in place of an alphabetical
 *    tail - is what this class provides. Loadable free weights, then the stack machines, then the
 *    portable implements, then the one nobody can add a plate to.
 * 4. **Whether programmes use it** ([ExercisePopularity]), then the name, so the order is total and
 *    a generated week is reproducible: two runs of the same answers produce the same plan.
 *
 * THE LIST THE USER SEES IS THE LIST THE GENERATOR DRAWS FROM. [rankedForMuscle] orders a part's
 * movements and [rankedForSlot] orders them for one slot, so "the best option" is the head of a
 * list that can be read top to bottom - not a hidden score.
 */
object ExerciseEffectiveness {

    /**
     * The loading-ceiling ladder: lower is more effective for a working slot.
     *
     * Barbell and dumbbell sit at the top because the load is the user's to add; a kettlebell is the
     * same kind of object in fixed sizes; a machine and a cable stack stop at their own pin; a
     * band's resistance is its own; bodyweight is the one implement nobody can add a plate to.
     * `Other` is the pool's grab-bag - stretches, poses, the ab wheel, the sled - and must never win
     * a slot ahead of a real movement: measured before it was pinned, a bodyweight-only build came
     * out 35 Other / 7 Bodyweight when it was not separated.
     *
     * Unknown categories (a user's own custom row) rank last, behind everything in the pool.
     */
    fun gearTier(equipment: String): Int = when (equipment) {
        WorkoutTemplates.EQUIPMENT_BARBELL -> 0
        WorkoutTemplates.EQUIPMENT_DUMBBELL -> 1
        WorkoutTemplates.EQUIPMENT_KETTLEBELL -> 2
        WorkoutTemplates.EQUIPMENT_MACHINE -> 3
        WorkoutTemplates.EQUIPMENT_CABLE -> 4
        WorkoutTemplates.EQUIPMENT_BANDS -> 5
        WorkoutTemplates.EQUIPMENT_BODYWEIGHT -> 6
        WorkoutTemplates.EQUIPMENT_OTHER -> 7
        else -> 8
    }

    /** [gearTier] for a movement, read from the catalogue's own equipment field. */
    fun gearTierFor(name: String): Int = gearTier(ExerciseCatalog.equipmentFor(name))

    /** True when the catalogue files [name] as a multi-joint movement. */
    fun isCompound(name: String): Boolean = ExerciseCatalog.entryFor(name)?.level == "compound"

    /** How many slots at the head of a session lead with compounds. */
    const val LEAD_SLOTS = 2

    /** How many slots at the tail of a long session close on isolation. */
    const val CLOSING_SLOTS = 2

    /**
     * Where a movement belongs in the session's shape: 0 when the slot wants what this movement is,
     * 1 otherwise. The lead slots want a compound and the closing slots want an isolation;
     * everything in between is neutral and takes either.
     */
    fun roleRank(name: String, slotIndex: Int, slotCount: Int): Int {
        val compound = isCompound(name)
        return when {
            slotIndex < LEAD_SLOTS -> if (compound) 0 else 1
            slotCount > LEAD_SLOTS + CLOSING_SLOTS && slotIndex >= slotCount - CLOSING_SLOTS ->
                if (compound) 1 else 0
            else -> 0
        }
    }

    /**
     * A part's movements, most effective first, with no slot in mind: compound before isolation,
     * then the gear ladder, then how commonly programmes use the movement, then the name. This is
     * the list the owner asked to see categorised.
     */
    fun rankedForMuscle(names: List<String>): List<String> = names.sortedWith(
        compareBy(
            { name: String -> if (isCompound(name)) 0 else 1 },
            { name: String -> gearTierFor(name) },
            { name: String -> ExercisePopularity.rankFor(name) },
            { name: String -> name }
        )
    )

    /**
     * A part's movements for ONE session slot, best first: the slot's role leads, then the compound
     * rule, then the gear ladder, then popularity, then the name.
     *
     * [slotCount] is the day's slot count so the closing slots can be recognised; a session too
     * short to have both a lead and a close takes compounds first throughout.
     */
    fun rankedForSlot(
        names: List<String>,
        slotIndex: Int,
        slotCount: Int,
        preferred: Set<String> = emptySet()
    ): List<String> = names.sortedWith(
        compareBy(
            // The user's own picks lead, whatever the ranking says: they are a
            // request rather than a suggestion.
            { name: String -> if (name.trim().lowercase() in preferred) 0 else 1 },
            { name: String -> roleRank(name, slotIndex, slotCount) },
            { name: String -> if (isCompound(name)) 0 else 1 },
            { name: String -> gearTierFor(name) },
            { name: String -> ExercisePopularity.rankFor(name) },
            { name: String -> name }
        )
    )

    /**
     * THE PICK: the best movement in [names] this WEEK has not already used and this day has not
     * already used.
     *
     * A WEEK-LEVEL REPEAT IS THE LAST RESORT, NOT THE FIRST. When every candidate is already used
     * this week the best one not already used TODAY is taken instead - a kit that owns two lat
     * movements and a week with four lat slots cannot avoid a repeat, and leaving the part
     * untrained would be the worse answer (the filter audit asks every preset and every split for a
     * gated movement on every day, and it is right to). What is never allowed is the same movement
     * twice in ONE session, or a repeat while an unused candidate exists.
     *
     * Null means the part has nothing left to give today at all, and the caller records it as
     * unsatisfied rather than inventing one.
     *
     * [names] is expected to be the part's already-gated pool, so a movement the user cannot do is
     * never a candidate here.
     */
    fun bestUnused(
        names: List<String>,
        slotIndex: Int,
        slotCount: Int,
        usedThisWeek: Set<String>,
        usedToday: Set<String> = emptySet(),
        preferred: Set<String> = emptySet()
    ): String? {
        val ranked = rankedForSlot(names, slotIndex, slotCount, preferred)
        return ranked.firstOrNull { it !in usedThisWeek && it !in usedToday }
            ?: ranked.firstOrNull { it !in usedToday }
    }
}
