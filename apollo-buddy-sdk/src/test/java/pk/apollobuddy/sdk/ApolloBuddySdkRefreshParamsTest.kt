package pk.apollobuddy.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class ApolloBuddySdkRefreshParamsTest {

    @Test
    fun updateParams_replacesPreviousParams_soNextLaunchUsesFreshToken() {
        ApolloBuddySdk.updateParams(paramsWithToken("expired-token"))
        ApolloBuddySdk.updateParams(paramsWithToken("fresh-token"))

        assertEquals("fresh-token", ApolloBuddySdk.initParams?.token)
    }

    private fun paramsWithToken(token: String): ApolloBuddyInitParams =
        ApolloBuddyInitParams.Builder()
            .setEloadNumber("03454729269")
            .setImsi("5fd446fa70b4109")
            .setProfileId(13)
            .setRegionId(1L)
            .setUserCode("dummy")
            .setToken(token)
            .build()
}
