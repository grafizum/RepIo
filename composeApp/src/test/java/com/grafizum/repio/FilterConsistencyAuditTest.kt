package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import com.grafizum.repio.data.WorkoutTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * THE WIDER FILTER AUDIT: generalises the owner's chair/bench report to every
 * filter the "create the weekly workout" path applies to the database.
 *
 * The report was "I only have chair and dumbell and yet the system asks me to do
 * an inclinde dumbell press which doesnt work without a bench". That is ONE
 * instance of a class: **a row survives the filter while the kit it is shown to
 * cannot perform it** (over-display), or **a row that the kit CAN perform is
 * never shown** (under-display). Both come from the same root cause - the tags
 * the gate REQUIRES and the tags a tile GRANTS drifting apart.
 *
 * This sweeps all four dimensions at once, and asserts each one, so the class
 * cannot come back through a different door:
 *
 *  1. VOCABULARY CLOSURE - every tag any requirement names must be granted by
 *     ticking some selectable tile (or a preset). Formerly `kettlebell_and_bench`
 *     and `dumbbells_and_floor` were required but granted by nothing, so the rows
 *     asking for them were silently invisible to a custom selection.
 *  2. CROSS-FAMILY REACH - no row of one equipment family may be reachable by a
 *     pure kit of a DIFFERENT family (a "Dumbbell" row a barbell kit can do).
 *  3. NO-KIT REACH - the strict bodyweight preset (no tags at all) must not reach
 *     a row that NAMES an implement or a piece of furniture. This is the chair/
 *     bench class, asserted over the whole pool rather than the one row reported.
 *  4. END-TO-END GENERATION - for every preset and every split the generator can
 *     produce, no part may be left with an empty gated pool, and every exercise
 *     it emits must be performable from the preset's own kit.
 *
 * The full sweep is written to `build/reports/level-system-audit/filter-leaks.txt`.
 * Regenerate with:
 *   ./gradlew :composeApp:testDebugUnitTest --tests "*FilterConsistencyAuditTest*"
 */
class FilterConsistencyAuditTest {

    private data class Family(val label: String, val category: String, val tags: Set<String>)

    private fun tag(vararg names: String) = names.toSet()

    private val families = listOf(
        Family("dumbbells", "Dumbbell", tag(
            ExerciseEquipmentTags.DUMBBELLS_ONLY,
            ExerciseEquipmentTags.DUMBBELLS_AND_FLOOR,
            ExerciseEquipmentTags.DUMBBELLS_AND_BENCH,
        )),
        Family("barbell", "Barbell", tag(
            ExerciseEquipmentTags.BARBELL_AND_RACK,
            ExerciseEquipmentTags.BARBELL_AND_FLOOR,
            ExerciseEquipmentTags.BARBELL_ONLY,
        )),
        Family("kettlebell", "Kettlebell", tag(
            ExerciseEquipmentTags.KETTLEBELL_ONLY,
            ExerciseEquipmentTags.KETTLEBELL_AND_BENCH,
        )),
        Family("cable", "Cable", tag(ExerciseEquipmentTags.CABLE_MACHINE)),
        Family("machines", "Machine", tag(
            ExerciseEquipmentTags.MACHINE_OTHER,
            ExerciseEquipmentTags.LEG_PRESS_MACHINE,
            ExerciseEquipmentTags.LAT_PULLDOWN_MACHINE,
            ExerciseEquipmentTags.SMITH_MACHINE,
            ExerciseEquipmentTags.LEG_EXTENSION_MACHINE,
            ExerciseEquipmentTags.LEG_CURL_MACHINE,
            ExerciseEquipmentTags.CHEST_PRESS_MACHINE,
            ExerciseEquipmentTags.SEATED_ROW_MACHINE,
        )),
        Family("bands", "Bands", tag(ExerciseEquipmentTags.RESISTANCE_BANDS)),
    )

    /** Categories the cross-check applies to (Bodyweight/Other are exempt). */
    private val checkedCategories = families.map { it.category }.toSet()

    private fun render(reqs: List<Set<String>>): String =
        reqs.joinToString(" OR ") { set -> set.sorted().joinToString("+") }

    /** Every tag a user can obtain by ticking a tile, or from a normal preset. */
    private fun grantableTags(): Set<String> {
        val byTile = ExerciseEquipmentTags.SELECTABLE_TAGS
            .flatMap { ExerciseEquipmentTags.tagsGrantedByTicking(it) }
        val byPreset = ExerciseEquipmentTags.PRESET_IDS
            // Full gym grants EVERYTHING, so it would satisfy any requirement
            // vacuously; the question is whether a normal kit can reach the tag.
            .filter { it != ExerciseEquipmentTags.PRESET_FULL_GYM }
            .flatMap { ExerciseEquipmentTags.presetTags(it) }
        return (byTile + byPreset).toSet()
    }

    /** A row is a LOADED movement if it names a weight implement. */
    private val implementWords =
        Regex("\\b(barbell|ez[- ]?bar|dumbbell|kettlebell|cable|machine|smith)\\b")

    /** Furniture/angle words a bodyweight movement may legitimately need. */
    private val furnitureWords = Regex("\\b(bench|chair|step|box|decline|incline)\\b")

    /**
     * Rows whose NAME reads as furniture or an implement but which correctly
     * require none, each with the reason. Verified by hand - a pose called
     * "Chair" is not performed on a chair, and a floor dip is not a bar dip.
     */
    private val noKitExceptions = setOf(
        "Chair Pose", "Revolved Chair Pose", // yoga poses; the word is the shape
    )

    private fun emptyKitReaches(name: String) =
        ExerciseEquipmentTags.isAvailable(name, emptySet())

    @Test
    fun everyRequiredTagIsReachableByTickingATile() {
        val grantable = grantableTags()
        val unreachable = ExerciseCatalog.ENTRIES
            .flatMap { ExerciseEquipmentTags.requirementsFor(it.name) }
            .flatten()
            .filter { it != ExerciseEquipmentTags.NONE_REQUIRED }
            .distinct()
            .filterNot { it in grantable }
            .sorted()
        assertTrue(
            "these tags are demanded by a requirement but no tile grants them, so " +
                "every row behind them is invisible to a tile-built kit: $unreachable",
            unreachable.isEmpty(),
        )
    }

    @Test
    fun noForeignFamilyReachesAnotherFamilysRow() {
        val leaks = mutableListOf<String>()
        for (entry in ExerciseCatalog.ENTRIES) {
            if (entry.equipment !in checkedCategories) continue
            for (family in families) {
                if (family.category == entry.equipment) continue
                if (!ExerciseEquipmentTags.isAvailable(entry.name, family.tags)) continue
                leaks.add(
                    "${entry.name} [category=${entry.equipment}] is reachable by the " +
                        "${family.label} kit -> requires " +
                        render(ExerciseEquipmentTags.requirementsFor(entry.name))
                )
            }
        }
        assertTrue("a foreign equipment family reaches these rows: ${leaks.sorted()}", leaks.isEmpty())
    }

    @Test
    fun everyRowIsReachableByAtLeastOneRealKit() {
        // The converse of the leak check, and the one that catches UNDER-display:
        // every row must be reachable by the kit of SOME family widened with the
        // home furniture tiles - a kit a real user can actually build by ticking.
        // A row that no such kit reaches is listed in the app and can never be
        // performed, which is the silent half of the same bug. Full gym is NOT
        // offered here (it grants everything, so it would satisfy this vacuously).
        // The bench, dip bars and rings moved to the GYM grid section on the
        // owner's 2026-10-13 ruling, but they are still tiles a user can tick, so
        // a "real kit" still contains them; only their SECTION changed.
        val home = ExerciseEquipmentTags.HOME_EQUIPMENT_TAGS.toSet() +
            setOf(
                ExerciseEquipmentTags.BENCH,
                ExerciseEquipmentTags.DIP_BARS,
                ExerciseEquipmentTags.GYMNASTIC_RINGS,
            )
        val realKits = families.flatMap { listOf(it.tags, it.tags + home) } + listOf(home)
        val orphans = ExerciseCatalog.ENTRIES
            .filterNot { entry -> realKits.any { ExerciseEquipmentTags.isAvailable(entry.name, it) } }
            .map { "${it.name} [${it.equipment}] -> ${render(ExerciseEquipmentTags.requirementsFor(it.name))}" }
        assertTrue(
            "no family kit (widened with the home furniture tiles) can perform these rows: $orphans",
            orphans.isEmpty(),
        )
    }

    @Test
    fun aKitWithNoEquipmentReachesNothingThatNamesKit() {
        // Over-display, the general form of the chair/bench report: the strict
        // bodyweight preset grants NO tags, so it may only reach movements that
        // genuinely need nothing - never one whose NAME names an implement.
        val offenders = ExerciseCatalog.ENTRIES
            .filter { emptyKitReaches(it.name) }
            .filter { implementWords.containsMatchIn(it.name.lowercase()) }
            .map { it.name }
        assertTrue(
            "a bodyweight-only user is offered movements that name equipment they " +
                "do not have: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun aKitWithNoEquipmentReachesNothingThatNamesFurniture() {
        val offenders = ExerciseCatalog.ENTRIES
            .filter { emptyKitReaches(it.name) }
            .filter { it.name !in noKitExceptions }
            .filter { furnitureWords.containsMatchIn(it.name.lowercase()) }
            .map { it.name }
        assertTrue(
            "a bodyweight-only user is offered furniture-dependent movements " +
                "(the chair-as-bench class): $offenders",
            offenders.isEmpty(),
        )
    }

    /**
     * The presets whose kit is EMPTY: a user who owns no weight kit and no
     * furniture. Only the furniture-free stretches and the bodyweight moves
     * pass, and of those the pool has no biceps movement at all - every biceps
     * row needs dumbbells, a bar, a cable, a kettlebell, rings, a pull-up bar or
     * a machine, so an equipment-free user's arm day has one slot less.
     *
     * That is a DATA gap, not a gate bug (the gate is right to block a barbell
     * curl), so it is pinned here rather than "fixed" by loosening a rule. The
     * assertion fails in BOTH directions: a new hole and a filled one.
     */
    private val bodyweightOnlyPresets = setOf(
        ExerciseEquipmentTags.PRESET_BODYWEIGHT,
        ExerciseEquipmentTags.PRESET_BW_PULLUP_BAR,
        ExerciseEquipmentTags.PRESET_BW_DIP_BARS,
        ExerciseEquipmentTags.PRESET_BW_BANDS,
    )

    private val knownBareKitGap = setOf("Biceps")

    @Test
    fun equipmentPresetsAndEverySplitLeaveNoPartWithoutAGatedExercise() {
        val equipmentPresets = ExerciseEquipmentTags.PRESET_IDS
            .filterNot { it == ExerciseEquipmentTags.PRESET_CUSTOM || it in bodyweightOnlyPresets }
        val failures = mutableListOf<String>()
        equipmentPresets.forEach { preset ->
            val equipment = listOf(preset)
            val granted = WorkoutTemplates.grantedTagsForPublic(equipment) ?: emptySet()
            for (days in 1..6) {
                val program = WorkoutTemplates.generateEngineProgram(
                    selectedDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
                        .take(days),
                    workoutGoal = "goal_bodybuilding",
                    equipment = equipment,
                )
                // The adjacency rule's own report is separate; only the FILTER
                // hole is asserted here - a part with no exercise its kit can do.
                program.unsatisfiedParts
                    .filter { it.contains("no exercise left in the gated pool") }
                    .forEach { failures.add("[$preset] $it") }
                program.exercises.forEach { ex ->
                    if (!ExerciseEquipmentTags.isAvailable(ex.name, granted)) {
                        failures.add("[$preset] '${ex.name}' is not performable from the preset kit")
                    }
                }
            }
        }
        assertTrue("filter holes found: ${failures.distinct().sorted()}", failures.isEmpty())
    }

    @Test
    fun aBareKitGapIsExactlyTheKnownDataGapAndNothingElse() {
        val holes = mutableSetOf<String>()
        bodyweightOnlyPresets.forEach { preset ->
            for (days in 1..6) {
                val program = WorkoutTemplates.generateEngineProgram(
                    selectedDays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
                        .take(days),
                    workoutGoal = "goal_bodybuilding",
                    equipment = listOf(preset),
                )
                program.unsatisfiedParts
                    .filter { it.contains("no exercise left in the gated pool") }
                    .forEach { holes.add(it.substringBefore(" (")) }
            }
        }
        assertEquals(
            "the equipment-free kits should leave exactly the known data gap" +
                " (no equipment-free biceps movement exists); anything else is a" +
                " filter hole, and an empty set means the gap was filled and this" +
                " expectation should be retired",
            knownBareKitGap,
            holes,
        )
    }

    @Test
    fun report() {
        val dir = File("build/reports/level-system-audit").apply { mkdirs() }
        val lines = StringBuilder()
        lines.appendLine("FILTER CONSISTENCY AUDIT (every dimension of the weekly-build path)")
        lines.appendLine()
        lines.appendLine("TAGS A TILE CAN GRANT:")
        ExerciseEquipmentTags.SELECTABLE_TAGS.sorted().forEach { tile ->
            lines.appendLine("  $tile -> ${ExerciseEquipmentTags.tagsGrantedByTicking(tile).sorted()}")
        }
        lines.appendLine()
        lines.appendLine("REQUIREMENTS PER ROW:")
        for (entry in ExerciseCatalog.ENTRIES.sortedBy { it.equipment }) {
            lines.appendLine(
                "  [${entry.equipment}] ${entry.name} -> " +
                    render(ExerciseEquipmentTags.requirementsFor(entry.name))
            )
        }
        File(dir, "filter-leaks.txt").writeText(lines.toString())

        assertEquals(
            "the sweep must cover the pool",
            601,
            ExerciseCatalog.ENTRIES.size,
        )
        assertTrue(File(dir, "filter-leaks.txt").length() > 0)
    }
}
