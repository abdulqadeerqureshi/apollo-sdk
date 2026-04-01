package com.example.apollobuddy.test

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pk.apollobuddy.sdk.ApolloBuddyConfig
import pk.apollobuddy.sdk.ApolloBuddyInitParams
import pk.apollobuddy.sdk.ApolloBuddyLaunchInput
import pk.apollobuddy.sdk.ApolloBuddyResult
import pk.apollobuddy.sdk.ApolloBuddySdk
import pk.apollobuddy.sdk.ApolloBuddySdk.Contract

class MainActivity : AppCompatActivity() {

    private val apolloBuddyLauncher = registerForActivityResult(Contract()) { result ->
        when (result) {
            is ApolloBuddyResult.Success -> Toast.makeText(
                this,
                "Success: ${result.data}",
                Toast.LENGTH_LONG
            ).show()

            is ApolloBuddyResult.Failure -> Toast.makeText(
                this,
                "Failed: ${result.reason}",
                Toast.LENGTH_LONG
            ).show()

            is ApolloBuddyResult.Cancelled -> Toast.makeText(
                this,
                "Cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SDK
        val params: ApolloBuddyInitParams = ApolloBuddyInitParams.Builder()
            .setWebURL("www.google.com")
            .setEloadNumber("031234567890")
            .setImsi("a410t010y5689")
            .setProfileId(15)
            .setRegionId(1)
            .setUserCode("APOL-1112")
            .setEmployeeId(1456)
            .setToken("t293f4XXXXXXXXXX")
            .build()

        ApolloBuddySdk.init(applicationContext, params)

        val button: Button = Button(this).apply {
            text = "Launch SDK"
            setOnClickListener {
                launchSdk()
            }
        }
        setContentView(button)
    }

    private fun launchSdk() {
        val config: ApolloBuddyConfig = ApolloBuddyConfig.Builder()
            .setEnableJs(enable = true)
            .setShowToolbar(show = true)
            .setAllowThirdPartyCookies(allow = true)
            .setIgnoreSslErrors(ignore = true)
            .setSuccessUrlPattern("success")
            .setStatusQueryParam(
                param = "status",
                successValue = "success",
                failureValue = "failed"
            )
            .build()

        apolloBuddyLauncher.launch(
            ApolloBuddyLaunchInput(
                config = config
            )
        )

        // Legacy Way (Commented out)
        /*
        ApolloBuddySdk.launch(this, config, object : ApolloBuddySdk.Callback {
            override fun onResult(result: ApolloBuddyResult) {
                // Handle result
            }
        })
        */
    }
}
