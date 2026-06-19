package pk.apollobuddy.sdk

import android.view.View
import android.view.Window
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import pk.apollobuddy.sdk.ApolloBuddyEdgeToEdge.computeEdgePadding
import pk.apollobuddy.sdk.ApolloBuddyEdgeToEdge.install

/**
 * Edge-to-edge inset handling for the Apollo Buddy SDK UI.
 *
 * ### Why this exists
 * [ApolloBuddyWebViewActivity] calls `enableEdgeToEdge()`, which makes the system bars transparent
 * and lets content draw behind them. That call alone is *not enough*: unless the SDK consumes the
 * resulting [WindowInsetsCompat], the toolbar renders behind the status bar and the WebView runs
 * under the navigation bar / display cutout. This object owns that inset consumption.
 *
 * ### Defensive by design
 * - The SDK renders into its **own** activity window, so enabling edge-to-edge and mutating the
 *   decor view here never touches the host application's window. The host can have edge-to-edge on,
 *   off, or be unaware of it; the SDK UI is correct either way.
 * - Padding is computed from the union of `systemBars()` and `displayCutout()` so the UI is safe on
 *   notched / punch-hole devices and in landscape (where the nav bar and cutout move to the sides).
 * - The keyboard (`ime()`) inset is merged into the bottom so focused inputs in the WebView are not
 *   hidden behind the soft keyboard.
 *
 * The arithmetic lives in [computeEdgePadding] as a pure function so it can be unit-tested on the
 * JVM without a real `Window`; [install] is the thin View/framework wiring on top of it.
 */
internal object ApolloBuddyEdgeToEdge {

    /** Immutable description, in pixels, of the padding to apply to the toolbar and content view. */
    @VisibleForTesting
    internal data class EdgePadding(
        val toolbarLeft: Int,
        val toolbarTop: Int,
        val toolbarRight: Int,
        val contentLeft: Int,
        val contentTop: Int,
        val contentRight: Int,
        val contentBottom: Int,
    )

    /**
     * Pure, framework-free computation of the padding needed to keep SDK UI clear of the system
     * bars and display cutout.
     *
     * @param barLeft/[barTop]/[barRight]/[barBottom] union of `systemBars()` + `displayCutout()`.
     * @param imeBottom current keyboard inset; merged into the content bottom (max with the nav bar)
     *   so the larger of the two wins and inputs stay visible.
     * @param toolbarVisible when the toolbar is shown it absorbs the top inset (its background then
     *   fills the status-bar area); when hidden, the content view absorbs the top inset instead so a
     *   full-bleed WebView never sits under the status bar.
     */
    @VisibleForTesting
    internal fun computeEdgePadding(
        barLeft: Int,
        barTop: Int,
        barRight: Int,
        barBottom: Int,
        imeBottom: Int,
        toolbarVisible: Boolean,
    ): EdgePadding {
        val bottom = maxOf(barBottom, imeBottom)
        return if (toolbarVisible) {
            EdgePadding(
                toolbarLeft = barLeft,
                toolbarTop = barTop,
                toolbarRight = barRight,
                contentLeft = barLeft,
                contentTop = 0,
                contentRight = barRight,
                contentBottom = bottom,
            )
        } else {
            EdgePadding(
                toolbarLeft = 0,
                toolbarTop = 0,
                toolbarRight = 0,
                contentLeft = barLeft,
                contentTop = barTop,
                contentRight = barRight,
                contentBottom = bottom,
            )
        }
    }

    /**
     * Installs inset handling on [root]:
     *  - an [OnApplyWindowInsetsListener][ViewCompat.setOnApplyWindowInsetsListener] that distributes
     *    the *resting* padding to [toolbar] and [content], and
     *  - a [WindowInsetsAnimationCompat.Callback] that, on API 30+, tracks the IME (keyboard) frame
     *    by frame so the content's bottom padding slides with the keyboard instead of snapping.
     *
     * The apply-listener is idempotent: [updatePadding] sets absolute padding, so repeated passes
     * (rotation, keyboard show/hide, multi-window resize) never accumulate. It returns
     * [WindowInsetsCompat.CONSUMED] so descendants — notably the WebView — do not re-apply the same
     * insets and double-pad.
     *
     * @param toolbarVisibleProvider read on every inset pass, so it reflects the toolbar visibility
     *   set during `Activity.onCreate` before the first layout. NOTE: the apply-listener only
     *   re-fires when the system delivers new insets. If a future revision toggles toolbar
     *   visibility *dynamically* (e.g. hide-on-scroll), it must call [ViewCompat.requestApplyInsets]
     *   on [root] after changing visibility, or the padding will not be redistributed.
     */
    fun install(
        root: View,
        toolbar: View,
        content: View,
        toolbarVisibleProvider: () -> Boolean,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val padding = computeEdgePadding(
                barLeft = bars.left,
                barTop = bars.top,
                barRight = bars.right,
                barBottom = bars.bottom,
                imeBottom = ime.bottom,
                toolbarVisible = toolbarVisibleProvider(),
            )
            toolbar.updatePadding(
                left = padding.toolbarLeft,
                top = padding.toolbarTop,
                right = padding.toolbarRight,
            )
            content.updatePadding(
                left = padding.contentLeft,
                top = padding.contentTop,
                right = padding.contentRight,
                bottom = padding.contentBottom,
            )
            WindowInsetsCompat.CONSUMED
        }

        // Smooth keyboard tracking (API 30+). DISPATCH_MODE_STOP suspends the apply-listener for the
        // duration of the IME animation, so onProgress is the sole updater while the keyboard slides
        // and there is no initial jump to the final position. When the animation ends, the system
        // re-dispatches insets and the apply-listener restores the full resting padding. Below API
        // 30 this is a no-op and the apply-listener + windowSoftInputMode=adjustResize handle it.
        ViewCompat.setWindowInsetsAnimationCallback(
            root,
            object : WindowInsetsAnimationCompat.Callback(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
            ) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: List<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    val imeAnimating = runningAnimations.any {
                        it.typeMask and WindowInsetsCompat.Type.ime() != 0
                    }
                    if (imeAnimating) {
                        val bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars() or
                                    WindowInsetsCompat.Type.displayCutout(),
                        )
                        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                        // Reuse the single source of truth for the bottom value; only the bottom
                        // animates for the IME, so leave the sides/top untouched mid-animation.
                        val padding = computeEdgePadding(
                            barLeft = bars.left,
                            barTop = bars.top,
                            barRight = bars.right,
                            barBottom = bars.bottom,
                            imeBottom = ime.bottom,
                            toolbarVisible = toolbarVisibleProvider(),
                        )
                        content.updatePadding(bottom = padding.contentBottom)
                    }
                    return insets
                }
            },
        )

        // Force a pass in case the first inset dispatch happened before the listener was attached.
        ViewCompat.requestApplyInsets(root)
    }

    /**
     * Sets status-bar icon contrast for a known bar [backgroundColor] (e.g. the toolbar color).
     *
     * `enableEdgeToEdge()` picks icon color from the system day/night mode, but the SDK toolbar
     * color is independent of that, so we override based on the background's luminance: light
     * background → dark icons, dark background → light icons. Scoped to the SDK's own [window].
     */
    fun applyStatusBarIconContrast(window: Window, isLightBackground: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = isLightBackground
    }
}
