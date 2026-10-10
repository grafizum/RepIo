package com.grafizum.repio.util

import android.os.Build
import com.grafizum.repio.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface SupportManager {
    suspend fun sendTicket(subject: String, message: String): Result<Unit>
    fun getDeviceModel(): String
    fun getAppVersion(): String
}

class AndroidSupportManager : SupportManager {
    /**
     * Sends a ticket, initializing Firebase first if it is not up yet.
     *
     * Firebase is no longer initialized in `Application.onCreate` - that was a
     * main-thread startup cost paid on every launch for a path that is idle almost
     * always. So the FIRST ticket awaits initialization here and later ones find it
     * ready. Both halves are plain `suspend` calls in an already-suspending
     * function: nothing blocks a thread, and nothing runs on the main thread.
     *
     * The `ensureInitialized` failure is folded into the same `Result.failure`
     * channel as a Firestore failure, which is what the pre-existing catch below
     * already did - a ticket that cannot be sent is a failed ticket, never a crash.
     */
    override suspend fun sendTicket(subject: String, message: String): Result<Unit> {
        try {
            FirebaseInitializer.ensureInitialized(requireApplicationContext())
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val db = FirebaseFirestore.getInstance()
            val ticket = hashMapOf(
                "subject" to subject,
                "message" to message,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "status" to "open",
                "device_model" to getDeviceModel(),
                "app_version" to getAppVersion()
            )
            db.collection("support_tickets").add(ticket)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        val descriptiveError = when {
                            e.message?.contains("PERMISSION_DENIED") == true ->
                                Exception("Permission Denied: Check Firestore rules or authentication.", e)
                            e.message?.contains("AppCheck") == true ->
                                Exception("App Check Failed: Secure connection could not be verified.", e)
                            else -> e
                        }
                        if (continuation.isActive) continuation.resume(Result.failure(descriptiveError))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
        }
    }

    override fun getDeviceModel(): String = Build.MODEL

    /**
     * The build's OWN version, from `BuildConfig` - never a literal.
     *
     * This returned a hardcoded `"1.0.0"`, so every support ticket a user sent
     * reported a version of the app that has never existed: the app's first
     * release was 1.4.3, and the field had been stale since the feature was added.
     * `version.properties` at the repo root is the single source of truth for the
     * version (AGENTS.md: *"Never hardcode a version anywhere else."*), and
     * `composeApp/build.gradle.kts` turns it into `BuildConfig.VERSION_NAME` /
     * `VERSION_CODE`, so reading those cannot drift from what the user is running.
     *
     * The CODE travels with the name because two builds can share a version name:
     * the code is what identifies the exact artifact a support report came from,
     * and it is the number Play shows. Guarded by AppVersionSourceTest, which fails
     * the build if any shipped source hardcodes a version again.
     */
    override fun getAppVersion(): String =
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
}

fun getSupportManager(): SupportManager = AndroidSupportManager()
