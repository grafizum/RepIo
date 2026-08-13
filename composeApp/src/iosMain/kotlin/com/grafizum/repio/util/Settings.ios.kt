package com.grafizum.repio.util

class IosSettings : PlatformSettings {
    override fun getString(key: String, defaultValue: String?): String? = defaultValue
    override fun putString(key: String, value: String) {}
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    override fun putBoolean(key: String, value: Boolean) {}
    override fun getInt(key: String, defaultValue: Int): Int = defaultValue
    override fun putInt(key: String, value: Int) {}
    override fun getLong(key: String, defaultValue: Long): Long = defaultValue
    override fun putLong(key: String, value: Long) {}
    override fun getFloat(key: String, defaultValue: Float): Float = defaultValue
    override fun putFloat(key: String, value: Float) {}
}

actual fun getPlatformSettings(name: String): PlatformSettings = IosSettings()
