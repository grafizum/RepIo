package com.grafizum.repio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True on narrow phones (Pixel 8 ≈ 411dp, compact models ≈ 360dp).
 * Use it to swap to denser typography and tighter layouts so content
 * never overflows or collides on small screens.
 */
@Composable
@Suppress("ConfigurationScreenWidthHeight") // LocalWindowInfo.containerSize needs Compose 1.8 (BOM 2025.x); see lint.xml note
fun rememberIsCompactWidth(maxWidthDp: Int = 400): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp < maxWidthDp
}

/** Material 3 window width classes (dp breakpoints per official guidance). */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Current window width class: COMPACT (< 600dp) for most phones in portrait,
 * MEDIUM (600–840dp) for tablets/foldables and large phones in landscape,
 * EXPANDED (> 840dp) for tablets/desktops. Use it to switch layout shapes
 * (single column vs. two-pane, grids vs. lists) per screen size.
 */
@Composable
@Suppress("ConfigurationScreenWidthHeight") // LocalWindowInfo.containerSize needs Compose 1.8 (BOM 2025.x); see lint.xml note
fun rememberWindowWidthClass(): WindowWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WindowWidthClass.COMPACT
        widthDp < 840 -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }
}

/** Widest a screen's content column should ever get, even on tablets. */
val MaxContentWidth = 560.dp

/**
 * Adaptive screen shell: on phones (COMPACT) children simply fill the screen;
 * on MEDIUM/EXPANDED surfaces the content column is capped at [maxWidth] and
 * centered, so the app stays readable and composed instead of stretching
 * edge-to-edge across a tablet.
 */
@Composable
fun AdaptiveWidthContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = MaxContentWidth,
    fadeBottom: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (fadeBottom) Modifier.bottomEdgeFade() else Modifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxSize()) {
            content()
        }
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
    overflow: TextOverflow = TextOverflow.Clip
) {
    val mergedStyle = style.copy(
        color = if (color != Color.Unspecified) color else style.color,
        fontWeight = fontWeight ?: style.fontWeight
    )
    BoxWithConstraints(modifier = modifier) {
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
