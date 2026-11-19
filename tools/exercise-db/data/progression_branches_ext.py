"""Additional progression branches: the established beginner-to-advanced chains.

WHY A SECOND FILE
-----------------
`progression_branches.py` holds the ORIGINAL 29 hand-curated branches. This
file adds the movement-pattern ladders the owner asked for on 2026-10-02
("i want to have their corresponding progression added"), kept separate so the
curated set is never silently rewritten by a regeneration of this one. Both are
merged in build_progression.py; `entry` and every `nodes` name must exist in
the pool, and a name that does not FAILS the build.

SCOPE - AND WHY IT IS NOT ALL 601
---------------------------------
The owner asked for every exercise to have a progression. Measured over the
pool, that is not one job but three:

  * 341 machine rows, 66 stretches/poses, 7 cardio machines. A seated leg curl
    does not progress INTO another exercise - it progresses by load, which the
    ladder already does. Giving it a branch would mean inventing a
    machine-to-machine chain no coach uses, and the post-workout popup would then
    recommend nonsense to whoever had that exercise in their routine. The owner
    chose "only where a real progression exists".
  * 158 free-weight and bodyweight rows, which is where established beginner ->
    advanced chains genuinely exist and what this file covers.

The reasoning used for every chain below is movement-pattern reasoning, NOT
reputation or novelty:

  * bodyweight -> loaded free weight is the standard way difficulty is added;
  * within a lift, angle/grip/tempo variants shift emphasis and are what
    coaches progress to once the main lift is solid;
  * calisthenics ladders follow the published skill order (wall/incline push-up
    -> knee -> floor -> decline/deficit -> diamond/pseudo-planche -> archer ->
    planche; lat pulldown -> assisted pull-up -> full pull-up -> archer -> weighted);
  * isolation work progresses by making the elbow harder to move, not by adding
    load to a compound.

`tag` keeps its original meaning: "new" is a lever that is easier to get into
and often a different emphasis; "advanced" is harder mechanics. A branch may
carry several `nodes` because that is what the popup's forced-choice rule is
for: the owner asked that the user must pick one when more than one exists.
"""

BRANCHES_EXT: dict[str, list[dict]] = {
    # ------------------------------------------------------------------
    # HORIZONTAL PUSH - the calisthenics press ladder
    # ------------------------------------------------------------------
    "wall_push_ups": [
        {
            "entry": "Incline Push-Up",
            "nodes": ["Knee Push Ups", "Decline Push-Up"],
            "note": "Standing at a wall is the easiest way to get the full press pattern; a bench or box takes the same movement lower and harder.",
            "tag": "new",
        },
    ],
    "knee_push_ups": [
        {
            "entry": "Push-Up",
            "nodes": ["Diamond Push Ups", "Decline Push-Up", "Deficit Push Ups"],
            "note": "Knees down is the standard rung between the incline and the floor press: the range of motion is the same, only the load changes.",
            "tag": "new",
        },
    ],
    "incline_push_up": [
        {
            "entry": "Push-Up",
            "nodes": ["Diamond Push Ups", "Wide Grip Push Ups"],
            "note": "Once the incline is comfortable, the floor press puts the load through a longer range at the same angle.",
            "tag": "new",
        },
    ],
    "push_up": [
        {
            "entry": "Dumbbell Bench Press",
            "nodes": ["Barbell Bench Press", "Incline Dumbbell Press"],
            "note": "Bodyweight volume runs out before the pressing muscles do; loading the same pattern is the standard next step.",
            "tag": "advanced",
        },
        {
            "entry": "Decline Push-Up",
            "nodes": ["Deficit Push Ups", "Pseudo Planche Push Ups"],
            "note": "Feet raised makes the press harder without adding weight, which is the calisthenics line when no bar is available.",
            "tag": "advanced",
        },
        {
            "entry": "Diamond Push Ups",
            "nodes": ["Pseudo Planche Push Ups", "Archer Push Ups"],
            "note": "Hands close to the midline take the triceps and shift load toward the lower chest and lean.",
            "tag": "advanced",
        },
        {
            "entry": "Archer Push Ups",
            "nodes": ["Pseudo Planche Push Ups", "Planche"],
            "note": "One arm does the work while the other reaches: the last rung before the straight-arm skills.",
            "tag": "advanced",
        },
    ],
    "decline_push_up": [
        {
            "entry": "Deficit Push Ups",
            "nodes": ["Handstand Push Ups"],
            "note": "Hands on a bench put the chest below the hands, lengthening the range and loading the shoulders more.",
            "tag": "advanced",
        },
    ],
    "deficit_push_ups": [
        {
            "entry": "Close Grip Push Ups",
            "nodes": ["Pseudo Planche Push Ups"],
            "note": "A longer lever at the bottom of the range; the standard trick past a deficit press plateau.",
            "tag": "advanced",
        },
    ],
    "close_grip_push_ups": [
        {
            "entry": "Pseudo Planche Push Ups",
            "nodes": ["Planche"],
            "note": "Hands under the shoulders and the body in a straight line takes the load out of the arms and onto the chest and shoulders.",
            "tag": "advanced",
        },
    ],
    "pseudo_planche_push_ups": [
        {
            "entry": "Planche",
            "nodes": [],
            "note": "The held horizontal strength position; there is nothing further in this pool beyond it.",
            "tag": "advanced",
        },
    ],
    "wall_sit": [
        {
            "entry": "Bodyweight Squat",
            "nodes": ["Goblet Squat", "Box Squat"],
            "note": "An isometric hold teaches the brace and the position without needing to move; the free-weight squat then loads it.",
            "tag": "new",
        },
    ],
    # ------------------------------------------------------------------
    # HORIZONTAL PUSH - loaded bench
    # ------------------------------------------------------------------
    "dumbbell_floor_press": [
        {
            "entry": "Dumbbell Bench Press",
            "nodes": ["Incline Dumbbell Press", "Close-Grip Dumbbell Bench Press"],
            "note": "The floor removes the stretch, so it is the easier of the two presses; a full bench or an angle change follows.",
            "tag": "new",
        },
    ],
    "dumbbell_bench_press": [
        {
            "entry": "Barbell Bench Press",
            "nodes": ["Incline Barbell Bench Press", "Paused Bench Press", "Close-Grip Bench Press"],
            "note": "The same press with a bar lets both sides work together, which is how the lift is normally progressed to.",
            "tag": "advanced",
        },
    ],
    "barbell_bench_press": [
        {
            "entry": "Incline Barbell Bench Press",
            "nodes": ["Paused Incline Bench Press", "Close-Grip Incline Bench Press"],
            "note": "Angle shifts the work to the upper chest once the flat press is solid; the same angle, paused, attacks the sticking point.",
            "tag": "advanced",
        },
        {
            "entry": "Close-Grip Bench Press",
            "nodes": ["Close-Grip EZ-Bar Bench Press", "Close-Grip Incline Bench Press"],
            "note": "Narrower grip takes the load onto the triceps and shifts it lower on the chest; the EZ curve eases the wrists.",
            "tag": "advanced",
        },
    ],
    "incline_dumbbell_press": [
        {
            "entry": "Incline Barbell Bench Press",
            "nodes": ["Paused Incline Bench Press"],
            "note": "Same angle, both arms: more load than two dumbbells can offer at the same effort.",
            "tag": "advanced",
        },
    ],
    "close_grip_bench_press": [
        {
            "entry": "Close-Grip EZ-Bar Bench Press",
            "nodes": ["Close-Grip Incline Bench Press"],
            "note": "The EZ bar lets a close grip be held without wrist pain, which is the usual reason people cannot progress this one.",
            "tag": "advanced",
        },
    ],
    "bench_dips": [
        {
            "entry": "Straight Bar Dips",
            "nodes": ["Weighted Dips", "Ring Dips"],
            "note": "Bench dips are the bodyweight ring on the way to a loaded dip; rings add the instability that makes it a different skill.",
            "tag": "advanced",
        },
    ],
    "machine_assisted_dips": [
        {
            "entry": "Bench Dips",
            "nodes": ["Straight Bar Dips", "Weighted Dips"],
            "note": "Assisted dips build the pressing pattern and the shoulder tolerance; bodyweight on the bars is the next step.",
            "tag": "new",
        },
    ],
    "straight_bar_dips": [
        {
            "entry": "Weighted Dips",
            "nodes": ["Ring Dips"],
            "note": "Adding weight is the standard way a dip progresses once the bodyweight reps are easy; rings are the skill line.",
            "tag": "advanced",
        },
    ],
    # ------------------------------------------------------------------
    # VERTICAL PUSH
    # ------------------------------------------------------------------
    "pike_push_ups": [
        {
            "entry": "Handstand Push Ups",
            "nodes": [],
            "note": "Inverting the press puts nearly all the load on the shoulders; there is nothing beyond it in this pool.",
            "tag": "advanced",
        },
    ],
    "bodyweight_overhead_press": [
        {
            "entry": "Dumbbell Shoulder Press",
            "nodes": ["Barbell Overhead Press", "Arnold Press"],
            "note": "Standing without load is the start; dumbbells add load in a range a beginner can control, and the press follows.",
            "tag": "advanced",
        },
    ],
    "seated_dumbbell_shoulder_press": [
        {
            "entry": "Dumbbell Shoulder Press",
            "nodes": ["Arnold Press", "Push Press"],
            "note": "Seated removes the leg drive, so it is the harder version; standing adds the legs back for more weight.",
            "tag": "advanced",
        },
    ],
    "dumbbell_shoulder_press": [
        {
            "entry": "Barbell Overhead Press",
            "nodes": ["Paused Overhead Press", "Push Press"],
            "note": "The bar lets both arms work together and takes more weight than two dumbbells; pausing removes the kick-through.",
            "tag": "advanced",
        },
        {
            "entry": "Arnold Press",
            "nodes": ["Landmine Press"],
            "note": "Rotating the elbows in front takes the front delts through more of their range on the way up.",
            "tag": "new",
        },
    ],
    "barbell_overhead_press": [
        {
            "entry": "Push Press",
            "nodes": ["Push Jerk", "Handstand Push Ups"],
            "note": "Adding a leg dip lets the press move more weight; the jerk dips deeper still, and the handstand line drops the bar entirely.",
            "tag": "advanced",
        },
    ],
    "push_press": [
        {
            "entry": "Push Jerk",
            "nodes": ["Split Jerk"],
            "note": "The jerk dips and drives under the bar rather than pushing straight up, which is what allows the jump to overhead.",
            "tag": "advanced",
        },
    ],
}