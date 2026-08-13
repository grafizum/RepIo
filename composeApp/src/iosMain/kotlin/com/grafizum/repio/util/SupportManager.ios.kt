package com.grafizum.repio.util

class IosSupportManager : SupportManager {
    override suspend fun sendTicket(subject: String, message: String): Result<Unit> {
        // Dummy implementation for iOS
        return Result.failure(Exception("Not implemented on iOS"))
    }

    override fun getDeviceModel(): String = "iPhone"

    override fun getAppVersion(): String = "1.0.0"
}

actual fun getSupportManager(): SupportManager = IosSupportManager()
