package com.grafizum.repio.util

import android.content.Intent
import android.net.Uri
import android.app.Application
import com.grafizum.repio.RepIoApplication
import java.util.Calendar

private var appInstance: Application? = null

fun initPlatformUtils(application: Application) {
    appInstance = application
}

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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Log or show toast
    }
}
