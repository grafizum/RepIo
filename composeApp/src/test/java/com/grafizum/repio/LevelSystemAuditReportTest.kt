package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * THE PHASE 0 MEASUREMENT for the level-system / equipment remake (board tasks
 * "Level 1 is too high …", "Inverted row requires a machine …" and the level
 * remake). It reads the REAL catalog, gate and engine - so the numbers are what
 * the app ships, not a re-derivation - and writes a CSV + a summary under
 * `build/reports/level-system-audit/`.
 *
 * It asserts only invariants that must hold whatever the data says, so it cannot
 * be "passed" by softening a threshold: the pool size, and that every pooled row
 * participates in the level system. Everything else it reports, because Phase 0
 * is measurement and the decisions belong to the owner.
 *
 * Regenerate with:
 *   ./gradlew :composeApp:testDebugUnitTest --tests "*LevelSystemAuditReportTest*"
 */
class LevelSystemAuditReportTest {

    private fun render(reqs: List<Set<String>>): String =
        reqs.joinToString("|") { set -> set.sorted().joinToString("+") }

    /** A stretch or pose names furniture but works on any of it - not a leak. */
    private fun isFurnitureOnlyMovement(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("stretch") || n.contains("pose") || n.contains("pull-in") ||
            n.contains("push-up") || n.contains("crunch") || n.contains("dip")
    }

    @Test
    fun report() {
        val dir = File("build/reports/level-system-audit").apply { mkdirs() }
        val csv = StringBuilder()
        csv.append(
            "name,equipment,mechanic,type,ready,unit,level1,mediumLevel,mediumValue," +
                "minValue,maxValue,step,effMaxLevel,ladderRungs,ladderTop,clampedLevels,requirements\n"
        )

        var totalRows = 0
        var readyRows = 0
        var loadRows = 0
        var durationRows = 0
        var otherRows = 0
        var clampedRows = 0
        var totalClamped = 0
        val loadLevel1 = mutableListOf<Triple<String, Double, String>>()

        val chairKit = setOf(
            ExerciseEquipmentTags.DUMBBELLS_ONLY,
            ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
            ExerciseEquipmentTags.CHAIR,
        )
        val dumbbellChairRows = mutableListOf<String>()
        val benchLeaks = mutableListOf<String>()

        for (entry in ExerciseCatalog.ENTRIES) {
            totalRows++
            val prog = ProgressionEngine.entryFor(entry.name)
            val ready = ProgressionEngine.isProgressionReady(prog)
            if (ready) readyRows++

            val reqText = render(ExerciseEquipmentTags.requirementsFor(entry.name))
            val chairAvailable = ExerciseEquipmentTags.isAvailable(entry.name, chairKit)
            if (chairAvailable && entry.equipment == "Dumbbell") dumbbellChairRows.add(entry.name)

            // An incline/decline/bench/chest-supported LOADED movement needs a real
            // bench (its angle or its elbow support is the point); a kitchen chair is
            // not one. Stretch/pose/crunch/push-up furniture work is excluded.
            val nameLower = entry.name.lowercase()
            val benchMovement = listOf("incline", "decline", "bench", "chest-supported", "chest supported")
                .any { nameLower.contains(it) }
            if (chairAvailable && benchMovement && entry.equipment == "Dumbbell" &&
                !isFurnitureOnlyMovement(entry.name)
            ) {
                benchLeaks.add(entry.name)
            }

            var unit = ""
            var level1 = ""
            var mediumLevel = ""
            var mediumValue = ""
            var minValue = ""
            var maxValue = ""
            var step = ""
            var effMax = ""
            var rungs = ""
            var top = ""
            var clamped = ""
            if (prog != null) {
                unit = prog.unit
                level1 = prog.level1?.toString() ?: ""
                mediumLevel = prog.mediumLevel?.toString() ?: ""
                mediumValue = prog.mediumValue?.toString() ?: ""
                minValue = prog.minValue?.toString() ?: ""
                maxValue = prog.maxValue?.toString() ?: ""
                step = prog.step?.toString() ?: ""
                if (ready) {
                    when (unit) {
                        "kg", "kg_per_kettlebell", "stack_step", "resistance_step" -> loadRows++
                        "seconds" -> durationRows++
                        else -> otherRows++
                    }
                    val eff = ProgressionEngine.effectiveMaxLevel(prog)
                    effMax = eff.toString()
                    // A CLAMPED level is one whose prescribed load equals the level
                    // below's - a level-up that changes nothing.
                    var previous: Double? = null
                    var repeats = 0
                    for (level in 1..eff) {
                        val v = ProgressionEngine.prescribedValueAtLevel(prog, level)?.value
                        if (v != null && previous != null && v == previous) repeats++
                        previous = v
                    }
                    clamped = repeats.toString()
                    if (repeats > 0) { clampedRows++; totalClamped += repeats }
                    top = (ProgressionEngine.prescribedValueAtLevel(prog, prog.maxLevel ?: 1)?.value)
                        ?.toString() ?: ""
                    rungs = prog.levelValues.size.toString()
                    if (unit.startsWith("kg")) {
                        prog.level1?.let { loadLevel1.add(Triple(entry.name, it, unit)) }
                    }
                }
            }

            csv.append(
                listOf(
                    entry.name, entry.equipment, entry.level, entry.type, ready.toString(),
                    unit, level1, mediumLevel, mediumValue, minValue, maxValue, step,
                    effMax, rungs, top, clamped, reqText,
                ).joinToString(",") { cell -> "\"" + cell.replace("\"", "'") + "\"" }
            ).append("\n")
        }

        File(dir, "rows.csv").writeText(csv.toString())

        val summary = buildString {
            appendLine("LEVEL-SYSTEM / EQUIPMENT PHASE 0 AUDIT")
            appendLine("total rows: $totalRows")
            appendLine("progression-ready: $readyRows")
            appendLine("load rows: $loadRows  duration rows: $durationRows  other: $otherRows")
            appendLine()
            appendLine("CLAMPED LEVELS (a level-up whose load does not change):")
            appendLine("  rows affected: $clampedRows")
            appendLine("  total dead levels: $totalClamped")
            appendLine()
            appendLine("KIT AUDIT (dumbbells + a chair, NO bench):")
            appendLine("  dumbbell rows a chair-only kit can reach: ${dumbbellChairRows.size}")
            appendLine("  BENCH movements wrongly reached by a chair: ${benchLeaks.size}")
            benchLeaks.sorted().forEach { appendLine("    - $it") }
            appendLine()
            appendLine("LEVEL-1 LOADS (kg rows): ${loadLevel1.size}")
            appendLine("  highest 15:")
            loadLevel1.sortedByDescending { it.second }.take(15).forEach {
                appendLine("    ${it.second} ${it.third}  ${it.first}")
            }
            val median = loadLevel1.map { it.second }.sorted().let {
                if (it.isEmpty()) 0.0 else it[it.size / 2]
            }
            appendLine("  median: $median")
        }
        File(dir, "summary.txt").writeText(summary)

        // Invariants that must hold whatever the numbers say.
        assertEquals("the pool size is a fixed fact about this build", 601, totalRows)
        assertEquals("every pooled row must participate in the level system", totalRows, readyRows)
        assertTrue(
            "the report must have been written (path: ${dir.absolutePath})",
            File(dir, "summary.txt").length() > 0,
        )
    }
}
