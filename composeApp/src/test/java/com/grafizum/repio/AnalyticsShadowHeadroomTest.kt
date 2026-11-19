package com.grafizum.repio

import androidx.compose.ui.unit.dp
import com.grafizum.repio.ui.screens.CARD_SHADOW_HEADROOM
import com.grafizum.repio.ui.screens.SHADOW_HEADROOM_ABOVE_SCROLL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Analytics tab's shadow room.
 *
 * Owner board task_13503y3v4d5e3o6w5e5e0e5v, 2026-10-12: *"the shadow around \"overall level\"
 * makes the label look cut off above so give the overlay top section a bit smaller so the shadows
 * can also go over the top side of the overall level card"*.
 *
 * WHAT THESE PIN, and why they are worth a file: the fix is a number in a layout that no other
 * test can see. A screenshot of the screen needs a live view model, and the shadow itself is drawn
 * by the platform, so the checkable facts are the two the owner asked for - the scroll box gives
 * the card room to cast its own shadow, and the space for it was taken out of the gap ABOVE the
 * box rather than added to the screen.
 */
class AnalyticsShadowHeadroomTest {

    /** The elevation the compact [com.grafizum.repio.ui.components.StatCard] draws with. */
    private val statCardElevation = 4.dp

    @Test
    fun theScrollBoxLeavesRoomForTheCardsOwnShadow() {
        assertTrue(
            "headroom $CARD_SHADOW_HEADROOM is smaller than the ${statCardElevation} shadow it " +
                "exists to show, so the card's label is clipped above again",
            CARD_SHADOW_HEADROOM >= statCardElevation
        )
    }

    @Test
    fun theHeadroomWasTakenOutOfTheGapAboveTheScrollBox() {
        // The gap above the box was 8dp before this change; the headroom had to come out of
        // THAT, or the fix would have pushed the whole page down instead of showing a shadow.
        assertEquals(
            8.dp,
            CARD_SHADOW_HEADROOM + SHADOW_HEADROOM_ABOVE_SCROLL
        )
    }
}
