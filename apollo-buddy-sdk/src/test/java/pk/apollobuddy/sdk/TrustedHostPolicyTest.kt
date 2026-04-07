package pk.apollobuddy.sdk

import androidx.core.net.toUri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrustedHostPolicyTest {

    @Test
    fun exact_host_match() {
        val uri = "https://apollo.example.com/app".toUri()
        assertTrue(TrustedHostPolicy.isHostTrusted(uri, listOf("apollo.example.com")))
    }

    @Test
    fun subdomain_allowed() {
        val uri = "https://www.apollo.example.com/x".toUri()
        assertTrue(TrustedHostPolicy.isHostTrusted(uri, listOf("apollo.example.com")))
    }

    @Test
    fun other_host_rejected() {
        val uri = "https://evil.com/".toUri()
        assertFalse(TrustedHostPolicy.isHostTrusted(uri, listOf("apollo.example.com")))
    }

    @Test
    fun empty_list_not_trusted() {
        val uri = "https://example.com/".toUri()
        assertFalse(TrustedHostPolicy.isHostTrusted(uri, emptyList()))
    }

    @Test
    fun webStorageOriginForUrl_https_default_port() {
        assertTrue(
            webStorageOriginForUrl("https://example.com/path") == "https://example.com",
        )
    }

    @Test
    fun webStorageOriginForUrl_custom_port() {
        assertTrue(
            webStorageOriginForUrl("https://example.com:8443/") == "https://example.com:8443",
        )
    }

    @Test
    fun pathMatchesPrefix_stricter_than_substring() {
        assertTrue(pathMatchesPrefix("/payment/complete", "/payment"))
        assertFalse(pathMatchesPrefix("/evil/payment", "/payment"))
        assertTrue(pathMatchesPrefix("/payment", "/payment"))
    }
}
