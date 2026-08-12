package com.grafizum.repio.ui.screens

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.grafizum.repio.R
import com.grafizum.repio.ui.viewmodel.WorkoutViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.util.Log

data class ThemeColorItem(
    val key: String,
    val nameResId: Int,
    val previewPrimary: Color,
    val previewBackground: Color
)

val lightThemes = listOf(
    ThemeColorItem("BEIGE", R.string.warm_beige, Color(0xFF8C6239), Color(0xFFF7F4EE)),
    ThemeColorItem("EMERALD", R.string.emerald_mint, Color(0xFF00796B), Color(0xFFF1F8F7)),
    ThemeColorItem("CRIMSON", R.string.crimson_red, Color(0xFFC62828), Color(0xFFFFFAFA)),
    ThemeColorItem("AZURE", R.string.azure_sky, Color(0xFF007AFF), Color.White)
)

val darkThemes = listOf(
    ThemeColorItem("PURPLE", R.string.elegant_purple, Color(0xFFD0BCFF), Color(0xFF1C1B1F)),
    ThemeColorItem("FLAME", R.string.flame_orange, Color(0xFFFF6D00), Color(0xFF121212)),
    ThemeColorItem("OCEAN", R.string.ocean_teal, Color(0xFF00B8D4), Color(0xFF0F1E24)),
    ThemeColorItem("ROSE", R.string.rose_pink, Color(0xFFF06292), Color(0xFF28111A))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val selectedTheme by viewModel.themeOption.collectAsState()
    val defaultRestTimerSeconds by viewModel.defaultRestTimerSeconds.collectAsState()
    val defaultExerciseTimerSeconds by viewModel.defaultExerciseTimerSeconds.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val showRestartDialog by viewModel.showRestartDialog.collectAsState()
    val context = LocalContext.current
    var expandedLanguage by remember { mutableStateOf(false) }
    var showSupportCard by remember { mutableStateOf(false) }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { /* Force user to click OK to apply changes correctly */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = { Text(stringResource(R.string.restart_required_title)) },
            text = { Text(stringResource(R.string.restart_required_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyThemeRestart()
                        (context as? Activity)?.finishAffinity()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // COMPACT HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MAIN SETTINGS CONTENT
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
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
                                text = stringResource(R.string.language),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.change_app_language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val languageOptions = listOf(
                        "system" to stringResource(R.string.system_default),
                        "en" to stringResource(R.string.english),
                        "de" to stringResource(R.string.german),
                        "it" to stringResource(R.string.italian)
                    )

                    val selectedLanguageLabel = languageOptions.find { it.first == appLanguage }?.second ?: stringResource(R.string.system_default)

                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { expandedLanguage = !expandedLanguage },
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

            // SET REST TIMER DURATION CARD
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
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.resting_timer_duration),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.auto_countdown_interval),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val timerOptions = listOf(30, 45, 60, 90, 120, 180)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        timerOptions.forEach { seconds ->
                            val isSelected = defaultRestTimerSeconds == seconds
                            Button(
                                onClick = { viewModel.setDefaultRestTimerSeconds(seconds) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .semantics { testTag = "rest_timer_option_${seconds}s" }
                            ) {
                                val timerLabel = when (seconds) {
                                    120 -> "2m"
                                    180 -> "3m"
                                    else -> "${seconds}s"
                                }
                                Text(
                                    text = timerLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // EXERCISE TRANSITION TIMER DURATION CARD
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
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.exercise_transition_timer),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.exercise_transition_timer_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val transitionOptions = listOf(60, 90, 120, 180, 240, 300)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        transitionOptions.forEach { seconds ->
                            val isSelected = defaultExerciseTimerSeconds == seconds
                            Button(
                                onClick = { viewModel.setDefaultExerciseTimerSeconds(seconds) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .semantics { testTag = "exercise_timer_option_${seconds}s" }
                            ) {
                                val timerLabel = when (seconds) {
                                    60 -> "1m"
                                    120 -> "2m"
                                    180 -> "3m"
                                    240 -> "4m"
                                    300 -> "5m"
                                    else -> "${seconds}s"
                                }
                                Text(
                                    text = timerLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

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
                                    text = stringResource(R.string.keep_screen_on),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.force_screen_stay_active),
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
                            text = stringResource(R.string.color_theme),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LIGHT THEMES SUB-SECTION
                    Text(
                        text = stringResource(R.string.light_mode_theme),
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
                        text = stringResource(R.string.dark_mode_theme),
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

            // CUSTOMER SUPPORT SECTION
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.support_requests),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.submit_feedback),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Button(
                            onClick = { showSupportCard = !showSupportCard },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.semantics { testTag = "settings_support_button" }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ContactSupport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(if (showSupportCard) R.string.close else R.string.open_support))
                        }
                    }

                    AnimatedVisibility(visible = showSupportCard) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            SupportTicketForm()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SupportTicketForm() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("support_prefs", Context.MODE_PRIVATE) }

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    val MAX_DAILY_TICKETS = 5
    val SHORT_COOLDOWN_MILLIS = 10 * 60 * 1000L
    
    var lastSubmitTime by remember { mutableStateOf(sharedPrefs.getLong("last_submit_time", 0L)) }
    var dailyCount by remember { mutableStateOf(sharedPrefs.getInt("daily_ticket_count", 0)) }
    var lastResetDay by remember { mutableStateOf(sharedPrefs.getLong("last_reset_day", 0L)) }

    val currentTime = System.currentTimeMillis()
    val isNewDay = !android.text.format.DateUtils.isToday(lastResetDay)
    val effectiveDailyCount = if (isNewDay) 0 else dailyCount

    val isDailyLimitReached = effectiveDailyCount >= MAX_DAILY_TICKETS
    val isShortCooldownActive = effectiveDailyCount >= 2 && (currentTime - lastSubmitTime < SHORT_COOLDOWN_MILLIS)
    val isCooldownActive = isDailyLimitReached || isShortCooldownActive
    
    val cooldownMessage = when {
        isDailyLimitReached -> stringResource(R.string.daily_limit_reached)
        isShortCooldownActive -> {
            val remaining = ((SHORT_COOLDOWN_MILLIS - (currentTime - lastSubmitTime)) / 60000) + 1
            stringResource(R.string.please_wait_cooldown, remaining)
        }
        else -> ""
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
                text = stringResource(R.string.contact_us),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text(stringResource(R.string.subject), style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(stringResource(R.string.message), style = MaterialTheme.typography.bodySmall) },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        if (isCooldownActive) {
            Text(
                text = cooldownMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                if (isCooldownActive) return@Button

                if (subject.isBlank() || message.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.please_fill_fields), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!isNetworkAvailable(context)) {
                    Toast.makeText(context, context.getString(R.string.internet_unavailable), Toast.LENGTH_LONG).show()
                    return@Button
                }

                isSubmitting = true
                val db = FirebaseFirestore.getInstance()

                val timeoutHandler = Handler(Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (isSubmitting) {
                        isSubmitting = false
                        Toast.makeText(context, context.getString(R.string.server_unreachable), Toast.LENGTH_LONG).show()
                    }
                }
                timeoutHandler.postDelayed(timeoutRunnable, 10000L)

                val ticket = hashMapOf(
                    "subject" to subject,
                    "message" to message,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "status" to "open",
                    "device_model" to android.os.Build.MODEL,
                    "app_version" to "1.0.0"
                )

                Log.d("SupportTicket", "Attempting to send ticket: $ticket")

                db.collection("support_tickets")
                    .add(ticket)
                    .addOnSuccessListener { documentReference ->
                        Log.d("SupportTicket", "Ticket sent successfully with ID: ${documentReference.id}")
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        if (!isSubmitting) return@addOnSuccessListener

                        val now = System.currentTimeMillis()
                        val newDailyCount = if (isNewDay) 1 else effectiveDailyCount + 1
                        
                        sharedPrefs.edit()
                            .putLong("last_submit_time", now)
                            .putInt("daily_ticket_count", newDailyCount)
                            .putLong("last_reset_day", now)
                            .apply()
                            
                        lastSubmitTime = now
                        dailyCount = newDailyCount
                        lastResetDay = now
                        
                        isSubmitting = false
                        subject = ""
                        message = ""
                        Toast.makeText(context, context.getString(R.string.ticket_sent_success), Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("SupportTicket", "Failed to send ticket", e)
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        if (!isSubmitting) return@addOnFailureListener

                        isSubmitting = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            },
            enabled = !isSubmitting && !isCooldownActive,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.send_ticket), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    theme: ThemeColorItem,
    isSelected: Boolean,
    onSelect: () -> Unit
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
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(theme.previewBackground)
                    .border(
                        width = 2.dp,
                        color = theme.previewPrimary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(theme.nameResId),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

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
