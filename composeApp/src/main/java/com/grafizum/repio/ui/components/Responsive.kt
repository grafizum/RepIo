package com.grafizum.repio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window width classes, deliberately aligned with Material 3's breakpoints so the
 * app-wide shell (nav bar, content column) and the individual screens agree on
 * what "big" means.
 *
 * COMPACT  : every phone in portrait (< 600dp) — Pixel 4a/8 at 393–411dp included.
 * MEDIUM   : small tablets, foldables and large phones in landscape (600–840dp).
 * EXPANDED : tablets and desktop-class windows (>= 840dp).
 */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Below this width a phone is treated as *narrow* and gets the dense treatment:
 * slightly smaller headings, tighter screen padding and a compact app header.
 * Covers the small-Pixel class of devices (Pixel 4a/5 at 393dp, Pixel 6a/7/8 at 411dp)
 * as well as phones whose display size has been turned up in Android settings.
 */
private const val NarrowWidthThreshold = 420

/**
 * At or above this font scale a window is treated as *large text*.
 *
 * Calibrated against what the frame does to itself: the frame tracks the user's font
 * scale 1:1 (see [MAXIMUM_FRAME_SCALE]), so at 1.3x every padding, gap and chip grows
 * by a third while the SCREEN does not. That is the point where a wrapped filter row
 * stops being able to hold two chips per line — measured on the Pixel-class emulator at
 * 1.8x, the "Bodyweight" chip alone is 466px of a 738px content row, so the equipment
 * picker drew one chip per line, eight lines deep, and pushed the exercise list off the
 * bottom of the replace dialog entirely. The owner's report was exactly that: "the user
 * cant even see exercises to replace".
 *
 * Unlike [ShortHeightThreshold] this is NOT about the window being small — the same
 * 411x914dp phone is perfectly roomy at 1.0x. It is about the frame having outgrown the
 * window, which is when a screen has to stop assuming its filters and its list fit
 * together. Structural decisions only: a screen may collapse, reorder or hide chrome on
 * this flag, never pick a dimension from it.
 */
private const val LargeTextFontScaleThreshold = 1.3f

/**
 * Below this height a window is treated as *short*.
 *
 * Calibrated against real hardware: a 360x640dp phone (Galaxy S6 Edge class) leaves the app
 * roughly 560dp of usable height once the status and navigation bars are subtracted. On a
 * window this short a screen can no longer hand out "leftover" space - three stacked cards
 * already exceed the viewport - so such screens must scroll and give every card a real
 * height instead of a `weight(1f)` slice of whatever is left. The reference tall phone
 * (Pixel 5/8 class, ~800dp) never takes this branch.
 *
 * Only *structural* decisions may branch on this (scroll vs. squeeze, one column vs. two). No
 * dimension is ever chosen by it - dimensions come from the interpolated ramps below.
 */
private const val ShortHeightThreshold = 700

// ---------------------------------------------------------------------------------------------
// Adaptive token table
//
// Every dimension the app uses is described by what it should be on the smallest phone worth
// supporting, on a typical phone, and on a large tablet/desktop window. windowSizeFor()
// interpolates between those three anchors, so a window of *any* size gets values proportional
// to its own space instead of being rounded into a bucket (narrow / compact / medium / ...).
// That is what keeps the layout continuous rather than tuned for a handful of known devices.
// ---------------------------------------------------------------------------------------------

/** Smallest phone width still worth designing for (320dp-class devices). */
private const val MinLayoutWidthDp = 320f

/** A comfortable phone width. Anchors at this size are what a mainstream phone receives. */
private const val ReferenceWidthDp = 400f

/** Largest window the phone-first layout is expected to stretch into. */
private const val MaxLayoutWidthDp = 1000f

/** Shortest window: a phone in landscape, or one whose display size is turned up. */
private const val MinLayoutHeightDp = 480f

/** Typical phone height in portrait (about 640dp of usable window). */
private const val ReferenceHeightDp = 640f

/** Tallest window worth spending extra vertical room on. */
private const val MaxLayoutHeightDp = 1000f

/** Material's minimum comfortable tap target. Tappable containers never go below it. */
private val MinimumTouchTarget = 48.dp

/** A header icon's glyph as a fraction of its tile, so tile and glyph stay in proportion. */
private const val IconGlyphRatio = 0.57f

/**
 * A dimension described by three anchors: its value on the smallest supported window, on a
 * typical phone, and on the largest window.
 *
 * [at] takes a 0..1 position in which 0.5 is the typical phone, so a token can be read off at any
 * window size in between. Values are never quantised to a discrete bucket, which is what lets a
 * 360dp phone and a 411dp phone each get the spacing that suits them.
 */
private class Ramp(
    private val atMin: Float,
    private val atReference: Float,
    private val atMax: Float
) {
    fun at(position: Float): Float = if (position <= 0.5f) {
        atMin + (atReference - atMin) * (position * 2f)
    } else {
        atReference + (atMax - atReference) * ((position - 0.5f) * 2f)
    }

    fun dp(position: Float): Dp = at(position).dp

    /** Same as [dp], but never smaller than [minimum]. */
    fun dpAtLeast(position: Float, minimum: Dp): Dp =
        at(position).coerceAtLeast(minimum.value).dp
}

/** 0..1 coordinate of [value] between [min] and [max], with [reference] pinned at 0.5. */
private fun anchorPosition(value: Float, min: Float, reference: Float, max: Float): Float =
    if (value <= reference) {
        ((value - min) / (reference - min)).coerceIn(0f, 1f) * 0.5f
    } else {
        0.5f + ((value - reference) / (max - reference)).coerceIn(0f, 1f) * 0.5f
    }

/** Where a window width sits between the width anchors. */
private fun widthPosition(widthDp: Int): Float =
    anchorPosition(widthDp.toFloat(), MinLayoutWidthDp, ReferenceWidthDp, MaxLayoutWidthDp)

/** Where a window height sits between the height anchors. */
private fun heightPosition(heightDp: Int): Float =
    anchorPosition(heightDp.toFloat(), MinLayoutHeightDp, ReferenceHeightDp, MaxLayoutHeightDp)

// Spacing and typography follow the room a window has horizontally.
private val ScreenPadding = Ramp(atMin = 10f, atReference = 16f, atMax = 32f)
private val SectionSpacing = Ramp(atMin = 10f, atReference = 16f, atMax = 24f)
private val CardPadding = Ramp(atMin = 14f, atReference = 20f, atMax = 28f)
private val HeaderMinHeight = Ramp(atMin = 52f, atReference = 64f, atMax = 80f)
private val HeaderIconSize = Ramp(atMin = 36f, atReference = 42f, atMax = 52f)
private val HeaderIconSpacing = Ramp(atMin = 8f, atReference = 12f, atMax = 16f)
// Anchored so a mainstream phone gets Material's own 80dp bar at font 1.0 (the shell's
// hard-coded number until 2026-10-08), which keeps the font-1.0 layout pixel-identical
// while letting the bar grow with its label once the text is large.
private val NavBarHeight = Ramp(atMin = 64f, atReference = 80f, atMax = 96f)
private val ContentWidth = Ramp(atMin = 560f, atReference = 600f, atMax = 800f)
private val TypeScale = Ramp(atMin = 0.92f, atReference = 1f, atMax = 1.12f)

// The gap between two pieces of text that share one line. It is the one spacing the
// table was missing: padding and card gaps grew with the font, but the handful of gaps
// that sit BETWEEN words were written as plain `Spacer(width = 8.dp)`, and a dp does not
// follow the user's font size. At 1.8x on the 411dp phone a single space is ~7px against
// a 26px-tall letter, so "4 sets × 8–12 reps" and its "• Moderate" arrived with zero
// pixels of air - the owner's "pressed together labeling and text".
private val InlineGap = Ramp(atMin = 6f, atReference = 8f, atMax = 12f)

// Blocks that consume vertical space follow the room a window has vertically, so a short window
// hands space back to the content instead of spending it on chrome.
private val DayChipHeight = Ramp(atMin = 48f, atReference = 52f, atMax = 56f)
private val DonutHeight = Ramp(atMin = 190f, atReference = 240f, atMax = 340f)
// Popup surface: one shell for every dialog, so the three ramps below are what keeps a form
// dialog and a filter popup the same object on a given window.
private val DialogMaxWidth = Ramp(atMin = 480f, atReference = 560f, atMax = 680f)
private val DialogCorner = Ramp(atMin = 20f, atReference = 24f, atMax = 28f)
private val DialogGap = Ramp(atMin = 12f, atReference = 16f, atMax = 20f)

/**
 * Every size-dependent dimension the UI is allowed to care about.
 *
 * Screens must read spacing from here instead of hard-coding `16.dp`, otherwise the
 * same fixed numbers have to work on a 360dp phone *and* a 1280dp tablet — which is
 * exactly how an interface ends up cramped on one and stretched on the other.
 */
data class WindowSize(
    /** Window width in dp (the usable width, excluding system insets). */
    val widthDp: Int = 420,
    val widthClass: WindowWidthClass = WindowWidthClass.COMPACT,
    /** True on narrow phones: denser spacing, smaller headings. */
    val isNarrow: Boolean = false,
    /**
     * True when the user's font scale is large enough that the frame has outgrown the
     * window (see [LargeTextFontScaleThreshold]), so filters and lists can no longer be
     * assumed to fit together and a screen must prioritise.
     */
    val isLargeText: Boolean = false,
    /** Window height in dp, excluding the status/navigation bars. */
    val windowHeightDp: Int = 800,
    /**
     * True when the window is too short to give a screen a "leftover space" layout; see
     * [ShortHeightThreshold]. Such screens must scroll and size their cards explicitly.
     */
    val isShort: Boolean = windowHeightDp < ShortHeightThreshold,
    /** Horizontal padding every screen applies to its content column. */
    val screenPadding: Dp = 16.dp,
    /** Vertical gap between major blocks (cards, sections). */
    val sectionSpacing: Dp = 16.dp,
    /** Inner padding of a card. */
    val cardPadding: Dp = 20.dp,
    /** Minimum height of a screen header; it grows further if the title wraps. */
    val headerMinHeight: Dp = 64.dp,
    /** Diameter of the round/tile icon next to a screen title. */
    val headerIconSize: Dp = 42.dp,
    /** Glyph drawn inside the header icon tile. */
    val headerIconGlyph: Dp = 24.dp,
    /** Gap between the header icon tile and the title. */
    val headerIconSpacing: Dp = 12.dp,
    /** Height of the bottom navigation bar. */
    val navBarHeight: Dp = 76.dp,
    /** Widest the content column may get before it is centered on the surface. */
    val contentMaxWidth: Dp = MaxContentWidth,
    /** Multiplier applied to the display/headline/title styles at the theme level. */
    val typeScale: Float = 1f,
    /**
     * Gap between two pieces of text on one line (a value and its unit, an exercise and
     * its badge). Scales with the user's font scale, because what it separates does:
     * every fixed-dp gap in the app grew stale the moment the text around it grew.
     */
    val inlineGap: Dp = 8.dp,
    /**
     * Height of one day chip in the workout day wheel. Deliberately *definite* rather than a
     * minimum: the chip's content fills its box, so a minimum lets the wheel grow to
     * whatever height the surrounding column happens to offer and swallow the whole screen.
     */
    val dayChipHeight: Dp = 48.dp,
    /**
     * Height of the analytics muscle-group card. Definite for the same reason as
     * [dayChipHeight] - the donut and its legend divide this height between them, which only
     * works while the height is bounded.
     */
    val donutHeight: Dp = 240.dp,
    /**
     * Widest any popup surface may get. Every dialog in the app renders through one shell, so a
     * form dialog and a filter popup cannot end up different sizes on the same phone.
     */
    val dialogMaxWidth: Dp = 560.dp,
    /** Corner radius of the popup surface. */
    val dialogCorner: Dp = 24.dp,
    /** Gap between a dialog's title, its scrolling body and its button row. */
    val dialogGap: Dp = 16.dp
)

/** Widest a screen's content column should ever get on a regular phone. */
val MaxContentWidth = 560.dp

/**
 * Current window size, provided once by [com.grafizum.repio.ui.theme.WorkoutTrackerTheme].
 * Defaults keep previews and tests working without an explicit provider.
 */
val LocalWindowSize = staticCompositionLocalOf { WindowSize() }

/** Hard-coded breakpoints, resolved from the window width. */
private fun WindowWidthClassFor(widthDp: Int): WindowWidthClass = when {
    widthDp < 600 -> WindowWidthClass.COMPACT
    widthDp < 840 -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}

/**
 * Most the user's font scale may multiply the FRAME by. Text itself follows the
 * system setting without a ceiling; this only stops one accessibility setting from
 * multiplying the whole layout several times over.
 */
private const val MAXIMUM_FRAME_SCALE = 2.2f

/**
 * How much the whole dp frame grows to match the user's font scale.
 *
 * OWNER REPORT, 2026-10-08 (the third time this has come up): "the complete app /
 * GUI is pressed together on the emulator... i want it to look like its on my
 * phone nice and spacious and not so close together".
 *
 * Two earlier attempts fixed only the tokens (`screenPadding` and friends) and left
 * the hundreds of `Modifier.padding(16.dp)`, `Spacer(height = 4.dp)`, `size(24.dp)`
 * in the components at their authored size. Measured on the 1.8x emulator: the
 * tokens were already correct (a 16dp screen padding measured 75px = 28.8dp, the
 * same as the owner's phone at 1.0x), but the gap between an exercise's name and
 * its "4 sets x 8-12 reps" line was 5px where the same gap is 16px on the phone -
 * because that spacer is a hard-coded 4dp and a dp does not follow the font size.
 * Crumbs like that are what "the complete app is pressed together" means, and no
 * token table can fix them: they are inside components.
 *
 * So the factor is applied ONCE, to the DENSITY (see WorkoutTrackerTheme), where it
 * governs every dp in the tree - tokens and hard-coded values alike - while sp text
 * keeps exactly the size the platform gave it. That is what this function returns,
 * and it is what a caller needs to know only if it is doing the arithmetic itself
 * (the theme, and the tests). Components keep writing `16.dp`.
 */
fun frameScaleFor(fontScale: Float): Float =
    1f + (fontScale.coerceIn(1f, MAXIMUM_FRAME_SCALE) - 1f) * FRAME_GROWTH_SHARE

/**
 * How much of the font scale's surplus the frame takes.
 *
 * ALL OF IT. This is the rule AGENTS.md has documented since 2026-10-07 - "the user's
 * fontScale drives the FRAME and the text with the SAME factor" - and it is the only
 * setting under which the app looks like the SAME app on two phones whose font sizes
 * differ. A gap, a padding and the letters they separate then all sit in the same
 * proportion as they do at font scale 1.0; anything less makes the text outgrow its
 * own chrome, which is precisely the "pressed together" the owner has now reported
 * three times ("its about the general space formatting depending on each device that
 * needs to be remade").
 *
 * WHY THIS WAS BRIEFLY HALVED, AND WHAT REPLACED IT. Growing every dp by the font
 * scale was tried once before and reverted, because on the 1.8x emulator the workout
 * screen's FIXED chrome (header, day wheel, progress row, pinned action, bottom bar)
 * grew past the window and left the scrolling middle of the screen about 100px - the
 * exercise name and its level card were pushed behind the fold. The lesson was right
 * but the conclusion was wrong: the answer is not to keep the whole frame undersized,
 * it is to stop pinning that much chrome. At a large text scale the workout screen is
 * now ONE scroll region (WorkoutNowScreen), so no amount of frame growth can starve
 * the content - the page simply scrolls - and the frame is free to be the honest size.
 *
 * Cost, stated plainly and measured, not hidden: at font scale 1.8 on a 411x914dp
 * phone the window only holds 1/1.8 of its font-1.0 content in each axis, so about a
 * third of the phone's page is visible and the rest is a scroll away. That is what a
 * 1.8x zoom of a fixed-size window IS, and it is why the size of the fixed chrome - not
 * the frame factor - is the thing that has to be watched from now on.
 */
private const val FRAME_GROWTH_SHARE = 1f

/**
 * Maps a window size to the spacing/typography tokens that fit it.
 *
 * Width drives spacing and typography, height drives the blocks that consume vertical space, and
 * the user's font scale is folded in. Every value is interpolated between the anchors above, so a
 * 360dp phone and a 411dp phone each get their own answer instead of sharing one hard-coded set,
 * and nothing changes abruptly when a device happens to sit on a breakpoint.
 */
fun windowSizeFor(widthDp: Int, heightDp: Int = 800, fontScale: Float = 1f): WindowSize {
    val width = widthPosition(widthDp)
    val height = heightPosition(heightDp)
    val widthClass = WindowWidthClassFor(widthDp)

    // THE USER'S FONT SCALE DRIVES THE FRAME AND THE TEXT WITH THE SAME FACTOR -
    // through the DENSITY, not through this table (see [frameScaleFor]). What stays
    // here is the part that is about the WINDOW, and the two exemptions that must
    // NOT ride the font scale, because growing them changes the layout rather than
    // the legibility of text: the height-driven BLOCKS (they are sized against
    // vertical space, so a large font must not eat a short window) and the tokens
    // that hold no in-line text at all (contentMaxWidth, the dialog WIDTH, the dialog
    // corner). Both are divided back out of the density's factor so they keep the
    // size they have on a font-scale-1.0 phone. Do not cross the axes.
    val frameScale = frameScaleFor(fontScale)
    val requested = fontScale.coerceIn(1f, MAXIMUM_FRAME_SCALE)
    // The tokens keep their full 1:1 ride on the font scale (they are the parts that
    // hold in-line text), so they are authored as `tokenZoom` and the density finishes
    // the job: tokenZoom * frameScale == the font scale. The exempt ones are divided
    // back out, so they render exactly the px they did before any of this existed.
    val tokenZoom = requested / frameScale
    val notZoomed = 1f / frameScale
    val containerRoom = 1f + (requested - 1f) * 0.5f

    return WindowSize(
        widthDp = widthDp,
        windowHeightDp = heightDp,
        widthClass = widthClass,
        isNarrow = widthDp < NarrowWidthThreshold,
        isLargeText = fontScale >= LargeTextFontScaleThreshold,
        screenPadding = ScreenPadding.dp(width) * tokenZoom,
        sectionSpacing = SectionSpacing.dp(width) * tokenZoom,
        cardPadding = CardPadding.dp(width) * tokenZoom,
        headerMinHeight = HeaderMinHeight.dp(width) * tokenZoom,
        headerIconSize = HeaderIconSize.dp(width) * tokenZoom,
        headerIconGlyph = HeaderIconSize.dp(width) * IconGlyphRatio * tokenZoom,
        headerIconSpacing = HeaderIconSpacing.dp(width) * tokenZoom,
        // The bar rides the frame like every other token - which at font 1.0 is exactly
        // Material's 80dp (see the ramp's anchors), so the phone is unchanged. It must
        // NOT be divided back out: its whole content is dp (a 24dp icon inside a 32dp
        // indicator), the frame multiplies those with everything else, and a phone-sized
        // 199px slot could not hold the resulting 113px icon + 151px pill - measured on
        // the 1.8x emulator, the icon band sat at y 2122-2235 while the bar ran out from
        // under it. (The labels are gone at that size for the same reason; see MainActivity.)
        navBarHeight = NavBarHeight.dp(width),
        contentMaxWidth = ContentWidth.dp(width) * notZoomed,
        typeScale = TypeScale.at(width),
        inlineGap = InlineGap.dp(width) * tokenZoom,
        // Height-driven, and never below Material's tap-target floor, so a short window gets
        // slimmer chips instead of a per-device number.
        // The day chip HOLDS in-line text (the day letter and the TODAY tag), so it
        // rides the font scale like the other text-bearing tokens - measured on the
        // owner's screenshot at 1.8x, the TODAY tag was drawn over its own chip's
        // rounded edge because the chip's height was pinned to a font-1.0 number.
        // `dpAtLeast` still hands a SHORT window slimmer chips, which is the part of
        // the old damping that mattered; the half-damped share it also carried would
        // just have made a large-font chip twice as tall as the text inside it.
        dayChipHeight = DayChipHeight.dpAtLeast(height, MinimumTouchTarget) * tokenZoom,
        donutHeight = DonutHeight.dp(height) * containerRoom * notZoomed,
        dialogMaxWidth = DialogMaxWidth.dp(width) * notZoomed,
        dialogCorner = DialogCorner.dp(width) * notZoomed,
        dialogGap = DialogGap.dp(width) * tokenZoom
    )
}

/**
 * Resolves the current [WindowSize] tokens from the window width, measured at [fontScale].
 *
 * [fontScale] is an explicit parameter because the app PINS its text to one scale
 * (`APP_TEXT_SCALE` in Theme.kt) and these tokens have to describe the text the app will
 * actually draw, not a system setting it has stopped honouring. It defaults to the
 * platform's own scale, so for any other caller the answer is still "this real window".
 */
@Composable
@Suppress("ConfigurationScreenWidthHeight") // LocalWindowInfo.containerSize needs Compose 1.8 (BOM 2025.x); see lint.xml note
fun rememberWindowSize(fontScale: Float = LocalDensity.current.fontScale): WindowSize {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp, fontScale) {
        windowSizeFor(widthDp, heightDp, fontScale)
    }
}

/**
 * Current window width class: COMPACT (< 600dp) for most phones in portrait,
 * MEDIUM (600–840dp) for tablets/foldables and large phones in landscape,
 * EXPANDED (> 840dp) for tablets/desktops. Use it to switch layout shapes
 * (single column vs. two-pane, grids vs. lists) per screen size.
 */
@Composable
@Suppress("ConfigurationScreenWidthHeight")
fun rememberWindowWidthClass(): WindowWidthClass =
    WindowWidthClassFor(LocalConfiguration.current.screenWidthDp)

/**
 * The system's OWN chrome, in dp: the status bar (and display cutout) above the window's
 * content, and the gesture/navigation area below it.
 *
 * WHY THE APP NEEDS TO KNOW THIS. `Scaffold` insets its content with
 * `ScaffoldDefaults.contentWindowInsets` (= the system bars) for free, so every screen inside
 * the app shell is safe by construction. Whole-screen surfaces that are NOT inside it - the
 * one-time level-guide gate and the welcome setup - are siblings of the `Scaffold` in
 * `MainActivity`, so nothing was reserving that space for them: each padded its page by
 * [WindowSize.screenPadding] alone (about 16dp), which is the app's own margin and has nothing
 * to do with the system's.
 *
 * Measured on the two phones that are the design pair - both 411dp wide, so comparable:
 *
 *  - Samsung S22+ (the benchmark): status bar 28dp, gesture bar 15dp.
 *  - Pixel 7a: status bar 45dp, gesture bar 48dp.
 *
 * With a 16dp page margin that difference decides whether a screen is broken: the guide's top
 * row sat 29dp under the Pixel's status bar (and 2dp under the S22+'s), and its closing button
 * was painted under the Pixel's 48dp gesture area - behind the OS's opaque navigation bar, so
 * invisible as well as untappable - while on the S22+ the same margin just about cleared. That
 * is why this reads as a thin-phone-only fault and survived two passes: the benchmark device
 * is the one whose chrome is small enough to hide it.
 *
 * Exposed as a token rather than applied at the shell so a screen can pad its CONTENT while its
 * background still fills the window edge to edge: insetting the surface instead would leave the
 * window background showing as a band outside it (measured, the setup screen's own white leaves
 * a 3/255 step in light and a 20/255 one in dark).
 */
data class SafeArea(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val start: Dp = 0.dp,
    val end: Dp = 0.dp
)

/**
 * Current [SafeArea], provided once by [com.grafizum.repio.ui.theme.WorkoutTrackerTheme].
 * Zero by default so previews and tests keep the window they already had.
 */
val LocalSafeArea = staticCompositionLocalOf { SafeArea() }

/**
 * Adaptive screen shell: on phones (COMPACT) children simply fill the screen;
 * on MEDIUM/EXPANDED surfaces the content column is capped at [maxWidth] and
 * centered, so the app stays readable and composed instead of stretching
 * edge-to-edge across a tablet.
 *
 * When [maxWidth] is left null the cap comes from the current [WindowSize], which
 * scales continuously with the surface (a phone keeps a phone-width column, a large
 * tablet gets a wider - but still readable - one) instead of pinning every device to
 * one narrow column.
 */
@Composable
fun AdaptiveWidthContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp? = null,
    fadeBottom: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveMaxWidth = maxWidth ?: LocalWindowSize.current.contentMaxWidth
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (fadeBottom) Modifier.bottomEdgeFade() else Modifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.widthIn(max = effectiveMaxWidth).fillMaxSize()) {
            content()
        }
    }
}

/**
 * The one header every screen uses.
 *
 * It is deliberately built so a long title (German "Intelligenter Trainingsassistent",
 * Italian labels, or a user with a large font size) can never push the trailing action
 * buttons off-screen or squeeze them: the title block takes the leftover width and is
 * CAPPED with an ellipsis inside it, while the actions keep their intrinsic size.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    titleStyle: TextStyle? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val window = LocalWindowSize.current
    val tint = MaterialTheme.colorScheme.primary
    val resolvedTitleStyle = titleStyle
        ?: MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)

    // THE ACTIONS DROP BELOW THE TITLE WHEN THE TEXT IS BIG.
    //
    // Measured on the 1.8x emulator (owner's screenshot, 2026-10-08): the title row
    // kept its trailing actions at their intrinsic size and gave the title whatever
    // was left - which was nothing. "Thursday" did not appear ANYWHERE in the
    // accessibility tree, and the same header on the other tabs showed an empty gap
    // between the icon and the pills. No amount of spacing fixes a row whose parts
    // do not fit; the row has to become two rows.
    val stackActions = window.isLargeText && trailing != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = window.headerMinHeight)
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // weight(1f): the title block absorbs the leftover width and keeps the trailing
            // actions pinned to the right, exactly like the previous SpaceBetween layout — but
            // a long title now wraps inside that block instead of compressing the actions.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                leading?.invoke()

                if (icon != null || iconPainter != null) {
                    Box(
                        modifier = Modifier
                            .size(window.headerIconSize)
                            .clip(CircleShape)
                            // NO WHITE TILE, IN ANY THEME.
                            //
                            // The developer skin used to paint this disc `onSurface`,
                            // which on a dark scheme is #EDEBE8 - a near-white circle with
                            // the gold glyph inside it. The owner read it exactly as it
                            // looked: "the icon on the top left of the dumbbell ... make
                            // sure it doesnt have a white background color but its only a
                            // nicely modern designed good quality icon". The inversion was
                            // a leftover of the black-and-white pixel skin this theme
                            // replaced, so the tile is now the accent at 15% in every
                            // theme - a dark disc with an accent mark on the gold scheme.
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val glyphModifier = Modifier.size(window.headerIconGlyph)
                        when {
                            icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = glyphModifier)
                            iconPainter != null -> Icon(iconPainter, contentDescription = null, tint = tint, modifier = glyphModifier)
                        }
                    }
                    Spacer(modifier = Modifier.width(window.headerIconSpacing))
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    // THE FULL TITLE, ON ONE ROW, CAPPED WITH AN ELLIPSIS WHEN IT DOES
                    // NOT FIT.
                    //
                    // OWNER RULING, 2026-10-08, which replaces the abbreviation ladder
                    // that used to live here: "change the day back from the short version
                    // Do to the long one donnerstag but cap it and write ... next to it in
                    // case the name is too long for the screen". So the title is never
                    // shortened to a different WORD and never wraps: it is drawn in full
                    // at the header's own size, and only the tail is replaced by an
                    // ellipsis when the row cannot hold it.
                    //
                    // One row is still the invariant, and it is the whole reason the
                    // abbreviation existed: "Donnerstag" measured 221px tall (three lines
                    // of token line height) in the 411dp phone's workout header, beside
                    // actions that are one row high, so a wrapping title pushed the rest
                    // of the header down for no reading gain. `maxLines = 1` keeps that
                    // protection while the name stays the real name.
                    Text(
                        text = title,
                        style = resolvedTitleStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!stackActions) trailing?.invoke()
    }

        if (stackActions) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                trailing?.invoke()
            }
        }
    }
}

/**
 * Draws a short gradient scrim over the bottom edge of the content, matching
 * the background color, to hint that content continues below the fold.
 *
 * [scrim] resolves the color the fade runs INTO, and is a lambda because it is read inside
 * [composed]'s composable scope - a plain `Color` parameter would read the theme outside of
 * one. The default is the app background, which is what a scrim on a screen wants; a scrim
 * drawn inside a dialog passes the dialog's own surface ([AppDialog] uses
 * surfaceContainerHigh), because the background color there is simply not what is behind the
 * content - the fade would paint the wrong color and read as a smudge rather than a fold.
 */
fun Modifier.bottomEdgeFade(
    height: Dp = 20.dp,
    scrim: @Composable () -> Color = { MaterialTheme.colorScheme.background }
): Modifier = composed {
    val scrimColor = scrim()
    drawWithContent {
        drawContent()
        val fadePx = height.toPx()
        if (size.height > fadePx) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(scrimColor.copy(alpha = 0f), scrimColor),
                    startY = size.height - fadePx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - fadePx),
                size = Size(size.width, fadePx)
            )
        }
    }
}

/**
 * Text that visually shrinks (down to [minScale] of its normal size) when its
 * single line would be wider than the available box, instead of overflowing,
 * clipping mid-glyph, or being authored at an unreadable fixed font size.
 *
 * The scale is applied at draw time only, so layout size never changes and the
 * measurement settles in a single recomposition — no loops, even inside
 * fixed-size cells like the 7-slot day wheel.
 */
@Composable
fun AutoScaleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    minScale: Float = 0.72f,
    overflow: TextOverflow = TextOverflow.Clip,
    contentAlignment: Alignment = Alignment.Center
) {
    val mergedStyle = style.copy(
        color = if (color != Color.Unspecified) color else style.color,
        fontWeight = fontWeight ?: style.fontWeight
    )
    // contentAlignment matters: with a modifier that makes the box wider than the text (the day
    // wheel passes fillMaxWidth) a top-start Box would leave the label parked against the left
    // edge. Centring is a no-op whenever the box is exactly the text's own size.
    BoxWithConstraints(modifier = modifier, contentAlignment = contentAlignment) {
        val boxWidthPx = if (constraints.maxWidth != Constraints.Infinity) {
            constraints.maxWidth.toFloat()
        } else {
            Float.MAX_VALUE
        }
        var scale by remember(text, boxWidthPx) { mutableFloatStateOf(1f) }
        Text(
            text = text,
            style = mergedStyle,
            maxLines = maxLines,
            softWrap = false,
            overflow = overflow,
            modifier = Modifier
                .onSizeChanged { size ->
                    if (boxWidthPx > 0f && size.width > boxWidthPx) {
                        val target = (boxWidthPx / size.width).coerceIn(minScale, 1f)
                        if (target < scale) scale = target
                    } else if (scale < 1f && size.width * scale <= boxWidthPx) {
                        // Fits again (e.g. after rotation) — restore full size.
                        scale = 1f
                    }
                }
                .drawWithContent {
                    if (scale < 1f) {
                        drawContext.transform.scale(
                            scaleX = scale,
                            scaleY = scale,
                            pivot = Offset(size.width / 2f, size.height / 2f)
                        )
                    }
                    drawContent()
                }
        )
    }
}
