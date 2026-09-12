package com.grafizum.repio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The notification icon has to be the launcher icon's mark, not a lookalike:
 * same proportions, same spacing between the dumbbell and the heartbeat line,
 * same share of its box, same centring.
 *
 * The launcher logo sits at x 34..73, y 33..74.5 on the 108dp adaptive-icon
 * canvas, which is 39 x 41.5dp — 54.17% wide and 57.64% tall *of the 72dp
 * viewport the launcher mask actually shows*. These tests rasterise the real
 * drawables and check that the notification icon reproduces those ratios,
 * so a redraw can't silently drift again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NotificationIconTest {

    /** Launcher logo, as a fraction of the 72dp visible viewport. */
    private val launcherLogoWidth = 39.0 / 72.0      // 0.5417
    private val launcherLogoHeight = 41.5 / 72.0     // 0.5764
    private val launcherLogoCenterX = 53.5 / 108.0   // 0.4954
    private val launcherLogoCenterY = 53.75 / 108.0  // 0.4977

    // The 24dp icon is rasterised at 8x, so a pixel of antialiasing on each
    // edge is worth 2/192 = 0.0104 of the box.
    private val tolerance = 0.015

    private fun rasterize(drawableId: Int, sizePx: Int): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable: Drawable = requireNotNull(ContextCompat.getDrawable(context, drawableId))
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private data class Ink(
        val widthFraction: Double,
        val heightFraction: Double,
        val centerX: Double,
        val centerY: Double
    )

    /** Bounding box of every pixel the drawable actually painted. */
    private fun inkOf(bitmap: Bitmap): Ink {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        pixels.forEachIndexed { index, argb ->
            if (argb ushr 24 > 8) {
                val x = index % bitmap.width
                val y = index / bitmap.width
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }
        assertTrue("the drawable painted nothing at all", maxX >= 0)
        val w = bitmap.width.toDouble()
        val h = bitmap.height.toDouble()
        return Ink(
            widthFraction = (maxX - minX + 1) / w,
            heightFraction = (maxY - minY + 1) / h,
            centerX = ((minX + maxX) / 2.0 + 0.5) / w,
            centerY = ((minY + maxY) / 2.0 + 0.5) / h
        )
    }

    @Test
    fun notificationIconReproducesTheLauncherLogoFootprint() {
        val ink = inkOf(rasterize(R.drawable.ic_notification, 24 * 8))

        assertEquals("logo width share", launcherLogoWidth, ink.widthFraction, tolerance)
        assertEquals("logo height share", launcherLogoHeight, ink.heightFraction, tolerance)
        assertEquals("horizontally centred", 0.5, ink.centerX, tolerance)
        assertEquals("vertically centred", 0.5, ink.centerY, tolerance)
    }

    @Test
    fun launcherMonochromeLayerMatchesTheForegroundPlacement() {
        // Not centred exactly: it copies the foreground layer, which sits at
        // 39/108 x 41.5/108 with its centre a hair above the middle.
        val ink = inkOf(rasterize(R.drawable.ic_launcher_monochrome, 108 * 4))

        assertEquals("logo width share", 39.0 / 108.0, ink.widthFraction, tolerance)
        assertEquals("logo height share", 41.5 / 108.0, ink.heightFraction, tolerance)
        assertEquals("horizontal centre", launcherLogoCenterX, ink.centerX, tolerance)
        assertEquals("vertical centre", launcherLogoCenterY, ink.centerY, tolerance)
    }

    @Test
    fun bothIconsShareTheSameArtwork() {
        // The two drawables duplicate the path data (Android cannot share a
        // VectorDrawable's geometry), so guard against one being edited alone.
        assertEquals(
            pathDataOf(R.drawable.ic_notification),
            pathDataOf(R.drawable.ic_launcher_monochrome)
        )
    }

    /** Every pathData in a vector drawable, in document order. */
    private fun pathDataOf(drawableId: Int): List<String> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(drawableId)
        val found = mutableListOf<String>()
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                val data = parser.getAttributeValue(
                    "http://schemas.android.com/apk/res/android", "pathData"
                )
                if (data != null) found += data.replace(Regex("\\s+"), " ").trim()
            }
            event = parser.next()
        }
        return found
    }
}
