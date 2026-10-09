package com.grafizum.repio.ui.screens

import com.grafizum.repio.ui.components.LocalSafeArea
import com.grafizum.repio.ui.components.LocalWindowSize
import com.grafizum.repio.ui.components.appLanguageOptions
import com.grafizum.repio.ui.components.appLanguageShortTag
import com.grafizum.repio.ui.screens.LevelSystemGuideExamples
import com.grafizum.repio.ui.screens.OverallLevelExplainer
import com.grafizum.repio.ui.theme.LocalDeveloperSkin
import com.grafizum.repio.ui.theme.LocalLightTheme
import com.grafizum.repio.util.getString
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The one-time walkthrough.
 *
 * Both explainers as graphics — how levels and stages move you up, and how the
 * Overall Level adds those levels up — so a new user can scan the whole system in
 * a few seconds.
 *
 * Shown once per install (after the welcome setup on fresh installs, at first
 * launch for updating users), and re-armed when a new routine replaces the old
 * one. Dismissible from the top corner and the bottom button.
 *
 * Design rule (same look on every phone, the S22+ as the benchmark): the page is
 * a stack of SURFACED SECTIONS — one hero band, one example-card section per topic,
 * a reassurance strip, and the CTA — so a large accessibility font grows the type but
 * not the layout: every section is a card, so text wraps inside a surface instead of
 * threading edge-to-edge and touching the next row.
 */
@Composable
fun LevelGuideOnboardingScreen(
    onDismiss: () -> Unit,
    appLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var dismissing by remember { mutableStateOf(false) }
    val dismissOnce: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            onDismiss()
        }
    }

    val window = LocalWindowSize.current
    val isDeveloper = LocalDeveloperSkin.current
    val isLight = LocalLightTheme.current

    // The system's own chrome, reserved HERE because this page is not inside the
    // app's Scaffold and so inherits none of its insets. Added to the page's own
    // margin rather than replacing it, so the page keeps the app's rhythm and the
    // system bars simply stop being drawn over.
    //
    // Measured on the Pixel 7a (status bar 45dp, gesture bar 48dp): with the page
    // margin alone, the language/close row sat 29dp under the status bar and the
    // closing CTA was painted under the gesture area - behind the OS's opaque
    // navigation bar, so the user could neither see nor tap it. On the S22+
    // (28dp / 15dp) the same 16dp margin cleared both, which is why a full pass over
    // the S22+ alone never caught it.
    val safeArea = LocalSafeArea.current

    // Page frame + scroll. The frame rides the token scale, so at font 1.8 the
    // page's outer margins match the text's growth instead of staying at a 20dp
    // constant (the crowding the token rule exists to prevent).
    //
    // THIS PAGE MUST PAINT ITS OWN BACKGROUND, because it is not inside the app's
    // Scaffold and therefore inherits none of it.
    //
    // OWNER REPORT, 2026-10-08: "in the new feature screen the background color of the
    // light beige screen was a weird ugly grey". Measured on the Pixel 7a, the page
    // was painting the WINDOW's colour, not the chosen theme's: #303030 on the base
    // activity (AppCompat's DayNight default window background, since
    // `Theme.MyApplication` set none) and #121212 under the Retro/developer alias.
    // Two flat greys, with warm beige cards (#F9F7F4 / #F4F1ED) sitting on top.
    //
    // The window colour cannot be the right answer for this page even in principle:
    // it comes from the ENABLED LAUNCHER ALIAS's XML theme, while the colours the user
    // is looking at come from a PREFERENCE inside the app (`theme_option`). Those are
    // two independent choices - the alias follows the launcher icon, the preference
    // follows the theme picker - and this screen is exactly the one that runs before
    // the app's own shell has drawn anything. Painting `background` here means the page
    // always matches the theme the rest of the app is about to show.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                top = window.screenPadding + safeArea.top,
                bottom = window.screenPadding + safeArea.bottom,
                start = window.screenPadding + safeArea.start,
                end = window.screenPadding + safeArea.end
            ),
        verticalArrangement = Arrangement.spacedBy(window.sectionSpacing)
    ) {
        // Top corner row: language control in the start, dismiss in the end.
        // Laid out on a single row so neither grows into the other at any font.
        TopCornerRow(
            appLanguage = appLanguage,
            dismissOnce = dismissOnce,
            onLanguageSelected = onLanguageSelected
        )

        // HERO: title + one-line intro together in one surfaced band. Both stay
        // centered and bounded, so the page reads as ONE subject before the two
        // examples, rather than two headlines back to back.
        HeroBand(
            title = getString("level_guide_onboarding_title"),
            intro = getString("guide_onboarding_intro")
        )

        // SECTION 1 — how levels and stages move you up. One card = one chapter.
        ExampleSectionCard(
            heading = getString("level_system_guide_title"),
            content = { LevelSystemGuideExamples() }
        )

        // SECTION 2 — how the Overall Level adds those levels up. Same grammar
        // as section 1, so the two topics look like peers instead of one long list.
        ExampleSectionCard(
            heading = getString("overall_level_how_title"),
            content = { OverallLevelExplainer() }
        )

        // Reassurance stripe: icon + one sentence. A short card, not a bare row
        // of text jammed between the examples and the button.
        ReassuranceStrip(
            message = getString("level_guide_reassurance")
        )

        // CTA: full-width button with enough height that it is always a clear
        // tap target, not a sliver squeezed between two text rows.
        Spacer(modifier = Modifier.height(window.sectionSpacing))
        CtaButton(
            onClick = dismissOnce,
            label = getString("level_guide_cta_got_it")
        )
    }
}

// ----------------------------------------------------------------------
// Page-level pieces that define the look: hero band, example card section,
// reassurance strip, corner row, CTA. All use the same surfaced-section
// grammar, so the page reads the same at every font scale.
// ----------------------------------------------------------------------

@Composable
private fun TopCornerRow(
    appLanguage: String,
    dismissOnce: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
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
}

@Composable
private fun HeroBand(title: String, intro: String) {
    val isDeveloper = LocalDeveloperSkin.current
    val isLight = LocalLightTheme.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isLight) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
                     1.dp,
                     MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                 ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExampleSectionCard(
    heading: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ReassuranceStrip(message: String) {
    val isDeveloper = LocalDeveloperSkin.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CtaButton(
    onClick: () -> Unit,
    label: String
) {
    val isDeveloper = LocalDeveloperSkin.current

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

// ----------------------------------------------------------------------
// Shared pieces reused from the rest of the app: language control and the
// guide example graphics. Kept as thin wrappers here so the onboarding page's
// own look is in this file only.
// ----------------------------------------------------------------------

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
            contentPadding = PaddingValues(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            ),
            modifier = Modifier.semantics {
                contentDescription = label
            }
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = appLanguageShortTag(appLanguage),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            // ArrowDropDown is not material-icons-core on this dependency set;
            // the menu button is still tappable and label-led without it.
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
