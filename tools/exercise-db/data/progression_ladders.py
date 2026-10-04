"""Published progression ladders, from which branches are DERIVED.

WHY LADDERS INSTEAD OF 470 HAND-WRITTEN BRANCHES
-------------------------------------------------
The owner asked (2026-10-02) for a progression on every exercise we hold, and
specifically that it be something "known from professionals ... used everywhere,
not something new this app has". Hand-writing four hundred independent branch
dicts is exactly how you end up with four hundred invented chains that happen to
read plausibly, so this file does the opposite: it writes down the ORDERINGS that
already exist, and `derive_branches_from_ladders()` turns each ordering into
branches.

A ladder is a list of pool SLUGS from easiest to hardest. For a ladder
`[a, b, c]` the derivation produces, for each rung:

    entry = <name of this rung>
    nodes = [ <name of the next rung up> ]        (empty for the top rung)

So every chain this file produces is literally "the next exercise up in the
ladder a coach would already name", which is the requirement. Nothing is
invented: the only editorial act is the ORDER, and the order is the published
one.

THE ORDERINGS USED, AND WHERE THEY COME FROM
-------------------------------------------
* Calisthenics press: wall -> incline -> knee -> floor -> decline/deficit ->
  close-grip -> pseudo-planche -> archer -> planche. This is the skill order in
  every calisthenics text (the same one Behind-the-Boss / Overcoming
  Gravity-style progressions use), and it follows from one lever: the further the
  feet are from the hands, the more of the body weight the chest and shoulders
  carry.
* Calisthenics pull: band-assisted -> negative -> full -> close-grip -> archer
  -> muscle-up -> ring muscle-up -> weighted. The standard documented pull-up
  roadmap; the transition is "how much of the rep you can do unassisted", then
  "how much of it one arm does".
* Loaded squats and hinges: unweighted -> goblet/loaded -> barbell -> front/pause/
  overhead -> single-leg or deficit. Standard periodisation by loading and
  complexity, and the reason the level ladder cannot replace it: a heavier squat
  and a harder squat are different exercises.
* Isolation work: the elbow is made harder to move - narrower grip, longer
  lever, slower tempo, one arm, unstable implement. This is the "making the
  isolation harder without adding much load" principle used in every hypertrophy
  text (B hypertrophy / D strength, Iversen's narrative review PMC8449772,
  "Resistance-Training Recommendations to Maximize Muscle Hypertrophy in an
  Athletic Population").
* Kettlebell: the swing's standard hard-point is the clean/press/jerk that
  follows it, and the double-bell single-arm ladder is the usual way a one-arm
  swing is progressed.

WHAT A LADDER DOES NOT CLAIM
----------------------------
A ladder is NOT a claim that you must do these in order, and it does not claim
rep ranges or weekly frequencies. It says only: given this exercise, the exercise
a coach would name as the harder sibling of the same movement is THIS one. The
post-workout popup presents it as a choice, not a prescription.
"""

# ---------------------------------------------------------------------------
# Helper aliases are NOT used: every entry below is a literal pool slug so the
# build can validate it against catalog_v5.json and fail loudly on a typo.
# ---------------------------------------------------------------------------

LADDERS: dict[str, list[str]] = {}

# How many steps beyond the destination a branch advertises. See _derive below.
MAX_NODES = 3


def ladder(name: str, rungs: list[str]) -> None:
    """Register a ladder. `rungs` must be pool slugs, easiest first."""
    LADDERS[name] = rungs


# ===========================================================================
# PULL - vertical (calisthenics pull-up roadmap)
# ===========================================================================
ladder(
    "pull_up",
    [
        "band_assisted_pull_ups",
        "negative_pull_ups",
        "pull_up",
        "close_grip_pull_ups",
        "archer_pull_ups",
        "muscle_ups",
        "ring_muscle_up",
        "weighted_pull_up",
    ],
)

# ===========================================================================
# PULL - horizontal (the row)
# ===========================================================================
ladder(
    "row_supported_to_single_arm",
    [
        "trx_row",
        "rings_inverted_row",
        "ring_row",
        "chest_supported_dumbbell_row",
        "bent_over_dumbbell_row",
        "single_arm_dumbbell_row",
        "bent_over_barbell_row",
        "t_bar_row",
        "one_arm_kettlebell_row",
        "double_kettlebell_row",
    ],
)

ladder(
    "pullover",
    [
        "bench_pull",
        "dumbbell_bench_pull",
        "ez_bar_pullover",
        "barbell_pullover",
        "floor_kettlebell_pullover",
        "bent_arm_ez_bar_pullover",
        "bent_arm_barbell_pullover",
        "plate_pullover",
    ],
)

# ===========================================================================
# PUSH - vertical
# ===========================================================================
ladder(
    "overhead_press",
    [
        "bodyweight_overhead_press",
        "dumbbell_shoulder_press",
        "barbell_overhead_press",
        "paused_overhead_press",
        "handstand_push_ups",
    ],
)

# ===========================================================================
# PUSH - horizontal (bench)
# ===========================================================================
ladder(
    "bench_press",
    [
        "dumbbell_floor_press",
        "dumbbell_bench_press",
        "ez_bar_bench_press",
        "barbell_bench_press",
        "paused_bench_press",
        "incline_barbell_bench_press",
    ],
)

# ===========================================================================
# SQUAT
# ===========================================================================
ladder(
    "squat",
    [
        "bodyweight_squat",
        "goblet_squat",
        "dumbbell_squat",
        "sumo_squat",
        "barbell_back_squat",
        "pause_squat",
        "front_squat",
        "overhead_squat",
        "heel_elevated_squat",
    ],
)

# ===========================================================================
# LUNGE
# ===========================================================================
ladder(
    "lunge_forward",
    [
        "lunge",
        "split_squat",
        "reverse_lunge",
        "dumbbell_lunge",
        "barbell_lunge",
        "walking_lunge",
        "bulgarian_split_squat",
        "kettlebell_bulgarian_split_squat",
    ],
)

# ===========================================================================
# HINGE - deadlift family
# ===========================================================================
ladder(
    "deadlift",
    [
        "kettlebell_deadlift",
        "dumbbell_deadlift",
        "barbell_deadlift",
        "stiff_leg_deadlift",
        "sumo_deadlift",
        "hex_bar_deadlift",
        "deficit_deadlift",
        "pause_deadlift",
    ],
)

ladder(
    "romanian_deadlift",
    [
        "bodyweight_good_morning",
        "dumbbell_romanian_deadlift",
        "ez_bar_romanian_deadlift",
        "romanian_deadlift",
        "barbell_deadlift",
    ],
)
# ===========================================================================
# GLUTE - bridge / hip thrust
# ===========================================================================
ladder(
    "hip_thrust",
    [
        "glute_bridge",
        "single_leg_glute_bridge",
        "barbell_glute_bridge",
        "banded_hip_thrust",
        "dumbbell_hip_thrust",
        "barbell_hip_thrust",
    ],
)

# ===========================================================================
# HAMSTRING - the curl
# ===========================================================================
ladder(
    "hamstring_curl",
    [
        "banded_standing_leg_curl",
        "trx_hamstring_curl",
        "stability_ball_leg_curl",
        "nordic_hamstring_curl",
        "reverse_nordic_curl",
    ],
)

# ===========================================================================
# QUAD - knee extension / the knee-dominant single leg
# ===========================================================================
ladder(
    "pistol_squat",
    [
        "split_squat",
        "trx_pistol_squat",
        "dumbbell_pistol_squat",
        "kettlebell_pistol_squat",
        "pistol_squat",
    ],
)

# ===========================================================================
# CALF
# ===========================================================================
ladder(
    "calf_raise",
    [
        "bodyweight_calf_raise",
        "dumbbell_calf_raise",
        "barbell_calf_raise",
        "single_leg_calf_raise",
        "donkey_calf_raise",
    ],
)

# ===========================================================================
# BICEPS
# ===========================================================================
ladder(
    "biceps_curl_long_lever",
    [
        "trx_bicep_curl",
        "concentration_curl",
        "strict_curl",
        "seated_dumbbell_curl",
        "incline_dumbbell_curl",
        "preacher_curl",
        "drag_curl",
        "cheat_curl",
    ],
)

ladder(
    "biceps_close_grip_bar",
    [
        "barbell_curl",
        "close_grip_barbell_curl",
        "ez_bar_curl",
        "close_grip_ez_bar_curl",
        "barbell_preacher_curl",
    ],
)

# ===========================================================================
# TRICEPS - the extension
# ===========================================================================
ladder(
    "triceps_extension",
    [
        "dumbbell_tricep_extension",
        "lying_tricep_extension",
        "overhead_tricep_extension",
        "single_arm_dumbbell_overhead_tricep_extension",
        "barbell_overhead_extension",
        "trx_triceps_extension",
    ],
)

# ===========================================================================
# FRONT DELT - the raise
# ===========================================================================
ladder(
    "front_raise",
    [
        "bodyweight_lateral_raise",
        "dumbbell_front_raise",
        "ez_bar_front_raise",
        "barbell_front_raise",
    ],
)

# ===========================================================================
# LATERAL DELT
# ===========================================================================
ladder(
    "lateral_raise",
    [
        "bodyweight_lateral_raise",
        "seated_dumbbell_lateral_raise",
        "side_lying_lateral_raise",
        "dumbbell_upright_row",
        "ez_bar_upright_row",
        "barbell_upright_row",
    ],
)

# ===========================================================================
# REAR DELT
# ===========================================================================
ladder(
    "rear_delt",
    [
        "band_pull_apart",
        "trx_y_fly",
        "dumbbell_reverse_fly",
        "rear_delt_fly",
        "dumbbell_face_pull",
        "trx_face_pull",
        "ring_face_pull",
    ],
)

# ===========================================================================
# UPPER TRAPS - the shrug
# ===========================================================================
ladder(
    "shrug",
    [
        "dumbbell_shrug",
        "kettlebell_shrug",
        "barbell_shrug",
        "ez_bar_shrug",
        "behind_the_back_barbell_shrug",
    ],
)

# ===========================================================================
# CORE - anti-extension, then flexion, then the hanging skills
# ===========================================================================
ladder(
    "core_anti_extension",
    [
        "dead_bug",
        "ab_wheel_rollout",
        "barbell_ab_rollout",
        "dragon_flag",
    ],
)

ladder(
    "core_flexion",
    [
        "sit_ups",
        "reverse_crunches",
        "lying_leg_raise",
        "hanging_knee_raise",
        "hanging_leg_raise",
        "jackknife_sit_up",
        "toes_to_bar",
    ],
)

# ===========================================================================
# FOREARMS - grip
# ===========================================================================
ladder(
    "grip",
    [
        "dead_hang",
        "ring_dead_hang",
        "dumbbell_farmer_s_walk",
        "kettlebell_farmer_s_walk",
        "suitcase_carry",
        "rope_climb",
    ],
)

ladder(
    "wrist_curl",
    [
        "dumbbell_wrist_curl",
        "kettlebell_wrist_curl",
        "barbell_wrist_curl",
        "ez_bar_wrist_curl",
    ],
)

# ===========================================================================
# SPINAL ERECTORS - the hip hinge for the back
# ===========================================================================
ladder(
    "spinal_erector",
    [
        "cat_cow",
        "superman",
        "back_extension",
        "back_lever",
        "good_morning",
    ],
)

# ===========================================================================
# OBLIQUES / ROTATION
# ===========================================================================
ladder(
    "rotation",
    [
        "russian_twist",
        "kettlebell_russian_twist",
        "cross_body_crunch",
        "standing_side_bend",
        "dumbbell_side_bend",
    ],
)

# ===========================================================================
# HIP ADDUCTOR
# ===========================================================================
ladder(
    "hip_adduction",
    [
        "side_lying_hip_adduction",
        "banded_standing_hip_adduction",
        "garland_pose",
        "kettlebell_windmills",
    ],
)

# ===========================================================================
# PLYO / POWER
# ===========================================================================
ladder(
    "plyo_lower",
    [
        "box_jump",
        "jump_squat",
        "plyo_lunge",
        "high_knees",
        "burpees",
    ],
)


# ===========================================================================
# DERIVATION
# ===========================================================================

def _fmt(names: list[str]) -> str:
    if not names:
        return "the top of the ladder"
    if len(names) == 1:
        return names[0]
    if len(names) == 2:
        return f"{names[0]} or {names[1]}"
    return ", ".join(names[:-1]) + " or " + names[-1]


def derive_branches_from_ladders(slug_to_name: dict[str, str]) -> dict[str, list[dict]]:
    """Turn every ladder into `ProgressionBranch`-shaped dicts.

    DIRECTION - READ THIS BEFORE CHANGING ANYTHING
    ---------------------------------------------
    A `ProgressionBranch` is keyed on the exercise the user is LEAVING and its
    `entry` is the exercise they move TO. ProgressionEngine.evolutionOffer looks
    up `sourceEntry.branches.firstOrNull { it.entry == targetName }`, so entry
    must be the DESTINATION, never the source. That is why the original table
    reads `barbell_bench_press -> entry "Incline Barbell Bench Press"`.

    So for a ladder `[a, b, c]` the derivation is:

        key a  ->  entry "b",  nodes ["c"]
        key b  ->  entry "c",  nodes []
        key c  ->  (no branch; nothing above it in this ladder)

    The TOP rung deliberately gets no entry in that ladder. `nodes` are the
    exercise's own future path (what it can become next), which is why `b`
    still gets a branch pointing at `c` while carrying `nodes ["c"]` - it is
    both a destination and a source, and the two roles are separate fields.

    `slug_to_name` maps a pool slug to its DISPLAY name, because branches carry
    names, not slugs (the Kotlin model resolves them by name).

    A rung that appears in two ladders contributes a branch from each; the merge
    in build_progression.py concatenates rather than overwrites, so e.g.
    `bodyweight_lateral_raise` is reachable as the bottom of both the front raise
    ladder and the lateral raise ladder, which is honest - it belongs to both.
    """
    out: dict[str, list[dict]] = {}

    def name_of(slug: str, ladder_name: str) -> str:
        if slug not in slug_to_name:
            # build_progression.py validates every slug before this point and
            # fails the build on an unknown one, so reaching here is a wiring bug.
            raise KeyError(
                f"ladder {ladder_name!r} references {slug!r}, which is not in the pool"
            )
        return slug_to_name[slug]

    for ladder_name, rungs in LADDERS.items():
        pretty = ladder_name.replace("_", " ")
        for i, slug in enumerate(rungs):
            if i + 1 >= len(rungs):
                continue  # top rung: nothing above it to move to
            entry = name_of(rungs[i + 1], ladder_name)
            # What the DESTINATION can itself become next. Capped at
            # MAX_NODES: the popup renders this as the target's own future path,
            # and a list of six names stops being a path and becomes a wall of
            # text. Three says "this goes somewhere" without listing the route.
            above = [name_of(s, ladder_name) for s in rungs[i + 2: i + 2 + MAX_NODES]]
            out.setdefault(slug, []).append(
                {
                    "entry": entry,
                    "nodes": above,
                    "note": (
                        f"The standard next step up the {pretty} ladder. "
                        f"{entry} keeps the same movement and asks more of it"
                        + (f", and from there {_fmt(above)}." if above else ".")
                    ),
                    "tag": "advanced",
                    "ladder": ladder_name,
                }
            )
    return out

# ===========================================================================
# BATCH 2 - the PARALLEL FAMILIES
#
# The batch above covers the main movement of each muscle. What was left over
# is mostly the sibling that sits beside it on the same bar: the decline twin of
# a flat press, the EZ-bar twin of a straight-bar press, the close-grip press
# for the triceps, the banded twin of a loaded lift. Those are exactly the
# progressions coaches already use ("when the flat stalls, go decline"), so they
# get their own ladders rather than being bolted onto the main one as noise.
# ===========================================================================

# Bench - the decline line. Raising the feet lengthens the stretch at the bottom
# and shifts the work to the lower chest; the standard next press once a flat
# bench press has stalled.
ladder(
    "decline_bench",
    [
        "decline_bench_press",
        "decline_ez_bar_bench_press",
        "decline_barbell_bench_press",
        "decline_dumbbell_fly",
    ],
)

# Bench - the EZ-bar / floor line. An EZ curve takes wrist stress off a close
# grip, which is the usual reason a press stalls before the strength does.
ladder(
    "ez_bar_press",
    [
        "floor_press",
        "floor_ez_bar_press",
        "ez_bar_bench_press",
        "incline_ez_bar_bench_press",
        "spoto_press",
    ],
)

# Chest - the triceps-taking press family, ordered by how narrow the grip gets.
ladder(
    "close_grip_press",
    [
        "close_grip_dumbbell_bench_press",
        "close_grip_ez_bar_bench_press",
        "close_grip_incline_bench_press",
        "wide_grip_bench_press",
    ],
)

# Chest - the fly / stretch line.
ladder(
    "fly",
    [
        "dumbbell_svend_press",
        "dumbbell_fly",
        "incline_dumbbell_fly",
        "decline_dumbbell_fly",
    ],
)

# Chest - the bodyweight press above the push-up floor.
ladder(
    "press_bodyweight_top",
    [
        "clap_push_ups",
        "wide_grip_push_ups",
        "planche",
    ],
)

# Dips - the triceps and chest versions, then the loaded and ring rungs.
ladder(
    "dip",
    [
        "bench_dips",
        "straight_bar_dips",
        "chest_dips",
        "weighted_dips",
        "ring_dips",
    ],
)

# Squat - the sumo's and the dumbbell's, both loading the same pattern a
# different way; the sumo's adducts, the dumbbell's corrects for the bars.
ladder(
    "squat_sumo_and_dumbbell",
    [
        "dumbbell_sumo_squat",
        "sumo_squat",
        "cossack_squat",
        "dumbbell_somersault_squat",
    ],
)

ladder(
    "squat_dumbbell",
    [
        "dumbbell_squat",
        "dumbbell_front_squat",
        "one_arm_kettlebell_front_squat",
    ],
)

# Lunge - the reverse direction and the banded twin.
ladder(
    "lunge_reverse",
    [
        "bodyweight_reverse_lunge",
        "reverse_lunge",
        "barbell_reverse_lunge",
        "kettlebell_reverse_lunge",
    ],
)

# Lunge - the single-leg knee-dominant line.
ladder(
    "lunge_single_leg",
    [
        "split_squat",
        "dumbbell_split_squat",
        "bulgarian_split_squat",
        "dumbbell_pistol_squat",
    ],
)

# Banded twins. A band is the standard way to take the load OUT of a lift for a
# beginner, so each banded row is the bottom rung of its loaded twin's ladder.
ladder(
    "banded_lower",
    [
        "banded_squat",
        "banded_glute_bridge",
        "banded_hip_thrust",
        "banded_romanian_deadlift",
    ],
)

ladder(
    "banded_pull",
    [
        "band_assisted_pull_ups",
        "weighted_pull_up",
    ],
)

# Kettlebell - the swing's standard hard point, and the single-arm line.
ladder(
    "kettlebell_swing",
    [
        "kettlebell_swing",
        "one_arm_kettlebell_swing",
        "kettlebell_swing_clean",
        "double_kettlebell_clean",
        "double_kettlebell_clean_and_press",
    ],
)

# Kettlebell - the clean and the pull that lead into the olympic lifts.
ladder(
    "kettlebell_clean",
    [
        "kettlebell_deadlift",
        "double_kettlebell_dead_clean",
        "hang_power_clean",
        "hang_clean",
        "clean",
    ],
)

# Kettlebell - overhead. The bottom-up press is the standard shoulder-delt
# progression before the rack position.
ladder(
    "kettlebell_overhead",
    [
        "one_arm_kettlebell_bottoms_up_press",
        "one_arm_kettlebell_shoulder_press",
        "double_kettlebell_overhead_press",
        "double_kettlebell_overhead_carry",
    ],
)

# The olympic second pull: dip, drive, catch overhead.
ladder(
    "olympic_second_pull",
    [
        "dumbbell_push_press",
        "one_arm_kettlebell_push_press",
        "double_kettlebell_push_press",
        "push_jerk",
        "double_kettlebell_jerk",
        "split_jerk",
        "double_kettlebell_split_jerk",
    ],
)

# Row - the EZ-bar and reverse-grip twins of the bent-over row.
ladder(
    "row_bent_over_bar",
    [
        "reverse_grip_bent_over_row",
        "bent_over_ez_bar_row",
        "ez_bar_reverse_grip_row",
        "barbell_rear_delt_row",
        "double_kettlebell_rear_delt_row",
    ],
)

# Row - the chest-supported and kettlebell twins.
ladder(
    "row_supported",
    [
        "chest_supported_dumbbell_row",
        "single_arm_chest_supported_dumbbell_row",
        "chest_supported_kettlebell_row",
    ],
)

# Pullover - the dumbbell and kettlebell twins of the bar pullover.
ladder(
    "pullover_light",
    [
        "kettlebell_pullover",
        "dumbbell_pullover",
        "barbell_pullover",
    ],
)

# Pull - the grip variants of the pull-up, all harder than the neutral grip.
ladder(
    "pull_up_grip",
    [
        "neutral_grip_pull_ups",
        "close_grip_pull_ups",
        "wide_grip_pull_ups",
        "behind_the_neck_pull_up",
    ],
)

# Pull - the scapular and pause variants that bracket the full rep.
ladder(
    "pull_up_control",
    [
        "scapular_pull_ups",
        "pause_pull_up",
        "weighted_pull_up",
    ],
)

# Core - the plank family and the L/V skills that bracket it.
ladder(
    "core_plank",
    [
        "high_plank",
        "trx_plank",
        "downward_dog_to_plank",
        "reverse_plank",
    ],
)

ladder(
    "core_l_sit",
    [
        "hanging_knee_raise",
        "l_sit",
        "v_sit",
        "hanging_pike",
    ],
)

# Core - the crunch variants ordered by how much of the torso they involve.
ladder(
    "core_crunch",
    [
        "bicycle_crunch",
        "decline_crunch",
        "weighted_wall_crunch",
        "bench_leg_pull_in",
    ],
)

# Biceps - the hammer line, where the arm stays neutral and the bicep cannot
# shorten as much, so the load is on the brachialis.
ladder(
    "biceps_hammer",
    [
        "single_arm_hammer_curl",
        "kettlebell_hammer_curl",
        "preacher_hammer_curl",
        "cross_body_hammer_curl",
        "incline_hammer_curl",
    ],
)

# Biceps - the spider / incline line, where the arm is behind the torso and the
# stretch does the work.
ladder(
    "biceps_spider",
    [
        "spider_curl",
        "ez_bar_spider_curl",
        "kettlebell_concentration_curl",
        "dumbbell_reverse_curl",
    ],
)

# Triceps - the skull crusher line, ordered by where the elbow ends up.
ladder(
    "triceps_skull_crusher",
    [
        "dumbbell_skull_crusher",
        "kettlebell_skull_crusher",
        "skull_crusher",
        "lying_tricep_extension",
        "ez_bar_lying_triceps_extension",
    ],
)

# Triceps - the overhead line, where the long head gets the full stretch.
ladder(
    "triceps_overhead",
    [
        "overhead_tricep_extension",
        "kettlebell_overhead_tricep_extension",
        "barbell_overhead_extension",
        "seated_dumbbell_tricep_extension",
        "ez_bar_overhead_tricep_extension",
    ],
)

# Triceps - the kickback, and the bodyweight triceps extensions.
ladder(
    "triceps_kickback",
    [
        "dumbbell_tricep_kickback",
        "one_arm_kettlebell_tricep_kickback",
        "crab_dips",
        "reverse_plank_dips",
    ],
)

# Hamstring - the single-leg RDL twins, which is the standard single-leg hinge.
ladder(
    "single_leg_hinge",
    [
        "dumbbell_kickstand_deadlift",
        "kettlebell_kickstand_deadlift",
        "single_leg_romanian_deadlift",
        "one_arm_single_leg_dumbbell_romanian_deadlift",
        "one_arm_single_leg_kettlebell_romanian_deadlift",
    ],
)

# Hamstring - the banded and dumbbell twins of the RDL.
ladder(
    "romanian_deadlift_light",
    [
        "banded_good_morning",
        "banded_romanian_deadlift",
        "dumbbell_romanian_deadlift",
        "ez_bar_romanian_deadlift",
        "romanian_deadlift",
    ],
)

# Hamstring - the dumbbell / kettlebell deadlifts below the barbell one.
ladder(
    "deadlift_light",
    [
        "kettlebell_deadlift",
        "kettlebell_single_leg_deadlift",
        "dumbbell_deadlift",
        "barbell_deadlift",
    ],
)

# Shoulders - the upright-row and windmill line for the lateral delts.
ladder(
    "upright_row",
    [
        "dumbbell_upright_row",
        "dumbbell_windmill",
        "ez_bar_upright_row",
        "barbell_upright_row",
    ],
)

# Shoulders - the overhead carry, where the load has to be held overhead.
ladder(
    "overhead_carry",
    [
        "dumbbell_overhead_carry",
        "double_dumbbell_overhead_carry",
        "kettlebell_overhead_carry",
        "double_kettlebell_overhead_carry",
        "kettlebell_turkish_get_ups",
    ],
)

# Shoulders - the press variants that angle or asymmetrically load the shoulder.
ladder(
    "shoulder_press_angle",
    [
        "dumbbell_shoulder_press",
        "incline_dumbbell_press",
        "landmine_press",
        "behind_the_neck_press",
    ],
)

# Shrug - the chest-supported and single-arm twins.
ladder(
    "shrug_supported",
    [
        "dumbbell_shrug",
        "chest_supported_dumbbell_shrug",
        "barbell_shrug",
    ],
)

# Traps - the neck isometric, which is the same hold applied to the neck.
ladder(
    "neck",
    [
        "chin_tuck_hold",
        "isometric_neck_lateral_flexion",
    ],
)

# Glute - the clamshell / abduction isolation line, banded first.
ladder(
    "hip_abduction",
    [
        "clamshells",
        "banded_clamshell",
        "side_lying_hip_abduction",
        "banded_lateral_walk",
    ],
)

# Glute - the bridge twins that are not on the hip-thrust ladder.
ladder(
    "glute_bridge_band",
    [
        "glute_bridge",
        "banded_glute_bridge",
        "barbell_glute_bridge",
    ],
)

# Glute - the kickback.
ladder(
    "glute_kickback",
    [
        "glute_kickback",
        "pilates_kneeling_side_kick",
        "side_plank_with_leg_lift",
    ],
)

# Forearms - the reverse wrist curl, the other half of the wrist ladder.
ladder(
    "wrist_reverse",
    [
        "dumbbell_reverse_wrist_curl",
        "kettlebell_reverse_wrist_curl",
        "ez_bar_wrist_curl",
    ],
)

# Calf - the loaded and seated variants.
ladder(
    "calf_raise_loaded",
    [
        "bodyweight_calf_raise",
        "dumbbell_calf_raise",
        "barbell_calf_raise",
    ],
)

# Oblique - the side plank, which is the anti-lateral-extension line.
ladder(
    "oblique_side_plank",
    [
        "side_plank",
        "trx_side_plank",
        "side_plank_with_leg_lift",
        "side_plank_leg_lift_hold",
    ],
)

# Quads - the terminal knee extension, the banded knee-dominant isolation.
ladder(
    "knee_extension_band",
    [
        "banded_terminal_knee_extension",
        "lunge",
        "bulgarian_split_squat",
    ],
)

# ===========================================================================
# BATCH 3 - THE LOADED TWINS THAT HAD NO RUNG YET
#
# Batch 2 ordered the main movements but left their dumbbell/kettlebell twins
# stranded, which is a real gap: a coach would absolutely say "do the kettlebell
# squat when the dumbbell one stalls". These put each implement on the ladder it
# belongs to.
# ===========================================================================

# Squat - the kettlebell sits between bodyweight and the barbell: the bell hangs
# where a bar would sit, so it is the loaded rung a beginner reaches first.
ladder(
    "squat_kettlebell",
    [
        "kettlebell_squat",
        "kettlebell_goblet_lunge",
        "front_squat",
        "overhead_squat",
    ],
)

# Squat - the assisted twins, which is where a beginner actually starts.
ladder(
    "squat_assisted",
    [
        "trx_squat",
        "wall_sit",
        "bodyweight_squat",
        "step_ups",
        "heel_elevated_squat",
    ],
)

# Lunge - the kettlebell and TRX twins.
ladder(
    "lunge_kettlebell",
    [
        "trx_lunge",
        "kettlebell_reverse_lunge",
        "kettlebell_lunge_press",
        "kettlebell_bulgarian_split_squat",
    ],
)

# Glute - the kettlebell twins of the bridge and the thrust.
ladder(
    "glute_kettlebell",
    [
        "stability_ball_hip_bridge",
        "glute_bridge",
        "kettlebell_hip_thrust",
        "barbell_hip_thrust",
    ],
)

# Glute - the banded abduction family, banded before loaded.
ladder(
    "hip_abduction_band",
    [
        "side_lying_hip_abduction",
        "banded_standing_hip_abduction",
        "banded_seated_hip_abduction",
    ],
)

# Glute - the sumo and lateral walks, which are the banded adduction line.
ladder(
    "glute_band_walk",
    [
        "banded_lateral_walk",
        "banded_sumo_walk",
        "banded_fire_hydrant",
        "banded_kneeling_hip_thrust",
    ],
)

# Chest - the kettlebell and single-bell floor presses.
ladder(
    "press_kettlebell",
    [
        "one_arm_kettlebell_floor_press",
        "kettlebell_floor_press",
        "kettlebell_close_grip_floor_press",
        "dumbbell_bench_press",
    ],
)

# Chest - the isometric press family: the Svend is the bodyweight twin.
ladder(
    "svend",
    [
        "svend_press",
        "single_dumbbell_svend_press",
        "dumbbell_svend_press",
        "kettlebell_svend_press",
    ],
)

# Chest - the rings and TRX presses, the assisted twins of the bar press.
ladder(
    "press_assisted",
    [
        "trx_chest_press",
        "ring_push_up",
        "push_up",
        "chest_dips",
    ],
)

# Press - the plyometric twin of the push-up.
ladder(
    "push_up_plyo",
    [
        "push_up",
        "plyo_push_up",
        "clap_push_ups",
        "planche",
    ],
)

# Press - the handstand line, ordered by how vertical the press is.
ladder(
    "handstand_press",
    [
        "pike_push_ups",
        "handstand_push_ups",
    ],
)

# Lats - the plate and single-arm pullover twins.
ladder(
    "pullover_plate",
    [
        "plate_pullover",
        "dumbbell_pullover",
        "ez_bar_pullover",
    ],
)

# Lats - the sled and TRX rows are the loaded twins of the bodyweight row.
ladder(
    "row_loaded",
    [
        "trx_row",
        "sled_row",
        "t_bar_row",
        "double_kettlebell_row",
        "chest_supported_kettlebell_row",
    ],
)

# Biceps - the double-bell and single-arm bell curls.
ladder(
    "biceps_kettlebell",
    [
        "one_arm_kettlebell_bicep_curl",
        "double_kettlebell_bicep_curl",
        "kettlebell_hammer_curl",
    ],
)

# Biceps - the straight-bar reverse curl, the bar twin of the dumbbell one.
ladder(
    "biceps_reverse_bar",
    [
        "dumbbell_reverse_curl",
        "reverse_curl",
        "ez_bar_reverse_curl",
    ],
)

# Biceps - the barbell drag and cheat curls sit above the strict curl.
ladder(
    "biceps_barbell_strict",
    [
        "strict_curl",
        "barbell_curl",
        "cheat_curl",
        "drag_curl",
    ],
)

# Triceps - the EZ-bar extensions and the bodyweight twist.
ladder(
    "triceps_ez",
    [
        "lying_tricep_extension",
        "ez_bar_lying_triceps_extension",
        "overhead_tricep_extension",
        "ez_bar_overhead_tricep_extension",
    ],
)

# Shoulders - the one-arm asymmetric press line.
ladder(
    "press_one_arm",
    [
        "seated_barbell_overhead_press",
        "dumbbell_shoulder_press",
        "one_arm_dumbbell_push_press",
        "one_arm_landmine_press",
    ],
)

# Shoulders - the kettlebell press and the double-bell clean-and-press.
ladder(
    "press_kettlebell_shoulder",
    [
        "one_arm_kettlebell_shoulder_press",
        "double_kettlebell_overhead_press",
        "double_kettlebell_clean_and_press",
        "clean_and_jerk",
    ],
)

# Shoulders - the thruster, the standard combination of the two presses.
ladder(
    "thruster",
    [
        "front_squat",
        "thruster",
        "clean_and_jerk",
    ],
)

# The snatch, from the hang to the floor and then the split version.
ladder(
    "snatch",
    [
        "hang_power_clean",
        "snatch",
        "double_kettlebell_dead_split_snatch",
    ],
)

# The kettlebell swing, ending in the snatch and the split versions.
ladder(
    "swing_to_snatch",
    [
        "one_arm_dumbbell_swing",
        "one_arm_kettlebell_swing",
        "double_kettlebell_swing_snatch",
    ],
)

# Hamstrings - the deadlift twins that still had no rung.
ladder(
    "deadlift_kickstand",
    [
        "dumbbell_kickstand_deadlift",
        "double_dumbbell_kickstand_deadlift",
        "kettlebell_kickstand_deadlift",
    ],
)

# Hamstrings - the sumo deadlift and its kettlebell twin.
ladder(
    "deadlift_sumo",
    [
        "kettlebell_sumo_deadlift",
        "sumo_deadlift",
        "hex_bar_deadlift",
    ],
)

# Hamstrings - the deadlift rack line: the pull from the floor, then the rack.
ladder(
    "deadlift_pull",
    [
        "barbell_deadlift",
        "rack_pull",
        "pause_deadlift",
        "good_morning",
    ],
)

# Back - the Jefferson curl is the loaded spinal-erector twin of the good morning.
ladder(
    "spinal_erector_loaded",
    [
        "good_morning",
        "jefferson_curl",
        "stiff_leg_deadlift",
    ],
)

# Back - the pilates spinal work, in the order it is taught.
ladder(
    "pilates_spine",
    [
        "cat_cow",
        "thoracic_bridge",
        "pilates_saw",
        "pilates_roll_down",
    ],
)

# Back - the lever skills, the straight-arm end of the erector line.
ladder(
    "lever",
    [
        "superman",
        "front_lever",
        "back_lever",
        "human_flag",
    ],
)

# Core - the anti-rotation family, which is the dead bug's harder cousin. The
# pool has no Pallof press, so this ladder stops at the bird-dog; the pool, not
# the idea, is what sets the ceiling.
ladder(
    "core_antirotation",
    [
        "dead_bug",
        "bird_dog",
    ],
)

# Core - the gymnastic line, from the tuck to the pike to the dragon flag.
ladder(
    "core_gymnastic",
    [
        "hanging_knee_raise",
        "hanging_leg_raise",
        "toes_to_bar",
        "dragon_flag",
        "ball_pike",
    ],
)

# Core - the dynamic leg work, which is the cardio-side of the abs.
ladder(
    "core_dynamic",
    [
        "flutter_kicks",
        "mountain_climbers",
        "scissor_kicks",
        "medicine_ball_slam",
    ],
)

# Core - the crunch and jackknife line, ordered by how far the torso comes up.
ladder(
    "core_jackknife",
    [
        "sit_ups",
        "jackknife_sit_up",
        "v_ups",
        "hanging_leg_raise",
        "toes_to_bar",
    ],
)

# Core - the supine and side-lying isolation line.
ladder(
    "core_supine",
    [
        "bench_leg_pull_in",
        "reverse_crunches",
        "supine_windshield_wipers",
        "pilates_roll_over",
    ],
)

# Obliques - the standing side bend, loaded and unloaded.
ladder(
    "oblique_side_bend",
    [
        "standing_side_bend",
        "dumbbell_side_bend",
        "kettlebell_windmills",
    ],
)

# Obliques - the rotation line, unloaded then loaded then standing.
ladder(
    "oblique_rotation",
    [
        "russian_twist",
        "kettlebell_russian_twist",
        "kettlebell_windmills",
    ],
)

# Quads - the side lunge, the adductor-side knee-dominant movement.
ladder(
    "lunge_lateral",
    [
        "side_lunge",
        "sumo_squat",
        "cossack_squat",
    ],
)

# Quads - the step-up, the assisted single-leg knee-dominant movement.
ladder(
    "step_up",
    [
        "step_ups",
        "split_squat",
        "bulgarian_split_squat",
        "dumbbell_pistol_squat",
    ],
)

# Quads - the supported single-leg line, ending in the true pistol.
ladder(
    "pistol_assisted",
    [
        "trx_pistol_squat",
        "dumbbell_pistol_squat",
        "kettlebell_pistol_squat",
        "pistol_squat",
    ],
)

# Quads - the kettlebell lunge-and-press, the loaded single-leg knee lift.
ladder(
    "lunge_press",
    [
        "kettlebell_lunge_press",
        "kettlebell_offset_reverse_lunge_and_press",
    ],
)

# Hip adductors - the sumo high pull and the windmill, the loaded twins.
ladder(
    "hip_adduction_loaded",
    [
        "banded_standing_hip_adduction",
        "kettlebell_sumo_high_pull",
        "kettlebell_windmills",
    ],
)

# Forearms - the pinch and the roller, the grip family that is not a carry.
ladder(
    "grip_pinch",
    [
        "dead_hang",
        "plate_pinch",
        "wrist_roller",
    ],
)

# Forearms - the bilateral dumbbell curl, the two-handed twin of the single.
ladder(
    "wrist_bilateral",
    [
        "dumbbell_wrist_curl",
        "bilateral_dumbbell_wrist_curl",
        "barbell_wrist_curl",
    ],
)

# Forearms - the reverse wrist line, dumbbell to bar.
ladder(
    "wrist_reverse_full",
    [
        "dumbbell_reverse_wrist_curl",
        "kettlebell_reverse_wrist_curl",
        "barbell_wrist_curl",
    ],
)

# Traps - the barbell shrugs, the loaded twins of the dumbbell shrug.
ladder(
    "shrug_barbell",
    [
        "dumbbell_shrug",
        "chest_supported_dumbbell_shrug",
        "behind_the_back_barbell_shrug",
        "barbell_shrug",
    ],
)

# Neck - the isometric and the stretch hold.
ladder(
    "neck_isometric",
    [
        "chin_tuck_hold",
        "isometric_neck_lateral_flexion",
    ],
)

# Rear delts - the rows, which reach the rear delts through a pulling pattern.
ladder(
    "rear_delt_row",
    [
        "band_pull_apart",
        "barbell_rear_delt_row",
        "double_kettlebell_rear_delt_row",
        "ring_face_pull",
    ],
)

# Rear delts - the cable-free face pull line, banded to rings.
ladder(
    "rear_delt_face_pull",
    [
        "band_pull_apart",
        "trx_face_pull",
        "ring_face_pull",
        "dumbbell_face_pull",
    ],
)

# Lateral delts - the upright row and the halo, the loaded side-delt lines.
ladder(
    "lateral_raise_loaded",
    [
        "bodyweight_lateral_raise",
        "kettlebell_halo",
        "muscle_snatch",
        "barbell_upright_row",
    ],
)

# Hip thrust - the kneeling banded version, the assisted rung of the thrust.
ladder(
    "hip_thrust_band",
    [
        "glute_bridge",
        "banded_kneeling_hip_thrust",
        "banded_hip_thrust",
        "barbell_hip_thrust",
    ],
)

# Glute - the reverse tabletop pulses, the isometric twin of the bridge.
ladder(
    "glute_isometric",
    [
        "glute_bridge",
        "reverse_tabletop_hip_pulses",
        "side_plank_with_leg_lift",
    ],
)

# Balance/posterior chain - the standing split and warrior III, the
# single-leg balance line.
ladder(
    "single_leg_balance",
    [
        "step_ups",
        "split_squat",
        "standing_split",
        "warrior_iii",
    ],
)

# Olympic - the clean and jerk, and the barbell double of the bell version.
ladder(
    "clean_and_jerk",
    [
        "clean",
        "clean_and_jerk",
    ],
)

# Plyo - the jump and the burpee, the explosive knee-dominant work.
ladder(
    "plyo_jump",
    [
        "jump_rope",
        "jumping_jacks",
        "high_knees",
        "box_jump",
        "jump_squat",
        "burpees",
    ],
)
