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

    /**
     * Runs [write] against one editor, so N writes become ONE disk transaction.
     *
     * WHY THIS EXISTS (owner: "the phone is lagging slightly when changing training days").
     * SharedPreferences `edit { }` commits asynchronously, but EACH call opens the XML file,
     * serialises every key, writes it and fsyncs. The day-change path wrote nine keys - the
     * saved session plus the per-day progress - as nine independent transactions on the UI's
     * own frame, so a single day swipe queued nine file writes before the frame could finish.
     * Batching turns that into one.
     */
    fun edit(block: PlatformSettingsEditor.() -> Unit)
}

/** The write half of [PlatformSettings.edit]. Read-only accessors are not offered on purpose:
 *  a batch that reads back what it just wrote is a sign the batch is in the wrong place. */
interface PlatformSettingsEditor {
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
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

    override fun edit(block: PlatformSettingsEditor.() -> Unit) {
        // One editor, one commit. `commit = false` keeps the write async - the caller is the UI
        // thread and the data is session state, not a durability guarantee.
        prefs.edit(commit = false) {
            object : PlatformSettingsEditor {
                override fun putString(key: String, value: String) { this@edit.putString(key, value) }
                override fun putBoolean(key: String, value: Boolean) { this@edit.putBoolean(key, value) }
                override fun putInt(key: String, value: Int) { this@edit.putInt(key, value) }
                override fun putLong(key: String, value: Long) { this@edit.putLong(key, value) }
                override fun putFloat(key: String, value: Float) { this@edit.putFloat(key, value) }
                override fun remove(key: String) { this@edit.remove(key) }
            }.apply(block)
        }
    }
}

fun getPlatformSettings(name: String): PlatformSettings {
    val context = appInstance!!
    return AndroidSettings(context.getSharedPreferences(name, Context.MODE_PRIVATE))
}
