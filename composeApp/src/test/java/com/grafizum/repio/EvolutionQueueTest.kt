package com.grafizum.repio

import com.grafizum.repio.data.ProgressionEngine
import com.grafizum.repio.ui.screens.seedEvolutionQueue
import com.grafizum.repio.ui.viewmodel.EvolutionTreeItem
import com.grafizum.repio.ui.viewmodel.MasteryCardData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The evolution popup queue — ONE popup per exercise.
 *
 * Owner: "earlier i had 2 popups to progress the same exercise".
 *
 * The mechanism was not a duplicated list. `computeMasteredExercises` already
 * does `distinctBy { it.name }`, so the published list never contained the same
 * exercise twice — the queue was ASSIGNED from that list on every
 * `evolutionToken` bump, and the token bumps on every mastery refresh,
 * including the one the first popup's own Accept triggers. The exercise the user
 * had just decided about was still in the new list, so it came straight back.
 *
 * So the interesting cases are all about the SEQUENCE, not about a single call:
 * a merge that only deduplicates the incoming list passes every single-call test
 * and still shows the popup twice.
 */
class EvolutionQueueTest {

    /** A queue entry for a name. The card's other fields never reach the queue. */
    private fun item(name: String) = EvolutionTreeItem(
        card = MasteryCardData(
            exerciseName = name,
            muscleGroup = "Chest",
            equipment = "Barbell",
            level = 6,
            stageRoman = "V",
            load = null,
            unit = "kg",
            reason = ProgressionEngine.MasteryReason.CEILING_REACHED,
            ceiling = null,
            branches = emptyList(),
            suggestions = emptyList(),
        ),
        offers = emptyList(),
    )

    private fun names(items: List<EvolutionTreeItem>) = items.map { it.card.exerciseName }

    @Test
    fun aFirstPublishQueuesEverything() {
        val q = seedEvolutionQueue(
            pending = emptyList(),
            incoming = listOf(item("Bench Press"), item("Push-Up")),
            alreadyShown = emptySet()
        )
        assertEquals(listOf("Bench Press", "Push-Up"), names(q))
    }

    /**
     * THE BUG. Accepting a branch refreshes mastery, which re-publishes the very
     * exercise that was just accepted — because a card only leaves the list once
     * it has been marked reviewed and the refresh after THAT has run. A queue
     * built purely from the new list therefore shows the same exercise twice.
     */
    @Test
    fun aRepublishDoesNotQueueAnExerciseThatIsAlreadyPending() {
        val pending = listOf(item("Bench Press"))
        val republished = listOf(item("Bench Press"), item("Pull-Up"))

        val q = seedEvolutionQueue(pending = pending, incoming = republished, alreadyShown = emptySet())

        assertEquals(
            "the pending decision is kept and the re-published copy is dropped",
            listOf("Bench Press", "Pull-Up"),
            names(q)
        )
    }

    /**
     * The other half, and the one a "just filter the duplicates" fix misses: the
     * exercise is no longer pending (the user answered it) but the refresh that
     * drops it from the published list has not happened yet, so it is still in
     * `incoming`. Without `alreadyShown` it returns as popup #2 on the very next
     * bump.
     */
    @Test
    fun anExerciseAlreadyShownIsNotQueuedAgain() {
        val q = seedEvolutionQueue(
            pending = emptyList(),
            incoming = listOf(item("Bench Press"), item("Pull-Up")),
            alreadyShown = setOf("Bench Press")
        )
        assertEquals(listOf("Pull-Up"), names(q))
    }

    /** The whole pass, in the order the owner met it. */
    @Test
    fun theWholePassNeverShowsOneExerciseTwice() {
        var pending = seedEvolutionQueue(emptyList(), listOf(item("Bench Press"), item("Pull-Up")), emptySet())
        assertEquals(listOf("Bench Press", "Pull-Up"), names(pending))

        // Accept "Bench Press": the popup retires, and mastery is refreshed -
        // which re-publishes Bench Press, because the card is still unreviewed.
        var shown = setOf("Bench Press")
        pending = seedEvolutionQueue(
            pending = pending.drop(1),
            incoming = listOf(item("Bench Press"), item("Pull-Up")),
            alreadyShown = shown
        )
        assertEquals("one popup per exercise, ever", listOf("Pull-Up"), names(pending))

        // Accept "Pull-Up" and another refresh lands.
        shown = shown + "Pull-Up"
        pending = seedEvolutionQueue(
            pending = pending.drop(1),
            incoming = listOf(item("Bench Press"), item("Pull-Up")),
            alreadyShown = shown
        )
        assertEquals(emptyList<String>(), names(pending))
    }

    /**
     * Pending keeps its ORDER. A merge that sorted or de-duplicated by rebuilding
     * the list would silently reorder decisions the user has not reached yet.
     */
    @Test
    fun pendingKeepsItsOrder() {
        val pending = listOf(item("C"), item("A"))
        val q = seedEvolutionQueue(pending, listOf(item("A"), item("B"), item("C")), setOf("C", "A"))
        assertEquals(listOf("C", "A", "B"), names(q))
    }

    /**
     * An exercise already pending is also in `alreadyShown` by the time the user
     * reaches it, so both rules must agree rather than one undoing the other.
     */
    @Test
    fun aNameInBothPendingAndShownStaysPending() {
        val q = seedEvolutionQueue(
            pending = listOf(item("Bench Press")),
            incoming = listOf(item("Bench Press")),
            alreadyShown = setOf("Bench Press")
        )
        assertEquals(
            "alreadyShown gates what a PUBLISH may add; it must not evict a pending popup",
            listOf("Bench Press"),
            names(q)
        )
    }
}
