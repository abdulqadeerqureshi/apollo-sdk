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

        /** Optional base URL */
        fun setWebURL(webURL: String) = apply { this.webURL = webURL }

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
