package com.grafizum.repio

import com.grafizum.repio.util.FirebaseInitializer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app must not initialize Firebase on its startup path.
 *
 * The owner's report was "the phone might be lagging slightly". Measured on an
 * SM-S906B, the app was NOT leaking and was NOT janky at steady state - idle CPU
 * 0.0%, 213 frames with 8 janky (3.76%), 50th percentile 6ms. The cost was
 * concentrated in one place: cold start at 1.8-4.2s, and highly variable.
 *
 * That variance is the tell. `Application.onCreate` runs BEFORE `setContent`, on
 * the main thread, so any work it does lands directly in the time the user waits
 * watching a splash screen - and any scheduling noise on the device lands there
 * too, which is exactly what a variable 1.8-4.2s looks like.
 *
 * The work found there was `FirebaseApp.initializeApp` plus installing an App Check
 * provider factory. The ONLY consumer of Firebase in the entire app is
 * `SupportManager` - the support-ticket path, which a user reaches by asking for
 * help. So every launch paid for a subsystem that is idle essentially always.
 *
 * This asserts the fix at the SOURCE, because that is the only level at which it
 * is falsifiable. Asserting it behaviourally would be meaningless: a unit test
 * cannot launch the app, and even on a device "Firebase works" looks identical
 * whether it was initialized eagerly or lazily. The regression that matters is
 * someone re-adding the eager call, and only the source can catch that.
 */
class FirebaseStartupCostTest {

    private val applicationSource: String =
        File("src/main/java/com/grafizum/repio/RepIoApplication.kt").readText()

    /**
     * `Application.onCreate` must not touch Firebase at all.
     *
     * Checked by absence of the call rather than presence of the lazy version,
     * because the eager form is what costs the time. `ensureInitialized` is the
     * lazy entry point and is asserted separately - if both greps were
     * "contains", adding the lazy call back would pass this test.
     */
    @Test
    fun applicationOnCreateDoesNotInitializeFirebase() {
        val code = codeOnly(applicationSource)
        listOf(
            "FirebaseApp.initializeApp",
            "FirebaseAppCheck.getInstance",
            "installAppCheckProviderFactory",
        ).forEach { eagerCall ->
            assertFalse(
                "RepIoApplication.onCreate calls '$eagerCall' again. That is main-thread " +
                    "work before the first frame, for a subsystem only the support-ticket " +
                    "path uses. It belongs in FirebaseInitializer, awaited by " +
                    "SupportManager.sendTicket.",
                code.contains(eagerCall),
            )
        }
    }

    /**
     * The lazy initializer must exist and be the thing that does the work.
     *
     * Without this, the test above would also pass if someone deleted the Firebase
     * setup outright - support tickets would break and this file would be silent.
     *
     * NOTE ON WHY COMMENTS ARE STRIPPED. An earlier version of this test read the
     * file as plain text and was found to be satisfiable by PROSE: renaming
     * `installAppCheckProviderFactory` to something else left the name present in
     * this file's own documentation of it, so the assertion passed while the code
     * was broken. Comments are the one part of a source file that changes without
     * changing behaviour, so they are removed before any of these greps. Verified:
     * with stripping, renaming the call fails the suite; without it, it passed.
     */
    @Test
    fun theLazyInitializerIsWhatPerformsTheFirebaseSetup() {
        val initializer = File("src/main/java/com/grafizum/repio/util/FirebaseInitializer.kt")
        assertTrue("FirebaseInitializer.kt is missing", initializer.isFile)
        val source = codeOnly(initializer.readText())
        assertTrue(
            "FirebaseInitializer must perform the initializeApp call it replaced",
            source.contains("FirebaseApp.initializeApp"),
        )
        assertTrue(
            "FirebaseInitializer must install the App Check provider factory, or " +
                "support tickets fail App Check verification",
            source.contains("installAppCheckProviderFactory"),
        )
        // The DEBUG/release split decides WHICH provider, and changing it silently
        // would alter ticket behaviour rather than performance.
        assertTrue(
            "the debug provider must still be chosen in debug builds",
            source.contains("DebugAppCheckProviderFactory"),
        )
        assertTrue(
            "the Play Integrity provider must still be chosen in release builds",
            source.contains("PlayIntegrityAppCheckProviderFactory"),
        )
        // The branch must still exist at all - a build that hardcoded one provider
        // would satisfy the two greps above while quietly changing release
        // behaviour, which is exactly the kind of edit that looks like cleanup.
        assertTrue(
            "the debug/release branch selecting the App Check provider is gone; one " +
                "of the two providers must be conditional on BuildConfig.DEBUG",
            source.contains("BuildConfig.DEBUG"),
        )
    }

    /**
     * The one consumer of Firebase must actually await the initializer.
     *
     * This is the other half of "not at startup": deferring initialization only
     * works if the path that needs it waits for it. If this call were dropped,
     * `FirebaseFirestore.getInstance()` would throw on the first ticket - and
     * nothing else in the suite would notice, because no test exercises Firestore.
     */
    @Test
    fun theSupportTicketPathAwaitsFirebaseInitialization() {
        val support = codeOnly(File("src/main/java/com/grafizum/repio/util/SupportManager.kt").readText())
        assertTrue(
            "SupportManager must await FirebaseInitializer.ensureInitialized before " +
                "touching Firestore; without it the first support ticket throws",
            support.contains("FirebaseInitializer.ensureInitialized"),
        )
        // runBlocking inside a suspend function would block a thread and defeat the
        // point; it is called out because it is the tempting way to sequence these.
        assertFalse(
            "SupportManager must not use runBlocking - it is already a suspend " +
                "function, so runBlocking just blocks a thread",
            support.contains("runBlocking"),
        )
    }

    /**
     * A fresh process must report Firebase as not-yet-initialized.
     *
     * Cheapest possible check that the state machine starts un-initialized rather
     * than being primed by class loading or a static block - which is the subtle
     * way an "eager" init creeps back in.
     */
    /**
     * Strip comments and string literals so a grep cannot be satisfied by prose.
     *
     * Kotlin block comments nest, so this tracks depth rather than assuming a
     * non-greedy `.*?` works. Line comments are dropped to end-of-line. String
     * literals are blanked but their delimiters kept, so `runBlocking` is still
     * visible when it appears as a real call and invisible when it appears only
     * inside an error message.
     */
    private fun codeOnly(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        var blockDepth = 0
        while (i < source.length) {
            val rest = source.substring(i)
            when {
                blockDepth > 0 -> {
                    if (rest.startsWith("/*")) { blockDepth++; i += 2 }
                    else if (rest.startsWith("*/")) { blockDepth--; i += 2 }
                    else i++
                }
                rest.startsWith("/*") -> { blockDepth++; i += 2 }
                rest.startsWith("//") -> {
                    val end = source.indexOf('\n', i)
                    i = if (end == -1) source.length else end
                }
                source[i] == '"' -> {
                    out.append('"')
                    i++
                    while (i < source.length && source[i] != '"') {
                        if (source[i] == '\\') i++
                        if (i < source.length) i++
                    }
                    if (i < source.length) { out.append('"'); i++ }
                }
                else -> { out.append(source[i]); i++ }
            }
        }
        return out.toString()
    }

    @Test
    fun firebaseIsNotInitializedInAFreshProcess() {
        assertFalse(
            "Firebase reported ready without anything awaiting initialization - " +
                "something is still initializing it eagerly",
            FirebaseInitializer.isReadyForTest(),
        )
    }
}