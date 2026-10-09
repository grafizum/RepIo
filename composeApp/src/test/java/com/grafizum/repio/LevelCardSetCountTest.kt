package com.grafizum.repio

import com.grafizum.repio.ui.screens.LevelCardBody
import com.grafizum.repio.ui.screens.levelCardShowsPerSetBanner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE OWNER'S ASK: "I want each exercise no many how many sets, to have the same
 * level style card as the 3 set exercises. Fix it now."
 *
 * The card's per-set action banner (the tinted "SET 2 OF 4 - do 10 reps" strip with
 * one pill per set) used to be gated on `repTargets.size in 1..3`, so every
 * exercise with FOUR or FIVE sets - which is exactly what the engine's hypertrophy
 * (4x8-12) and strength (5x4-6) templates prescribe - silently fell through to the
 * plain summary line and rendered in the custom-mode style beside 3-set exercises.
 *
 * The gate is now the pure [levelCardShowsPerSetBanner], because the rule lived
 * inline in the composable where no test could reach it - which is how a set-count
 * cap nobody chose survived in the first place.
 */
class LevelCardSetCountTest {

    /**
     * The regression itself. 4 and 5 are the template set counts, so this is the
     * case the owner was looking at; the sweep is what keeps a future cap from
     * reappearing at a different number.
     */
    @Test
    fun everySetCountWearsTheSameBannerAsTheThreeSetExercises() {
        for (sets in 1..12) {
            assertTrue(
                "a rep-based exercise with $sets set(s) must wear the banner, not the plain summary",
                levelCardShowsPerSetBanner(LevelCardBody.BANNER, sets),
            )
        }
        // Named explicitly because these two ARE the report.
        assertTrue(levelCardShowsPerSetBanner(LevelCardBody.BANNER, 4))
        assertTrue(levelCardShowsPerSetBanner(LevelCardBody.BANNER, 5))
    }

    @Test
    fun aRepBannerWithNoTargetsAtAllStillFallsBack() {
        // There is no number to headline, so the banner cannot be drawn - this is
        // the only legitimate fallback for an exercise that IS in the system.
        assertFalse(levelCardShowsPerSetBanner(LevelCardBody.BANNER, 0))
    }

    @Test
    fun anExerciseOutsideTheLevelSystemNeverWearsTheBanner() {
        for (sets in 0..12) {
            assertFalse(
                "SUMMARY is the custom-mode presentation and must stay summary (sets=$sets)",
                levelCardShowsPerSetBanner(LevelCardBody.SUMMARY, sets),
            )
        }
    }

    @Test
    fun aDurationBannerIsNeverGatedOnRepTargets() {
        // A timed exercise's headline is the seconds; requiring rep targets would
        // push every plank back to the custom card, the earlier regression.
        for (sets in listOf(0, 1, 3, 4, 5, 12)) {
            assertTrue(
                "a duration banner must not depend on rep targets (sets=$sets)",
                levelCardShowsPerSetBanner(LevelCardBody.BANNER_DURATION, sets),
            )
        }
    }
}
