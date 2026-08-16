package com.grafizum.repio.util

import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface SupportManager {
    suspend fun sendTicket(subject: String, message: String): Result<Unit>
    fun getDeviceModel(): String
    fun getAppVersion(): String
}

class AndroidSupportManager : SupportManager {
    override suspend fun sendTicket(subject: String, message: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            val db = FirebaseFirestore.getInstance()
            val ticket = hashMapOf(
                "subject" to subject,
                "message" to message,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "status" to "open",
                "device_model" to getDeviceModel(),
                "app_version" to getAppVersion()
            )
            db.collection("support_tickets").add(ticket)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    val descriptiveError = when {
                        e.message?.contains("PERMISSION_DENIED") == true -> 
                            Exception("Permission Denied: Check Firestore rules or authentication.", e)
                        e.message?.contains("AppCheck") == true -> 
                            Exception("App Check Failed: Secure connection could not be verified.", e)
                        else -> e
                    }
                    if (continuation.isActive) continuation.resume(Result.failure(descriptiveError))
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(Result.failure(e))
        }
    }

    override fun getDeviceModel(): String = Build.MODEL

    override fun getAppVersion(): String = "1.0.0" 
}

fun getSupportManager(): SupportManager = AndroidSupportManager()
