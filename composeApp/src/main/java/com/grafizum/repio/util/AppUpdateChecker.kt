package com.grafizum.repio.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Checks the Play Store for an available app update and gives the user a gentle,
 * dismissible nudge ("flexible" flow).
 *
 * Ground rules:
 *  - Never forces connectivity: the check simply no-ops when the device is offline.
 *  - The nudge only appears when Play actually reports an update.
 *  - If the user dismisses the nudge, it stays quiet for [NUDGE_COOLDOWN_MS],
 *    then nudges again the next time they open the app while online.
 *
 * The install *source* is deliberately not a gate. It used to be — the check
 * silently returned null unless the installer was the Play Store — which meant a
 * sideloaded build could never be nudged and there was no way to tell a broken
 * check from an up-to-date app. Play is already the authority on whether an
 * update exists, so the answer is taken from Play alone: a build installed by adb
 * (installerPackageName == null) is now checked like any other, and simply stays
 * quiet while the installed version is the newest one Play publishes.
 */
object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"

    /** How long to stay quiet after the user taps "Not now" (3 days). */
    private const val NUDGE_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000

    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_LAST_DISMISSED = "update_nudge_last_dismissed"

    /** True when the device has an active internet connection (any transport). */
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        return try {
            // minSdk is 24 (N), so the modern NetworkCapabilities API is always available.
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /** True if the user dismissed the nudge more recently than the cooldown window. */
    fun isWithinCooldown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDismissed = prefs.getLong(KEY_LAST_DISMISSED, 0L)
        return System.currentTimeMillis() - lastDismissed < NUDGE_COOLDOWN_MS
    }

    /** Stores "user said not now" so we don't nag again for [NUDGE_COOLDOWN_MS]. */
    fun markNudgeDismissed(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putLong(KEY_LAST_DISMISSED, System.currentTimeMillis()) }
    }

    /**
     * Asks Play whether an update exists.
     *
     * Returns [AppUpdateInfo] only when the device is online, the user is outside the
     * dismissal cooldown, and Play reports a flexible update. Returns null in every
     * other case — callers can treat null as "nothing to do".
     *
     * Every outcome is logged, because the silent variants (offline, cooling down,
     * Play unreachable) are indistinguishable from "you are up to date" at the UI
     * level by design.
     */
    suspend fun checkForUpdate(context: Context, ignoreCooldown: Boolean = false): AppUpdateInfo? {
        // Never force connectivity: silently skip while offline.
        if (!isOnline(context)) {
            Log.i(TAG, "Update check skipped: no active network.")
            return null
        }
        // Respect the user's "not now" for a while (unless they explicitly asked to update).
        if (!ignoreCooldown && isWithinCooldown(context)) {
            Log.i(TAG, "Update check skipped: inside the $NUDGE_COOLDOWN_MS ms dismissal cooldown.")
            return null
        }

        return try {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            val info = appUpdateManager.appUpdateInfo.await()

            val availability = info.updateAvailability()
            val updateAvailable = availability == UpdateAvailability.UPDATE_AVAILABLE
            val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            Log.i(
                TAG,
                "Play answered availability=$availability " +
                    "availableVersionCode=${info.availableVersionCode()} " +
                    "updatePriority=${info.updatePriority()} flexibleAllowed=$flexibleAllowed"
            )

            if (updateAvailable && flexibleAllowed) info else null
        } catch (cancellation: CancellationException) {
            // Leaving the foreground cancels the check. Not a failure — never swallow it.
            throw cancellation
        } catch (exception: Exception) {
            // Play Core unavailable, network hiccup, Play Store missing, etc. — stay quiet,
            // but leave a trace so a check that never fires can be told apart from one that
            // ran and found nothing.
            val errorCode = (exception as? InstallException)?.errorCode
            if (errorCode == InstallErrorCode.ERROR_APP_NOT_OWNED) {
                // Play only answers for apps it installed itself. A build installed with
                // adb is not "owned", so there is nothing for Play to offer — and no
                // amount of checking will change that. Install the bundle from Play
                // (internal testing counts) to exercise the nudge.
                Log.i(
                    TAG,
                    "Update check skipped: this build was not acquired from Play " +
                        "(ERROR_APP_NOT_OWNED), so Play owns no update for it."
                )
            } else {
                Log.w(TAG, "Update check failed: ${exception.javaClass.simpleName}: ${exception.message}")
            }
            null
        }
    }

    /** Starts the Play Store flexible update flow (small, non-blocking download nudge). */
    fun startFlexibleUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        try {
            AppUpdateManagerFactory.create(activity).startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                UPDATE_REQUEST_CODE
            )
        } catch (_: Exception) {
            // If Play cannot show its dialog, fall back to the store page.
            openPlayStore(activity)
        }
    }

    /** Opens the app's Play Store listing as a last-resort nudge path. */
    fun openPlayStore(activity: Activity) {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=${activity.packageName}".toUri())
            )
        } catch (_: Exception) {
            try {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
                    )
                )
            } catch (_: Exception) {
                // No browser/store available — nothing else we can do.
            }
        }
    }

    const val UPDATE_REQUEST_CODE = 4711
}
