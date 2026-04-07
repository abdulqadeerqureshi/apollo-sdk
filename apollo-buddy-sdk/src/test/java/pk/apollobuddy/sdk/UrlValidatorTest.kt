package pk.apollobuddy.sdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UrlValidatorTest {

    @Test
    fun https_urls_valid() {
        assertTrue(UrlValidator.isValid("https://example.com/"))
        assertTrue(UrlValidator.isValid("https://apollo.example.com/path?q=1"))
    }

    @Test
    fun http_rejected() {
        assertFalse(UrlValidator.isValid("http://example.com/"))
    }

    @Test
    fun blank_and_non_network_rejected() {
        assertFalse(UrlValidator.isValid(null))
        assertFalse(UrlValidator.isValid(""))
        assertFalse(UrlValidator.isValid("mailto:a@b.com"))
    }
}
