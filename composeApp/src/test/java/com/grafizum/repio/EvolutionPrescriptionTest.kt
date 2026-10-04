package com.grafizum.repio

import com.grafizum.repio.data.ExerciseProgression
import com.grafizum.repio.data.ProgressionEngine
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A branch target is not always a loaded exercise, and the evolution popup
 * must not pretend it is.
 *
 * Found ON THE DEVICE, not by reading: the popup rendered "Decline Push-Up —
 * Level 4 of 6 — **4 kg**". Decline Push-Up is a `ValueKind.VARIATION` row, so
 * its level-4 value is the ordinal of a variation in a sequence. Printing it
 * through the load path dressed the number 4 as kilograms on a bodyweight
 * push-up variation — the bodyweight-reads-as-a-weight bug this project has
 * already paid for three times (the chair/bench split, the coarse-tag round
 * trip, the seven mis-categorised rows).
 *
 * The mistake was reading `EvolutionSide.load`, which is a bare `Double?` with
 * no unit attached, and printing it with "kg". The value is only meaningful
 * next to the row's own `ValueKind`, which is what
 * `prescribedValueAtLevel` carries.
 */
class EvolutionPrescriptionTest {

    /**
     * Every branched exercise, at the level a popup would open it on.
     *
     * A target with NO value is kept as a null so the sweeps below can see it:
     * dropping it here (which is what this used to do, with `?.let`) would
     * quietly shrink the sweep, and the count assertion below would then pass
     * over rows nothing was checking.
     */
    private val targets: List<Pair<String, ProgressionEngine.PrescribedValue?>> =
        ExerciseProgression.ENTRIES
            .filter { it.branches.isNotEmpty() }
            .flatMap { source ->
                source.branches.mapNotNull { branch ->
                    val entry = ProgressionEngine.entryFor(branch.entry) ?: return@mapNotNull null
                    val state = ProgressionEngine.ProgressionState(
                        level = ProgressionEngine.branchStartLevel(
                            source, ProgressionEngine.ProgressionState(1, 1), entry
                        ),
                        stage = 1,
                    )
                    branch.entry to ProgressionEngine.prescribedValueAtLevel(entry, state.level)
                }
            }

    /**
     * A branch target the popup can print honestly.
     *
     * Two shapes qualify: a typed value (a load, a duration, a variation), or -
     * for a REP LADDER - the reps, which are its whole prescription since the
     * level stopped naming a movement. Anything else would leave the popup
     * with nothing to show and a "kg" fallback to guess with.
     */
    private fun honestRepsFor(name: String): List<Int>? {
        val entry = ProgressionEngine.entryFor(name) ?: return null
        val level = ProgressionEngine.clampLevel(entry, 1)
        return ProgressionEngine.prescribedReps(entry, level, 1)?.takeIf { it.isNotEmpty() }
    }

    @Test
    fun aVariationTargetIsNeverTreatedAsAWeight() {
        // The concrete regression: a push-up branch rendering "4 kg".
        val pushUp = ProgressionEngine.entryFor("Push-Up")
        assertTrue("Push-Up must be in the pool", pushUp != null)
        val variationTargets = pushUp!!.branches.mapNotNull { branch ->
            val entry = ProgressionEngine.entryFor(branch.entry) ?: return@mapNotNull null
            if (ProgressionEngine.valueKindOf(entry) == ProgressionEngine.ValueKind.VARIATION) {
                branch.entry
            } else null
        }
        assertTrue(
            "Push-Up is expected to branch into variation rows, so this test has " +
                "something to guard; found none among " +
                pushUp.branches.map { it.entry },
            variationTargets.isNotEmpty(),
        )

        // For each of them there must be NO numeric load - and since 2026-10-03 not
        // even a variation name: a rep ladder's level counts reps, so the popup
        // shows its reps and has no value to print at all.
        variationTargets.forEach { name ->
            val entry = ProgressionEngine.entryFor(name)!!
            assertTrue(
                "$name is a VARIATION row but still names a movement at a level",
                ProgressionEngine.isRepLadder(entry),
            )
            val value = ProgressionEngine.prescribedValueAtLevel(entry, 1)
            assertTrue(
                "$name is a VARIATION row but carries the value $value",
                value == null,
            )
            assertTrue(
                "$name is a rep ladder but prescribes no reps for the popup to show",
                !honestRepsFor(name).isNullOrEmpty(),
            )
        }
    }

    @Test
    fun everyBranchTargetCarriesATypedPrescription() {
        // A null prescription is the other way this goes wrong: the popup then
        // has nothing honest to print, and a "kg" fallback would be a guess.
        assertTrue(
            "expected a few hundred branch targets to sweep",
            targets.size > 300,
        )
        val untyped = targets.filter { (name, v) ->
            when {
                v == null -> honestRepsFor(name) == null
                v.kind == ProgressionEngine.ValueKind.LOAD -> v.value == null
                v.kind == ProgressionEngine.ValueKind.VARIATION -> v.variation.isNullOrBlank()
                else -> v.value == null
            }
        }
        assertTrue(
            "branch targets with no typed prescription: " +
                untyped.take(10).joinToString { it.first },
            untyped.isEmpty(),
        )
    }

    @Test
    fun onlyLoadRowsEverCarryAUnit() {
        // The invariant behind the popup, stated precisely. An earlier version
        // of this test asserted "a numeric value means a weight", which is
        // simply false: a DURATION row (a chin tuck hold) carries a number too -
        // it is a count of SECONDS. The thing that makes a number a weight is
        // the unit, and prescribedValueAtLevel sets `loadUnit` only on LOAD.
        // So the real rule is: kilograms exist if and only if kind == LOAD.
        val withUnit = targets.filter { it.second?.loadUnit != null }
        val notLoad = withUnit.filter { it.second?.kind != ProgressionEngine.ValueKind.LOAD }
        assertTrue(
            "a unit attached to a non-LOAD prescription: " +
                notLoad.take(10).joinToString { "${it.first} (${it.second?.kind})" },
            notLoad.isEmpty(),
        )

        val loadWithoutUnit = targets.filter {
            val v = it.second ?: return@filter false
            v.kind == ProgressionEngine.ValueKind.LOAD && v.loadUnit == null
        }
        assertTrue(
            "a LOAD prescription with no unit, so the popup would have to guess 'kg': " +
                loadWithoutUnit.take(10).joinToString { it.first },
            loadWithoutUnit.isEmpty(),
        )
    }
}
