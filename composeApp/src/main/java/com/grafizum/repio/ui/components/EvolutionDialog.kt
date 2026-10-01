package com.grafizum.repio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grafizum.repio.R
import com.grafizum.repio.data.ProgressionEngine

/**
 * The evolution offer, drawn as "what it was" and "what it can become".
 *
 * The owner asked for the progression window to "show the upgrade like an
 * evolution - first show what the exercise was before and then show what it
 * can become (graphically informative)". There are no exercise images in the
 * app, so the picture is drawn from the numbers themselves: each side is its
 * own ladder, filled to where the user actually stands, with the real level,
 * stage, load and reps printed on it. That is honest in a way a stock photo
 * would not be - the bars carry the same values `ProgressionEngine` hands the
 * prescriptions, so the popup cannot promise a start level the next session
 * would not deliver.
 *
 * The two bars are deliberately NOT the same bar. The "before" side is drawn
 * full and muted - it has been climbed and it is spent. The "after" side is
 * drawn from the mapped start level with its future still open. A user who
 * reads only the shapes sees a complete track give way to a fresh one, which
 * is the whole argument the popup is making.
 *
 * It renders through [AppDialog] like every other popup in the app, so it
 * shares the container, corner radius, width cap and button row.
 */
@Composable
fun EvolutionDialog(
    offer: ProgressionEngine.EvolutionOffer,
    onEvolve: () -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.evolution_title),
        dismissLabel = stringResource(R.string.evolution_not_now),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.evolution_accept),
        onConfirm = onEvolve,
    ) {
        Text(
            text = stringResource(R.string.evolution_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            EvolutionSideCard(
                side = offer.from,
                heading = stringResource(R.string.evolution_heading_before),
                spent = true,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            EvolutionSideCard(
                side = offer.to,
                heading = stringResource(R.string.evolution_heading_after),
                spent = false,
                modifier = Modifier.weight(1f),
            )
        }

        if (offer.note.isNotBlank()) {
            Text(
                text = offer.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // What declining actually does, said BEFORE the user does it. The
        // lock is permanent, so this is the one line that must not be vague.
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.evolution_decline_warns),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        TextButton(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.evolution_decline),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * One side of the offer: the exercise name, its ladder bar, and the numbers
 * that fill it. [spent] draws the track full and muted - that is the "before"
 * side, whose whole point is that there is nothing left on it.
 */
@Composable
private fun EvolutionSideCard(
    side: ProgressionEngine.EvolutionSide,
    heading: String,
    spent: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (spent) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.border(
            width = 1.dp,
            color = accent.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
            Text(
                text = translateExerciseName(side.exerciseName),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = accent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        // Never zero-width: a bar that renders as nothing reads
                        // as "no data", not as "you have just started".
                        .fillMaxWidth(side.progressFraction.coerceAtLeast(0.04f))
                        .height(8.dp)
                        .background(color = accent, shape = RoundedCornerShape(4.dp)),
                )
            }
            Text(
                text = stringResource(R.string.evolution_level_of_max, side.level, side.maxLevel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (side.reps.isNotEmpty()) {
                Text(
                    text = side.reps.joinToString(" / ") + " " + stringResource(R.string.reps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!spent && side.nodes.isNotEmpty()) {
                // translateExerciseName is @Composable, so the names are
                // resolved before the join rather than inside its lambda.
                val ahead = side.nodes.take(2).map { translateExerciseName(it) }
                Text(
                    text = stringResource(R.string.evolution_path_ahead) + " " +
                        ahead.joinToString(" › "),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}

/**
 * A compact badge for an exercise whose evolution offer was declined: the
 * editor shows it so the lock is visible and, tapped, reopenable. A lock the
 * user cannot see is a lock they will not think to go looking for.
 */
@Composable
fun EvolutionLockedBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.evolution_locked_badge),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
