package com.grafizum.repio

import com.grafizum.repio.ui.components.MaxContentWidth
import com.grafizum.repio.ui.components.WindowSize
import com.grafizum.repio.ui.components.WindowWidthClass
import com.grafizum.repio.ui.components.frameScaleFor
import com.grafizum.repio.ui.components.windowSizeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards [windowSizeFor], the single place that decides how big everything is.
 *
 * The contract these tests pin down is *adaptivity*: every dimension is interpolated between
 * three anchors (smallest phone, typical phone, large tablet) so a window of any size gets values
 * proportional to its own space, and no device is ever served by a hard-coded bucket.
 *
 * The history behind it:
 *
 * 1. The workout day wheel's chips filled their parent's *maximum* height instead of hugging their
 *    label, so all seven stretched to 388dp on a 360x640dp phone and pushed the day's exercise
 *    list below down to zero height. The fix is a definite `dayChipHeight`, which also has to stay
 *    a comfortable tap target and must not be handed out at short-window sizes.
 * 2. The analytics muscle-group card asked for `weight(1f)` - whatever height was left over - and
 *    on a short window that was ~0dp, so the whole card vanished. Hence `donutHeight` is a
 *    definite, height-derived value.
 * 3. The tokens used to be picked from four hard-coded profiles (narrow / compact / medium /
 *    expanded), so two phones one dp apart could receive wildly different spacing. The
 *    continuity tests below are what stop that from coming back.
 */
class WindowSizeTest {

    private fun token(widthDp: Int, heightDp: Int = 800, fontScale: Float = 1f) =
        windowSizeFor(widthDp, heightDp, fontScale)

    // ---------------------------------------------------------------------------------------
    // Continuity: the property that makes the design adaptive rather than tuned per device.
    // ---------------------------------------------------------------------------------------

    @Test
    fun oneDpOfWidthNeverChangesATokenByMoreThanOneDp() {
        // A jump between neighbouring widths is exactly the "custom measured per screen" failure:
        // a device one pixel away from a bucket would otherwise get a different layout.
        var previous: WindowSize? = null
        for (width in 320..1200) {
            val size = token(width, 640)
            previous?.let { prev ->
                assertSmallStep("screenPadding", prev, size) { it.screenPadding.value }
                assertSmallStep("sectionSpacing", prev, size) { it.sectionSpacing.value }
                assertSmallStep("cardPadding", prev, size) { it.cardPadding.value }
                assertSmallStep("headerMinHeight", prev, size) { it.headerMinHeight.value }
                assertSmallStep("headerIconSize", prev, size) { it.headerIconSize.value }
                assertSmallStep("contentMaxWidth", prev, size) { it.contentMaxWidth.value }
                assertSmallStep("dialogMaxWidth", prev, size) { it.dialogMaxWidth.value }
                assertSmallStep("dialogCorner", prev, size) { it.dialogCorner.value }
                assertSmallStep("dialogGap", prev, size) { it.dialogGap.value }
                assertSmallStep("inlineGap", prev, size) { it.inlineGap.value }
                assertSmallStep("typeScale", prev, size) { it.typeScale }
            }
            previous = size
        }
    }

    @Test
    fun oneDpOfHeightNeverChangesAVerticalTokenByMoreThanOneDp() {
        var previous: WindowSize? = null
        for (height in 480..1200) {
            val size = token(360, height)
            previous?.let { prev ->
                assertSmallStep("dayChipHeight", prev, size) { it.dayChipHeight.value }
                assertSmallStep("donutHeight", prev, size) { it.donutHeight.value }
            }
            previous = size
        }
    }

    private fun assertSmallStep(
        name: String,
        previous: WindowSize,
        current: WindowSize,
        read: (WindowSize) -> Float
    ) {
        val delta = kotlin.math.abs(read(current) - read(previous))
        assertTrue(
            "$name jumped by $delta between neighbouring window sizes - tokens must interpolate",
            delta <= 1f
        )
    }

    @Test
    fun everyWidthInRangeGetsItsOwnSpacingValue() {
        // Sanity check on the ramps: if these collapsed into a handful of values we would be
        // bucketing again, just with more buckets.
        val paddings = (320..1000).map { token(it, 640).screenPadding.value }.distinct()
        assertTrue("expected fine-grained spacing, got ${paddings.size} distinct values", paddings.size > 200)
    }

    @Test
    fun realPhonesDoNotShareOneHardCodedLayout() {
        val s6Edge = token(360, 640).screenPadding.value
        val pixel4a = token(393, 851).screenPadding.value
        val pixel8 = token(411, 915).screenPadding.value

        assertTrue("360dp and 393dp must not be rounded to the same spacing", s6Edge != pixel4a)
        assertTrue("393dp and 411dp must not be rounded to the same spacing", pixel4a != pixel8)
        assertTrue("spacing must grow with the window", s6Edge < pixel4a && pixel4a < pixel8)
    }

    // ---------------------------------------------------------------------------------------
    // Monotonicity and bounds.
    // ---------------------------------------------------------------------------------------

    @Test
    fun spacingNeverShrinksAsTheWindowGrows() {
        var previous = token(320, 640)
        for (width in 321..1200) {
            val size = token(width, 640)
            assertTrue("screenPadding shrank at ${width}dp", size.screenPadding.value >= previous.screenPadding.value)
            assertTrue("sectionSpacing shrank at ${width}dp", size.sectionSpacing.value >= previous.sectionSpacing.value)
            assertTrue("cardPadding shrank at ${width}dp", size.cardPadding.value >= previous.cardPadding.value)
            previous = size
        }
    }

    @Test
    fun chipsStayAComfortableTapTargetOnEveryWindow() {
        // Halfway through the supported range at every height, on every width we care about.
        val widths = listOf(320, 360, 393, 411, 430, 600, 700, 840, 1000, 1280)
        val heights = listOf(400, 480, 560, 640, 800, 915, 1000, 1200)
        widths.forEach { w ->
            heights.forEach { h ->
                val chip = token(w, h).dayChipHeight.value
                assertTrue("chip height $chip for ${w}x$h is too small to tap", chip >= 48f)
                assertTrue("chip height $chip for ${w}x$h is absurdly tall", chip <= 56f)
            }
        }
    }

    @Test
    fun analyticsCardAlwaysHasRoomToRender() {
        // The card needs its header, the ring (>= 96dp) and its padding to fit.
        val widths = listOf(320, 360, 393, 411, 600, 840, 1280)
        val heights = listOf(480, 640, 800, 1000, 1200)
        widths.forEach { w ->
            heights.forEach { h ->
                val donut = token(w, h).donutHeight.value
                assertTrue("donut height $donut for ${w}x$h cannot fit the chart", donut >= 190f)
                assertTrue("donut height $donut for ${w}x$h wastes the viewport", donut <= 340f)
            }
        }
    }

    @Test
    fun everyPopupGetsTheSameSurfaceOnAGivenWindow() {
        // The owner asked for the popup cards to stop mixing designs, so the
        // dialog tokens have to be a function of the window alone: two popups on
        // one window are the same size and radius, and a phone never gets a
        // popup wider than the screen it is on.
        val widths = listOf(320, 360, 393, 411, 430, 600, 700, 840, 1000, 1280)
        widths.forEach { w ->
            val size = token(w, 640)
            // The popup is the smaller of its cap and the room the screen padding
            // leaves, so a 320dp phone gets a full-width sheet and a tablet gets the
            // capped card - never one wider than the window, never a sliver.
            val effective = minOf(size.dialogMaxWidth.value, w - 2 * size.screenPadding.value)
            assertTrue(
                "popup would be ${effective}dp wide at ${w}dp",
                effective >= 240f && effective <= w
            )
            assertTrue(
                "dialog corner ${size.dialogCorner} at ${w}dp is not a rounded card",
                size.dialogCorner.value in 16f..32f
            )
            assertTrue(
                "dialog gap ${size.dialogGap} at ${w}dp is unusable",
                size.dialogGap.value >= 8f
            )
        }
    }

    @Test
    fun typeScaleStaysInsideAReadableRange() {
        (320..1400 step 7).forEach { width ->
            val scale = token(width, 900).typeScale
            assertTrue("typeScale $scale at ${width}dp is unreadably small", scale >= 0.92f)
            assertTrue("typeScale $scale at ${width}dp is oversized", scale <= 1.12f)
        }
    }

    @Test
    fun anchorValuesAreExactlyWhereTheyAreDocumented() {
        // The three anchors are the contract the rest of the file interpolates between.
        val smallest = token(320, 480)
        val phone = token(400, 640)
        val large = token(1000, 1000)

        assertEquals(10f, smallest.screenPadding.value, 0.001f)
        assertEquals(16f, phone.screenPadding.value, 0.001f)
        assertEquals(32f, large.screenPadding.value, 0.001f)

        assertEquals(48f, smallest.dayChipHeight.value, 0.001f)
        assertEquals(52f, phone.dayChipHeight.value, 0.001f)
        assertEquals(56f, large.dayChipHeight.value, 0.001f)

        assertEquals(480f, smallest.dialogMaxWidth.value, 0.001f)
        assertEquals(560f, phone.dialogMaxWidth.value, 0.001f)
        assertEquals(680f, large.dialogMaxWidth.value, 0.001f)

        assertEquals(20f, smallest.dialogCorner.value, 0.001f)
        assertEquals(24f, phone.dialogCorner.value, 0.001f)
        assertEquals(28f, large.dialogCorner.value, 0.001f)

        assertEquals(190f, smallest.donutHeight.value, 0.001f)
        assertEquals(240f, phone.donutHeight.value, 0.001f)
        assertEquals(340f, large.donutHeight.value, 0.001f)


        assertEquals(0.92f, smallest.typeScale, 0.001f)
        assertEquals(1f, phone.typeScale, 0.001f)
        assertEquals(1.12f, large.typeScale, 0.001f)
    }

    // ---------------------------------------------------------------------------------------
    // Which axis drives which token.
    // ---------------------------------------------------------------------------------------

    @Test
    fun heightOnlyMovesTheTokensThatConsumeVerticalSpace() {
        // Turning a window short must not shrink its horizontal rhythm; it hands back *vertical*
        // room so the content can still fit.
        val tall = token(360, 900)
        val short = token(360, 640)

        assertEquals(tall.widthClass, short.widthClass)
        assertEquals(tall.isNarrow, short.isNarrow)
        assertEquals(tall.screenPadding.value, short.screenPadding.value, 0.001f)
        assertEquals(tall.cardPadding.value, short.cardPadding.value, 0.001f)
        assertEquals(tall.typeScale, short.typeScale, 0.001f)
        assertEquals(tall.contentMaxWidth.value, short.contentMaxWidth.value, 0.001f)

        assertTrue("a shorter window must not get taller chips", short.dayChipHeight.value < tall.dayChipHeight.value)
        assertTrue("a shorter window must not get a taller analytics card", short.donutHeight.value < tall.donutHeight.value)
    }

    @Test
    fun widthOnlyMovesTheTokensThatConsumeHorizontalSpace() {
        val narrow = token(360, 640)
        val wide = token(1200, 640)

        assertEquals(narrow.dayChipHeight.value, wide.dayChipHeight.value, 0.001f)
        assertEquals(narrow.donutHeight.value, wide.donutHeight.value, 0.001f)
        assertTrue(wide.screenPadding.value > narrow.screenPadding.value)
        assertTrue(wide.contentMaxWidth.value > narrow.contentMaxWidth.value)
    }

    @Test
    fun contentColumnWidensOnBiggerSurfacesButStaysReadable() {
        assertTrue(token(320, 800).contentMaxWidth.value >= MaxContentWidth.value)
        assertTrue(token(700, 900).contentMaxWidth.value > MaxContentWidth.value)
        assertTrue(token(1200, 900).contentMaxWidth.value > token(700, 900).contentMaxWidth.value)
        // Never a single line stretched across a desktop window.
        assertTrue(token(2560, 1400).contentMaxWidth.value <= 800f)
    }

    // ---------------------------------------------------------------------------------------
    // Accessibility: the user's font scale has to be able to grow the boxes around their text.
    // ---------------------------------------------------------------------------------------

    @Test
    fun largerSystemFontsGrowTheBoxesThatHoldText() {
        // THE MECHANISM MOVED ON 2026-10-08, THE CONTRACT DID NOT.
        //
        // The boxes around the text still have to grow with it - but they now grow
        // in the DENSITY (see WorkoutTrackerTheme), because the thing that was
        // crowded was not a token: it was every hard-coded `Modifier.padding(16.dp)`
        // and `Spacer(height = 4.dp)` inside the components, which no token table can
        // reach. So what has to hold is the ARITHMETIC of that override:
        //   dp  -> px grows by the factor
        //   sp  (= density x fontScale) -> px does not move at all
        // asserted against the owner's emulator (420dpi, font scale 1.8).
        val platformDensity = 2.625f
        val fontScale = 1.8f
        val frame = frameScaleFor(fontScale)
        assertEquals("the frame takes the WHOLE font surplus", 1.8f, frame, 0.001f)

        val scaledDensity = platformDensity * frame
        val scaledFontScale = fontScale / frame
        assertEquals(
            "a hard-coded 4dp spacer must render larger than it is authored, like the text around it",
            4f * platformDensity * 1.8f,
            4f * scaledDensity,
            0.001f,
        )
        assertEquals(
            "and the text itself must keep the exact size the platform gave it",
            16f * platformDensity * fontScale,
            16f * scaledDensity * scaledFontScale,
            0.001f,
        )

        // The tokens that hold in-line text keep the 1:1 ride they were given on
        // 2026-10-07: their own factor times the density's is exactly the font scale.
        assertEquals(
            "a padding token must still render 1.8x its font-1.0 size",
            token(360, 640).screenPadding.value * platformDensity * 1.8f,
            token(360, 640, fontScale).screenPadding.value * scaledDensity,
            0.001f,
        )

        // A font scale below 1 must not shrink the layout - sp already handles the text.
        assertEquals(1f, frameScaleFor(0.85f), 0.001f)
        assertEquals(1f, frameScaleFor(1f), 0.001f)
    }

    /**
     * THE PIXEL 7a REPORT, pinned to the factor that answers it.
     *
     * At a system font scale of 1.8 the text rendered at 1.8x its authored size while
     * the frame around it held still, so every padding and gap was crowded even though
     * everything technically fitted - "everything might fit but its extremely squished
     * together". The owner reported the same thing twice more ("the complete app / GUI
     * is pressed together on the emulator... i want it to look like its on my phone nice
     * and spacious"), which is how the fix ended up in the density rather than in the
     * token table: the tokens were already scaling, and the cramped pixels were the
     * components' own hard-coded dp.
     *
     * Asserted as EXACT proportionality rather than `>`, because "grows a bit" is what
     * the old damped version did: the frame has to ride the SAME factor as the text.
     */
    @Test
    fun theFrameRidesTheFontScaleWithoutSwallowingTheContent() {
        // The DENSITY's zoom IS the font scale - the whole of the surplus, no damping
        // hidden in the curve. Half was tried and is what the owner saw as "still
        // completely pressed together": at 1.8x the letters were 1.8x the phone's while
        // every box around them was 1.4x. The starvation that halving was protecting
        // against is handled structurally instead - at a large text scale the workout
        // screen is one scroll region, so no frame factor can push content off it.
        var scale = 1f
        while (scale <= 2.2f) {
            val zoom = frameScaleFor(scale)
            assertEquals(
                "the frame must ride the font scale exactly at $scale, not a fraction of it",
                scale,
                zoom,
                0.001f,
            )
            // ...while the tokens that hold in-line text still render at EXACTLY the font
            // scale: token x zoom == scale. This is the property the 2026-10-07 pass
            // pinned, and it must survive the density being folded in underneath it.
            assertEquals(
                "a spacing token must render at $scale times its font-1.0 size",
                token(360, 640).screenPadding.value * scale,
                token(360, 640, scale).screenPadding.value * zoom,
                0.001f,
            )
            scale += 0.05f
        }

        // The tokens that hold no in-line text and are sized against VERTICAL space are
        // divided back out of that zoom, so they keep the px size they have on a
        // font-scale-1.0 phone: growing a dialog's WIDTH would change the layout, not the
        // legibility. (The height-driven blocks are exempt only from the full font growth
        // - they still take their documented half-damped share, which is what the
        // 2026-10-07 pass gave them.)
        val normal = token(360, 640)
        val large = token(360, 640, fontScale = 1.8f)
        val zoom = frameScaleFor(1.8f)
        // ...AND SO DOES THE NAVIGATION BAR, which is the opposite of what it used to do.
        // Its content is all dp (a 24dp icon inside a 32dp indicator) and the frame
        // multiplies those like everything else, so dividing the bar back out left a
        // phone-sized 199px slot holding a 113px icon: on the 1.8x emulator the icon
        // band measured y 2122-2235 and the bar simply ran out from under it. At font 1.0
        // the ramp's anchor is Material's own 80dp, so the phone is unchanged.
        assertEquals(
            "the nav bar rides the frame like its icon does",
            normal.navBarHeight.value * 1.8f,
            large.navBarHeight.value * zoom,
            0.001f,
        )
        assertEquals(
            "a mainstream 411dp phone keeps Material's own 80dp bar at font 1.0",
            80f,
            token(411, 914).navBarHeight.value,
            0.5f,
        )
        assertEquals(normal.contentMaxWidth.value, large.contentMaxWidth.value * zoom, 0.001f)
        assertEquals(normal.dialogMaxWidth.value, large.dialogMaxWidth.value * zoom, 0.001f)
        assertEquals(normal.dialogCorner.value, large.dialogCorner.value * zoom, 0.001f)
        assertEquals(
            "a day chip carries text, so it rides the font scale like the other text tokens",
            normal.dayChipHeight.value * 1.8f,
            large.dayChipHeight.value * zoom,
            0.001f,
        )
        assertEquals(
            "and so does the analytics card",
            normal.donutHeight.value * 1.4f,
            large.donutHeight.value * zoom,
            0.001f,
        )
    }

    @Test
    fun fontScaleIsClampedAndNeverShrinksTheLayout() {
        // 2.2 is the cap on the font scale the frame listens to; past it the frame stops
        // growing, at 2.2. Everything at or past the cap must be identical, in the tokens
        // and in the density.
        assertEquals(2.2f, frameScaleFor(2.2f), 0.001f)
        assertEquals(2.2f, frameScaleFor(4f), 0.001f)

        val atCap = token(360, 640, fontScale = 2.2f)
        val beyondCap = token(360, 640, fontScale = 4f)
        val smaller = token(360, 640, fontScale = 0.85f)

        assertEquals(atCap.dayChipHeight.value, beyondCap.dayChipHeight.value, 0.001f)
        assertEquals(atCap.sectionSpacing.value, beyondCap.sectionSpacing.value, 0.001f)
        assertEquals(atCap.screenPadding.value, beyondCap.screenPadding.value, 0.001f)
        // A font scale below 1 must not shrink the layout - sp already handles the text itself.
        val normal = token(360, 640)
        assertEquals(normal.dayChipHeight.value, smaller.dayChipHeight.value, 0.001f)
        assertEquals(normal.sectionSpacing.value, smaller.sectionSpacing.value, 0.001f)
    }

    // ---------------------------------------------------------------------------------------
    // Structural switches (these may branch; dimensions may not).
    // ---------------------------------------------------------------------------------------

    @Test
    fun widthClassBoundariesFollowMaterial3() {
        assertEquals(WindowWidthClass.COMPACT, token(599, 900).widthClass)
        assertEquals(WindowWidthClass.MEDIUM, token(600, 900).widthClass)
        assertEquals(WindowWidthClass.MEDIUM, token(839, 900).widthClass)
        assertEquals(WindowWidthClass.EXPANDED, token(840, 900).widthClass)
    }

    @Test
    fun narrowBoundaryIs420dp() {
        assertTrue("419dp is still narrow", token(419, 900).isNarrow)
        assertFalse("420dp is no longer narrow", token(420, 900).isNarrow)
    }

    @Test
    fun shortBoundaryIs700dp() {
        assertTrue("699dp is short", token(400, 699).isShort)
        assertFalse("700dp is tall enough", token(400, 700).isShort)
    }
}
