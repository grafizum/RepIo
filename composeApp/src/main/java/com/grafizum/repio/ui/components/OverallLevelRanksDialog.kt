package com.grafizum.repio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grafizum.repio.data.OverallLevelRank
import com.grafizum.repio.util.getString

/**
 * The whole rank scale, in the one unit the user can act on: LEVELS.
 *
 * The owner asked (2026-10-06) for the Overall Level card to open a table that
 * "shows min (lvl 1) to max … and the ranks range based on each level range".
 * The ranks ARE percent bands (see [OverallLevelRank]); a level is what the card
 * prints and what a user sees moving, so the table translates each band into the
 * level range it covers. Nothing is written down twice: the ranges are DERIVED
 * from `minPercent` / `rangeEnd`, so adding a rank cannot leave a gap or an
 * overlap in the table. `OverallLevelRankTest` sweeps for exactly that.
 *
 * The ranges are deliberately not typed out as literals. The owner's example
 * ("1 to 20 is E, 20 to 30 is D") is an illustration of the SHAPE, and hard-coding
 * a second copy of the scale is how the card and the table start disagreeing.
 * The numbers come from [OverallLevelRank.levelRangeOf], which is where the
 * "no gap, no overlap" property is tested.
 */
@Composable
fun OverallLevelRanksDialog(
    currentLevel: Int?,
    currentRank: OverallLevelRank.Info?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = getString("rank_system_guide_title"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Where the user is right now, so the table is a map rather than
                // a list: the row they are on is marked, and the header repeats
                // the level the card showed them.
                if (currentLevel != null && currentRank != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(
                            rank = currentRank.rank,
                            contentDescription = getString(
                                "rank_badge_content_desc",
                                currentRank.letter
                            ),
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = getString("rank_tooltip_level", currentRank.letter) +
                                " · " + getString("overall_level_value_format", currentLevel),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OverallLevelRank.ORDERED.forEach { letter ->
                    val isCurrent = letter == currentRank?.rank
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            RankBadge(
                                rank = letter,
                                contentDescription = null,
                                size = 34.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rankLevelRangeLabel(letter),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = getString("level_details_current"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = getString(letter.descriptionKey),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(getString("done"))
            }
        }
    )
}

/**
 * The level range a rank covers, as the user reads levels: "Lvl. 1 – Lvl. 13",
 * and "Lvl. 95+" for the open top band.
 *
 * [OverallLevelRank.displayLevel] is the same mapping the card uses, so the
 * number on the card and the number at the top of its band are the same number
 * by construction.
 */
@Composable
fun rankLevelRangeLabel(letter: OverallLevelRank.Letter): String {
    val range = OverallLevelRank.levelRangeOf(letter)
    val start = getString("overall_level_value_format", range.first)
    val last = range.last
    return if (last == null) {
        getString("rank_range_open", start)
    } else {
        getString("rank_range", start, getString("overall_level_value_format", last))
    }
}
