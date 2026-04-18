package pk.apollobuddy.sdk

import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ApolloBuddyInitParams.buildUrl] uses [android.net.Uri.Builder.appendQueryParameter], which
 * percent-encodes query values the same way as `URL` + `searchParams.set` in JavaScript (e.g. `/` →
 * `%2F`, `+` → `%2B`, `=` → `%3D`), so tokens with base64-like characters survive in the query string.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApolloBuddyInitParamsBuildUrlTest {

    @Test
    fun buildUrl_percentEncodesQueryValuesLikeUrlSearchParams() {
        val params =
            ApolloBuddyInitParams.Builder()
                .setEloadNumber("03454729269")
                .setImsi("5fd446fa70b4109")
                .setProfileId(13)
                .setRegionId(1L)
                .setUserCode("dummy")
                .setToken(LongSampleTokenFixture.RAW)
                // Use https so normalization does not rewrite the scheme (http is upgraded to https).
                .setWebURL("https://localhost:3001/")
                .build()

        val built = params.buildUrl()
        assertEquals(
            "https://localhost:3001/?eloadNumber=03454729269&imsi=5fd446fa70b4109&profileId=13&regionId=1&userCode=dummy&token=${LongSampleTokenFixture.PERCENT_ENCODED}",
            built,
        )
    }

    @Test
    fun rawUriAppendQueryParameter_encodesSlashesInValue() {
        // Document Android behavior: slashes in query values must be escaped for tokens/JWTs.
        val built =
            "https://localhost:3001/"
                .toUri()
                .buildUpon()
                .appendQueryParameter("token", "a/b+c=d")
                .build()
                .toString()
        assertEquals("https://localhost:3001/?token=a%2Fb%2Bc%3Dd", built)
    }
}
