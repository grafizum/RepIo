package com.grafizum.repio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression test: every plurals resource must format without throwing for all
 * quantities the app can pass, using the SAME argument shapes as the production
 * call sites (see util/Resources.kt getQuantityString helpers).
 *
 * Guards against IllegalFormatConversionException crashes at runtime caused by
 * placeholder type/index mismatches between plurals XML and code (e.g. a %d slot
 * receiving a String). These resources are resolved dynamically at runtime, so
 * the compiler cannot catch such mismatches.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PluralsFormatTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** name -> argument provider matching the production call-site arg order. */
    private val pluralShapes: Map<Int, (Int) -> Array<Any>> = mapOf(
        R.plurals.exercises_scheduled to { count -> arrayOf(count) },
        R.plurals.reps_count to { count -> arrayOf(count) },
        R.plurals.confirm_delete_multiple_msg to { count -> arrayOf(count) },
        R.plurals.workout_days_format_pl to { count -> arrayOf(count) },
        R.plurals.sets_x_reps_pl to { count -> arrayOf(count, "8-12") },
        R.plurals.sets_x_time_pl to { count -> arrayOf(count, "45s") },
        R.plurals.kg_x_reps_pl to { count -> arrayOf("12.5", count) },
        R.plurals.workout_completed_text to { count -> arrayOf(count, "Monday") },
        // OverallLevelTooltip passes the exercise count once (see RankBadge.kt).
        R.plurals.overall_level_exercises_pl to { count -> arrayOf(count) }
    )

    @Test
    fun `all plurals format without throwing for every quantity`() {
        val resources = context.resources
        val counts = listOf(0, 1, 2, 3, 5, 11, 21, 100)

        for ((resId, argProvider) in pluralShapes) {
            for (count in counts) {
                val formatted = resources.getQuantityString(resId, count, *argProvider(count))
                // No unresolved placeholders may remain after formatting.
                assertFalse(
                    "Unresolved placeholder in quantity=$count for resource #0x${resId.toString(16)}: \"$formatted\"",
                    formatted.contains("%")
                )
                assertTrue("Empty formatted text for resource #0x${resId.toString(16)}", formatted.isNotBlank())
            }
        }
    }
}
