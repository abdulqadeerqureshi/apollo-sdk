package pk.apollobuddy.sdk

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers common developer inputs and combinations for [normalizeWebUrl].
 * Uses Robolectric so [android.net.Uri] behaves like on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NormalizeWebUrlTest {

    @Test
    fun google_facebook_spacex_variants() {
        val cases =
            listOf(
                // User-requested hosts (with/without trailing slash, mixed case)
                "www.google.com/" to "https://www.google.com/",
                "www.google.com" to "https://www.google.com/",
                "www.facebook.org" to "https://www.facebook.org/",
                "www.facebook.org/" to "https://www.facebook.org/",
                "www.sapaceX.org" to "https://www.sapaceX.org/",
                "www.sapaceX.org/" to "https://www.sapaceX.org/",
                "WWW.GOOGLE.COM" to "https://WWW.GOOGLE.COM/",
                "WWW.GOOGLE.COM/" to "https://WWW.GOOGLE.COM/",
            )
        assertAllCases(cases)
    }

    @Test
    fun explicit_http_https() {
        val cases =
            listOf(
                "http://www.google.com" to "http://www.google.com/",
                "http://www.google.com/" to "http://www.google.com/",
                "https://www.google.com" to "https://www.google.com/",
                "https://www.google.com/" to "https://www.google.com/",
                "https://www.facebook.org/path" to "https://www.facebook.org/path",
                "http://www.facebook.org/foo/bar" to "http://www.facebook.org/foo/bar",
            )
        assertAllCases(cases)
    }

    @Test
    fun scheme_relative_double_slash() {
        val cases =
            listOf(
                "//www.google.com" to "https://www.google.com/",
                "//www.google.com/" to "https://www.google.com/",
                "//www.facebook.org/foo" to "https://www.facebook.org/foo",
            )
        assertAllCases(cases)
    }

    @Test
    fun whitespace_trimmed() {
        assertEquals("https://www.google.com/", normalizeWebUrl("  www.google.com  "))
        assertEquals("https://www.google.com/", normalizeWebUrl("\twww.google.com\n"))
    }

    @Test
    fun query_without_path_gets_slash_before_query() {
        val cases =
            listOf(
                "https://www.google.com?a=1" to "https://www.google.com/?a=1",
                "www.google.com?x=y" to "https://www.google.com/?x=y",
                "http://www.facebook.org?q=1" to "http://www.facebook.org/?q=1",
            )
        assertAllCases(cases)
    }

    @Test
    fun host_with_port() {
        val cases =
            listOf(
                "www.google.com:443" to "https://www.google.com:443/",
                "www.google.com:443/" to "https://www.google.com:443/",
                "localhost:8080" to "https://localhost:8080/",
                "localhost:8080/app" to "https://localhost:8080/app",
            )
        assertAllCases(cases)
    }

    @Test
    fun opaque_schemes_not_prefixed_with_https_slash() {
        val cases =
            listOf(
                "mailto:test@example.com" to "mailto:test@example.com",
                "tel:+123" to "tel:+123",
                "data:text/plain,hi" to "data:text/plain,hi",
            )
        assertAllCases(cases)
    }

    @Test
    fun empty_after_trim() {
        assertEquals("", normalizeWebUrl("   "))
    }

    /** Large matrix: subdomains, paths, query, fragment, ports — documents expected normalization. */
    @Test
    fun combination_matrix_additional_hosts() {
        val cases =
            listOf(
                // Subdomain / app paths
                "m.facebook.org" to "https://m.facebook.org/",
                "m.facebook.org/chat" to "https://m.facebook.org/chat",
                "api.sapaceX.org/v1" to "https://api.sapaceX.org/v1",
                // Fragment (no path → slash before #)
                "https://www.google.com#section" to "https://www.google.com/#section",
                "www.google.com#section" to "https://www.google.com/#section",
                // Query + fragment
                "https://www.facebook.org?ref=1#x" to "https://www.facebook.org/?ref=1#x",
                // Already-canonical https with path
                "https://www.sapaceX.org/launch" to "https://www.sapaceX.org/launch",
                "HTTP://WWW.GOOGLE.COM" to "HTTP://WWW.GOOGLE.COM/",
            )
        assertAllCases(cases)
    }

    private fun assertAllCases(cases: List<Pair<String, String>>) {
        cases.forEach { (input, expected) ->
            assertEquals(
                "input=$input",
                expected,
                normalizeWebUrl(input),
            )
        }
    }
}
