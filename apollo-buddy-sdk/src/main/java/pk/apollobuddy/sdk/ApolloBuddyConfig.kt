package pk.apollobuddy.sdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration options for the Apollo Buddy SDK WebView.
 */
@Parcelize
data class ApolloBuddyConfig(
    val enableJs: Boolean = true,
    val enableDomStorage: Boolean = true,
    val showToolbar: Boolean = true,
    val allowThirdPartyCookies: Boolean = false,
    val ignoreSslErrors: Boolean = false, // Honored only when the host app is debuggable; see README.
    val customUserAgent: String? = null,
    // Legacy: substring match on full URL (prefer [successPathPrefix] / [failurePathPrefix] when possible).
    val successUrlPattern: String? = null,
    val failureUrlPattern: String? = null,
    // Stricter: path must start with this prefix (must begin with `/` or it will be normalized).
    val successPathPrefix: String? = null,
    val failurePathPrefix: String? = null,
    // Query param match (e.g., param="status", value="success")
    val statusQueryParam: String? = null,
    val successQueryValue: String? = null,
    val failureQueryValue: String? = null,

    val themeColor: Int? = null,

    /**
     * Approved hostnames (e.g. `apollo.example.com`). When [enforceTrustedHostNavigation] is true,
     * only these hosts may load; subdomains of a listed host are allowed.
     */
    val trustedHosts: List<String> = emptyList(),
    /**
     * When true, initial URL and all navigations must match [trustedHosts]. Callback detection
     * only runs for URLs on a trusted host.
     */
    val enforceTrustedHostNavigation: Boolean = false,
) : Parcelable {

    class Builder {
        private var enableJs: Boolean = true
        private var enableDomStorage: Boolean = true
        private var showToolbar: Boolean = true
        private var allowThirdPartyCookies: Boolean = false
        private var ignoreSslErrors: Boolean = false
        private var customUserAgent: String? = null
        private var successUrlPattern: String? = null
        private var failureUrlPattern: String? = null
        private var successPathPrefix: String? = null
        private var failurePathPrefix: String? = null
        private var statusQueryParam: String? = null
        private var successQueryValue: String? = null
        private var failureQueryValue: String? = null
        private var themeColor: Int? = null
        private var trustedHosts: List<String> = emptyList()
        private var enforceTrustedHostNavigation: Boolean = false

        fun setEnableJs(enable: Boolean) = apply { this.enableJs = enable }
        fun setEnableDomStorage(enable: Boolean) = apply { this.enableDomStorage = enable }
        fun setAllowThirdPartyCookies(allow: Boolean) = apply { this.allowThirdPartyCookies = allow }
        fun setIgnoreSslErrors(ignore: Boolean) = apply { this.ignoreSslErrors = ignore }
        fun setCustomUserAgent(agent: String) = apply { this.customUserAgent = agent }

        /**
         * Partial string match on the full URL. Prefer [setSuccessPathPrefix] for stricter matching.
         */
        fun setSuccessUrlPattern(pattern: String) = apply { this.successUrlPattern = pattern }
        fun setFailureUrlPattern(pattern: String) = apply { this.failureUrlPattern = pattern }

        /**
         * Path must start with this prefix (e.g. `/payment/done`). Leading slash optional.
         */
        fun setSuccessPathPrefix(prefix: String?) = apply { this.successPathPrefix = prefix }
        fun setFailurePathPrefix(prefix: String?) = apply { this.failurePathPrefix = prefix }

        /**
         * Detect result by query parameter.
         * E.g. statusParam="payment_status", successValue="paid", failureValue="failed"
         */
        fun setStatusQueryParam(param: String, successValue: String?, failureValue: String?) = apply {
            this.statusQueryParam = param
            this.successQueryValue = successValue
            this.failureQueryValue = failureValue
        }

        fun setThemeColor(color: Int) = apply { this.themeColor = color }
        fun setShowToolbar(show: Boolean) = apply { this.showToolbar = show }

        fun setTrustedHosts(hosts: List<String>) = apply {
            this.trustedHosts = hosts.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        }

        fun setEnforceTrustedHostNavigation(enforce: Boolean) = apply {
            this.enforceTrustedHostNavigation = enforce
        }

        fun build() = ApolloBuddyConfig(
            enableJs,
            enableDomStorage,
            showToolbar,
            allowThirdPartyCookies,
            ignoreSslErrors,
            customUserAgent,
            successUrlPattern,
            failureUrlPattern,
            successPathPrefix = normalizePathPrefix(prefix = successPathPrefix),
            failurePathPrefix = normalizePathPrefix(failurePathPrefix),
            statusQueryParam = statusQueryParam,
            successQueryValue = successQueryValue,
            failureQueryValue = failureQueryValue,
            themeColor = themeColor,
            trustedHosts = trustedHosts,
            enforceTrustedHostNavigation = enforceTrustedHostNavigation,
        )

        private fun normalizePathPrefix(prefix: String?): String? {
            if (prefix.isNullOrBlank()) return null
            val p = prefix.trim()
            return if (p.startsWith("/")) p else "/$p"
        }
    }
}

internal fun pathMatchesPrefix(uriPath: String?, prefix: String?): Boolean {
    if (prefix.isNullOrBlank()) return false
    val path = if (uriPath.isNullOrBlank()) "/" else uriPath
    return path == prefix || path.startsWith("$prefix/")
}
