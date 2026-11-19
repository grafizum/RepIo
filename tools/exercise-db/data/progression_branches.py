"""Hand-curated progression branches and realistic human ceilings.

Merged into every progression row by build_progression.py, after the CSV's
own variation ladders. The generated ExerciseProgression.kt carries the
result, so the app never reads this file.

BRANCHES
--------
Keyed by the catalog slug. Each value is a list of branch dicts:
    entry : the catalog exercise the user switches to when they pick the
            branch (must exist in the pool — a typo FAILS the build)
    nodes : the branch's future path after the entry, for the mini progress
            track on the mastery card (all names must exist in the pool)
    note  : one-line coach note shown on the card (what it emphasizes)
    tag   : "new" (an easier/new lever) or "advanced" (harder mechanics)

Branches model the spec's multi-path progression: one movement may branch
into several professionally used chains (e.g. push-up → weighted/high-skill
vs push-up → one-arm leverage work). The CSV's own variation ladder remains
the exercise's main path; branches are the alternative continuations offered
when the main path is mastered.

CEILING_OVERRIDES
-----------------
Keyed by the catalog slug. Values:
    load        : the realistic healthy human ceiling in the exercise's unit
    female_load : optional elite-female figure (same unit) when the data
                  separates it meaningfully
    basis       : short citation-ish note surfaced on the mastery card

Anything NOT listed here falls back to the row's own maximum_progression_value
from the v5 CSV, which is already professional curation. The overrides exist
where peer-reviewed/elite-norm data (Strength Level/StrengthLog elite tables,
NSCA guidelines, powerlifting records for the big three) is firmer than the
CSV value or the spec explicitly names a figure.

Sources for the key-lift figures: Strength Level elite standards (~93 kg male,
~63 kg female raw, drug-tested assumptions), IPF elite totals as sanity checks,
StrengthLog's exercise pages for isolation lifts, gymnastics-strength practice
norms for the bodyweight skills.
"""

BRANCHES: dict[str, list[dict]] = {
    # ------------------------------------------------------------------
    # Horizontal push
    # ------------------------------------------------------------------
    "push_up": [
        {
            "entry": "Dumbbell Bench Press",
            "nodes": ["Barbell Bench Press", "Incline Dumbbell Press"],
            "note": "Loaded pressing takes over from bodyweight volume, and the barbell path follows as the load grows.",
            "tag": "advanced",
        },
        {
            "entry": "Barbell Bench Press",
            "nodes": ["Incline Barbell Bench Press", "Paused Bench Press"],
            "note": "The barbell path: heavier loading than bodyweight pressing can offer, with grip and tempo variants beyond.",
            "tag": "advanced",
        },
    ],
    "knee_push_ups": [
        {
            "entry": "Dumbbell Bench Press",
            "nodes": ["Barbell Bench Press", "Incline Dumbbell Press"],
            "note": "Light loaded pressing replaces the bodyweight volume while the full push-up pattern is coached separately.",
            "tag": "advanced",
        },
    ],
    "incline_push_up": [
        {
            "entry": "Dumbbell Bench Press",
            "nodes": ["Barbell Bench Press", "Incline Dumbbell Press"],
            "note": "Loaded pressing continues the horizontal pattern as the incline angle reaches the floor.",
            "tag": "advanced",
        },
    ],
    "diamond_push_ups": [
        {
            "entry": "Close-Grip Bench Press",
            "nodes": ["Skull Crusher", "Weighted Dips"],
            "note": "Triceps-biased pressing continues under the bar, where the load can grow far past bodyweight.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Overhead push
    # ------------------------------------------------------------------
    "pike_push_ups": [
        {
            "entry": "Dumbbell Shoulder Press",
            "nodes": ["Barbell Overhead Press", "Push Press"],
            "note": "Loaded overhead pressing takes over where the bodyweight skill plateaus, and the barbell press follows.",
            "tag": "advanced",
        },
    ],
    "barbell_overhead_press": [
        {
            "entry": "Push Press",
            "nodes": ["Dumbbell Push Press"],
            "note": "The weightlifting family: leg drive joins the press, allowing substantially heavier loads overhead.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Vertical pull
    # ------------------------------------------------------------------
    "pull_up": [
        {
            "entry": "Lat Pulldown",
            "nodes": ["Close Grip Lat Pulldown", "Reverse Grip Lat Pulldown"],
            "note": "The load path in the gym: heavier lat loading on the pulldown while the bodyweight skill catches up.",
            "tag": "advanced",
        },
    ],
    "chin_ups": [
        {
            "entry": "Close Grip Lat Pulldown",
            "nodes": ["Lat Pulldown", "Pull-Up"],
            "note": "Rotate grip and add cable assistance to keep progress moving while the pulling pattern stays loaded.",
            "tag": "new",
        },
    ],
    "lat_pulldown": [
        {
            "entry": "Close Grip Lat Pulldown",
            "nodes": ["Reverse Grip Lat Pulldown", "Pull-Up"],
            "note": "Narrower grips bias the lower lats; the pull-up is the eventual bodyweight transfer of the same pattern.",
            "tag": "new",
        },
    ],

    # ------------------------------------------------------------------
    # Row
    # ------------------------------------------------------------------
    "bent_over_barbell_row": [
        {
            "entry": "Pendlay Row",
            "nodes": ["Chest-Supported Dumbbell Row"],
            "note": "Dead-start rows from the floor build stronger hip-hinge positioning; supported rows remove the lower-back limit.",
            "tag": "advanced",
        },
    ],
    "inverted_row": [
        {
            "entry": "Lat Pulldown",
            "nodes": ["Close Grip Lat Pulldown", "Weighted Pull-Up"],
            "note": "The horizontal row graduates to vertical pulling, with the pulldown bridging the strength gap.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Squat pattern
    # ------------------------------------------------------------------
    "bodyweight_squat": [
        {
            "entry": "Goblet Squat",
            "nodes": ["Barbell Back Squat", "Front Squat"],
            "note": "The classic loaded line: a counterweighted front squat teaches upright posture before the barbell goes on the back.",
            "tag": "advanced",
        },
        {
            "entry": "Bulgarian Split Squat",
            "nodes": ["Pistol Squat", "Dumbbell Pistol Squat"],
            "note": "The single-leg line: elevated rear foot first, then the full pistol.",
            "tag": "advanced",
        },
    ],
    "goblet_squat": [
        {
            "entry": "Barbell Back Squat",
            "nodes": ["Front Squat", "Pause Squat"],
            "note": "The barbell continuation: back squat for maximal load, front squat for upright torso and core demand.",
            "tag": "advanced",
        },
        {
            "entry": "Bulgarian Split Squat",
            "nodes": ["Pistol Squat", "Dumbbell Pistol Squat"],
            "note": "The unilateral continuation: single-leg work exposes and fixes side imbalances the barbell hides.",
            "tag": "new",
        },
    ],
    "barbell_back_squat": [
        {
            "entry": "Front Squat",
            "nodes": ["Pause Squat", "Box Squat"],
            "note": "Front-loading raises the core and upper-back demand and transfers to cleans; variants target sticking points.",
            "tag": "advanced",
        },
    ],
    "box_squat": [
        {
            "entry": "Barbell Back Squat",
            "nodes": ["Front Squat"],
            "note": "Once free-squat depth is confident, the plain back squat is the standard continuation.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Hinge
    # ------------------------------------------------------------------
    "romanian_deadlift": [
        {
            "entry": "Barbell Deadlift",
            "nodes": ["Deficit Deadlift"],
            "note": "From the hip-hinge variant to the pull from the floor; deficits extend the range for stronger off-floor pulls.",
            "tag": "advanced",
        },
    ],
    "kettlebell_swing": [
        {
            "entry": "Romanian Deadlift",
            "nodes": ["Barbell Deadlift"],
            "note": "The swing's hinge pattern graduates to slow strength work with progressively heavier bars.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Arms
    # ------------------------------------------------------------------
    "dumbbell_bicep_curl": [
        {
            "entry": "Dumbbell Hammer Curl",
            "nodes": ["Concentration Curl", "Preacher Curl"],
            "note": "The neutral-grip line emphasizes the brachialis and forearms, then strict seated variations remove momentum.",
            "tag": "new",
        },
        {
            "entry": "Zottman Curl",
            "nodes": ["Incline Dumbbell Curl", "Cable Curl"],
            "note": "The rotation line: Zottman curls train both curl and reverse-curl strength, then incline and cable angles finish the biceps.",
            "tag": "new",
        },
    ],
    "dumbbell_hammer_curl": [
        {
            "entry": "Preacher Hammer Curl",
            "nodes": ["Concentration Curl"],
            "note": "Elbow-anchored variations remove all cheating so the biceps does every rep alone.",
            "tag": "new",
        },
    ],
    "barbell_curl": [
        {
            "entry": "EZ-Bar Curl",
            "nodes": ["Close-Grip EZ-Bar Curl"],
            "note": "The angled EZ grip spares the wrists at heavy loads while keeping the barbell's loading advantage.",
            "tag": "new",
        },
    ],
    "cable_tricep_pushdown": [
        {
            "entry": "Skull Crusher",
            "nodes": ["Close-Grip Bench Press"],
            "note": "From cable isolation to free-weight elbow extension, ending at the close-grip press where the load can grow largest.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Chest press
    # ------------------------------------------------------------------
    "barbell_bench_press": [
        {
            "entry": "Incline Barbell Bench Press",
            "nodes": ["Close-Grip Bench Press", "Paused Bench Press"],
            "note": "Angle and grip variants shift emphasis to the upper chest and triceps and attack sticking points.",
            "tag": "advanced",
        },
    ],
    "dumbbell_bench_press": [
        {
            "entry": "Incline Dumbbell Press",
            "nodes": ["Decline Dumbbell Fly"],
            "note": "The dumbbell family continues at angles the barbell path already covered, with a larger stretch per rep.",
            "tag": "new",
        },
    ],
    "standing_calf_raise": [
        {
            "entry": "Dumbbell Calf Raise",
            "nodes": ["Barbell Calf Raise", "Donkey Calf Raise"],
            "note": "Adding external load doubles the demand per calf; the barbell version scales furthest.",
            "tag": "new",
        },
    ],
    "leg_press": [
        {
            "entry": "Hack Squat",
            "nodes": ["Barbell Back Squat"],
            "note": "Machine-supported squats graduate to free-weight barbell work as stability matures.",
            "tag": "advanced",
        },
    ],
    "plank": [
        {
            "entry": "Hanging Knee Raise",
            "nodes": ["Hanging Leg Raise", "Toes to Bar"],
            "note": "Static bracing graduates to dynamic anti-extension, the core's real job under a barbell.",
            "tag": "advanced",
        },
    ],
    "crunches": [
        {
            "entry": "Hanging Knee Raise",
            "nodes": ["Hanging Leg Raise", "Toes to Bar"],
            "note": "The hanging line: knee raises first, straight legs next, toes-to-bar last.",
            "tag": "advanced",
        },
    ],

    # ------------------------------------------------------------------
    # Shoulders
    # ------------------------------------------------------------------
    "dumbbell_lateral_raise": [
        {
            "entry": "Cable Lateral Raise",
            "nodes": ["Cable Front Raise"],
            "note": "Cables keep tension through the whole range where dumbbells unload the bottom half.",
            "tag": "new",
        },
    ],
    "dumbbell_shoulder_press": [
        {
            "entry": "Barbell Overhead Press",
            "nodes": ["Push Press"],
            "note": "The seated dumbbell press transfers to the standing barbell press, then to the weightlifting jerk family.",
            "tag": "advanced",
        },
    ],
}


CEILING_OVERRIDES: dict[str, dict] = {
    # ---------------- Barbell, the big lifts ----------------
    "barbell_back_squat": {
        "load": 220.0, "female_load": 145.0, "basis": "Strength Level elite raw standards (~93 kg male / ~63 kg female)",
    },
    "barbell_bench_press": {
        "load": 180.0, "female_load": 100.0, "basis": "Strength Level elite raw standards",
    },
    "barbell_deadlift": {
        "load": 250.0, "female_load": 160.0, "basis": "Strength Level elite raw standards",
    },
    "barbell_overhead_press": {
        "load": 80.0, "female_load": 50.0, "basis": "Strength Level elite strict-press standards",
    },
    "front_squat": {
        "load": 180.0, "female_load": 115.0, "basis": "typically ~80% of the elite back squat",
    },
    "romanian_deadlift": {
        "load": 200.0, "female_load": 130.0, "basis": "RDLs live below competition pulls; ~80% of the elite deadlift",
    },
    "bent_over_barbell_row": {
        "load": 140.0, "female_load": 90.0, "basis": "elite bent-over rows track ~55-60% of the elite deadlift",
    },
    "barbell_lunge": {
        "load": 140.0, "female_load": 90.0, "basis": "single-stance loading caps safe barbell position",
    },
    "push_press": {
        "load": 110.0, "female_load": 70.0, "basis": "elite push press ~35% over the strict press",
    },

    # ---------------- Dumbbell (per-hand ceilings) ----------------
    "dumbbell_bicep_curl": {
        "load": 45.0, "female_load": 30.0, "basis": "elite dumbbell curl ~45-50 kg/arm (StrengthLog); strict-form ceiling",
    },
    "dumbbell_hammer_curl": {
        "load": 50.0, "female_load": 32.0, "basis": "neutral grip allows ~5 kg over the supinated curl",
    },
    "dumbbell_bench_press": {
        "load": 60.0, "female_load": 35.0, "basis": "elite DB bench ~120 kg total, ~60 kg/hand",
    },
    "incline_dumbbell_press": {
        "load": 50.0, "female_load": 30.0, "basis": "incline angle costs ~15% over flat",
    },
    "dumbbell_shoulder_press": {
        "load": 45.0, "female_load": 25.0, "basis": "elite seated DB press ~90 kg total",
    },
    "dumbbell_lateral_raise": {
        "load": 25.0, "female_load": 15.0, "basis": "delt isolation caps early; elite strict raises ~25-30 kg",
    },
    "dumbbell_lunge": {
        "load": 50.0, "female_load": 32.0, "basis": "balance, not strength, caps single-stance DB loading",
    },
    "goblet_squat": {
        "load": 60.0, "female_load": 40.0, "basis": "hold position fails before the legs do past ~2 plates",
    },

    # ---------------- Bodyweight skills ----------------
    # Planche-class skills (one-arm work, full pistol) have no load ceiling:
    # the skill IS the ceiling, their rows carry no maxValue and the ladder
    # ends at the final variation. No override needed here — ceiling stays
    # null and MasteryReason is LADDER_COMPLETE.
}
