package pk.apollobuddy.sdk

import android.net.Uri
import androidx.core.net.toUri

/**
 * Initialization parameters for the Apollo Buddy SDK.
 */
class ApolloBuddyInitParams private constructor(
    val eloadNumber: String,
    val imsi: String,
    val profileId: Int,
    val regionId: Int,
    val userCode: String?,
    val employeeId: Int?,
    val token: String,
    val webURL: String,
) {

    fun buildUrl(): String {
        val uriBuilder: Uri.Builder? = webURL.toUri().buildUpon()
        uriBuilder?.appendQueryParameter("eloadNumber", eloadNumber)
        uriBuilder?.appendQueryParameter("imsi", imsi)
        uriBuilder?.appendQueryParameter("profileId", profileId.toString())
        uriBuilder?.appendQueryParameter("regionId", regionId.toString())

        userCode?.let { uriBuilder?.appendQueryParameter("userCode", it) }
        employeeId?.let { uriBuilder?.appendQueryParameter("employeeId", it.toString()) }

        uriBuilder?.appendQueryParameter("token", token)

        return uriBuilder?.build().toString()
    }

    class Builder {
        private var eloadNumber: String? = null
        private var imsi: String? = null
        private var profileId: Int? = null
        private var regionId: Int? = null
        private var userCode: String? = null
        private var employeeId: Int? = null
        private var token: String? = null
        private var webURL: String? = null

        fun setEloadNumber(eloadNumber: String) = apply { this.eloadNumber = eloadNumber }
        fun setImsi(imsi: String) = apply { this.imsi = imsi }
        fun setProfileId(profileId: Int) = apply { this.profileId = profileId }
        fun setRegionId(regionId: Int) = apply { this.regionId = regionId }

        /** Required for Non-AD users */
        fun setUserCode(userCode: String) = apply { this.userCode = userCode }

        /** Required for AD users */
        fun setEmployeeId(employeeId: Int) = apply { this.employeeId = employeeId }

        fun setToken(token: String) = apply { this.token = token }

        /** Optional base URL. Trims input and fixes common mistakes (missing scheme, slash after host). */
        fun setWebURL(webURL: String) = apply { this.webURL = normalizeWebUrl(webURL) }

        fun build(): ApolloBuddyInitParams {
            val finalEloadNumber: String = requireNotNull(value = eloadNumber) { "eloadNumber is required" }
            require(finalEloadNumber.isNotBlank()) { "eloadNumber cannot be blank" }

            val finalImsi: String = requireNotNull(value = imsi) { "imsi is required" }
            require(finalImsi.isNotBlank()) { "imsi cannot be blank" }

            val finalProfileId = requireNotNull(profileId) { "profileId is required" }
            require(finalProfileId > 0) { "profileId must be a valid positive number" }

            val finalRegionId = requireNotNull(regionId) { "regionId is required" }
            require(finalRegionId > 0) { "regionId must be a valid positive number" }

            val finalToken: String = requireNotNull(value = token) { "token is required" }
            require(finalToken.isNotBlank()) { "token cannot be blank" }

            require(userCode != null || employeeId != null) {
                "Either userCode (for Non-AD users) or employeeId (for AD users) must be provided"
            }

            val finalWebUrl: String = webURL ?: "https://apollo.digitalmiles.org/"

            return ApolloBuddyInitParams(
                eloadNumber = finalEloadNumber,
                imsi = finalImsi,
                profileId = finalProfileId,
                regionId = finalRegionId,
                userCode = userCode,
                employeeId = employeeId,
                token = finalToken,
                webURL = finalWebUrl
            )
        }
    }
}

/**
 * Normalizes a developer-provided base URL so [Uri.Builder] can append query parameters reliably.
 *
 * - Trims whitespace.
 * - Adds a scheme when missing: `https://` for normal hosts; `https:` for scheme-relative `//host/...`;
 *   does **not** prepend `https://` to opaque URIs (`mailto:`, `tel:`, `data:`, …) or to `host:port` forms
 *   (handled by prefixing `https://` to the whole string).
 * - If the URI has a hierarchical `scheme://authority` but **no path**, sets the path to `/`
 *   (including `https://host?query` → `https://host/?query`).
 * - If a non-root path already exists, the string is unchanged (no extra trailing slash).
 *
 * Not handled (callers should pass valid absolute web bases): relative paths (`foo/bar`), malformed
 * URLs, or userinfo edge cases (`user:pass@host` mis-parsed as opaque).
 */
internal fun normalizeWebUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed

    val url = ensureHttpsForMissingScheme(trimmed)

    val uri: Uri = url.toUri()
    if (uri.scheme.isNullOrEmpty() || uri.authority.isNullOrEmpty()) {
        return url
    }

    val path = uri.path
    return if (path.isNullOrEmpty()) {
        uri.buildUpon().path("/").build().toString()
    } else {
        url
    }
}

/** Adds `https` when the string is clearly a missing-scheme web URL; leaves opaque schemes alone. */
private fun ensureHttpsForMissingScheme(url: String): String {
    if (url.contains("://")) return url
    if (url.startsWith("//")) return "https:$url"
    // host:port or host:port/path (e.g. api.example.com:443, localhost:8080/foo)
    if (HOST_WITH_PORT_WITHOUT_SCHEME.matches(url)) return "https://$url"
    val colon = url.indexOf(':')
    if (colon > 0) {
        val afterColon = url.substring(colon + 1)
        if (!afterColon.startsWith("//")) {
            val beforeColon = url.substring(0, colon)
            if (beforeColon.matches(SCHEME_TOKEN)) {
                return url
            }
        }
    }
    return "https://$url"
}

/** First segment is digits-only after ':' (port), optional path/query. */
private val HOST_WITH_PORT_WITHOUT_SCHEME =
    Regex("^[^/?#]+:(\\d+)(?:/.*)?(?:[?#].*)?$")

private val SCHEME_TOKEN = Regex("[a-zA-Z][a-zA-Z0-9+.-]*")
