package com.grafizum.repio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.MuscleGroupDonutChart
import com.grafizum.repio.ui.components.ScreenHeader
import com.grafizum.repio.ui.components.StatCard
import com.grafizum.repio.ui.components.WorkoutCalendar
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.util.getString


/**
 * Room the Analytics scroll box leaves for a card's elevation shadow — INSIDE the box.
 *
 * The scrolling column clips its content, so the Overall Level card's own 4dp shadow had nowhere
 * to be drawn at the top of the list and the card read as cut off above its label. Owner board
 * task_13503y3v4d5e3o6w5e5e0e5v, 2026-10-12: *"the shadow around \"overall level\" makes the
 * label look cut off above so give the overlay top section a bit smaller so the shadows can also
 * go over the top side of the overall level card"*.
 *
 * It is measured against the card's own elevation (4dp in [StatCard], read at its compact size),
 * so the number cannot quietly fall below the shadow it exists to show.
 */
internal val CARD_SHADOW_HEADROOM = 6.dp

/**
 * What is left of the gap that used to sit ABOVE the scroll box. The headroom it gave up moved
 * inside the box, so the card ends up where it always did — with its shadow now drawn.
 */
internal val SHADOW_HEADROOM_ABOVE_SCROLL = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val loggedSets by viewModel.allLoggedSets.collectAsState()
    // The Overall Level replaced the consecutive-days streak on this screen.
    val userStats by viewModel.userStats.collectAsState()
    val isDeveloper = LocalDeveloperSkin.current
    val focusManager = LocalFocusManager.current
    val window = LocalWindowSize.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val contentScrollState = rememberScrollState()

    // Logic for Charts
    var selectedChartDay by remember { mutableStateOf<String?>(null) }
    
    val monShort = getString("monday_short")
    val tueShort = getString("tuesday_short")
    val wedShort = getString("wednesday_short")
    val thuShort = getString("thursday_short")
    val friShort = getString("friday_short")
    val satShort = getString("saturday_short")
    val sunShort = getString("sunday_short")

    val muscleGroupDistribution = remember(loggedSets) {
        val map = mutableMapOf<String, Int>()
        loggedSets.forEach { set ->
            // Aggregate by the precise muscle group so broad parts ("Legs", "Shoulders",
            // "Back") split into their specific subcategories in the chart.
            val group = com.grafizum.repio.ui.components.resolvePreciseMuscleGroup(set.exerciseName, set.muscleGroup)
            map[group] = (map[group] ?: 0) + 1
        }
        map
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("clear_history_title"))
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("clear_history_text"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(getString("delete_all"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showClearConfirmDialog = false
                }) {
                    Text(getString("cancel"))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = window.screenPadding)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        Spacer(modifier = Modifier.height(window.sectionSpacing / 2))

        // COMPACT HEADER — title and the clear-history action no longer compete for one
        // fixed-width row, so neither gets squeezed on narrow screens or long locales.
        ScreenHeader(
            title = getString("analytics_reports"),
            icon = Icons.Default.Assessment,
            titleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            leading = {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = getString("cancel"))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            },
            trailing = {
                if (loggedSets.isNotEmpty()) {
                    IconButton(onClick = { showClearConfirmDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        // Removed showCalendarDialog block

        // Smaller than the 8dp this used to be, with the difference spent INSIDE the scroll
        // box below: see [CARD_SHADOW_HEADROOM].
        Spacer(modifier = Modifier.height(SHADOW_HEADROOM_ABOVE_SCROLL))

        // Cards column: compact stat card + week metrics + muscle focus.
        //
        // Scrolls, and every card carries its own height. The previous version handed the
        // muscle-focus card whatever height was left over (`weight(1f)`), which is fine on a
        // tall phone but collapses to ~0dp on a 360x640dp one - the stat card and the week
        // metrics already fill that viewport, so the whole bottom card (title, donut, legend)
        // was clipped away and the page simply looked like it ended after "Training Output".
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(contentScrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.spacedBy(window.sectionSpacing)
        ) {
            // The Overall Level card's shadow, which the box's own clip would otherwise eat.
            Spacer(modifier = Modifier.height(CARD_SHADOW_HEADROOM))

            // 1. QUICK STAT CARD (compact so the Analytics tab fits one screen)
            //
            // The Overall Level sits here now, in place of the consecutive-days
            // streak: this screen reports what the training has produced, and the
            // level is that answer at a glance. Consistency is still visible in the
            // weekly calendar directly below.
            val overallLevel = userStats?.takeIf { it.overallLevelReady }?.overallStrengthLevel
            // Same badge and same explanation as Home: the rank is one value, so
            // it is derived once and shown identically in both places.
            val overallRank = com.grafizum.repio.data.OverallLevelRank.of(userStats)
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        com.grafizum.repio.ui.components.OverallLevelTooltip(
                            level = overallLevel,
                            rank = overallRank,
                            exerciseCount = userStats?.exercisesCountForLevel ?: 0
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                StatCard(
                    title = getString("overall_level_label"),
                    // The LEVEL as a whole number, with the rank letter as the
                    // leading badge — same as Home, and for the same reason: the
                    // letter was already the badge, so printing "Rank E" as the
                    // value said it twice and hid the one number that moves
                    // every session. The raw 0-100 figure and the weighted
                    // median behind it stay backend-only (owner, 2026-10-02).
                    value = if (overallLevel != null && overallRank != null) {
                        getString(
                            "overall_level_value_format",
                            com.grafizum.repio.data.OverallLevelRank.displayLevel(overallLevel)
                        )
                    } else {
                        getString("overall_level_none")
                    },
                    subtitle = "",
                    // Same as Home's card: the rank letter leads, in the themed tile
                    // the trophy used to own, and the trophy falls back into that same
                    // tile while there is no rank yet. Two cards showing one stat must
                    // not disagree about where the rank sits.
                    icon = if (overallRank == null) Icons.Default.EmojiEvents else null,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    badgeText = if (overallLevel != null) null
                                else getString("overall_level_not_ready"),
                    compact = true,
                    leading = if (overallRank != null) {
                        {
                            com.grafizum.repio.ui.components.RankBadge(
                                rank = overallRank.rank,
                                contentDescription = getString("rank_badge_content_desc", overallRank.letter),
                                size = 40.dp
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            // 2. WEEK METRICS (TRAINING OUTPUT)
            WorkoutCalendar(
                loggedSets = loggedSets,
                selectedDayKey = selectedChartDay,
                onDaySelected = { selectedChartDay = it }
            )

            // 3. MUSCLE GROUP FOCUS — a real, bounded height instead of a slice of the
            // leftover space. The card divides its height between the donut and the legend,
            // which only works while the height is known; it ends 10dp above the bottom menu
            // bar, and the column scrolls if the three cards cannot all fit at once.
            MuscleGroupDonutChart(
                distribution = muscleGroupDistribution,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(window.donutHeight)
                    .padding(bottom = 10.dp)
            )
        }
    }
}
