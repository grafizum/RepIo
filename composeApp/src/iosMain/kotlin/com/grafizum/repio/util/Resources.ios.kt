package com.grafizum.repio.util

import androidx.compose.runtime.Composable

@Composable
actual fun getString(key: String): String = key

@Composable
actual fun getString(key: String, vararg args: Any): String = key
