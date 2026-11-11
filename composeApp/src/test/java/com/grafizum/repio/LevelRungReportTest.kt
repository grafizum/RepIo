package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Test
import java.io.File

/**
 * Measurement for two owner reports (board 2026-11-11): "on weights makes no
 * sense when its only one weight", and "from the 12th reps level to the 13th
 * there is no in between reps (12 12 13..)".
 *
 * Walks the real pool and lists every level whose displayed prescription repeats
 * the level below it - one line per repeat, on the load column and on the reps
 * column - so the fix is aimed at the rows that actually ship a dead rung.
 */
class LevelRungReportTest {

    @Test
    fun report() {
        val dir = File("build/reports/level-rungs").apply { mkdirs() }
        val detail = StringBuilder("name,unit,levels,level,load,reps\n")
        val loadDup = mutableListOf<String>()
        val repDup = mutableListOf<String>()
        val singleWeight = mutableListOf<String>()

        for (entry in ExerciseCatalog.ENTRIES) {
            val prog = ProgressionEngine.entryFor(entry.name) ?: continue
            val eff = ProgressionEngine.effectiveMaxLevel(prog).coerceIn(1, 80)
            var prevLoad: Double? = null
            var prevReps: Int? = null
            val seenLoads = LinkedHashSet<Double>()
            for (level in 1..eff) {
                val v = ProgressionEngine.prescribedValueAtLevel(prog, level)?.value
                val reps = ProgressionEngine.prescribedReps(prog, level, 1)?.firstOrNull()
                detail.append("\"${entry.name}\",${prog.unit},$eff,$level,${v ?: ""},${reps ?: ""}\n")
                if (v != null) seenLoads.add(v)
                if (v != null && v == prevLoad) {
                    loadDup.add("${entry.name} | ${prog.unit} | level $level repeats ${v}")
                }
                if (reps != null && reps == prevReps) {
                    repDup.add("${entry.name} | ${prog.unit} | level $level repeats ${reps} reps")
                }
                prevLoad = v
                prevReps = reps
            }
            if (seenLoads.size == 1 && eff > 1) {
                singleWeight.add("${entry.name} | ${prog.unit} | ${seenLoads.first()} at all $eff levels")
            }
        }

        File(dir, "levels.csv").writeText(detail.toString())
        val summary = buildString {
            appendLine("SINGLE-WEIGHT LADDERS (one load at every level): ${singleWeight.size}")
            singleWeight.take(40).forEach { appendLine("  $it") }
            appendLine()
            appendLine("LOAD LADDERS WITH A REPEATED RUNG: ${loadDup.size}")
            loadDup.take(60).forEach { appendLine("  $it") }
            appendLine()
            appendLine("REP LADDERS WITH A REPEATED RUNG: ${repDup.size}")
            repDup.take(60).forEach { appendLine("  $it") }
        }
        File(dir, "summary.txt").writeText(summary)
    }
}
