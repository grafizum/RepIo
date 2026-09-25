package com.grafizum.repio.ui.components

import com.grafizum.repio.data.ExerciseCatalog

/**
 * Returns the method description for a built-in exercise, or null when unknown.
 *
 * The texts live in the generated [ExerciseCatalog] (rebuilt from the v5
 * progression database); this delegate keeps the historical call sites stable.
 */
fun getExerciseDescription(name: String): String? =
    ExerciseCatalog.descriptionFor(name)
