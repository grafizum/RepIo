#!/usr/bin/env python3
"""Progression database (v5) -> RepIo exercise pool + progression tables.

The authoritative source is `tools/exercise-db/data/exercise_progression_database_v5.csv`
(one row per exercise, 90 columns of progression metadata). This pipeline:

  1. loads and validates the v5 CSV (JSON fields must parse; Roman-stage /
     target-rep pairs must agree; validation flags must be consistent),
  2. maps the v5 taxonomy onto the app's 8 equipment categories and 19 precise
     muscles (the join keys the whole app already uses),
  3. merges the English method descriptions from the previous pipeline's
     catalog (the v5 CSV carries none), then fills the rest from the
     hand-written table in data/hand_descriptions.py,
  4. writes:
       tools/exercise-db/out/catalog_v5.json        - the merged catalog
       tools/exercise-db/out/progression_v5.json    - the progression payload
       tools/exercise-db/out/report_progression.txt - stats + invariants
       tools/exercise-db/out/unmatched_v5.csv       - rows that could not be
                                                      reconciled with the old
                                                      pool and lost their
                                                      description as a result

The previous catalog (out/catalog.json) is used ONLY as a description source
and for reconciliation stats; the v5 CSV alone decides which exercises exist.

Run from the repository root:
    python tools/exercise-db/build_progression.py
"""

from __future__ import annotations

import generalize  # noqa: F401  (name generalization, see generalize.py)

import csv
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = pathlib.Path(__file__).resolve().parent / "out"
DATA = pathlib.Path(__file__).resolve().parent / "data"

V5_CSV = DATA / "exercise_progression_database_v5.csv"
OLD_CATALOG = OUT / "catalog.json"

# The hand-written description table: the tier-5 merge source for rows the
# old catalog could not cover.
sys.path.insert(0, str(DATA))
from hand_descriptions import HAND_DESCRIPTIONS  # noqa: E402
sys.path.insert(0, str(pathlib.Path(__file__).parent / "data"))
from progression_branches import BRANCHES, CEILING_OVERRIDES  # noqa: E402

# --------------------------------------------------------------------------
# The app's taxonomies (must mirror WorkoutTemplates / mapping.py exactly).
# --------------------------------------------------------------------------
EQUIPMENT_CATEGORIES = [
    "Bodyweight", "Barbell", "Dumbbell", "Kettlebell",
    "Cable", "Machine", "Bands", "Other",
]

# v5 equipment_category -> app category. "flagged_*" values inherit the
# mapping of their stem; anything unknown lands in "Other".
EQUIPMENT_MAP = {
    "bodyweight": "Bodyweight",
    "barbell": "Barbell",
    "ez_bar": "Barbell",
    "smith_machine": "Machine",
    "dumbbell": "Dumbbell",
    "machine_or_cable": "Machine",   # provisional; split by equipment_raw below
    "kettlebell": "Kettlebell",
    "resistance_band": "Bands",
    "external_added_load": "Other",
    "unmapped_ab_wheel": "Other",
}

# machine_or_cable rows carry the concrete apparatus in equipment_raw; cable
# columns/pulleys are the "Cable" chip, everything else is a "Machine".
CABLE_RAW_VALUES = {
    "cable", "cable_crossover", "cable_pulley", "cable_column",
    "dual_cable_pulley", "adjustable_cable_pulley", "cable_and_bench",
}

# Per-exercise category corrections, keyed by normalized_exercise_id.
#
# The CSV's category is right about the MOVEMENT and wrong about the DRAWER
# the app files it in, and the two app layers read different columns: the
# picker filters by this category ("Kettlebell" chip = the Kettlebell rows)
# while the equipment gate reads ExerciseEquipmentTags' fine tags. A row that
# disagrees with itself therefore passes the gate and still shows up under
# gear the user does not own - which is exactly how a bodyweight + dumbbell
# kit was offered a "Kettlebell" goblet squat and "Bodyweight" stability-ball
# presses that need a gym machine. EquipmentCategoryConsistencyTest fails on
# any row whose category and requirements disagree, so this table is the
# place to answer it.
EQUIPMENT_OVERRIDES = {
    # A dumbbell held at the chest, not a bell: the classic goblet hold.
    "goblet-squat": "Dumbbell",
    # Done under a doorway bar or a sturdy table - home gear, no plates.
    "inverted-row": "Bodyweight",
    # A captain's chair and a stability ball are gym apparatus, not floor work.
    "captains-chair-knee-raise": "Machine",
    "captains-chair-leg-raise": "Machine",
    "stability-ball-push-up": "Machine",
    "stability-ball-push-up-hands-on-ball": "Machine",
    "stability-ball-wall-squat": "Machine",
}

# v5 anatomical muscle tokens -> the app's precise muscles. Compound
# "a;b" primaries resolve to the FIRST token that maps (the CSV lists the
# primary mover first); multi-mover rows fall back to movement-pattern rules.
MUSCLE_MAP = {
    "pectoralis_major": "Pectorals",
    "latissimus_dorsi": "Lats",
    "anterior_deltoid": "Front Deltoids",
    "lateral_deltoid": "Lateral Deltoids",
    "posterior_deltoid": "Rear Deltoids",
    "trapezius": "Upper Traps",
    "rhomboids": "Upper Back",
    "erector_spinae": "Spinal Erectors",
    "biceps_brachii": "Biceps",
    "triceps_brachii": "Triceps",
    "forearm_flexors": "Forearms",
    "forearm_extensors": "Forearms",
    "brachialis": "Biceps",
    "rectus_abdominis": "Abs",
    "transverse_abdominis": "Abs",
    "obliques": "Obliques",
    "quadriceps": "Quadriceps",
    "hamstrings": "Hamstrings",
    "gluteus_maximus": "Glutes",
    "gluteus_medius": "Glutes",
    "gastrocnemius": "Calves",
    "soleus": "Calves",
    "tibialis_anterior": "Calves",
    "adductors": "Hip Adductors",
    "abductors": "Hip Abductors",
    "gluteus_minimus": "Glutes",
    "hip_flexors": "Abs",
    "serratus_anterior": "Abs",
}

# Movement-pattern fallbacks for rows whose mapped muscles are ambiguous
# (e.g. "gluteus_maximus;quadriceps" must stay a Quadriceps movement when the
# pattern is a squat, and a Glutes movement when it is a thrust).
# "hip_flexors" maps to Abs: the app has no Hip Flexors group and a hip-flexor
# raise IS a core/abs movement in the app's 19-muscle taxonomy.
PATTERN_MUSCLE = {
    "squat": "Quadriceps",
    "lunge_or_split_stance": "Quadriceps",
    "hip_hinge": "Hamstrings",
    "ballistic_hip_hinge": "Hamstrings",
    "horizontal_push": "Pectorals",
    "vertical_or_overhead_push": "Front Deltoids",
    "pull_vertical_or_horizontal": "Lats",
    "elbow_flexion_isolation": "Biceps",
    "elbow_or_joint_extension_isolation": "Triceps",
    "core_stability_general": "Abs",
    "core_anti_extension": "Abs",
    "core_flexion": "Abs",
    "core_rotation": "Obliques",
    "core_or_hip_lateral_stability": "Glutes",
    "cardio_locomotion": "Quadriceps",
    "loaded_carry": "Upper Back",
    "ankle_plantarflexion": "Calves",
    "grip_or_forearm_isolation": "Forearms",
    "shoulder_or_hip_raise_isolation": "Glutes",
    "olympic_technical": "Quadriceps",
    "plyometric_lower_body": "Quadriceps",
    "lower_body_compound_or_isolation": "Quadriceps",
    "mobility_stretch": "",
    "full_body_compound": "Quadriceps",
}

# --------------------------------------------------------------------------
# Description reconciliation with the previous catalog.
#
# The v5 CSV carries no descriptions; the old catalog does. Names drifted
# between the two sources, so matching runs in tiers:
#   1. slug equality                       (exact, punctuation-normalized)
#   2. normalized-token equality           (plurals, hyphens, apostrophes)
#   3. reordered tokens                    ("Dumbbell Tricep Kickback" vs
#                                           "Tricep Dumbbell Kickback")
#   4. curated alias table                 (systematic name drift, reviewed)
#
# Ambiguous partial matches ("Barbell Calf Raise" -> "Barbell Seated Calf
# Raise") are deliberately NOT matched: a wrong description is worse than a
# missing one, and those rows land in the unmatched report instead.
# --------------------------------------------------------------------------
ALIASES = {
    # v5 name -> old-pool slug (the systematic drift the tiers cannot see).
    "ab_wheel_rollout": "ab_wheel_roll_out",
    "barbell_ab_rollout": "barbell_ab_roll_out",
    "banded_good_morning": "band_good_morning",
    "lat_pulldown": "lat_pull_down",
    "reverse_grip_lat_pulldown": "reverse_grip_lat_pull_down",
    "close_grip_lat_pulldown": "close_grip_front_lat_pulldown",
    "behind_the_neck_lat_pulldown": "behind_the_neck_pull_down",
    "chin_ups": "chin_up",
    "muscle_ups": "muscle_up",
    "sit_ups": "sit_up",
    "burpees": "burpee",
    "dumbbell_fly": "dumbbell_flyes",
    "decline_dumbbell_fly": "decline_dumbbell_flyes",
    "dumbbell_pullover": "dumbbell_pull_over",
    "dumbbell_skull_crusher": "dumbbell_skullcrusher",
    "skull_crusher": "skullcrusher",
    "pec_deck": "pec_dec",
    "bench_press": "barbell_bench_press",
    "smith_machine_squat": "smith_machine_back_squat",
    "smith_machine_split_squat": "smith_machine_split_squat",
    "band_assisted_pull_ups": "band_assisted_pull_up",
    "negative_pull_ups": "negative_pull_up",
    "neutral_grip_pull_ups": "neutral_grip_pull_up",
    "diamond_push_ups": "diamond_push_up",
    "handstand_push_ups": "handstand_push_up",
    "archer_push_ups": "archer_push_up",
    "archer_pull_ups": "archer_pull_up",
    "assisted_pull_ups": "assisted_pull_up",
    "machine_assisted_dips": "machine_assisted_dip",
    "air_bike": "fan_bike",
    "battle_rope_double_slam": "battle_rope_slam",
    "behind_the_back_barbell_shrug": "barbell_behind_the_back_shrug",
    "kettlebell_single_leg_deadlift": "single_leg_kettlebell_deadlift",
    "dumbbell_tricep_kickback": "tricep_dumbbell_kickback",
    "bent_arm_barbell_pullover": "bent_arm_barbell_pull_over",
    "bulgarian_split_squat": "barbell_bulgarian_split_squat",
    "single_leg_romanian_deadlift": "single_leg_dumbbell_romanian_deadlift",
    "one_arm_single_leg_dumbbell_romanian_deadlift": "single_leg_dumbbell_romanian_deadlift",
    "single_leg_glute_bridge_hold": "single_leg_glute_bridge",
    "single_leg_extension": "single_leg_leg_extension",
    "ez_bar_reverse_curl": "reverse_grip_ez_bar_curl",
    "ez_bar_shrug": "seated_ez_bar_shrug",
    "cable_hammer_curl": "cable_rope_hammer_curl",
    "cable_lateral_raise": "cable_seated_lateral_raise",
    "cable_front_raise": "lying_cable_front_raise",
    "dumbbell_reverse_fly": "incline_dumbbell_reverse_fly",
    "dumbbell_upright_row": "standing_dumbbell_upright_row",
    "dumbbell_reverse_curl": "reverse_grip_dumbbell_curl",
    "dumbbell_calf_raise": "dumbbell_walking_calf_raise",
    "barbell_calf_raise": "barbell_seated_calf_raise",
    "machine_calf_raise": "machine_seated_calf_raise",
    "dumbbell_tricep_extension": "decline_dumbbell_triceps_extension",
    "lying_tricep_extension": "lying_cable_triceps_extension",
    "overhead_tricep_extension": "band_overhead_triceps_extension",
    "preacher_hammer_curl": "preacher_hammer_dumbbell_curl",
    "barbell_preacher_curl": "reverse_barbell_preacher_curls",
    "hanging_knee_raise": "alternating_hanging_knee_raise",
    "lying_leg_raise": "alternating_lying_leg_raise",
    "kettlebell_floor_press": "kettlebell_alternating_floor_press",
    "kettlebell_sumo_high_pull": "kettlebell_sumo_deadlift_high_pull",
    "decline_bench_press": "decline_barbell_bench_press",
    "decline_dumbbell_flyes": "decline_dumbbell_flyes",
    "cable_face_pull": "up_cable_face_pull",
    "standing_calf_stretch": "standing_gastrocnemius_calf_stretch",
    "standing_side_bend": "standing_side_bend_stretch",
    "flutter_kicks": "flutter_kick",
    "scissor_kicks": "scissor_kick",
    "mountain_climbers": "mountain_climber",
    "jumping_jacks": "jumping_jack",
    "kettlebell_windmills": "kettlebell_windmill",
    "ring_dips": "ring_dip",
    "bench_dips": "bench_dip",
    "seated_cable_row": "seated_cable_rows",
    "incline_hammer_curl": "incline_hammer_curls",
    "reverse_crunches": "reverse_crunch",
    "bird_dog_hold": "bird_dog",
    "stability_ball_leg_curl": "exercise_ball_leg_curl",
    "ball_pike": "exercise_ball_pike",
}


def normalize_name(name: str) -> str:
    """Lowercase, drop punctuation, singularize simple plurals."""
    n = name.lower().replace("-", " ").replace("(", " ").replace(")", " ")
    n = n.replace("'", " ").replace(".", " ")
    words = [w[:-1] if w.endswith("s") and len(w) > 3 else w for w in n.split()]
    return re.sub(r"\s+", " ", " ".join(words)).strip()


# --------------------------------------------------------------------------
# Roman pattern consistency (the CSV is validated, but import re-checks).
# --------------------------------------------------------------------------
ROMAN_VALUES = {
    "I": 1, "II": 2, "III": 3, "IV": 4, "V": 5, "VI": 6, "VII": 7,
    "VIII": 8, "IX": 9, "X": 10, "XI": 11, "XII": 12, "XIII": 13,
}
NUM_TO_ROMAN = {v: k for k, v in ROMAN_VALUES.items()}

# Every roman_pattern_id the CSV uses maps to this shape; the stage -> reps
# table itself comes from roman_pattern_json, so we verify against that.
def stage_reps(row: dict, stage: str) -> list[int] | None:
    raw = row.get("roman_pattern_json") or ""
    if not raw:
        return None
    try:
        table = json.loads(raw)
    except json.JSONDecodeError:
        return None
    reps = table.get(stage)
    return [int(x) for x in reps] if reps else None


def slugify(name: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
    return re.sub(r"_+", "_", slug)


def load_old_descriptions() -> tuple[dict[str, str], dict[str, str], dict[str, list[str]]]:
    """Description merge source: the previous catalog.

    Returns (by_slug, by_normalized_name, by_reordered_tokens); the two extra
    indexes back the tier-2/3 matching described at [ALIASES].
    """
    if not OLD_CATALOG.exists():
        return {}, {}, {}
    entries = json.loads(OLD_CATALOG.read_text(encoding="utf-8"))
    by_slug: dict[str, str] = {}
    by_norm: dict[str, list[str]] = {}
    for e in entries:
        desc = (e.get("description") or "").strip()
        slug = slugify(e["name"])
        if desc:
            by_slug[slug] = desc
        norm = normalize_name(e["name"])
        if norm:
            by_norm.setdefault(norm, []).append(slug)
    by_tokens: dict[str, list[str]] = {}
    for norm, slugs in by_norm.items():
        by_tokens.setdefault(" ".join(sorted(norm.split())), slugs[0])
    return by_slug, by_norm, by_tokens


def parse_float(value: str) -> float | None:
    if value is None or value.strip() == "":
        return None
    try:
        return float(value)
    except ValueError:
        return None


def parse_int(value: str) -> int | None:
    f = parse_float(value)
    return int(f) if f is not None else None


def parse_bool(value: str) -> bool:
    return (value or "").strip().lower() == "true"


def map_equipment(row: dict) -> str:
    override = EQUIPMENT_OVERRIDES.get((row.get("normalized_exercise_id") or "").strip())
    if override:
        return override
    cat = (row.get("equipment_category") or "").strip()
    raw = (row.get("equipment_raw") or "").strip()
    if cat == "machine_or_cable":
        # The concrete apparatus decides which chip the exercise sits under.
        return "Cable" if raw in CABLE_RAW_VALUES else "Machine"
    if cat in EQUIPMENT_MAP:
        return EQUIPMENT_MAP[cat]
    # flagged_* inherits the stem when the stem is a known category.
    stem = cat.replace("flagged_", "")
    if stem in EQUIPMENT_MAP:
        return EQUIPMENT_MAP[stem]
    # Cardio machines and other one-offs: keep them out of the strength chips.
    return "Other"


def map_muscle(row: dict) -> str:
    primary = (row.get("primary_muscle") or "").strip()
    tokens = [t.strip() for t in primary.split(";") if t.strip()]
    mapped = [MUSCLE_MAP[t] for t in tokens if t in MUSCLE_MAP]
    pattern = (row.get("movement_pattern") or "").strip()

    if len(mapped) >= 1:
        first = mapped[0]
        # A "gluteus_maximus;quadriceps" squat stays a quad movement, a
        # "gluteus_maximus;hamstrings" hinge stays a hamstring movement:
        # the pattern decides which of the two named movers is primary.
        if len(mapped) >= 2 and pattern in PATTERN_MUSCLE and PATTERN_MUSCLE[pattern] in mapped:
            return PATTERN_MUSCLE[pattern]
        return first
    if pattern in PATTERN_MUSCLE and PATTERN_MUSCLE[pattern]:
        return PATTERN_MUSCLE[pattern]
    return ""


def validate_row(row: dict, line_no: int) -> list[str]:
    """Sanity checks on import. Returns a list of problems (empty = ok)."""
    problems: list[str] = []
    name = row.get("exercise_name") or f"line {line_no}"

    # JSON fields must parse.
    for field in (
        "roman_pattern_json", "level_value_sequence_json",
        "level_increment_sequence_json", "level_jump_ratio_sequence_json",
        "level_jump_class_sequence_json", "easy_target_reps_json",
        "medium_target_reps_json", "hard_target_reps_json",
        "pre_jump_target_reps_json", "post_jump_target_reps_json",
        "variation_sequence_json",
    ):
        raw = (row.get(field) or "").strip()
        if raw:
            try:
                json.loads(raw)
            except json.JSONDecodeError as exc:
                problems.append(f"{name}: {field} does not parse ({exc})")

    # Stage / target-rep pairs must agree with the Roman table.
    for level in ("easy", "medium", "hard"):
        stage = (row.get(f"{level}_roman_stage") or row.get(f"{level}_start_stage") or "").strip()
        reps_raw = (row.get(f"{level}_target_reps_json") or "").strip()
        if not stage or not reps_raw:
            continue
        try:
            target = json.loads(reps_raw)
        except json.JSONDecodeError:
            continue  # already reported
        expected = stage_reps(row, stage)
        if expected is not None and [int(x) for x in target] != expected:
            problems.append(
                f"{name}: {level} stage {stage} should carry reps {expected}, found {target}"
            )

    # Validation flags must be coherent.
    status = (row.get("progression_validation_status") or "").strip()
    coach = parse_bool(row.get("requires_coach_review"))
    if status == "manual_review_required" and not coach:
        problems.append(f"{name}: status manual_review_required but requires_coach_review=false")

    return problems


def main() -> int:
    OUT.mkdir(exist_ok=True)
    problems: list[str] = []

    with V5_CSV.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    if len({r["exercise_name"].lower() for r in rows}) != len(rows):
        problems.append("duplicate exercise_name values in the v5 CSV")

    old_desc, old_by_norm, old_by_tokens = load_old_descriptions()
    old_slugs = set(old_desc)

    catalog: list[dict] = []
    progression: list[dict] = []
    unmatched: list[dict] = []
    stats = {
        "rows": len(rows),
        "valid_json_rows": 0,
        "matched_with_description": 0,
        "description_from_old_pool": 0,
        "description_from_hand": 0,
        "no_description": 0,
        "beginner_build_eligible": 0,
        "app_ready": 0,
    }

    for line_no, row in enumerate(rows, start=2):  # header is line 1
        problems.extend(validate_row(row, line_no))

        name = (row["exercise_name"] or "").strip()
        if not name:
            problems.append(f"line {line_no}: blank exercise_name")
            continue

        status = (row.get("progression_validation_status") or "").strip()
        slug = slugify(name)

        # --- description reconciliation (tiers 1-4) -----------------------
        norm = normalize_name(name)
        desc = old_desc.get(slug, "")
        matched_via = "slug" if desc else ""
        if not desc and norm in old_by_norm:
            desc = old_desc.get(old_by_norm[norm][0], "")
            matched_via = "normalized" if desc else ""
        if not desc:
            reordered = " ".join(sorted(norm.split()))
            target_slug = old_by_tokens.get(reordered)
            if target_slug:
                desc = old_desc.get(target_slug, "")
                matched_via = "reordered" if desc else ""
        if not desc:
            alias_slug = ALIASES.get(slug)
            if alias_slug:
                desc = old_desc.get(alias_slug, "")
                matched_via = "alias" if desc else ""
        if not desc:
            # Tier 5: the hand-written table. Keyed by slug; unknown keys are
            # a build failure (checked below), so a typo cannot silently
            # leave one exercise without a description.
            desc = HAND_DESCRIPTIONS.get(slug, "")
            matched_via = "hand" if desc else ""

        # --- catalog entry (what the app's pickers/badges/search need) ----
        # `name` is the DISPLAY name and goes through the generalization step:
        # a leading apparatus word ("Smith Machine Bent Over Row") repeats the
        # `equipment` field this same row already carries. The slug and the id
        # keep the CSV's spelling, so descriptions, progression branches and
        # every stored id stay exactly where they were.
        entry = {
            "id": row.get("normalized_exercise_id") or slugify(name),
            "slug": slug,
            "name": name,
            "sourceName": name,
            "muscle": map_muscle(row),
            "equipment": map_equipment(row),
            "type": (row.get("training_category") or "").strip(),
            "level": (row.get("mechanic") or "").strip(),
            "description": desc,
            "in_old_pool": slug in old_slugs,
            "matchedVia": matched_via,
        }
        catalog.append(entry)

        # --- progression payload (the engine's source of truth) ----------
        prog = {
            "id": entry["id"],
            "slug": slug,
            "method": (row.get("progression_method") or "").strip(),
            "valueSource": (row.get("progression_value_source") or "").strip(),
            "unit": (row.get("level_1_unit") or row.get("easy_start_unit") or "").strip(),
            "level1": parse_float(row.get("level_1_value")),
            "increment": parse_float(row.get("main_level_increment")),
            "maxLevel": parse_int(row.get("max_supported_main_level")),
            "minValue": parse_float(row.get("minimum_progression_value")),
            "maxValue": parse_float(row.get("maximum_progression_value")),
            "step": parse_float(row.get("progression_value_step")),
            "repMin": parse_int(row.get("rep_min")),
            "repMax": parse_int(row.get("rep_max")),
            "restSeconds": parse_int(row.get("rest_seconds")),
            "defaultSetCount": parse_int(row.get("default_set_count")),
            "easy": {
                "level": parse_int(row.get("easy_main_level")),
                "value": parse_float(row.get("easy_start_value")),
                "stage": (row.get("easy_start_stage") or row.get("easy_roman_stage") or "").strip(),
                "reps": row.get("easy_target_reps_json") or "",
            },
            "medium": {
                "level": parse_int(row.get("medium_main_level")),
                "value": parse_float(row.get("medium_start_value")),
                "stage": (row.get("medium_start_stage") or row.get("medium_roman_stage") or "").strip(),
                "reps": row.get("medium_target_reps_json") or "",
            },
            "hard": {
                "level": parse_int(row.get("hard_main_level")),
                "value": parse_float(row.get("hard_start_value")),
                "stage": (row.get("hard_start_stage") or row.get("hard_roman_stage") or "").strip(),
                "reps": row.get("hard_target_reps_json") or "",
            },
            "resetPolicy": (row.get("rep_reset_policy") or "").strip(),
            "resetStageAfterJump": (row.get("rep_reset_stage_after_jump") or "").strip(),
            "preJumpReps": row.get("pre_jump_target_reps_json") or "",
            "postJumpReps": row.get("post_jump_target_reps_json") or "",
            "romanPattern": row.get("roman_pattern_json") or "",
            "romanPatternId": row.get("roman_pattern_id") or "",
            "levelValueSequence": row.get("level_value_sequence_json") or "",
            "variationSequence": row.get("variation_sequence_json") or "",
            "isBeginnerFriendly": parse_bool(row.get("is_beginner_friendly")),
            "isSimpleAppReady": parse_bool(row.get("is_simple_app_ready")),
            "requiresCoachReview": parse_bool(row.get("requires_coach_review")),
            "includeInStrengthV1": parse_bool(row.get("include_in_strength_v1")),
            "includeInCardioV1": parse_bool(row.get("include_in_cardio_v1")),
            "includeInMobilityV1": parse_bool(row.get("include_in_mobility_v1")),
            "beginnerBuildEligible": parse_bool(row.get("beginner_build_eligible")),
            "validationStatus": status,
        }
        progression.append(prog)

        # --- stats / unmatched -------------------------------------------
        if entry["description"]:
            if matched_via == "hand":
                stats["description_from_hand"] += 1
            else:
                stats["description_from_old_pool"] += 1
        else:
            stats["no_description"] += 1
            unmatched.append({
                "exercise_name": name,
                "progression_method": prog["method"],
                "include_in_strength_v1": row.get("include_in_strength_v1", ""),
                "reason": "no description available from any tier (old pool or hand table)",
            })
        if prog["beginnerBuildEligible"]:
            stats["beginner_build_eligible"] += 1
        if prog["isSimpleAppReady"] and prog["validationStatus"] == "valid":
            stats["app_ready"] += 1

    stats["valid_json_rows"] = sum(1 for p in progression if p["levelValueSequence"] or p["method"])

    # ------------------------------------------------------------------
    # Progression branches + human ceilings (coach-curated sidecar).
    #
    # Branches give a mastered exercise professional alternative paths; the
    # ceilings cap the ladder at realistic human performance. Both merge
    # into the progression payload so the generated Kotlin carries the
    # result and the app never reads this file.
    # ------------------------------------------------------------------
    name_by_slug = {e["slug"]: e["name"] for e in catalog}
    for e in catalog:
        slug = e["slug"]
        prog = next(p for p in progression if p["slug"] == slug)

        # --- branches ---
        branch_list = BRANCHES.get(slug) or []
        cleaned = []
        for b in branch_list:
            entry_name = b["entry"]
            entry_slug = None
            for s, n in name_by_slug.items():
                if n == entry_name:
                    entry_slug = s
                    break
            if entry_slug is None:
                problems.append(
                    f"branches: '{slug}' -> entry '{entry_name}' is not a catalog exercise")
                continue
            bad_nodes = [n for n in b.get("nodes", []) if n not in name_by_slug.values()]
            if bad_nodes:
                problems.append(
                    f"branches: '{slug}' nodes not in catalog: {bad_nodes}")
                continue
            cleaned.append({
                "entry": entry_name,
                "entrySlug": entry_slug,
                "nodes": list(b.get("nodes", [])),
                "note": b.get("note", ""),
                "tag": b.get("tag", "new"),
            })
        prog["branches"] = cleaned

        # --- human ceiling ---
        ov = CEILING_OVERRIDES.get(slug)
        if ov is not None:
            prog["ceiling"] = {
                "load": ov.get("load"),
                "femaleLoad": ov.get("female_load"),
                "basis": ov.get("basis", ""),
            }
        elif prog.get("maxValue") is not None and prog.get("unit") != "variation_step":
            # variation_step rows carry no load ceiling: their maxValue is the
            # hardest variation's rep count, not a level the ladder climbs to.
            prog["ceiling"] = {
                "load": prog["maxValue"],
                "femaleLoad": None,
                "basis": "v5 progression database maximum progression value",
            }
        else:
            prog["ceiling"] = None

    # Sidecar integrity: every branch key and ceiling key must be a catalog slug.
    for key in sorted(BRANCHES):
        if key not in name_by_slug:
            problems.append(f"progression_branches: branch key '{key}' matches no catalog exercise")
    for key in sorted(CEILING_OVERRIDES):
        if key not in name_by_slug:
            problems.append(f"progression_branches: ceiling key '{key}' matches no catalog exercise")
    stats["rows_with_branches"] = sum(1 for p in progression if p.get("branches"))
    stats["rows_with_ceiling"] = sum(1 for p in progression if p.get("ceiling"))

    # Every hand-written key must hit a catalog row: a typo would otherwise
    # silently leave that exercise blank.
    catalog_slugs = {e["slug"] for e in catalog}
    for key in sorted(HAND_DESCRIPTIONS):
        if key not in catalog_slugs:
            problems.append(f"hand_descriptions: key '{key}' matches no catalog exercise")
    match_counts: dict[str, int] = {}
    for e in catalog:
        if e["description"]:
            key = e["matchedVia"] or "slug"
            match_counts[key] = match_counts.get(key, 0) + 1

    # Invariants the app relies on.
    by_muscle: dict[str, int] = {}
    for e in catalog:
        by_muscle[e["muscle"]] = by_muscle.get(e["muscle"], 0) + 1
    slugs = [e["slug"] for e in catalog]
    if len(set(slugs)) != len(slugs):
        problems.append("slug collisions after mapping")

    # --- name generalization (see generalize.py) -------------------------
    # Applied LAST, after every slug-keyed lookup has run, so the description
    # merge and the progression branches still match on the CSV's own spelling.
    # The rename map is written alongside the catalog: the app needs it to keep
    # routines logged under the old names pointing at the same exercise.
    try:
        renames = generalize.build_rename_map([e["name"] for e in catalog])
    except ValueError as exc:
        renames = {}
        problems.append(f"name generalization: {exc}")
    for e in catalog:
        e["name"] = renames.get(e["name"], e["name"])
    # Progression branches point at sibling exercises BY NAME ("mastered this,
    # try that"), so they have to travel with the rename. Leaving them on the
    # old spelling would leave the generated Kotlin linking to exercises the
    # pool no longer has - 26 dangling references when this was added.
    if renames:
        for p in progression:
            for b in p.get("branches") or []:
                if b.get("entry") in renames:
                    b["entry"] = renames[b["entry"]]
                if b.get("nodes"):
                    b["nodes"] = [renames.get(n, n) for n in b["nodes"]]
    display_names = [e["name"] for e in catalog]
    if len(set(display_names)) != len(display_names):
        duplicates = sorted({n for n in display_names if display_names.count(n) > 1})
        problems.append(
            "display names must be unique - the pool is joined by name, so a "
            f"duplicate merges two exercises: {duplicates}"
        )
    known = set(display_names)
    for p in progression:
        for b in p.get("branches") or []:
            for ref in [b.get("entry")] + list(b.get("nodes") or []):
                if ref and ref not in known:
                    problems.append(
                        f"branch for {p['slug']} points at '{ref}', which is not in the pool")

    lines = [
        "PROGRESSION DATABASE BUILD (v5)",
        f"v5 rows                     : {stats['rows']}",
        f"catalog entries             : {len(catalog)}",
        f"descriptions merged (old)   : {stats['description_from_old_pool']}",
        f"descriptions hand-written   : {stats['description_from_hand']}",
        f"rows with branches          : {stats['rows_with_branches']}",
        f"rows with ceiling           : {stats['rows_with_ceiling']}",
        "  by match tier             : " + ", ".join(
            f"{k}={v}" for k, v in sorted(match_counts.items())
        ),
        f"entries without description : {stats['no_description']}",
        f"display names generalized   : {len(renames)}",
        f"beginner_build_eligible     : {stats['beginner_build_eligible']}",
        f"app-ready (valid + simple)  : {stats['app_ready']}",
        "",
        "per equipment category (must mirror the app's 8 chips):",
        f"{'':26}" + "  ".join(
            f"{cat} {sum(1 for e in catalog if e['equipment'] == cat)}"
            for cat in EQUIPMENT_CATEGORIES
        ),
        "",
        "per precise muscle:",
    ]
    for muscle in sorted(by_muscle):
        lines.append(f"  {muscle:<18} {by_muscle[muscle]}")

    if problems:
        lines.append("")
        lines.append(f"VALIDATION FAILURES: {len(problems)}")
        lines.extend(f"  - {p}" for p in problems[:200])
        if len(problems) > 200:
            lines.append(f"  ... and {len(problems) - 200} more")
    else:
        lines.append("")
        lines.append("VALIDATION: all rows passed (JSON parses, stage/rep pairs agree, flags coherent)")

    report = "\n".join(lines)
    (OUT / "report_progression.txt").write_text(report, encoding="utf-8")
    (OUT / "catalog_v5.json").write_text(
        json.dumps(catalog, ensure_ascii=False, indent=1), encoding="utf-8")
    (OUT / "name_renames.json").write_text(
        json.dumps(renames, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")
    (OUT / "progression_v5.json").write_text(
        json.dumps(progression, ensure_ascii=False, indent=1), encoding="utf-8")
    with (OUT / "unmatched_v5.csv").open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "exercise_name", "progression_method", "include_in_strength_v1", "reason"])
        writer.writeheader()
        writer.writerows(unmatched)

    print(report)
    print(f"\nwrote {OUT / 'catalog_v5.json'}")
    print(f"wrote {OUT / 'progression_v5.json'}")
    print(f"wrote {OUT / 'unmatched_v5.csv'} ({len(unmatched)} rows)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
