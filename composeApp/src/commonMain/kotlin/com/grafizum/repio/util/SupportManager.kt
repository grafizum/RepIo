package com.grafizum.repio.util

interface SupportManager {
    suspend fun sendTicket(subject: String, message: String): Result<Unit>
    fun getDeviceModel(): String
    fun getAppVersion(): String
}

expect fun getSupportManager(): SupportManager
