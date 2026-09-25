package com.grafizum.repio.ui.components

import androidx.compose.foundation.background
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
import com.grafizum.repio.ui.theme.LocalDeveloperSkin

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
private val NavBarHeight = Ramp(atMin = 60f, atReference = 76f, atMax = 92f)
private val ContentWidth = Ramp(atMin = 560f, atReference = 600f, atMax = 800f)
private val TypeScale = Ramp(atMin = 0.92f, atReference = 1f, atMax = 1.12f)

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

    // Text is measured in sp and already grows with the user's font size, so the boxes that hold
    // it have to grow too - otherwise a large-font setting clips labels inside fixed-height rows.
    // Capped at 1.6x so one setting cannot multiply the layout several times over.
    val textRoom = fontScale.coerceIn(1f, 1.6f)
    // Blocks that hold text only follow it halfway: they must not clip a large font, but they are
    // controls and cards, not paragraphs, and should not grow into a ladder either.
    val containerRoom = 1f + (textRoom - 1f) * 0.5f

    return WindowSize(
        widthDp = widthDp,
        windowHeightDp = heightDp,
        widthClass = widthClass,
        isNarrow = widthDp < NarrowWidthThreshold,
        screenPadding = ScreenPadding.dp(width),
        sectionSpacing = SectionSpacing.dp(width) * textRoom,
        cardPadding = CardPadding.dp(width) * textRoom,
        headerMinHeight = HeaderMinHeight.dp(width) * textRoom,
        headerIconSize = HeaderIconSize.dp(width),
        headerIconGlyph = HeaderIconSize.dp(width) * IconGlyphRatio,
        headerIconSpacing = HeaderIconSpacing.dp(width),
        navBarHeight = NavBarHeight.dp(width),
        contentMaxWidth = ContentWidth.dp(width),
        typeScale = TypeScale.at(width),
        // Height-driven, and never below Material's tap-target floor, so a short window gets
        // slimmer chips instead of a per-device number.
        dayChipHeight = DayChipHeight.dpAtLeast(height, MinimumTouchTarget) * containerRoom,
        donutHeight = DonutHeight.dp(height) * containerRoom,
        dialogMaxWidth = DialogMaxWidth.dp(width),
        dialogCorner = DialogCorner.dp(width),
        dialogGap = DialogGap.dp(width) * textRoom
    )
}

/** Resolves the current [WindowSize] tokens from the window width. */
@Composable
@Suppress("ConfigurationScreenWidthHeight") // LocalWindowInfo.containerSize needs Compose 1.8 (BOM 2025.x); see lint.xml note
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
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
 * buttons off-screen or squeeze them: the title block takes the leftover width and
 * wraps/ellipsizes inside it, while the actions keep their intrinsic size.
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
    val isDeveloper = LocalDeveloperSkin.current
    val window = LocalWindowSize.current
    val tint = if (isDeveloper) Color.Black else MaterialTheme.colorScheme.primary
    val resolvedTitleStyle = titleStyle
        ?: MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = window.headerMinHeight)
            .padding(vertical = 4.dp)
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
                        .clip(if (isDeveloper) RoundedCornerShape(0.dp) else CircleShape)
                        .background(
                            if (isDeveloper) Color.White
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
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
                Text(
                    text = title,
                    style = resolvedTitleStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
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

        trailing?.invoke()
    }
}

/**
 * Draws a short gradient scrim over the bottom edge of the content, matching
 * the background color, to hint that content continues below the fold.
 */
fun Modifier.bottomEdgeFade(height: Dp = 20.dp): Modifier = composed {
    val scrim = MaterialTheme.colorScheme.background
    drawWithContent {
        drawContent()
        val fadePx = height.toPx()
        if (size.height > fadePx) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(scrim.copy(alpha = 0f), scrim),
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
