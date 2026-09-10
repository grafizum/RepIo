package com.grafizum.repio.util

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

interface PlatformSettings {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)
    fun remove(key: String)
}

private var appInstance: Application? = null

fun initSettings(application: Application) {
    appInstance = application
}

class AndroidSettings(private val prefs: SharedPreferences) : PlatformSettings {
    override fun getString(key: String, defaultValue: String?): String? = prefs.getString(key, defaultValue)
    override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    override fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    override fun putLong(key: String, value: Long) = prefs.edit { putLong(key, value) }
    override fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
    override fun putFloat(key: String, value: Float) = prefs.edit { putFloat(key, value) }
    override fun remove(key: String) = prefs.edit { remove(key) }
}

fun getPlatformSettings(name: String): PlatformSettings {
    val context = appInstance!!
    return AndroidSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))
}
