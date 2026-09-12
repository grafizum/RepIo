package com.grafizum.repio

import com.grafizum.repio.data.MotivationPhrases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Canary for the catchphrase nudge.
 *
 * The quoted lines are held back until David Goggins' team grants permission, and
 * they were compiled into the APK even with the nudge switched off. These tests
 * therefore assert the catalogue is EMPTY, so pasting the quotes back in without a
 * permission decision fails the build instead of quietly shipping them.
 */
class MotivationPhrasesTest {

    @Test
    fun noQuotedCatchphrasesShipWhilePermissionIsPending() {
        assertTrue(
            "MotivationPhrases must stay empty until permission to use the quoted " +
                "catchphrases is granted — see MotivationNudger.NUDGES_ENABLED",
            MotivationPhrases.all.isEmpty()
        )
    }

    @Test
    fun pickingAPhraseFromTheEmptyCatalogueIsSafe() {
        // The nudge is switched off, but the lookup must never throw even if
        // something else calls it while the catalogue is empty.
        val phrase = MotivationPhrases.forDay(20_000L)
        assertTrue(phrase.line.isEmpty())
        assertTrue(phrase.author.isEmpty())
    }

    @Test
    fun extremesDoNotOverflowTheIndex() {
        // Long absences and epoch-day sized numbers must not overflow once the
        // catalogue is repopulated.
        MotivationPhrases.forSkipCount(999, Long.MAX_VALUE)
        MotivationPhrases.forDay(Long.MIN_VALUE)
        assertEquals(MotivationPhrases.forDay(7L), MotivationPhrases.forDay(7L))
    }

    @Test
    fun lineLengthGuardIsUsableOnceLinesReturn() {
        // Enforced again once the approved lines are pasted back.
        assertTrue(MotivationPhrases.MAX_LINE_LENGTH in 1..200)
        assertFalse(MotivationPhrases.MAX_LINE_LENGTH <= 0)
    }
}
