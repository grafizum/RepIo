package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseEquipmentTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The name-based equipment sets must stay truthful about two things, and NEITHER
 * fails loudly when it breaks.
 *
 * THE OWNER'S REPORT was "barbell and squat rack equipment are both selecting
 * and deselecting at the same time, it has some weird link, I want you to check
 * that every equipment has a personalized equipment value that is filtered
 * correctly inside the DB". The link was real - two tiles sharing one family -
 * but the DB audit underneath it found the quieter problem this test exists for.
 *
 * 1. EVERY ENTRY MUST NAME AN EXERCISE THAT STILL EXISTS.
 *    The rename pass moved the apparatus into the name ("Smith Machine Bench
 *    Press" became "Bench Press (Smith Machine)", across two passes totalling 220
 *    entries), and these sets were not in the list of sources it rewrote. An entry
 *    that no longer matches anything is not a duplicate and not a warning - it is
 *    a silent miss, because `requirementsFor` asks "which set is this name in?"
 *    and a name in no set simply falls through to the next branch. That is how a
 *    Smith-machine kit was denied Smith exercises and how `barbell_only` came to
 *    gate 4 exercises instead of 18.
 *
 * 2. EVERY ENTRY MUST BE LOWER CASE.
 *    `requirementsFor` tests membership with `n in SOME_SET` where `n` is
 *    `entry.name.lowercase()`, and `in` on a Set<String> is CASE-SENSITIVE. One
 *    capital letter makes the lookup miss, with the same silent outcome as (1).
 *    Rewriting these sets to the pool's display casing - which looks like a
 *    tidy-up - does exactly that: "Machine Bicep Curl" against a lowercased `n`
 *    never matches, the row falls through to the muscle fallback, and a Machine
 *    kit comes out with no biceps exercise at all.
 */
class EquipmentSetFreshnessTest {

    private val pool: Set<String> =
        ExerciseCatalog.ENTRIES.map { it.name.lowercase() }.toSet()

    @Test
    fun everyNameInEveryEquipmentSetStillExistsInThePool() {
        val stale = mutableListOf<String>()
        ExerciseEquipmentTags.nameLookupSets().forEach { (setName, names) ->
            names.forEach { name ->
                if (name.lowercase() !in pool) stale += "$setName -> \"$name\""
            }
        }
        assertTrue(
            "these equipment-set entries name no exercise in the pool, so each one " +
                "is a silent miss and the exercise it was written for falls through " +
                "to the next branch:\n" + stale.joinToString("\n"),
            stale.isEmpty(),
        )
    }

    @Test
    fun everyEquipmentSetEntryIsLowerCase() {
        val shouting = mutableListOf<String>()
        ExerciseEquipmentTags.nameLookupSets().forEach { (setName, names) ->
            names.forEach { name ->
                if (name != name.lowercase()) shouting += "$setName -> \"$name\""
            }
        }
        assertTrue(
            "membership is tested as `lowercased name in SET`, which is " +
                "case-sensitive, so a capitalised entry silently never matches:\n" +
                shouting.joinToString("\n"),
            shouting.isEmpty(),
        )
    }

    /**
     * The lookup sets have to actually be reached. A Machine row must be gated by
     * a machine tag, never by `cable_machine`: the catalogue files "Machine Bicep
     * Curl" under equipment "Machine", and a Machine kit grants
     * `machine_other` and the eight stations but NOT the cable, so a cable gate
     * leaves the kit owning a curl and being offered none.
     */
    @Test
    fun aMachineCurlIsGatedByTheMachineAndNotByTheCable() {
        listOf("Machine Bicep Curl", "Machine Preacher Curl").forEach { name ->
            val req = ExerciseEquipmentTags.requirementsFor(name)
            assertTrue(
                "$name is a Machine row and must require a machine, was $req",
                req.any { ExerciseEquipmentTags.MACHINE_OTHER in it },
            )
            assertTrue(
                "$name must not require a cable machine a Machine-only kit does " +
                    "not grant, was $req",
                req.none { ExerciseEquipmentTags.CABLE_MACHINE in it },
            )
        }
    }

    /**
     * Every catalogue row must resolve to a gate that agrees with its own
     * equipment column. A row filed as Machine that resolves to nothing at all is
     * the fallback for "name in no set", which is where stale entries end up.
     */
    @Test
    fun everyPoolRowResolvesToAGate() {
        val silent = ExerciseCatalog.ENTRIES
            .filter { ExerciseEquipmentTags.requirementsFor(it.name).isEmpty() }
            .map { it.name }
        // "Other" rows and bodyweight rows legitimately require nothing, so only
        // the ones filed as real equipment are asserted here.
        assertEquals(
            "rows that require nothing despite being filed as equipment are " +
                "falling through every lookup set",
            emptyList<String>(),
            silent.filter { name ->
                val row = ExerciseCatalog.ENTRIES.first { it.name == name }
                row.equipment in setOf("Barbell", "Dumbbell", "Kettlebell", "Cable", "Machine", "Bands")
            },
        )
    }
}