package com.grafizum.repio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.grafizum.repio.ui.components.muscleGroupIconRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * THE SIX MUSCLE GLYPHS ARE SIX BODY PARTS, DRAWN IN THE EQUIPMENT SET'S HAND.
 *
 * Owner, 2026-10-11: *"i want clean muscle group icons of body parts, arm leg etc etc. not some
 * wierd sht that you just drew in the preview tab. remake them to actually match with the icons you
 * drew for the equipments"*. That is two claims, and each one can quietly stop being true without
 * anybody noticing until the filter row looks wrong:
 *
 *  1. ONE PART EACH, DRAWN ALONE. An arm, a pair of legs, a chest, a back, a core, a pair of
 *     shoulders. Not one figure six times, and not six of the same picture - two groups sharing a
 *     drawing means one of them is lying about which part it filters by, and a glyph that is a
 *     single stray stroke is not a body part at all.
 *  2. THE SAME HAND AS THE KIT GLYPHS. Nothing filled, round caps, and stroke weights inside the
 *     lane the equipment icons themselves use - and the same 24dp box on the same 24x24 viewport,
 *     so the two rows line up instead of one of them sitting proud.
 *
 * The weights are measured off the equipment drawables rather than written down here: the honest
 * question is "is this in the same lane as the kit icons", and that answer lives in those files.
 *
 * The footprint checks rasterise the real drawables. They are not about beauty - they catch the two
 * ways a hand-written vector can silently degrade: painting nothing, and running off its own canvas.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MuscleGlyphFamilyTest {

    private val res = File("src/main/res")

    /** One muscle group, the drawable the filter row shows for it, and that file's name. */
    private data class Glyph(val group: String, val id: Int, val file: String)

    private val glyphs = listOf(
        Glyph("Arms", R.drawable.ic_muscle_arms, "ic_muscle_arms"),
        Glyph("Legs", R.drawable.ic_muscle_legs, "ic_muscle_legs"),
        Glyph("Chest", R.drawable.ic_muscle_chest, "ic_muscle_chest"),
        Glyph("Back", R.drawable.ic_muscle_back, "ic_muscle_back"),
        Glyph("Core", R.drawable.ic_muscle_core, "ic_muscle_core"),
        Glyph("Shoulders", R.drawable.ic_muscle_shoulders, "ic_muscle_shoulders"),
    )

    /** The style authority: the kit glyphs these have to sit beside. */
    private val equipment = listOf(
        "ic_eq_barbell", "ic_eq_bench", "ic_eq_cable", "ic_eq_chair", "ic_eq_dipbars",
        "ic_eq_kettlebell", "ic_eq_machine", "ic_eq_pullup", "ic_eq_rack", "ic_eq_rings",
    )

    private class GlyphPath(
        val data: String,
        val stroke: Double?,
        val fill: String?,
        val cap: String?,
    )

    private fun document(file: String) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(res, "drawable/$file.xml")).documentElement

    private fun paths(file: String): List<GlyphPath> {
        val root = document(file)
        return (0 until root.childNodes.length)
            .mapNotNull { root.childNodes.item(it) as? Element }
            .filter { it.tagName == "path" }
            .map { el ->
                val data = el.getAttribute("android:pathData")
                assertTrue("$file has a <path> with no pathData", data.isNotBlank())
                GlyphPath(
                    data = data,
                    stroke = el.getAttribute("android:strokeWidth").takeIf { it.isNotBlank() }
                        ?.toDouble(),
                    fill = el.getAttribute("android:fillColor").takeIf { it.isNotBlank() },
                    cap = el.getAttribute("android:strokeLineCap").takeIf { it.isNotBlank() },
                )
            }
    }

    // ---------------------------------------------------------------- the stroke lane

    @Test
    fun everyGlyphIsDrawnInTheEquipmentSetsStrokeLane() {
        val equipmentWidths = equipment.flatMap { paths(it) }.mapNotNull { it.stroke }
        val lightest = equipmentWidths.min()
        val heaviest = equipmentWidths.max()

        for (glyph in glyphs) {
            val drawn = paths(glyph.file)
            assertTrue("${glyph.group} draws nothing", drawn.isNotEmpty())
            for (path in drawn) {
                assertNull(
                    "${glyph.group} FILLS a shape. The kit icons are line art - nothing filled - " +
                        "and these sit in the same row.",
                    path.fill,
                )
                val width = path.stroke
                assertNotNull(
                    "${glyph.group} has a path that is neither stroked nor filled, so it draws " +
                        "nothing at all",
                    width,
                )
                assertTrue(
                    "${glyph.group} strokes a line at $width, outside the ${lightest}..$heaviest " +
                        "lane the equipment icons are drawn in, so it will not read as one of them",
                    width!! >= lightest && width <= heaviest,
                )
                assertEquals(
                    "${glyph.group} has a blunt-capped line; the kit icons are all drawn with " +
                        "round caps",
                    "round",
                    path.cap,
                )
            }
        }
    }

    @Test
    fun theGlyphsAreDrawnOnTheEquipmentSetsGrid() {
        val reference = document("ic_eq_kettlebell")
        val width = reference.getAttribute("android:width")
        val height = reference.getAttribute("android:height")
        val viewportWidth = reference.getAttribute("android:viewportWidth")
        val viewportHeight = reference.getAttribute("android:viewportHeight")

        for (glyph in glyphs) {
            val root = document(glyph.file)
            assertEquals("${glyph.group} is not drawn at the kit set's width", width, root.getAttribute("android:width"))
            assertEquals("${glyph.group} is not drawn at the kit set's height", height, root.getAttribute("android:height"))
            assertEquals(
                "${glyph.group} is not drawn on the kit set's viewport",
                viewportWidth,
                root.getAttribute("android:viewportWidth"),
            )
            assertEquals(
                "${glyph.group} is not drawn on the kit set's viewport",
                viewportHeight,
                root.getAttribute("android:viewportHeight"),
            )
        }
    }

    // ---------------------------------------------------------------- six parts, six drawings

    @Test
    fun everyGlyphDrawsAPartAndNoTwoDrawTheSameOne() {
        val drawings = glyphs.associate { it.group to paths(it.file) }

        for ((group, drawn) in drawings) {
            // A body part is either an outline that closes on itself - a limb, a trunk - or a
            // figure built from a handful of strokes. ONE stray stroke is not a part.
            val closes = drawn.any { it.data.trimEnd().endsWith("Z") }
            assertTrue(
                "$group draws neither a closed outline nor a set of component strokes, so it is " +
                    "a stray line rather than a body part",
                closes || drawn.size >= 3,
            )
        }

        for (i in glyphs.indices) {
            for (j in i + 1 until glyphs.size) {
                assertNotEquals(
                    "${glyphs[i].group} and ${glyphs[j].group} draw the SAME picture, so one of " +
                        "them says nothing about which part it filters by",
                    drawings[glyphs[i].group]!!.map { it.data },
                    drawings[glyphs[j].group]!!.map { it.data },
                )
            }
        }
    }

    // ---------------------------------------------------------------- it actually paints

    private fun rasterize(drawableId: Int, sizePx: Int): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable: Drawable = requireNotNull(ContextCompat.getDrawable(context, drawableId))
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private class Ink(
        val widthFraction: Double,
        val heightFraction: Double,
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
    )

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
        return Ink(
            widthFraction = (maxX - minX + 1) / bitmap.width.toDouble(),
            heightFraction = (maxY - minY + 1) / bitmap.height.toDouble(),
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }

    @Test
    fun everyGlyphPaintsAPartThatFitsInsideItsOwnBox() {
        val size = 24 * 8
        for (glyph in glyphs) {
            val ink = inkOf(rasterize(glyph.id, size))
            // Big enough to be a drawing rather than a speck. The narrowest of the six is the
            // abdomen block, which is deliberately compact; the floor sits under it.
            assertTrue(
                "${glyph.group} paints only ${"%.0f".format(ink.widthFraction * 100)}% of its " +
                    "width, which is too little to read as a body part",
                ink.widthFraction >= 0.25,
            )
            assertTrue(
                "${glyph.group} paints only ${"%.0f".format(ink.heightFraction * 100)}% of its " +
                    "height, which is too little to read as a body part",
                ink.heightFraction >= 0.25,
            )
            // And it stays inside its box. A stroke clipped by the viewport is how a hand-written
            // path goes wrong quietly - it still draws, it just draws amputated.
            assertTrue(
                "${glyph.group} touches the left/top edge of its box (${ink.minX}, ${ink.minY})",
                ink.minX >= 2 && ink.minY >= 2,
            )
            assertTrue(
                "${glyph.group} runs off the right/bottom of its box " +
                    "(${ink.maxX}, ${ink.maxY} of $size)",
                ink.maxX <= size - 3 && ink.maxY <= size - 3,
            )
        }
    }

    // ---------------------------------------------------------------- what the row asks for

    /**
     * THE ONE THING THE FILTER ROW CANNOT GET WRONG. It draws
     * `muscleGroupIconRes(selectedMuscleGroup)` in place of the group's name, so a group that
     * answers with the wrong glyph, or with none, silently mislabels the list under it.
     */
    @Test
    fun everyBroadPartResolvesToItsOwnGlyph() {
        for (glyph in glyphs) {
            assertEquals(glyph.group, glyph.id, muscleGroupIconRes(glyph.group))
        }
    }

    /**
     * AND A PRECISE MUSCLE RESOLVES TO ITS PART. The filter offers nineteen precise muscles as well
     * as the six broad parts, so the icon has to survive the same reduction the colour and the
     * badge already do - the row cannot be left showing the word "Lats" while the muscle is
     * filtered to Back.
     */
    @Test
    fun aPreciseMuscleResolvesToItsPartsGlyph() {
        assertEquals(R.drawable.ic_muscle_back, muscleGroupIconRes("Lats"))
        assertEquals(R.drawable.ic_muscle_shoulders, muscleGroupIconRes("Lateral Deltoids"))
        assertEquals(R.drawable.ic_muscle_chest, muscleGroupIconRes("Pectorals"))
        assertEquals(R.drawable.ic_muscle_arms, muscleGroupIconRes("Biceps"))
        assertEquals(R.drawable.ic_muscle_legs, muscleGroupIconRes("Hamstrings"))
        assertEquals(R.drawable.ic_muscle_core, muscleGroupIconRes("Obliques"))
    }

    /**
     * AND "ANY" IS A WORD, NOT A GLYPH. Nothing is chosen, so no body part can be drawn - the row
     * falls back to the label, exactly as the equipment row falls back to "All equipment" when it
     * has no kit glyph to show. A glyph here would claim a part the user never picked, and the
     * unplaceable groups (a custom exercise's free-text muscle) have no part to claim at all.
     */
    @Test
    fun anUnchosenOrUnplaceableGroupHasNoGlyph() {
        assertNull(muscleGroupIconRes(null))
        assertNull(muscleGroupIconRes(""))
        assertNull(muscleGroupIconRes("Other"))
        assertNull(muscleGroupIconRes("Hip Flexors"))
    }
}
