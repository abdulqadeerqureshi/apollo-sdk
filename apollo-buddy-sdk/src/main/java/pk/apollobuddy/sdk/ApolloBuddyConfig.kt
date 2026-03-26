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
    val ignoreSslErrors: Boolean = false, // Security warning: Only use TRUE for testing!
    val customUserAgent: String? = null,
    // String content match
    val successUrlPattern: String? = null,
    val failureUrlPattern: String? = null,
    // Query param match (e.g., param="status", value="success")
    val statusQueryParam: String? = null,
    val successQueryValue: String? = null,
    val failureQueryValue: String? = null,

    val themeColor: Int? = null,
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
        private var statusQueryParam: String? = null
        private var successQueryValue: String? = null
        private var failureQueryValue: String? = null
        private var themeColor: Int? = null

        fun setEnableJs(enable: Boolean) = apply { this.enableJs = enable }
        fun setEnableDomStorage(enable: Boolean) = apply { this.enableDomStorage = enable }
        fun setAllowThirdPartyCookies(allow: Boolean) = apply { this.allowThirdPartyCookies = allow }
        fun setIgnoreSslErrors(ignore: Boolean) = apply { this.ignoreSslErrors = ignore }
        fun setCustomUserAgent(agent: String) = apply { this.customUserAgent = agent }

        /**
         * partial string match on the URL. 
         * If the URL contains this string, it is considered a success.
         */
        fun setSuccessUrlPattern(pattern: String) = apply { this.successUrlPattern = pattern }
        fun setFailureUrlPattern(pattern: String) = apply { this.failureUrlPattern = pattern }

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

        fun build() = ApolloBuddyConfig(
            enableJs,
            enableDomStorage,
            showToolbar,
            allowThirdPartyCookies,
            ignoreSslErrors,
            customUserAgent,
            successUrlPattern,
            failureUrlPattern,
            statusQueryParam,
            successQueryValue,
            failureQueryValue,
            themeColor
        )
    }
}
