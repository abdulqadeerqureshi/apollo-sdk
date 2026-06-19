package pk.apollobuddy.sdk

import android.content.Intent
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration test proving the inset listener installed by [ApolloBuddyEdgeToEdge.install] actually
 * applies the computed padding to the real toolbar and content views of a launched
 * [ApolloBuddyWebViewActivity].
 *
 * We build the activity, then dispatch a mocked [WindowInsetsCompat] (as if the system reported a
 * status bar of 63px and a navigation bar of 126px) and assert the views offset themselves.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApolloBuddyEdgeToEdgeRobolectricTest {

    private fun launch(config: ApolloBuddyConfig): ApolloBuddyWebViewActivity {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ApolloBuddyWebViewActivity::class.java,
        ).apply {
            putExtra(ApolloBuddyWebViewActivity.EXTRA_URL, "https://pay.example.com/checkout")
            putExtra(ApolloBuddyWebViewActivity.EXTRA_CONFIG, config)
        }
        return Robolectric.buildActivity(ApolloBuddyWebViewActivity::class.java, intent)
            .setup()
            .get()
    }

    private fun systemBarInsets(top: Int, bottom: Int, left: Int = 0, right: Int = 0) =
        WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(left, top, right, bottom),
            )
            .build()

    @Test
    fun toolbarVisible_appliesStatusBarPaddingToToolbar_andNavBarToContent() {
        val activity = launch(ApolloBuddyConfig(showToolbar = true))

        val toolbar = activity.findViewById<View>(R.id.toolbar)
        val content = activity.findViewById<View>(R.id.content_container)
        val root = content.parent as View

        ViewCompat.dispatchApplyWindowInsets(root, systemBarInsets(top = 63, bottom = 126))

        assertEquals(63, toolbar.paddingTop)
        assertEquals(0, content.paddingTop)
        assertEquals(126, content.paddingBottom)
    }

    @Test
    fun toolbarHidden_appliesStatusBarPaddingToContent() {
        val activity = launch(ApolloBuddyConfig(showToolbar = false))

        val toolbar = activity.findViewById<View>(R.id.toolbar)
        val content = activity.findViewById<View>(R.id.content_container)
        val root = content.parent as View

        ViewCompat.dispatchApplyWindowInsets(root, systemBarInsets(top = 63, bottom = 126))

        assertEquals(View.GONE, toolbar.visibility)
        assertEquals(63, content.paddingTop)
        assertEquals(126, content.paddingBottom)
    }

    @Test
    fun horizontalInsets_applyToContentSides() {
        val activity = launch(ApolloBuddyConfig(showToolbar = true))

        val content = activity.findViewById<View>(R.id.content_container)
        val root = content.parent as View

        ViewCompat.dispatchApplyWindowInsets(
            root,
            systemBarInsets(top = 0, bottom = 0, left = 44, right = 66),
        )

        assertEquals(44, content.paddingLeft)
        assertEquals(66, content.paddingRight)
    }

    @Test
    fun insetsAreIdempotent_secondDispatchDoesNotAccumulate() {
        val activity = launch(ApolloBuddyConfig(showToolbar = true))

        val content = activity.findViewById<View>(R.id.content_container)
        val root = content.parent as View

        ViewCompat.dispatchApplyWindowInsets(root, systemBarInsets(top = 63, bottom = 126))
        ViewCompat.dispatchApplyWindowInsets(root, systemBarInsets(top = 63, bottom = 126))

        // Padding is set absolutely, not added, so two passes leave it at 126 (not 252).
        assertEquals(126, content.paddingBottom)
    }
}
