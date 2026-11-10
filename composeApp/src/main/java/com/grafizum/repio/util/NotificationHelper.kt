package com.grafizum.repio.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.graphics.Color
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.grafizum.repio.R

object NotificationHelper {
    private const val CHANNEL_ID = "rest_timer_channel"
    private const val NOTIFICATION_ID = 1001

    private const val UPDATE_CHANNEL_ID = "app_update_channel"
    private const val UPDATE_NOTIFICATION_ID = 1005
    private const val MOTIVATION_CHANNEL_ID = "motivation_channel"
    private const val MOTIVATION_NOTIFICATION_ID = 1007

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

    /**
     * Clears a rest / timed-exercise alert that is still sitting in the tray.
     *
     * The alert is only ever POSTED while the app is in the background (in the
     * foreground the same moment vibrates and the in-app banner says it), so by
     * the time the app is open the tray entry describes something already over.
     * Owner, 2026-11-03: *"Have repio message be dismissed when opening the app
     * or when the app is open once it gave the popup notification."*
     */
    fun cancelRestTimerNotification(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
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

    /**
     * Opens this app's own notification settings page.
     *
     * The last resort when the runtime permission dialog will not appear: on
     * Android 13+ a second denial is FINAL, and `launch(POST_NOTIFICATIONS)`
     * then returns immediately without showing anything - a tap that looks
     * like it did nothing. Settings is the only place the grant can still be
     * switched on, so a refused tap has to land there rather than go silent.
     *
     * Three intents, tried in order, because the first two are absent on some
     * OEM builds: the per-app notification page, the app's detail page, and the
     * top-level settings screen as a final fallback.
     */
    fun openNotificationSettings(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try the next, less specific page.
            }
        }
    }

    /**
     * Default-importance channel for the motivational "you skipped a day" nudge.
     * Deliberately not HIGH: it should sit quietly in the tray, not interrupt.
     */
    fun createMotivationNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MOTIVATION_CHANNEL_ID,
                context.getString(R.string.notification_motivation_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_motivation_channel_desc)
                enableVibration(false)
                enableLights(true)
                lightColor = Color.GREEN
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts the skip nudge. Only two things are shown: the short [line] and the
     * [author] credited directly beneath it. No extra copy — the quote is the
     * whole message. Tapping opens the workout screen.
     */
    fun showMotivationNotification(
        context: Context,
        line: String,
        author: String
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }

        createMotivationNotificationChannel(context)

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
            1008,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MOTIVATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(line)
            // Only the quote and its credit, so the name is visible even when the
            // line is all the user reads.
            .setSubText(author)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$line\n— $author")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            NotificationManagerCompat.from(context).notify(MOTIVATION_NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Fallback gracefully
        }
    }

    /** Clears the skip nudge (user came back, trained, or turned the setting off). */
    fun cancelMotivationNotification(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(MOTIVATION_NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Reports the end of a rest.
     *
     * [alert] is false for a silent timer, which ends without a sound, a vibration or a
     * notification — the session still hears about it either way.
     */
    fun showRestTimerEndedNotification(
        context: Context,
        alert: Boolean = true,
        wasExerciseTimer: Boolean = false
    ) {
        val isInForeground = AppEventBus.isAppInForeground.value

        // Always trigger in-app event so it's ready when user returns or is inside
        AppEventBus.onRestTimerEnded(wasExerciseTimer)

        if (!alert) return

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
            .setContentTitle(
                context.getString(
                    // A TIMED EXERCISE is not a rest. One function fired both
                    // alerts with the rest wording, so finishing a 45-second
                    // plank announced "Rest Pause Ended! Time to smash your next
                    // set!" - the owner reported it as "wrong, i want it to show
                    // some other message to say the time finished but not rest
                    // time". The KIND of timer is the only thing that decides
                    // these two strings, so it is a parameter rather than a
                    // second notification method that could drift.
                    if (wasExerciseTimer) R.string.notification_exercise_title
                    else R.string.notification_title
                )
            )
            .setContentText(
                context.getString(
                    if (wasExerciseTimer) R.string.notification_exercise_text
                    else R.string.notification_text
                )
            )
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
