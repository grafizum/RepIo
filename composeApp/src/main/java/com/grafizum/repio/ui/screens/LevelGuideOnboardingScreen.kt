package com.grafizum.repio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grafizum.repio.ui.components.appLanguageOptions
import com.grafizum.repio.ui.components.appLanguageShortTag
import com.grafizum.repio.util.getString

/**
 * The one-time walkthrough: both explainers as graphics — how levels and stages
 * move you up, and how the Overall Level adds those levels up — so a new user
 * can scan the whole system in a few seconds.
 *
 * Shown once per install (after the welcome setup on fresh installs, at first
 * launch for updating users), and re-armed when a new routine replaces the old
 * one. It is dismissible from the top corner as well as from the bottom button:
 * the content is long enough that forcing a scroll to the end would make the
 * thing a nag instead of a welcome.
 *
 * Every word on this screen comes from a string resource, and the language can be
 * changed from the corner control without leaving it — which is the one moment a
 * user is most likely to need that, since the page is the first thing they read.
 */
@Composable
fun LevelGuideOnboardingScreen(
    onDismiss: () -> Unit,
    appLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    // A dismissal guard closes the screen on the FIRST tap: marking the seen
    // flag and recomposing take a frame, and in that window a second press on
    // "Got it" would queue a second dismissal — the double-tap the owner saw.
    // Declared before the layout because both exits (corner ✕ and the button)
    // share it.
    var dismissing by remember { mutableStateOf(false) }
    val dismissOnce: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            onDismiss()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quiet corner bar. Deliberately not a filled button and not in the
        // reading path: it stays out of the way of the copy, but it is the first
        // thing someone who cannot read the page will scan for.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            LanguageSwitcher(
                appLanguage = appLanguage,
                onLanguageSelected = onLanguageSelected
            )
            IconButton(onClick = dismissOnce) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = getString("close"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            // Deliberately headlines the level system: it is the feature this
            // walkthrough introduces, and the Overall Level section below is part
            // of the same story (it averages those levels).
            text = getString("level_guide_onboarding_title"),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = getString("guide_onboarding_intro"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // 1 — levels and stages.
        GuideSectionHeading(getString("level_system_guide_title"))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LevelSystemGuideExamples()
        }

        // 2 — the Overall Level. Same graphic as Settings ▸ Instructions, so the
        // first-run explanation and the one the user can return to never differ.
        GuideSectionHeading(getString("overall_level_how_title"))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Short form here: the walkthrough already shows the same three-step
            // graphic as Settings, so repeating the long explanation would bury it.
            OverallLevelExplainer(detailed = false)
        }

        // Reassurance: early levels come straight from the setup choice and may
        // feel off — the system takes over from there automatically.
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = getString("level_guide_reassurance"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The primary action is still dismissal only: both systems explain
        // themselves from here on, and the details live in Settings ▸ Instructions.
        Button(
            onClick = dismissOnce,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = getString("level_guide_cta_got_it"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * The category label that separates the two explainers on the walkthrough, so
 * the page reads as two subjects rather than one very long one.
 */
@Composable
private fun GuideSectionHeading(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The small language control in the walkthrough's corner.
 *
 * Global symbol plus the current language's two-letter code — the shortest thing
 * that still reads as "this changes the language" to someone who cannot parse the
 * page. It is a text button with no fill and a low-emphasis tint, so it sits
 * below the title and the graphics in the visual hierarchy, but it keeps the full
 * Material tap target so it is never fiddly to hit. Selecting an option drives the
 * same preference Settings does, so the whole app switches immediately.
 */
@Composable
private fun LanguageSwitcher(
    appLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = getString("change_app_language")

    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            ),
            modifier = Modifier.semantics { contentDescription = label }
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = appLanguageShortTag(appLanguage),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            appLanguageOptions().forEach { (code, optionLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
