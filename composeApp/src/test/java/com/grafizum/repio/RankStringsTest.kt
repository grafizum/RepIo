package com.grafizum.repio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.grafizum.repio.data.OverallLevelRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the rank constants that live in resource files rather than in Kotlin.
 *
 * The rank table resolves each tier's description through the dynamic
 * `getString` bridge, which **echoes an unknown key straight to the screen
 * instead of throwing** (see util/Resources.kt). So a renamed or misspelled key
 * would ship a row reading "rank_s_desc" to the user and nothing would fail. This
 * test is what makes that impossible: it resolves every key [OverallLevelRank]
 * names, in each shipped locale, the way the UI will.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RankStringsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(key: String): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        assertTrue("missing string resource: $key", resId != 0)
        return context.getString(resId)
    }

    private fun assertRankKeysResolve(prefix: String) {
        OverallLevelRank.ORDERED.forEach { rank ->
            val text = string(rank.descriptionKey)
            assertNotEquals(
                "$prefix: ${rank.descriptionKey} resolved to its own key",
                rank.descriptionKey,
                text
            )
            assertTrue(
                "$prefix: ${rank.descriptionKey} is blank",
                text.isNotBlank()
            )
        }
        val title = string("rank_system_guide_title")
        val desc = string("rank_system_guide_desc")
        assertNotEquals("$prefix: guide title is the raw key", "rank_system_guide_title", title)
        assertNotEquals("$prefix: guide description is the raw key", "rank_system_guide_desc", desc)
    }

    @Test
    fun everyRankDescriptionResolvesInEnglish() {
        assertRankKeysResolve("en")
    }

    @Test
    @Config(qualifiers = "de")
    fun everyRankDescriptionResolvesInGerman() {
        assertRankKeysResolve("de")
    }

    @Test
    @Config(qualifiers = "it")
    fun everyRankDescriptionResolvesInItalian() {
        assertRankKeysResolve("it")
    }

    @Test
    fun theTooltipFormatsItsOwnArguments() {
        // SAME argument shapes as the production call sites in OverallLevelTooltip:
        // the rank letter ALONE, and an Int then the next letter.
        //
        // The level line takes only the letter. It used to take the 0-100 figure
        // too ("Overall Level: 6.4 (Rank S)"), and the owner ruled on 2026-10-02
        // that the figure and the weighted median behind it stay backend-only -
        // so the argument was dropped and this call still passed the old pair.
        // An unused extra argument is silently ignored by getString, which is why
        // it rendered "Overall Level: Rank 6.4" instead of failing loudly.
        val level = context.getString(R.string.rank_tooltip_level, "S")
        assertEquals("Overall Level: Rank S", level)

        val progress = context.getString(R.string.rank_tooltip_progress, 40, "SS")
        assertEquals("40% to Rank SS", progress)

        assertNotEquals("rank_tooltip_top", context.getString(R.string.rank_tooltip_top))
    }

    @Test
    fun theExerciseCountPluralFormatsForBothQuantities() {
        val resources = context.resources
        val one = resources.getQuantityString(R.plurals.overall_level_exercises_pl, 1, 1)
        val many = resources.getQuantityString(R.plurals.overall_level_exercises_pl, 23, 23)
        assertEquals("Based on 1 strength exercise", one)
        assertEquals("Based on 23 strength exercises", many)
    }

    @Test
    fun theRangeLabelsAreBuiltFromTheirOwnNumbers() {
        // rank_range takes two already-formatted numbers, rank_range_open one, so
        // the table row is assembled from the thresholds rather than typed out.
        assertEquals("6.0 – 6.9", context.getString(R.string.rank_range, "6.0", "6.9"))
        assertEquals("8.0+", context.getString(R.string.rank_range_open, "8.0"))
        assertEquals("Rank S", context.getString(R.string.rank_label_format, "S"))
    }
}
