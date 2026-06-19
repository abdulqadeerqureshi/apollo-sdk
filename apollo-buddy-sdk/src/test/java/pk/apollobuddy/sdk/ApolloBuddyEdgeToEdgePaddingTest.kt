package pk.apollobuddy.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM unit tests for [ApolloBuddyEdgeToEdge.computeEdgePadding].
 *
 * No Android framework, no Robolectric — this verifies the inset distribution math directly, which
 * is where the real logic lives. The Robolectric test ([ApolloBuddyEdgeToEdgeRobolectricTest])
 * separately proves the listener actually applies these values to real Views.
 */
class ApolloBuddyEdgeToEdgePaddingTest {

    @Test
    fun toolbarVisible_topInsetGoesToToolbar_bottomToContent() {
        val p = ApolloBuddyEdgeToEdge.computeEdgePadding(
            barLeft = 0,
            barTop = 63,   // status bar
            barRight = 0,
            barBottom = 126, // navigation bar
            imeBottom = 0,
            toolbarVisible = true,
        )

        // Toolbar absorbs the status bar so its background fills behind it.
        assertEquals(63, p.toolbarTop)
        // Content sits flush under the toolbar (no extra top), and clears the nav bar at the bottom.
        assertEquals(0, p.contentTop)
        assertEquals(126, p.contentBottom)
    }

    @Test
    fun toolbarHidden_topInsetGoesToContent() {
        val p = ApolloBuddyEdgeToEdge.computeEdgePadding(
            barLeft = 0,
            barTop = 63,
            barRight = 0,
            barBottom = 126,
            imeBottom = 0,
            toolbarVisible = false,
        )

        // With no toolbar, the content must absorb the top inset itself.
        assertEquals(0, p.toolbarTop)
        assertEquals(63, p.contentTop)
        assertEquals(126, p.contentBottom)
    }

    @Test
    fun landscapeCutout_horizontalInsetsApplyToBothToolbarAndContent() {
        // Gesture nav + left-edge cutout in landscape: insets move to the sides.
        val p = ApolloBuddyEdgeToEdge.computeEdgePadding(
            barLeft = 44,
            barTop = 0,
            barRight = 66,
            barBottom = 0,
            imeBottom = 0,
            toolbarVisible = true,
        )

        assertEquals(44, p.toolbarLeft)
        assertEquals(66, p.toolbarRight)
        assertEquals(44, p.contentLeft)
        assertEquals(66, p.contentRight)
    }

    @Test
    fun keyboardOpen_bottomIsMaxOfNavBarAndIme() {
        // IME taller than the nav bar -> keyboard wins so inputs stay above it.
        val keyboardUp = ApolloBuddyEdgeToEdge.computeEdgePadding(
            barLeft = 0, barTop = 63, barRight = 0, barBottom = 126,
            imeBottom = 900,
            toolbarVisible = true,
        )
        assertEquals(900, keyboardUp.contentBottom)

        // IME hidden (0) -> nav bar wins.
        val keyboardDown = ApolloBuddyEdgeToEdge.computeEdgePadding(
            barLeft = 0, barTop = 63, barRight = 0, barBottom = 126,
            imeBottom = 0,
            toolbarVisible = true,
        )
        assertEquals(126, keyboardDown.contentBottom)
    }

    @Test
    fun noInsets_producesZeroPadding() {
        // Host with opaque bars / pre-edge-to-edge: insets are already consumed by the system, so
        // nothing should be added and the layout must not shift.
        val p = ApolloBuddyEdgeToEdge.computeEdgePadding(
            barLeft = 0, barTop = 0, barRight = 0, barBottom = 0,
            imeBottom = 0,
            toolbarVisible = true,
        )

        assertEquals(0, p.toolbarLeft)
        assertEquals(0, p.toolbarTop)
        assertEquals(0, p.toolbarRight)
        assertEquals(0, p.contentLeft)
        assertEquals(0, p.contentTop)
        assertEquals(0, p.contentRight)
        assertEquals(0, p.contentBottom)
    }
}
