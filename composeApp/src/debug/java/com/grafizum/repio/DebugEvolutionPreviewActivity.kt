package com.grafizum.repio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.components.EvolutionTreeDialog
import com.grafizum.repio.ui.theme.WorkoutTrackerTheme
import com.grafizum.repio.ui.viewmodel.MasteryCardData

/**
 * DEBUG SOURCE SET ONLY - not in any release build.
 *
 * Renders [EvolutionTreeDialog] against REAL `ProgressionEngine` data so the
 * Canvas drawing can be checked on a device. The popup otherwise only appears
 * when a workout is completed with a mastered exercise, which cannot be
 * provoked from adb, and the project has no ui-test-junit4 on the classpath, so
 * a Compose screenshot test is not an option either.
 *
 * It builds a `ProgressionState` at the exercise's own max level - i.e. the
 * state the popup is designed for - and takes the branches from the catalog,
 * so every number on screen is the engine's, not a fixture.
 */
class DebugEvolutionPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val which = intent.getStringExtra("exercise") ?: "Push-Up"
        setContent {
            WorkoutTrackerTheme(themeOption = "BEIGE") {
                Preview(which)
            }
        }
    }
}

@Composable
private fun Preview(exerciseName: String) {
    var visible by remember { mutableStateOf(true) }

    val entry = ProgressionEngine.entryFor(exerciseName)
    val state = entry?.let { ProgressionEngine.ProgressionState(level = it.maxLevel ?: 1, stage = 1) }

    val offers = remember(entry, state) {
        if (entry == null || state == null) emptyList() else entry.branches.mapNotNull { b ->
            ProgressionEngine.evolutionOffer(exerciseName, b.entry, state)
        }
    }

    val card = MasteryCardData(
        exerciseName = exerciseName,
        muscleGroup = "",
        equipment = "",
        level = state?.level ?: 1,
        stageRoman = "",
        load = null,
        unit = entry?.unit ?: "",
        reason = ProgressionEngine.MasteryReason.LADDER_COMPLETE,
        ceiling = null,
        branches = entry?.branches.orEmpty(),
        suggestions = emptyList(),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (visible && offers.isNotEmpty()) {
            EvolutionTreeDialog(
                card = card,
                offers = offers,
                onEvolve = { visible = false },
                onDecline = { visible = false },
                onDismiss = { visible = false },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clickable { visible = true },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(exerciseName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "offers=${offers.size} | maxLevel=${entry?.maxLevel} | tap to reopen",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}