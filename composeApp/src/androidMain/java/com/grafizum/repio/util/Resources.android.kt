package com.grafizum.repio.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.grafizum.repio.R

@Composable
actual fun getString(key: String): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else key
}

@Composable
actual fun getString(key: String, vararg args: Any): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId, *args) else key
}
