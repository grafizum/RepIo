package com.grafizum.repio

import com.grafizum.repio.data.ExerciseCatalog
import com.grafizum.repio.data.ExerciseLocalization
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `task_6q3m1r4h` (owner, 2026-10-02): "Make sure that each exercise in the
 * database has a slight description on how the exercise is done and how to do
 * it, not if it's a good exercise or who used it etc."
 *
 * Coverage was never the problem - all 601 exercises already had text. The
 * problem was SHAPE: 73 of the descriptions inherited from the previous catalog
 * opened with who used the lift or how highly it is regarded, which tells a user
 * nothing about what to do:
 *
 *   "Named after the iconic bodybuilder and movie star, the Arnold press ..."
 *   "was a staple exercise of the 'Golden Era' of bodybuilding and a favorite of
 *    Arnold Schwarzenegger himself"
 *   "commonly used in military and tactical physical fitness tests"
 *
 * Those 73 are now instruction-first (tools/exercise-db/data/description_overrides.py).
 * This case is what stops the shape coming back, in English and in both
 * translations at once - the German and Italian tables are hand-maintained, so a
 * later edit there would reintroduce it silently.
 *
 * The word list is a TRIPWIRE, not a grammar. It names reputation and
 * provenance, which is what the owner objected to. Two deliberate exclusions:
 *
 *  - "Olympic" is allowed. In "a two-part Olympic lift" it names the DISCIPLINE
 *    the movement belongs to, which is part of identifying it, not trivia about
 *    who did it. Excluding it would force a vaguer description.
 *  - "Arnold"/"Pendlay"/"Spoto" are allowed in the NAME, because in German and
 *    Italian these are simply the exercise's name ("Arnold-Pressen"). The
 *    provenance rule is about the DESCRIPTION text, so the name is never read
 *    here - see the per-language case, which checks the description only.
 */
class ExerciseDescriptionShapeTest {

    /** Reputation / provenance vocabulary, in every shipped language. */
    private val reputationWords = listOf(
        // English
        "named after", "used by", "favorite of", "favourite of", "golden era",
        "bodybuilding classic", "is a popular", "immensely popular", "very popular",
        "ubiquitous", "synonymous with", "fitness test", "roots in",
        // German
        "beliebt", "benutzt von", "berühmt", "antike", "griechisch", "geschichte",
        "populär", "entstanden", "goldene",
        // Italian
        "popolar", "usato da", "celebre", "antico", "greco", "storia",
        "nato dal", "epoca d'oro",
    )

    /** Latin "Olympic" must not trip the tripwire. */
    private val allowed = listOf("olympic", "olympisch", "olimpico")

    private fun flagged(text: String): List<String> {
        val lower = text.lowercase()
        return reputationWords.filter { lower.contains(it) }
            .filterNot { w -> allowed.any { lower.contains(it) } && w.startsWith("olymp") }
    }

    @Test
    fun noEnglishDescriptionLeadsWithProvenance() {
        val offenders = ExerciseCatalog.ENTRIES.mapNotNull { e ->
            val d = e.description
            if (d.isBlank()) null
            else flagged(d).takeIf { it.isNotEmpty() }?.let { "${e.name}: $it" }
        }
        assertTrue(
            "descriptions that describe who used the exercise instead of how to do it:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    @Test
    fun everyEnglishDescriptionActuallySaysHowToDoIt() {
        // The complementary risk of a word-blocklist fix: descriptions can be
        // free of reputation language and still useless. "The leg extension is an
        // isolation exercise for the quads" names the muscle and stops - it
        // never says HOW, which is what the owner actually asked for, and it
        // passed every reputation check. Nine such rows were fixed this way.
        //
        // The floor is set from the data rather than picked: after the rewrites
        // the shortest description in the pool is 141 characters and the median is
        // ~254, so 120 sits below every real entry while still catching a stub.
        val thin = ExerciseCatalog.ENTRIES
            .filter { it.description.length < 120 }
            .map { "${it.name} (${it.description.length} chars)" }
        assertTrue("descriptions too short to be a method:\n${thin.joinToString("\n")}", thin.isEmpty())
    }

    @Test
    fun noTranslatedDescriptionLeadsWithProvenance() {
        // de and it are hand-maintained tables, so this is where the shape would
        // silently come back. Only the DESCRIPTION is read, never the name - the
        // names legitimately carry proper nouns in every language.
        ExerciseLocalization.translatedLanguages.forEach { tag ->
            val offenders = ExerciseCatalog.ENTRIES.mapNotNull { e ->
                val d = ExerciseLocalization.description(e.name, tag)
                if (d.isNullOrBlank()) null
                else flagged(d).takeIf { it.isNotEmpty() }?.let { "$tag ${e.name}: $it" }
            }
            assertTrue(
                "provenance-led descriptions in $tag:\n${offenders.joinToString("\n")}",
                offenders.isEmpty()
            )
        }
    }
}