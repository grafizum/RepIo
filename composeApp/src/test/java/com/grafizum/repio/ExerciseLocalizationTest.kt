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
 * Guards the exercise localization machinery: whatever a language table contains must be
 * correct — keys that match pool exercises, unique names and descriptions, no shadowing —
 * and the canonical-name resolution must keep working.
 *
 * The pool was rebuilt from the exercise-database CSV on branch
 * `experiment/csv-exercise-db`, so every canonical name changed and the German/Italian
 * tables were retired. They have since been regrown to cover the whole pool again, so
 * `everyTableCoversTheWholePool` now asserts that. If a language is ever retired again,
 * drop that one language from the coverage test rather than weakening the rest.
 */
class ExerciseLocalizationTest {

    private val pool = WorkoutTemplates.getAllExerciseNames()
    private val poolBySlug = pool.associateBy { ExerciseLocalization.slugify(it) }
    private val languages = ExerciseLocalization.translatedLanguages

    @Test
    fun thePoolIsNotEmpty() {
        assertTrue("the rebuilt pool should not be empty", pool.isNotEmpty())
    }

    @Test
    fun noTableEntryBelongsToAnExerciseOutsideThePool() {
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
    fun everyTableEntryHasANameAndADescription() {
        for (language in languages) {
            for (slug in ExerciseLocalization.translatedSlugs(language)) {
                val text = ExerciseLocalization.textForSlug(slug, language)
                assertNotNull("[$language] no entry for slug '$slug'", text)
                assertTrue("[$language] blank name for '$slug'", text!!.name.isNotBlank())
                assertTrue("[$language] blank description for '$slug'", text.description.isNotBlank())
            }
        }
    }

    @Test
    fun translatedNamesAreUniqueWithinALanguage() {
        for (language in languages) {
            val names = ExerciseLocalization.translatedSlugs(language)
                .map { ExerciseLocalization.textForSlug(it, language)!!.name }
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
        // Distinct exercises must not read as the same movement in any language.
        for (language in languages) {
            val descriptions = ExerciseLocalization.translatedSlugs(language)
                .map { ExerciseLocalization.textForSlug(it, language)!!.description }
            assertEquals(
                "[$language] distinct exercises must not share a description",
                descriptions.size,
                descriptions.distinct().size
            )
        }
    }

    @Test
    fun everyTableCoversTheWholePool() {
        for (language in languages) {
            val poolSlugs = poolBySlug.keys
            val missing = poolSlugs - ExerciseLocalization.translatedSlugs(language)
            assertEquals(
                "[$language] ${missing.size} pool exercises still fall back to English",
                emptySet<String>(),
                missing
            )
        }
    }

    @Test
    fun translationsAreActuallyTranslated() {
        // A copy-paste of the English name would silently pass every check above.
        // An empty table is legitimate on the clean-slate branch; a non-empty one
        // must actually be translated.
        for (language in languages) {
            val entries = ExerciseLocalization.translatedSlugs(language)
                .map { slug -> slug to ExerciseLocalization.textForSlug(slug, language)!! }
            if (entries.isEmpty()) continue
            val identical = entries.count { (slug, text) -> poolBySlug[slug] == text.name }
            assertTrue(
                "[$language] only $identical/${entries.size} names differ from English; " +
                    "the table looks untranslated",
                identical < entries.size / 4
            )
        }
    }

    @Test
    fun slugifyIsStableAndIgnoresPunctuation() {
        assertEquals("dumbbell_bicep_curl", ExerciseLocalization.slugify("Dumbbell Bicep Curl"))
        assertEquals("push_up", ExerciseLocalization.slugify("Push-Up"))
        // Apostrophe disappears rather than becoming its own separator, matching the tooling.
        assertEquals("farmer_s_walk", ExerciseLocalization.slugify("Farmer's Walk"))
        assertEquals("3_4_sit_up", ExerciseLocalization.slugify("3/4 Sit-Up"))
        assertEquals("", ExerciseLocalization.slugify("   "))
        // A name it cannot represent yields no translation rather than a wrong one.
        assertNull(ExerciseLocalization.textFor("Kniebeugen ü", "de"))
    }

    @Test
    fun canonicalForResolvesEveryTranslatedName() {
        for (language in languages) {
            for (slug in ExerciseLocalization.translatedSlugs(language)) {
                val localized = ExerciseLocalization.textForSlug(slug, language)!!.name
                assertEquals(
                    "[$language] '$localized' must resolve back to '${poolBySlug[slug]}'",
                    poolBySlug[slug],
                    ExerciseLocalization.canonicalFor(localized)
                )
            }
        }
    }

    @Test
    fun canonicalForResolvesCanonicalNamesAndRejectsUnknownOnes() {
        assertEquals("Dumbbell Bicep Curl", ExerciseLocalization.canonicalFor("Dumbbell Bicep Curl"))
        assertEquals("Dumbbell Bicep Curl", ExerciseLocalization.canonicalFor("bicep curl (dumbbell)"))
        assertNull(ExerciseLocalization.canonicalFor("My Custom Exercise"))
        assertNull(ExerciseLocalization.canonicalFor(""))
    }

    @Test
    fun noTranslatedNameShadowsADifferentExercise() {
        // A translated name that happens to slugify to another exercise's canonical slug
        // would make [canonicalFor] resolve it to the wrong exercise. Canonical names win
        // by construction in ExerciseLocalization; this proves no translation hits that case.
        for (language in languages) {
            for (slug in ExerciseLocalization.translatedSlugs(language)) {
                val localized = ExerciseLocalization.textForSlug(slug, language)!!.name
                val collides = poolBySlug[ExerciseLocalization.slugify(localized)]
                if (collides != null) {
                    assertEquals(
                        "[$language] translated name '$localized' collides with '$collides'",
                        poolBySlug[slug],
                        collides
                    )
                }
            }
        }
    }

    @Test
    fun displayNameFallsBackToEnglishForUnsupportedLanguages() {
        // No table entry -> the canonical name, unchanged, so nothing can render blank.
        assertEquals(
            "Dumbbell Bicep Curl",
            ExerciseLocalization.displayName("Dumbbell Bicep Curl", ExerciseLocalization.ENGLISH)
        )
        assertNull(ExerciseLocalization.description("Dumbbell Bicep Curl", ExerciseLocalization.ENGLISH))
        // A custom exercise is not in the tables and keeps its own text.
        assertEquals(
            "My Custom Exercise",
            ExerciseLocalization.displayName("My Custom Exercise", "de")
        )
    }

    @Test
    fun allNamesForStartsWithTheCanonicalName() {
        val names = ExerciseLocalization.allNamesFor("Dumbbell Bicep Curl")
        assertEquals("canonical name first", "Dumbbell Bicep Curl", names.first())
        assertTrue("canonical name always present", names.contains("Dumbbell Bicep Curl"))
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
}
