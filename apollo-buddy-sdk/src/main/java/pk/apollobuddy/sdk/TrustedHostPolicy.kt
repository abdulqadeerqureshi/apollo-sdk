package pk.apollobuddy.sdk

import android.net.Uri

/**
 * Host allowlist matching for [ApolloBuddyConfig.trustedHosts].
 * Hosts are compared case-insensitively; a trusted entry `example.com` also allows `www.example.com`
 * and other subdomains.
 */
internal object TrustedHostPolicy {

    fun isHostTrusted(uri: Uri?, trustedHosts: List<String>): Boolean {
        if (trustedHosts.isEmpty()) return false
        val host = uri?.host?.lowercase() ?: return false
        return trustedHosts.any { trusted ->
            matchesHost(host, trusted.trim().lowercase())
        }
    }

    private fun matchesHost(requestHost: String, trusted: String): Boolean {
        if (trusted.isEmpty()) return false
        if (requestHost == trusted) return true
        return requestHost.endsWith(".$trusted")
    }
}
