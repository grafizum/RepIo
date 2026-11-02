package com.grafizum.repio

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The progression popup's connector ([com.grafizum.repio.ui.components.EvolutionTreeDialog]'s
 * `TreeElbows`), held to the three rules a RENDER taught and no pure case can hold: the drawing is
 * private, the project has no ui-test on the classpath, and the defect this guards was a
 * pixel-perfect line of the right colour at a plausible coordinate - nothing a unit test could
 * have seen.
 *
 * Owner, 2026-10-16: the lines of *\"the progression panel that shows when the user did max level
 * of an exercise\"* were *\"badly designed\"*. MEASURED on the Pixel_7a emulator through the
 * debug preview harness: the fourth branch card, outside the dialog's scroll viewport, reported a
 * rect whose centre was at y = 504 - the scroll container's own top, 749px ABOVE the trunk it
 * branches from. The spine took its far end from `arms.last()` without asking whether that arm was
 * ever drawn, so it was drawn from the trunk's bottom edge (y = 1232) UP to that phantom row: a
 * grey stripe across the trunk card and the dialog's subtitle, while the three real arms below
 * hung off nothing (evidence/progression-lines-2026-10-16/before-pushup.png vs after).
 */
class EvolutionConnectorGuardTest {

    private val source =
        File("src/main/java/com/grafizum/repio/ui/components/EvolutionTreeDialog.kt").readText()

    /** Comments stripped, so a rule mentioned in prose is not mistaken for a rule in code. */
    private val code = source
        .replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        .replace(Regex("//[^\\n]*"), "")

    /** The drop's near end is the trunk's bottom edge - not its middle, not the canvas top. */
    @Test
    fun theDropLeavesFromTheTrunkBottomEdge() {
        assertTrue(
            "the drop must be measured from the trunk card's own bottom edge",
            code.contains("val dropTop = sx.bottom - origin.y"),
        )
    }

    /** THE RULE THE RENDER TAUGHT: the spine may only end on an arm that is actually drawn. */
    @Test
    fun theSpineOnlyEndsOnAnArmThatIsDrawn() {
        assertFalse(
            "the spine ends at `arms.last()`, which is the bug: the last row can be one that " +
                "reported no rectangle, in which case the drop is drawn UP out of the trunk",
            code.contains("arms.last()"),
        )
        assertTrue(
            "the drop must end on the LOWEST arm that passed the gates",
            code.contains("arms.maxOf { it.y }"),
        )
    }

    /** A row that has not been laid out reports an EMPTY rect, and an empty rect is not a card. */
    @Test
    fun aRowWithoutARectangleCanNeverGrowAnArm() {
        assertTrue(
            "rects with no area must be filtered out before an arm is built from them",
            code.contains(".filter { (_, rect) -> !rect.isEmpty }"),
        )
        assertTrue(
            "an arm must also land past the spine and BELOW the drop - a child drawn at or above " +
                "its parent is a phantom row by another name",
            code.contains("it.endX > spineX && it.y > dropTop"),
        )
    }

    /**
     * Each arm keeps ITS OWN row, because the gates above can drop rows and the colours are
     * indexed by row: a shifted index dresses the fourth card's arm in the third card's colour.
     */
    @Test
    fun anArmCarriesItsOwnRowSoTheColoursCannotShift() {
        assertTrue(
            "the arm colour must be looked up by the arm's own row",
            code.contains("armColors.getOrElse(arm.row)"),
        )
        assertFalse(
            "an index over the FILTERED list is the colour shift this rule exists to prevent",
            code.contains("arms.forEachIndexed"),
        )
    }
}
