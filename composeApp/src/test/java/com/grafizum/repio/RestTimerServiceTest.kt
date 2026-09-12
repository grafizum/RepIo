package com.grafizum.repio

import android.Manifest
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.grafizum.repio.util.NotificationHelper
import com.grafizum.repio.util.RestTimerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A rest timer has to keep running after the app is closed, which only works while the
 * countdown is a foreground service: the notification it posts is the one the user sees in
 * the tray, and Android 14+ refuses to start that service at all unless the manifest
 * declares the matching permissions and service type (plus the subtype justification Play
 * asks for). Losing any of that would put us back to a countdown that freezes the moment
 * the app goes away, so the parts the platform enforces are pinned here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RestTimerServiceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun storeTimer(secondsFromNow: Long) {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit { putLong("timer_end_time_millis", System.currentTimeMillis() + secondsFromNow * 1000L) }
    }

    @Test
    fun theCountdownShowsTheTimeThatIsLeft() {
        val notification = NotificationHelper.restTimerCountdownNotification(context, 90)

        assertEquals("1:30", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("1:30", notification.extras.getString(Notification.EXTRA_BIG_TEXT))
        // An indicator, not an alert: it belongs on the silent countdown channel and has
        // to stay in the tray for as long as the rest lasts.
        assertEquals("rest_timer_countdown_channel", notification.channelId)
        assertTrue(
            "the countdown must stay in the tray",
            notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        )
        assertTrue("expected a skip action", notification.actions.isNotEmpty())
    }

    @Test
    fun theRunningTimerTakesOverTheForegroundNotification() {
        storeTimer(secondsFromNow = 90)

        val controller = Robolectric.buildService(RestTimerService::class.java)
        controller.create().startCommand(0, 1)

        val shadow = shadowOf(controller.get())
        assertEquals(
            NotificationHelper.COUNTDOWN_NOTIFICATION_ID,
            shadow.lastForegroundNotificationId
        )
        val notification = shadow.lastForegroundNotification
        assertNotNull("the service must run with the countdown notification", notification)
        assertEquals("1:30", notification?.extras?.getString(Notification.EXTRA_TEXT))

        controller.destroy()
    }

    @Test
    fun withNoTimerStoredTheServiceDoesNotGoForeground() {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit { remove("timer_end_time_millis") }

        val controller = Robolectric.buildService(RestTimerService::class.java)
        controller.create().startCommand(0, 1)

        assertNull(shadowOf(controller.get()).lastForegroundNotification)

        controller.destroy()
    }

    @Test
    fun manifestDeclaresTheForegroundTimerTheSystemExpects() {
        val info = context.packageManager.getServiceInfo(
            ComponentName(context, RestTimerService::class.java),
            PackageManager.GET_META_DATA
        )

        assertNotNull("RestTimerService must be declared", info)
        assertFalse("the timer is ours alone", info.exported)
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            info.foregroundServiceType
        )

        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toList()
        assertTrue(requested.contains(Manifest.permission.FOREGROUND_SERVICE))
        assertTrue(requested.contains(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE))
    }
}
