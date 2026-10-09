package com.grafizum.repio.data

import com.grafizum.repio.data.ExerciseCatalog.ENTRIES

/**
 * Fine-grained equipment tagging for the exercise pool.
 *
 * The coarse catalog category ("Dumbbell", "Bodyweight", …) answers "which
 * family of kit does this belong to", but not "can THIS user actually do it":
 * a Decline Bench Press was offered to a dumbbells-without-bench user, and a
 * Bench Dips to a user without a stable chair. This layer derives the precise
 * setup every pool exercise needs — the spec's `equipment_required` field —
 * from the catalog entry at runtime, so the generated catalog stays untouched
 * and the canonical English name remains the single join key.
 *
 * Requirement model: every exercise maps to a list of requirement SETS
 * (OR of ANDs). A set is satisfied when the user was granted every tag in it;
 * the exercise is available when ANY set is satisfied:
 *
 *   Decline Bench Press  -> [ {barbell_and_rack}, {dumbbells_and_bench} ]
 *   Bulgarian Split Squat -> [ {chair, dumbbells_only}, {bench, dumbbells_only} ]
 *
 * Pure bodyweight movements require the EMPTY set, which is always satisfied —
 * the generator's invariant "the user's equipment + bodyweight" becomes
 * structural instead of a special case.
 *
 * No catalog exercise currently REQUIRES stairs or a weighted bag; the two
 * tags exist and are granted by the welcome screen (the spec's Home Equipment
 * checkboxes), and stair work aliases the step-up tier so the checkboxes are
 * never dead ends.
 */
object ExerciseEquipmentTags {

    // ---------- 1. BODYWEIGHT EQUIPMENT ----------
    const val NONE_REQUIRED = "none_required"
    const val PULL_UP_BAR = "pull_up_bar"
    const val DIP_BARS = "dip_bars"
    const val GYMNASTIC_RINGS = "gymnastic_rings"

    // ---------- 2. HOME EQUIPMENT ----------
    // A chair and a bench are two different pieces of furniture and the owner
    // picks them on two different tiles: "Chair / Bench" on one button meant a
    // user with only a kitchen chair silently claimed a weight bench (and vice
    // versa), and both drew the same bench icon. They are separate tags now.
    const val CHAIR = "chair"
    const val BENCH = "bench"
    /**
     * The pre-split tag. It is NOT in [ALL_TAGS] any more: requirements never
     * ask for it, so an exercise can no longer be satisfied by "some furniture".
     * [allowedTagsFor] still expands a persisted `chair_or_bench` into both new
     * tags, which is what keeps an existing selection working.
     */
    const val CHAIR_OR_BENCH = "chair_or_bench"
    const val WALL = "wall"
    const val STAIRS = "stairs"
    const val BACKPACK_OR_WEIGHTED_BAG = "backpack_or_weighted_bag"
    const val TOWEL = "towel"
    const val RESISTANCE_BANDS = "resistance_bands"

    // ---------- 3. DUMBBELL EQUIPMENT ----------
    const val DUMBBELLS_ONLY = "dumbbells_only"
    const val DUMBBELLS_AND_BENCH = "dumbbells_and_bench"
    const val DUMBBELLS_AND_FLOOR = "dumbbells_and_floor"

    // ---------- 4. BARBELL EQUIPMENT ----------
    const val BARBELL_AND_RACK = "barbell_and_rack"
    const val BARBELL_AND_FLOOR = "barbell_and_floor"
    const val BARBELL_ONLY = "barbell_only"

    // ---------- 5. MACHINE / CABLE EQUIPMENT ----------
    const val CABLE_MACHINE = "cable_machine"
    const val LEG_PRESS_MACHINE = "leg_press_machine"
    const val LAT_PULLDOWN_MACHINE = "lat_pulldown_machine"
    const val SMITH_MACHINE = "smith_machine"
    const val LEG_EXTENSION_MACHINE = "leg_extension_machine"
    const val LEG_CURL_MACHINE = "leg_curl_machine"
    const val CHEST_PRESS_MACHINE = "chest_press_machine"
    const val SEATED_ROW_MACHINE = "seated_row_machine"
    // Selectorized stations the spec's eight machine tags don't cover
    // (shrug/crunch/calf/back-extension/hip-abduction stations, ab wheel,
    // cardio machines, stability ball work, sled/rope/plate gear). Only a
    // full gym grants it, so nothing half-matches an honest home setup.
    const val MACHINE_OTHER = "machine_other"

    // ---------- 6. KETTLEBELL EQUIPMENT ----------
    const val KETTLEBELL_ONLY = "kettlebell_only"
    const val KETTLEBELL_AND_BENCH = "kettlebell_and_bench"

    /** Every tag the classifier can emit. */
    val ALL_TAGS = listOf(
        NONE_REQUIRED, PULL_UP_BAR, DIP_BARS, GYMNASTIC_RINGS,
        CHAIR, BENCH, WALL, STAIRS, BACKPACK_OR_WEIGHTED_BAG, TOWEL, RESISTANCE_BANDS,
        DUMBBELLS_ONLY, DUMBBELLS_AND_BENCH, DUMBBELLS_AND_FLOOR,
        BARBELL_AND_RACK, BARBELL_AND_FLOOR, BARBELL_ONLY,
        CABLE_MACHINE, LEG_PRESS_MACHINE, LAT_PULLDOWN_MACHINE, SMITH_MACHINE,
        LEG_EXTENSION_MACHINE, LEG_CURL_MACHINE, CHEST_PRESS_MACHINE, SEATED_ROW_MACHINE,
        MACHINE_OTHER,
        KETTLEBELL_ONLY, KETTLEBELL_AND_BENCH,
    )

    /** Tags the welcome screen's "Home equipment" checkboxes grant. */
    val HOME_EQUIPMENT_TAGS = listOf(
        CHAIR, BENCH, PULL_UP_BAR, DIP_BARS, GYMNASTIC_RINGS,
        WALL, STAIRS, BACKPACK_OR_WEIGHTED_BAG, TOWEL, RESISTANCE_BANDS,
    )

    /**
     * Tags the welcome screen's "Gym equipment" checkboxes grant.
     *
     * [DUMBBELLS_AND_BENCH] is deliberately NOT here. "Dumbbell + Bench" was a
     * second, redundant button for a combination the grid already expresses as
     * two tiles (Dumbbells and Bench), and it was the tile that read as
     * nonsense next to the real Bench tile. The requirements it used to carry
     * now ask for the two tiles directly, so nothing is lost by dropping it.
     * The tag itself stays in [ALL_TAGS] so a selection persisted under it
     * still parses after an update, and [allowedTagsFor] expands it to the two
     * tiles that replaced it.
     */
    val GYM_EQUIPMENT_TAGS = listOf(
        DUMBBELLS_ONLY, BARBELL_AND_RACK, BARBELL_AND_FLOOR,
        KETTLEBELL_ONLY, CABLE_MACHINE, MACHINE_OTHER,
    )

    // ---------- PRESETS ----------

    // Home scenarios (spec §2). Pure bodyweight grants NOTHING extra — every
    // no-requirement movement passes by construction.
    const val PRESET_BODYWEIGHT = "preset_bodyweight"
    const val PRESET_BW_PULLUP_BAR = "preset_bw_pullup_bar"
    const val PRESET_BW_DIP_BARS = "preset_bw_dip_bars"
    const val PRESET_BW_BANDS = "preset_bw_bands"
    const val PRESET_DUMBBELLS = "preset_dumbbells"
    const val PRESET_DB_BENCH = "preset_db_bench"
    const val PRESET_DB_BENCH_BAR = "preset_db_bench_bar"
    const val PRESET_BARBELL_RACK = "preset_barbell_rack"
    const val PRESET_FULL_GYM = "preset_full_gym"
    const val PRESET_CUSTOM = "preset_custom"

    val PRESET_IDS = listOf(
        PRESET_BODYWEIGHT, PRESET_BW_PULLUP_BAR, PRESET_BW_DIP_BARS, PRESET_BW_BANDS,
        PRESET_DUMBBELLS, PRESET_DB_BENCH, PRESET_DB_BENCH_BAR, PRESET_BARBELL_RACK,
        PRESET_FULL_GYM, PRESET_CUSTOM,
    )

    fun presetTags(presetId: String): Set<String> = when (presetId) {
        PRESET_BODYWEIGHT -> emptySet()
        PRESET_BW_PULLUP_BAR -> setOf(PULL_UP_BAR)
        PRESET_BW_DIP_BARS -> setOf(DIP_BARS)
        PRESET_BW_BANDS -> setOf(RESISTANCE_BANDS)
        PRESET_DUMBBELLS -> setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_FLOOR)
        // The bench presets grant the home BENCH tile too: the user said they
        // own a bench, so the grid shows it checked instead of hiding it.
        PRESET_DB_BENCH -> setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_FLOOR, BENCH)
        PRESET_DB_BENCH_BAR -> setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_FLOOR, BENCH, PULL_UP_BAR)
        PRESET_BARBELL_RACK -> setOf(BARBELL_AND_RACK, BARBELL_AND_FLOOR, BARBELL_ONLY)
        PRESET_FULL_GYM -> ALL_TAGS.toSet()
        else -> emptySet()
    }

    fun isPreset(id: String): Boolean = id in PRESET_IDS

    fun homeEquipmentLabelKey(tag: String): String = when (tag) {
        CHAIR -> "home_equipment_chair"
        BENCH -> "home_equipment_bench"
        PULL_UP_BAR -> "home_equipment_pullup_bar"
        DIP_BARS -> "home_equipment_dip_bars"
        GYMNASTIC_RINGS -> "home_equipment_rings"
        WALL -> "home_equipment_wall"
        STAIRS -> "home_equipment_stairs"
        BACKPACK_OR_WEIGHTED_BAG -> "home_equipment_backpack"
        TOWEL -> "home_equipment_towel"
        RESISTANCE_BANDS -> "home_equipment_bands"
        else -> "equipment"
    }

    fun gymEquipmentLabelKey(tag: String): String = when (tag) {
        DUMBBELLS_ONLY -> "gym_equipment_dumbbells"
        DUMBBELLS_AND_BENCH -> "gym_equipment_db_bench"
        BARBELL_AND_RACK -> "gym_equipment_barbell"
        BARBELL_AND_FLOOR -> "gym_equipment_rack"
        KETTLEBELL_ONLY -> "gym_equipment_kettlebell"
        CABLE_MACHINE -> "gym_equipment_cable"
        MACHINE_OTHER -> "gym_equipment_machines"
        else -> "equipment"
    }

    // ---------- REQUIREMENT CLASSIFIER ----------

    private fun s(vararg tags: String): Set<String> = tags.toSet()
    private fun req(vararg alternatives: Set<String>): List<Set<String>> = alternatives.toList()

    /**
     * "a chair OR a bench", as two alternatives rather than one merged tag.
     *
     * The tiles are separate, but the movements that take a piece of furniture
     * at a specific height genuinely work on either - bench dips on a chair is
     * the standard home substitute, an incline push-up on a chair is the easier
     * rung of the ladder. Merging them back into one tag is what let a chair
     * stand in for a bench everywhere; keeping two alternatives keeps the
     * buttons honest without pretending the furniture is interchangeable on
     * the movements that name it (those ask for BENCH or DUMBBELLS_AND_BENCH).
     */
    private fun chairOrBench(): List<Set<String>> = req(s(CHAIR), s(BENCH))

    /** The same, with one more piece of kit every alternative needs. */
    private fun chairOrBenchWith(vararg also: String): List<Set<String>> =
        req(s(CHAIR, *also), s(BENCH, *also))

    /** Exact-name overrides, evaluated before every rule. Keys are lowercase. */
    private val EXACT_OVERRIDES: Map<String, List<Set<String>>> = mapOf(
        // The spec's worked examples.
        "push-up" to req(emptySet()),
        // The seat is TWO alternatives, so a kitchen chair is not a dead end for
        // a user who also owns dumbbells. DUMBBELLS_AND_BENCH was retired as a
        // tile and nothing may ask for it any more.
        // A decline/incline/bench movement needs a real BENCH, not "a chair or a
        // bench": a kitchen chair has no adjustable angle and no pad to brace
        // against. See the Dumbbell branch below for the full reasoning.
        //
        // This row is the DUMBBELL decline press (its catalog category is
        // Dumbbell, and "Decline Barbell Bench Press" / "Decline EZ-Bar Bench
        // Press" are separate rows). It used to list a barbell alternative too,
        // which meant a barbell-only user was offered a row filed under the
        // Dumbbell family - the category/gate disagreement this class is about.
        "decline bench press" to req(s(BENCH, DUMBBELLS_ONLY)),
        "dumbbell bench press" to req(s(BENCH, DUMBBELLS_ONLY)),
        "dumbbell floor press" to req(s(DUMBBELLS_AND_FLOOR)),
        "goblet squat" to req(s(DUMBBELLS_ONLY)),
        "bench dips" to chairOrBench(),
        "bulgarian split squat" to chairOrBenchWith(DUMBBELLS_ONLY),
        "kettlebell bulgarian split squat" to chairOrBenchWith(KETTLEBELL_ONLY),
        // Substitution-chain members the rules would otherwise misplace.
        "trx row" to req(s(GYMNASTIC_RINGS)),
        "assisted pull ups" to req(s(LAT_PULLDOWN_MACHINE)),
        "machine assisted dips" to req(s(DIP_BARS)),
        "romanian deadlift" to req(s(BARBELL_AND_FLOOR)),
        "decline push-up" to chairOrBench(),
        "incline push-up" to chairOrBench(),
        // Floor dips are floor moves, not parallel-bar moves.
        "crab dips" to req(emptySet()),
        "reverse plank dips" to req(emptySet()),
        // Yoga poses whose names contain gear words ("Chair Pose" needs no chair).
        "chair pose" to req(emptySet()),
        "revolved chair pose" to req(emptySet()),
        // Rows under a sturdy table need something sturdy to hang from — the
        // chair tier (a table is furniture the home checkboxes cover).
        "inverted row" to chairOrBench(),
        // Weighted floor work: any weight source the home checkboxes grant.
        "weighted wall crunch" to req(s(BACKPACK_OR_WEIGHTED_BAG), s(DUMBBELLS_ONLY)),
        // Gear-specific items with no home equivalent.
        "ab wheel rollout" to req(s(MACHINE_OTHER)),
        "captain's chair knee raise" to req(s(MACHINE_OTHER)),
        "captain's chair leg raise" to req(s(MACHINE_OTHER)),
        "dead hang" to req(s(PULL_UP_BAR)),
        "human flag" to req(s(PULL_UP_BAR)),
        "front lever" to req(s(PULL_UP_BAR)),
        "back lever" to req(s(PULL_UP_BAR)),
        "planche" to req(emptySet()),
        "l sit" to req(emptySet()),
        "weighted dips" to req(s(DIP_BARS)),
        "weighted pull-up" to req(s(PULL_UP_BAR)),
        "stability ball push-up" to req(s(MACHINE_OTHER)),
        "stability ball push-up (hands on ball)" to req(s(MACHINE_OTHER)),
        "stability ball wall squat" to req(s(MACHINE_OTHER)),
        // THE BALL IS EQUIPMENT, and these four rows never said so in a word the
        // rules key on, so a bodyweight-only kit was offered them even though
        // their own instructions put the hands or the feet ON A BALL. They join
        // the stability-ball rows above in [MACHINE_OTHER], which is where this
        // pool already keeps the ball.
        "ball pike" to req(s(MACHINE_OTHER)),
        "knee tuck (ball)" to req(s(MACHINE_OTHER)),
        "hip bridge (ball)" to req(s(MACHINE_OTHER)),
        "leg curl (ball)" to req(s(MACHINE_OTHER)),
        // A 45-DEGREE / HYPEREXTENSION BENCH IS NOT THE FLAT BENCH TILE. It has the
        // hip pad and the anchored foot roller, and no flat bench or chair stands
        // in for it - so the row asks for the gym-gear bucket, which is also what
        // its own sibling "Machine Back Extension" asks for. Owner report,
        // 2026-10-09: "back extensions only works with a certain bench which i
        // didnt select so thats wrongly shown".
        "back extension" to req(s(MACHINE_OTHER)),
        // "The lifter touches their feet to a pull-up bar."
        "toes to bar" to req(s(PULL_UP_BAR)),
        // "...curls a bell with a neutral grip OVER A PREACHER PAD, which fixes the
        // elbow". The pad is the exercise - the sibling "Machine Preacher Curl"
        // already asks for the same bucket.
        "preacher hammer curl" to req(s(MACHINE_OTHER)),
        // "...performed face down on either an INCLINE BENCH or on the flat side of a
        // preacher bench." A barbell plus a bench that can be laid face-down on, so
        // both alternatives carry both tags - a floor-only barbell user is not
        // being offered a curl they cannot set up.
        "spider curl (ez-bar)" to req(
            s(BARBELL_AND_FLOOR, BENCH), s(BARBELL_AND_RACK, BENCH), s(BARBELL_AND_FLOOR, MACHINE_OTHER)
        ),
        // "A specific machine or another person on one's back ... this method uses a
        // weight belt": the bodyweight variant still needs the apparatus.
        "donkey calf raise" to req(s(MACHINE_OTHER)),
        "box jump" to req(s(CHAIR), s(BENCH), s(STAIRS)),
        "step ups" to req(s(CHAIR), s(BENCH), s(STAIRS)),
        "nordic hamstring curl" to req(emptySet()),
        "jump rope" to req(emptySet()),
    )

    private val SMITH_SET = setOf(
        "chest-supported smith machine row", "seated smith machine shoulder press",
        "bench press (smith machine)", "bent over row (smith machine)",
        "bulgarian split squat (smith machine)", "calf raise (smith machine)",
        "decline bench press (smith machine)", "front squat (smith machine)",
        "good morning (smith machine)", "hip thrust (smith machine)",
        "incline bench press (smith machine)", "reverse grip bent over row (smith machine)",
        "reverse lunge (smith machine)", "romanian deadlift (smith machine)",
        "shoulder press (smith machine)", "smith machine shrug",
        "split squat (smith machine)", "smith machine squat", "upright row (smith machine)",
    )

    private val LEG_PRESS_SET = setOf(
        "close-stance leg press", "high-foot leg press", "horizontal leg press",
        "leg press", "single leg press", "wide-stance leg press",
        "hack squat", "hack squat calf raise",
    )

    private val LEG_CURL_SET = setOf(
        "lying leg curl", "single leg lying leg curl", "seated leg curl",
    )

    private val LEG_EXTENSION_SET = setOf("leg extension", "single leg extension")

    private val CHEST_PRESS_SET = setOf(
        "machine chest press", "machine chest fly", "pec deck",
        "machine shoulder press", "single-arm machine shoulder press",
        "plate-loaded lateral raise", "single-arm plate-loaded lateral raise",
    )

    private val SEATED_ROW_SET = setOf<String>()

    private val MACHINE_OTHER_SET = setOf(
        "machine bicep curl", "machine preacher curl", "machine triceps extension",
        "machine calf raise", "machine back extension", "machine seated crunch",
        "plate-loaded shrug", "plate-loaded donkey calf raise", "plate-loaded glute drive",
        "seated calf raise", "standing calf raise",
        "hip adduction", "machine hip abduction",
    )

    private val DUMBBELL_BENCH_SET = setOf(
        "dumbbell bench press", "close-grip dumbbell bench press",
        "incline dumbbell press", "incline dumbbell fly", "decline dumbbell fly",
        "incline dumbbell curl", "incline hammer curl",
        "chest-supported dumbbell row", "single-arm chest-supported dumbbell row",
        "dumbbell bench pull",
    )

    private val DUMBBELL_FLOOR_SET = setOf("dumbbell floor press", "dumbbell pullover")

    private val BARBELL_ONLY_SET = setOf(
        "barbell curl", "barbell preacher curl", "barbell wrist curl",
        "barbell shrug", "behind the back barbell shrug", "ez-bar shrug",
        "barbell upright row", "rear delt row (barbell)",
        "cheat curl", "drag curl", "ez-bar curl", "ez-bar reverse curl",
          "close-grip ez-bar curl",
        "ez-bar upright row",  
    )

    private val KETTLEBELL_BENCH_SET = setOf(
        "chest-supported kettlebell row",
    )

    private val MACHINE_TAG_BY_MUSCLE = mapOf(
        "triceps" to CABLE_MACHINE, "biceps" to CABLE_MACHINE, "forearms" to CABLE_MACHINE,
        "pectorals" to CHEST_PRESS_MACHINE, "lats" to LAT_PULLDOWN_MACHINE,
        "rear deltoids" to CABLE_MACHINE,
    )

    /**
     * The requirement sets for [name]: available when the user's granted tags
     * fully contain ANY set. Unknown names (custom exercises) require nothing
     * — they are never blocked by equipment.
     */
    fun requirementsFor(name: String): List<Set<String>> {
        val clean = name.trim().lowercase()
        if (clean.isEmpty()) return req(emptySet())
        EXACT_OVERRIDES[clean]?.let { return it }
        val entry = ENTRIES.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return req(emptySet())

        val n = entry.name.lowercase()
        // A hyphen and a space are the same word to a human, and this gate is
        // ABOUT what the exercise needs, so the spelling must not decide it.
        // One row was catalogued as "Chest Supported Dumbbell Shrug" while its
        // four siblings are hyphenated, and the rule below matches
        // "chest-supported" - so that shrug fell through to dumbbells-only and
        // was offered to users who own no bench. That is the equipment-leak
        // class this file exists to prevent, caused by punctuation.
        val gateName = n.replace("chest supported", "chest-supported")
        when (entry.equipment) {
            "Cable" -> return req(s(CABLE_MACHINE))
            "Bands" -> return req(s(RESISTANCE_BANDS))
            "Machine" -> {
                if (n in SMITH_SET) return req(s(SMITH_MACHINE))
                if (n.contains("pulldown")) return req(s(LAT_PULLDOWN_MACHINE))
                if (n in LEG_PRESS_SET) return req(s(LEG_PRESS_MACHINE))
                if (n in LEG_CURL_SET) return req(s(LEG_CURL_MACHINE))
                if (n in LEG_EXTENSION_SET) return req(s(LEG_EXTENSION_MACHINE))
                if (n in SEATED_ROW_SET) return req(s(SEATED_ROW_MACHINE))
                if (n in CHEST_PRESS_SET) return req(s(CHEST_PRESS_MACHINE))
                if (n in MACHINE_OTHER_SET) return req(s(MACHINE_OTHER))
                MACHINE_TAG_BY_MUSCLE[entry.muscle.lowercase()]?.let { return req(s(it)) }
                return req(s(MACHINE_OTHER))
            }
            "Barbell" -> {
                if (n.contains("squat") || n.contains("bench press") ||
                    n.contains("overhead press") || n.contains("behind the neck")
                ) return req(s(BARBELL_AND_RACK))
                if (n in BARBELL_ONLY_SET) return req(s(BARBELL_ONLY))
                return req(s(BARBELL_AND_FLOOR))
            }
            "Dumbbell" -> {
                if (n.contains("bulgarian")) return chairOrBenchWith(DUMBBELLS_ONLY)
                // AN INCLINE, A DECLINE OR ELBOW SUPPORT IS A BENCH, NOT "SOME FURNITURE".
                //
                // These used to return chairOrBenchWith(DUMBBELLS_ONLY), i.e. "a chair
                // OR a bench is enough", so a kitchen chair satisfied an incline press.
                // That is the owner's report on 2026-10-08: "I only have chair and dumbell
                // and yet the system asks me to do an inclinde dumbell press which doesnt
                // work without a bench". A chair cannot do what the exercise needs here -
                // it has no adjustable back angle (incline/decline) and no pad to brace
                // the elbow or the sternum against (chest-supported). A chair is still a
                // valid substitute where the furniture is just something to put a hand or
                // a foot on (bench dips, incline/decline PUSH-UPS, step-ups); those go
                // through the Bodyweight branch and are untouched.
                //
                // Locked by BenchMovementGateTest, which sweeps the movement names.
                if (n in DUMBBELL_BENCH_SET || gateName.contains("bench") ||
                    gateName.contains("incline") || gateName.contains("decline") ||
                    gateName.contains("chest-supported")
                ) return req(s(BENCH, DUMBBELLS_ONLY))
                if (n in DUMBBELL_FLOOR_SET || gateName.contains("floor") ||
                    gateName.contains("pullover"))
                    return req(s(DUMBBELLS_AND_FLOOR))
                return req(s(DUMBBELLS_ONLY))
            }
            "Kettlebell" -> {
                // Ask for the TWO tiles, not the retired composite: no tile's
                // tick grants `kettlebell_and_bench`, so a user who ticked
                // "Kettlebell" and "Bench" - the honest expression - could
                // never reach this row. Same shape as the dumbbell bench rows.
                if (n in KETTLEBELL_BENCH_SET) return req(s(BENCH, KETTLEBELL_ONLY))
                return req(s(KETTLEBELL_ONLY))
            }
            "Bodyweight" -> {
                if (n.contains("ring")) return req(s(GYMNASTIC_RINGS))
                if (n.contains("trx")) return req(s(GYMNASTIC_RINGS))
                if (n.contains("wall")) return req(s(WALL))
                if (n.contains("handstand")) return req(s(WALL))
                if (n.contains("pull up") || n.contains("pull-up") || n.contains("chin") ||
                    n.contains("hang") || n.contains("muscle up")
                ) return req(s(PULL_UP_BAR))
                if (n.contains("dip")) return req(s(DIP_BARS))
                // A decline movement puts the torso on an angle, which is a piece
                // of furniture (a bench, or a chair under the feet for the feet-
                // elevated variant). "Decline Crunch" fell through to the empty
                // requirement and so was offered to a user with NO furniture at
                // all - the same over-display class as the chair-as-bench leak,
                // in the one branch that needs no equipment to reach.
                if (n.contains("bench") || n.contains("chair") || n.contains("step") ||
                    n.contains("box") || n.contains("decline")
                ) return req(s(CHAIR), s(BENCH), s(STAIRS))
                return req(emptySet())
            }
            "Other" -> {
                if (n in SMITH_SET) return req(s(SMITH_MACHINE))
                // Suspension work and wall-supported moves keep their home tags
                // even though the catalog files them under "Other".
                if (n.contains("trx")) return req(s(GYMNASTIC_RINGS))
                if (n.contains("ring")) return req(s(GYMNASTIC_RINGS))
                if (n.contains("wall")) return req(s(WALL))
                // Gear-specific "Other" items: cardio machines, sled/rope/plate
                // work, battle ropes, the wrist roller, medicine ball slams.
                if (n.contains("bike") || n.contains("treadmill") || n.contains("rowing") ||
                    n.contains("elliptical") || n.contains("stair climber") ||
                    n.contains("sled") || n.contains("rope climb") || n.contains("battle rope") ||
                    n.contains("medicine ball") || n.contains("wrist roller") ||
                    n.contains("plate") || n.contains("svend")
                ) return req(s(MACHINE_OTHER))
                // A movement that NAMES furniture needs it, even when it is a
                // stretch or a pose: the "Bench …" stretch family (chest, lat,
                // calf, adductor, glute, couch, figure-4) is catalogued under
                // "Other" and fell through to the empty requirement, so a user
                // who ticked no furniture at all was still offered nine
                // stretches that name a bench. The gear check above MUST come
                // first - "Stair Climber" names a machine, not stairs.
                if (n.contains("bench") || n.contains("chair") || n.contains("box") ||
                    n.contains("step")
                ) return req(s(CHAIR), s(BENCH), s(STAIRS))
                return req(emptySet())
            }
        }
        return req(emptySet())
    }

    /** True when a user holding [granted] tags can perform [name]. */
    fun isAvailable(name: String, granted: Set<String>): Boolean =
        requirementsFor(name).any { granted.containsAll(it) }

    // ---------- SELECTION NORMALIZATION ----------

    /**
     * Reduces a persisted selection (preset ids, fine tags, legacy categories)
     * to the coarse catalog categories it covers — the vocabulary the pickers'
     * equipment chips and [WorkoutTemplates.EQUIPMENT_*] constants speak.
     *
     * A no-barbell user's saved tags reduce to a list WITHOUT "Barbell", which
     * is exactly what a recommendation filter should start from. Unrestricted
     * selections (empty, or "All") reduce to an empty list meaning "no
     * preference". Order follows [ExerciseCatalog.CATEGORIES].
     */
    fun coarseCategoriesFor(equipment: List<String>): List<String> {
        val granted = allowedTagsFor(equipment)
        // An empty granted set is the strict bodyweight preset, not "no
        // preference" — surface it as the Bodyweight category.
        if (granted.isEmpty()) return listOf(WorkoutTemplates.EQUIPMENT_BODYWEIGHT)
        // "Full gym" (or any grant that covers every tag) means unrestricted.
        if (granted.containsAll(ALL_TAGS.toSet())) return emptyList()

        val has: (Set<String>) -> Boolean = { family -> granted.any { it in family } }
        val categories = mutableListOf<String>()
        if (has(setOf(PULL_UP_BAR, DIP_BARS, GYMNASTIC_RINGS, CHAIR, BENCH, WALL, STAIRS, BACKPACK_OR_WEIGHTED_BAG, TOWEL)))
            categories.add(WorkoutTemplates.EQUIPMENT_BODYWEIGHT)
        if (has(setOf(BARBELL_AND_RACK, BARBELL_AND_FLOOR, BARBELL_ONLY)))
            categories.add(WorkoutTemplates.EQUIPMENT_BARBELL)
        if (has(setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_BENCH, DUMBBELLS_AND_FLOOR)))
            categories.add(WorkoutTemplates.EQUIPMENT_DUMBBELL)
        if (has(setOf(KETTLEBELL_ONLY, KETTLEBELL_AND_BENCH)))
            categories.add(WorkoutTemplates.EQUIPMENT_KETTLEBELL)
        if (CABLE_MACHINE in granted)
            categories.add(WorkoutTemplates.EQUIPMENT_CABLE)
        if (has(setOf(
                LEG_PRESS_MACHINE, LAT_PULLDOWN_MACHINE, SMITH_MACHINE,
                LEG_EXTENSION_MACHINE, LEG_CURL_MACHINE, CHEST_PRESS_MACHINE,
                SEATED_ROW_MACHINE, MACHINE_OTHER,
            )))
            categories.add(WorkoutTemplates.EQUIPMENT_MACHINE)
        if (RESISTANCE_BANDS in granted)
            categories.add(WorkoutTemplates.EQUIPMENT_BANDS)
        return categories
    }

    /**
     * The name-based lookup sets, exposed so a test can assert they stay
     * truthful.
     *
     * Two properties are load-bearing and neither is visible from the outside:
     * every entry must name an exercise that still EXISTS, and every entry must
     * be lower case. The second one is not a style preference - `requirementsFor`
     * tests membership with `n in SOME_SET` where `n` is the lowercased name, and
     * `in` on a Set is case-sensitive, so one capital letter makes the lookup miss
     * and the exercise silently loses its gate.
     */
    internal fun nameLookupSets(): Map<String, Set<String>> = mapOf(
        "SMITH_SET" to SMITH_SET,
        "LEG_PRESS_SET" to LEG_PRESS_SET,
        "LEG_CURL_SET" to LEG_CURL_SET,
        "LEG_EXTENSION_SET" to LEG_EXTENSION_SET,
        "CHEST_PRESS_SET" to CHEST_PRESS_SET,
        "SEATED_ROW_SET" to SEATED_ROW_SET,
        "MACHINE_OTHER_SET" to MACHINE_OTHER_SET,
        "DUMBBELL_BENCH_SET" to DUMBBELL_BENCH_SET,
        "DUMBBELL_FLOOR_SET" to DUMBBELL_FLOOR_SET,
        "BARBELL_ONLY_SET" to BARBELL_ONLY_SET,
        "KETTLEBELL_BENCH_SET" to KETTLEBELL_BENCH_SET,
    )

    /** Every tag the equipment tile grid can render, in grid order. */
    val SELECTABLE_TAGS: List<String> = HOME_EQUIPMENT_TAGS + GYM_EQUIPMENT_TAGS

    /**
     * Everything a tile stands for, tile included.
     *
     * The stored vocabulary is FINER than the tile grid: eight selectorized
     * stations sit behind the single "Machines" tile, and `barbell_only` behind
     * the "Barbell" tile. A full-gym kit persists fifteen tags of which the grid
     * renders six. The owner saw a bodyweight+dumbbell week containing a
     * Plate-Loaded Lateral Raise, opened the grid, and found nothing ticked that
     * explained it - and the tags survived every tap, because unticking removed
     * only the tag it could see. State the user cannot see is state the user
     * cannot remove.
     *
     * The families must NOT be folded together on the way IN, though: the eight
     * stations gate different exercises from MACHINE_OTHER, so collapsing them
     * into it would take the leg press and the Smith machine away. That is
     * exactly why they are separate tags. This is only about removal.
     *
     * NO TWO TILES MAY COVER THE SAME SET. They used to: Barbell and Rack both
     * mapped to {barbell_and_rack, barbell_and_floor, barbell_only}, and Chair
     * and Bench both mapped to {bench, chair, chair_or_bench}. Identical families
     * make two tiles the same control twice - both read as ticked from any tag in
     * the family, and unticking either cleared both. The owner reported "barbell
     * and squat rack are both selecting and deselecting at the same time, it has
     * some weird link".
     *
     * THE BARBELL PAIR IS NOW TWO INDEPENDENT SWITCHES, which is the owner's
     * second ruling on the same complaint (2026-10-08): "the squat rack and barbell
     * equipment icons are turning on and off together so the user cant select one
     * but not the other that needs to be fixed".
     *
     * Between the two rulings this pair was NESTED - Rack covered all three tags,
     * Barbell covered the two that need no rack - on the theory that a rack implies
     * a barbell. The theory is true about the hardware and false about the CONTROL:
     * `isChecked` reads a tile as ticked when ANY of its tags is present, so ticking
     * Barbell still lit Rack up, and the user still could not tick one without the
     * other. A tile is a question ("do you have this?"), not a set of implications,
     * so each barbell tile now owns exactly its own tag and nothing else:
     *
     *  - a rack owner who also does floor presses ticks BOTH tiles, which is honest -
     *    they are two pieces of kit;
     *  - and the two tags still gate different rows, so nothing becomes unreachable:
     *    floor movements come from the Barbell tile, rack movements from the Rack one.
     */
    fun tagsCoveredByTile(tile: String): Set<String> = when (tile) {
        MACHINE_OTHER -> setOf(
            MACHINE_OTHER, LEG_PRESS_MACHINE, LAT_PULLDOWN_MACHINE, SMITH_MACHINE,
            LEG_EXTENSION_MACHINE, LEG_CURL_MACHINE, CHEST_PRESS_MACHINE, SEATED_ROW_MACHINE,
        )
        // The rack tile stands for a rack, and BARBELL_ONLY - which has no tile of its
        // own - belongs to the Barbell tile beside it, because a barbell with neither
        // rack nor floor space is still the barbell the user ticked.
        BARBELL_AND_RACK -> setOf(BARBELL_AND_RACK)
        BARBELL_AND_FLOOR -> setOf(BARBELL_AND_FLOOR, BARBELL_ONLY)
        // A chair and a bench are two separate pieces of furniture, and a user
        // may own one without the other. CHAIR_OR_BENCH is the retired union;
        // [allowedTagsFor] expands it into the two, so it is kept here only so a
        // raw persisted value is still removable.
        CHAIR -> setOf(CHAIR, CHAIR_OR_BENCH)
        BENCH -> setOf(BENCH, CHAIR_OR_BENCH)
        KETTLEBELL_ONLY -> setOf(KETTLEBELL_ONLY, KETTLEBELL_AND_BENCH)
        DUMBBELLS_ONLY -> setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_FLOOR, DUMBBELLS_AND_BENCH)
        else -> setOf(tile)
    }

    /**
     * What ticking a tile GRANTS, which is a different question from what
     * unticking it removes.
     *
     * Unticking removes the whole [tagsCoveredByTile] family, because a tag with
     * no tile of its own - the eight stations behind "Machines" - would otherwise
     * be impossible to remove. Ticking must NOT do the same: ticking "Machines"
     * must not grant a leg press the user never ticked.
     *
     * There is NO wildcard left at all. A tick used to grant the whole rack family
     * so that "I own a rack" also opened the floor movements - which reads well on
     * its own, and is what kept the rack and barbell tiles lighting each other up:
     * the extra tags are read straight back by `isChecked` on the neighbouring tile.
     * The owner's ruling is that the two must be selectable separately, and the only
     * way to mean that is to stop hiding one tag behind the other's tick.
     */
    fun tagsGrantedByTicking(tile: String): Set<String> = when (tile) {
        BARBELL_AND_RACK -> setOf(BARBELL_AND_RACK)
        // The Barbell tile grants the two tags it COVERS, so a barbell with no rack
        // still reaches the barbell-only movements (curls, strict press, floor
        // rows). Granting only the tile's own tag would leave `barbell_only`
        // reachable from the Full-gym preset alone - the same "the tile is a lie"
        // shape as the Machines tile below.
        BARBELL_AND_FLOOR -> setOf(BARBELL_AND_FLOOR, BARBELL_ONLY)
        // A barbell and a rack are not the only tile that stands for a family.
        //
        // The gate asks for the SUB-VARIANT a movement needs, and three of those
        // are reachable with no extra kit, so the tile that names the implement
        // must grant them or the movement is invisible:
        //
        //  - floor presses and pullovers need `dumbbells_and_floor`, and a floor
        //    is not kit, so "Dumbbells" grants it.
        //  - the eight selectorized stations (leg press, Smith, pulldown, leg
        //    extension/curl, chest press, seated row) have NO tile of their own -
        //    they sit behind "Machines". Granting only `machine_other` made the
        //    tile a lie: a user who ticked Machines got the battle ropes and the
        //    ab wheel but could reach NONE of the ~45 rows the stations gate, and
        //    had no way to say "I have a leg press" short of the Full-gym preset.
        //    Tick and untick now stand for the same family, so removal still
        //    works - which was the reason the grant was narrowed in the first
        //    place; the narrowing fixed removal by breaking display.
        DUMBBELLS_ONLY -> setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_FLOOR)
        MACHINE_OTHER -> tagsCoveredByTile(MACHINE_OTHER)
        else -> setOf(tile)
    }

    /**
     * Normalizes whatever the persistence layer holds — fine tags, preset ids,
     * legacy categories, or "All" — into the granted tag set. An empty input
     * list means "unrestricted" and grants everything (the legacy no-filter
     * value); use [PRESET_BODYWEIGHT] to grant deliberately nothing.
     */
    fun allowedTagsFor(equipment: List<String>): Set<String> {
        val items = equipment.map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty() || items.any { it.equals(WorkoutTemplates.EQUIPMENT_ALL, ignoreCase = true) }) {
            return ALL_TAGS.toSet()
        }
        val granted = mutableSetOf<String>()
        items.forEach { item ->
            when {
                isPreset(item) -> granted += presetTags(item)
                // The pre-split selection ("chair or bench") is one tile's worth
                // of intent, so it grants the two tiles that replaced it.
                item == CHAIR_OR_BENCH -> granted += setOf(CHAIR, BENCH)
                // A selection persisted under the retired "Dumbbell + Bench"
                // tile meant dumbbells plus a bench; grant the two tiles that
                // replaced it, or the user would silently keep a grant they
                // can no longer see or untick.
                item == DUMBBELLS_AND_BENCH ->
                    granted += setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_FLOOR, BENCH)
                item in ALL_TAGS -> granted += item
                // Legacy catalog categories stay permissive (category-wide) so
                // previously persisted selections keep their old reach.
                item.equals(WorkoutTemplates.EQUIPMENT_BODYWEIGHT, ignoreCase = true) -> Unit
                item.equals(WorkoutTemplates.EQUIPMENT_DUMBBELL, ignoreCase = true) ->
                    granted += setOf(DUMBBELLS_ONLY, DUMBBELLS_AND_BENCH, DUMBBELLS_AND_FLOOR)
                item.equals(WorkoutTemplates.EQUIPMENT_BARBELL, ignoreCase = true) ->
                    granted += setOf(BARBELL_AND_RACK, BARBELL_AND_FLOOR, BARBELL_ONLY)
                item.equals(WorkoutTemplates.EQUIPMENT_KETTLEBELL, ignoreCase = true) ->
                    granted += setOf(KETTLEBELL_ONLY, KETTLEBELL_AND_BENCH)
                item.equals(WorkoutTemplates.EQUIPMENT_CABLE, ignoreCase = true) ->
                    granted += CABLE_MACHINE
                item.equals(WorkoutTemplates.EQUIPMENT_MACHINE, ignoreCase = true) ->
                    granted += setOf(
                        LEG_PRESS_MACHINE, LAT_PULLDOWN_MACHINE, SMITH_MACHINE,
                        LEG_EXTENSION_MACHINE, LEG_CURL_MACHINE, CHEST_PRESS_MACHINE,
                        SEATED_ROW_MACHINE, MACHINE_OTHER, DIP_BARS,
                    )
                item.equals(WorkoutTemplates.EQUIPMENT_BANDS, ignoreCase = true) ->
                    granted += RESISTANCE_BANDS
                item.equals(WorkoutTemplates.EQUIPMENT_OTHER, ignoreCase = true) ->
                    granted += MACHINE_OTHER
            }
        }
        return granted
    }

    /**
     * Maps a persisted pre-tag-era selection to the new model: pure-legacy
     * sets become the matching preset, anything else becomes its fine tags
     * (the custom path). Idempotent on already-migrated values.
     */
    fun migrateLegacy(selected: List<String>): List<String> {
        val items = selected.map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty()) return emptyList()
        if (items.size == 1) {
            val one = items[0]
            if (isPreset(one) || one in ALL_TAGS || one == CHAIR_OR_BENCH) return items
            return when {
                one.equals(WorkoutTemplates.EQUIPMENT_ALL, ignoreCase = true) -> listOf(PRESET_FULL_GYM)
                one.equals(WorkoutTemplates.EQUIPMENT_BODYWEIGHT, ignoreCase = true) -> listOf(PRESET_BODYWEIGHT)
                one.equals(WorkoutTemplates.EQUIPMENT_DUMBBELL, ignoreCase = true) -> listOf(PRESET_DUMBBELLS)
                one.equals(WorkoutTemplates.EQUIPMENT_BARBELL, ignoreCase = true) -> listOf(PRESET_BARBELL_RACK)
                one.equals(WorkoutTemplates.EQUIPMENT_KETTLEBELL, ignoreCase = true) ->
                    listOf(PRESET_CUSTOM) + KETTLEBELL_ONLY + KETTLEBELL_AND_BENCH
                one.equals(WorkoutTemplates.EQUIPMENT_CABLE, ignoreCase = true) ->
                    listOf(PRESET_CUSTOM) + CABLE_MACHINE
                one.equals(WorkoutTemplates.EQUIPMENT_MACHINE, ignoreCase = true) ->
                    listOf(PRESET_CUSTOM) + MACHINE_OTHER
                one.equals(WorkoutTemplates.EQUIPMENT_BANDS, ignoreCase = true) -> listOf(PRESET_BW_BANDS)
                else -> listOf(PRESET_CUSTOM) + one
            }
        }
        // Multi-legacy selections ("Dumbbell" + "Bands") become their tags.
        val tags = allowedTagsFor(items)
        return listOf(PRESET_CUSTOM) + tags
    }
}
