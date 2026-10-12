package com.grafizum.repio.ui.components

import androidx.annotation.DrawableRes
import com.grafizum.repio.R
import com.grafizum.repio.data.WorkoutTemplates

/**
 * The visual vocabulary of the MUSCLE filter, the muscle-side twin of
 * [equipmentCategoryIcon].
 *
 * WHY IT EXISTS. The two filter rows above the exercise list were one control family drawn two
 * ways: equipment showed the chosen kit as 22dp glyphs, and the muscle row showed the chosen group
 * as a bordered text chip. Owner, 2026-10-11: *"i want the equipment way of displaying ... to also
 * have the same style in muscle group 'show more' label. And instead of the muscle group label name
 * it shows also the icon."* The toggles were already identical (measured: same `TextButton`, same
 * `labelSmall`+bold label, same 16dp chevron, same 8dp padding) - the VALUE was the whole
 * difference, so the value is what this file gives an icon to.
 *
 * ONE GLYPH PER BROAD PART, because that is the vocabulary the filter works in: the six parts
 * partition the nineteen precise muscles exactly (`getPreciseMuscleGroupsFor`), so a precise
 * selection ([`"Lats"`]) can be reduced to its part ([`"Back"`]) and still draw the right glyph. That
 * reduction is [WorkoutTemplates.getTopicForMuscle], the same one the colour and the badge already
 * use, so the icon, the colour and the badge can never disagree about which part a muscle belongs
 * to.
 *
 * WHAT THE SIX GLYPHS ARE: one body part each, drawn alone. An arm; a pair of legs; a chest; a back;
 * a core; a pair of shoulders. The first attempt drew all six as a single standing figure with the
 * chosen part marked on it, and the owner rejected it outright - *"absolutely not i want clean muscle
 * group icons of body parts, arm leg etc etc. not some wierd sht ... remake them to actually match
 * with the icons you drew for the equipments"*. So each `ic_muscle_*` drawable is drawn in the kit
 * icons' own lane - stroke only, 1.8 wide, round caps, nothing filled, the same 24dp box on the same
 * 24x24 viewport - and carries no body around it. [MuscleGlyphFamilyTest] holds the lane open: it
 * measures the equipment set's own stroke weights and requires all six to sit inside them.
 *
 * NULL IS A REAL ANSWER, unlike the equipment side's generic `Category` fallback. The muscle row's
 * opening state is "Any" - every muscle - and no single body part draws that. The caller falls back
 * to the word, which is exactly what the equipment row already does when it has no glyph to show
 * ("All equipment", "No equipment needed"). A generic silhouette here would claim a group the user
 * has not chosen.
 */
@DrawableRes
fun muscleGroupIconRes(muscleGroup: String?): Int? =
    when (WorkoutTemplates.getTopicForMuscle(muscleGroup.orEmpty())) {
        "Arms" -> R.drawable.ic_muscle_arms
        "Back" -> R.drawable.ic_muscle_back
        "Chest" -> R.drawable.ic_muscle_chest
        "Core" -> R.drawable.ic_muscle_core
        "Legs" -> R.drawable.ic_muscle_legs
        "Shoulders" -> R.drawable.ic_muscle_shoulders
        // "Other" and blank both land here: an unrecognised muscle (a custom exercise's free-text
        // group) has no part to draw, and guessing one would put a chest glyph on a row filtering
        // by something the app cannot place.
        else -> null
    }
