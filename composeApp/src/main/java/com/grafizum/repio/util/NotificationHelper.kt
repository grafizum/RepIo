package com.grafizum.repio.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.grafizum.repio.R

object NotificationHelper {
    private const val CHANNEL_ID = "rest_timer_channel"
    private const val COUNTDOWN_CHANNEL_ID = "rest_timer_countdown_channel"
    private const val NOTIFICATION_ID = 1001
    private const val COUNTDOWN_NOTIFICATION_ID = 1003
    private const val UPDATE_CHANNEL_ID = "app_update_channel"
    private const val UPDATE_NOTIFICATION_ID = 1005

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                lightColor = Color.GREEN
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            // Silent, ongoing channel for the countdown indicator (no sound / vibration while counting down)
            val countdownChannel = NotificationChannel(
                COUNTDOWN_CHANNEL_ID,
                context.getString(R.string.notification_countdown_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_countdown_channel_desc)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(countdownChannel)
        }
    }

    /**
     * Shows a silent, ongoing countdown notification: the remaining time as a big text line
     * under the title (no small chronometer next to the app name) plus a "Skip" action that
     * stops the timer. No sound, no vibration — it's only an indicator.
     *
     * The caller re-posts this from the same 1-second tick that updates the in-app timer,
     * passing the identical [remainingSeconds] value, so both stay in lockstep.
     */
    fun showRestTimerCountdown(context: Context, remainingSeconds: Int) {
        // Only show system notification if notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        createNotificationChannel(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TARGET_SCREEN", "workout")
        } ?: Intent(context, com.grafizum.repio.MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TARGET_SCREEN", "workout")
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Skip action -> broadcast to the receiver so the ViewModel's tick loop clears the timer
        val skipIntent = Intent(context, RestTimerReceiver::class.java).apply {
            action = RestTimerReceiver.ACTION_SKIP_TIMER
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            1004,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeLeft = formatRestTime(remainingSeconds)

        val builder = NotificationCompat.Builder(context, COUNTDOWN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_countdown_title))
            .setContentText(timeLeft)
            .setStyle(NotificationCompat.BigTextStyle().bigText(timeLeft))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, context.getString(R.string.skip_timer), skipPendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(COUNTDOWN_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    fun cancelRestTimerCountdown(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(COUNTDOWN_NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Creates a default-importance channel for the "update available" nudge
     * (visible in the tray, but no heads-up interruption).
     */
    fun createUpdateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                context.getString(R.string.notification_update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_update_channel_desc)
                enableVibration(true)
                enableLights(true)
                lightColor = Color.GREEN
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a system notification telling the user a Play Store update is available.
     * Tapping it opens the app, which immediately launches the Play update flow.
     */
    fun showUpdateAvailableNotification(context: Context) {
        // Only show system notification if notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        createUpdateNotificationChannel(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("START_UPDATE_FLOW", true)
        } ?: Intent(context, com.grafizum.repio.MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("START_UPDATE_FLOW", true)
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1006,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.update_available_title))
            .setContentText(context.getString(R.string.update_available_message))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.update_available_message))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    fun cancelUpdateNotification(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(UPDATE_NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun showRestTimerEndedNotification(context: Context) {
        // Remove the countdown indicator once the timer has ended
        cancelRestTimerCountdown(context)

        val isInForeground = AppEventBus.isAppInForeground.value
        
        // Always trigger in-app event so it's ready when user returns or is inside
        AppEventBus.onRestTimerEnded()
        
        if (isInForeground) {
            vibrate(context)
            return
        }

        // Only show system notification if NOT in app AND notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        createNotificationChannel(context)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TARGET_SCREEN", "workout")
        } ?: Intent(context, com.grafizum.repio.MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("TARGET_SCREEN", "workout")
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    private fun vibrate(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }
        try {
            val pattern = longArrayOf(0, 500) // Single 500ms pulse
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibrator fails
        }
    }
}