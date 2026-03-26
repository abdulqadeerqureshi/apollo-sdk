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
    internal var initParams: HisaabInitParams? = null

    /**
     * Initializes the SDK. Should be called in the Application class.
     */
    fun init(context: Context, params: HisaabInitParams) {
        if (isInitialized) return
        applicationContext = context.applicationContext
        initParams = params
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
     * @param config Optional configuration for the WebView.
     * @param callback Optional callback for results (legacy style). 
     *                 For Activity Result API, use the [Contract].
     */
    fun launch(
        context: Context,
        config: HSConfig = HSConfig(),
        callback: Callback? = null
    ) {
        if (!isInitialized || initParams == null) {
            throw IllegalStateException("HisaabSdk must be initialized with params before launching")
        }

        val url = initParams!!.buildUrl()

        if (!UrlValidator.isValid(url)) {
            callback?.onResult(HSResult.Failure("Invalid generated URL: $url", 400))
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
            val url = initParams?.buildUrl() ?: throw IllegalStateException("HisaabSdk not initialized with params")
            return Intent(context, HisaabWebViewActivity::class.java).apply {
                putExtra(HisaabWebViewActivity.EXTRA_URL, url)
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
    val config: HSConfig = HSConfig()
)

internal object ActiveCallbackHolder {
    var callback: HisaabSdk.Callback? = null
}
