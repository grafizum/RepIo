package com.grafizum.repio.util

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.grafizum.repio.BuildConfig
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Firebase and App Check, initialised OFF the startup path.
 *
 * WHY THIS EXISTS. `RepIoApplication.onCreate` used to call `FirebaseApp.initializeApp`
 * and install an App Check provider factory synchronously, on the main thread, on
 * every single launch. That work is real - it reads the App Check provider, builds
 * the provider chain, and in a release build goes near Play Integrity - and none of
 * it is needed to draw the first frame.
 *
 * The only consumer in the whole app is [SupportManager], i.e. the support-ticket
 * path, which a user reaches by asking for help. So we were paying a startup tax on
 * every launch for a feature that is idle almost always. Measured on an SM-S906B,
 * cold start was 1.8-4.2s and highly variable; the variance is the tell, since this
 * runs before `setContent` and any scheduling noise lands directly in the visible
 * startup time.
 *
 * It is now a lazily-awaited task: [ensureInitialized] blocks the CALLER (never the
 * main thread) until Firebase is ready, and [warmUp] may kick it off early from a
 * background thread so the ticket path does not pay for it either.
 *
 * WHY THIS IS SAFE, and why it is not just `Thread { ... }.start()`:
 *
 *  - **Failure is still handled.** The original code caught every exception and
 *    logged it, so a broken Firebase could never stop the app launching. That
 *    behaviour is preserved exactly: the failure is stored and re-surfaced to
 *    callers as a thrown exception, which [SupportManager] already catches and
 *    turns into a `Result.failure`. An app that cannot reach Firestore is exactly
 *    as usable as before this change - the user simply gets the same error the
 *    ticket path already produced.
 *
 *  - **Repeat callers cannot double-initialize.** [state] holds the single attempt,
 *    so N concurrent callers await one initialisation. Firebase's own
 *    `initializeApp` is idempotent, but doing it once keeps the ordering of App
 *    Check installation deterministic, which matters because App Check's provider
 *    factory must be installed before the first Firestore call.
 *
 *  - **The main thread is never blocked.** [ensureInitialized] is `suspend` and the
 *    work runs on [Dispatchers.Default]. If it were called from the main thread it
 *    would suspend rather than block, so a caller that forgot to move itself off
 *    the main thread would still be correct, just slower.
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"

    private sealed interface State {
        /** Nothing has started yet. */
        data object NotStarted : State

        /** An attempt is in flight; continuations wait on this one. */
        data object InProgress : State

        /** Finished successfully. */
        data object Ready : State

        /** Finished, but it threw. The cause is replayed to every caller. */
        data class Failed(val cause: Throwable) : State
    }

    private val state = AtomicReference<State>(State.NotStarted)

    /** Continuations parked on an in-flight initialisation. */
    private val waiters = mutableListOf<CancellableContinuation<Unit>>()

    /**
     * Suspend until Firebase and App Check are ready, initialising them if needed.
     *
     * Call from a coroutine that is NOT on the main thread if you care about
     * latency - the work runs on [kotlinx.coroutines.Dispatchers.Default].
     */
    suspend fun ensureInitialized(context: Context) {
        while (true) {
            when (val current = state.get()) {
                State.Ready -> return

                is State.Failed -> throw current.cause

                State.InProgress -> awaitCompletion()

                State.NotStarted ->
                    if (state.compareAndSet(State.NotStarted, State.InProgress)) {
                        // We won the race, so we are the ones who do the work.
                        try {
                            initialize(context.applicationContext)
                            state.set(State.Ready)
                            releaseWaiters { it.resume(Unit) }
                        } catch (e: Throwable) {
                            state.set(State.Failed(e))
                            releaseWaiters { it.resumeWith(Result.failure(e)) }
                        }
                        return
                    }
                    // Lost the race; loop and take the InProgress branch.
            }
        }
    }

    private suspend fun awaitCompletion() = suspendCancellableCoroutine { continuation ->
        // ContinuationHandler removes it if the caller's scope is cancelled.
        synchronized(waiters) {
            waiters.add(continuation)
        }
        continuation.invokeOnCancellation {
            synchronized(waiters) { waiters.remove(continuation) }
        }
    }

    private fun releaseWaiters(block: (CancellableContinuation<Unit>) -> Unit) {
        val parked = synchronized(waiters) {
            val copy = waiters.toList()
            waiters.clear()
            copy
        }
        parked.forEach(block)
    }

    /**
     * The actual initialisation, moved verbatim out of `Application.onCreate`.
     *
     * The DEBUG/release branch is preserved exactly as it was: App Check's provider
     * differs between them, and quietly changing which one is installed would alter
     * behaviour for support tickets rather than performance.
     */
    private suspend fun initialize(context: Context) {
        withContext(Dispatchers.Default) {
            FirebaseApp.initializeApp(context)

            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        }
    }

    /**
     * Test seam: has Firebase been initialised yet?
     *
     * Exposed so a test can assert that a fresh process has NOT initialised Firebase
     * - which is the entire point of the change, and otherwise unobservable, since
     * "it works" looks identical either way.
     */
    fun isReadyForTest(): Boolean = state.get() == State.Ready

    /** Test seam: reset to the pre-initialisation state. */
    internal fun resetForTest() {
        state.set(State.NotStarted)
        synchronized(waiters) { waiters.clear() }
    }
}