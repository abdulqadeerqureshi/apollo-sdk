package pk.myhisaab.sdk

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Main entry point for the Hisaab SDK.
 */
object HisaabSdk {

    private var isInitialized = false
    private var applicationContext: Context? = null

    /**
     * Initializes the SDK. Should be called in the Application class.
     */
    fun init(context: Context) {
        if (isInitialized) return
        applicationContext = context.applicationContext
        isInitialized = true
    }

    /**
     * Interface for legacy callback-style integration.
     */
    interface Callback {
        fun onResult(result: HSResult)
    }

    /**
     * Launches the Hisaab Web Flow.
     *
     * @param context Context to launch the Activity from.
     * @param url The URL to open. Must be a valid HTTP/HTTPS URL.
     * @param config Optional configuration for the WebView.
     * @param callback Optional callback for results (legacy style). 
     *                 For Activity Result API, use the [Contract].
     */
    fun launch(
        context: Context,
        url: String,
        config: HSConfig = HSConfig(),
        callback: Callback? = null
    ) {
        if (!isInitialized) {
            // We can strictly throw or just log. 
            // Given "robust" requirement, throwing helps dev catch it early, 
            // but auto-init is friendlier. I'll auto-init but warn.
            init(context) 
        }

        if (!UrlValidator.isValid(url)) {
            callback?.onResult(HSResult.Failure("Invalid URL: $url", 400))
            return
        }

        val intent = Intent(context, HisaabWebViewActivity::class.java).apply {
            putExtra(HisaabWebViewActivity.EXTRA_URL, url)
            putExtra(HisaabWebViewActivity.EXTRA_CONFIG, config)
        }
        
        if (callback != null) {
            ActiveCallbackHolder.callback = callback
        }
        
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    }

    /**
     * Activity Result Contract for modern integration.
     * Use with registerForActivityResult.
     */
    class Contract : ActivityResultContract<HSLaunchInput, HSResult>() {
        override fun createIntent(context: Context, input: HSLaunchInput): Intent {
            return Intent(context, HisaabWebViewActivity::class.java).apply {
                putExtra(HisaabWebViewActivity.EXTRA_URL, input.url)
                putExtra(HisaabWebViewActivity.EXTRA_CONFIG, input.config)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): HSResult {
            return intent?.getParcelableExtra(HisaabWebViewActivity.EXTRA_RESULT)
                ?: HSResult.Cancelled()
        }
    }
}

data class HSLaunchInput(
    val url: String,
    val config: HSConfig = HSConfig()
)

internal object ActiveCallbackHolder {
    var callback: HisaabSdk.Callback? = null
}
