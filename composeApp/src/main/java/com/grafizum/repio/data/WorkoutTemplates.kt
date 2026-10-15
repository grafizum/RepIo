package com.grafizum.repio.data

import java.util.Calendar

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

    // Equipment categories used to organize the exercise pool. The rebuilt pool
    // (ExerciseCatalog, generated from the exercise-database CSV) uses eight
    // categories; "All" is the UI's no-filter value.
    const val EQUIPMENT_ALL = "All"
    const val EQUIPMENT_BODYWEIGHT = "Bodyweight"
    const val EQUIPMENT_BARBELL = "Barbell"
    const val EQUIPMENT_DUMBBELL = "Dumbbell"
    const val EQUIPMENT_KETTLEBELL = "Kettlebell"
    const val EQUIPMENT_CABLE = "Cable"
    const val EQUIPMENT_MACHINE = "Machine"
    const val EQUIPMENT_BANDS = "Bands"
    const val EQUIPMENT_OTHER = "Other"

    // ---------- SESSION DURATION ----------
    // The third knob of a build (beside frequency and equipment): how much
    // time one session may take, expressed as the exercise count that fits
    // in it. With the app's universal 3 working sets, an exercise costs
    // roughly 6-8 minutes including rest, so the windows map 1:1 to the
    // duration options offered on the welcome screen.
    const val DURATION_QUICK = "duration_quick"
    const val DURATION_STANDARD = "duration_standard"
    const val DURATION_FULL = "duration_full"
    val DURATION_OPTIONS = listOf(DURATION_QUICK, DURATION_STANDARD, DURATION_FULL)

    /** True when [value] is one of the [DURATION_OPTIONS]. */
    fun isValidDuration(value: String?): Boolean = value != null && value in DURATION_OPTIONS

    /**
     * The exercise-count window one session of [duration] fits: 30-45 min
     * = 6-8 exercises (compound focus, minimal isolation), 45-60 min = 8-10
     * (balanced compounds + accessories), 60-75 min = 10-12 (full volume).
     * Unknown values fall back to the standard window.
     */
    fun durationRangeFor(duration: String?): IntRange = when (duration) {
        DURATION_QUICK -> 6..8
        DURATION_FULL -> 10..12
        else -> 8..10
    }

    // ---------- THE PREMADE WEEK ----------

    /** The week in order, so every "which days" answer shares one vocabulary. */
    val WEEK_DAY_ORDER = listOf(
        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    )

    /**
     * The week the app lays out for a user who picks a NUMBER of training days and does not
     * want to choose the days themselves.
     *
     * The owner asked for this on 2026-10-08: *"i want the 2 to 6 days to be a premade
     * selection of the week for the best days to make pause for example 5 days is 3 days
     * workout 1 pause 2 workout 1 pause.., make these premade days based on the usual
     * professional sources"*. Those sources agree on three rules, and the patterns below
     * are what those rules produce:
     *
     *  1. **Never more than three consecutive training days.** Past three, session quality
     *     and recovery both fall (ACSM/NSCA guidance on consecutive-day loading), which is
     *     why 4, 5 and 6 days all carry a mid-week pause rather than running the week's days
     *     end to end from Monday.
     *  2. **A rest day is what makes the split work, not leftover space.** A muscle group
     *     trained on consecutive days is trained under fatigue; the pause is what gives every
     *     group the ~48h the same literature asks for. That is why 5 days is 3-on / 1-off /
     *     2-on / 1-off - exactly the example the owner gave - and never Mon-Fri.
     *  3. **The week ends at rest.** 6 days pauses on THURSDAY (mid-week, where fatigue has
     *     accumulated) and keeps the weekend days that people actually have free; 2 days
     *     spreads to Monday and Thursday so the two sessions cannot crowd into one week.
     *
     * 3 days is deliberately UNCHANGED at MON/WED/FRI: it is the pattern this screen already
     * shipped and the one the app's own tests and copy are written against, and it is also
     * the textbook full-body week (every muscle trained every other day).
     *
     * These are a WINDOW, not a program: the user can still pick the days themselves through
     * the specific-days control, which is what makes 1 and 7 days honest choices rather than
     * refused ones.
     */
    fun premadeWeekFor(daysPerWeek: Int): List<String> {
        val pattern = when (daysPerWeek) {
            1 -> listOf("MONDAY")
            2 -> listOf("MONDAY", "THURSDAY")
            4 -> listOf("MONDAY", "TUESDAY", "THURSDAY", "FRIDAY")
            5 -> listOf("MONDAY", "TUESDAY", "WEDNESDAY", "FRIDAY", "SATURDAY")
            6 -> listOf("MONDAY", "TUESDAY", "WEDNESDAY", "FRIDAY", "SATURDAY", "SUNDAY")
            7 -> WEEK_DAY_ORDER
            // 3 and anything out of range: the shipped full-body week.
            else -> listOf("MONDAY", "WEDNESDAY", "FRIDAY")
        }
        return pattern.sortedBy { WEEK_DAY_ORDER.indexOf(it) }
    }

    /**
     * The number of training days in [premadeWeekFor] - i.e. what "days per week" means to
     * this screen. Kept beside the pattern so the two can never disagree.
     */
    fun premadeWeekDayCount(daysPerWeek: Int): Int = premadeWeekFor(daysPerWeek).size

    /**
     * True when [count] is a frequency the app supports but does not recommend. The screen
     * marks these with a small "not recommended" label instead of refusing them: the owner
     * asked for both frequencies to stay selectable (2026-10-08).
     */
    fun isNotRecommendedFrequency(count: Int): Boolean = count <= 1 || count >= 7

    /**
     * The TIME window that goes with [durationRangeFor]: 30-45 / 45-60 / 60-75 minutes.
     *
     * The two windows are the SAME decision read two ways, which is why they live beside
     * each other. [durationRangeFor] is what the generator must obey and what the
     * `duration_*_desc` copy explains; this is what the user is told before choosing.
     *
     * The welcome screen's "Sitzungslänge" chips used to render the EXERCISE counts with
     * the minute unit, so every option claimed a length it plainly is not - measured on
     * the Pixel 7a 2026-10-08, they read "6–8 Min." / "8–10 Min." / "10–12 Min." while
     * the numbers were the exercise windows 6-8 / 8-10 / 10-12. A 12-exercise session is
     * the longest option the app builds; 12 minutes is shorter than its own warm-up.
     *
     * Unknown values fall back to the standard window, exactly like [durationRangeFor].
     */
    fun durationMinutesFor(duration: String?): IntRange = when (duration) {
        DURATION_QUICK -> 30..45
        DURATION_FULL -> 60..75
        else -> 45..60
    }

    /** All equipment categories (excluding [EQUIPMENT_ALL]). */
    val allEquipmentCategories: List<String> = ExerciseCatalog.CATEGORIES

    /**
     * The categories a FILTER UI may draw a chip for: every one but [EQUIPMENT_OTHER].
     *
     * WHY "OTHER" HAS NO CHIP. Owner, 2026-10-11: *"make sure all equipments that are from the
     * welcome screen apply and the (other) is not shown"*. The welcome screen cannot grant it -
     * it is not kit - so a chip for it is a control the user never chose, drawn in the same row
     * as the kit they did choose, and it reads as equipment they are missing rather than as the
     * category that needs nothing (a plank, a walk, every stretch and pose: 144 of the
     * catalogue's 601 movements).
     *
     * IT IS HIDDEN FROM THE UI, NOT REMOVED FROM THE FILTER. Every picker still filters by it -
     * `defaultEquipmentFilter` starts from the user's kit PLUS this category - because dropping
     * it is exactly the bug this replaced: those 144 movements became unreachable for every user
     * who had answered the equipment question. Measured 2026-10-11: with the whole catalogue and
     * all eight categories, 0 movements are unreachable; without this one category, 144 are.
     */
    val filterableEquipmentCategories: List<String> =
        allEquipmentCategories.filter { it != EQUIPMENT_OTHER }

    /**
     * The equipment a picker opened INSIDE the setup builder should filter by.
     *
     * The builder's own live selection wins while it has one - the user is choosing kit on that
     * same screen, so the filter must follow the taps. When it has none, the kit the welcome
     * screen already SAVED applies instead, which is the owner's *"make sure all equipments that
     * are from the welcome screen apply"*: opening the exercise editor before re-visiting the
     * equipment step used to fall back to "unrestricted", so a returning user was offered
     * equipment they had already said they do not own.
     *
     * Both inputs are the RAW tag lists as stored ([WorkoutViewModel.savedEquipmentSelection]),
     * never a derived one: the coarse category list is EMPTY for an unrestricted/full-gym
     * selection, so deciding on it would mistake "everything" for "nothing chosen".
     */
    fun pickerEquipmentSelection(
        builderSelection: List<String>,
        savedSelection: List<String>
    ): List<String> = builderSelection.ifEmpty { savedSelection }

    /** The string-resource key for an equipment category's display label. */
    fun equipmentLabelKey(category: String): String = when (category) {
        EQUIPMENT_BODYWEIGHT -> "bodyweight_label"
        EQUIPMENT_BARBELL -> "equipment_barbell"
        EQUIPMENT_DUMBBELL -> "equipment_dumbbell"
        EQUIPMENT_KETTLEBELL -> "equipment_kettlebell"
        EQUIPMENT_CABLE -> "equipment_cable"
        EQUIPMENT_MACHINE -> "equipment_machine"
        EQUIPMENT_BANDS -> "equipment_bands"
        else -> "equipment_other"
    }

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
    )        /**
     * The five canonical training goals (the welcome screen's picker).
     *
     * The persisted keys are the historical ones so no preference or database
     * migration is needed: Hypertrophy keeps the classic "goal_bodybuilding"
     * key, the other four speak for themselves. The scheme functions below
     * accept every retired key via [canonicalGoal] so old stored values keep
     * producing a plan; the picker just never offers them again.
     */
    val allWorkoutGoals = listOf(
        "goal_bodybuilding",   // Hypertrophy
        "goal_strength",       // Strength
        "goal_calisthenics",   // Calisthenics (bodyweight skill)
        "goal_endurance",      // Endurance (stamina / conditioning)
        "goal_general"         // General fitness (health)
    )

    /** The retired keys and the canonical goal each one folds into. */
    private val RETIRED_GOALS = mapOf(
        "goal_powerbuilding" to "goal_bodybuilding", // rep scheme was BB-adjacent
        "goal_functional" to "goal_general",         // movement variety -> health
        "goal_athletic" to "goal_general",
        "goal_olympic" to "goal_strength",           // barbell maximal-force work
        "goal_crossfit" to "goal_endurance"          // conditioning-dominant
    )

    /**
     * Maps any stored or legacy goal key onto one of the five canonical
     * goals. Unknown values fall back to the default (Hypertrophy).
     */
    fun canonicalGoal(goal: String): String {
        if (goal in allWorkoutGoals) return goal
        return RETIRED_GOALS[goal] ?: "goal_bodybuilding"
    }

    /**
     * Week-length tiers for the program structure. Every tier keeps each muscle
     * at ~2x/week — the frequency floor the meta-analyses support (Schoenfeld
     * et al. 2016; Schoenfeld, Grgic & Krieger 2019) — they only change HOW the
     * week is arranged to get there:
     *  - <=3 days: full-body rotation (a split would push muscles to 1x/week).
     *  - 4-5 days: upper/lower alternation (focused sessions, ~2x/week each).
     *  - 6-7 days: dedicated push/pull/legs cycle (spreads the high weekly
     *    volume across movement patterns without dropping any muscle's
     *    frequency — a 1x/week body-part-per-week split at this cadence would
     *    undo the frequency benefit the same research established).
     */
    private const val FULL_BODY_MAX_DAYS = 3

    // ------------------------------------------------------------------
    // Smart day assignment: fixed split templates per training frequency.
    //
    // The old tap-order distribution walked the weekday chips in whatever
    // order the user happened to tap them: lower-body parts went to tap
    // slots 0 and 2 and the majors top-up to tap slot 4, so tapping MONDAY,
    // WEDNESDAY, FRIDAY, SATURDAY, TUESDAY put the majors top-up (quads,
    // hams, glutes …) on TUESDAY — the day after MONDAY's leg day. Legs
    // trained back-to-back with zero recovery.
    //
    // A split template is a SESSION SEQUENCE, not a set of weekday names:
    // the selected weekdays are sorted into calendar order and filled from
    // the sequence, so the pattern itself guarantees the rest rules and the
    // tap order cannot matter any more. Validation ([validatePlan]) keeps
    // this true even if a template ever drifts.
    // ------------------------------------------------------------------

    /** One session in a split template: the broad topics it works. */
    data class SessionTemplate(
        val name: String,
        val topics: List<String>,
        /** True when the session may not carry a broad topic twice in a row across the week. */
        val titleKey: String = ""
    )

    /**
     * A frequency's split: the SESSION SEQUENCE the picked days are filled
     * with, in calendar order (an explicit rest-slot map is optional). The
     * sequence — not the weekday names — is what guarantees recovery: no two
     * consecutive sessions share a strict topic, so the same topics never
     * land on adjacent calendar days when the picked days themselves are not
     * adjacent.
     *
     * [restSlots] (0 = Monday … 6 = Sunday) names which calendar days stay
     * rest days when the user picks MORE days than the template trains; the
     * latest picked days are dropped first so earlier rest days survive.
     */
    data class WorkoutSplitTemplate(
        val frequency: Int,
        val sequence: List<List<String>>,
        val restSlots: Set<Int> = emptySet(),
        /**
         * Session indices whose name is given rather than derived, for the one
         * session that shares its topics with another on purpose: the weak-point
         * day trains exactly what an upper day trains, and calling it "Upper" on
         * screen would hide what it is FOR. Every other session is named by
         * [sessionLabelId] from the topics it trains, so no name can drift away
         * from the work.
         */
        val namedSessions: Map<Int, String> = emptyMap()
    ) {
        init {
            require(frequency == sequence.size) {
                "template frequency $frequency does not match ${sequence.size} sessions"
            }
            require(namedSessions.keys.all { it in sequence.indices }) {
                "named sessions ${namedSessions.keys} fall outside a $frequency-session week"
            }
        }

        /**
         * Assigns the template's sessions to [orderedPickedDays], returning
         * topics per weekday name for all seven days. Days the template
         * treats as rest (either an explicit [restSlots] day or a picked day
         * beyond the session count) stay empty.
         *
         * The sequence is applied in calendar order when that week validates
         * cleanly — the canonical layout. When the picked days make the
         * canonical order violate the recovery rules (e.g. an A/B/A template
         * whose two leg-carrying A sessions land wrap-adjacent on Sunday and
         * Monday), the assignment that minimizes validatePlan failures wins,
         * canonical order breaking ties. The session MIX per week is
         * unchanged; only which day hosts which session adapts.
         */
        fun assign(orderedPickedDays: List<String>): Map<String, List<String>> {
            val weekDays = listOf(
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
            )
            val restSlotSet = restSlots.map { it % 7 }.toSet()
            // Picked days beyond the session count drop from the END, so an
            // explicitly-named rest slot earlier in the week survives.
            val trainable = orderedPickedDays.withIndex()
                .filter { (index, day) -> weekDays.indexOf(day) % 7 !in restSlotSet }
                .map { it.value }
                .take(sequence.size)

            fun topicsFor(order: List<Int>): Map<String, List<String>> {
                val out = weekDays.associateWith { emptyList<String>() }.toMutableMap()
                order.forEachIndexed { i, seqIdx -> out[trainable[i]] = sequence[seqIdx] }
                return out
            }

            // Lexicographic permutations of the session indices — the identity
            // (canonical calendar order) comes first, so ties keep it.
            val k = trainable.size
            if (k > 1) {
                var best: Pair<List<Int>, Int>? = null
                fun search(prefix: MutableList<Int>, remaining: MutableList<Int>) {
                    if (remaining.isEmpty()) {
                        val score = validatePlan(topicsFor(prefix), frequency).size
                        if (score == 0) {
                            best = prefix.toList() to 0
                            return
                        }
                        if (best == null || score < best!!.second) best = prefix.toList() to score
                        return
                    }
                    for (i in remaining.indices) {
                        val next = remaining.removeAt(i)
                        prefix.add(next)
                        search(prefix, remaining)
                        prefix.removeAt(prefix.size - 1)
                        remaining.add(i, next)
                        if (best?.second == 0) return
                    }
                }
                search(mutableListOf(), (0 until k).toMutableList())
                best?.let { (order, _) ->
                    if (order != (0 until k).toList()) return topicsFor(order)
                }
            }
            return topicsFor((0 until k).toList())
        }
    }

    private val FULL_BODY_TOPICS = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
    private val UPPER_TOPICS = listOf("Chest", "Back", "Shoulders", "Arms", "Core")
    private val LOWER_TOPICS = listOf("Legs")
    private val PUSH_TOPICS = listOf("Chest", "Shoulders", "Arms")
    private val PULL_TOPICS = listOf("Back", "Arms")
    /** One pull session of the 7-day week carries core, for the 4-day ab frequency. */
    private val PULL_CORE_TOPICS = listOf("Back", "Arms", "Core")
    private val LEGS_TOPICS = listOf("Legs", "Core")
    private val FULL_BODY_TOPICS_NO_LEGS = listOf("Chest", "Back", "Shoulders", "Arms", "Core")
    /** An accessory-only session carries no strict topic, so it can sit anywhere. */
    private val CORE_ARMS_TOPICS = listOf("Core", "Arms")

    /**
     * Option B's extra day: the upper body again, on purpose. It is named for
     * what it is FOR - the muscle the app's progression data says is lagging -
     * and it carries every upper group because the app has no per-user lagging
     * muscle to narrow it to yet. Making it dynamic is a real follow-up, not a
     * comment: the day must not start picking a different muscle group, or the
     * name becomes a lie. Until then it is an honest "go again over everything
     * above the waist" day, and the legs still get exactly one session.
     */
    private val WEAK_POINT_TOPICS = listOf("Chest", "Back", "Shoulders", "Arms", "Core")

    // Dedicated body-part sessions (the classic Arnold-style split for high
    // frequencies). Legs + core stay fused: the research-backed leg-day layout
    // always pairs squat/hinge/press patterns with core stability work, and a
    // shoulders day is where rear-delt/trap work pays off after two pressing
    // days. "Arms" in a session list takes the arm side from the session's
    // movement pattern (see [partsForTopics]) — on the shoulders day (presses,
    // no rows) that is the triceps side, and on the arms day (rows, no
    // presses) the biceps side, so each arm muscle still trains exactly twice
    // per week across the two pressing and two pulling days.
    private val CHEST_TOPICS = listOf("Chest", "Arms")
    private val BACK_TOPICS = listOf("Back", "Arms")
    // The shoulder day carries secondary chest work: incline pressing hits the
    // upper-chest/front-delt overlap, which is what gives chest its 2x/week
    // frequency without a second chest day.
    private val SHOULDER_TOPICS = listOf("Shoulders", "Chest", "Arms")
    // The arm day opens with vertical pulls (chin-ups) — biceps from a fully
    // stretched position, and back's second weekly touch.
    private val ARMS_TOPICS = listOf("Back", "Arms")

    /**
     * The topics the recovery rules guard strictly: the large, slow-recovering
     * groups. Arms and Core are deliberately excluded — the canonical splits
     * this feature implements (push day before pull day, upper before lower)
     * necessarily ride the small accessory groups on adjacent days; flagging
     * that would reject every real PPL. Shoulders sit on push and upper days
     * in DIFFERENT sessions only — the templates never place two pressing
     * sessions back to back.
     */
    private val STRICT_ADJACENCY_TOPICS = setOf("Chest", "Back", "Legs", "Shoulders")


    /**
     * How the user's training days are organised.
     *
     * The frequency (how many days) and the style (what each day is) are two
     * different choices, and a lot of people only ever see the first one. The
     * three options are the ones the evidence and the published programs
     * actually divide into, and each one is a different answer to "what are you
     * training this week for", not a different name for the same week:
     *
     *  - [OPTION_A] is the default: it lands every major muscle on the SAME
     *    weekly frequency at every day count, which is what the training-load
     *    literature asks for - about twice a week per muscle, and let the split
     *    decide how (Schoenfeld et al. 2016; Grgic et al. 2019).
     *  - [OPTION_B] buys upper-body volume and isolation work with exactly ONE
     *    leg day at every frequency from 2 up. The trade-off is the point and
     *    is named in the UI rather than hidden: the legs are trained once a
     *    week whatever else the week does, and the extra days go to the upper
     *    body and to the user's weak points. It is the only option that cannot
     *    give 2x/week to the legs - that is what the user is choosing.
     *  - [OPTION_C] is the hybrid: a different balance again - more leg days
     *    than B, fewer than a pure body-part rotation, and the only option that
     *    mixes full-body days into a specialised week.
     *
     * Key principle behind all three, and the reason [validatePlan] is written
     * the way it is: total weekly VOLUME per muscle (roughly 10-20 hard sets)
     * and progressive overload matter more than the split itself. Any of these
     * works if volume and intensity are managed, so no option is a trap.
     *
     * The ids are the owner's own option letters so the stored preference
     * survives a rename. Anything unrecognised (including the ids of the
     * pre-option styles) falls back to [DEFAULT], which is the option that
     * gives every muscle the most even week.
     */
    enum class SplitStyle(val id: String) {
        OPTION_A("a"),
        OPTION_B("b"),
        OPTION_C("c");

        companion object {
            val DEFAULT = OPTION_A

            /** The stored id, falling back to the default for anything unknown. */
            fun fromId(id: String?): SplitStyle =
                entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: DEFAULT
        }
    }

    /**
     * Every option's week, keyed by days per week - the three options the owner
     * specified, one row per day count, exactly as laid out:
     *
     * ```
         * Days  Option A                Option B (1 leg day)   Option C
     * 2     Full Body / Full Body    Upper / Lower          Full Body / Full Body
     * 3     Full Body x3             Upper / Lower / Upper  Full Body / Upper / Lower
     * 4     Upper / Lower / U / L    Push / Pull / Legs / Upper
     *                                                 Full Body / Push / Pull / Legs
     * 5     Push / Pull / Legs / U / L
     *                                   Push / Pull / Legs / Upper / Arms & Core
     *                                                 Upper / Lower / Push / Pull / Legs
     * 6     Push / Pull / Legs x2    ... / Weak Point      Upper / Lower / Push / Pull / Legs / Full Body
     * ```
     *
     * Option B's 5- and 6-day rows are the owner's REVISION of the first
     * table, which read "Chest / Back / Legs / Shoulders / Arms": a week of
     * one-part days is not a mix, and a whole day of only shoulders is not what
     * "upper-body volume" means to the person reading it. The one leg day is
     * unchanged - that is still the promise - and what replaces the single-part
     * days is upper-body work that is not a body part.
     *
     * The two day counts the spec does not name (1 and 7) are filled in from
     * the option's own logic rather than left broken, and the gaps are called
     * out in the notes on the weeks themselves. Every week is run through
     * [validatePlan] by SplitStyleLayoutTest for EVERY way of picking the days,
     * because the validator treats Monday as following Sunday.
     */
    private val styleTemplates: Map<SplitStyle, Map<Int, WorkoutSplitTemplate>> = mapOf(
        // -------------------------------------------------------------------
        // OPTION A - balanced muscle-group frequency: every major muscle on the
        // same weekly frequency, ~2x/week wherever the week has the days for it.
        // -------------------------------------------------------------------
        SplitStyle.OPTION_A to mapOf(
            // One day a week leaves nothing to balance: the only week that trains
            // the whole body once is the only week there is.
            1 to WorkoutSplitTemplate(1, listOf(FULL_BODY_TOPICS)),
            // Full Body / Full Body, not Full Body / Upper: a second day that
            // skipped the legs would drop them to 1x/week while everything else
            // hit 2x, which is the lopsidedness this option exists to avoid.
            2 to WorkoutSplitTemplate(2, listOf(FULL_BODY_TOPICS, FULL_BODY_TOPICS)),
            3 to WorkoutSplitTemplate(
                3, listOf(FULL_BODY_TOPICS, FULL_BODY_TOPICS, FULL_BODY_TOPICS)
            ),
            4 to WorkoutSplitTemplate(
                4, listOf(UPPER_TOPICS, LOWER_TOPICS, UPPER_TOPICS, LOWER_TOPICS)
            ),
            // Push / Pull / Legs / Upper / Lower - 2x everywhere: chest and
            // shoulders get the push day and the upper day, the legs get the legs
            // day and the lower day.
            5 to WorkoutSplitTemplate(
                5, listOf(PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, UPPER_TOPICS, LOWER_TOPICS)
            ),
            6 to WorkoutSplitTemplate(
                6, listOf(PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS)
            ),
            // 7 days is past the spec's table. This keeps A's promise at the top
            // end: two of every pattern, one pull of which carries core, and the
            // accessory day last so it can sit anywhere without stacking.
            7 to WorkoutSplitTemplate(
                7, listOf(
                    PUSH_TOPICS, PULL_CORE_TOPICS, LEGS_TOPICS,
                    PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, CORE_ARMS_TOPICS
                )
            ),
        ),
        // -------------------------------------------------------------------
        // OPTION B - upper-body volume and isolation work, exactly ONE dedicated
        // leg day at every frequency. Every extra day goes to the upper body or
        // to the weak points; the legs never get a second session.
        // -------------------------------------------------------------------
        SplitStyle.OPTION_B to mapOf(
            1 to WorkoutSplitTemplate(1, listOf(FULL_BODY_TOPICS)),
            2 to WorkoutSplitTemplate(2, listOf(UPPER_TOPICS, LOWER_TOPICS)),
            3 to WorkoutSplitTemplate(3, listOf(UPPER_TOPICS, LOWER_TOPICS, UPPER_TOPICS)),
            // Push / Pull / Legs / Upper. The upper day cannot be moved: it
            // shares chest and shoulders with the push day, so wherever the user
            // puts the rest day, one of the two lands next to the other. That is
            // what "upper-body volume" means - the upper body is trained on both
            // days - and [validatePlan] exempts a broad day for exactly this
            // reason. Legs: one.
            4 to WorkoutSplitTemplate(
                4, listOf(PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, UPPER_TOPICS)
            ),
            // Push / Pull / Legs / Upper / Arms & Core: the owner's revision of
            // the table, which first asked for "Chest / Back / Legs /
            // Shoulders / Arms" here. One leg day stays exactly as promised, but
            // no day is a single body part any more - a whole day of only
            // shoulders is not a mix, and the fifth day was the difference
            // between the two readings of "upper-body volume": a leg day again
            // (which is Option A) or upper-body work that is not a body part
            // (which is this). Legs: one, upper body: four days.
            5 to WorkoutSplitTemplate(
                5, listOf(PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, UPPER_TOPICS, CORE_ARMS_TOPICS)
            ),
            6 to WorkoutSplitTemplate(
                6,
                listOf(
                    PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, UPPER_TOPICS,
                    CORE_ARMS_TOPICS, WEAK_POINT_TOPICS
                ),
                // The weak-point day is the upper body again, deliberately: it is
                // the day that goes after whatever is lagging, and the option's
                // promise is that the extra day never becomes a second leg day.
                namedSessions = mapOf(5 to SESSION_WEAK_POINT)
            ),
            // 7 is past the spec's table too, and the promise gets tightest
            // here: the seventh session cannot be a leg day, so it goes to the
            // weak points as well - which is the whole thesis of the option.
            7 to WorkoutSplitTemplate(
                7,
                listOf(
                    PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS, UPPER_TOPICS,
                    CORE_ARMS_TOPICS, WEAK_POINT_TOPICS, WEAK_POINT_TOPICS
                ),
                namedSessions = mapOf(5 to SESSION_WEAK_POINT, 6 to SESSION_WEAK_POINT)
            ),
        ),
        // -------------------------------------------------------------------
        // OPTION C - the hybrid: a deliberately different balance, with
        // full-body days mixed into specialised weeks and more leg work than B.
        // -------------------------------------------------------------------
        SplitStyle.OPTION_C to mapOf(
            1 to WorkoutSplitTemplate(1, listOf(FULL_BODY_TOPICS)),
            // The spec's table starts at 2 days, and at 2 days there is only one
            // sensible hybrid: two full-body days, the same week as A.
            2 to WorkoutSplitTemplate(2, listOf(FULL_BODY_TOPICS, FULL_BODY_TOPICS)),
            // Also unnamed in the spec. The hybrid reading is one full-body day
            // plus one upper/lower pair: every muscle gets 2x except the legs,
            // which get the full-body day and the lower day.
            3 to WorkoutSplitTemplate(
                3, listOf(FULL_BODY_TOPICS, UPPER_TOPICS, LOWER_TOPICS)
            ),
            // The full-body day opens the week on purpose: it is the strength
            // day, the heavy compounds, and the push day that follows it is
            // where the volume goes.
            4 to WorkoutSplitTemplate(
                4, listOf(FULL_BODY_TOPICS, PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS)
            ),
            5 to WorkoutSplitTemplate(
                5, listOf(UPPER_TOPICS, LOWER_TOPICS, PUSH_TOPICS, PULL_TOPICS, LEGS_TOPICS)
            ),
            6 to WorkoutSplitTemplate(
                6,
                listOf(
                    UPPER_TOPICS, LOWER_TOPICS, PUSH_TOPICS, PULL_TOPICS,
                    LEGS_TOPICS, FULL_BODY_TOPICS
                )
            ),
            7 to WorkoutSplitTemplate(
                7,
                listOf(
                    UPPER_TOPICS, LOWER_TOPICS, PUSH_TOPICS, PULL_TOPICS,
                    LEGS_TOPICS, FULL_BODY_TOPICS, CORE_ARMS_TOPICS
                )
            ),
        ),
    )

    /** Every style's week for [frequency] days/week. */
    fun splitTemplatesFor(style: SplitStyle): Map<Int, WorkoutSplitTemplate> =
        styleTemplates[style] ?: styleTemplates.getValue(SplitStyle.DEFAULT)

    /**
     * The split template for [frequency] days/week in [style], or null outside
     * 1-7. Callers that predate the style picker get [SplitStyle.DEFAULT].
     */
    fun splitTemplateFor(frequency: Int, style: SplitStyle = SplitStyle.DEFAULT): WorkoutSplitTemplate? =
        splitTemplatesFor(style)[frequency]

    /** The topics the style picker reports, in the order it reports them. */
    val MAJOR_TOPICS = listOf("Chest", "Back", "Shoulders", "Legs")

    /**
     * The week as the list of days it is - "Upper, Lower, Upper, Lower, Arms &
     * Core" - one label per day, in order.
     *
     * This is what the style picker shows, because it is the only line a user
     * can count against the day picker sitting right above it. The per-muscle
     * frequency it replaced ("Chest 2x, Back 2x, Shoulders 2x, Legs 2x") said
     * what the week achieves but not what the week IS, and a wall of four
     * identical numbers reads as one number repeated.
     *
     * The labels are derived from the topics each session trains, never typed
     * next to the layout, so a week edited in [styleTemplates] cannot show a
     * day it does not build. One label per day, always: a mismatch in length
     * would be a picker disagreeing with the day count above it.
     */
    fun dayLabelIds(
        style: SplitStyle = SplitStyle.DEFAULT,
        frequency: Int
    ): List<String> {
        val template = splitTemplateFor(frequency, style) ?: return emptyList()
        return template.sequence.mapIndexed { index, topics ->
            template.namedSessions[index] ?: sessionLabelId(topics)
        }
    }

    /**
     * How many sessions a week [style] puts the legs in, at [frequency]
     * days/week. This is the number the picker states next to every option,
     * because it is the difference between them: Option B's whole promise is
     * that this stays at 1 while the week grows.
     *
     * Counted off the layout rather than typed, so it cannot claim one leg day
     * and build two.
     */
    fun legSessionsPerWeek(
        style: SplitStyle = SplitStyle.DEFAULT,
        frequency: Int
    ): Int {
        val sequence = splitTemplateFor(frequency, style)?.sequence ?: return 0
        return sequence.count { "Legs" in it }
    }

    /**
     * The short name of a single session, from the topics it trains.
     *
     * Ordered widest-first so a session claims its biggest job: a legs session
     * that also carries core is a leg day, and core is the accessory riding
     * along rather than the reason the day exists. "Full body" is reserved for
     * a session that covers the legs AND two other major groups — an upper
     * session reaches three major groups too (chest, back, shoulders) and is
     * still an upper day in every week that contains it. The one session that
     * trains no major group at all is named for what it does train, because
     * there is nothing bigger to call it.
     *
     * The two leg days are deliberately different names for near-identical
     * sessions: a plain legs session is the lower half of an upper/lower week
     * and reads "Lower", while the push/pull/legs leg day carries core and
     * reads "Legs", so a card never says "Lower" under the heading
     * "Push / Pull / Legs". No week mixes the two, so no card shows both.
     *
     * A one-part session is named after the part (Option B's chest / back /
     * shoulders / arms days), which is the whole point of those days.
     */
    fun sessionLabelId(topics: List<String>): String {
        val trains = { topic: String -> topic in topics }
        val majorGroups = MAJOR_TOPICS.count(trains)
        return when {
            trains("Legs") && majorGroups >= 3 -> SESSION_FULL_BODY
            trains("Chest") && trains("Back") && trains("Shoulders") -> SESSION_UPPER
            trains("Chest") && trains("Shoulders") -> SESSION_PUSH
            trains("Back") -> SESSION_PULL
            trains("Legs") && trains("Core") -> SESSION_LEGS
            trains("Legs") -> SESSION_LOWER
            // No major group at all: an accessory-only session, named for what
            // it does train. Core is the difference between "Arms" and
            // "Arms & core", not a day of its own.
            majorGroups == 0 -> if (trains("Core")) SESSION_ARMS_CORE else SESSION_ARMS
            else -> SESSION_LEGS
        }
    }

    const val SESSION_FULL_BODY = "full_body"
    const val SESSION_UPPER = "upper"
    const val SESSION_PUSH = "push"
    const val SESSION_PULL = "pull"
    const val SESSION_LOWER = "lower"
    const val SESSION_LEGS = "legs"
    const val SESSION_ARMS = "arms"
    const val SESSION_ARMS_CORE = "arms_core"
    const val SESSION_WEAK_POINT = "weak_point"

    /**
     * The legacy 5-day default map, now the [SplitStyle.BALANCED] week.
     * Kept as its own name because several callers (and the tests that lock the
     * layout) read it directly.
     */
    val splitTemplates: Map<Int, WorkoutSplitTemplate> get() = splitTemplatesFor(SplitStyle.DEFAULT)

    /** True for the two frequencies the spec asks to warn about (but allow). */
    fun frequencyNeedsWarning(frequency: Int): Boolean = frequency == 1 || frequency == 7

    /**
     * The string-resource key for a frequency's warning, or empty when the
     * frequency needs none. 1 day/week: minimal benefits. 7 days/week: high
     * overtraining risk. Both per the spec (WHO/ACSM/NSCA activity guidance).
     */
    fun frequencyWarningKey(frequency: Int): String = when (frequency) {
        1 -> "frequency_warning_1_day"
        7 -> "frequency_warning_7_day"
        else -> ""
    }

    /**
     * Splits each session's topics across the distinct parts the user picked,
     * in a stable order. "Legs" -> [Quadriceps, Hamstrings, Glutes] when the
     * user picked all three; "Back" -> whatever back parts were picked.
     */
    private fun partsForTopics(topics: List<String>, pickedParts: List<String>): List<String> {
        // "Arms" spans two movement patterns: the biceps are a PULL muscle while
        // the triceps and forearms are PUSH muscles. A PPL template that carries
        // the bare "Arms" topic on both the push and the pull session would train
        // biceps 4x/week (every push AND pull day). The side is read from the
        // session itself: a session that presses (chest/shoulders, no back) takes
        // the push side, one that rows (back, no pressing) takes the pull side —
        // biceps then repeat exactly twice on the 6-day cadence. Sessions that do
        // both (upper, full body) take both sides.
        val presses = topics.any { it == "Chest" || it == "Shoulders" }
        val rows = topics.any { it == "Back" }
        val armSideFilter: (String) -> Boolean = when {
            rows && !presses -> { part ->
                getTopicForMuscle(part) != "Arms" ||
                    (!part.contains("Triceps", ignoreCase = true) && !part.contains("Forearm", ignoreCase = true))
            }
            presses && !rows -> { part ->
                getTopicForMuscle(part) != "Arms" || !part.contains("Biceps", ignoreCase = true)
            }
            else -> { _ -> true }
        }
        val out = mutableListOf<String>()
        topics.forEach { topic ->
            pickedParts.filter { getTopicForMuscle(it) == topic && armSideFilter(it) }
                .sorted()
                .let(out::addAll)
        }
        return out
    }

    /**
     * Validates a generated week against the recovery rules AND the session
     * duration budget:
     *  1. no strict muscle group ([STRICT_ADJACENCY_TOPICS]) trains two
     *     calendar days in a row — Monday follows Sunday too;
     *  2. legs get at most 2 *dedicated* sessions per week below the 6-day
     *     cadence. A dedicated leg session trains legs without any upper-body
     *     topic; a full-body day that includes leg work among six topics is
     *     not one (the 3-day full-body template would otherwise fail its own
     *     definition).
     *
     *  3. every non-empty training day carries between [durationRangeFor]
     *     `min` and `max` exercises (30-45 min = 6-8, 45-60 min = 8-10,
     *     60-75 min = 10-12).
     *
     * [topicsByDay] maps each of the seven weekday names to the broad topics
     * it trains ("MONDAY" to listOf("Chest", "Arms")); empty = rest day.
     * [exercisesPerDay] maps each weekday to that day's exercise count.
     * Returns the rule violations, empty when the week is clean.
     */
    fun validatePlan(
        topicsByDay: Map<String, List<String>>,
        weeklyFrequency: Int,
        exercisesPerDay: Map<String, Int> = emptyMap(),
        duration: String = DURATION_STANDARD
    ): List<String> {
        val weekDays = listOf(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
        )
        val failures = mutableListOf<String>()

        // 1. No strict muscle group two days in a row — Monday follows Sunday too.
        //
        // A BROAD session (3+ strict topics: a full-body day, or an upper day
        // covering chest, back and shoulders) is exempt ON ITS OWN. The reason is
        // the same reason the specified weeks look the way they do: a broad day
        // spreads its volume thinly across several muscles, so a focused day
        // next to it trains a muscle that had a light dose ~24h earlier rather
        // than a hard one. Full Body / Push / Pull / Legs and
        // Push / Pull / Legs / Upper are standard published weeks, and the
        // literature behind them is blunt that weekly volume per muscle and
        // progressive overload matter more than the split itself.
        //
        // Two FOCUSED sessions sharing a strict muscle is still a violation, and
        // that is the rule doing its real work: no two consecutive days both hit
        // the same small set of muscles hard. Note that two full-body days
        // adjacent by the user's own day choice overlap by definition - that is
        // what full-body training IS.
        val isBroadSession: (List<String>) -> Boolean = { topics ->
            topics.count { it in STRICT_ADJACENCY_TOPICS } >= 3
        }
        weekDays.indices.forEach { i ->
            val day = weekDays[i]
            val next = weekDays[(i + 1) % 7]
            val todayTopics = topicsByDay[day].orEmpty()
            val nextTopics = topicsByDay[next].orEmpty()
            val overlap = todayTopics.filter { it in STRICT_ADJACENCY_TOPICS }
                .toSet() intersect nextTopics.filter { it in STRICT_ADJACENCY_TOPICS }.toSet()
            val eitherIsBroad = isBroadSession(todayTopics) || isBroadSession(nextTopics)
            if (overlap.isNotEmpty() && !eitherIsBroad) {
                failures.add("$day and $next train the same muscle groups: $overlap")
            }
        }

        // 2. Dedicated leg sessions cap below the 6-day cadence.
        if (weeklyFrequency < 6) {
            val upperTopics = setOf("Chest", "Back", "Shoulders", "Arms")
            val legDays = weekDays.count { day ->
                val topics = topicsByDay[day].orEmpty()
                "Legs" in topics && topics.none { it in upperTopics }
            }
            if (legDays > 2) {
                failures.add("legs train ${legDays}x on a $weeklyFrequency-day plan (max 2 below the 6-day cadence)")
            }
        }
        // 3. Session duration: each training day must fit the window.
        if (exercisesPerDay.isNotEmpty()) {
            val range = durationRangeFor(duration)
            weekDays.forEach { day ->
                val count = exercisesPerDay[day] ?: 0
                if (count > 0 && count !in range) {
                    failures.add(
                        "$day has $count exercises, expected ${range.first}-${range.last} " +
                            "for a ${durationLabel(duration)} session"
                    )
                }
            }
        }

        return failures
    }

    /** Human-readable duration label for validation messages. */
    fun durationLabel(duration: String?): String = when (duration) {
        DURATION_QUICK -> "30-45 min"
        DURATION_FULL -> "60-75 min"
        else -> "45-60 min"
    }

    /**
     * Guided mode has exactly 3 working sets for every exercise — universal,
     * fixed, and part of the level system's Roman rep walk. Difficulty only
     * selects the starting effort level; the goal only selects reps and rest.
     */
    const val GUIDED_SETS_PER_EXERCISE = 3
    private val UPPER_LOWER_MAX_DAYS = 5

    private val STRATEGY_FULL_BODY = 0
    private val STRATEGY_UPPER_LOWER = 1
    private val STRATEGY_PPL = 2

    /**
     * Which push/pull/legs bucket a body part trains on the split tiers. The
     * setup picker sends precise parts; the broad aliases keep programmatic
     * callers (and the tests) working.
     */
    private fun splitBucketFor(part: String): String? = when (part) {
        "Chest", "Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Triceps", "Shoulders" -> "push"
        "Lats", "Upper Back", "Upper Traps", "Spinal Erectors", "Biceps", "Forearms", "Back", "Arms" -> "pull"
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Legs" -> "legs"
        "Abs", "Obliques", "Core" -> "core"
        else -> null
    }

    /**
     * Parts the upper/lower alternation assigns to the lower sessions (the
     * core parts rotate with upper so ab work opens each upper day).
     */
    private val LOWER_BODY_PARTS = setOf(
        "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors", "Hip Abductors", "Legs"
    )

    /**
     * The two independent knobs the build is parameterised by:
     *  - [workoutGoal] owns the SCHEME: rep range, rest, exercise selection.
     *  - [intensity] owns the EFFORT & VOLUME: the starting LEVEL (the CSV's
     *    easy/medium/hard rungs), sets, exercises per muscle, and the suggested
     *    starting load.
     * Each axis is owned by exactly one control, so nothing double-dips.
     */
    fun generateAssistedProgram(
        bodyParts: List<String>, 
        selectedDays: List<String>,
        intensity: String,
        workoutGoal: String = "goal_bodybuilding",
        includedExercises: List<String> = emptyList(),
        excludedExercises: List<String> = emptyList(),
        equipment: List<String> = emptyList(),
        duration: String = DURATION_STANDARD,
        splitStyle: SplitStyle = SplitStyle.DEFAULT
    ): GeneratedProgram {
        val goal = canonicalGoal(workoutGoal)
        val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val routineDays = mutableListOf<RoutineDayEntity>()
        val routineExercises = mutableListOf<RoutineExerciseEntity>()
        val unsatisfiedParts = mutableListOf<String>()
        val insights = mutableListOf<ScienceInsight>()

        if (bodyParts.isEmpty() || selectedDays.isEmpty()) return GeneratedProgram(emptyList(), emptyList(), emptyList(), emptyList())

        // 1. VOLUME STRATEGY (frequency-driven distribution)
        val majorMuscles = listOf("Chest", "Lats", "Upper Back", "Quadriceps", "Hamstrings", "Glutes")
        val expandedBodyParts = bodyParts.toMutableList()

        // Distribute parts across selected days, strategy chosen by training
        // frequency (Schoenfeld et al. 2016 Sports Med; Schoenfeld, Grgic & Krieger
        // 2019 J Sports Sci; Grgic et al. 2019): a muscle trained 2x/week grows
        // more than 1x/week, while volume-equated higher frequencies add little —
        // so a split is only the better tool once the week has enough days for
        // each part to still recover and every day to stay focused.
        //  - 4+ days: body-part split (Mon chest, Tue arms, Wed legs, …). Each
        //    part's weekly volume lands in one fresh, concentrated session —
        //    higher per-set quality and more total volume per part.
        //  - 3- days: full-body rotation. The major muscles the user picked are
        //    trained on EVERY day (2-3x/week, the evidence-backed sweet spot)
        //    and the remaining parts rotate across days, because concentrating
        //    them into single sessions would mean every muscle is hit only 1x/week.
        // Distribute parts across selected days with the strategy the week's
        // length supports (Schoenfeld et al. 2016 Sports Med; Schoenfeld, Grgic &
        // Krieger 2019 J Sports Sci; Grgic et al. 2019): a muscle trained
        // 2x/week grows more than 1x/week, while volume-equated higher
        // frequencies add little — so every tier below is arranged to keep the
        // ~2x/week floor, and they differ only in how the week is structured:
        //  - <=3 days: full-body rotation. Concentrating parts into sessions
        //    would drop every muscle to 1x/week, so the major muscles the user
        //    picked are trained on EVERY day (2-3x/week each) and the rest
        //    rotate across days.
        //  - 4-5 days: upper/lower alternation. Enough days exist to give each
        //    half its own fresh, focused session and still hit it twice a week
        //    (Mon lower / Tue upper / Thu lower / Fri upper).
        //  - 6-7 days: dedicated push/pull/legs cycle. The high weekly volume an
        //    advanced cadence carries is spread across movement patterns while
        //    every muscle still repeats ~2x/week — a one-part-per-week
        //    arrangement would undo the frequency benefit the same research
        //    established.
        // ---------------------------------------------------------------
        // 1a. SMART DAY ASSIGNMENT — split-template driven.
        //
        // The old tap-order distribution walked the weekday chips in tap
        // order: lower parts went to tap slots 0 and 2 and the majors top-up
        // to tap slot 4, so tapping MONDAY, WEDNESDAY, FRIDAY, SATURDAY,
        // TUESDAY put the majors top-up (quads, hams, glutes …) on TUESDAY,
        // the day after MONDAY's leg day. Legs trained back-to-back.
        //
        // The split template is a SESSION SEQUENCE over the calendar week:
        // the selected weekdays are sorted into week order first, so tap
        // order cannot matter, and the pattern itself spaces the sessions
        // that share a topic. [validatePlan] re-checks the result.
        // ---------------------------------------------------------------
        val strategy = when {
            selectedDays.size <= FULL_BODY_MAX_DAYS -> STRATEGY_FULL_BODY
            selectedDays.size <= UPPER_LOWER_MAX_DAYS -> STRATEGY_UPPER_LOWER
            else -> STRATEGY_PPL
        }
        val weekDaysAll = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        // Calendar order — the tap order is deliberately discarded here.
        val orderedDays = weekDaysAll.filter { it in selectedDays }
        val template = splitTemplateFor(selectedDays.size, splitStyle)
        // Template topics per calendar day (empty on rest days). The template
        // is a SESSION SEQUENCE applied to the picked days in calendar order,
        // so a Mon/Tue/Thu/Fri/Sat pick and a Mon/Wed/Thu/Fri/Sat pick both
        // get the same balanced Push/Pull/Legs/Upper/Lower week.
        val topicsByDay: Map<String, List<String>> = template?.assign(orderedDays) ?: emptyMap()
        val partsPerDay: MutableList<MutableList<String>> = MutableList(weekDaysAll.size) { i ->
            val day = weekDaysAll[i]
            val t = topicsByDay[day].orEmpty()
            mutableListOf<String>().apply { addAll(partsForTopics(t, expandedBodyParts)) }
        }

        if (template == null) {
            // Frequencies outside 1-7 (a custom picker state) keep the old
            // full-body rotation: majors every day, secondaries rotating.
            val workingDays = List(selectedDays.size) { mutableListOf<String>() }
            val majors = expandedBodyParts.filter { it in majorMuscles }
            val secondaries = expandedBodyParts.filter { it !in majorMuscles }
            secondaries.forEachIndexed { index, part ->
                workingDays[index % selectedDays.size].add(part)
            }
            workingDays.forEach { it.addAll(majors) }
            workingDays.forEachIndexed { idx, parts ->
                val slot = weekDaysAll.indexOf(orderedDays[idx])
                if (slot >= 0) partsPerDay[slot] = parts
            }
        }

        // 2. VOLUME — universal constant
        // Guided mode uses 3 working sets for every exercise, at every
        // difficulty. The goal owns reps and rest; it never touches set count.
        val setsPerExercise = GUIDED_SETS_PER_EXERCISE

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

        // Short week: the full-body rotation is exactly why the muscles still get
        // their 2x/week frequency — say so, so the day titles listing several parts
        // read as deliberate rather than cluttered.
        if (strategy == STRATEGY_FULL_BODY) {
            insights.add(ScienceInsight(
                titleKey = "insight_fullbody_title",
                descriptionKey = "insight_fullbody_desc",
                citation = "Schoenfeld et al. (2016)"
            ))
        }

        // Upper/lower tier: each half is trained in its own fresh session twice
        // a week — the same frequency floor, just arranged across more days.
        if (strategy == STRATEGY_UPPER_LOWER) {
            insights.add(ScienceInsight(
                titleKey = "insight_upperlower_title",
                descriptionKey = "insight_upperlower_desc",
                citation = "Schoenfeld et al. (2016)"
            ))
        }

        // Dedicated tier: from five days a body-part split concentrates each
        // muscle's weekly volume into one fresh, focused session — per-session
        // quality and total volume per part go up, while the paired arm work
        // keeps every muscle at its 2x/week frequency (Schoenfeld 2016 vs
        // 2019: frequency pays until volume is equated — which is exactly
        // what concentrating the sessions does).
        if (selectedDays.size >= 5) {
            insights.add(ScienceInsight(
                titleKey = "insight_ppl_title",
                descriptionKey = "insight_ppl_desc",
                citation = "Schoenfeld et al. (2017)"
            ))
        }

        // Validation is a precondition of the build: the template guarantees
        // the rest rules, and this check proves it for the parts actually
        // picked. A failure would mean a template regression — surface it in
        // unsatisfiedParts rather than shipping an unbalanced week.
        //
        // The guarantee is predicated on the picked parts COVERING every topic
        // the sessions use (the guided flow's default). A narrow pick shrinks
        // the sessions below the validator's full-body exemption, and then
        // adjacency between the user's own picked days (Sunday + Monday, say)
        // can be unavoidable no matter how the sessions are arranged — the
        // assignment search in assign() already minimizes it. Reporting such
        // phantom violations as unsatisfied parts would blame the user for
        // their own day choice, so the diagnostic only runs with full cover.
        val pickedTopics = expandedBodyParts
            .mapNotNull { getTopicForMuscle(it).takeIf { t -> t != "Other" } }
            .toSet()
        val sessionsCoveredByPick = topicsByDay.values.flatten().all { it in pickedTopics }
        if (sessionsCoveredByPick) {
            val plannedTopics = weekDaysAll.associateWith { day ->
                partsPerDay[weekDaysAll.indexOf(day)].mapNotNull { p ->
                    getTopicForMuscle(p).takeIf { it != "Other" }
                }.distinct()
            }
            val validationFailures = validatePlan(plannedTopics, selectedDays.size)
            if (validationFailures.isNotEmpty()) {
                validationFailures.forEach { failures ->
                    println("WorkoutTemplates: plan validation failed: $failures")
                }
                unsatisfiedParts.addAll(validationFailures)
            }
        }

        // DURATION VALIDATION — every session must fit the selected window
        // (the split-template rule). The generator trims/pads below, so a
        // residual failure would mean a part whose pool could not fill a day.
        if (!isValidDuration(duration)) {
            unsatisfiedParts.add("invalid duration: $duration")
        }

        insights.add(ScienceInsight(
            titleKey = "insight_goal_title",
            descriptionKey = "insight_${goal}_desc",
            citation = "ACSM Guidelines"
        ))

        insights.add(ScienceInsight(
            titleKey = "insight_rest_title",
            descriptionKey = "insight_rest_${goal}_desc",
            citation = "Henselmans et al. (2014)"
        ))

        // 5. PROGRAM GENERATION
        val usedIncluded = mutableSetOf<String>()
        val exerciseOccurrenceCount = mutableMapOf<String, Int>()
        // How many times each part has already been scheduled this week. The
        // candidate pool for a part rotates by this count, so the same part
        // landing on several days (full-body rotation, upper/lower pairs)
        // picks DIFFERENT exercises each time instead of duplicating day one.
        val partOccurrenceCount = mutableMapOf<String, Int>()

        // Session duration owns the per-day exercise budget: 30-45 min = 6-8
        // (compound focus), 45-60 min = 8-10 (balanced), 60-75 min = 10-12
        // (full volume). Every day is trimmed or padded into the window
        // before the program is returned.
        val durationRange = durationRangeFor(duration)

        days.forEach { dayName ->
            val workoutIndex = selectedDays.indexOf(dayName)
            val isWorkout = workoutIndex != -1
            
            if (isWorkout) {
                // TEMPLATE-DRIVEN PARTS: when a split template owns this week,
                // the parts come from the calendar-day topic map. The tap-order
                // path used selectedDays.indexOf, which made the same taps in a
                // different order produce a different (sometimes back-to-back
                // legs) week. weekDaysAll.indexOf is calendar order.
                val currentParts = if (template != null) {
                    partsPerDay[weekDaysAll.indexOf(dayName)]
                } else {
                    partsPerDay[workoutIndex]
                }
                val satisfiedPartsOnThisDay = mutableListOf<String>()
                val usedOnThisDay = mutableSetOf<String>()

                // Per-day duration budget. The day is built compounds-first
                // (rule: "select 2-3 compound movements first"), so an automatic
                // trim always cuts the newest isolation/accessory rows.
                // The ceiling is HARD: a day with more parts than slots simply
                // stops filling once the budget is spent — the picked-but-skipped
                // parts are covered by the sibling session of the same topic.
                val targetCount = durationRange.last
                // Variation rule: the same exercise never appears twice in one
                // week (rule: "do not repeat the same exercise twice in one week
                // unless it's a different variation"). Week-level pool rotation
                // already supplies the variations.
                val usedThisWeek = routineExercises.map { it.name }.toMutableSet()
                
                // Round-robin the parts across TOPIC groups so a day's budget
                // spreads over the muscle groups the session is for (push: chest
                // + shoulders + triceps, not chest ×4). [partsForTopics] emits
                // parts grouped by template topic; interleaving one part per
                // group keeps every group early enough to reach the budget.
                val sortedParts = run {
                    fun interleave(parts: List<String>): List<String> {
                        val groups = parts.groupBy { getTopicForMuscle(it) }.values
                        val out = mutableListOf<String>()
                        var round = 0
                        while (out.size < parts.size) {
                            groups.forEach { g -> if (round < g.size) out.add(g[round]) }
                            round++
                        }
                        return out
                    }

                    if (currentParts.size <= targetCount) {
                        // The day fits: one topic-interleaved pass reaches every
                        // scheduled part inside the budget.
                        interleave(currentParts)
                    } else {
                        // SQUEEZE — full-body days carry every picked part while
                        // the duration ceiling cannot. The cut list is chosen the
                        // way a coach writes the session instead of by list
                        // position: majors lead every day, and secondaries that
                        // have not been trained yet this week rotate in ahead of
                        // already-covered ones. The WEEK then covers every picked
                        // muscle (the balance card stays green) while majors keep
                        // their 2-3x/week frequency — a static order would starve
                        // the same tail parts (triceps, calves) on every day.
                        val majorQ = interleave(currentParts.filter { it in majorMuscles })
                        val secondaryQ = interleave(currentParts.filter { it !in majorMuscles })
                        val uncovered = secondaryQ.filter { (partOccurrenceCount[it] ?: 0) == 0 }
                        val covered = secondaryQ.filter { (partOccurrenceCount[it] ?: 0) > 0 }
                        val out = mutableListOf<String>()
                        var u = 0
                        var m = 0
                        while (u < uncovered.size || m < majorQ.size) {
                            if (u < uncovered.size) out.add(uncovered[u++])
                            if (m < majorQ.size) out.add(majorQ[m++])
                        }
                        out.addAll(covered)
                        out
                    }
                }
                
                sortedParts.forEachIndexed { partIndex, part ->
                    var available = getExercisesForPart(part)
                    // Popularity first: within every later ranking tier
                    // (on-part, compound-first) the stable sorts keep this
                    // order as the tiebreaker, so the picked head of a pool
                    // is the movements programs actually use — not whichever
                    // obscure variation catalog order surfaced first.

                    // Restrict the pool to the equipment the user actually has
                    // access to (empty = every category). The equipment promise
                    // is hard: a part with no allowed exercise must NOT silently
                    // fall back to the full pool — that is how a "bodyweight
                    // only" build used to hand the user a barbell squat. When
                    // the direct filter empties the pool, the substitution chain
                    // reopens it with an equivalent movement from the nearest
                    // available link (Barbell Squat -> Goblet Squat -> Bodyweight
                    // Squat); the part's own bodyweight movements are the last
                    // resort so a leg day is never dropped for missing kit.
                    // The fine-tag gate: null = no restriction, an empty granted
                    // set = strict bodyweight-only. No-kit movements need no
                    // special case — their empty requirement set passes any
                    // granted set, so "the user's equipment + bodyweight" holds
                    // structurally (a dumbbells-only build still offers Push-Ups).
                    grantedTagsFor(equipment)?.let { allowedSet ->
                        available = widenWithSubstitutes(available, allowedSet)
                    }

                    available = available.filter { it !in excludedExercises }

                    // Beginner builds only use progression-validated exercises:
                    // rows flagged for coach review or failed validation never
                    // enter a premade strength build for a new user. Falls back
                    // to the unfiltered pool for a part with nothing eligible
                    // left, so a day is never left empty by a data flag.
                    if (classifyIntensity(intensity) == "Easy") {
                        val eligible = available.filter { isBeginnerBuildEligible(it) }
                        if (eligible.isNotEmpty()) available = eligible
                    }

                    // HARD FILTERING BASED ON GOAL & INTENSITY
                    // The full allowed pool is kept aside BEFORE the goal
                    // preference narrows it: it feeds the week-variety fallback
                    // so a repeated part rotates through every exercise the
                    // user can actually do before any exercise has to repeat.
                    val equipmentPool = available
                    // The goal preferences narrow the pool but never widen it:
                    // every branch's output is intersected with the equipment
                    // gate. The calisthenics branch used to REPLACE the gated
                    // pool with the raw "Bodyweight" category list, which let
                    // TRX/ring moves (catalogued as Bodyweight but requiring
                    // gymnastic_rings) into builds whose kit had none — caught
                    // by WeeklyProgramFuzzTest.
                    fun gate(names: List<String>): List<String> =
                        grantedTagsFor(equipment)?.let { allowed ->
                            names.filter { ExerciseEquipmentTags.isAvailable(it, allowed) }
                        } ?: names
                    available = when(goal) {
                        "goal_calisthenics" -> {
                            val bw = gate(getBodyweightExercises(part))
                            if (bw.isNotEmpty()) bw
                            else gate(available.filter { !it.contains("Barbell", true) && !it.contains("Machine", true) })
                                .ifEmpty { available }
                        }
                        "goal_strength" -> {
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
                        "goal_bodybuilding" -> {
                            // If intensity is HARD, prefer Barbell variations for primary lifts
                            val preferred = if (intensity == "Hard" && part in majorMuscles) {
                                available.filter { it.contains("Barbell", true) || it.contains("Bench Press", true) || it.contains("Squat", true) }
                            } else emptyList()

                            val bb = available.filter { 
                                it.contains("Cable", true) || it.contains("Machine", true) || 
                                it.contains("Incline", true) || it.contains("Decline", true) ||
                                it.contains("Flyes", true) || it.contains("Curl", true) || 
                                it.contains("Extension", true) || it.contains("Pushdown", true) ||
                                it.contains("Skull", true) || it.contains("Bench Dips", true) ||
                                it.contains("Chin-up", true) || it.contains("Raise", true) ||
                                it.contains("Pec Deck", true) || it.contains("Preacher", true)
                            }
                            if (preferred.isNotEmpty()) preferred + bb else if (bb.isNotEmpty()) bb else available
                        }
                        "goal_endurance" -> {
                            // Conditioning-dominant: keep the blood moving with
                            // unilateral, ballistic and carry patterns.
                            val cond = available.filter {
                                it.contains("Lunge", true) || it.contains("Step-up", true) ||
                                    it.contains("Single-leg", true) || it.contains("Thruster", true) ||
                                    it.contains("Kettlebell", true) || it.contains("Kettlebell Swing", true) ||
                                    it.contains("Burpee", true) || it.contains("Jump", true) ||
                                    it.contains("Carry", true) || it.contains("Rower", true) ||
                                    it.contains("Bike", true) || it.contains("Jump Rope", true)
                            }
                            if (cond.isNotEmpty()) cond else available
                        }
                        "goal_general" -> {
                            // Health-first: movement variety across patterns —
                            // unilateral, hinging, carrying and core-stability work
                            // rank ahead of machine isolation.
                            val general = available.filter {
                                it.contains("Lunge", true) || it.contains("Step-up", true) ||
                                    it.contains("Single-leg", true) || it.contains("Kettlebell", true) ||
                                    it.contains("Plank", true) || it.contains("Bridge", true) ||
                                    it.contains("Carry", true) || it.contains("Deadlift", true) ||
                                    it.contains("TRX Row", true) || it.contains("Press", true)
                            }
                            if (general.isNotEmpty()) general else available
                        }
                        else -> available
                    }

                    // Suggest on-part exercises first: at Easy difficulty only ONE
                    // exercise is picked per part, and the unsorted pool could offer a
                    // cross-listed movement (Dips under Chest) whose stored PRIMARY
                    // muscle is another group - which flipped the setup's balance
                    // warning to "incomplete" with no user-visible cause. Ordering by
                    // stored primary muscle matching the part keeps the default build
                    // green at every difficulty, and changing ONLY the equipment later
                    // re-ranks within the allowed pool the same way, so a green build
                    // stays green (and a red one only changes for a real reason).
                    // Gear effectiveness breaks ties between equally on-part rows, so
                    // the most effective implement the user owns comes first.
                    available = available.sortedBy { name ->
                        val primary = getMuscleGroupForExercise(name)
                        ((if (getTopicForMuscle(primary) == getTopicForMuscle(part)) 0 else 1) * 10) +
                            equipmentEffectivenessRank(name)
                    }

                    if (available.isEmpty() && includedExercises.none { exerciseMatchesPart(it, part) }) {
                        unsatisfiedParts.add(part)
                    } else {
                        satisfiedPartsOnThisDay.add(part)

                        // Compounds lead the session: the pool is ranked
                        // compound-before-isolation (catalog `level`) after the
                        // on-part ranking, so the 2-3 movements picked from the
                        // head of the pool are the day's primary lifts and the
                        // trimming tail is always isolation/accessory work, with
                        // the most effective gear winning the ties inside each
                        // role.
                        available = available.sortedBy { name ->
                            ((if (ExerciseCatalog.entryFor(name)?.level == "compound") 0 else 1) * 10) +
                                equipmentEffectivenessRank(name)
                        }
                        
                        // The intensity still owns how many exercises go to
                        // ONE part; the duration owns how many the DAY may
                        // carry. A Full Session adds one accessory per part
                        // (capped at the Hard share) so its days land mid-window
                        // instead of scraping the floor. One slot is RESERVED
                        // for every scheduled part after this one, so every
                        // muscle the session is FOR gets at least one exercise
                        // before an earlier part takes its second — the coach
                        // spec's muscle-group distribution instead of one group
                        // eating the whole budget.
                        val remaining = targetCount - routineExercises.count { it.dayOfWeek == dayName }
                        val reserve = (sortedParts.size - partIndex - 1).coerceAtLeast(0)
                        // SQUEEZE = the day cannot fit every scheduled part even
                        // once. Breadth beats depth: one exercise per part per day
                        // so the queue advances and a part with 2-share cannot
                        // spend the slots its neighbours need (triceps starvation).
                        val isSqueezedDay = sortedParts.size > targetCount
                        val share = if (duration == DURATION_FULL && !isSqueezedDay) {
                            val intensityShare = when(intensity) {
                                "Hard" -> 3
                                "Medium" -> 2
                                else -> 1
                            }
                            (intensityShare + 1).coerceAtMost(3)
                        } else {
                            when(intensity) {
                                "Hard" -> 3
                                "Medium" -> 2
                                else -> 1
                            }
                        }
                        val dayShare = if (isSqueezedDay) 1 else share
                        val exerciseLimit = if (remaining > reserve) minOf(dayShare, remaining - reserve)
                                            else minOf(dayShare, remaining)
                        if (exerciseLimit <= 0) return@forEachIndexed
                        
                        var exercisesPicked = 0
                        // The rotation offset is read once per scheduled part —
                        // the same part on a later day rotates past what earlier
                        // days already picked.
                        val poolRotation = partOccurrenceCount.getOrPut(part) { 0 }
                        partOccurrenceCount[part] = poolRotation + 1
                        // Explicit user picks are a hard request: the goal-keyword
                        // narrowing above is a *preference* mechanism for the
                        // auto-suggestion, not a veto. An included exercise enters
                        // the day as long as it matches the part and the equipment
                        // the user actually has — with the 1,470-exercise pool the
                        // keyword filter otherwise silently dropped picks like
                        // Push-Up (no functional keyword in the name). A pick whose
                        // kit is missing substitutes to the nearest available chain
                        // link (Barbell Bench Press with dumbbells only -> Dumbbell
                        // Bench Press) instead of silently vanishing; only when no
                        // chain link matches the part is the pick dropped, as before.
                        val matchingIncluded = includedExercises.filter {
                            exerciseMatchesPart(it, part) && it !in usedIncluded
                        }.mapNotNull { inc ->
                            if (equipment.isEmpty() || getEquipmentForExercise(inc) in equipment) {
                                inc
                            } else {
                                val sub = substituteExercise(inc, equipment)
                                if (sub != null && exerciseMatchesPart(sub, part) && sub !in usedIncluded) sub else null
                            }
                        }
                        
                        matchingIncluded.take(exerciseLimit).forEach { exName ->
                            usedThisWeek.add(exName)
                            val occurrence = exerciseOccurrenceCount.getOrPut(exName) { 0 } + 1
                            exerciseOccurrenceCount[exName] = occurrence
                            
                            routineExercises.add(RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayName,
                                name = exName,
                                // Store the exercise's own precise primary muscle, not the
                                // broad part the picker chose from: Push-ups offered by the
                                // bodyweight pool under "Front Deltoids" are still a chest
                                // movement, and storing them as a deltoid is what made rows
                                // display as shoulders while every resolver said Chest.
                                muscleGroup = getMuscleGroupForExercise(exName).ifBlank { part },
                                targetSets = setsPerExercise,
                                // Goal owns reps + rest; difficulty owns the effort label.
                                targetReps = getGoalReps(exName, goal),
                                targetIntensity = getGoalIntensity(goal, intensity, exName),
                                targetWeightKg = 0.0,
                                restSeconds = getGoalRestSeconds(goal, exName),
                                transitionRestSeconds = 90, 
                                videoUrl = "",
                                orderIndex = (partIndex * 10) + exercisesPicked,
                                tempo = ""
                            ))
                            usedIncluded.add(exName)
                            usedOnThisDay.add(exName)
                            exercisesPicked++
                        }

                        if (exercisesPicked < exerciseLimit) {
                            // Dedup tiers, strictest first:
                            //  1. week-fresh candidates from the goal-narrowed pool;
                            //  2. week-fresh candidates from the full equipment pool
                            //     (variety beats the goal keyword preference);
                            //  3. day-fresh candidates from the narrowed pool — a
                            //     week-level repeat is acceptable, the same exercise
                            //     twice in ONE day never is. When even this is empty
                            //     the part simply contributes nothing this day; its
                            //     topic is covered by the sibling part that shares it.
                            val freshNarrow = available.filter {
                                it !in usedIncluded && it !in usedOnThisDay && it !in usedThisWeek
                            }
                            val freshWide = if (freshNarrow.isNotEmpty()) freshNarrow
                                            else equipmentPool.filter {
                                it !in usedIncluded && it !in usedOnThisDay && it !in usedThisWeek
                            }
                            val candidates = if (freshWide.isNotEmpty()) freshWide
                                             else {
                                                 // Frequency floor beats variety: a
                                                 // part whose topic has NOT yet
                                                 // trained on 2 distinct days this
                                                 // week may repeat week-level
                                                 // exercises (rotated past earlier
                                                 // picks) so the muscle keeps its
                                                 // evidence-backed 2x/week. Topics
                                                 // already at 2 days stay silent —
                                                 // their volume rides the sibling
                                                 // parts and the padding stage.
                                                 val topic = getTopicForMuscle(part)
                                                 val daysSeen = routineExercises
                                                     .filter { it.dayOfWeek != dayName }
                                                     .mapNotNull { ex ->
                                                         if (getTopicForMuscle(ex.muscleGroup) == topic) ex.dayOfWeek else null
                                                     }.distinct().size
                                                 if (daysSeen < 2) {
                                                     available.filter { it !in usedOnThisDay }
                                                 } else if (poolRotation == 0) {
                                                     available.filter { it !in usedOnThisDay }
                                                 } else {
                                                     emptyList()
                                                 }
                                             }
                            // A part with nothing left contributes nothing; the
                            // day budget is refilled from sibling parts instead.
                            if (candidates.isEmpty()) return@forEachIndexed
                            // Candidates from the wide pool need the compound-first
                            // and on-part ranking applied; for already-ranked
                            // candidates the stable sort is a no-op.
                            val unrotated = candidates.sortedBy { name ->
                                val primary = getMuscleGroupForExercise(name)
                                ((((if (getTopicForMuscle(primary) == getTopicForMuscle(part)) 0 else 1) * 2) +
                                    (if (ExerciseCatalog.entryFor(name)?.level == "compound") 0 else 1)) * 10) +
                                    equipmentEffectivenessRank(name)
                            }
                            // Week-level variety: rotate the candidate pool by the
                            // part's occurrence count so repeated days work fresh
                            // movements rather than repeating the first pick.
                            val pool = if (unrotated.isEmpty()) unrotated
                                       else unrotated.drop(poolRotation % unrotated.size) +
                                            unrotated.take(poolRotation % unrotated.size)
                            
                            pool.take(exerciseLimit - exercisesPicked).forEach { exName ->
                                usedThisWeek.add(exName)
                                val occurrence = exerciseOccurrenceCount.getOrPut(exName) { 0 } + 1
                                exerciseOccurrenceCount[exName] = occurrence
                                
                                routineExercises.add(RoutineExerciseEntity(
                                    id = 0,
                                    dayOfWeek = dayName,
                                    name = exName,
                                    // Same as above: the stored group is the precise primary
                                    // muscle ("Pectorals"), never the broad picker part.
                                    muscleGroup = getMuscleGroupForExercise(exName).ifBlank { part },
                                    targetSets = setsPerExercise,
                                    // Goal owns reps + rest; difficulty owns the effort label.
                                    targetReps = getGoalReps(exName, goal),
                                    targetIntensity = getGoalIntensity(goal, intensity, exName),
                                    targetWeightKg = 0.0,
                                    restSeconds = getGoalRestSeconds(goal, exName),
                                    transitionRestSeconds = 90,
                                    videoUrl = "",
                                    orderIndex = (partIndex * 10) + exercisesPicked,
                                    tempo = ""
                                ))
                                usedOnThisDay.add(exName)
                                exercisesPicked++
                            }
                        }
                    }
                }
                
                // DURATION BUDGET — the last gate of a day. Everything above
                // builds generously; this trims or pads the finished day into
                // the selected window. Trimming (over the ceiling) drops the
                // day's tail — ranked newest/isolation-last — so the compound
                // core survives. Padding (under the floor, possible when a part
                // was budgeted a single exercise) pulls the part's next
                // week-fresh candidate so Short builds still cover the muscle
                // with 6-8 exercises.
                var dayExs = routineExercises.filter { it.dayOfWeek == dayName }
                val overBy = dayExs.size - durationRange.last
                if (overBy > 0) {
                    // Trimmed from the END of the session, which is the same
                    // order the day is built in, so the least important work
                    // goes first and core — now last — is what a too-long day
                    // loses before anything else.
                    val trimmed = dayExs.sortedWith(compareBy(
                        { if (isCoreExercise(it.name)) 1 else 0 },
                        { getExercisePriority(it.name) },
                        { it.orderIndex }
                    )).takeLast(overBy).map { it.name }.toSet()
                    routineExercises.removeAll { it.dayOfWeek == dayName && it.name in trimmed }
                    dayExs = routineExercises.filter { it.dayOfWeek == dayName }
                }
                var paddedCount = 0
                if (dayExs.size < durationRange.first) {
                    currentParts.forEach { part ->
                        if (dayExs.size + paddedCount >= durationRange.first) return@forEach
                        var padPool = getExercisesForPart(part)
                        grantedTagsFor(equipment)?.let { allowedSet ->
                            padPool = widenWithSubstitutes(padPool, allowedSet)
                        }
                        padPool = padPool.filter {
                            it !in usedThisWeek && it !in excludedExercises && dayExs.none { r -> r.name == it } &&
                                routineExercises.none { r -> r.dayOfWeek == dayName && r.name == it }
                        }
                        // Padded rows follow the same compounds-first rule as
                        // the regular picks.
                        padPool = padPool.sortedBy { name ->
                            val primary = getMuscleGroupForExercise(name)
                            ((((if (getTopicForMuscle(primary) == getTopicForMuscle(part)) 0 else 1) * 2) +
                                (if (ExerciseCatalog.entryFor(name)?.level == "compound") 0 else 1)) * 10) +
                                equipmentEffectivenessRank(name)
                        }
                        padPool.take(durationRange.first - dayExs.size - paddedCount).forEach { exName ->
                            usedThisWeek.add(exName)
                            paddedCount++
                            routineExercises.add(RoutineExerciseEntity(
                                id = 0,
                                dayOfWeek = dayName,
                                name = exName,
                                muscleGroup = getMuscleGroupForExercise(exName).ifBlank { part },
                                targetSets = setsPerExercise,
                                targetReps = getGoalReps(exName, goal),
                                targetIntensity = getGoalIntensity(goal, intensity, exName),
                                targetWeightKg = 0.0,
                                restSeconds = getGoalRestSeconds(goal, exName),
                                transitionRestSeconds = 90,
                                videoUrl = "",
                                orderIndex = 100 + paddedCount,
                                tempo = ""
                            ))
                        }
                    }
                }

                val dayExsFinal = routineExercises.filter { it.dayOfWeek == dayName }
                val sortedDayExs = dayExsFinal.sortedWith(compareBy(
                    // Core closes the session, always. This key comes FIRST so
                    // it outranks everything below, including the compound rule:
                    // a "compound" core movement (a loaded carry, a cable chop)
                    // must not be promoted to the top of the session to lead it.
                    { if (isCoreExercise(it.name)) 1 else 0 },
                    // Compound movements open the session (rule: "select 2-3
                    // compound movements first"), regardless of equipment tier —
                    // a bodyweight squat must still lead a machine accessory.
                    { if (ExerciseCatalog.entryFor(it.name)?.level == "compound") 0 else 1 },
                    { getExercisePriority(it.name) },
                    { it.orderIndex }
                ))
                routineExercises.removeAll(dayExsFinal.toSet())
                routineExercises.addAll(sortedDayExs.mapIndexed { i, ex -> ex.copy(orderIndex = i + 1) })

                // A tapped day that ends up with nothing is only honest if the
                // user can find out why. It happens when the kit cannot serve the
                // session at all: a bands + stairs setup has no biceps curl at
                // all, so the Back/Arms session the template scheduled has
                // nothing to fill it with. Rest is the right outcome; a silent
                // rest day is not, so the parts go on the unsatisfied list the
                // balance card reads.
                if (satisfiedPartsOnThisDay.isEmpty() && currentParts.isNotEmpty()) {
                    unsatisfiedParts.addAll(currentParts)
                }

                // A split day names its one or two parts; a full-body day works most
                // of the picked pool, so the title is capped to keep it readable.
                val distinctParts = satisfiedPartsOnThisDay.distinct()
                val partsLabel = if (distinctParts.size > 4) {
                    distinctParts.take(3).joinToString(", ") + ", …"
                } else {
                    distinctParts.joinToString(", ")
                }
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

        return GeneratedProgram(
            routineDays,
            routineExercises.map { it.copy(targetReps = enforceRepRange(it.targetReps)) },
            unsatisfiedParts.distinct(),
            insights
        )
    }

    // =====================================================================
    // THE ENGINE WEEK — the owner's pasted template-engine prompt applied
    // to the welcome screen (owner, 2026-10-06: "apply the exercise making
    // changes for the welcome screen with the things and information i gave
    // you, and then update the app on my phone"). The prompt is the spec;
    // this is its execution against the local 601-row pool. WHAT CHANGES
    // versus [generateAssistedProgram] — each item is the prompt's own:
    //
    //  - THE SPLIT IS THE PROMPT'S TABLE (Step 1), not styleTemplates:
    //    1-2 days full body, 3 full body, 4 upper/lower x2, 5
    //    push/pull/legs/upper/lower, 6 PPL x2, 7 PPL/upper/lower/arms/core.
    //  - THE GOAL OWNS SETS AS WELL AS REPS AND REST (Step 3): the old
    //    generator pins 3 working sets everywhere ("the goal owns reps and
    //    rest; it never touches set count") — the prompt's tables put
    //    4x8-12, 5x4-6, 3x15-20 per SLOT. The prompt wins on this path.
    //  - SLOT ROLES: compound leads slots 1-2, isolation closes the last
    //    two slots (Step 2).
    //  - DAY-TYPE -> MUSCLES is the prompt's mapping (push = chest, front/
    //    side delts, triceps, ...), so picked body parts no longer decide
    //    which muscles a day trains; they stay recorded in prefs.
    //  - DURATION (Step 4) is applied BY TIME, not by a flat exercise
    //    count. The prompt's "~1 exercise per 10 minutes" assumed its own
    //    slots cost ~10 minutes each; these templates cost very different
    //    amounts (a strength slot of 5x4-6 at 120s rest is ~13 minutes, an
    //    endurance slot of 3x15-20 at 30s rest is ~5.6), so a flat 4/5/6
    //    produced 21-minute "30-45 min" days and 31-minute "60-75 min"
    //    days — the owner's report (2026-10-06, "the session length the
    //    user inputs is not correctly applied"). Slots are now added while
    //    the running cost is under the window's MIDPOINT and never past its
    //    END, and the working-sets guard scales with the window (20/24/28)
    //    because the prompt's flat 20-set guard is what collapsed its own
    //    long windows onto its short ones.
    //  - EXCLUSIONS (Step 2): case-insensitive partial match, resolved
    //    through ExerciseLocalization.canonicalFor so an old name still
    //    excludes the row the rename pipeline moved.
    //
    // WHAT DOES NOT CHANGE: the fine-tag equipment gate (the prompt's
    // equipment filter IS the user's kit), canonical English storage with
    // display-time localization, and the level system, which layers its own
    // prescriptions over these stored rows. [generateAssistedProgram] is
    // kept intact — the existing suite pins it and its tests call it
    // directly — only the welcome screen and goal regeneration call THIS.
    //
    // INERT PARAMETERS: bodyParts and splitStyle are accepted so every
    // call site keeps its shape (both are persisted for prefs/other
    // consumers), but the engine's table decides the week. The prompt has
    // no include list, so includedExercises are PINNED to the head of
    // their muscle's pool rather than force-slotted — a forced slot would
    // break the compound/isolation role order the prompt does specify.
    // =====================================================================

    /** One template slot: the prompt's sets x reps x rest (Step 3 tables). */
    private data class EngineSlot(val sets: Int, val reps: String, val rest: Int)

    /** The prompt's goal families mapped onto this app's canonical goal keys. */
    private fun engineGoalKey(goal: String): String = when (canonicalGoal(goal)) {
        "goal_strength" -> "strength"
        "goal_endurance" -> "endurance"   // the prompt groups fat_loss with endurance
        "goal_calisthenics", "goal_general" -> "general"
        else -> "hypertrophy"
    }

    private val ENGINE_TEMPLATES: Map<String, List<EngineSlot>> = mapOf(
        "hypertrophy" to listOf(
            EngineSlot(4, "8–12", 90), EngineSlot(3, "8–12", 90), EngineSlot(3, "10–15", 60),
            EngineSlot(3, "12–15", 45), EngineSlot(2, "12–15", 45)
        ),
        "strength" to listOf(
            EngineSlot(5, "4–6", 120), EngineSlot(4, "5–8", 90),
            EngineSlot(3, "8–10", 60), EngineSlot(3, "10–12", 45)
        ),
        "endurance" to listOf(
            EngineSlot(3, "15–20", 30), EngineSlot(3, "15–20", 30),
            EngineSlot(3, "15–20", 30), EngineSlot(2, "20–25", 30)
        ),
        "general" to listOf(
            EngineSlot(3, "8–12", 90), EngineSlot(3, "8–12", 60),
            EngineSlot(3, "10–15", 60), EngineSlot(2, "12–15", 45)
        )
    )

    /** The isolation slot appended when duration asks for more than the template has. */
    private val ENGINE_ISOLATION_TOPUP: Map<String, EngineSlot> = mapOf(
        "hypertrophy" to EngineSlot(2, "12–15", 45),
        "strength" to EngineSlot(3, "10–12", 45),
        "endurance" to EngineSlot(2, "20–25", 30),
        "general" to EngineSlot(2, "12–15", 45)
    )

    /** The prompt's split table (Step 1), verbatim. Exposed for the tests. */
    fun engineSplitFor(dayCount: Int): List<String> = when (dayCount) {
        in 1..2 -> List(dayCount) { "full_body" }
        3 -> List(3) { "full_body" }
        4 -> listOf("upper", "lower", "upper", "lower")
        5 -> listOf("push", "pull", "legs", "upper", "lower")
        6 -> listOf("push", "pull", "legs", "push", "pull", "legs")
        7 -> listOf("push", "pull", "legs", "upper", "lower", "arms", "core")
        else -> List(dayCount) { "full_body" }
    }

    private val ENGINE_PUSH_PARTS = listOf("Chest", "Front Deltoids", "Lateral Deltoids", "Triceps")
    private val ENGINE_PULL_PARTS = listOf("Lats", "Upper Traps", "Rear Deltoids", "Biceps")

    private val ENGINE_LEG_PARTS = listOf("Quadriceps", "Hamstrings", "Glutes", "Calves")

    /**
     * The trunk movements a trunk-less session closes on, rotated across the week.
     *
     * Owner report 2026-10-13: "when looking at the week the system created, i see
     * no core exercises on any day at all. thats not balanced or is it?" It was the
     * 3-day (full_body) and 6-day (push/pull/legs) weeks: neither day type declares
     * a trunk muscle, so a full week of generated training touched the abs on no
     * day. These are the candidates that close such a session (see
     * [generateEngineProgram]), and they are the same three the dedicated core day
     * trains, so the app has one vocabulary for the trunk.
     */
    private val ENGINE_TRUNK_PARTS = listOf("Abs", "Obliques", "Spinal Erectors")

    /**
     * A LOWER day is the lower-body half of an upper/lower week, and it is NOT
     * the same session as the dedicated LEGS day.
     *
     * Owner report 2026-10-08: "A day training (Lower) body, is a day full of legs
     * which im fairly sure is wrong." It was exactly right: `"lower"` returned
     * ENGINE_LEG_PARTS verbatim, so the 4-day split (upper/lower/upper/lower) and
     * the 5- and 7-day splits that include both trained the posture twice with no
     * way to tell the two days apart - the user got the same four leg muscles on
     * both lower days of the week.
     *
     * What actually separates them in a real program is the POSTERIOR CHAIN and the
     * CORE: the lower day carries the hip hinge and the trunk (spinal erectors,
     * abs) that an upper day never touches, while the legs day stays the
     * quad-dominant session. The order matters as well as the membership - the
     * generator round-robins this list across a day's slots, so the hinge and the
     * trunk lead and the quads follow, instead of a second squat day.
     */
    private val ENGINE_LOWER_PARTS =
        listOf("Hamstrings", "Glutes", "Spinal Erectors", "Abs", "Quadriceps", "Calves")

    /** The prompt's body-part mapping per day type (Step 3). */
    private fun enginePartsFor(type: String): List<String> = when (type) {
        "push" -> ENGINE_PUSH_PARTS
        "pull" -> ENGINE_PULL_PARTS
        "legs" -> ENGINE_LEG_PARTS
        "upper" -> ENGINE_PUSH_PARTS + ENGINE_PULL_PARTS
        "lower" -> ENGINE_LOWER_PARTS
        "full_body" -> listOf(
            "Chest", "Lats", "Quadriceps", "Hamstrings", "Glutes",
            "Front Deltoids", "Biceps", "Triceps"
        )
        "arms" -> listOf("Biceps", "Triceps")
        "core" -> listOf("Abs", "Obliques", "Spinal Erectors")
        else -> emptyList()
    }

    /** True for a trunk muscle, through the app's own broad-topic mapping. */
    private fun isTrunkPart(part: String): Boolean = getTopicForMuscle(part) == "Core"

    /**
     * True for a MAJOR part - chest, back or legs - again by the app's own topic
     * mapping.
     *
     * It is the same three the balance card gives a FLOOR to (chest 1, back 1,
     * legs 3 per week); shoulders, arms and core have a floor of 0. That is what
     * makes "may this slot be spent on something else" a question with an answer
     * rather than a guess: a closing slot that would otherwise train a deltoid, an
     * arm or a calf is free, one that would train a chest or a leg is not.
     */
    private fun isMajorPart(part: String): Boolean =
        getTopicForMuscle(part) in setOf("Chest", "Back", "Legs")

    private fun engineTypeLabel(type: String): String =
        if (type == "full_body") "Full Body" else type.replaceFirstChar { it.uppercase() }

    /** The prompt's exclusion filter (Step 2): partial, case-insensitive, rename-aware. */
    private fun engineIsExcluded(name: String, excluded: List<String>): Boolean {
        if (excluded.isEmpty()) return false
        val canonical = (ExerciseLocalization.canonicalFor(name) ?: name).lowercase()
        val raw = name.lowercase()
        return excluded.any { entry ->
            val needle = ExerciseLocalization.canonicalFor(entry.trim())?.lowercase()
                ?: entry.trim().lowercase()
            needle.isNotEmpty() && (canonical.contains(needle) || raw.contains(needle))
        }
    }

    /**
     * The session windows in MINUTES — the same mapping the duration labels
     * show on the welcome screen ("30-45 min" / "45-60 min" / "60-75 min",
     * see durationLabel). Exposed for the tests.
     */
    fun engineWindowMinutes(duration: String): IntRange = when (duration) {
        DURATION_QUICK -> 30..45
        DURATION_FULL -> 60..75
        else -> 45..60
    }

    /**
     * The working-sets guard per window. The prompt's flat 20 is kept for
     * the short and standard windows; the long window gets headroom (24/28)
     * because 20 hard sets of these templates cost ~40-45 minutes and
     * collapsed its own 60-75 minute window onto its 45-60 one — the
     * owner's duration requirement is the newer instruction and wins.
     */
    fun engineSetsGuard(duration: String): Int = when (duration) {
        DURATION_FULL -> 28
        DURATION_STANDARD -> 24
        else -> 20
    }

    /**
     * What one engine slot COSTS in seconds: sets x (work + rest) plus the
     * transition between exercises. Work time is reps-aware (~3 seconds per
     * rep, floored at 20s because a heavy low-rep set carries setup too).
     * Exposed for the tests so the fit assertions share ONE cost model
     * instead of reimplementing it.
     */
    fun engineSlotCostSeconds(slotSets: Int, reps: String, restSeconds: Int): Int {
        val repsList = reps.split("–").mapNotNull { it.toIntOrNull() }
        val midReps = if (repsList.isEmpty()) 10.0 else (repsList.first() + repsList.last()) / 2.0
        val work = (3.0 * midReps).toInt().coerceAtLeast(20)
        return slotSets * (work + restSeconds) + ENGINE_TRANSITION_SECONDS
    }

    private const val ENGINE_TRANSITION_SECONDS = 90

    /**
     * The day's slots, built by TIME (prompt Step 4 adjusted for the
     * owner's 2026-10-06 duration report). Slots are added — template
     * first, then the goal's isolation top-up — while the running cost is
     * under the window's midpoint; a slot that would overrun the window's
     * END is not added, and the window-scaled sets guard stops the fill
     * when the templates cannot spend more hard sets. A day never drops
     * below two slots. Private: the tests pin its OUTPUT through the
     * generated programs and share only the pure cost/window functions.
     */
    private fun engineSlotsFor(goalKey: String, duration: String): List<EngineSlot> {
        val template = ENGINE_TEMPLATES.getValue(goalKey)
        val topup = ENGINE_ISOLATION_TOPUP.getValue(goalKey)
        val window = engineWindowMinutes(duration)
        val targetSeconds = (window.first + window.last) * 30   // midpoint
        val endSeconds = window.last * 60
        val setsGuard = engineSetsGuard(duration)
        val out = mutableListOf<EngineSlot>()
        var cost = 0
        var next = 0
        var sets = 0
        while (true) {
            val slot = if (next < template.size) template[next] else topup
            next++
            val projected = cost + engineSlotCostSeconds(slot.sets, slot.reps, slot.rest)
            if (out.isNotEmpty() && (projected > endSeconds || cost >= targetSeconds)) break
            if (sets + slot.sets > setsGuard) break
            out.add(slot)
            cost = projected
            sets += slot.sets
        }
        while (out.size < 2 && out.size < template.size + 4) {
            val slot = template[minOf(out.size, template.lastIndex)]
            out.add(slot)
        }
        return out
    }

    /**
     * The welcome screen's exercise maker, per the owner's template-engine
     * prompt. See the section note above for what this path does differently
     * from [generateAssistedProgram]; both produce the same
     * [GeneratedProgram] shape the setup flow and goal regeneration consume.
     */
    fun generateEngineProgram(
        bodyParts: List<String> = emptyList(),
        selectedDays: List<String>,
        intensity: String = "",
        workoutGoal: String = "goal_bodybuilding",
        includedExercises: List<String> = emptyList(),
        excludedExercises: List<String> = emptyList(),
        equipment: List<String> = emptyList(),
        duration: String = DURATION_STANDARD,
        splitStyle: SplitStyle = SplitStyle.DEFAULT
    ): GeneratedProgram {
        val canonical = canonicalGoal(workoutGoal)
        val goalKey = engineGoalKey(canonical)
        val weekDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        val orderedDays = weekDays.filter { it in selectedDays }
        if (orderedDays.isEmpty()) {
            return GeneratedProgram(emptyList(), emptyList(), emptyList(), emptyList())
        }
        val types = engineSplitFor(orderedDays.size)
        // Judged over the WHOLE week, before any day is built: a week that already
        // carries a trunk day keeps it as its only source of trunk work (see the
        // closing-slot rules below).
        val weekHasATrunkDay = types.any { type -> enginePartsFor(type).any { isTrunkPart(it) } }
        // Slots are built BY TIME from the goal template and the picked
        // session window — see engineSlotsFor. The prompt's flat 6-exercise
        // cap is superseded by the owner's duration requirement: these
        // templates cost 5-13 minutes per slot, so 6 exercises cannot fill
        // a 45-60 minute window, which is exactly the defect being fixed.
        val slots = engineSlotsFor(goalKey, duration)

        val includedKeys = includedExercises
            .mapNotNull { raw -> ExerciseLocalization.canonicalFor(raw) ?: raw }
            .map { it.lowercase() }
            .toSet()

        val routineDays = mutableListOf<RoutineDayEntity>()
        val routineExercises = mutableListOf<RoutineExerciseEntity>()
        val unsatisfied = mutableListOf<String>()
        val usedThisWeek = mutableSetOf<String>()
        var prevDayParts: Set<String> = emptySet()
        // Week-scoped, not day-scoped: it is what makes the closing slot rotate
        // across the week instead of restarting on the same part every session.
        var tailCursor = 0

        orderedDays.forEachIndexed { index, dayName ->
            val type = types.getOrElse(index) { "full_body" }
            val parts = enginePartsFor(type)
            if (parts.isEmpty()) return@forEachIndexed
            routineDays.add(
                RoutineDayEntity(
                    dayOfWeek = dayName,
                    title = "$dayName — ${engineTypeLabel(type)}",
                    description = "push_desc",
                    isRestDay = false
                )
            )
            // WHAT THE DAY'S OWN ROTATION CANNOT REACH, and what it never trains
            // at all.
            //
            // A day type declares more parts than a session has slots - a
            // full_body day carries eight and the quick window spends five - so
            // `parts[slotIndex % parts.size]` never leaves the first few and the
            // tail (biceps, triceps, and on a trunk-less week the abs) trained on
            // NO day of the week. Owner report 2026-10-13: "when looking at the
            // week the system created, i see no core exercises on any day at all.
            // thats not balanced or is it?"
            //
            // Both rules below act on the CLOSING slot of the day, which was
            // already an isolation - so no compound is displaced and the trunk is
            // never promoted into the lead slots:
            //
            //  1. A TRUNK-LESS WEEK closes each of its sessions on one trunk
            //     movement, rotating Abs -> Obliques -> Spinal Erectors. The whole
            //     week is judged at once here: a week that already carries a trunk
            //     day (the 5- and 7-day splits have a lower/core day; the 4- and
            //     7-day ones a lower day) is left EXACTLY as it was - adding a
            //     second source of trunk work to it would break the balance card's
            //     ceiling, which counts 10 core rows a week as the evidence-backed
            //     maximum.
            //  2. Otherwise the closing slot rotates through the parts this day
            //     cannot reach, so the tail is trained across the week.
            //
            // A slot that trains a MAJOR is left alone by both, and that is the
            // balance rule rather than caution: a short day is five slots, the
            // floors need chest, back and three leg movements, and five cannot hold
            // those AND the trunk - so the trunk waits for a day with room instead
            // of dropping the week under the legs floor, which is the report the
            // balance card exists for.
            val rotationReachesTrunk = parts.take(slots.size).any { isTrunkPart(it) }
            val claimTrunkSlot = !weekHasATrunkDay && !rotationReachesTrunk
            val unreached = parts.drop(slots.size)
            // The LAST slot of the day that has no major to train - the one slot
            // that can be spent without touching the chest/back/legs floors. A day
            // with none (every slot trains a major, which is what a five-slot
            // full_body session is) is left alone by both rules below.
            val spareSlot = slots.indices.lastOrNull { !isMajorPart(parts[it % parts.size]) }
            val trainedToday = mutableSetOf<String>()
            slots.forEachIndexed { slotIndex, slot ->
                // Round-robin the day's mapped parts. When the day type repeats
                // (the prompt's own 3-day full_body week), prefer a part not
                // trained on the previous workout day — best effort, because a
                // full-body day trains the same majors by construction.
                val roundRobin = parts[slotIndex % parts.size]
                val base = when {
                    slotIndex != spareSlot -> roundRobin
                    claimTrunkSlot -> ENGINE_TRUNK_PARTS[tailCursor++ % ENGINE_TRUNK_PARTS.size]
                    unreached.isNotEmpty() -> unreached[tailCursor++ % unreached.size]
                    else -> roundRobin
                }
                val part = if (base in prevDayParts) {
                    parts.firstOrNull { it !in prevDayParts && it !in trainedToday } ?: base
                } else base

                var pool = getExercisesForPart(part)
                grantedTagsFor(equipment)?.let { allowed -> pool = widenWithSubstitutes(pool, allowed) }
                pool = pool.filterNot { engineIsExcluded(it, excludedExercises) }
                if (classifyIntensity(intensity) == "Easy") {
                    val eligible = pool.filter { isBeginnerBuildEligible(it) }
                    if (eligible.isNotEmpty()) pool = eligible
                }
                // Rank: pinned (included) exercises first, then the slot's role
                // — compound leads slots 1-2, isolation closes the last two —
                // with popularity order as the stable tiebreaker underneath.
                val ranked = pool.sortedWith(
                    compareBy(
                        // 1. The user's own picks lead.
                        { name: String -> if (name.lowercase() in includedKeys) 0 else 1 },
                        // 2. The slot's role: compound leads slots 1-2, isolation
                        // closes the last two.
                        { name: String ->
                            val compound = ExerciseCatalog.entryFor(name)?.level == "compound"
                            when {
                                slotIndex < 2 -> if (compound) 0 else 1
                                slots.size > 2 && slotIndex >= slots.size - 2 -> if (compound) 1 else 0
                                else -> 0
                            }
                        },
                        // 3. WHICH GEAR THE CANDIDATE USES, so a kit that has both a
                        // bell and a floor gets the bell for this job (see
                        // [equipmentEffectivenessRank]).
                        { name: String -> equipmentEffectivenessRank(name) }
                        // 4. Popularity order is the input order, which the stable
                        // sort keeps as the last word.
                    )
                )
                val pick = ranked.firstOrNull { it !in usedThisWeek } ?: ranked.firstOrNull()
                if (pick == null) {
                    unsatisfied.add("$part ($dayName): no exercise left in the gated pool")
                    return@forEachIndexed
                }
                usedThisWeek.add(pick)
                trainedToday.add(part)
                routineExercises.add(
                    RoutineExerciseEntity(
                        dayOfWeek = dayName,
                        name = pick,
                        muscleGroup = part,
                        targetSets = slot.sets,
                        targetReps = slot.reps,
                        targetIntensity = getGoalIntensity(canonical, intensity, pick),
                        targetWeightKg = 0.0,
                        restSeconds = slot.rest,
                        transitionRestSeconds = 90,
                        videoUrl = "",
                        orderIndex = slotIndex + 1,
                        tempo = "",
                        locked = false
                    )
                )
            }
            prevDayParts = trainedToday
        }

        // Days the user did not pick stay rest days (prompt: "skip it").
        weekDays.filter { it !in orderedDays }.forEach { day ->
            routineDays.add(RoutineDayEntity(day, "$day — Rest", "rest_desc", true))
        }

        // The shipped validator still runs, report-only. The prompt's own
        // adjacency rule ("never the same primary muscle two days running")
        // cannot hold on its full_body weeks, and validatePlan's broad-day
        // exemption is what lets Full Body / Push / Pull / Legs stand.
        val plannedTopics = orderedDays.mapIndexed { index, day ->
            day to enginePartsFor(types.getOrElse(index) { "full_body" })
                .mapNotNull { getTopicForMuscle(it).takeIf { topic -> topic != "Other" } }
                .distinct()
        }.toMap()
        validatePlan(plannedTopics, orderedDays.size).forEach { failure ->
            unsatisfied.add(failure)
        }

        val insights = listOf(
            ScienceInsight("insight_goal_title", "insight_${canonical}_desc", "ACSM Guidelines"),
            ScienceInsight("insight_rest_title", "insight_rest_${canonical}_desc", "Henselmans et al. (2014)")
        )

        return GeneratedProgram(routineDays, routineExercises, unsatisfied.distinct(), insights)
    }

    /**
     * Every exercise must span at least [MIN_REP_RANGE_SPAN] reps so the reps
     * control has a usable range. 4 keeps the real schemes intact — a strength
     * block stays "1–5" instead of being widened into something meaningless.
     * Accepts formats like "8–12", "8-12", "10 each leg", "30 sec" and single values.
     * Time-based entries ("30 sec") are returned unchanged.
     */
    const val MIN_REP_RANGE_SPAN = 4

    fun parseRepRange(targetReps: String): Pair<Int, Int>? {
        if (targetReps.contains("sec", ignoreCase = true)) return null
        val matches = Regex("\\d+").findAll(targetReps).toList()
        return when {
            matches.size >= 2 -> {
                val v1 = matches.first().value.toInt()
                val v2 = matches.last().value.toInt()
                minOf(v1, v2) to maxOf(v1, v2)
            }
            matches.size == 1 -> {
                val v = matches.first().value.toInt()
                v to v
            }
            else -> null
        }
    }

    /** Widens a reps string so that max - min >= [MIN_REP_RANGE_SPAN], preserving the center of the range. */
    fun enforceRepRange(targetReps: String): String {
        val range = parseRepRange(targetReps) ?: return targetReps
        val (minVal, maxVal) = range
        if (maxVal - minVal >= MIN_REP_RANGE_SPAN) return targetReps
        val mid = (minVal + maxVal) / 2
        val newMin = (mid - MIN_REP_RANGE_SPAN / 2).coerceAtLeast(1)
        val newMax = newMin + MIN_REP_RANGE_SPAN
        val suffix = targetReps.substringAfterLast(Regex("\\d+").findAll(targetReps).last().value, "")
        return "$newMin–$newMax$suffix"
    }

    /** The auto-selected rep count for an exercise: the middle of its target range. */
    fun getMiddleRep(targetReps: String): Int {
        val (minVal, maxVal) = parseRepRange(targetReps) ?: return 10
        return (minVal + maxVal) / 2
    }

    /** Builds a reps string spanning [MIN_REP_RANGE_SPAN] centered on [reps] (clamped at 1). */
    fun repRangeAround(reps: Int): String {
        val minVal = (reps - MIN_REP_RANGE_SPAN / 2).coerceAtLeast(1)
        return "$minVal–${minVal + MIN_REP_RANGE_SPAN}"
    }

    /**
     * True when [name] is a core exercise, by the catalog's own primary muscle.
     *
     * The core is a prerequisite rather than a target: it transfers to the rest
     * of the session by bracing the spine, not by being fatigued. Training it
     * first would spend the freshest part of the session on the muscle with the
     * least to gain from freshness, so the app trains core LAST, when the spine
     * is no longer loaded.
     *
     * Read through [getMuscleGroupForExercise] rather than from the name, for
     * the same reason the rest of the app does: a name is not data. "Plank" and
     * "Dead Bug" say nothing about the primary mover; the catalog does.
     */
    private fun isCoreExercise(name: String): Boolean =
        getTopicForMuscle(getMuscleGroupForExercise(name)) == "Core"

    private fun getExercisePriority(name: String): Int {
        val n = name.lowercase()
        // The apparatus is read from the catalog, not from the name. After the
        // generalization "Barbell Back Squat" IS the barbell squat and its name no
        // longer says so, while the catalog's equipment field still does.
        val gear = equipmentClassFor(name)
        return when {
            (gear == EQUIPMENT_BARBELL && n.contains("squat")) ||
            (n.contains("deadlift") && !n.contains("romanian") && !n.contains("stiff")) ||
            n.contains("clean") || n.contains("snatch") || n.contains("thruster") -> 1
            (gear == EQUIPMENT_BARBELL && n.contains("bench press")) ||
            n.contains("military press") || n.contains("overhead press") -> 2
            gear == EQUIPMENT_BARBELL -> 3
            n.contains("row") || n.contains("pull-up") || n.contains("chin-up") -> 4
            gear == EQUIPMENT_DUMBBELL -> 5
            gear == EQUIPMENT_CABLE || gear == EQUIPMENT_MACHINE -> 6
            else -> 7
        }
    }

    /**
     * The apparatus family of [name]: the catalog's own `equipment` field when
     * the exercise is in the pool, and a name-based guess for a user's own
     * custom exercise, which has no catalog row to ask.
     */
    private fun equipmentClassFor(name: String): String {
        val fromCatalog = ExerciseCatalog.equipmentFor(name)
        if (fromCatalog.isNotBlank()) return fromCatalog
        val n = name.lowercase()
        return when {
            n.contains("barbell") -> EQUIPMENT_BARBELL
            n.contains("dumbbell") -> EQUIPMENT_DUMBBELL
            n.contains("kettlebell") -> EQUIPMENT_KETTLEBELL
            n.contains("cable") -> EQUIPMENT_CABLE
            else -> EQUIPMENT_BODYWEIGHT
        }
    }

    private fun isHeavyCompound(name: String): Boolean {
        val n = name.lowercase()
        // Exercises that safe and productive for low-rep strength work
        val gear = equipmentClassFor(name)
        return ((gear == EQUIPMENT_BARBELL) || n.contains("barbell") || n.contains("military press")) &&
               (n.contains("squat") || (n.contains("deadlift") && !n.contains("romanian")) ||
                n.contains("bench press") || n.contains("overhead press") ||
                n.contains("military press") || n.contains("press behind the neck") ||
                n.contains("clean") || n.contains("snatch"))
    }

    /**
     * Rest between sets, decided by the training goal alone (plus whether the
     * movement is a heavy compound). ACSM/NSCA-style: long rest for maximal
     * strength and power, short rest for endurance and conditioning work.
     */
    fun getGoalRestSeconds(rawGoal: String, exerciseName: String = ""): Int {
        val goal = canonicalGoal(rawGoal)
        val isCompound = if (exerciseName.isNotEmpty()) isHeavyCompound(exerciseName) else true
        return when(goal) {
            // Maximal strength needs the ATP-CP window: long rest on the big
            // rocks, a working pause for accessories.
            "goal_strength" -> if (isCompound) 240 else 90
            // Metabolic-stress work rests just long enough to keep the load.
            "goal_bodybuilding" -> 90
            // Skill work stays fresh; short rests keep the nervous system crisp.
            "goal_calisthenics" -> 45
            // Conditioning: short rests, the clock is part of the stimulus.
            "goal_endurance" -> 45
            // Health/fitness: moderate rests, no scheme extremes.
            "goal_general" -> 60
            else -> 60
        }
    }

    /**
     * The effort cue shown for an exercise. Difficulty is the only input: the
     * goal decides which words fit, but Easy / Medium / Hard decide the level.
     */
    fun getGoalIntensity(rawGoal: String, intensity: String, exerciseName: String): String {
        val goal = canonicalGoal(rawGoal)
        val isCompound = isHeavyCompound(exerciseName)
        return when(intensity) {
            "Hard" -> {
                if (goal == "goal_strength") {
                    if (isCompound) "Very Heavy" else "Heavy"
                } else if (goal == "goal_bodybuilding") {
                    "To Failure"
                } else {
                    "High Intensity"
                }
            }
            "Easy" -> "Light"
            else -> "Moderate"
        }
    }

    /**
     * The rep range for an exercise, decided by the training goal alone.
     *
     * Difficulty does not move reps: a "Hard" strength squat and an "Easy"
     * strength squat use the same 1–5 scheme, they differ in sets and load.
     * That is what keeps the three axes from fighting each other.
     */
    fun getGoalReps(exerciseName: String, goal: String): String {
        if (isExerciseTimeBased(exerciseName)) return "30 sec"

        val isCompound = isHeavyCompound(exerciseName)
        val isIsolation = exerciseName.lowercase().let {
            it.contains("curl") || it.contains("extension") || it.contains("lateral") ||
            it.contains("raise") || it.contains("pushdown") || it.contains("kickback") ||
            it.contains("fly")
        }

        // Even in a strength block, accessories stay in a hypertrophy range —
        // low reps on single-joint work is not productive or safe.
        val range = when(canonicalGoal(goal)) {
            "goal_strength" -> if (isCompound) 1..5 else 8..12
            "goal_bodybuilding" -> if (isIsolation) 10..15 else 8..12
            "goal_calisthenics" -> 12..20
            // Stamina work lives in the 15-20 window; accessories 12-15.
            "goal_endurance" -> if (isCompound) 15..20 else 12..15
            // Health guidelines (ACSM): 8-15 controlled reps per set.
            "goal_general" -> 10..15
            else -> 8..12
        }
        return enforceRepRange("${range.first}–${range.last}")
    }

    /**
     * Re-parameterizes a replaced exercise row from the user's settings — the
     * same defaults [generateAssistedProgram] would have given the incoming
     * exercise — instead of letting the new row inherit the old one's sets,
     * reps and rest. Swapping Squats for Planks used to keep "8–12" reps and
     * a barbell's rest timer; now the swap lands the way the plan would have
     * built the exercise in the first place: the goal owns reps and rest, the
     * difficulty owns sets and the effort label, and the load resets (the
     * workout screen still suggests the user's last-used weight for that
     * exercise when it has one).
     *
     * Pure so the setup builder can pass the goal/difficulty it is still
     * editing, while the routine screen passes the persisted ones.
     */
    fun parameterizeReplacement(
        original: RoutineExerciseEntity,
        newName: String,
        newMuscleGroup: String,
        workoutGoal: String,
        intensity: String,
        videoUrl: String = ""
    ): RoutineExerciseEntity {
        // 3 working sets universally — Guided mode has no set-count knob.
        return original.copy(
            id = 0L,
            name = newName,
            muscleGroup = newMuscleGroup,
            targetSets = GUIDED_SETS_PER_EXERCISE,
            targetReps = getGoalReps(newName, workoutGoal),
            targetIntensity = getGoalIntensity(workoutGoal, intensity, newName),
            targetWeightKg = 0.0,
            restSeconds = getGoalRestSeconds(workoutGoal, newName),
            transitionRestSeconds = 90,
            videoUrl = videoUrl,
            tempo = ""
        )
    }

    fun getAllExerciseNames(): List<String> = ExerciseCatalog.NAMES

    /**
     * Whether [name] may appear in a beginner premade strength build: the v5
     * progression data flags every eligible exercise (validated rows, simple
     * progression, no coach review). Custom exercises and names outside the
     * pool keep the old behavior (eligible) so a future re-import can never
     * silently empty a user's day.
     */
    fun isBeginnerBuildEligible(name: String): Boolean {
        val entry = ProgressionEngine.entryFor(name) ?: return true
        return entry.beginnerBuildEligible && !entry.requiresCoachReview
    }

    /** Every pool exercise allowed in a beginner premade build, sorted. */
    fun getBeginnerBuildEligibleExercises(): List<String> =
        ExerciseCatalog.NAMES.filter { isBeginnerBuildEligible(it) }

    fun getAllMuscleGroups(): List<String> = (getBroadMuscleGroups() + getPreciseMuscleGroups())
        .distinct()
        .sorted()

    /**
     * The precise muscles an exercise's own data names: "Lats", "Rear Deltoids", "Glutes"…
     *
     * These are the groups the catalog stores on every pool exercise, and the ones the
     * replace dialog's filter is now allowed to narrow by (owner, 2026-10-09: *"allow the
     * user to filter which muscle too"*). [getBroadMuscleGroups] is the OTHER vocabulary -
     * six body parts — and the two are different tools rather than rivals:
     *
     *  - a body part is a clean PARTITION of the pool (an exercise belongs to exactly one),
     *    which is what makes "another back movement" a predictable answer;
     *  - a precise muscle is a POOL (the triceps list carries the dumbbell floor press), which
     *    is what makes "something else for my triceps" a useful answer.
     *
     * Both were already answerable — [exercisesForMuscleGroup] has branched on which kind it
     * was given for as long as this filter has existed — so this function only stops the list
     * from being written out a second time where the UI needs it. Keeping one declaration is
     * the point: two hand-kept lists of muscle names is how a filter ends up offering a muscle
     * the lookup cannot answer.
     */
    fun getPreciseMuscleGroups(): List<String> = listOf(
        "Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back",
        "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs",
        "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves", "Hip Adductors",
        "Hip Abductors", "Pectorals"
    )

    /**
     * The broad body parts a user picks a substitute exercise by.
     *
     * [getAllMuscleGroups] also returns 19 precise muscles (Lats, Pectorals,
     * Rear Deltoids…). Those are the right granularity for labelling what an
     * exercise trains, but the wrong one for filtering: someone replacing a row
     * wants to say "another back movement", not "another Lats movement", and a
     * 25-entry dropdown makes that choice harder, not more precise.
     */
    fun getBroadMuscleGroups(): List<String> =
        listOf("Arms", "Back", "Chest", "Core", "Legs", "Shoulders")

    /**
     * The precise muscles that belong to [muscleGroup], in the catalogue's own order.
     *
     * Owner, 2026-10-11: *"when selecting muscle group back, then make sure that the precise
     * muscles are shown only of that 'back' cathegory / group"*. The filter used to draw all
     * nineteen whatever was selected, so picking Back and reading the list below it said nothing
     * about which muscles Back had actually answered with — the two vocabularies were two lists in
     * one block rather than a choice and its refinements.
     *
     * [muscleGroup] may be a BROAD part ("Back"), a PRECISE muscle ("Lats") or null:
     *
     *  - null or blank is "any muscle", which is every precise muscle — the block's opening state;
     *  - a precise muscle answers with its own GROUP's muscles, because that is the list the user is
     *    already looking at when they tap it (tapping "Lats" must not leave the user in a list of
     *    one, and must not silently re-open the other five groups);
     *  - anything unrecognised falls back to every muscle rather than to an empty row: an empty
     *    filter is indistinguishable from a broken one.
     *
     * THE SIX BROAD PARTS PARTITION THE NINETEEN EXACTLY. That is what makes narrowing safe, and
     * `MuscleFilterGroupingTest` pins it: no precise muscle falls outside the six, so no muscle
     * becomes unreachable by picking a group, and no two groups claim the same one.
     */
    fun getPreciseMuscleGroupsFor(muscleGroup: String?): List<String> {
        if (muscleGroup.isNullOrBlank()) return getPreciseMuscleGroups()
        val topic = getTopicForMuscle(muscleGroup)
        if (topic == "Other") return getPreciseMuscleGroups()
        return getPreciseMuscleGroups().filter { getTopicForMuscle(it) == topic }
    }

    /** Lowercase keys of [getBroadMuscleGroups], for the lookup in [exercisesForMuscleGroup]. */
    private val broadMuscleGroupKeys =
        listOf("arms", "back", "chest", "core", "legs", "shoulders")

    fun getTopicForMuscle(muscle: String): String {
        val mg = muscle.lowercase().trim()
        return when {
            mg.contains("chest") || mg.contains("pectoral") -> "Chest"
            // "lat" must not swallow "Lateral Deltoids"
            mg.contains("back") || (mg.contains("lat") && !mg.contains("lateral")) || mg.contains("trap") || mg.contains("erector") -> "Back"
            mg.contains("leg") || mg.contains("quad") || mg.contains("hamstring") || mg.contains("glute") || mg.contains("calve") || mg.contains("adductor") || mg.contains("abductor") -> "Legs"
            mg.contains("shoulder") || mg.contains("delt") -> "Shoulders"
            mg.contains("bicep") || mg.contains("tricep") || mg.contains("arm") || mg.contains("forearm") -> "Arms"
            mg.contains("abs") || mg.contains("core") || mg.contains("oblique") || mg.contains("abdominal") -> "Core"
            else -> "Other"
        }
    }

    /**
     * Returns the exercises available for [muscleGroup] that use [equipment].
     * Pass [EQUIPMENT_ALL] (default) to include every equipment category.
     */
    fun getExercisesForEquipment(equipment: String, muscleGroup: String): List<String> {
        // The muscle pool decides WHICH muscles an exercise trains; the equipment
        // table decides WHICH kit it needs. Filtering the pool through the table
        // keeps the two independent, so an exercise can no longer leak into an
        // equipment category just because a muscle list mentioned it.
        val allForGroup = exercisesForMuscleGroup(muscleGroup)
        val filtered = if (equipment == EQUIPMENT_ALL) allForGroup
                       else allForGroup.filter { getEquipmentForExercise(it) == equipment }
        // Popularity order: the staples a program would actually use come
        // before the obscure variations of the same kit + muscle.
        return ExercisePopularity.sortedByPopularity(filtered.distinct())
    }

    /**
     * Every pool exercise that trains [muscleGroup].
     *
     * A precise group is answered by its own pool list, which lists *every*
     * muscle an exercise works. A broad body part cannot be answered that way:
     * the pools are deliberately liberal (the triceps list carries the dumbbell
     * floor press, the biceps list carries chin-ups), so unioning them would
     * offer a chest press under "Arms" and put chin-ups under both "Arms" and
     * "Back". A body part is instead answered by the exercise's own
     * authoritative body part — the one its badge already shows — which makes
     * the six parts a clean partition of the pool.
     */
    private fun exercisesForMuscleGroup(muscleGroup: String): List<String> {
        if (muscleGroup.trim().lowercase() !in broadMuscleGroupKeys) {
            return getExercisesForPart(muscleGroup)
        }
        val topic = getTopicForMuscle(muscleGroup)
        return getAllExerciseNames().filter {
            getTopicForMuscle(getMuscleGroupForExercise(it)) == topic
        }
    }

    /**
     * Every exercise available for an equipment category across all muscle
     * groups (used when the muscle group filter is set to "Any").
     */
    fun getAllExercisesForEquipment(equipment: String): List<String> {
        return ExercisePopularity.sortedByPopularity(
            getAllExerciseNames()
                .filter { equipment == EQUIPMENT_ALL || getEquipmentForExercise(it) == equipment }
        )
    }

    /**
     * Returns the equipment category an exercise belongs to (one of the
     * EQUIPMENT_* constants), or "" when it isn't part of the built-in pool
     * (e.g. a user-created custom exercise).
     */
    fun getEquipmentForExercise(name: String): String {
        val clean = name.trim().lowercase()
        if (clean.isEmpty()) return ""
        return exerciseEquipment[clean] ?: ""
    }

    /**
     * How much EFFECT a piece of gear buys, lower being better: the tie-breaker
     * that decides between two exercises that do the same job equally well.
     *
     * OWNER RULING, 2026-10-09: *"lots of exercises are more bodyweight than
     * dumbbell when selecting both. If i select that i have dumbbells i would
     * naturally want more dumbbell exercises than bodyweight if thats possible so
     * that it has more effect ... This should be applied for each equipment so that
     * i use all of them but maybe the most effective once come first always."*
     *
     * THE ORDER IS THE LOADING CEILING, which is what "effect" means for a
     * hypertrophy week: a barbell takes more plates than a dumbbell pair, a
     * machine or cable takes more than a band, and plain bodyweight is the one
     * implement nobody can add a plate to - so a kit holding both a bell and a
     * floor gets the bell first and the push-up only when nothing loaded covers
     * that job. That keeps EVERY selected category in play (nothing is filtered
     * out - a bodyweight-only kit scores every row the same and its week is
     * unchanged) while the most effective rows lead.
     *
     * It is deliberately a TIE-BREAKER, not the first key. Compound-before-
     * isolation still leads a session and stays the reason a slot exists; this
     * only decides between two equally good candidates, which is precisely the
     * dumbbell-vs-bodyweight choice the owner was looking at.
     */
    fun equipmentEffectivenessRank(name: String): Int = when (getEquipmentForExercise(name)) {
        EQUIPMENT_BARBELL -> 0
        EQUIPMENT_DUMBBELL -> 1
        EQUIPMENT_KETTLEBELL -> 2
        EQUIPMENT_MACHINE -> 3
        EQUIPMENT_CABLE -> 4
        EQUIPMENT_BANDS -> 5
        EQUIPMENT_BODYWEIGHT -> 6
        // "Other" is the pool's GRAB-BAG: it holds the stretches, the poses and
        // the ab-wheel/sled odds and ends, so it is not a loading tier at all and
        // it must never win a slot ahead of a real movement. Measured before this
        // line was last: a bodyweight-only kit came out 35 Other / 7 Bodyweight,
        // i.e. a week of stretching in place of push-ups.
        EQUIPMENT_OTHER -> 7
        // Outside the pool (custom rows): a known movement always outranks it.
        else -> 8
    }

    fun getAlternativeExercises(currentExerciseName: String, muscleGroup: String, equipment: String = EQUIPMENT_ALL): List<String> {
        return getExercisesForEquipment(equipment, muscleGroup)
            .filter { it.lowercase() != currentExerciseName.lowercase() }
    }

    /**
     * The precise (non-topic) muscle groups of the built-in pool, in lookup
     * order. Exercises legitimately appear under several groups (rows under
     * Lats / Upper Back / Rear Deltoids, presses under Chest / Triceps, …);
     * the first match wins for a generic lookup.
     */
    private val detailedMuscleGroups = listOf(
        "Front Deltoids", "Lateral Deltoids", "Rear Deltoids", "Lats", "Upper Back",
        "Upper Traps", "Spinal Erectors", "Biceps", "Triceps", "Forearms", "Abs",
        "Obliques", "Quadriceps", "Hamstrings", "Glutes", "Calves",
        "Hip Adductors", "Hip Abductors", "Chest", "Pectorals"
    )

    /**
     * Authoritative primary muscle for every exercise in the built-in pool.
     *
     * Generated from the exercise-database CSV (see tools/exercise-db/); the
     * precise muscle is the join key the badges, the analytics split and the
     * broad body-part filter all read. A test asserts every pool exercise has
     * exactly one entry and no entry names an exercise outside the pool.
     */
    private val exercisePrimaryMuscles: Map<String, String> =
        ExerciseCatalog.ENTRIES.associate { it.name.lowercase() to it.muscle }

    /** The precise primary muscle for [name], or "" when it isn't in the pool. */
    fun getPrimaryMuscleForExercise(name: String): String =
        exercisePrimaryMuscles[name.trim().lowercase()] ?: ""

    /**
     * The exercise names covered by the authoritative primary-muscle table,
     * sorted. Exposed so the pool integrity tests can prove the table has no
     * gaps (a pool exercise with no entry), no strays (an entry for a name that
     * isn't in the pool) and no duplicates.
     */
    fun getPrimaryMuscleTableNames(): List<String> = exercisePrimaryMuscles.keys.sorted()

    /**
     * Authoritative equipment for every exercise in the built-in pool, one of
     * [allEquipmentCategories]. Generated from the exercise-database CSV; the
     * equipment filter never guesses from the name, it reads this table.
     */
    private val exerciseEquipment: Map<String, String> =
        ExerciseCatalog.ENTRIES.associate { it.name.lowercase() to it.equipment }

    /** Every exercise covered by the authoritative equipment table, sorted. */
    fun getEquipmentTableNames(): List<String> = exerciseEquipment.keys.sorted()

    /**
     * The string key that tells the user a set of THIS exercise is done one side
     * at a time ("each arm" / "each leg" / "each side"), or null when a set
     * counts both sides together.
     *
     * Owner, 2026-10-13: *"When giving one arm exercises define that its 8 reps
     * on each arm (care that exercises like bicycle crunch also have 'on each
     * side' type of exercise so the same applies to those too)"*. The prescription
     * numbers are per-side by construction (a one-arm row's 8 reps means 8 per
     * arm), so this is a DISPLAY fact about the exercise, not a change to the
     * ladders - the card and the summary line say whose reps they are.
     *
     * Read from the NAME rather than a new catalog column because the naming is
     * the data here: "Single-Arm", "Single-Leg", "One Arm" and the bicycle /
     * side-plank family are exactly the movements the request names, and a
     * column would have to be hand-flagged to say the same thing.
     */
    fun perSideLabelKey(name: String): String? {
        val n = name.trim().lowercase()
        return when {
            // Alternating one-side work: the set is finished when BOTH sides
            // have had their share, so "each side" is the honest phrase.
            "bicycle" in n || "side plank" in n || "mountain climber" in n -> "per_side_side"
            "single-arm" in n || "single arm" in n || "one-arm" in n || "one arm" in n -> "per_side_arm"
            "single-leg" in n || "single leg" in n -> "per_side_leg"
            else -> null
        }
    }

    private fun isExerciseInGroup(name: String, group: String): Boolean =
        ExerciseCatalog.muscleFor(name).equals(group, ignoreCase = true)

    /**
     * Every pool exercise whose authoritative body part is [bodyPart].
     *
     * The rebuilt pool carries exactly one precise muscle per exercise, so the
     * broad body parts (Arms, Back, Chest, Core, Legs, Shoulders) are a clean
     * partition: every exercise appears under exactly one part — the one its
     * badge shows. A precise muscle matches itself; a broad topic matches the
     * topic of the exercise's precise muscle.
     */
    private fun getExercisesForPart(bodyPart: String): List<String> {
        val wanted = bodyPart.lowercase().trim()
        // "Upper Back" is a picker alias, not a pool of its own: the rebuilt
        // catalog stores rows under the precise groups (Lats, Upper Traps,
        // Spinal Erectors), and NO exercise carries "Upper Back" as its primary
        // muscle. Resolving the alias to the whole Back topic keeps a user who
        // picked Upper Back (and the 3-day default build that includes it)
        // satisfiable — the alias used to resolve to an empty pool, which made
        // the build report the part unsatisfied with no user-visible cause.
        val aliasPart = when (bodyPart.trim()) {
            "Upper Back" -> "Back"
            else -> bodyPart
        }
        val resolved = aliasPart.lowercase().trim()
        // Popularity order: common staples before obscure variations, so the
        // generator's "pick from the head of the pool" and every picker that
        // lists a part's movements surface the movements programs actually
        // use. Stable sorts downstream (on-part, compound-first) keep this
        // order as their tiebreaker.
        return ExercisePopularity.sortedByPopularity(ExerciseCatalog.NAMES.filter { name ->
            val muscle = ExerciseCatalog.muscleFor(name)
            muscle.equals(aliasPart, ignoreCase = true) ||
                (muscle.isNotEmpty() && getTopicForMuscle(muscle).lowercase() == resolved)
        })
    }

    /** Equipment-specific views of [getExercisesForPart], kept for call sites. */
    private fun getBodyweightExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter { getEquipmentForExercise(it) == EQUIPMENT_BODYWEIGHT }

    private fun getDumbbellExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter { getEquipmentForExercise(it) == EQUIPMENT_DUMBBELL }

    private fun getBarbellExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter { getEquipmentForExercise(it) == EQUIPMENT_BARBELL }

    /** The historical "Gym" umbrella: cables and machines. */
    private fun getGymExercises(bodyPart: String): List<String> =
        getExercisesForPart(bodyPart).filter {
            getEquipmentForExercise(it) == EQUIPMENT_CABLE || getEquipmentForExercise(it) == EQUIPMENT_MACHINE
        }

    // ------------------------------------------------------------------
    // Equipment substitution chains.
    //
    // The equipment filter must never hand back an exercise the user cannot
    // do, and a part must never go untrained just because its first-choice
    // kit is missing. A chain lists equivalent movements in loading order —
    // most-loaded first. When a picked exercise is not available, the
    // nearest available link substitutes it (Barbell Squat with a barbell
    // missing -> Goblet Squat -> Bodyweight Squat).
    // ------------------------------------------------------------------
    private val substitutionChains: List<List<String>> = listOf(
        // Squat pattern: barbell -> kettlebell/dumbbell -> bodyweight
        listOf("Barbell Back Squat", "Goblet Squat", "Dumbbell Squat", "Banded Squat", "Bodyweight Squat"),
        // Horizontal press: barbell -> dumbbell -> bodyweight. The incline
        // dumbbell press is in the default prefill, so it needs its own line
        // back to floor work: a dumbbells-only user was left with NO
        // substitution at all for it (the missing rung the equipment-gate
        // test flagged).
        listOf("Barbell Bench Press", "Dumbbell Bench Press", "Dumbbell Floor Press", "Push-Up"),
        listOf("Incline Barbell Bench Press", "Incline Dumbbell Press", "Dumbbell Floor Press", "Incline Push-Up"),
        listOf("Dumbbell Fly", "Dumbbell Floor Press", "Push-Up"),
        // Vertical pull: bodyweight -> band -> machine. The dumbbell pullover
        // is the floor-level rung: without it a dumbbells-only kit had NO
        // available link in the whole chain (every other rung needs bar,
        // band, bench or machine).
        listOf("Pull-Up", "Chin-Ups", "Assisted Pull Ups (Band)", "Assisted Pull Ups", "Lat Pulldown", "Dumbbell Pullover"),
        // Horizontal pull: bodyweight -> barbell -> cable -> dumbbell row
        listOf("Pull-Up", "Chin-Ups", "Inverted Row", "Rings Inverted Row", "TRX Row", "Wide Grip Seated Cable Row", "Bent-Over Dumbbell Row"),
        // Horizontal row variants: barbell row -> dumbbell/machine rows (the
        // default prefill and template builds lean on the barbell row, so a
        // no-barbell user must still receive a row).
        listOf("Bent-Over Barbell Row", "Chest-Supported Dumbbell Row", "Bent-Over Dumbbell Row", "Seated Cable Row", "Inverted Row"),
        // Hip hinge: barbell -> kettlebell -> band
        listOf("Barbell Deadlift", "Romanian Deadlift", "Dumbbell Romanian Deadlift", "Banded Romanian Deadlift"),
        // Overhead press: barbell -> dumbbell -> bodyweight
        listOf("Barbell Overhead Press", "Dumbbell Shoulder Press", "Arnold Press", "Pike Push Ups"),
        // Lunge pattern: dumbbell -> kettlebell -> bodyweight
        listOf("Dumbbell Lunge", "Goblet Squat", "Bodyweight Squat"),
        // Triceps: cable/machine -> bodyweight. Skull Crusher joins here so
        // the barbell row of the default prefill substitutes down for
        // kit-less users instead of vanishing.
        listOf("Cable Tricep Pushdown", "Single Arm Tricep Pushdown", "Dumbbell Tricep Extension", "Skull Crusher", "Dumbbell Floor Press", "Bench Dips", "Diamond Push Ups"),
        // Shrugs: barbell -> dumbbell -> machine. Both shrug rows of the
        // default prefill were dead ends for every kit without the row's own
        // equipment.
        listOf("Barbell Shrug", "Behind the Back Barbell Shrug", "Dumbbell Shrug", "Chest Supported Dumbbell Shrug", "Smith Machine Shrug"),
        // Hamstring curls: machine -> bodyweight (nordic needs nothing).
        listOf("Lying Leg Curl", "Seated Leg Curl", "Nordic Hamstring Curl", "Bodyweight Good Morning"),
        // Calf raises: machine -> dumbbell/bodyweight.
        listOf("Standing Calf Raise", "Machine Calf Raise", "Dumbbell Calf Raise", "Bodyweight Calf Raise"),
        // Curls: barbell -> EZ-bar -> dumbbell -> cable
        listOf("Barbell Curl", "EZ-Bar Curl", "Dumbbell Bicep Curl", "Cable Curl"),
        // Core raises: hanging needs a bar, lying needs nothing
        listOf("Hanging Leg Raise", "Lying Leg Raise", "Crunches")
    )

    /**
     * All chains containing [exerciseName]. Two movements are interchangeable
     * when they share a chain — the pattern, not the name, is what transfers.
     */
    private fun chainsFor(exerciseName: String): List<List<String>> =
        substitutionChains.filter { chain ->
            chain.any { it.equals(exerciseName, ignoreCase = true) }
        }

    /**
     * [candidate] can stand in for [original] when they share a chain and the
     * candidate is in it. Pure name-based: no catalog round-trips.
     */
    fun canSubstituteFor(candidate: String, original: String): Boolean {
        if (candidate.equals(original, ignoreCase = true)) return true
        val candidateChains = chainsFor(candidate)
        if (candidateChains.isEmpty()) return false
        return chainsFor(original).any { originalChain ->
            candidateChains.any { it === originalChain || it == originalChain }
        }
    }

    /**
     * The first exercise in [chain] the user can actually do with [equipment]
     * (empty equipment = no restriction), skipping [exclude]. Returns null
     * when no link is available — the caller keeps its own fallback.
     */
    private fun firstAvailableInChain(
        chain: List<String>,
        equipment: Set<String>,
        exclude: String = ""
    ): String? = chain.firstOrNull { link ->
        !link.equals(exclude, ignoreCase = true) &&
            ExerciseEquipmentTags.isAvailable(link, equipment)
    }

    /**
     * Exercise-level substitution: an available exercise replaces [original]
     * when it shares a substitution chain with it. Falls back to null so the
     * caller can apply its own part-level fallback.
     */
    fun substituteExercise(
        original: String,
        equipment: List<String>,
        exclude: List<String> = emptyList()
    ): String? {
        val picked = ExerciseEquipmentTags.allowedTagsFor(equipment)
        if (equipment.all { it.isBlank() }) return original
        // Fine-tag availability decides the gate; bodyweight needs no special
        // case because its empty requirement set passes any granted set. The
        // original staying in place when available keeps user intent intact
        // (a blocked Barbell Bench Press walks its OWN chain down to Dumbbell
        // Bench Press before anything else).
        if (ExerciseEquipmentTags.isAvailable(original, picked)) return original
        val excluded = exclude.map { it.lowercase() }.toSet()
        return chainsFor(original).asSequence()
            .mapNotNull { firstAvailableInChain(it, picked, original) }
            .firstOrNull { it.lowercase() !in excluded }
    }

    /**
     * Widens an equipment-filtered pool so a part with no direct match can
     * still train: exercises already allowed stay first, then chain links
     * of the blocked ones that are allowed, then the part's own bodyweight
     * movements (every pattern has a bodyweight expression). Returns the
     * original list untouched when the direct filter is non-empty.
     */
    /**
     * The granted fine-grained tag set for a persisted equipment selection,
     * or null when the selection means "no restriction" (the empty legacy
     * list or EQUIPMENT_ALL). An empty NON-null set (the pure-bodyweight
     * preset) is a real strict filter: only no-requirement movements pass.
     */
    private fun grantedTagsFor(equipment: List<String>): Set<String>? {
        val items = equipment.map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty() || items.any { it.equals(EQUIPMENT_ALL, ignoreCase = true) }) return null
        return ExerciseEquipmentTags.allowedTagsFor(items)
    }

    /** Public gate for callers outside the generator (ViewModel premade mapping). */
    fun grantedTagsForPublic(equipment: List<String>): Set<String>? = grantedTagsFor(equipment)

    /**
     * The equipment gate a PICKER should apply, given what the host knows.
     *
     * [fineTags] is the user's granted fine tags when the host has them;
     * [coarseCategories] is the chip list. The fine tags win outright, and
     * deliberately so: the coarse vocabulary is "Barbell", "Dumbbell", … and
     * has no category for a chair, a bench or a pull-up bar, so rebuilding the
     * gate from it silently drops grants the user actually ticked. The coarse
     * path is kept only for a host with nothing better to offer.
     *
     * Null = unrestricted (never restricted the kit). An EMPTY set is not the
     * same thing: it is the strict bodyweight-only scenario, and passing it
     * through keeps that strictness. This is why the two are distinguished all
     * the way down instead of being collapsed into a boolean.
     */
    fun pickerGrantedTags(
        fineTags: Set<String>?,
        coarseCategories: List<String>
    ): Set<String>? = fineTags
        ?: grantedTagsFor(coarseCategories)

    private fun widenWithSubstitutes(
        partPool: List<String>,
        allowedEquipment: Set<String>
    ): List<String> {
        // NOTE: an empty granted set is the strict bodyweight-only scenario,
        // not "no restriction" — callers pass null for unrestricted.
        val direct = partPool.filter { ExerciseEquipmentTags.isAvailable(it, allowedEquipment) }
        if (direct.isNotEmpty()) return direct

        val out = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        fun add(name: String) {
            val key = name.lowercase()
            if (key !in seen) {
                seen.add(key)
                out.add(name)
            }
        }
        // Chain links of the part's blocked exercises, ordered by loading
        // fidelity: the nearest link first (Barbell Squat blocked -> Goblet
        // Squat before Bodyweight Squat).
        partPool.forEach { blocked ->
            chainsFor(blocked).forEach { chain ->
                val idx = chain.indexOfFirst { it.equals(blocked, ignoreCase = true) }
                // Walk outward from the blocked link in both directions.
                val order = mutableListOf<String>()
                for (d in 1 until chain.size) {
                    if (idx - d >= 0) order.add(chain[idx - d])
                    if (idx + d < chain.size) order.add(chain[idx + d])
                }
                order.forEach { link ->
                    if (ExerciseEquipmentTags.isAvailable(link, allowedEquipment)) add(link)
                }
            }
        }
        // Every pattern's last resort: the part's own movements that need no
        // equipment beyond what the user holds. No-kit movements pass any
        // granted set (empty requirement), so a part is never left empty —
        // but a chair-tier move no longer leaks into a strict bodyweight-only
        // build through this path.
        partPool.filter { ExerciseEquipmentTags.isAvailable(it, allowedEquipment) }.forEach(::add)
        return out
    }

    fun getMuscleGroupForExercise(name: String): String {
        // The authoritative catalog wins: it states the single primary mover,
        // so the result never depends on where an exercise happens to be listed.
        getPrimaryMuscleForExercise(name).takeIf { it.isNotEmpty() }?.let { return it }

        // Outside the pool (user-created custom exercises): no data -> no claim.
        // Guessing from the name re-introduced the "rows shown as shoulders" class
        // of bug at the analytics layer, so the blank result is the contract: the
        // stored group a log already carries stays authoritative for unknown names.
        return ""
    }

    /**
     * The best-known precise muscle for [name]. For pool exercises this is the
     * authoritative primary muscle from the catalog, so the result never depends
     * on the order the pool happens to list an exercise in. Only names outside
     * the pool fall back to the heuristic, which prefers a muscle whose topic
     * matches [preferredTopic] (the topic of the stored group).
     * Returns "" when the exercise is not part of the built-in pool.
     */
    fun getPrimaryMuscleGroupForExercise(name: String, preferredTopic: String): String {
        if (name.isBlank()) return ""
        getPrimaryMuscleForExercise(name).takeIf { it.isNotEmpty() }?.let { return it }
        val guessed = getMuscleGroupForExercise(name)
        return if (guessed.isNotEmpty() && getTopicForMuscle(guessed) == preferredTopic) guessed else ""
    }

    /**
     * Whether [exerciseName] belongs to the selected body [part]. The exercise's
     * precise primary muscle counts as a match, and so does the same broad topic
     * (a Chest day still accepts an exercise labelled "Pectorals", a Legs day
     * accepts "Quadriceps").
     */
    private fun exerciseMatchesPart(exerciseName: String, part: String): Boolean {
        val precise = getMuscleGroupForExercise(exerciseName)
        if (precise.equals(part, ignoreCase = true)) return true
        val preciseTopic = getTopicForMuscle(precise)
        return preciseTopic != "Other" && preciseTopic == getTopicForMuscle(part)
    }

    val template5DaySplit = WorkoutTemplate(
        id = "5day_hypertrophy",
        nameKey = "hypertrophy_5day_name",
        descriptionKey = "hypertrophy_5day_desc",
        badgeKey = "most_popular",
        daysCountKey = "workout_days_format_pl",
        daysCountArgs = 5 to 2,
        dayTemplates = listOf(
            DayTemplate("MONDAY", "chest_title", "chest_desc", false),
            DayTemplate("TUESDAY", "back_title", "back_desc", false),
            DayTemplate("WEDNESDAY", "legs_core_title", "legs_desc", false),
            DayTemplate("THURSDAY", "shoulders_title", "shoulders_desc", false),
            DayTemplate("FRIDAY", "arms_title", "arms_desc", false),
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
        daysCountKey = "workout_days_format_pl",
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell Bench Press", "Chest", 4, "8–18", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Lateral Raise", "Shoulders", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Tricep Extension", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Bent-Over Barbell Row", "Back", 4, "8–18", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Bent-Over Dumbbell Row", "Back", 3, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Bicep Curl", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "TUESDAY", "Dumbbell Hammer Curl", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "WEDNESDAY", "Barbell Back Squat", "Legs", 4, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Romanian Deadlift", "Legs", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Lunge", "Quadriceps", 3, "5–15 each leg", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Crunches", "Abs", 3, "15–25", "", 0.0, 45, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Floor Presses", "Chest", 4, "10–20", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Lateral Raise", "Shoulders", 4, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Bench Dips", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Bent-Over Barbell Row", "Back", 4, "8–18", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Pullover", "Back", 3, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Barbell Curl", "Biceps", 3, "8–18", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "SATURDAY", "Barbell Back Squat", "Legs", 4, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "SATURDAY", "Hanging Leg Raise", "Abs", 3, "10–20", "", 0.0, 45, 90, "", 2)
        )
    )

    val templateUpperLower = WorkoutTemplate(
        id = "upper_lower_4day",
        nameKey = "upper_lower_4day_name",
        descriptionKey = "upper_lower_4day_desc",
        badgeKey = "optimal_recovery",
        daysCountKey = "workout_days_format_pl",
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell Bench Press", "Chest", 4, "6–16", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Bent-Over Barbell Row", "Back", 4, "6–16", "", 0.0, 120, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "8–18", "", 0.0, 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Curl", "Biceps", 3, "8–18", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "TUESDAY", "Barbell Back Squat", "Legs", 4, "6–16", "", 0.0, 150, 90, "", 1),
            RoutineExerciseEntity(0, "TUESDAY", "Romanian Deadlift", "Legs", 3, "8–18", "", 0.0, 120, 90, "", 2),
            RoutineExerciseEntity(0, "TUESDAY", "Plank", "Abs", 3, "45 sec", "", 0.0, 45, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Floor Presses", "Chest", 3, "10–20", "", 0.0, 90, 90, "", 1),
            RoutineExerciseEntity(0, "THURSDAY", "Bent-Over Dumbbell Row", "Back", 3, "10–20", "", 0.0, 60, 90, "", 2),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Lateral Raise", "Shoulders", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Hammer Curl", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "THURSDAY", "Dumbbell Tricep Extension", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 5),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Lunge", "Quadriceps", 3, "5–15 each leg", "", 0.0, 60, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Hanging Leg Raise", "Abs", 3, "10–20", "", 0.0, 45, 90, "", 2)
        )
    )

    val templateFullBody = WorkoutTemplate(
        id = "fullbody_3day",
        nameKey = "fullbody_3day_name",
        descriptionKey = "fullbody_3day_desc",
        badgeKey = "time_efficient",
        daysCountKey = "workout_days_format_pl",
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
            RoutineExerciseEntity(0, "MONDAY", "Barbell Back Squat", "Legs", 3, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "MONDAY", "Barbell Bench Press", "Chest", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "MONDAY", "Bent-Over Barbell Row", "Back", 3, "8–18", "", 0.0, 90, 90, "", 3),
            RoutineExerciseEntity(0, "MONDAY", "Dumbbell Lateral Raise", "Shoulders", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "MONDAY", "Crunches", "Abs", 3, "15–25", "", 0.0, 45, 90, "", 5),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Lunge", "Quadriceps", 3, "5–15 each leg", "", 0.0, 60, 90, "", 1),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Shoulder Press", "Shoulders", 3, "8–18", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "WEDNESDAY", "Bent-Over Dumbbell Row", "Back", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "WEDNESDAY", "Dumbbell Bicep Curl", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Romanian Deadlift", "Legs", 3, "8–18", "", 0.0, 120, 90, "", 1),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Floor Presses", "Chest", 3, "10–20", "", 0.0, 90, 90, "", 2),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Hammer Curl", "Biceps", 3, "10–20", "", 0.0, 60, 90, "", 3),
            RoutineExerciseEntity(0, "FRIDAY", "Dumbbell Tricep Extension", "Triceps", 3, "10–20", "", 0.0, 60, 90, "", 4),
            RoutineExerciseEntity(0, "FRIDAY", "Hanging Leg Raise", "Abs", 3, "10–20", "", 0.0, 45, 90, "", 5)
        )
    )

    val allTemplates = listOf(template5DaySplit, templatePPL, templateUpperLower, templateFullBody)

    /**
     * Whether an exercise is measured in SECONDS rather than reps.
     *
     * THE ENGINE ANSWERS FIRST, because a name is not a unit. The old body sniffed
     * the name for "plank", "hold" or "rest", which misses every timed row whose
     * name says none of those - Jumping Jacks, all the cardio machines, every
     * stretch, the carries - and calls a row like "Rest-Pause Squat" timed. The
     * pool's own progression row carries the unit (`seconds`), so that is the
     * answer wherever the row exists; the name heuristic stays only as the
     * fallback for a custom exercise outside the pool.
     *
     * Owner's report (2026-10-09): "Jumping jacks is one of many exercises that
     * doesnt show the level style and it shows reps in the card yet the exercise is
     * based on time and has a timer????" - so this one question now has ONE
     * definition: the workout card, the view model's auto-selected seconds and the
     * exercise editor's time toggle all come through here.
     */
    fun isExerciseTimeBased(name: String): Boolean {
        ProgressionEngine.entryFor(name)?.let { return ProgressionEngine.isDurationBased(it) }
        val cleanName = name.trim().lowercase()
        return cleanName.contains("plank") || cleanName.contains("hold") || cleanName.contains("rest")
    }

    /**
     * The seconds a routine row's stored target carries ("30 sec" -> 30, "45" ->
     * 45, "90 s" -> 90), or null when the row does not carry a plain duration.
     *
     * A REP RANGE IS NOT A DURATION, and reading one as seconds is how a timed
     * exercise went wrong a fifth time. Rows written before the level system store
     * a rep range for timed exercises (Jumping Jacks' "8-12"), so taking the first
     * number out of them opened the editor's SECONDS field at "8" - and the save
     * path strips non-digits, so "8-12" would have been written back as "812 sec"
     * and started a 812-second timer for a 90-second exercise. Anything carrying a
     * range separator is refused instead, and the caller falls back to the level
     * system's own prescription ([ProgressionEngine.valueKindOf]).
     */
    fun secondsInStoredTarget(stored: String?): Int? {
        val raw = stored?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (raw.any { it == '-' || it == '\u2013' || it == '\u2014' }) return null
        val match = Regex("^(\\d+)\\s*(?:sec|secs|s)?$", RegexOption.IGNORE_CASE).find(raw) ?: return null
        return match.groupValues[1].toIntOrNull()?.takeIf { it in 1..7200 }
    }

    fun getRecommendedRepsForExercise(name: String): String {
        if (isExerciseTimeBased(name)) return "30"
        val cleanName = name.trim().lowercase()
        val muscleGroup = getMuscleGroupForExercise(name)
        val rawRange = when {
            cleanName.contains("squat") || cleanName.contains("deadlift") || 
            cleanName.contains("bench") || cleanName.contains("press") ||
            cleanName.contains("row") || cleanName.contains("pull-up") -> "8–12"
            muscleGroup in listOf("Biceps", "Triceps", "Lateral Deltoids", "Rear Deltoids", "Calves") -> "10–15"
            muscleGroup == "Abs" || muscleGroup == "Obliques" -> "15–20"
            else -> "8–12"
        }
        return enforceRepRange(rawRange)
    }

    /**
     * True when [reps] parses cleanly but sits entirely outside the recommended
     * band for [exerciseName] — a soft nudge, not a validation error: the row
     * still saves. A PARTIAL overlap (e.g. 8–20 vs the 8–12 recommendation) is
     * fine, as is anything unparseable.
     */
    fun isOutsideRecommendedRepRange(exerciseName: String, reps: String): Boolean {
        if (isExerciseTimeBased(exerciseName)) return false
        val typed = parseRepRange(reps) ?: return false
        val recommended = parseRepRange(getRecommendedRepsForExercise(exerciseName)) ?: return false
        val (tMin, tMax) = typed
        val (rMin, rMax) = recommended
        return tMax < rMin || tMin > rMax
    }

    /** Classifies an intensity string into "Easy", "Medium" or "Hard" based on RPE, RIR or keywords. */
    fun classifyIntensity(intensity: String): String {
        val t = intensity.lowercase()
        val rpeMatch = Regex("rpe\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*rpe").find(t)
        if (rpeMatch != null) {
            val raw = rpeMatch.groupValues[1].ifBlank { rpeMatch.groupValues[2] }
            val rpe = raw.toDoubleOrNull()
            if (rpe != null) {
                return when {
                    rpe >= 9.0 -> "Hard"
                    rpe <= 7.0 -> "Easy"
                    else -> "Medium"
                }
            }
        }
        val rirMatch = Regex("rir\\s*(\\d+)|(\\d+)\\s*rir").find(t)
        if (rirMatch != null) {
            val raw = rirMatch.groupValues[1].ifBlank { rirMatch.groupValues[2] }
            val rir = raw.toIntOrNull()
            if (rir != null) {
                return when {
                    rir <= 1 -> "Hard"
                    rir >= 3 -> "Easy"
                    else -> "Medium"
                }
            }
        }
        return when {
            t.contains("hard") || t.contains("heavy") || t.contains("failure") || t.contains("max") || t.contains("intense") -> "Hard"
            t.contains("easy") || t.contains("light") || t.contains("warm") -> "Easy"
            else -> "Medium"
        }
    }

    /**
     * Adapts an auto-selected reps range to the intensity/difficulty the user typed:
     * Hard / high RPE shifts the rep center down, Easy / low RPE shifts it up.
     * Single values (e.g. time-based entries) are returned unchanged.
     */
    fun adaptRepsToIntensity(recommended: String, intensity: String): String {
        val range = parseRepRange(recommended) ?: return recommended
        val (minVal, maxVal) = range
        if (minVal == maxVal) return recommended
        val center = (minVal + maxVal) / 2
        val adaptedCenter = when (classifyIntensity(intensity)) {
            "Hard" -> (center - 3).coerceAtLeast(1)
            "Easy" -> center + 3
            else -> center
        }
        return repRangeAround(adaptedCenter)
    }

    // Starting loads by equipment class, before difficulty/style scaling. These are
    // deliberately conservative first-session numbers: the point is a sane starting
    // point the user adjusts, not a 1RM estimate.
    private const val BASE_LOAD_BARBELL_COMPOUND = 40.0
    private const val BASE_LOAD_BARBELL = 20.0
    private const val BASE_LOAD_DUMBBELL_COMPOUND = 10.0
    private const val BASE_LOAD_DUMBBELL_ISOLATION = 6.0
    private const val BASE_LOAD_MACHINE = 20.0
    private const val BASE_LOAD_DEFAULT = 10.0

    /**
     * A suggested starting weight for an exercise, adapted to the chosen difficulty
     * (Easy / Medium / Hard). Difficulty is the only input, so the estimate can't
     * be pulled in two directions by two different controls.
     *
     * Since the v5 progression integration the PRIMARY source is the exercise's
     * per-exercise starting prescription from the progression database: Easy /
     * Medium / Hard map to the CSV's easy/medium/hard start levels and carry the
     * load the ladder actually uses there. The old equipment-class heuristic below
     * remains as the fallback for exercises the progression data does not cover
     * (custom exercises, manual-review rows) — never for a pool exercise with a
     * valid prescription.
     *
     * Returns 0.0 when there is nothing to load — bodyweight and timed movements —
     * which the UI already reads as "no weight".
     *
     * This is a fallback only: a weight the plan set explicitly, or one the user has
     * already worked up to for this exercise, always wins over this estimate.
     */
    fun getStartingWeightKg(
        exerciseName: String,
        intensity: String
    ): Double {
        if (exerciseName.isBlank()) return 0.0
        if (isBodyweightExercise(exerciseName)) return 0.0
        if (isExerciseTimeBased(exerciseName)) return 0.0

        // Per-exercise prescription from the v5 progression database wins.
        val difficulty = when (classifyIntensity(intensity)) {
            "Easy" -> ProgressionEngine.Difficulty.EASY
            "Hard" -> ProgressionEngine.Difficulty.HARD
            else -> ProgressionEngine.Difficulty.MEDIUM
        }
        val prescription = ProgressionEngine.startingPrescription(exerciseName, difficulty)
        if (prescription != null) {
            val kg = prescription.value
            if (kg != null && kg > 0.0 && prescription.unit.startsWith("kg")) {
                return roundToPlate(kg)
            }
            // Non-kg units (stack steps, resistance steps, variation steps) and
            // duration progressions carry no absolute weight: bodyweight-style 0.0.
            if (!prescription.unit.startsWith("kg")) return 0.0
        }

        val difficultyFactor = when (classifyIntensity(intensity)) {
            "Easy" -> 0.75
            "Hard" -> 1.25
            else -> 1.0
        }
        return roundToPlate(baseLoadForExercise(exerciseName) * difficultyFactor)
    }

    /**
     * The starting target reps for an exercise at a difficulty, from the
     * progression database's per-exercise prescriptions. Returns null when the
     * exercise has no valid progression data (the caller keeps the goal's
     * generic rep range).
     */
    fun getStartingTargetReps(exerciseName: String, intensity: String): List<Int>? {
        if (exerciseName.isBlank()) return null
        val difficulty = when (classifyIntensity(intensity)) {
            "Easy" -> ProgressionEngine.Difficulty.EASY
            "Hard" -> ProgressionEngine.Difficulty.HARD
            else -> ProgressionEngine.Difficulty.MEDIUM
        }
        return ProgressionEngine.startingPrescription(exerciseName, difficulty)?.reps
    }

    /** Base load for an exercise, picked from the equipment it needs. */
    private fun baseLoadForExercise(name: String): Double {
        val n = name.lowercase().trim()
        val isDumbbell = n.contains("dumbbell") || n.contains("db ") || n.contains("goblet")
        val isMachine = n.contains("machine") || n.contains("cable") || n.contains("smith") ||
            n.contains("pec deck") || n.contains("pulldown") || n.contains("pushdown") ||
            n.contains("crossover") || n.contains("rope") || n.contains("hammer strength")
        // Isolation = single-joint movements, loaded much lighter than compounds.
        val isIsolation = n.contains("curl") || n.contains("extension") || n.contains("lateral") ||
            n.contains("raise") || n.contains("pushdown") || n.contains("kickback") ||
            n.contains("fly") || n.contains("flye") || n.contains("shrug") || n.contains("calf")
        return when {
            n.contains("barbell") && isHeavyCompound(n) -> BASE_LOAD_BARBELL_COMPOUND
            n.contains("barbell") -> BASE_LOAD_BARBELL
            isDumbbell && isIsolation -> BASE_LOAD_DUMBBELL_ISOLATION
            isDumbbell -> BASE_LOAD_DUMBBELL_COMPOUND
            isMachine -> BASE_LOAD_MACHINE
            else -> BASE_LOAD_DEFAULT
        }
    }

    /**
     * Chip semantics for the equipment filter.
     *
     * [EQUIPMENT_ALL] is not a separate type — it is the empty selection, which the
     * generator reads as "every type allowed". It is mutually exclusive with the
     * specific chips, so the result is never a mix of All and a type:
     *  - tapping All clears the selection (everything allowed),
     *  - tapping a type while All is on narrows the build to just that type,
     *  - tapping a selected type removes it, and clearing the last one falls back to All,
     *  - ticking every type by hand is also just "no restriction", so it collapses to All.
     */
    fun toggleEquipmentSelection(
        selected: List<String>,
        tapped: String,
        allOptions: List<String>
    ): List<String> = when {
        tapped == EQUIPMENT_ALL -> emptyList()
        selected.isEmpty() -> listOf(tapped)
        selected.contains(tapped) -> selected - tapped
        else -> (selected + tapped).let { next ->
            if (next.size == allOptions.size) emptyList() else next
        }
    }

    /** Rounds to a loadable step: 1 kg on the light dumbbells, 2.5 kg on the rest. */
    private fun roundToPlate(kg: Double): Double {
        if (kg <= 0.0) return 0.0
        val step = if (kg < 10.0) 1.0 else 2.5
        val rounded = kotlin.math.round(kg / step) * step
        return kotlin.math.max(step, kotlin.math.round(rounded * 10.0) / 10.0)
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
        // The pool states its own apparatus, so a pool exercise is bodyweight
        // exactly when the catalog says so. The catalog is consulted FIRST on
        // purpose: the name heuristic below calls a "squat" bodyweight unless
        // the name says otherwise, and after the name generalization the
        // barbell squat is called "Barbell Back Squat" - so the heuristic was declaring
        // it bodyweight and zeroing its starting load. The name is only ever a
        // fallback, for a user's own exercise that has no catalog row.
        val fromCatalog = ExerciseCatalog.equipmentFor(name)
        if (fromCatalog.isNotEmpty()) return fromCatalog == EQUIPMENT_BODYWEIGHT

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
            lowerName.contains("inverted row") ||
            lowerName.contains("dead hang") ||
            lowerName.contains("australian pull-up") ||
            lowerName.contains("false grip") ||
            lowerName.contains("inverted curl") ||
            (lowerName.contains("squat") && !lowerName.contains("barbell") && !lowerName.contains("dumbbell") && !lowerName.contains("machine") && !lowerName.contains("hack") && !lowerName.contains("goblet")) ||
            (lowerName.contains("lunge") && !lowerName.contains("db") && !lowerName.contains("dumbbell") && !lowerName.contains("barbell")) ||
            lowerName.contains("(bw)") || 
            lowerName.contains("bodyweight")
        ) return true

        return false
    }

    /**
     * Consecutive days with at least one logged set, counting back from today.
     * If today has no workout yet, the streak is counted from yesterday so a
     * workout done yesterday still keeps the streak alive. Returns 0 when there
     * is no streak.
     */
    fun calculateConsecutiveWorkoutDays(loggedSets: List<LoggedSetEntity>): Int {
        if (loggedSets.isEmpty()) return 0
        val workoutDayKeys = loggedSets.map { dateKey(it.timestamp) }.toSet()

        val cursor = Calendar.getInstance().apply { startOfDay() }
        if (dateKey(cursor.timeInMillis) !in workoutDayKeys) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var count = 0
        while (dateKey(cursor.timeInMillis) in workoutDayKeys) {
            count++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    /**
     * Scheduled workout days (routine days that are not rest days) which have
     * passed without a single logged set. Counted from the Monday of the week
     * containing the first logged set (or the current week when nothing has
     * been logged yet) up to yesterday, so today isn't marked as skipped while
     * it can still be completed. Starts at 0 when there is no schedule.
     */
    fun calculateSkippedWorkoutDays(
        loggedSets: List<LoggedSetEntity>,
        routineDays: List<RoutineDayEntity>
    ): Int {
        val scheduledDays = routineDays
            .filter { !it.isRestDay }
            .mapNotNull { dayOfWeekToCalendarDay(it.dayOfWeek) }
            .toSet()
        if (scheduledDays.isEmpty()) return 0

        val loggedKeys = loggedSets.map { dateKey(it.timestamp) }.toSet()

        val start = Calendar.getInstance().apply { startOfDay() }
        if (loggedSets.isNotEmpty()) {
            start.timeInMillis = loggedSets.minOf { it.timestamp }
            start.startOfDay()
        }
        // Align to the Monday of the start week
        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1)
        }

        val today = Calendar.getInstance().apply { startOfDay() }
        var skipped = 0
        val cursor = start.clone() as Calendar
        while (cursor.before(today)) {
            if (cursor.get(Calendar.DAY_OF_WEEK) in scheduledDays &&
                dateKey(cursor.timeInMillis) !in loggedKeys
            ) {
                skipped++
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return skipped
    }

    private fun dayOfWeekToCalendarDay(dayOfWeek: String): Int? = when (dayOfWeek.uppercase()) {
        "MONDAY" -> Calendar.MONDAY
        "TUESDAY" -> Calendar.TUESDAY
        "WEDNESDAY" -> Calendar.WEDNESDAY
        "THURSDAY" -> Calendar.THURSDAY
        "FRIDAY" -> Calendar.FRIDAY
        "SATURDAY" -> Calendar.SATURDAY
        "SUNDAY" -> Calendar.SUNDAY
        else -> null
    }

    /** Normalizes a timestamp to a (year, day-of-year) key for date comparisons. */
    private fun dateKey(timestamp: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            startOfDay()
        }
        return cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
    }

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
