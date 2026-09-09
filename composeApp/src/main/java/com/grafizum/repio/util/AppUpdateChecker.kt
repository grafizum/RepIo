package com.grafizum.repio.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
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
 */
object AppUpdateChecker {

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities =
                    connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        } catch (_: Exception) {
            false
        }
    }

    /** True when the installed app actually came from the Play Store (skips sideloaded/debug builds). */
    private fun isInstalledFromPlayStore(context: Context): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer == "com.android.vending"
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
            .edit()
            .putLong(KEY_LAST_DISMISSED, System.currentTimeMillis())
            .apply()
    }

    /**
     * Asks Play whether an update exists.
     *
     * Returns [AppUpdateInfo] only when the device is online, the app is Play-installed,
     * the user is outside the dismissal cooldown, and Play reports a flexible update.
     * Returns null in every other case — callers can treat null as "nothing to do".
     */
    suspend fun checkForUpdate(context: Context, ignoreCooldown: Boolean = false): AppUpdateInfo? {
        // Never force connectivity: silently skip while offline.
        if (!isOnline(context)) return null
        // Only bother the Play Store if the app actually came from there.
        if (!isInstalledFromPlayStore(context)) return null
        // Respect the user's "not now" for a while (unless they explicitly asked to update).
        if (!ignoreCooldown && isWithinCooldown(context)) return null

        return try {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            val info = appUpdateManager.appUpdateInfo.await()

            val updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

            if (updateAvailable && flexibleAllowed) info else null
        } catch (_: Exception) {
            // Play Core unavailable, network hiccup, Play Store missing, etc. — stay quiet.
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
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${activity.packageName}"))
            )
        } catch (_: Exception) {
            try {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
                    )
                )
            } catch (_: Exception) {
                // No browser/store available — nothing else we can do.
            }
        }
    }

    const val UPDATE_REQUEST_CODE = 4711
}
