package com.grafizum.repio.util

import android.content.Intent
import android.net.Uri
import android.app.Application
import com.grafizum.repio.RepIoApplication

private var appInstance: Application? = null

fun initPlatformUtils(application: Application) {
    appInstance = application
}

actual fun applyAppIconChange(themeKey: String) {
    val context = appInstance ?: return
    IconSwitchHelper.switchIcon(context, themeKey)
}

actual fun openUrl(url: String) {
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
