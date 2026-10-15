package com.grafizum.repio.data

/**
 * An RPE band and the reps in reserve it means.
 *
 * RPE and RIR are the same scale read from opposite ends — RPE 8 is 2 reps left in the tank — so
 * the pair is stored once as the RPE band and the reserve is DERIVED ([rirLow]/[rirHigh]) rather
 * than carried beside it. A band is a range because a stored intensity can be one ("RPE 8–9");
 * [isRange] is false for the single value, which is the common case.
 */
data class RpeRir(val low: Int, val high: Int) {
    val rirLow: Int get() = (10 - high).coerceIn(0, 10)
    val rirHigh: Int get() = (10 - low).coerceIn(0, 10)
    val isRange: Boolean get() = low != high
}

/**
 * The RPE/RIR figure an exercise's stored intensity word means, or null when it means none.
 *
 * Owner prompt, 2026-10-15: *"we have RPE and RIR inside the settings instructions ... i want it
 * to be integrated in the workout creation or exercises (just as labels for users nothing more)
 * find out a way to integrate it seemlessly without needing to change so much."*
 *
 * THE WORDS ARE ALREADY THERE, which is what makes this seamless: every planned row carries an
 * intensity string — the templates write "Moderate", "Heavy", "Very Heavy", "Light",
 * "To Failure" — and until now only a row that literally said "RPE 8" got an explanation
 * ([com.grafizum.repio.ui.components.getRpeRirExplanation] matches the three characters "RPE"),
 * so the words that are actually stored showed a bare adjective. Nothing new is entered by the
 * user and nothing new is saved: this function reads the word that is already on the row.
 *
 * IT IS A TABLE, NOT A GUESS. The keys are the exact strings the resolvers in
 * [WorkoutTemplates.getGoalIntensity] and the setup flow write, so a value either has a figure
 * here or is honestly unknown. An unknown word returns null and the caller shows the word alone
 * — the behaviour before this existed — rather than inventing a number for it (a user typing
 * "Beast mode" must not be told it means RPE 7).
 *
 * A literal RPE/RIR expression is read for its number, including the reverse direction
 * ("8 RPE") and the reserve direction ("2 RIR" is RPE 8), because those are the other two ways
 * the field is filled in by hand.
 */
fun rpeRirFor(intensity: String): RpeRir? {
    val text = intensity.trim().lowercase()
    if (text.isEmpty()) return null

    // "rpe 8-9" / "rpe 8" — the shape the app's own RPE explanations already match.
    //
    // The trailing lookahead is what keeps a HALF step out: without it "rpe 8.5" matches the
    // "8" and the row would be labelled a whole RPE below what it says. A value this reader
    // cannot express exactly is reported as unknown, and unknown changes nothing on screen.
    Regex("""rpe\s*(\d+)\s*[-–]\s*(\d+)(?!\d|\.)""").find(text)?.let { m ->
        return band(m.groupValues[1], m.groupValues[2])
    }
    Regex("""rpe\s*(\d+)(?!\d|\.)""").find(text)?.let { m ->
        return band(m.groupValues[1], m.groupValues[1])
    }
    // "8-9 rpe" / "8 rpe" — the same thing written the other way round.
    Regex("""(\d+)\s*[-–]\s*(\d+)\s*rpe""").find(text)?.let { m ->
        return band(m.groupValues[1], m.groupValues[2])
    }
    Regex("""(\d+)(?!\d|\.)\s*rpe""").find(text)?.let { m ->
        return band(m.groupValues[1], m.groupValues[1])
    }
    // "2 rir" / "2 reps in reserve" — stated from the reserve end, so the RPE is 10 - n.
    Regex("""(\d+)\s*(?:rir|reps?\s+in\s+reserve)""").find(text)?.let { m ->
        val reserve = m.groupValues[1].toIntOrNull() ?: return null
        return RpeRir(low = 10 - reserve, high = 10 - reserve)
    }

    return WORDS[text]
}

/** One band from two matched digit groups, refusing an unparseable or inverted pair. */
private fun band(low: String, high: String): RpeRir? {
    val l = low.toIntOrNull() ?: return null
    val h = high.toIntOrNull() ?: return null
    if (l > h) return null
    return RpeRir(low = l, high = h)
}

/**
 * The words the app writes into `targetIntensity`, and the effort each one means.
 *
 * `To Failure` / `Very Heavy` / `Heavy` / `High Intensity` / `Moderate` / `Light` are the values
 * [WorkoutTemplates.getGoalIntensity] returns — including the `goal_strength` and
 * `goal_bodybuilding` branches, which is where "Very Heavy" and "To Failure" come from. The
 * difficulty axis itself (Easy / Medium / Hard) is here too, because the setup flow stores the
 * difficulty word on rows it builds before a goal is picked.
 *
 * Deliberately NOT contains-based: "Very Heavy" contains "heavy", so a substring rule would
 * need an order, and an order is the kind of thing that silently flips. Exact keys have one
 * answer each.
 */
private val WORDS: Map<String, RpeRir> = mapOf(
    "to failure" to RpeRir(10, 10),
    "failure" to RpeRir(10, 10),
    "absolute failure" to RpeRir(10, 10),
    "very heavy" to RpeRir(9, 9),
    "heavy" to RpeRir(8, 8),
    "high intensity" to RpeRir(8, 8),
    "hard" to RpeRir(8, 8),
    "moderate" to RpeRir(7, 7),
    "medium" to RpeRir(7, 7),
    "moderate intensity" to RpeRir(7, 7),
    "medium intensity" to RpeRir(7, 7),
    "light" to RpeRir(6, 6),
    "easy" to RpeRir(6, 6),
    "low intensity" to RpeRir(6, 6)
)
