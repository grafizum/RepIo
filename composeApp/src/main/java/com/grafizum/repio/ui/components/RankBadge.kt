package com.grafizum.repio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.data.OverallLevelRank
import com.grafizum.repio.util.getQuantityString
import com.grafizum.repio.util.getString
import kotlin.math.roundToInt

/**
 * The letter rank as type, not a badge: the bare letter(s) in a cursive-flavoured
 * serif, painted in the theme's primary colour. The old metal badges (stone →
 * platinum) were removed — the letter itself carries the rank now, and the
 * script style reads as a grade rather than a medal.
 *
 * [contentDescription] should be the spoken form ("Rank S"): the bare letter is
 * meaningless to a screen reader on its own.
 */
@Composable
fun RankBadge(
    rank: OverallLevelRank.Letter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp
) {
    // The letter shrinks as the label grows, so "SS+" fits the same box as "S".
    val glyphScale = when {
        rank.label.length >= 3 -> 0.34f
        rank.label.length == 2 -> 0.42f
        else -> 0.52f
    }
    val fontSize = with(LocalDensity.current) { (size * glyphScale).toSp() }

    // Serif (Cursive is not shipped on all devices and falls back unpredictably;
    // Serif with italic weight gives the same "grade on a report card" read and
    // is always present).
    val style = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center
    )

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { this.contentDescription = contentDescription.orEmpty() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rank.label,
            color = MaterialTheme.colorScheme.primary,
            style = style,
            maxLines = 1
        )
    }
}

/**
 * The long-press explanation behind an Overall Level card: the raw number with
 * its rank, how many exercises produced it, and how far the next rank is.
 *
 * Shared by Home and Analytics so the two cards cannot explain the same stat two
 * different ways, and because the five-exercise rule is not self-evident from a
 * bare "—" either.
 */
@Composable
fun OverallLevelTooltip(
    level: Double?,
    rank: OverallLevelRank.Info?,
    exerciseCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (level != null && rank != null) {
            Text(
                text = getString("rank_tooltip_level", level, rank.letter),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = getQuantityString("overall_level_exercises_pl", exerciseCount, exerciseCount)
            )
            Text(
                text = if (rank.isTop) {
                    getString("rank_tooltip_top")
                } else {
                    getString(
                        "rank_tooltip_progress",
                        (rank.progressToNext * 100).roundToInt(),
                        rank.nextLetter.orEmpty()
                    )
                }
            )
        }
        // The five-exercise rule only matters while there is no number to explain;
        // once there is one, the reader wants to know what the number means.
        Text(
            text = if (level != null && rank != null) {
                getString("overall_level_short")
            } else {
                getString("overall_level_help")
            }
        )
    }
}
