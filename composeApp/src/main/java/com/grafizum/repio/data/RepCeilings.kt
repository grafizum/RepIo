package com.grafizum.repio.data

/**
 * The highest honest rep count for each of the 121 bodyweight REP ladders in
 * the pool, authored per exercise (116 as of 2026-10-04, plus the last seven
 * manual-review rows the owner asked to be fixed on 2026-10-06).
 *
 * The two rows whose work is counted in STEPS - Heel-to-Toe Walk and Bear Crawl
 * - are deliberately NOT here: their ladder is a step count, not a rep count,
 * and it is authored in [ProgressionEngine]'s STEP_LADDERS so one exercise
 * cannot carry two different maxima.
 *
 * ## Why this is a table and not a rule
 *
 * The 123 rows with `unit == "variation_step"` all declare `repMin = 8` and a
 * `repMax` of only 12 or 15, so the CSV carries no max-effort figure for any of
 * them. A flat cap was standing in for that number - 25 for everything, 50 for
 * core, read off the row's muscle - and it was wrong at BOTH ends. It sent a
 * glute bridge to 25 reps (ordinary programming is 30-50) and a handstand
 * push-up to the same 25 (the movement itself ends near 12).
 *
 * What separates these rows is leverage and whether the movement is a skill or
 * a stamina piece of work, and that IS in the name - which is what the owner
 * authorised reading for this table on 2026-10-04. So the numbers are
 * **authored, not derived**: each one is a hand-set value with a stated basis,
 * in an explicit per-exercise map. Nothing here parses a string at runtime, so
 * a rename cannot silently re-tier an exercise - and because the key is the
 * pool **slug**, the 220 cumulative renames in [ExerciseCatalog.RENAMED] cannot
 * orphan a row either.
 *
 * The bands are the ones [ProgressionEngine] already documents: 8-12 is the
 * hypertrophy window (Schoenfeld et al. 2021), 15-25 is ACSM's local muscular
 * endurance band, and 50 is where work with no load left to raise simply ends.
 * Nothing goes below 12, because 12 is the first
 * [ProgressionEngine.REP_CHECKPOINTS] rung and a ladder that stopped below it
 * could never offer a progression.
 *
 * **Every rep-ladder row in the pool is present.** `RepCeilingCoverageTest`
 * fails the build if one is missing (a silent fallback to the flat cap is the
 * defect this table exists to remove) or if a slug here is not a real pool row.
 */
internal object RepCeilings {

    private val CEILINGS: Map<String, Int> = mapOf(
    // --- Abs (30) ---
    // Trunk work splits by how much leverage the movement puts on the spine. A slow low-load anti-extension hold (Dead Bug) or a plain Crunch is ordinary 40-50 rep programming; the same muscle under a hanging lever or a held V position runs out at 12-15.
    "crunches" to 50,   // Crunches
    "dead_bug" to 50,   // Dead Bug
    "bird_dog" to 40,   // Bird-Dog
    "reverse_crunches" to 40,   // Reverse Crunches
    "bicycle_crunch" to 30,   // Bicycle Crunch
    "decline_crunch" to 30,   // Decline Crunch
    "flutter_kicks" to 30,   // Flutter Kicks
    "scissor_kicks" to 30,   // Scissor Kicks
    "sit_ups" to 30,   // Sit-Ups
    "captain_s_chair_knee_raise" to 25,   // Captain's Chair Knee Raise
    "stability_ball_knee_tuck" to 25,   // Knee Tuck (Ball)
    "supine_windshield_wipers" to 25,   // Supine Windshield Wipers
    "bench_leg_pull_in" to 20,   // Bench Leg Pull-In
    "captain_s_chair_leg_raise" to 20,   // Captain's Chair Leg Raise
    "downward_dog_knee_tuck" to 20,   // Downward Dog Knee Tuck
    "downward_dog_to_knee_drive" to 20,   // Downward Dog to Knee Drive
    "hanging_knee_raise" to 20,   // Hanging Knee Raise
    "jackknife_sit_up" to 20,   // Jackknife Sit-Up
    "lying_leg_raise" to 20,   // Lying Leg Raise
    "pilates_leg_pull_front" to 20,   // Pilates Leg Pull Front
    "ball_pike" to 15,   // Ball Pike
    "cocoons" to 15,   // Cocoons
    "downward_dog_to_plank" to 15,   // Downward Dog to Plank
    "hanging_leg_raise" to 15,   // Hanging Leg Raise
    "hanging_pike" to 15,   // Hanging Pike
    "pilates_roll_over" to 15,   // Pilates Roll Over
    "toes_to_bar" to 15,   // Toes to Bar
    "v_ups" to 15,   // V Ups
    "dragon_flag" to 12,   // Dragon Flag
    "v_sit" to 12,   // V-Sit

    // --- Pectorals (19) ---
    // A push ladder is decided by leverage, not by the chest. A wall incline is a 40 rep movement; a pseudo-planche push-up is a skill almost nobody passes 12 of. The middle of the family - a strict Push-Up - stops at 25, which is already an elite bodyweight number.
    "wall_push_ups" to 40,   // Wall Push Ups
    "incline_push_up" to 35,   // Incline Push-Up
    "knee_push_ups" to 30,   // Knee Push Ups
    "stability_ball_push_up" to 30,   // Stability Ball Push-Up
    "trx_chest_press" to 30,   // TRX Chest Press
    "chest_dips" to 25,   // Chest Dips
    "push_up" to 25,   // Push-Up
    "stability_ball_push_up_hands_on_ball" to 25,   // Push-Up (Hands on Ball) (Ball)
    "ring_push_up" to 25,   // Ring Push-Up
    "straight_bar_dips" to 25,   // Straight Bar Dips
    "wide_grip_push_ups" to 25,   // Wide Grip Push Ups
    "close_grip_push_ups" to 20,   // Close Grip Push Ups
    "decline_push_up" to 20,   // Decline Push-Up
    // Diamond Push Ups: 20 was below what the movement is actually capable of, and the owner
    // said so in the plainest terms (2026-10-26, dashboard task_4s6x0u624o110j3l3y3z6g4q:
    // "Diamond pushups max rep is 20, how tf is this the max a human can do??! Makes 0 sense").
    // Basis: strengthlevel.com's own rep standards put the AVERAGE diamond set at 23 reps, so
    // 20 was a ceiling under the median; the Guinness record for diamond push-ups in one minute
    // is 122 (Hong Zhongtao, 2025), i.e. speed work, not a set. 40 is ~1.7x the average and a
    // third of what a record holder can do in the same time, which is where a top rung belongs.
    "diamond_push_ups" to 40,   // Diamond Push Ups
    "ring_dips" to 20,   // Ring Dips
    "archer_push_ups" to 15,   // Archer Push Ups
    "clap_push_ups" to 15,   // Clap Push-Ups
    "deficit_push_ups" to 15,   // Deficit Push Ups
    "pseudo_planche_push_ups" to 12,   // Pseudo Planche Push Ups

    // --- Lats (15) ---
    // Pulling is the most strength-limited of the families: a strict Pull-Up stops at 15 and anything past a muscle-up is a skill. The rows that only involve scapula or an inverted row are not that at all, so they top out in the 30s.
    "ring_row" to 30,   // Ring Row
    "rings_inverted_row" to 30,   // Rings Inverted Row
    "trx_row" to 30,   // TRX Row
    "chin_ups" to 20,   // Chin-Ups
    "neutral_grip_pull_ups" to 20,   // Neutral Grip Pull Ups
    "scapular_pull_ups" to 20,   // Scapular Pull Ups
    "wide_grip_pull_ups" to 20,   // Wide Grip Pull Ups
    "behind_the_neck_pull_up" to 15,   // Behind-the-Neck Pull-Up
    "close_grip_pull_ups" to 15,   // Close-Grip Pull-Ups
    "negative_pull_ups" to 15,   // Negative Pull Ups
    "pause_pull_up" to 15,   // Pause Pull-Up
    "pull_up" to 15,   // Pull-Up
    "archer_pull_ups" to 12,   // Archer Pull Ups
    "muscle_ups" to 12,   // Muscle Ups
    "ring_muscle_up" to 12,   // Ring Muscle-Up

    // --- Quadriceps (15) ---
    // Squats and lunges without load are stamina work in the 25-35 range until the movement becomes single-leg, and a pistol squat or a Cossack squat is a balance/strength skill at 15-20.
    "stability_ball_wall_squat" to 40,   // Wall Squat (Ball)
    "bodyweight_squat" to 35,   // Bodyweight Squat
    "trx_squat" to 35,   // TRX Squat
    "bodyweight_reverse_lunge" to 30,   // Bodyweight Reverse Lunge
    "box_squat" to 30,   // Box Squat
    "side_lunge" to 30,   // Side Lunge
    "step_ups" to 30,   // Step Ups
    "trx_lunge" to 30,   // TRX Lunge
    "lunge" to 25,   // Lunge
    "walking_lunge" to 25,   // Walking Lunge
    "pistol_squat" to 20,   // Pistol Squat
    "reverse_nordic_curl" to 20,   // Reverse Nordic Curl
    "split_squat" to 20,   // Split Squat
    "trx_pistol_squat" to 20,   // TRX Pistol Squat
    "cossack_squat" to 15,   // Cossack Squat

    // --- Glutes (10) ---
    // (Heel-to-Toe Walk is a STEPS ladder and lives in ProgressionEngine's STEP_LADDERS - a walked drill is counted in steps, not reps.)
    // The lightest family in the pool. A glute bridge or a clamshell is written 30-50 by every program that uses it, so these are the rows where the top checkpoint is genuinely reachable.
    "glute_bridge" to 50,   // Glute Bridge
    "clamshells" to 40,   // Clamshells
    "stability_ball_hip_bridge" to 40,   // Hip Bridge (Ball)
    "reverse_tabletop_hip_pulses" to 40,   // Reverse Tabletop Hip Pulses
    "side_lying_hip_abduction" to 40,   // Side-Lying Hip Abduction
    "single_leg_glute_bridge" to 40,   // Single Leg Glute Bridge
    "glute_kickback" to 30,   // Glute Kickback
    "pilates_kneeling_side_kick" to 30,   // Pilates Kneeling Side Kick
    "pilates_side_bend" to 30,   // Pilates Side Bend
    "side_plank_with_leg_lift" to 30,   // Side Plank with Leg Lift

    // --- Front Deltoids (4) ---
    // A press ladder under body weight. Handstand push-ups are the hardest pressing movement a person can do unaided and stop at 12; a pike press is a normal 20.
    // (Bear Crawl is a STEPS ladder and lives in ProgressionEngine's STEP_LADDERS - its work is counted in steps, not reps.)
    "bodyweight_overhead_press" to 20,   // Bodyweight Overhead Press
    "pike_push_ups" to 20,   // Pike Push Ups
    "downward_dog_to_upward_dog" to 15,   // Downward Dog to Upward Dog
    "handstand_push_ups" to 12,   // Handstand Push Ups

    // --- Triceps (4) ---
    // Dips are the only loaded-looking row here, and they sit in the ordinary 25-30 band; the TRX and plank-supported variants trade load for difficulty and stay with it.
    "bench_dips" to 30,   // Bench Dips
    "trx_triceps_extension" to 30,   // TRX Triceps Extension
    "reverse_plank_dips" to 25,   // Reverse Plank Dips
    "crab_dips" to 20,   // Crab Dips

    // --- Spinal Erectors (3) ---
    // Small, low-load extension work. Superman is a 30 rep movement; the Pilates variants add leverage and stop lower.
    "superman" to 30,   // Superman
    "back_extension" to 25,   // Back Extension
    "pilates_leg_pull_back" to 20,   // Pilates Leg Pull Back

    // --- Hamstrings (3) ---
    // Knee-flexion work without load. The eccentric Nordic variant is the hardest of the three and stops lowest.
    "bodyweight_good_morning" to 25,   // Bodyweight Good Morning
    "stability_ball_leg_curl" to 25,   // Leg Curl (Ball)
    "trx_hamstring_curl" to 25,   // TRX Hamstring Curl

    // --- Calves (3) ---
    // Standing calf work is famously a high-rep exercise, and a body-weight raise is not near a failure ceiling at 25. Donkey calf adds a deep range, single-leg halves the load.
    "bodyweight_calf_raise" to 50,   // Bodyweight Calf Raise
    "donkey_calf_raise" to 40,   // Donkey Calf Raise
    "single_leg_calf_raise" to 30,   // Single Leg Calf Raise

    // --- Rear Deltoids (3) ---
    // Light cable-substitute isolation. A face pull or a Y-fly is written 15-30 and fails on shoulder tolerance, not on the rep.
    "trx_face_pull" to 30,   // TRX Face Pull
    "trx_y_fly" to 30,   // TRX Y-Fly
    "ring_face_pull" to 25,   // Ring Face Pull

    // --- Obliques (2) ---
    // Rotational trunk work. High reps are ordinary, but the long lever of a Russian twist ends it below a straight crunch.
    "cross_body_crunch" to 40,   // Cross-Body Crunch
    "russian_twist" to 30,   // Russian Twist

    // --- Lateral Deltoids (1) ---
    // A hand-weight stand-in for a lateral raise: the smallest muscle in the pool, high reps, and the ceiling is the shoulder, not the deltoid.
    "bodyweight_lateral_raise" to 30,   // Bodyweight Lateral Raise

    // --- Hip Adductors (1) ---
    // The lightest single-joint movement in the pool, written 30-50 everywhere.
    "side_lying_hip_adduction" to 40,   // Side Lying Hip Adduction

    // --- Biceps (1) ---
    // One light isolation row under TRX. The ceiling is the grip, not the elbow.
    "trx_bicep_curl" to 30,   // TRX Bicep Curl

    // --- The last seven manual-review rows (owner, 2026-10-06) ---
    // Plyometrics, the Nordic, the rope climb and the wrist roller were the
    // only rows the CSV refused to ladder; each now counts its own reps -
    // ground contacts, slow eccentrics, climbs, revolutions - with the
    // implement's height, tempo or load left to the user. All seven open at
    // 2-3 and top out at 12: the band's floor is the first REP_CHECKPOINTS
    // rung, so a ladder that stopped below it could never offer the harder
    // movement it feeds - and 12 in one set is each movement's mastery figure,
    // not its start.
    "box_jump" to 12,   // Box Jump
    "jump_squat" to 12,   // Jump Squat
    "plyo_lunge" to 12,   // Plyo Lunge
    "plyo_push_up" to 12,   // Plyo Push-Up
    "nordic_hamstring_curl" to 12,   // Nordic Hamstring Curl
    "rope_climb" to 12,   // Rope Climb
    "wrist_roller" to 12,   // Wrist Roller
    )

    /** Every slug in this table - the coverage test reads this. */
    val slugs: Set<String> get() = CEILINGS.keys

    /**
     * The authored ceiling for [slug], or null when the row is not in the table
     * (a new pool exercise, or a user's own custom exercise). The caller
     * decides the fallback; this table does not guess one.
     */
    fun forSlug(slug: String): Int? = CEILINGS[slug]
}
