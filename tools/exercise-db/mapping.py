#!/usr/bin/env python3
"""Mapping decisions for the CSV -> RepIo exercise-catalog rebuild.

Everything here is deliberately data-driven but *reviewable*: when the build
report shows a surprise, fix it by editing the tables in this file — never by
hand-editing the generated Kotlin.

Consumed by build.py; run `python tools/exercise-db/build.py` instead.
"""

from __future__ import annotations

import re

# --------------------------------------------------------------------------
# Equipment: dataset value -> new 8-category taxonomy.
# (Bodyweight, Barbell, Dumbbell, Kettlebell, Cable, Machine, Bands, Other)
# --------------------------------------------------------------------------
EQUIPMENT_MAP = {
    "Body Only": "Bodyweight",
    "Dumbbell": "Dumbbell",
    "Barbell": "Barbell",
    "E-Z Curl Bar": "Barbell",
    "Cable": "Cable",
    "Machine": "Machine",
    "Kettlebells": "Kettlebell",
    "Bands": "Bands",
    "Medicine Ball": "Other",
    "Exercise Ball": "Other",
    "Foam Roll": "Other",
    "Other": "Other",
    "": "Unknown",  # resolved from the name by equipment_from_name(), else dropped
}

NEW_EQUIPMENT_CATEGORIES = [
    "Bodyweight", "Barbell", "Dumbbell", "Kettlebell",
    "Cable", "Machine", "Bands", "Other",
]

_EQUIPMENT_NAME_HINTS = [
    ("kettlebell", "Kettlebell"), ("dumbbell", "Dumbbell"), ("barbell", "Barbell"),
    ("ez bar", "Barbell"), ("e-z", "Barbell"), ("cable", "Cable"), ("smith", "Machine"),
    ("machine", "Machine"), ("band", "Bands"), ("medicine ball", "Other"),
    ("exercise ball", "Other"), ("swiss ball", "Other"), ("stability ball", "Other"),
    ("foam roll", "Other"), ("landmine", "Barbell"), ("body only", "Bodyweight"),
]


def equipment_from_name(title: str) -> str:
    t = title.lower()
    for hint, equipment in _EQUIPMENT_NAME_HINTS:
        if hint in t:
            return equipment
    return "Unknown"


# --------------------------------------------------------------------------
# Body part: dataset value -> RepIo precise muscle (the join key the app uses).
# --------------------------------------------------------------------------
BODYPART_MAP = {
    "Abdominals": "Abs",
    "Quadriceps": "Quadriceps",
    "Hamstrings": "Hamstrings",
    "Glutes": "Glutes",
    "Chest": "Pectorals",
    "Biceps": "Biceps",
    "Triceps": "Triceps",
    "Lats": "Lats",
    "Middle Back": "Upper Back",
    "Lower Back": "Spinal Erectors",
    "Calves": "Calves",
    "Forearms": "Forearms",
    "Traps": "Upper Traps",
    "Abductors": "Hip Abductors",
    "Adductors": "Hip Adductors",
    "Neck": None,  # no matching muscle in the app; 8 rows, dropped
}

# Dataset "Shoulders" has no precise split: resolve per name, default Front Deltoids.
SHOULDER_FALLBACK = "Front Deltoids"

# --------------------------------------------------------------------------
# Broad body part derived from the precise muscle (mirrors getTopicForMuscle).
# --------------------------------------------------------------------------
BROAD_FROM_PRECISE = {
    "Pectorals": "Chest",
    "Lats": "Back", "Upper Back": "Back", "Upper Traps": "Back", "Spinal Erectors": "Back",
    "Quadriceps": "Legs", "Hamstrings": "Legs", "Glutes": "Legs", "Calves": "Legs",
    "Hip Adductors": "Legs", "Hip Abductors": "Legs",
    "Front Deltoids": "Shoulders", "Lateral Deltoids": "Shoulders", "Rear Deltoids": "Shoulders",
    "Biceps": "Arms", "Triceps": "Arms", "Forearms": "Arms",
    "Abs": "Core", "Obliques": "Core",
}

# --------------------------------------------------------------------------
# Name junk filters (from the Phase 0 profile).
# --------------------------------------------------------------------------
BRAND_PREFIXES = [
    "FYR2 ", "FYR ", "HM ", "AM ", "UNS ", "KV ", "TBS ",
    "MetaBurn ", "Holman ", "Tyler Holt ", "Dumbbell Fix ", "30 ",
    "Paul Carter ", "King Maker ",
]
VARIATION_SUFFIXES = [" - Gethin Variation", " - Variation Gethin", " - With Bands"]
# Titles that are pure plan-noise even after prefix stripping.
DROP_TITLES_EXACT = {"roc everest"}

# Rows enter the pool only with a description or a community rating.
KEEP_IF = lambda desc, rating: (isinstance(desc, str) and desc.strip() != "") or (rating and rating > 0)

# --------------------------------------------------------------------------
# Muscle auto-fix: keyword rules for rows whose BodyPart contradicts the name.
# Order matters — core keywords win (a "plank row" stays Abs), then specific
# movement patterns map to a precise muscle. Only applied when the assigned
# BodyPart has NO core keyword but the name DOES name a specific movement —
# i.e. exactly the "kettlebell swing under Abdominals" class of error.
# --------------------------------------------------------------------------
_CORE_KEYWORDS = [
    "plank", "crunch", "sit-up", "sit up", "leg raise", "knee raise", "toes-to-bar",
    "toe touch", "toe-touch", "twist", "hollow", "v-up", "v up", "flutter", "sprawl",
    "burpee", "mountain climber", "bear crawl", "crawl", "dead bug", "bird dog",
    "ab wheel", "ab roll", "roll-out", "rollout", "core", " ab ", " abs", "abdominal",
    "oblique", "side bend", "jackknife", "hip raise", "leg lift", "knee-in", "chinn",
    "curl-up", "curl up", "sledgehammer",
]

# (pattern, precise muscle) — first match wins, checked case-insensitively.
_MOVEMENT_KEYWORDS = [
    (r"lower back", "Spinal Erectors"),
    (r"leg curl", "Hamstrings"),
    (r"hip thrust|glute bridge|frog pump", "Glutes"),
    (r"swing", "Hamstrings"),
    (r"deadlift|good morning|back ext", "Spinal Erectors"),
    (r"leg press|hack squat|sissy squat|squat", "Quadriceps"),
    (r"lunge|step-up", "Quadriceps"),
    (r"pulldown|pull-up|pull up|chin-up|pull over|pullover", "Lats"),
    (r"\brow\b|renegade", "Upper Back"),
    (r"shrug", "Upper Traps"),
    (r"face pull|rear delt|reverse fly", "Rear Deltoids"),
    (r"lateral raise|side delt|upright row", "Lateral Deltoids"),
    (r"front raise|overhead press|shoulder press|military press|arnold|landmine press", "Front Deltoids"),
    (r"fly|flye|bench press|floor press|chest press|push-up|push up|pec ", "Pectorals"),
    (r"skullcrusher|pushdown|tricep|kickback|jm press", "Triceps"),
    (r"\bcurl\b", "Biceps"),
    (r"calf", "Calves"),
    (r"wrist|farmer|grip", "Forearms"),
]


def strip_title_junk(title: str) -> tuple[str, bool]:
    """Strip brand prefixes, variation suffixes and trailing punctuation.

    Returns (clean_title, changed).
    """
    t = title.strip()
    original = t
    for suffix in VARIATION_SUFFIXES:
        if t.lower().endswith(suffix.lower()):
            t = t[: -len(suffix)].strip()
    t = t.rstrip("- ").strip()
    changed = True
    while changed:
        changed = False
        for prefix in BRAND_PREFIXES:
            if t.lower().startswith(prefix.lower()):
                t = t[len(prefix):].strip()
                changed = True
    # Collapse whitespace and stray double hyphens left behind.
    t = re.sub(r"\s{2,}", " ", t)
    return t, t != original


def core_keyword_hit(title: str) -> bool:
    t = f" {title.lower()} "
    return any(k in t for k in _CORE_KEYWORDS)


def movement_muscle(title: str) -> str:
    """The precise muscle the movement pattern in the name implies, or None."""
    t = title.lower()
    for pattern, muscle in _MOVEMENT_KEYWORDS:
        if re.search(pattern, t):
            return muscle
    return None


def movement_muscle_for_dump(title: str) -> str | None:
    """Reassignment candidate for rows the dataset dumped under 'Abdominals'.

    The Abs rows are the dataset's notorious dumping ground (kettlebell swings,
    hip thrusts and biceps curls all sit there), so a movement pattern that
    clearly points OUTSIDE the core wins. For every other body part the dataset
    label is treated as the primary mover — combo names like 'Biceps Curl in
    Squat Position' name an incidental pattern, not the target muscle."""
    return movement_muscle(title)


# The dataset has no obliques bucket — all rotational/side-bend work sits under
# Abdominals. These patterns carve the true oblique work back out so the app's
# Obliques muscle (badge + analytics) is populated with the right movements.
_OBLIQUE_PATTERN = re.compile(
    r"oblique|twist|side bend|woodchop|wood chop|rotational|rotation|side plank"
)


def oblique_refinement(title: str) -> bool:
    """True when an Abdominals-row name describes oblique-dominant work."""
    return bool(_OBLIQUE_PATTERN.search(title.lower()))


# High-confidence overrides: single-joint movements whose keyword IS the target
# muscle, regardless of the dataset's (sometimes plainly wrong) body part —
# e.g. the CSV files a branded dumbbell-shrug duplicate under Chest. Deliberately
# narrow: multi-joint names (squat, curl, row, press, swing) are NOT here
# because their keyword is often only an incidental pattern.
_MUSCLE_OVERRIDES = [
    # Dataset BodyPart mislabels verified against the movement:
    (r"inverted row", "Upper Back"),           # dataset says Front Deltoids
    (r"leg extensions?", "Quadriceps"),        # dataset files it under Hamstrings
    (r"hip adduction|hip adductor", "Hip Adductors"),  # dataset says Quadriceps
    (r"\bchin-?ups?\b", "Lats"),               # a chin-up is a pull first (secondary: biceps)
    (r"\bshrugs?\b|shrug", "Upper Traps"),
    (r"pull-over|pullover", "Lats"),
    (r"face pull|rear delt", "Rear Deltoids"),
    (r"lateral raise|side delt", "Lateral Deltoids"),
    (r"hip thrust|glute bridge", "Glutes"),
    (r"\bcalf\b|calf raise", "Calves"),
    (r"wrist", "Forearms"),
]


def muscle_override(title: str) -> str | None:
    t = title.lower()
    for pattern, muscle in _MUSCLE_OVERRIDES:
        if re.search(pattern, t):
            return muscle
    return None


def shoulder_precision(title: str) -> str:
    """Split the umbrella 'Shoulders' body part by name keywords."""
    t = title.lower()
    if re.search(r"rear delt|reverse fly|face pull|bent-over", t):
        return "Rear Deltoids"
    if re.search(r"lateral raise|side delt|upright", t):
        return "Lateral Deltoids"
    return SHOULDER_FALLBACK
