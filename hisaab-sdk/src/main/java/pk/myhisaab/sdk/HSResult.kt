package pk.myhisaab.sdk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents the result of a Hisaab SDK operation.
 */
sealed class HSResult : Parcelable {

    /**
     * Operation was successful.
     * @param data payload returned by the web flow.
     */
    @Parcelize
    data class Success(val data: String) : HSResult()

    /**
     * Operation failed.
     * @param reason Human readable reason for failure.
     * @param code Error code (e.g., HTTP status or internal error code).
     */
    @Parcelize
    data class Failure(val reason: String, val code: Int, val finalUrl: String? = null) : HSResult()

    /**
     * Operation was cancelled by the user or system.
     */
    @Parcelize
    data class Cancelled(val finalUrl: String? = null) : HSResult()
}
