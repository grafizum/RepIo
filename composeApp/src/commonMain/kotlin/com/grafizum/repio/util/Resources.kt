package com.grafizum.repio.util

import androidx.compose.runtime.Composable

@Composable
expect fun getString(key: String): String

@Composable
expect fun getString(key: String, vararg args: Any): String
