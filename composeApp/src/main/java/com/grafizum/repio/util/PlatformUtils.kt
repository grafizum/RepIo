package com.grafizum.repio.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import com.grafizum.repio.BuildConfig
import com.grafizum.repio.R
import com.grafizum.repio.RepIoApplication
import java.util.Calendar

private var appInstance: Application? = null

fun initPlatformUtils(application: Application) {
    appInstance = application
}

/** The Application context, for helpers that must work without an Activity (null before init). */
fun getApplicationContext(): android.content.Context? = appInstance

/**
 * The Application context, for callers that cannot proceed without one.
 *
 * `getApplicationContext()` is nullable because helpers may legitimately be asked
 * before init. A caller that genuinely needs the context should fail loudly instead
 * of silently receiving null and producing a null-pointer somewhere further away -
 * which is the failure mode that is hardest to trace back to its cause.
 */
fun requireApplicationContext(): android.content.Context =
    appInstance ?: error(
        "Application context requested before initPlatformUtils() ran. " +
            "If this is at startup, the caller must not need the context yet - " +
            "defer the work rather than reaching for it during onCreate."
    )

fun applyAppIconChange(themeKey: String) {
    val context = appInstance ?: return
    IconSwitchHelper.switchIcon(context, themeKey)
}

fun getTodayDayOfWeek(): String {
    val calendar = Calendar.getInstance()
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "MONDAY"
        Calendar.TUESDAY -> "TUESDAY"
        Calendar.WEDNESDAY -> "WEDNESDAY"
        Calendar.THURSDAY -> "THURSDAY"
        Calendar.FRIDAY -> "FRIDAY"
        Calendar.SATURDAY -> "SATURDAY"
        Calendar.SUNDAY -> "SUNDAY"
        else -> "MONDAY"
    }
}

fun currentTimeMillis(): Long = System.currentTimeMillis()

/** The app-wide [Application], for the few non-UI paths (notifications) that must reach the
 *  OS. Set in [initPlatformUtils] before any ViewModel exists. */
fun appContext(): android.content.Context =
    appInstance ?: throw IllegalStateException("initPlatformUtils() has not run yet")

/** Millis at 00:00 of the current calendar day (same basis as [currentTimeMillis]). */
fun startOfTodayMillis(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

fun openUrl(url: String) {
    val context = appInstance ?: return
    try {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, formattedUrl.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Log or show toast
    }
}

/** Where the support button addresses its message. */
private const val SUPPORT_EMAIL = "repio@grafizum.com"

/**
 * Opens the user's own mail app, addressed to [SUPPORT_EMAIL], with the app version
 * already in the subject.
 *
 * WHY THERE IS NO FORM ANY MORE. The support form used to write `subject`, `message`,
 * `device_model`, `app_version` and a timestamp into Firestore - which meant a Google
 * Play Data safety declaration for data the app itself collected. Handing the message
 * to a mail app instead means the app transmits nothing at all: the data-safety answer
 * becomes "no data collected", there is no endpoint to protect with App Check, and a
 * user can read (and discard) exactly what is about to be sent before it leaves.
 *
 * ACTION_SENDTO with a `mailto:` URI, not ACTION_VIEW: it targets mail apps only, so
 * a browser cannot claim it, and it needs no `<queries>` entry on API 30+. A device
 * with no mail app throws ActivityNotFoundException, which is why it is caught - the
 * alternative is a crash on a button press.
 */
fun openSupportEmail() {
    val context = appInstance ?: return
    val subject = context.getString(R.string.support_email_subject, BuildConfig.VERSION_NAME)
    val uri: Uri = Uri.parse("mailto:$SUPPORT_EMAIL")
        .buildUpon()
        .appendQueryParameter("subject", subject)
        .build()
    try {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        // No mail app installed. Nothing to open, and the button must not crash.
    }
}
