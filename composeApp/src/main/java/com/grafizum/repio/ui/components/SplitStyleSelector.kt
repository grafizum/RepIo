package com.grafizum.repio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.grafizum.repio.data.WorkoutTemplates
import com.grafizum.repio.data.WorkoutTemplates.SplitStyle
import com.grafizum.repio.util.getString

/**
 * Picks what each training day in the week IS.
 *
 * The day picker answers "how often", which is only half the question - a week
 * of five days can be five upper/lower sessions, a push/pull/legs rotation, or
 * something lopsided, and nothing on screen used to say which one you were
 * getting. Each option therefore spells out the week as the run of days it
 * produces ("Upper · Lower · Upper · Lower · Arms & Core"), read from the
 * layout itself ([WorkoutTemplates.dayLabelIds]) rather than typed into the
 * label, so the line cannot drift away from what the generator actually builds
 * - and so it can be counted against the day picker directly above it.
 *
 * The default is [SplitStyle.DEFAULT]; the "Recommended" wording on it is a
 * judgement, and the week next to every option is the evidence for taking it or
 * not.
 */
@Composable
fun SplitStyleSelector(
    selected: SplitStyle,
    daysPerWeek: Int,
    onSelect: (SplitStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val frequency = daysPerWeek.coerceIn(1, 7)
    val window = LocalWindowSize.current
    Column(
        verticalArrangement = Arrangement.spacedBy(window.sectionSpacing / 2),
        modifier = modifier
    ) {
        SplitStyle.entries.forEach { style ->
            SplitStyleOption(
                style = style,
                selected = style == selected,
                daysPerWeek = frequency,
                onSelect = { onSelect(style) }
            )
        }
    }
}

@Composable
private fun SplitStyleOption(
    style: SplitStyle,
    selected: Boolean,
    daysPerWeek: Int,
    onSelect: () -> Unit
) {
    val title = getString(style.titleKey())
    val description = getString(style.descriptionKey())
    val bestFor = getString(style.bestForKey())
    // Resolved up front: getString is a composable lookup, so it cannot run
    // inside the joinToString lambda below.
    val dayNames = WorkoutTemplates.dayLabelIds(style, daysPerWeek)
        .map { getString(sessionLabelKey(it)) }
    val weekLine = dayNames.joinToString("  ·  ")
    // The leg-day count is read off the layout, never typed: it is the whole
    // difference between the options, so a card that claims one leg day and
    // builds two would be the worst lie on this screen.
    val legDays = WorkoutTemplates.legSessionsPerWeek(style, daysPerWeek)
    val factsLine = listOf(bestFor, getString("split_style_leg_days", legDays)).joinToString("  ·  ")
    // The same lines, in sentences, for a screen reader: the separator glyphs
    // are not read reliably and the whole point of the lines is the order.
    val weekLabel = getString("split_style_week_of", daysPerWeek, weekLine.replace("·", ","))

    Card(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title. $description. $factsLine. $weekLabel" }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (weekLine.isNotEmpty()) {
                Text(
                    text = weekLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = factsLine,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

private fun SplitStyle.titleKey(): String = when (this) {
    SplitStyle.OPTION_A -> "split_style_a_title"
    SplitStyle.OPTION_B -> "split_style_b_title"
    SplitStyle.OPTION_C -> "split_style_c_title"
}

private fun SplitStyle.descriptionKey(): String = when (this) {
    SplitStyle.OPTION_A -> "split_style_a_desc"
    SplitStyle.OPTION_B -> "split_style_b_desc"
    SplitStyle.OPTION_C -> "split_style_c_desc"
}

/** The "best for" line - who an option is for, in the owner's own terms. */
private fun SplitStyle.bestForKey(): String = when (this) {
    SplitStyle.OPTION_A -> "split_style_a_best_for"
    SplitStyle.OPTION_B -> "split_style_b_best_for"
    SplitStyle.OPTION_C -> "split_style_c_best_for"
}

/** The localized name of one session, keyed by [WorkoutTemplates.sessionLabelId]. */
private fun sessionLabelKey(labelId: String): String = when (labelId) {
    WorkoutTemplates.SESSION_FULL_BODY -> "session_full_body"
    WorkoutTemplates.SESSION_UPPER -> "session_upper"
    WorkoutTemplates.SESSION_PUSH -> "session_push"
    WorkoutTemplates.SESSION_PULL -> "session_pull"
    WorkoutTemplates.SESSION_LOWER -> "session_lower"
    // The leg day and an arms-only session are named after the muscle itself,
    // which every language already has a word for.
    WorkoutTemplates.SESSION_LEGS -> "legs"
    WorkoutTemplates.SESSION_ARMS -> "arms"
    WorkoutTemplates.SESSION_ARMS_CORE -> "session_arms_core"
    WorkoutTemplates.SESSION_WEAK_POINT -> "session_weak_point"
    else -> "muscle_group"
}
