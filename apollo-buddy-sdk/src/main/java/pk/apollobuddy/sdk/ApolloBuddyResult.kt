package pk.apollobuddy.sdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the result of an Apollo Buddy SDK operation.
 *
 * **Logging:** [Success.data], [Failure.finalUrl], and [Cancelled.finalUrl] may contain sensitive
 * query parameters (tokens, identifiers). Do not log them in production as plain text; use
 * [SensitiveDataRedaction.redactUrl] if you need a safe string for diagnostics.
 */
sealed class ApolloBuddyResult : Parcelable {

    /**
     * Operation was successful.
     *
     * @param data Final URL string when success rules matched (may contain secrets in query params).
     */
    @Parcelize
    data class Success(val data: String) : ApolloBuddyResult()

    /**
     * Operation failed.
     *
     * @param reason Human-readable reason; does not include full raw URLs from the SDK.
     * @param code Error code (e.g. HTTP status or internal error code).
     * @param finalUrl Optional URL associated with the failure; treat as sensitive if logged.
     */
    @Parcelize
    data class Failure(val reason: String, val code: Int, val finalUrl: String? = null) : ApolloBuddyResult()

    /**
     * The user left the flow without a success or failure match (e.g. back with no history).
     *
     * @param finalUrl Optional current WebView URL; treat as sensitive if logged.
     */
    @Parcelize
    data class Cancelled(val finalUrl: String? = null) : ApolloBuddyResult()
}
