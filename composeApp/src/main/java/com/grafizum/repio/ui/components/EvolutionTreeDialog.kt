package com.grafizum.repio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grafizum.repio.R
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.viewmodel.MasteryCardData
import com.grafizum.repio.util.getString
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * How much further right than the trunk the progression cards sit.
 *
 * The owner's number: "the progression exercise to upgrade it to be under it
 * like it is but more offset lets say x there would be 10px". It is a NAMED step
 * rather than a bare 10 because it is also what the connector hangs back from -
 * the spine is drawn half a step in from the branch lane, which is what keeps
 * the elbow visible instead of collapsing it into the card's own edge.
 *
 * Ten is a step, not the whole indent: the branch lane is the dialog's own
 * gutter PLUS this, so the cards still clear the spine on a 320dp phone.
 */
private val BRANCH_INSET_STEP = 10.dp

/**
 * The end-of-workout evolution popup, drawn as a TREE.
 *
 * The owner rejected three flat presentations (two side-by-side bars, a list,
 * an inline card) as uninformative, and asked for "graphical information as a
 * popup at the end of the workout". So the picture IS the content: the exercise
 * just mastered is the trunk, every established way on branches off it through
 * a drawn elbow, and the selected branch trails off into buds naming what it
 * becomes after that.
 *
 * Two things make it information rather than decoration, and both are why the
 * lines are drawn from MEASURED node positions rather than from a guessed
 * layout:
 *
 * - **Every number comes from [ProgressionEngine].** The level, the ladder it
 *   runs on, the load and the reps are the same values the next prescription is
 *   built from, so the popup cannot promise a start level the session would not
 *   deliver.
 * - **The buds are the destination's own future path** (`nodes`), so the card
 *   shows the fork AND where the fork leads. A choice that dead-ends looks like
 *   a choice that dead-ends.
 *
 * The elbows are a [Path] with cubic control points, which is what makes them
 * read as a branch rather than as a stack of unrelated rows: every leg leaves
 * the trunk at the SAME point, so the trunk visibly forks.
 */
@Composable
fun EvolutionTreeDialog(
    card: MasteryCardData,
    offers: List<ProgressionEngine.EvolutionOffer>,
    onEvolve: (targetName: String) -> Unit,
    onDecline: () -> Unit,
    onDismiss: () -> Unit,
) {
    val window = LocalWindowSize.current
    val colors = MaterialTheme.colorScheme

    // The offer the popup opens on. `preferredEvolutionOffer` in the view model
    // already picks the coach's preferred branch, and `offers` is built in the
    // same order, so index 0 is that choice rather than an arbitrary one.
    var selected by remember(card.exerciseName) { mutableStateOf(0) }
    val chosen = offers.getOrNull(selected) ?: offers.firstOrNull()

    // Geometry, drawn from the window tokens rather than fixed dp so the gutter
    // and the cards scale with the rest of the dialog.
    //
    // THE TRUNK SITS AT x = 0 and every progression sits BELOW it, one step
    // further right (owner, 2026-10-05: "the current exercise shown in the popup
    // to be lets say x 0px, and the progression exercise to upgrade it to be
    // under it like it is but more offset lets say x there would be 10px, so its
    // under and shifted to the right slightly").
    //
    // Both cards used to be indented by the SAME gutter, which is why the two
    // states of the diagram were told apart only by the lines between them. A
    // parent that is flush with its own content edge, and children that step in
    // from it, read as parent and children before a single pixel of connector is
    // drawn — the indent IS the hierarchy, and the lines only confirm it.
    //
    // The spine therefore has to move too: it used to run down the empty gutter
    // to the LEFT of the trunk, which is not where the trunk is any more. It
    // now hangs [BRANCH_INSET_STEP] in from the branch lane, so the drop leaves
    // the trunk's own width and the arm runs right into each branch's edge.
    val gutter = window.cardPadding * 1.8f
    val trunkInset = 0.dp
    val branchInset = gutter + BRANCH_INSET_STEP
    // The spine's x, measured from the canvas's left edge (which is the trunk's
    // left edge, because the trunk is flush).
    val spineInset = branchInset - BRANCH_INSET_STEP / 2f

    var sourceRect by remember { mutableStateOf<Rect?>(null) }
    var rowRects by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var canvasOrigin by remember { mutableStateOf(Offset.Zero) }

    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.evolution_title),
        confirmLabel = stringResource(R.string.evolution_accept),
        onConfirm = { chosen?.let { onEvolve(it.targetName) } },
        confirmEnabled = chosen != null,
        dismissLabel = stringResource(R.string.evolution_not_now),
        onDismiss = onDismiss,
    ) {
        Text(
            text = stringResource(R.string.evolution_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            TreeElbows(
                sourceRect = sourceRect,
                rowRects = rowRects,
                origin = canvasOrigin,
                spineInset = spineInset,
                color = colors.outline,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { canvasOrigin = it.positionInRoot() },
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(window.dialogGap / 2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TrunkCard(
                    card = card,
                    inset = trunkInset,
                    modifier = Modifier.onGloballyPositioned { sourceRect = it.boundsInRoot() },
                )

                offers.forEachIndexed { index, offer ->
                    val isSelected = index == selected
                    BranchCard(
                        offer = offer,
                        selected = isSelected,
                        inset = branchInset,
                        onClick = { selected = index },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val rect = coords.boundsInRoot()
                            rowRects = rowRects
                                .let { old ->
                                    (old.indices.maxOfOrNull { i -> if (i < index) i else -1 } ?: -1)
                                        .let { keep -> old.take(keep + 1) }
                                } + rect
                        },
                    )
                }
            }
        }

        if (chosen != null && chosen.note.isNotBlank()) {
            Text(
                text = chosen.note,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        // What declining does, said BEFORE the user does it. The lock is
        // permanent, so this must not be vague.
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant,
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
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.evolution_decline_warns),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The trunk: the exercise that was just finished.
 *
 * Its ladder is drawn FULL and muted on purpose - it has been climbed and there
 * is nothing left on it. That is the whole premise of the popup, and it is why
 * the branches below are drawn in the primary colour.
 *
 * It sits flush at x = 0 (owner, 2026-10-05) while every progression card below
 * it steps in by [BRANCH_INSET_STEP], so the hierarchy is carried by the indent
 * rather than only by the drawn lines. Its [inset] parameter therefore exists
 * for the call site to state that flush placement explicitly — a diagram whose
 * parent position is an implicit default is one refactor away from drifting.
 */
@Composable
private fun TrunkCard(
    card: MasteryCardData,
    inset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val name = translateExerciseName(card.exerciseName)
    // Resolved HERE, not inside the semantics block: stringResource is a
    // @Composable and a semantics lambda is not a composable scope.
    val masteredLabel = stringResource(R.string.evolution_trunk_mastered)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .border(1.dp, colors.outline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            // One label for the whole node, rather than announcing a heading, a
            // level and a load as three separate things.
            .clearAndSetSemantics { contentDescription = "$name. $masteredLabel" },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.evolution_heading_before),
                style = MaterialTheme.typography.labelSmall,
                color = colors.outline,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = masteredLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            MiniLadder(level = 1, maxLevel = 1, accent = colors.outline)
        }
    }
}

/**
 * One leg of the fork.
 *
 * [inset] leaves room for the connector drawn underneath it, so the card never
 * sits on top of its own line.
 *
 * DELIBERATELY COMPACT. The owner's word was "ingombrant": this used to be six
 * stacked rows - name, level, ladder, prescription, reps, and what the branch
 * leads to - which for a short branch name made the card taller than the trunk
 * it branches from, and a popup offering six of those was a wall rather than a
 * choice. It is now three rows: the name, the ladder as a thin bar, and ONE
 * meta line carrying the level and the work together. What the branch leads to
 * stays, because a choice that dead-ends has to LOOK like one - it is the
 * reason this is information rather than a list - but as a single ellipsized
 * line.
 */
@Composable
private fun BranchCard(
    offer: ProgressionEngine.EvolutionOffer,
    selected: Boolean,
    inset: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (selected) colors.primary else colors.outline
    val to = offer.to
    val name = translateExerciseName(to.exerciseName)
    val levelLabel = stringResource(R.string.evolution_level_of_max, to.level, to.maxLevel)
    val repsLabel = if (to.reps.isEmpty()) null else
        to.reps.joinToString(" / ") + " " + stringResource(R.string.reps)
    // One line, not three: the level and the work are the same decision and
    // reading them separately is what made the card tall.
    val workLabel = listOfNotNull(levelLabel, repsLabel, prescriptionText(to.exerciseName, to.level))
        .joinToString(" \u00B7 ")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .border(1.dp, accent.copy(alpha = if (selected) 0.9f else 0.45f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$name. $workLabel" },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (offer.tag == "new") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = colors.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.evolution_new_branch),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // The destination's ladder, drawn from the START level the engine
            // will actually hand the next session - not from zero, and not full.
            MiniLadder(level = to.level, maxLevel = to.maxLevel, accent = accent)

            Text(
                text = workLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (to.nodes.isNotEmpty()) {
                // translateExerciseName is @Composable, so each name has to be
                // resolved in a composable scope BEFORE the join - a lambda
                // inside joinToString is not one.
                val ahead = to.nodes.map { translateExerciseName(it) }
                Text(
                    text = stringResource(R.string.evolution_path_ahead) + " " +
                        ahead.joinToString(" \u203a "),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A ladder drawn as rungs.
 *
 * Real ladders run to 40 levels (a barbell bench press), so this caps the drawn
 * rungs at [MAX_RUNGS] and fills them proportionally - a 40-level ladder shows
 * as ten rungs, which still reads as "barely started" at level 1. A rung is
 * never narrower than [RUNG_MIN], so the shortest ladder is still a visible bar
 * rather than a hairline.
 */
private const val MAX_RUNGS = 10

@Composable
private fun MiniLadder(level: Int, maxLevel: Int, accent: Color) {
    val colors = MaterialTheme.colorScheme
    val total = maxLevel.coerceAtLeast(1)
    val rungs = min(total, MAX_RUNGS)
    // Never zero filled: a ladder drawn entirely empty reads as "no data".
    val filled = when {
        total <= rungs -> level.coerceIn(0, total)
        else -> (level.toFloat() / total * rungs).roundToInt().coerceAtLeast(1)
    }.coerceAtMost(rungs)

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        repeat(rungs) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < filled) accent else colors.outline.copy(alpha = 0.28f)),
            )
        }
    }
}

/**
 * The connector: a clean DROP-DOWN from the card above, then right into the
 * progression.
 *
 * The owner asked for it by shape - "like drop down going from the card above
 * the current exercise down right to the progression possibility" - and that is
 * the shape an org chart has always used: a vertical segment leaving the card
 * it descends from, and a horizontal run into the card it arrives at. Both
 * earlier attempts failed in opposite directions: cubic sweeps were "too
 * fancy", and a spine drawn INSIDE the trunk card's own width read as
 * "randomly placed".
 *
 * Three properties make it read as drawn rather than assembled:
 *
 *  - the vertical segment starts at the TRUNK'S BOTTOM EDGE, not its middle, so
 *    it visibly drops out of the card above rather than out of its text;
 *  - ONE spine, from that edge down to the LAST arm, instead of a segment per
 *    row. Overlapping butt-capped segments leave a visible seam wherever they
 *    meet; a single line has no seam by construction;
 *  - every arm leaves the spine at the same x, so the spine reads as one
 *    object rather than a stack of elbows.
 *
 * Endpoints are MEASURED (`boundsInRoot` per node plus the canvas's own
 * `positionInRoot`), not derived from a guessed layout - a name that wraps, or
 * a larger font scale, moves the card and its arm with it.
 */
@Composable
private fun TreeElbows(
    sourceRect: Rect?,
    rowRects: List<Rect>,
    origin: Offset,
    spineInset: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val sx = sourceRect ?: return@Canvas
        if (rowRects.isEmpty()) return@Canvas

        val spineX = spineInset.toPx()
        val thickness = 2.dp.toPx()
        val dropTop = sx.bottom - origin.y
        // Pair each card's arm height with its OWN left edge: looking the edge
        // up by index into a list of heights silently reuses the FIRST card's
        // edge whenever two cards share a y, which is exactly the misplaced
        // line this whole rewrite exists to remove.
        val arms = rowRects.map { it.center.y - origin.y to (it.left - origin.x) }

        // The drop: one stroke, out of the trunk's bottom edge, down to the
        // last arm.
        drawLine(
            color = color,
            start = Offset(spineX, dropTop),
            end = Offset(spineX, arms.last().first),
            strokeWidth = thickness,
            cap = StrokeCap.Butt,
        )
        // Then right into each progression, all from the same x.
        arms.forEach { (armY, armEnd) ->
            drawLine(
                color = color,
                start = Offset(spineX, armY),
                end = Offset(armEnd, armY),
                strokeWidth = thickness,
                cap = StrokeCap.Butt,
            )
        }
        // The node the drop leaves from.
        drawCircle(
            color = color,
            radius = thickness,
            center = Offset(spineX, dropTop),
        )
    }
}

@Composable
private fun prescriptionText(exerciseName: String, level: Int): String? {
    val entry = ProgressionEngine.entryFor(exerciseName) ?: return null
    val prescribed = ProgressionEngine.prescribedValueAtLevel(entry, level) ?: return null
    return when (prescribed.kind) {
        ProgressionEngine.ValueKind.LOAD ->
            prescribed.value?.let { loggedLoadSummary(exerciseName, it) }?.ifBlank { null }
        ProgressionEngine.ValueKind.VARIATION -> prescribed.variation
        ProgressionEngine.ValueKind.DURATION ->
            prescribed.value?.let {
                getString("progression_load_seconds", formatWholeNumber(it))
            }
    }
}

/** 30.0 -> "30", 2.5 -> "2.5": a whole count never shows its decimal. */
private fun formatWholeNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
