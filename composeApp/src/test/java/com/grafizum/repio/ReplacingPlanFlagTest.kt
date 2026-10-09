package com.grafizum.repio

import com.grafizum.repio.ui.viewmodel.isReplacingAnExistingPlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE "EVERY LAUNCH" TRAP, pinned.
 *
 * The owner reported on 2026-10-08: "Closing and reopening the app asks for a new
 * workout (welcome page shows and level system explanation) which should only
 * show the first time the user opens the app not every time fix it."
 *
 * Reproduced on the AVD (Pixel 7a, font scale 1.8): Routine ▸ Edit ▸ New writes
 * `is_first_time = true`, and the re-entry intent used to live only in memory. The
 * app was then closed mid-flow, so the next launch saw `is_first_time = true` with
 * no re-entry flag - the welcome screen came back on EVERY launch, and because its
 * cancel button and Back handler are gated on the same flag, there was no way out
 * of it short of building a new plan. `is_first_time` stayed true across three
 * force-stop/relaunch cycles, so it never resolved itself.
 *
 * The rule is that a welcome screen belongs to the replace-flow whenever the user
 * demonstrably has a plan - either the intent was persisted, or a routine was saved
 * at some point. The second half is what repairs an install that is ALREADY stuck,
 * because such an install has `setup_type` on disk and nothing else.
 */
class ReplacingPlanFlagTest {

    @Test
    fun aSavedPlanMeansTheWelcomeIsAReplacementEvenWithNoPersistedIntent() {
        // The already-trapped install: `setup_manual_reentry` was never written
        // (the fix did not exist yet), but the routine is on disk.
        assertTrue(
            "an install with a saved routine must never be stuck in the welcome screen",
            isReplacingAnExistingPlan(manualReentryPersisted = false, savedSetupType = "ASSISTED")
        )
    }

    @Test
    fun aPersistedIntentIsEnoughOnItsOwn() {
        // "New" was tapped and the app was killed before a plan existed yet.
        assertTrue(
            isReplacingAnExistingPlan(manualReentryPersisted = true, savedSetupType = null)
        )
    }

    @Test
    fun aGenuineFirstRunIsNotAReplacementSoTheWelcomeHasNoWayOut() {
        // Nothing persisted: no intent, no routine. The welcome screen SHOULD be
        // unavoidable here - there is no plan to go back to.
        assertFalse(
            isReplacingAnExistingPlan(manualReentryPersisted = false, savedSetupType = null)
        )
        assertFalse(
            "a blank stored value is not a saved plan",
            isReplacingAnExistingPlan(manualReentryPersisted = false, savedSetupType = "")
        )
    }
}
