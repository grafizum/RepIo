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
import kotlin.math.max

/**
 * THE SIX MUSCLE GLYPHS ARE THE OWNER'S OWN DRAWINGS.
 *
 * Owner, 2026-10-13, five days after the hand-drawn set went in: *"your ... images drawn are not
 * at all the same as the images that you are showing, remake the ... things based on the
 * images"*. He was right, and the glyphs are now TRACED from the reference plates rather than
 * composed by hand: each drawable's paths are the plate's ink, vectorised (see
 * evidence/muscle-icons-2026-10-13/trace_refs.py).
 *
 * THAT CHANGED WHAT "THE SAME HAND" MEANS, so this file changed with it. The old set was stroked
 * line art - one width, round caps, nothing filled - and this test checked exactly those three
 * attributes, which was the right check for the right representation. The traced set is the
 * drawing's ink FILLED: the fill IS the drawn line, and it is the only way the artist's own
 * weight (0.7-1.3 units in this box) and their own round ends survive at all. Checking for a
 * strokeWidth attribute on a filled contour would be checking for the absence of the drawing.
 *
 * So what is asserted here is the invariant underneath those attributes - the thing the owner was
 * actually asking for both times:
 *
 *  1. ONE PART EACH, DRAWN ALONE, AND SIX DIFFERENT ONES. Two groups sharing a drawing means one
 *     of them is lying about which part it filters by, and a glyph that draws nothing is not a
 *     body part at all.
 *  2. THE SAME WEIGHT AS THE KIT GLYPHS. This is the part that had to be re-expressed. The kit's
 *     weight is MEASURED, both from the kit drawables and from these, by rasterising and eroding
 *     the ink until it is gone - half the number of layers is the line's weight, and unlike the
 *     strokeWidth attribute it means the same thing for a filled contour as for a stroked path.
 *     The lane is not written down here; it is the span the equipment icons themselves measure,
 *     because the honest question is "is this the same hand as the kit icons".
 *  3. THE SAME 24dp BOX on the same 24x24 viewport, so the two rows line up.
 *
 * The footprint checks rasterise the real drawables. They are not about beauty - they catch the
 * two ways a vector can silently degrade: painting nothing, and running off its own canvas.
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

    /** The style authority: the kit glyphs whose weight these have to sit inside. */
    private val equipment = listOf(
        "ic_eq_barbell" to R.drawable.ic_eq_barbell,
        "ic_eq_bench" to R.drawable.ic_eq_bench,
        "ic_eq_cable" to R.drawable.ic_eq_cable,
        "ic_eq_chair" to R.drawable.ic_eq_chair,
        "ic_eq_dipbars" to R.drawable.ic_eq_dipbars,
        "ic_eq_kettlebell" to R.drawable.ic_eq_kettlebell,
        "ic_eq_machine" to R.drawable.ic_eq_machine,
        "ic_eq_pullup" to R.drawable.ic_eq_pullup,
        "ic_eq_rack" to R.drawable.ic_eq_rack,
        "ic_eq_rings" to R.drawable.ic_eq_rings,
    )

    private class GlyphPath(
        val data: String,
        val stroke: Double?,
        val fill: String?,
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
                    // A traced glyph is filled and carries no strokeWidth; a stroked one carries
                    // no fill. Both are read here so the weight check below can say which it got.
                    stroke = el.getAttribute("android:strokeWidth").takeIf { it.isNotBlank() }
                        ?.toDouble(),
                    fill = el.getAttribute("android:fillColor").takeIf { it.isNotBlank() },
                )
            }
    }

    // ---------------------------------------------------------------- the ink weight

    private fun rasterize(drawableId: Int, sizePx: Int): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable: Drawable = requireNotNull(ContextCompat.getDrawable(context, drawableId))
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun inkMask(drawableId: Int, sizePx: Int): BooleanArray {
        val bitmap = rasterize(drawableId, sizePx)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // A high threshold, so the antialiasing halo does not thicken the measurement: a pixel
        // counts as ink only when it is mostly opaque.
        return BooleanArray(pixels.size) { pixels[it] ushr 24 > 96 }
    }

    /**
     * HOW THICK THE INK IS, measured the same way for a filled contour and for a stroked line:
     * erode it one layer at a time (a cross-shaped erosion takes a layer off every side of a
     * stroke) and halve the number of layers it took to disappear. A stroke of width w vanishes
     * after w/2 erosions, whatever direction the stroke runs in - which is why this measures the
     * same quantity the strokeWidth attribute describes, for art that no longer has one.
     */
    private fun inkWeightInUnits(drawableId: Int, sizePx: Int = 24 * 20): Double {
        var mask = inkMask(drawableId, sizePx)
        var layers = 0
        while (mask.any { it } && layers < sizePx) {
            val next = BooleanArray(mask.size)
            for (y in 0 until sizePx) {
                for (x in 0 until sizePx) {
                    val i = y * sizePx + x
                    if (!mask[i] || x == 0 || y == 0 || x == sizePx - 1 || y == sizePx - 1) continue
                    next[i] = mask[i - 1] && mask[i + 1] && mask[i - sizePx] && mask[i + sizePx]
                }
            }
            mask = next
            layers++
        }
        return 2.0 * layers * 24.0 / sizePx
    }

    @Test
    fun everyGlyphIsDrawnInTheEquipmentSetsInkWeight() {
        // THE LANE COMES FROM THE KIT's DECLARED STROKE WIDTHS, and the glyphs' weight is
        // MEASURED off the raster. The asymmetry is deliberate: the kit is stroked, so its width
        // is an attribute it can simply be asked for, while a traced glyph is filled and has no
        // such attribute - its width is what its ink actually measures. (Measuring the KIT by
        // erosion instead looks like the tidier symmetry and is wrong: the kettlebell's ink is
        // mostly a filled bell 13 units across, so the kit's "weight" would come out as 13 and a
        // correct traced contour would fail against it for the crime of being line art.)
        val kitWidths = equipment.flatMap { paths(it.first) }.mapNotNull { it.stroke }
        val lightest = kitWidths.min()
        val heaviest = kitWidths.max()

        for (glyph in glyphs) {
            val drawn = paths(glyph.file)
            assertTrue("${glyph.group} draws nothing", drawn.isNotEmpty())

            // Filled, and filled only: the fill IS the traced line. A glyph that mixes a stroked
            // path in with the contours would be two representations in one drawing, and the
            // weight below could no longer be read as one number.
            for (path in drawn) {
                assertNotNull(
                    "${glyph.group} has a path with no fill, so it traces nothing - the traced " +
                        "glyphs are filled contours of the drawing's ink",
                    path.fill,
                )
                assertNull(
                    "${glyph.group} mixes a STROKED path into its contours, so its weight is " +
                        "half a stroke and half a fill",
                    path.stroke,
                )
            }

            val weight = inkWeightInUnits(glyph.id)
            assertTrue(
                "${glyph.group}'s ink measures ${"%.2f".format(weight)} units against the kit's " +
                    "${"%.2f".format(lightest)}..${"%.2f".format(heaviest)} lane, so it will not " +
                    "read as one of them",
                weight >= lightest && weight <= heaviest,
            )
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
            // A part is a closed outline or a figure of several pieces. One stray line is not a
            // body part, and a traced plate is several contours (the drawing's own ink, plus the
            // holes of anything drawn as a ring).
            val closes = drawn.any { it.data.trimEnd().endsWith("Z") }
            assertTrue(
                "$group draws neither a closed outline nor a set of component pieces, so it is " +
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
            // Big enough to be a drawing rather than a speck. The traced plates are fitted into
            // the box with a ~2.7 unit margin, so they land around three quarters of it.
            assertTrue(
                "${glyph.group} paints only ${
                    "%.0f".format(ink.widthFraction * 100)
                }% of its width, which is too little to read as a body part",
                ink.widthFraction >= 0.25,
            )
            assertTrue(
                "${glyph.group} paints only ${
                    "%.0f".format(ink.heightFraction * 100)
                }% of its height, which is too little to read as a body part",
                ink.heightFraction >= 0.25,
            )
            // And it stays inside its box. Ink clipped by the viewport is how a hand-written path
            // goes wrong quietly - it still draws, it just draws amputated.
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
     * THE ONE THING THE FILTER CANNOT GET WRONG. Each broad part resolves to its own glyph
     * ([muscleGroupIconRes]), which the body-part chip draws as its leading icon — so a group
     * that answers with the wrong glyph, or with none, silently mislabels the choice the user is
     * about to make. The glyph moved here from the collapsed row's value slot on 2026-10-12, when
     * the owner rejected it sitting in the exercise list: *"the icons should be in the muscles
     * group filters not in the ... exercises displaying"*.
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

    // ---------------------------------------------------------------- the size it renders at

    /**
     * LEGIBLE WHERE IT IS ACTUALLY DRAWN. The filter chips draw this at 22dp, not 24, and a traced
     * plate carries detail a hand-composed symbol never did - hatch lines, a spine, a grid. If the
     * ink merges at that size the glyph stops being a body part and becomes a smudge, which is the
     * failure this whole round of work was about. So at 22dp the ink still has to break up into
     * more than one connected piece (a drawing) rather than one solid mass (a blob).
     */
    @Test
    fun everyGlyphStillReadsAsADrawingAtTheSizeTheChipDrawsIt() {
        // THE SIZE THE CHIP ACTUALLY USES IS 22dp, and this is where a traced plate is at risk: it
        // carries detail - hatch lines, a spine, a grid - that a hand-composed symbol never did,
        // and if its lines touch at that size the glyph stops being a body part and becomes a
        // smudge. THE MEASURE IS THE INK'S OWN WEIGHT AT THAT SIZE, which is the thing that
        // changes when lines merge: two 1.4-unit lines with a 1-unit gap between them are a
        // 3.8-unit mass once the gap closes, and the reading jumps out of the kit's lane.
        //
        // (Counting connected pieces was the first attempt and it does not work: the leg plate is
        // ONE continuous contour by design - thigh into knee into calf into foot - so a perfectly
        // legible leg reads as a single piece while a smudged one does too.)
        val kitWidths = equipment.flatMap { paths(it.first) }.mapNotNull { it.stroke }
        val lightest = kitWidths.min()
        val heaviest = kitWidths.max()
        for (glyph in glyphs) {
            val chipPx = 22 * 4 // 88px of detail for a 22dp glyph, so the measurement can resolve
            val weight = inkWeightInUnits(glyph.id, chipPx)
            assertTrue(
                "${glyph.group}'s ink measures ${"%.2f".format(weight)} units drawn at the 22dp " +
                    "of the chip, outside the kit's ${"%.2f".format(lightest)}..${
                        "%.2f".format(heaviest)
                    } lane - its lines have merged into a mass",
                weight in lightest..(heaviest + 0.3),
            )

            // ... and it has not bloomed into a silhouette: a solid shape paints most of the area
            // inside its own outline, line art paints a fraction of it.
            val ink = inkOf(rasterize(glyph.id, chipPx))
            val covered = (ink.maxX - ink.minX + 1) * (ink.maxY - ink.minY + 1)
            val painted = inkMask(glyph.id, chipPx).count { it }
            assertTrue(
                "${glyph.group} paints ${painted * 100 / max(1, covered)}% of the area inside its " +
                    "own outline at chip size, which is a solid mass rather than a drawing",
                painted <= covered / 2,
            )
        }
    }
}
