package com.grafizum.repio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the level card's and the level picker's strings.
 *
 * Every key here is resolved through the dynamic `getString` bridge, which
 * **echoes an unknown key straight to the screen instead of throwing** (see
 * util/Resources.kt) — the compiler cannot catch a renamed or misspelled key, and
 * neither can lint. This test resolves each one in every shipped locale, the way
 * the UI does, and pins the argument shape of the one key that takes a number.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LevelPickerStringsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Keys used by the live card's level display and by the picker dialog. */
    private val keys = listOf(
        // The level display on the card.
        "level_prescription_label",
        "tap_to_see_every_level",
        // The stepper's arrows walk Roman stages now, not levels.
        "stage_down_desc",
        "stage_up_desc",
        "this_set_label",
        // The per-exercise lock on the card, and the picker's locked state.
        "exercise_lock_desc",
        "exercise_unlock_desc",
        "level_details_locked_hint",
        // The picker.
        "level_details_intro",
        "level_details_current",
        // The picker's picture of a level being earned by itself. Owner, 2026-10-09:
        // "the app should also explain that the levels increase automatically after
        // finishing that exercise (show that graphically the same as the other informations
        // for the level panel)". Four keys rather than a sentence, so all four are resolved
        // here in every locale: a panel that explains the ladder in one language and not in
        // another is worse than one that does not explain it at all.
        "level_details_auto_title",
        "level_details_auto_arrow",
        "level_details_auto_caption",
        "level_details_auto_top_caption",
        "level_details_beyond_plan",
        "level_details_beyond_plan_desc",
        // Retired with the Settings switch (owner, 2026-10-09: "remove 'stay on this level'
        // switch in the settings completely"): level_lock_title and level_lock_desc were only
        // ever that row's copy. The card's own pin still uses level_lock_short /
        // level_locked_short, which stay in this list via the picker's own keys.
        "done"
    )

    /**
     * The keys that take arguments, with the SAME shapes as their production
     * call sites. They are formatted rather than fetched bare: a resource that
     * expects an argument throws MissingFormatArgumentException when read
     * without one, which is itself the check.
     */
    private val keysWithArgs: List<Pair<String, Array<Any>>> = listOf(
        "level_word_n" to arrayOf(5),
        "adjust_levels_n" to arrayOf(5),
        "stage_progress_format" to arrayOf("III", "XIII"),
        "progression_stage_target" to arrayOf("8 / 9 / 9"),
        "level_details_title" to arrayOf("Barbell Bench Press"),
        "level_details_beyond_plan_note" to arrayOf(40)
    )

    private fun string(key: String, vararg args: Any): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        assertTrue("missing string resource: $key", resId != 0)
        return context.getString(resId, *args)
    }

    private fun assertKeysResolve(prefix: String) {
        keys.forEach { key ->
            val text = string(key)
            assertNotEquals("$prefix: $key resolved to its own key", key, text)
            assertTrue("$prefix: $key is blank", text.isNotBlank())
        }
        keysWithArgs.forEach { (key, args) ->
            val text = string(key, *args)
            assertFalse("$prefix: $key left a placeholder: \"$text\"", text.contains("%"))
            assertTrue("$prefix: $key is blank", text.isNotBlank())
        }
    }

    @Test
    fun everyKeyResolvesInEnglish() {
        assertKeysResolve("en")
    }

    @Test
    @Config(qualifiers = "de")
    fun everyKeyResolvesInGerman() {
        assertKeysResolve("de")
    }

    @Test
    @Config(qualifiers = "it")
    fun everyKeyResolvesInItalian() {
        assertKeysResolve("it")
    }

    @Test
    fun theBeyondPlanNoteFormatsItsLevelArgument() {
        // SAME argument shape as the production call site in LevelDetailsDialog,
        // which passes the number of selectable rungs as an Int. A %1$s slot
        // there would throw IllegalFormatConversionException at runtime.
        val text = string("level_details_beyond_plan_note", 30)
        assertFalse("unresolved placeholder: \"$text\"", text.contains("%"))
        assertTrue("the level number is missing: \"$text\"", text.contains("30"))
    }

    @Test
    fun theLevelTitleAndStageLineFormatTheirArguments() {
        val title = string("level_details_title", "Barbell Bench Press")
        assertEquals("Levels — Barbell Bench Press", title)

        val stage = string("stage_progress_format", "III", "XIII")
        assertEquals("Stage III of XIII", stage)
    }
}
