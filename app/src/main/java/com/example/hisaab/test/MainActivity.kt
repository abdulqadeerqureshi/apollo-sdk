package com.example.hisaab.test

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pk.myhisaab.sdk.HisaabSdk
import pk.myhisaab.sdk.HSConfig
import pk.myhisaab.sdk.HSLaunchInput
import pk.myhisaab.sdk.HSResult
import pk.myhisaab.sdk.HisaabInitParams
import pk.myhisaab.sdk.HisaabSdk.Contract

class MainActivity : AppCompatActivity() {

    private val hisaabLauncher = registerForActivityResult(Contract()) { result ->
        when (result) {
            is HSResult.Success -> Toast.makeText(
                this,
                "Success: ${result.data}",
                Toast.LENGTH_LONG
            ).show()
            is HSResult.Failure -> Toast.makeText(
                this,
                "Failed: ${result.reason}",
                Toast.LENGTH_LONG
            ).show()
            is HSResult.Cancelled -> Toast.makeText(
                this,
                "Cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SDK
        val params: HisaabInitParams = HisaabInitParams.Builder()
            .setEloadNumber("031234567890")
            .setImsi("a410t010y5689")
            .setProfileId(15)
            .setRegionId(1)
            .setUserCode("APOL-1112")
            .setEmployeeId(1456)
            .setToken("t293f4XXXXXXXXXX")
            .build()
            
        HisaabSdk.init(applicationContext, params)
        
        val button: Button = Button(this).apply {
            text = "Launch SDK"
            setOnClickListener {
                launchSdk()
            }
        }
        setContentView(button)
    }

    private fun launchSdk() {
        val config: HSConfig = HSConfig.Builder()
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
            
        hisaabLauncher.launch(
            HSLaunchInput(
                config = config
            )
        )
        
        // Legacy Way (Commented out)
        /*
        HisaabSdk.launch(this, config, object : HisaabSdk.Callback {
            override fun onResult(result: HSResult) {
                // Handle result
            }
        })
        */
    }
}
