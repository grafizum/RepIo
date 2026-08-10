package com.example.ui.screens

import android.widget.Toast
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
    
    // Flexible Anti-spam configuration
    val MAX_DAILY_TICKETS = 5
    val SHORT_COOLDOWN_MILLIS = 10 * 60 * 1000L // 10 mins after 2 tickets
    
    var lastSubmitTime by remember { mutableStateOf(sharedPrefs.getLong("last_submit_time", 0L)) }
    var dailyCount by remember { mutableStateOf(sharedPrefs.getInt("daily_ticket_count", 0)) }
    var lastResetDay by remember { mutableStateOf(sharedPrefs.getLong("last_reset_day", 0L)) }

    val currentTime = System.currentTimeMillis()
    
    // Check if we need to reset the daily counter (if last reset was not today)
    val isNewDay = !android.text.format.DateUtils.isToday(lastResetDay)
    val effectiveDailyCount = if (isNewDay) 0 else dailyCount

    // Flexible logic:
    // 1. If daily limit (5) is reached -> Block for day
    // 2. If short-term limit (2) is reached -> 10 min cooldown
    val isDailyLimitReached = effectiveDailyCount >= MAX_DAILY_TICKETS
    val isShortCooldownActive = effectiveDailyCount >= 2 && (currentTime - lastSubmitTime < SHORT_COOLDOWN_MILLIS)
    
    val isCooldownActive = isDailyLimitReached || isShortCooldownActive
    
    val cooldownMessage = when {
        isDailyLimitReached -> "Daily limit reached. Please try again tomorrow."
        isShortCooldownActive -> {
            val remaining = ((SHORT_COOLDOWN_MILLIS - (currentTime - lastSubmitTime)) / 60000) + 1
            "Please wait $remaining min before sending another ticket."
        }
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // COMPACT HEADER (Matches HomeScreen)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Support",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SEND TICKET CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Contact Us",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Send us your questions or feedback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message", style = MaterialTheme.typography.bodySmall) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isCooldownActive) {
                            Text(
                                text = cooldownMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (isCooldownActive) return@Button

                                if (subject.isBlank() || message.isBlank()) {
                                    Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (!isNetworkAvailable(context)) {
                                    Toast.makeText(context, "Internet unavailable or Server unreachable. Please check your connection.", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                isSubmitting = true
                                val db = FirebaseFirestore.getInstance()

                                // Manual 10-second timeout to prevent infinite loading
                                val timeoutHandler = Handler(Looper.getMainLooper())
                                val timeoutRunnable = Runnable {
                                    if (isSubmitting) {
                                        isSubmitting = false
                                        Toast.makeText(context, "Server unreachable. Please try again later.", Toast.LENGTH_LONG).show()
                                    }
                                }
                                timeoutHandler.postDelayed(timeoutRunnable, 10000L)

                                val ticket = hashMapOf(
                                    "subject" to subject,
                                    "message" to message,
                                    "timestamp" to System.currentTimeMillis(),
                                    "status" to "open",
                                    "device_model" to android.os.Build.MODEL
                                )

                                db.collection("support_tickets")
                                    .add(ticket)
                                    .addOnSuccessListener {
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
                                        Toast.makeText(context, "Ticket sent successfully!", Toast.LENGTH_LONG).show()
                                    }
                                    .addOnFailureListener { e ->
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
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Ticket", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // SMALL FAQ SECTION
            item {
                Text(
                    text = "Quick FAQ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                FaqItem(
                    question = "How to change my workout program?",
                    answer = "Go to the 'My Workout' tab and tap 'Custom Workout' at the top to select a new template or build your own."
                )
            }

            item {
                FaqItem(
                    question = "Where are my past workout logs?",
                    answer = "You can view your progress and weekly summaries in the 'Analytics' tab."
                )
            }

            item {
                FaqItem(
                    question = "How to keep screen on while training?",
                    answer = "Tap the settings (gear) icon on the Home screen and toggle 'Keep Screen On'."
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
