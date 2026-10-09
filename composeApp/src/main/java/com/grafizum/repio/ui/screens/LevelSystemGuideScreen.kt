package com.grafizum.repio.ui.screens

import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grafizum.repio.data.OverallLevelRank
import com.grafizum.repio.ui.components.LEVEL_STAGE_SEPARATOR
import com.grafizum.repio.ui.components.LevelStageLabel
import com.grafizum.repio.ui.components.RankBadge
import com.grafizum.repio.util.getString
import com.grafizum.repio.util.rememberCurrentLocale

/**
 * The Level System Guide's five graphical examples. Every example is a small
 * visual plus a one-to-two-sentence caption — no paragraphs, no internal
 * fields (raw JSON, jump ratios, validation flags never appear here). The
 * Settings card embeds this content inline, following the app's existing
 * expandable-FAQ pattern; [LevelSystemGuideScreen] wraps it for standalone use.
 *
 * All copy comes from string resources so de/it can be filled in later.
 */
@Composable
fun LevelSystemGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = getString("level_system_guide_intro"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LevelSystemGuideExamples()
    }
}

/**
 * The example cards without the intro line — reused by onboarding, by the Settings guide and
 * by the bulk-adjust reminder, so one edit here reaches every place the level system is
 * explained.
 */
@Composable
fun LevelSystemGuideExamples() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GuideExampleCard(getString("level_example_title")) { LevelBarsExample() }
        GuideExampleCard(getString("stage_example_title")) { StageBoxesExample() }
        GuideExampleCard(getString("level_up_example_title")) { LevelUpExample() }
        GuideExampleCard(getString("large_jump_example_title")) { LargeJumpExample() }
        GuideExampleCard(getString("difficulty_change_example_title")) { DifficultyButtonsExample() }
        // THE LOCK, taught where the rest of the system is taught.
        //
        // Owner, 2026-10-08: "add the locking mechanism in the explanation on what the level
        // system is everywhere". This is the one list every explanation draws from — the
        // onboarding page, the Settings guide and the reminder strip above the bulk editor —
        // so adding the card here is what makes "everywhere" true rather than nearly true.
        GuideExampleCard(getString("lock_example_title")) { LevelLockExample() }
    }
}

/** Standalone, scrollable wrapper for the guide content (readable on small screens). */
@Composable
fun LevelSystemGuideScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LevelSystemGuideContent()
    }
}

/**
 * Compact re-usable example cards for the bulk-adjust screen's reminder
 * header: each pairs a mini version of the guide graphics with one sentence.
 */
@Composable
fun LevelGuideReminderCards() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideExampleCard(getString("level_example_title")) { LevelBarsExample() }
        GuideExampleCard(getString("stage_example_title")) { StageBoxesExample() }
        GuideExampleCard(getString("level_up_example_title")) { LevelUpExample() }
    }
}

@Composable
private fun GuideExampleCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun GuideCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    )
}

// ------------------------------------------------------------------
// Example 1 — What is a level?
// Horizontal bars: higher level = more resistance / harder variation.
// ------------------------------------------------------------------

@Composable
private fun LevelBarsExample() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GuideLevelBar(getString("level_word_n", 1), 0.35f)
        GuideLevelBar(getString("level_word_n", 2), 0.60f)
        GuideLevelBar(getString("level_word_n", 3), 0.85f, highlight = true)
    }
    GuideCaption(getString("level_example_caption"))
}

/**
 * "Level 3 | XIII" for the guide's data rows — the SAME notation the live level card uses
 * (one box, level then stage, see the owner's 2026-10-08 instruction), read from the same
 * separator constant. The word goes through resources (it is "Livello 3" in Italian); the
 * Roman stage stays a literal because a Roman numeral reads the same in every locale.
 */
@Composable
private fun levelStageLabel(level: Int, romanStage: String): String =
    getString("level_word_n", level) + LEVEL_STAGE_SEPARATOR + romanStage

/**
 * A number in the reader's own locale, so the illustration agrees with the rest
 * of the app: an English-formatted "6.0" printed beside the mini Home card's
 * German-formatted "L.6,4" would look like two different numbers.
 */
@Composable
private fun decimalText(value: Double, decimals: Int): String =
    String.format(rememberCurrentLocale(), "%.${decimals}f", value)

@Composable
private fun GuideLevelBar(label: String, fraction: Float, highlight: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (highlight) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

// ------------------------------------------------------------------
// Example 2 — What is a stage?
//
// The stages the app actually SHOWS: I and then IV, the rows whose sets all ask
// for the same reps. It used to draw I, II and III - the raw pattern - which is
// the one place in the app that still listed the in-between rows, and the owner
// caught it there (2026-10-09): *"it shows \"I\" and right after \"II\" although
// the display should be from \"I\" to \"IV\" since those are the only ones that
// have the same reps for each set"*.
//
// The rule is not invented here: it is the same one the exercise editor's level
// list already applies in `roundedStages` (owner ruling, same day: "round them up
// so each number shown is the same reps"), and on a thirteen-stage pattern it is
// what turns a wall of one-rep-apart mixtures into the five real steps I, IV, VII,
// X, XIII. The guide teaching the unrounded list was the last surface disagreeing
// with the screen it explains - and worse than the editor's version, because a
// reader has no way to know the app skips those rows.
//
// The numbers are the standard pattern's own (8 / 8 / 8 -> 9 / 9 / 9), pinned
// against the engine by [com.grafizum.repio.LevelGuideStageDisplayTest] so the
// example cannot drift from the data it illustrates.
// ------------------------------------------------------------------

@Composable
private fun StageBoxesExample() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GuideStageBox("I", "8 / 8 / 8", done = true)
        GuideStageBox("IV", "9 / 9 / 9", current = true)
    }
    GuideCaption(getString("stage_example_caption"))
}

@Composable
private fun GuideStageBox(stage: String, reps: String, done: Boolean = false, current: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when {
            current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            done -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (current) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stage,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (current) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = reps,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (done) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Example 3 & 4 — Level-up reset and large-jump reset.
// Two data rows with an arrow between: the last stage of a level,
// then the reset to stage I at more load.
// ------------------------------------------------------------------

@Composable
private fun LevelUpExample() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GuideDataRow(levelStageLabel(3, "XIII"), "12 / 12 / 12")
        GuideArrowRow(getString("level_up_example_arrow"))
        GuideDataRow(levelStageLabel(4, "I"), "8 / 8 / 8", highlight = true)
    }
    GuideCaption(getString("level_up_example_caption"))
}

@Composable
private fun LargeJumpExample() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // The number is illustrative data, but the unit goes through the same
        // format the rest of the app prints loads with, so the guide cannot end up
        // phrasing "6 kg" differently from the workout screen.
        GuideDataRow(getString("weight_kg_intl", "6") + " — " + levelStageLabel(2, "XIII"), "12 / 12 / 12")
        GuideArrowRow(getString("large_jump_example_arrow"))
        GuideDataRow(getString("weight_kg_intl", "10") + " — " + levelStageLabel(3, "I"), "8 / 8 / 8", highlight = true)
    }
    GuideCaption(getString("large_jump_example_caption"))
}

/**
 * The lock, drawn as the card draws it: the level box on the left, the lock chip on the
 * right, and a caption that says what the lock is FOR.
 *
 * It deliberately mirrors the live card's two controls rather than inventing a diagram, so a
 * reader who has seen this page can find the same two things on the workout screen. It is
 * copied from the live `LevelPrescriptionCard`, which is the surface that owns them - if the
 * card's chip ever changes shape, this example has to follow it.
 *
 * THE PENCIL IS PART OF THE DRAWING (owner, 2026-10-09). The card now shows the lock chip and
 * the pencil as one pair at its right edge - *"make it far right together with the pencil but
 * also make sure that the user can distinguish them"* - and this example is the one place that
 * tells a reader where to FIND the chip. A diagram that drew the lock alone in the corner
 * would send them looking for something that sits next to an edit glyph on the real card. The
 * pair is drawn at 2dp apart, exactly as the card's own Row sets it.
 */
@Composable
private fun LevelLockExample() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // The level box: "Level 10 | IV", exactly as the card prints it.
            //
            // It IS the card's own drawing, not a copy of it: [LevelStageLabel] owns the
            // reading and the thin bar between the two halves, and this example only
            // supplies the ring around them. The comment above used to promise "exactly as
            // the card prints it" while this row typed its own "Level 10 | IV" - so when
            // the owner rejected that pipe on the card ("thinner so it doesnt look like a
            // number or letter", 2026-10-09) the guide went on teaching the old one.
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                LevelStageLabel(
                    level = 10,
                    stage = 4, // "IV" - a Roman numeral reads the same in every locale.
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // The pair the card anchors at its right edge: the lock chip, then the pencil.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // The lock chip, in its locked state: a primary hairline around the same 14%
                // primary fill the action banner uses. Both numbers are the card's own.
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = getString("level_locked_short"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }

                // The pencil that shares that corner on the card, drawn the same size the card
                // draws its glyph so the shape is recognisable on the workout screen.
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(20.dp)
                )
            }
        }
        GuideCaption(getString("lock_example_caption"))
    }
}

@Composable
private fun GuideArrowRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun GuideDataRow(left: String, right: String, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = left,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = right,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (highlight) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ------------------------------------------------------------------
// Example 5 — Changing difficulty.
// Three static pills (Easy | Medium | Hard); the chosen one is tinted.
// ------------------------------------------------------------------

@Composable
private fun DifficultyButtonsExample() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        GuideDifficultyPill(getString("intensity_easy"), selected = true, modifier = Modifier.weight(1f))
        GuideDifficultyPill(getString("intensity_medium"), selected = false, modifier = Modifier.weight(1f))
        GuideDifficultyPill(getString("intensity_hard"), selected = false, modifier = Modifier.weight(1f))
    }
    GuideCaption(getString("difficulty_change_example_caption"))
}

@Composable
private fun GuideDifficultyPill(label: String, selected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

// ------------------------------------------------------------------
// Example 6 — The Overall Level.
// Three steps: what it averages, how recency weights it, and where it
// appears. Deliberately built from the same visual vocabulary as the
// level examples above (chips, bars, tinted surfaces) so it reads as part
// of one system rather than a bolted-on chart.
// ------------------------------------------------------------------

/**
 * The worked example the Overall Level graphic teaches with: five exercises,
 * each shown as a percentage of its OWN ladder, and the median they produce.
 *
 * Every figure is *derived* rather than written out beside it, and step 3
 * reuses the result for its mini Home card. That way the three steps cannot
 * show numbers that disagree with each other or with their own arithmetic — an
 * illustration whose figures do not actually work out teaches the wrong thing.
 *
 * The percentages are deliberately chosen to make the point the model exists
 * for: the first exercise is at level 5 of a 6-level ladder (a long way
 * through) and the second is at level 5 of a 30-level ladder (barely started).
 * Averaging those two RAW levels would call them identical, which is the
 * defect the per-exercise percentage fixes.
 */
object OverallLevelExample {
    /** Decimals a percentage is shown with in the guide. */
    const val DECIMALS = 1

    /**
     * Five exercises as level chips read them, paired with each one's own ladder
     * length. Deliberately ordinary movements a reader would recognise, and
     * deliberately NOT all the same ladder length — that contrast is the lesson.
     */
    val levels = listOf(5, 4, 5, 3, 6)

    /** Each exercise's own top rung, so the same level means different things. */
    val ladderLengths = listOf(6, 30, 8, 15, 20)

    /**
     * Where each exercise sits on its own ladder, 0-100: 83, 10, 57, 13, 26.
     * Sorted, that is 10 · 13 · 26 · 57 · 83, so the median — the middle one —
     * is 26.
     */
    val percents: List<Double> = levels.zip(ladderLengths) { level, max ->
        ((level - 1).toDouble() / (max - 1) * 100.0)
    }

    /** The median of [percents]: the middle exercise of the five. */
    val median: Double = percents.sorted()[percents.size / 2]

    /**
     * The rank that median lands in — what the app actually shows the user,
     * because the percentage itself is backend-only (owner, 2026-10-02).
     * Derived through the real rank table rather than hardcoded, so the guide
     * cannot advertise a letter the app would not give.
     */
    val medianRank: String =
        OverallLevelRank.of(median)?.letter ?: OverallLevelRank.Letter.E.label
}

/**
 * The Overall Level explained as a three-step picture: five per-exercise
 * levels average into one number, recent sessions outweigh old ones, and the
 * result appears on Home once five exercises qualify.
 *
 * Defined once and reused by the Settings FAQ and the first-run walkthrough, so
 * the explanation cannot drift between them.
 */
@Composable
fun OverallLevelExampleGraphic() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OverallLevelStep(
            number = 1,
            title = getString("overall_level_step_measure_title"),
            caption = getString("overall_level_step_measure_caption")
        ) { OverallAverageGraphic() }

        OverallLevelStep(
            number = 2,
            title = getString("overall_level_step_recency_title"),
            caption = getString("overall_level_step_recency_caption")
        ) { OverallRecencyGraphic() }

        OverallLevelStep(
            number = 3,
            title = getString("overall_level_step_where_title"),
            caption = getString("overall_level_step_where_caption")
        ) { OverallHomeGraphic() }
    }
}

/**
 * The Overall Level in words.
 *
 * [detailed] is the Settings version: the analogy, the three things the number is
 * worked out from, the worked example, and what it is for. The short form is what
 * the first-run walkthrough shows — that screen is already a wall of pictures, so
 * it needs one sentence, not five.
 *
 * The worked example quotes [OverallLevelExample.average] instead of a literal, so
 * the sentence and the graphic underneath it cannot state different numbers.
 */
@Composable
fun OverallLevelProse(detailed: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!detailed) {
            Text(
                text = getString("overall_level_short"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        Text(
            text = getString("overall_level_gpa_lead"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // One bullet per input the average is built from, so "what goes into it"
        // reads as a list of three steps rather than a paragraph to parse.
        listOf(
            "overall_level_gpa_step_1",
            "overall_level_gpa_step_2",
            "overall_level_gpa_step_3"
        ).forEach { key ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getString(key),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = getString(
                "overall_level_gpa_example",
                OverallLevelExample.median.roundToInt(),
                OverallLevelExample.medianRank
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = getString("overall_level_gpa_outro"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The prose plus the three-step graphic — what a reader who wants the whole story
 * actually gets, in both places it is offered.
 */
@Composable
fun OverallLevelExplainer(detailed: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OverallLevelProse(detailed = detailed)
        OverallLevelExampleGraphic()
    }
}

/** One numbered step: marker + title, then the graphic, then the explanation. */
@Composable
private fun OverallLevelStep(
    number: Int,
    title: String,
    caption: String,
    graphic: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        graphic()
        GuideCaption(caption)
    }
}

/**
 * Five per-exercise levels on the left, the single averaged number on the right.
 *
 * The chips share the row by weight rather than taking a fixed width: a fixed
 * chip would overflow the row once the user's font scale grows.
 */
@Composable
private fun OverallAverageGraphic() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // These really do reduce to the number on the right, which is the whole
        // point of the step. They are percentages of each exercise's OWN ladder,
        // not raw levels: the first is level 5 of 6 and the second level 5 of 30,
        // and showing them as the same "5" is exactly the mistake the model avoids.
        OverallLevelExample.percents.forEach { percent ->
            OverallValueChip(
                text = decimalText(percent, 0),
                modifier = Modifier.weight(1f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        OverallValueChip(
            text = decimalText(OverallLevelExample.median, 0),
            highlight = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverallValueChip(
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Bold
            ),
            color = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

/**
 * The recency weighting: three training gaps, the bar halving each half-life
 * and the multiplier spelled out beside it.
 */
@Composable
private fun OverallRecencyGraphic() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OverallRecencyBar(getString("overall_level_recency_today"), 1f, decimalText(1.0, 1))
        OverallRecencyBar(getString("overall_level_recency_month"), 0.5f, decimalText(0.5, 1))
        OverallRecencyBar(getString("overall_level_recency_two_months"), 0.25f, decimalText(0.25, 2))
    }
}

@Composable
private fun OverallRecencyBar(label: String, fraction: Float, multiplier: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(6.dp))
                    // Newer sessions read stronger, so the bar itself carries the
                    // ranking instead of relying on the number beside it.
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f + 0.6f * fraction)
                    )
            )
        }
        Text(
            text = getString("overall_level_weight_multiplier", multiplier),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(36.dp)
        )
    }
}

/**
 * Where the number shows up — a miniature of the Home stat card, so "it appears
 * on Home" points at something the user has already seen.
 */
@Composable
private fun OverallHomeGraphic() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getString("overall_level_label"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // The RANK, not the percentage. The owner ruled (2026-10-02)
                    // that the 0-100 figure is backend-only, so the card shows the
                    // same letter a user sees on Home rather than the number that
                    // produced it.
                    text = getString("rank_label_format", OverallLevelExample.medianRank),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Every rank from E to SS+, as a table: the badge, the level range it covers,
 * and a line on why it is worth having.
 *
 * Read straight from [OverallLevelRank.ORDERED], so the table can never list a
 * tier the threshold map does not define — and re-tuning a threshold moves the
 * range labels here without touching this file, because each range is derived
 * from the next rank's floor rather than written out.
 */
@Composable
fun RankTableContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OverallLevelRank.ORDERED.forEach { rank ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RankBadge(
                        rank = rank,
                        contentDescription = getString("rank_label_format", rank.label),
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = getString("rank_label_format", rank.label),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rankRangeText(rank),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = getString(rank.descriptionKey),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * "14 – 27" for a bounded tier, "95+" for the open-ended top one.
 *
 * The figures are the tier's own floors and ceilings on the internal 0-100
 * scale, rounded to whole points because the guide is explaining WHICH band a
 * rank covers, not reproducing the figure the app computes. They are read from
 * the rank table rather than written out, so re-tuning a threshold moves this
 * text with it.
 */
@Composable
private fun rankRangeText(rank: OverallLevelRank.Letter): String {
    val min = OverallLevelRank.displayPercent(rank.minPercent)
    val end = rank.rangeEnd
        ?: return getString("rank_range_open", min)
    return getString("rank_range", min, OverallLevelRank.displayPercent(end))
}
