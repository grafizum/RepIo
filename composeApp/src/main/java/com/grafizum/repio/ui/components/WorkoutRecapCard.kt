package com.grafizum.repio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.data.WeightUnit
import com.grafizum.repio.ui.viewmodel.WorkoutRecap
import com.grafizum.repio.util.getString

/**
 * What the session just achieved — the "Double-Bezel Console" layout the owner
 * picked from three previews, 2026-10-17: *"i want the session recap remade a
 * bit better to have a more boxed modern multi layered design"*.
 *
 * THE LAYERS ARE THE LADDER, not decoration. Every step lands on the theme's
 * own rungs so the nesting reads the same on dark and light:
 *
 *   outer shell   `surface`          #141414 — chips/sheets, the machined tray
 *   inner core    `surfaceVariant`   #191919 — general cards, the plate
 *   layers/tiles  `surfaceContainerHigh` #1E1E1E — hero panels, the front
 *
 * with the hairline (`outlineVariant`) between them, and each of the three
 * content layers — stat boxes, split, list — in its own boxed well inside the
 * core. On light themes the same three tokens run the other direction (the
 * beige theme's own steps), so the nesting never collapses into one flat card.
 *
 * Deliberate choices kept from the previous card:
 *
 *  - **Volume is omitted, not zeroed.** A bodyweight or timed session has no
 *    honest tonnage; the tile is absent and the remaining tiles keep order.
 *  - **The split bar uses the app's own muscle colours** (`getMuscleGroupColor`),
 *    now as spaced rounded segments so neighbouring muscles cannot bleed.
 *  - **The bar is sets, not load** — the unit every exercise shares.
 *  - **The rows name what was done**, values as logged; a part with no honest
 *    value (a bodyweight row's load) is left out rather than printed as zero.
 *
 * The card reads [WorkoutRecap] only, so it renders on both surfaces that show
 * the day: the Workout Now completion view and Home's daily card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutRecapCard(recap: WorkoutRecap) {
    val durationMinutes = (recap.durationSeconds / 60).toInt()
    // Tonnage follows the unit too, but WITHOUT the grid: it is a sum of many
    // sets, not a load anyone sets on a bar, so it converts and rounds whole.
    val volumeUnit = LocalWeightUnit.current
    val volumeText = recap.volumeKg?.let { kg ->
        val shown = volumeUnit.fromKg(kg)
        val clean = if (shown % 1.0 == 0.0) shown.toInt().toString() else String.format("%.1f", shown)
        getString(
            if (volumeUnit == WeightUnit.KG) "recap_volume_kg" else "recap_volume_lb",
            clean
        )
    }
    val tiles = buildList {
        add(getString("recap_exercises") to recap.exercises.toString())
        add(getString("recap_sets") to recap.sets.toString())
        if (volumeText != null) add(getString("recap_volume") to volumeText)
        if (durationMinutes > 0) add(getString("recap_duration") to getString("recap_duration_format", durationMinutes))
    }
    val totalSets = recap.setsByMuscle.values.sum().coerceAtLeast(1)

    // ---- OUTERMOST SHELL: the tray the whole card sits in. ----
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        // ---- INNER CORE: one radius step down, concentric with the shell. ----
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                // THE HEADLINE IS THE FINISHED LABEL — it takes the "Session
                // recap" title's slot, boxed and bigger (owner, 2026-10-17:
                // *"instead of showing session recap show that boxed label
                // (changed to be shorter) and make it slightly bigger i want it
                // to be better visible"*). The card has exactly two homes —
                // Home's swap (finished by definition) and the completion view
                // — so the strip can state the fact outright instead of the
                // title naming the card. Accent-tinted, not a flat green: the
                // strip is "done" in whatever skin is active, the same way the
                // completed-timeline dots are.
                val accent = MaterialTheme.colorScheme.primary
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = getString("home_exercises_finished"),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            ),
                            color = accent
                        )
                    }
                }

                // ---- LAYER 1: every figure in its own boxed well. ----
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tiles.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { (label, value) ->
                                StatBox(value = value, label = label, modifier = Modifier.weight(1f))
                            }
                            // An odd last tile stays half-width instead of
                            // stretching across the grid.
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ---- LAYER 2: where the work went. ----
                if (recap.setsByMuscle.isNotEmpty()) {
                    RecapLayer {
                        Text(
                            text = getString("recap_split_title"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Spaced rounded segments: two muscles can share a hue
                        // family, and an unbroken bar merges them visually.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            recap.setsByMuscle.forEach { (muscle, sets) ->
                                Box(
                                    modifier = Modifier
                                        .weight(sets.toFloat() / totalSets.toFloat())
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(getMuscleGroupColor(muscle))
                                )
                            }
                        }
                        // Legend chips, so the colours mean something. Sorted
                        // like the bar, so reading across matches reading down.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recap.setsByMuscle.entries.forEach { (muscle, sets) ->
                                LegendChip(muscle = muscle, sets = sets)
                            }
                        }
                    }
                }

                // ---- LAYER 3: what was actually done. ----
                if (recap.exerciseRows.isNotEmpty()) {
                    RecapLayer {
                        Text(
                            text = getString("recap_exercises"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        recap.exerciseRows.forEachIndexed { index, row ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(if (index > 0) 9.dp else 2.dp))
                                Text(
                                    text = translateExerciseName(row.exerciseName),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // "4 Sets · 12, 12, 12, 12 · 8 kg". A PART WITH
                                // NO HONEST VALUE IS LEFT OUT — the same rule the
                                // volume tile lives by.
                                Text(
                                    text = listOf(
                                        "${row.sets} ${getString("recap_sets")}",
                                        row.reps.joinToString(", "),
                                        loadsLabel(row.weightsKg, volumeUnit),
                                    ).filter { it.isNotBlank() }.joinToString("  ·  "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(9.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One boxed statistic: value over its eyebrow label, inside its own well. */
@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A content layer: one boxed well inside the core, with its own hairline. */
@Composable
private fun RecapLayer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}

/** One legend chip: coloured dot, muscle name, set count. */
@Composable
private fun LegendChip(muscle: String, sets: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(getMuscleGroupColor(muscle))
        )
        Text(
            text = "$muscle $sets",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * The distinct loads of one exercise row, in the user's unit: "40 kg" or
 * "40/42.5 kg" when a session mixed loads. Empty when the exercise carries no
 * load at all — a pin count and a plank have no kilograms to name, and the
 * caller drops the blank part instead of showing a bare separator.
 */
@Composable
private fun loadsLabel(weightsKg: List<Double>, unit: WeightUnit): String {
    if (weightsKg.isEmpty()) return ""
    // formatLoadValue, not String.format: the project's own "this is how a load
    // becomes text" helper — locale-independent, so "42.5" cannot print as
    // "42,5" on one machine and trip DefaultLocale on the next.
    val joined = weightsKg.joinToString("/") { formatLoadValue(unit.fromKg(it)) }
    return getString(
        if (unit == WeightUnit.KG) "recap_volume_kg" else "recap_volume_lb",
        joined
    )
}
