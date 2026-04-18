package pk.apollobuddy.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates the long-token URL shape against the **production** base host (not localhost).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionApolloUrlTest {

    @Test
    fun buildUrl_withProductionHost_encodesTokenAndPassesHttpsValidator() {
        val built =
            ApolloBuddyInitParams.Builder()
                .setEloadNumber("03454729269")
                .setImsi("5fd446fa70b4109")
                .setProfileId(13)
                .setRegionId(1L)
                .setUserCode("dummy")
                .setToken(LongSampleTokenFixture.RAW)
                .setWebURL(LongSampleTokenFixture.PRODUCTION_BASE_URL)
                .build()
                .buildUrl()

        val expected =
            "${LongSampleTokenFixture.PRODUCTION_BASE_URL.trimEnd('/')}/?" +
                    "eloadNumber=03454729269&imsi=5fd446fa70b4109&profileId=13&regionId=1&userCode=dummy&token=${LongSampleTokenFixture.PERCENT_ENCODED}"
        assertEquals(expected, built)

        assertTrue(UrlValidator.isValid(built))
    }

    @Test
    fun production_longToken_urlFitsInIntentExtra_byDefault() {
        val built =
            ApolloBuddyInitParams.Builder()
                .setEloadNumber("03454729269")
                .setImsi("5fd446fa70b4109")
                .setProfileId(13)
                .setRegionId(1L)
                .setUserCode("dummy")
                .setToken(LongSampleTokenFixture.RAW)
                .setWebURL(LongSampleTokenFixture.PRODUCTION_BASE_URL)
                .build()
                .buildUrl()

        assertTrue(
            "URL should be passed via Intent extra (not pending holder); length=${built.length}",
            built.length <= ApolloBuddySdk.MAX_URL_INTENT_EXTRA_LENGTH,
        )
    }
}
