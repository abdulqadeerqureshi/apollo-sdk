package com.example.hisaab.test

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pk.myhisaab.sdk.HisaabSdk
import pk.myhisaab.sdk.HSConfig
import pk.myhisaab.sdk.HSLaunchInput
import pk.myhisaab.sdk.HSResult

class MainActivity : AppCompatActivity() {

    private val hisaabLauncher = registerForActivityResult(HisaabSdk.Contract()) { result ->
        when (result) {
            is HSResult.Success -> Toast.makeText(this, "Success: ${result.data}", Toast.LENGTH_LONG).show()
            is HSResult.Failure -> Toast.makeText(this, "Failed: ${result.reason}", Toast.LENGTH_LONG).show()
            is HSResult.Cancelled -> Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SDK
        HisaabSdk.init(applicationContext)
        
        val button = Button(this).apply {
            text = "Launch SDK"
            setOnClickListener {
                launchSdk()
            }
        }
        setContentView(button)
    }

    private fun launchSdk() {
        // Modern Way
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
                url = "https://www.google.com",
                config = config
            )
        )
        
        // Legacy Way (Commented out)
        /*
        HisaabSdk.launch(this, "https://example.com", config, object : HisaabSdk.Callback {
            override fun onResult(result: HSResult) {
                // Handle result
            }
        })
        */
    }
}
