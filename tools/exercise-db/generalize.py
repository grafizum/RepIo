#!/usr/bin/env python3
"""Generalized exercise names for the built-in pool.

Owner task (2026-09-28): "Names like 'smith machine' at the front of a simple
'bent over row' is too much. Thats a specification not needed ... change the
complete database so the exercises have a generalization."

The catalog already carries the apparatus in its own `equipment` field, so a
leading equipment word in the NAME is a second copy of data the row already
has - "Smith Machine Bent Over Row" is a bent-over row, and the fact that the
pool will tag it as a machine is a separate fact.

Only EQUIPMENT words are stripped. Angle, grip, posture and support words are
part of what the movement IS and are left alone: a close-grip press and a
wide-grip press are two different exercises, and "Close-Grip Bench Press" ->
"Bench Press" would be a false generalization, not a clearer name.

Stripping alone is not enough, because the pool is joined BY NAME
(exerciseEquipment, exercisePrimaryMuscles, the logged sets and the built-in
templates all agree on it). Six different squats would all become "Squat" and
silently merge into one exercise. So where the stripped name is not unique, the
apparatus is kept as a short trailing qualifier:

    Smith Machine Squat   -> Squat (Smith Machine)
    Dumbbell Squat        -> Squat (Dumbbell)
    Smith Machine Hip Thrust -> Hip Thrust (Smith Machine)

A name that no other row competes with is left completely bare
("Smith Machine Bent Over Row" -> "Bent Over Row"). A row whose name is already
unprefixed keeps its name, and the variants are qualified around it.
"""

from __future__ import annotations

import re
from collections import defaultdict

# Equipment words that lead a name and are already recorded in `equipment`.
# Deliberately NOT here: incline/decline/seated/standing/lying, grip widths,
# single-arm, sumo/nordic, isometric/paused/tempo, box jump, medicine ball,
# sled, rope. Those describe the movement, not the kit.
APPARATUS_PREFIXES = [
    "Smith Machine",
    "Machine",
    "Barbell",
    "Dumbbell",
    "Kettlebell",
    "Cable",
    "Banded",
    # The source dataset spells some rows "Band ..." rather than "Banded ...".
    # Without this, "Band Assisted Pull Ups" kept a leading apparatus word the
    # rule was supposed to move into a qualifier.
    "Band",
    "EZ-Bar",
    "EZ Bar",
    "Trap Bar",
    "Stability Ball",
    "TRX",
    "Bodyweight",
]

# What each apparatus word becomes inside a "(...)" qualifier.
QUALIFIER_NAMES = {
    "Smith Machine": "Smith Machine",
    "Machine": "Machine",
    "Barbell": "Barbell",
    "Dumbbell": "Dumbbell",
    "Kettlebell": "Kettlebell",
    "Cable": "Cable",
    "Banded": "Band",
    "Band": "Band",
    "EZ-Bar": "EZ-Bar",
    "EZ Bar": "EZ-Bar",
    "Trap Bar": "Trap Bar",
    "Stability Ball": "Ball",
    "TRX": "TRX",
    "Bodyweight": "Bodyweight",
}


# Names of this many words or fewer keep their apparatus where it is (owner
# rule, 2026-10-01): "Dumbbell Row", "Cable Fly", "Banded Clamshell".
SHORT_NAME_MAX_WORDS = 3


def split_apparatus(name: str) -> tuple[list[str], str]:
    """Peel leading apparatus words off [name].

    Returns (apparatus words in order, the remaining movement name). The
    original name is returned as the movement when nothing peels off.
    """
    found: list[str] = []
    rest = name.strip()
    changed = True
    while changed:
        changed = False
        for lead in APPARATUS_PREFIXES:
            if rest.lower().startswith(lead.lower() + " "):
                found.append(lead)
                rest = rest[len(lead) + 1:].strip()
                changed = True
    rest = rest.strip(" -")
    if not rest:
        # "Dumbbell" alone, or "Bodyweight" alone: nothing left to keep, so the
        # row keeps its original name rather than collapsing to an empty string.
        return [], name.strip()
    return found, rest


def qualifier_for(apparatus: list[str]) -> str:
    seen: list[str] = []
    for word in apparatus:
        label = QUALIFIER_NAMES.get(word, word)
        if label not in seen:
            seen.append(label)
    return ", ".join(seen)


def collision_key(movement: str) -> str:
    """Punctuation-insensitive identity of a movement name.

    "Bent-Over Row" and "Bent Over Row" are the same name written two ways, and
    the translation tables are keyed by slug - slugify() maps both to
    "bent_over_row". Bucketing on this key is what stops the strip from
    producing two rows the rest of the app cannot tell apart.
    """
    return re.sub(r"[^a-z0-9]+", " ", movement.lower()).strip()


def build_rename_map(names: list[str]) -> dict[str, str]:
    """old name -> new name for every row whose display name changes.

    Raises when two rows would end up with the same name: the pool is joined by
    name, so a duplicate is a data-corruption bug and must stop the build rather
    than ship.
    """
    stripped: dict[str, list[str]] = defaultdict(list)
    parts: dict[str, tuple[list[str], str]] = {}
    for name in names:
        apparatus, movement = split_apparatus(name)
        parts[name] = (apparatus, movement)
        stripped[collision_key(movement)].append(name)

    rename: dict[str, str] = {}
    for name in names:
        apparatus, movement = parts[name]
        if not apparatus:
            continue  # already a plain movement name
        # A SHORT name already reads naturally with the apparatus in front, and
        # splitting it makes it worse, not clearer: "Dumbbell Row" -> "Row
        # (Dumbbell)" says less and looks silly. Owner rule (2026-10-01).
        if len(name.split()) <= SHORT_NAME_MAX_WORDS:
            continue
        # Everything longer carries the apparatus as a trailing qualifier. The
        # earlier build DROPPED it entirely whenever the stripped movement was
        # unique, on the reasoning that `equipment` already records it. That was
        # wrong for a user reading a name: "Bent Over Row" does not say whether it
        # is a cable row, a smith-machine row or a barbell row, and those are
        # three different exercises with three different ladders and three
        # different equipment gates. The qualifier is what keeps a machine
        # movement and the same movement with other kit as separate rows.
        rename[name] = f"{movement} ({qualifier_for(apparatus)})"

    seen: dict[str, str] = {}
    for name in names:
        target = rename.get(name, name)
        key = collision_key(target)
        if key in seen:
            raise ValueError(
                f"rename collision: '{seen[key]}' and '{name}' would both be '{target}'"
            )
        seen[key] = name
    return rename


def format_rename_table(renames: dict[str, str]) -> str:
    """A reviewable Kotlin map of old name -> new name, for logged-set repair."""
    if not renames:
        return "        // no renames\n"
    lines = ["        // Generated by tools/exercise-db/build_progression.py.",
             "        // Old display name -> current display name. A routine logged before",
             "        // the generalization still stores the old name, and the app resolves it",
             "        // through here so those sets keep their exercise, equipment and",
             "        // muscle instead of dropping out of the pool.",
             "        val RENAMED: Map<String, String> = mapOf("]
    for old in sorted(renames):
        lines.append(f'        "{old}" to "{renames[old]}",')
    lines.append("        )")
    return "\n".join(lines) + "\n"
