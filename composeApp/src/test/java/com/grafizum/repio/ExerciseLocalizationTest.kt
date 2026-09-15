package com.grafizum.repio

import com.grafizum.repio.data.ExerciseLocalization
import com.grafizum.repio.data.WorkoutTemplates
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the exercise localization tables: every built-in exercise must be translated in
 * every supported language, and the tables must not drift from the pool.
 *
 * This is the test that makes the translation tables safe to generate by hand. A typo in a
 * slug key does not crash the app — it just quietly leaves that one exercise in English — so
 * without these assertions the mistake would only be visible by reading German screenshots.
 */
class ExerciseLocalizationTest {

    private val pool = WorkoutTemplates.getAllExerciseNames()

    /** Exercises in the pool that legitimately keep their English name worldwide. */
    private val languages = ExerciseLocalization.translatedLanguages

    @Test
    fun everyPoolExerciseIsTranslatedInEveryLanguage() {
        assertTrue("the pool should not be empty", pool.isNotEmpty())
        assertTrue("expected at least one translated language", languages.isNotEmpty())

        for (language in languages) {
            val missing = pool.filter { ExerciseLocalization.textFor(it, language) == null }
            assertEquals(
                "[$language] every pool exercise needs a translation",
                emptyList<String>(),
                missing
            )
        }
    }

    @Test
    fun noTranslationBelongsToAnExerciseOutsideThePool() {
        val poolSlugs = pool.map { ExerciseLocalization.slugify(it) }.toSet()
        for (language in languages) {
            val orphans = ExerciseLocalization.translatedSlugs(language) - poolSlugs
            assertEquals(
                "[$language] these slugs match no pool exercise (typo in a key?)",
                emptySet<String>(),
                orphans
            )
        }
    }

    @Test
    fun everyTranslationHasANameAndADescription() {
        for (language in languages) {
            for (name in pool) {
                val text = ExerciseLocalization.textFor(name, language)
                assertNotNull("[$language] no entry for '$name'", text)
                assertTrue("[$language] blank name for '$name'", text!!.name.isNotBlank())
                assertTrue(
                    "[$language] blank description for '$name'",
                    text.description.isNotBlank()
                )
            }
        }
    }

    @Test
    fun translatedNamesAreUniqueWithinALanguage() {
        for (language in languages) {
            val names = pool.map { ExerciseLocalization.textFor(it, language)!!.name }
            val duplicates = names.groupingBy { it.lowercase() }.eachCount().filter { it.value > 1 }
            assertEquals(
                "[$language] two exercises must not share a display name: $duplicates",
                emptyMap<String, Int>(),
                duplicates
            )
        }
    }

    @Test
    fun translatedDescriptionsAreUniqueWithinALanguage() {
        // The English pool already guarantees one description per movement; the translations
        // must keep that property, otherwise two exercises read as the same movement again.
        for (language in languages) {
            val descriptions = pool.map { ExerciseLocalization.textFor(it, language)!!.description }
            assertEquals(
                "[$language] distinct exercises must not share a description",
                descriptions.size,
                descriptions.distinct().size
            )
        }
    }

    @Test
    fun translationsAreActuallyTranslated() {
        // A copy-paste of the English name would silently pass every check above.
        for (language in languages) {
            val identical = pool.filter { name ->
                ExerciseLocalization.textFor(name, language)!!.name == name
            }
            // Internationalisms ("Planks", "Dips", "Squats") are legitimate, but a whole
            // table of them means the translation never happened.
            assertTrue(
                "[$language] only ${identical.size}/${pool.size} names differ from English; " +
                    "the table looks untranslated",
                identical.size < pool.size / 4
            )
        }
    }

    @Test
    fun slugifyIsStableAndIgnoresPunctuation() {
        assertEquals("dumbbell_curls", ExerciseLocalization.slugify("Dumbbell Curls"))
        assertEquals("barbell_bench_presses", ExerciseLocalization.slugify("Barbell Bench Presses"))
        // Apostrophe disappears rather than becoming its own separator, matching the tooling
        // that generated tools/exercise-i18n.
        assertEquals("farmer_s_walks", ExerciseLocalization.slugify("Farmer's Walks"))
        assertEquals("dips_chest_focus", ExerciseLocalization.slugify("Dips (Chest Focus)"))
        assertEquals("l_sit_pull_ups", ExerciseLocalization.slugify("L-sit Pull-ups"))
        assertEquals("", ExerciseLocalization.slugify("   "))
        // A name it cannot represent yields no translation rather than a wrong one.
        assertNull(ExerciseLocalization.textFor("Kniebeugen ü", "de"))
    }

    @Test
    fun canonicalForResolvesEveryNameOfEveryExercise() {
        for (language in languages) {
            for (name in pool) {
                val localized = ExerciseLocalization.textFor(name, language)!!.name
                assertEquals(
                    "[$language] '$localized' must resolve back to '$name'",
                    name,
                    ExerciseLocalization.canonicalFor(localized)
                )
            }
        }
    }

    @Test
    fun canonicalForResolvesCanonicalNamesAndRejectsUnknownOnes() {
        assertEquals("Dumbbell Curls", ExerciseLocalization.canonicalFor("Dumbbell Curls"))
        assertEquals("Dumbbell Curls", ExerciseLocalization.canonicalFor("dumbbell curls"))
        assertNull(ExerciseLocalization.canonicalFor("My Custom Exercise"))
        assertNull(ExerciseLocalization.canonicalFor(""))
    }

    @Test
    fun noTranslatedNameShadowsADifferentExercise() {
        // A translated name that happens to slugify to another exercise's canonical slug would
        // make [canonicalFor] resolve it to the wrong exercise. Canonical names win by
        // construction in ExerciseLocalization; this proves no translation hits that case.
        val canonicalSlugs = pool.associateBy { ExerciseLocalization.slugify(it) }
        for (language in languages) {
            for (canonical in pool) {
                val localized = ExerciseLocalization.textFor(canonical, language)!!.name
                val collides = canonicalSlugs[ExerciseLocalization.slugify(localized)]
                if (collides != null) {
                    assertEquals(
                        "[$language] translated name '$localized' collides with '$collides'",
                        canonical,
                        collides
                    )
                }
            }
        }
    }

    @Test
    fun displayNameFallsBackToEnglishForUnsupportedLanguages() {
        // No table -> the canonical name, unchanged, so nothing can render blank.
        assertEquals(
            "Dumbbell Curls",
            ExerciseLocalization.displayName("Dumbbell Curls", ExerciseLocalization.ENGLISH)
        )
        assertNull(ExerciseLocalization.description("Dumbbell Curls", ExerciseLocalization.ENGLISH))
        // A custom exercise is not in the tables and keeps its own text.
        assertEquals(
            "My Custom Exercise",
            ExerciseLocalization.displayName("My Custom Exercise", "de")
        )
    }

    @Test
    fun allNamesForIncludesEveryLanguage() {
        val names = ExerciseLocalization.allNamesFor("Dumbbell Curls")
        assertEquals("canonical name first", "Dumbbell Curls", names.first())
        assertEquals(
            "canonical + one name per translated language",
            1 + languages.size,
            names.size
        )
        assertTrue(names.contains("Kurzhantel-Curls"))
        assertTrue(names.contains("Curl con manubri"))
    }

    @Test
    fun languageOfMapsLocalesToTables() {
        assertEquals("de", ExerciseLocalization.languageOf(Locale.GERMAN))
        assertEquals("de", ExerciseLocalization.languageOf(Locale.GERMANY))
        assertEquals("it", ExerciseLocalization.languageOf(Locale.ITALIAN))
        assertEquals("de", ExerciseLocalization.languageOf(Locale.forLanguageTag("de-AT")))
        assertEquals("en", ExerciseLocalization.languageOf(Locale.ENGLISH))
        assertEquals("en", ExerciseLocalization.languageOf(Locale.forLanguageTag("fr-FR")))
    }

    @Test
    fun spotChecksTheGermanAndItalianCopy() {
        assertEquals(
            "Kurzhantel-Bankdrücken",
            ExerciseLocalization.textFor("Dumbbell Bench Presses", "de")!!.name
        )
        assertEquals(
            "Panca piana con manubri",
            ExerciseLocalization.textFor("Dumbbell Bench Presses", "it")!!.name
        )
        // The description must be the localized method text, not the English one.
        val german = ExerciseLocalization.textFor("Barbell Squats", "de")!!.description
        assertTrue(
            "German description should read as German: $german",
            german.contains("Hocke") || german.contains("Stange")
        )
    }
}
