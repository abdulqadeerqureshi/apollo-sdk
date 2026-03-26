package pk.apollobuddy.sdk

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Main entry point for the Apollo Buddy SDK.
 */
object ApolloBuddySdk {

    private var isInitialized = false
    private var applicationContext: Context? = null
    internal var initParams: ApolloBuddyInitParams? = null

    /**
     * Initializes the SDK. Should be called in the Application class.
     */
    fun init(context: Context, params: ApolloBuddyInitParams) {
        if (isInitialized) return
        applicationContext = context.applicationContext
        initParams = params
        isInitialized = true
    }

    /**
     * Interface for legacy callback-style integration.
     */
    interface Callback {
        fun onResult(result: ApolloBuddyResult)
    }

    /**
     * Launches the ApolloBuddy Web Flow.
     *
     * @param context Context to launch the Activity from.
     * @param config Optional configuration for the WebView.
     * @param callback Optional callback for results (legacy style). 
     *                 For Activity Result API, use the [Contract].
     */
    fun launch(
        context: Context,
        config: ApolloBuddyConfig = ApolloBuddyConfig(),
        callback: Callback? = null,
    ) {
        if (!isInitialized || initParams == null) {
            throw IllegalStateException("ApolloBuddySdk must be initialized with params before launching")
        }

        val url = initParams!!.buildUrl()

        if (!UrlValidator.isValid(url)) {
            callback?.onResult(ApolloBuddyResult.Failure("Invalid generated URL: $url", 400))
            return
        }

        val intent = Intent(context, ApolloBuddyWebViewActivity::class.java).apply {
            putExtra(ApolloBuddyWebViewActivity.EXTRA_URL, url)
            putExtra(ApolloBuddyWebViewActivity.EXTRA_CONFIG, config)
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
    class Contract : ActivityResultContract<ApolloBuddyLaunchInput, ApolloBuddyResult>() {
        override fun createIntent(context: Context, input: ApolloBuddyLaunchInput): Intent {
            val url = initParams?.buildUrl() ?: throw IllegalStateException("ApolloBuddySdk not initialized with params")
            return Intent(context, ApolloBuddyWebViewActivity::class.java).apply {
                putExtra(ApolloBuddyWebViewActivity.EXTRA_URL, url)
                putExtra(ApolloBuddyWebViewActivity.EXTRA_CONFIG, input.config)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): ApolloBuddyResult {
            return intent?.getParcelableExtra(ApolloBuddyWebViewActivity.EXTRA_RESULT)
                ?: ApolloBuddyResult.Cancelled()
        }
    }
}

data class ApolloBuddyLaunchInput(
    val config: ApolloBuddyConfig = ApolloBuddyConfig(),
)

internal object ActiveCallbackHolder {
    var callback: ApolloBuddySdk.Callback? = null
}
