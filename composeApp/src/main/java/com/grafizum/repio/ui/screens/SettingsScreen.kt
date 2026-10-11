package com.grafizum.repio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.grafizum.repio.util.getString
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.ScreenHeader
import com.grafizum.repio.data.WeightUnit
import com.grafizum.repio.ui.components.appLanguageOptions
import com.grafizum.repio.ui.components.TimersMergedCard
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import kotlinx.coroutines.delay

data class ThemeColorItem(
    val key: String,
    val nameKey: String,
    val previewPrimary: Color,
    val previewBackground: Color
)

val lightThemes = listOf(
    ThemeColorItem("BEIGE", "warm_beige", Color(0xFF8C6239), Color(0xFFF7F4EE)),
    ThemeColorItem("EMERALD", "emerald_mint", Color(0xFF00796B), Color(0xFFF1F8F7)),
    ThemeColorItem("CRIMSON", "crimson_red", Color(0xFFC62828), Color(0xFFFFFAFA)),
    ThemeColorItem("ROYAL_BLUE", "royal_blue", Color(0xFF007AFF), Color.White)
)

val darkThemes = listOf(
    ThemeColorItem("PURPLE", "elegant_purple", Color(0xFFD0BCFF), Color(0xFF1C1B1F)),
    ThemeColorItem("FLAME", "flame_orange", Color(0xFFFF6D00), Color(0xFF121212)),
    ThemeColorItem("OCEAN", "ocean_teal", Color(0xFF00B8D4), Color(0xFF0F1E24)),
    ThemeColorItem("ROSE", "rose_pink", Color(0xFFF06292), Color(0xFF28111A))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val isDeveloper = LocalDeveloperSkin.current
    val focusManager = LocalFocusManager.current
    val window = LocalWindowSize.current
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val motivationalNudges by viewModel.motivationalNudges.collectAsState()
    val levelSystemEnabled by viewModel.levelSystemEnabled.collectAsState()
    val levelSystemConfirmation by viewModel.levelSystemConfirmation.collectAsState()
    val selectedTheme by viewModel.themeOption.collectAsState()
    val developerThemeEnabled by viewModel.developerThemeEnabled.collectAsState()
    val defaultRestTimerSeconds by viewModel.defaultRestTimerSeconds.collectAsState()
    val defaultExerciseTimerSeconds by viewModel.defaultExerciseTimerSeconds.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val showRestartDialog by viewModel.showRestartDialog.collectAsState()
    // val context = LocalContext.current
    var expandedLanguage by remember { mutableStateOf(false) }
    var showSupportCard by remember { mutableStateOf(false) }
    var showFAQ by remember { mutableStateOf(false) }
    var showLevelGuide by remember { mutableStateOf(false) }
    // The Overall Level explainer, in the same Instructions category as the
    // level guide. Kept as its own flag so the two open independently.
    var showOverallLevelGuide by remember { mutableStateOf(false) }
    var showRankGuide by remember { mutableStateOf(false) }
    var showRestTimerConfirmation by remember { mutableStateOf(false) }
    var showTransitionTimerConfirmation by remember { mutableStateOf(false) }
    var pendingRestTimer by remember { mutableIntStateOf(defaultRestTimerSeconds) }
    var pendingTransitionTimer by remember { mutableIntStateOf(defaultExerciseTimerSeconds) }

    val scrollState = rememberScrollState()

    LaunchedEffect(showFAQ) {
        if (showFAQ) {
            // Smoothly follow the expansion animation in real-time
            repeat(60) {
                scrollState.scrollTo(scrollState.maxValue)
                delay(20) 
            }
        }
    }

    LaunchedEffect(showSupportCard) {
        if (showSupportCard) {
            // Smoothly follow the expansion animation in real-time
            repeat(60) {
                scrollState.scrollTo(scrollState.maxValue)
                delay(20) 
            }
        }
    }

    // LEVEL SYSTEM TOGGLE CONFIRMATIONS — one dialog per direction. Disabling
    // warns the prescriptions disappear; enabling warns every level will be
    // re-derived from the weights last used.
    if (levelSystemConfirmation == com.grafizum.repio.ui.viewmodel.LevelSystemConfirmation.DISABLE) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLevelSystemConfirmation() },
            title = { Text(getString("level_system_off_title")) },
            text = { Text(getString("level_system_off_msg")) },
            confirmButton = {
                Button(onClick = { viewModel.confirmLevelSystemChange() }) {
                    Text(getString("level_system_off_confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLevelSystemConfirmation() }) {
                    Text(getString("cancel"))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    if (levelSystemConfirmation == com.grafizum.repio.ui.viewmodel.LevelSystemConfirmation.ENABLE) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLevelSystemConfirmation() },
            title = { Text(getString("level_system_on_title")) },
            text = { Text(getString("level_system_on_msg")) },
            confirmButton = {
                Button(onClick = { viewModel.confirmLevelSystemChange() }) {
                    Text(getString("level_system_on_confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLevelSystemConfirmation() }) {
                    Text(getString("cancel"))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showRestTimerConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestTimerConfirmation = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("confirm_timer_change_title"))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("confirm_timer_change_msg"))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = getString("timer_override_warning"),
                        color = Color.Red.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    focusManager.clearFocus()
                    viewModel.overrideAllWorkoutsRestTimer(pendingRestTimer)
                    showRestTimerConfirmation = false
                }) {
                    Text(getString("confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showRestTimerConfirmation = false
                }) {
                    Text(getString("cancel"))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showTransitionTimerConfirmation) {
        AlertDialog(
            onDismissRequest = { showTransitionTimerConfirmation = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("confirm_timer_change_title"))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("confirm_timer_change_msg"))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = getString("timer_override_warning"),
                        color = Color.Red.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    focusManager.clearFocus()
                    viewModel.overrideAllWorkoutsTransitionTimer(pendingTransitionTimer)
                    showTransitionTimerConfirmation = false
                }) {
                    Text(getString("confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus()
                    showTransitionTimerConfirmation = false
                }) {
                    Text(getString("cancel"))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* Force user to click OK to apply changes correctly */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() }
                ) {
                    Text(getString("restart_required_title"))
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
                    Text(getString("restart_required_message"))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.applyThemeRestart()
                        // (context as? Activity)?.finishAffinity()
                    }
                ) {
                    Text(getString("ok"))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
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
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(window.sectionSpacing))

        // COMPACT HEADER
        ScreenHeader(
            title = getString("app_settings"),
            icon = Icons.Default.Settings,
            titleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(window.sectionSpacing))

        // MAIN SETTINGS CONTENT
        Column(verticalArrangement = Arrangement.spacedBy(window.sectionSpacing)) {
            
            // WEIGHT UNIT CARD
            //
            // Defaulted from the phone's country, then whatever the user picks —
            // the question is what plates are on their rack, and a US gym has
            // pounds. Storage never changes: a set logged at 40 kg is 40 kg, so
            // this changes how the numbers are written down, not anyone's
            // history. That is why the choice is reversible in one tap.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = getString("weight_unit"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getString("change_weight_unit"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeightUnit.values().forEach { option ->
                            val selected = option == weightUnit
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.setWeightUnit(option)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = getString(
                                        if (option == WeightUnit.KG) "weight_unit_kg" else "weight_unit_lbs"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // LANGUAGE SELECTION CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = getString("language"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getString("change_app_language"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shared with the first-run walkthrough's language button, so
                    // the two pickers can never offer different sets.
                    val languageOptions = appLanguageOptions()

                    val selectedLanguageLabel = languageOptions.find { it.first == appLanguage }?.second ?: getString("system_default")

                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { 
                            focusManager.clearFocus()
                            expandedLanguage = !expandedLanguage 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedLanguageLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        ExposedDropdownMenu(
                            expanded = expandedLanguage,
                            onDismissRequest = { expandedLanguage = false }
                        ) {
                            languageOptions.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.setAppLanguage(code)
                                        expandedLanguage = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            }

            // TIMERS SECTION
            TimersMergedCard(
                restValue = defaultRestTimerSeconds,
                transitionValue = defaultExerciseTimerSeconds,
                onRestSelected = { seconds ->
                    pendingRestTimer = seconds
                    showRestTimerConfirmation = true
                },
                onTransitionSelected = { seconds ->
                    pendingTransitionTimer = seconds
                    showTransitionTimerConfirmation = true
                }
            )

            // SCREEN KEEP ALIVE TOGGLE
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.75f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = getString("keep_screen_on"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = getString("force_screen_stay_active"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = { viewModel.setKeepScreenOn(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics { testTag = "keep_screen_on_switch" }
                        )
                    }
                }
            }

            // LEVEL SYSTEM ON/OFF — the master switch between prescribed levels and
            // full custom control. When off, every exercise's level display is hidden
            // and the routine editor's sets/reps/weight/intensity fields unlock.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.75f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = getString("level_system_toggle_title"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = getString(
                                        if (levelSystemEnabled) "level_system_toggle_desc_on"
                                        else "level_system_toggle_desc_off"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        Switch(
                            checked = levelSystemEnabled,
                            // Both directions confirm first: OFF drops the prescriptions,
                            // ON re-derives every level from the last-used weights.
                            onCheckedChange = { viewModel.requestLevelSystemChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.semantics { testTag = "level_system_switch" }
                        )
                    }

                    // The app-wide "stay on this level" switch used to sit here. REMOVED by
                    // owner ruling 2026-10-09: *"remove 'stay on this level' switch in the
                    // settings completely"*. It was the only writer of
                    // `level_lock_enabled`, so with it gone the whole global pin is gone -
                    // the pref, its StateFlow, its setter and the gate the level engine
                    // read are all deleted with it. What stays is the PER-EXERCISE lock,
                    // which lives on the workout card's level box (the padlock that reads
                    // "Level lock"/"Locked") and writes `routine_exercises.locked`.
                }
            }

            // MOTIVATIONAL NUDGES TOGGLE
            // Hidden while the feature is dormant (waiting on quote permission), so
            // users never see a switch that does nothing. It reappears by itself
            // when MotivationNudger.NUDGES_ENABLED is set back to true.
            if (com.grafizum.repio.util.MotivationNudger.NUDGES_ENABLED) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(0.75f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = getString("motivational_nudges"),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = getString("motivational_nudges_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Switch(
                                checked = motivationalNudges,
                                onCheckedChange = { viewModel.setMotivationalNudges(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.semantics { testTag = "motivational_nudges_switch" }
                            )
                        }
                    }
                }
            }

            // COLOR THEMES SECTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = getString("color_theme"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (developerThemeEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = getString("developer_theme"),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        // The swatch shows the theme's own two colours - its very dark grey
                        // plate and its gold - instead of the white/black pair the pixel-era
                        // skin used, which made this row the one place in the app that still
                        // advertised the look the theme no longer has. Kept in step with
                        // DeveloperColorScheme by hand: the swatch cannot read the scheme
                        // (the picker draws every theme, only one of them is active).
                        ThemeOptionRow(
                            theme = ThemeColorItem(
                                "DEVELOPER", "developer_theme",
                                Color(0xFF131313), Color(0xFFE2B75A)
                            ),
                            isSelected = selectedTheme.equals("DEVELOPER", ignoreCase = true),
                            onSelect = { viewModel.setThemeOption("DEVELOPER") },
                            isDeveloper = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LIGHT THEMES SUB-SECTION
                    Text(
                        text = getString("light_mode_theme"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lightThemes.forEach { theme ->
                            ThemeOptionRow(
                                theme = theme,
                                isSelected = selectedTheme.equals(theme.key, ignoreCase = true),
                                onSelect = { viewModel.setThemeOption(theme.key) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // DARK THEMES SUB-SECTION
                    Text(
                        text = getString("dark_mode_theme"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        darkThemes.forEach { theme ->
                            ThemeOptionRow(
                                theme = theme,
                                isSelected = selectedTheme.equals(theme.key, ignoreCase = true),
                                onSelect = { viewModel.setThemeOption(theme.key) }
                            )
                        }
                    }
                }
            }

            // INSTRUCTIONS — every guided walkthrough sits in one category, so
            // "how does this work?" has a single answer in Settings instead of one
            // card per concept scattered down the page.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = getString("instructions_title"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // LEVELS & STAGES — the v5 progression explainer with graphical
                // examples, following the expandable-FAQ pattern.
                GuideSectionCard(
                    title = getString("level_system_guide_title"),
                    description = getString("level_system_guide_desc"),
                    expanded = showLevelGuide,
                    onToggle = {
                        showLevelGuide = !showLevelGuide
                        if (showLevelGuide) {
                            showFAQ = false
                            showSupportCard = false
                            showOverallLevelGuide = false
                            showRankGuide = false
                        }
                    }
                ) {
                    LevelSystemGuideContent()
                }

                // OVERALL LEVEL — the app-wide average. It lives here rather than
                // on the live workout tab, which is for logging the set at hand.
                GuideSectionCard(
                    title = getString("overall_level_label"),
                    description = getString("overall_level_guide_desc"),
                    expanded = showOverallLevelGuide,
                    onToggle = {
                        showOverallLevelGuide = !showOverallLevelGuide
                        if (showOverallLevelGuide) {
                            showFAQ = false
                            showSupportCard = false
                            showLevelGuide = false
                            showRankGuide = false
                        }
                    }
                ) {
                    // The long form: the reader who opened this card asked what the
                    // number is, so they get the words first and the picture after.
                    OverallLevelExplainer(detailed = true)
                }

                // RANKS — the letter tiers that sit on top of the overall level.
                // Its own card rather than a fourth panel inside the Overall Level
                // guide: the table is eight rows of its own content, and someone
                // who wants "what does S mean" should not have to open the
                // explanation of the number first.
                GuideSectionCard(
                    title = getString("rank_system_guide_title"),
                    description = getString("rank_system_guide_desc"),
                    expanded = showRankGuide,
                    onToggle = {
                        showRankGuide = !showRankGuide
                        if (showRankGuide) {
                            showFAQ = false
                            showSupportCard = false
                            showLevelGuide = false
                            showOverallLevelGuide = false
                        }
                    }
                ) {
                    RankTableContent()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FAQ SECTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (showFAQ) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        showFAQ = !showFAQ 
                        if (showFAQ) {
                            showSupportCard = false
                            showLevelGuide = false
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getString("rpe_rir_faq"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getString("rpe_rir_explanation"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(onClick = { 
                            showFAQ = !showFAQ 
                            if (showFAQ) {
                                showSupportCard = false
                                showLevelGuide = false
                            }
                        }) {
                            Icon(
                                imageVector = if (showFAQ) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = showFAQ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            FAQContent()
                        }
                    }
                }
            }

            // CUSTOMER SUPPORT SECTION
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        showSupportCard = true 
                        showFAQ = false
                        showLevelGuide = false
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getString("support_requests"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getString("submit_feedback"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = { 
                                showSupportCard = true 
                                showFAQ = false
                                showLevelGuide = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .semantics { testTag = "settings_support_button" }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getString("open"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .height(if (showFAQ) 16.dp else 0.dp)
            )
        }
    }

    if (showSupportCard) {
        ModalBottomSheet(
            onDismissRequest = { showSupportCard = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = getString("support_requests"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))
                SupportTicketForm(viewModel)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SupportTicketForm(viewModel: WorkoutViewModel) {
    val supportUiState by viewModel.supportUiState.collectAsState()
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Reset fields on success
    LaunchedEffect(supportUiState.success) {
        if (supportUiState.success) {
            subject = ""
            message = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getString("contact_us"),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedTextField(
            value = subject,
            onValueChange = { 
                subject = it
                viewModel.clearSupportStatus()
            },
            label = { Text(getString("subject"), style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !supportUiState.isSubmitting
        )

        OutlinedTextField(
            value = message,
            onValueChange = { 
                message = it
                viewModel.clearSupportStatus()
            },
            label = { Text(getString("message"), style = MaterialTheme.typography.bodySmall) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            enabled = !supportUiState.isSubmitting
        )

        if (supportUiState.error != null) {
            Column {
                Text(
                    text = if (supportUiState.error == "please_wait_cooldown") {
                        getString(supportUiState.error!!, supportUiState.cooldownMessage)
                    } else {
                        getString(supportUiState.error!!)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                supportUiState.errorDetail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (supportUiState.success) {
            Text(
                text = getString("ticket_sent_success"),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50) // Green
            )
        }

        Button(
            onClick = {
                viewModel.sendSupportTicket(subject, message)
            },
            enabled = !supportUiState.isSubmitting && !supportUiState.isCooldownActive,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            if (supportUiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(getString("send_ticket"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    theme: ThemeColorItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isRainbow: Boolean = false,
    isDeveloper: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { testTag = "theme_option_${theme.key}" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val rainbowBrush = androidx.compose.ui.graphics.Brush.sweepGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(theme.previewBackground)
                    .border(
                        width = 2.dp,
                        brush = if (isRainbow) rainbowBrush else androidx.compose.ui.graphics.SolidColor(theme.previewPrimary),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = getString(theme.nameKey),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )}

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FAQContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // RPE Section
        Column {
            Text(
                text = getString("what_is_rpe"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getString("rpe_desc"),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = getString("rpe_10"), style = MaterialTheme.typography.bodySmall)
            Text(text = getString("rpe_9"), style = MaterialTheme.typography.bodySmall)
            Text(text = getString("rpe_8"), style = MaterialTheme.typography.bodySmall)
        }

        // RIR Section
        Column {
            Text(
                text = getString("what_is_rir"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getString("rir_desc"),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = getString("rir_0"), style = MaterialTheme.typography.bodySmall)
            Text(text = getString("rir_1"), style = MaterialTheme.typography.bodySmall)
            Text(text = getString("rir_2"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * One expandable walkthrough under the Instructions category: a title, a
 * one-line description and a chevron that reveals the graphic content.
 *
 * Both guides share this, so opening one looks exactly like opening the other —
 * same shape, same border highlight, same padding.
 */
@Composable
private fun GuideSectionCard(
    title: String,
    description: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val isDeveloper = LocalDeveloperSkin.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            width = 1.dp,
            color = if (expanded) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}
